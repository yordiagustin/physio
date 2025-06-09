using System.Data;
using Dapper;
using Physio.Core;

namespace Physio.Api;

public class DatabaseService(IConfiguration configuration)
{
    private readonly string _connectionString = configuration.GetConnectionString("DefaultConnection") ??
                                                throw new ArgumentNullException($"Connection String is required");

    public async Task<bool> PatientPhoneNumberExistsAsync(string phoneNumber)
    {
        const string query = "SELECT COUNT(1) FROM patient WHERE phone_number = @phoneNumber";

        return await ExecuteAsync(async connection =>
        {
            var count = await connection.ExecuteScalarAsync<int>(query, new { PhoneNumber = phoneNumber });
            return count > 0;
        });
    }

    public async Task<Patient?> GetPatientByPhoneNumberAsync(string phoneNumber)
    {
        const string query = "SELECT id, name, surname FROM patient WHERE phone_number = @phoneNumber";
        
        return await ExecuteAsync(async connection =>
            await connection.QueryFirstOrDefaultAsync<Patient>(query, new { PhoneNumber = phoneNumber }));
    }

    public async Task<IEnumerable<Exercise>> GetExerciseByPatient(string patientId)
    {
        const string query = """
                             SELECT
                                 e.id as exercise_id,
                                 e.name as exercise_name,
                                 e.estimated_duration_minutes,
                                 e.difficulty_level,
                                 e.instructions
                             FROM patient_exercise_assignments pea
                             JOIN exercises e ON pea.exercise_id = e.id
                             WHERE pea.patient_id = @PatientId
                               AND pea.is_active = true
                               AND e.is_active = true
                             ORDER BY pea.assigned_date DESC;
                             """;
        
        return await ExecuteAsync(async connection =>
            await connection.QueryAsync<Exercise>(query, new { PatientId = patientId }));
    }

    private async Task<T> ExecuteAsync<T>(Func<IDbConnection, Task<T>> operation)
    {
        try
        {
            await using var connection = new Npgsql.NpgsqlConnection(_connectionString);
            return await operation(connection);
        }
        catch (Exception ex)
        {
            throw new Exception("Database operation failed.", ex);
        }
    }
}