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

    public async Task<IEnumerable<Exercise>?> GetRoutineExercisesByDateAsync(DateTime date, Guid phaseId)
    {
        const string query = """
                             SELECT e.id, 
                                    e.name, 
                                    e.description, 
                                    e.estimated_duration as EstimatedDuration,
                                    e.repetitions,
                                    e.difficulty 
                             FROM routine r 
                             JOIN exercise e on e.id = r.exercise_id
                             WHERE execution_date = @Date
                             AND phase_id = @PhaseId
                             ORDER BY order_number;
                             """;

        return await ExecuteAsync(async connection =>
            await connection.QueryAsync<Exercise>(query, new { Date = date, PhaseId = phaseId }));
    }

    public async Task<Exercise?> GetExerciseById(Guid id)
    {
        const string query = """
                             SELECT id, 
                                    name, 
                                    description, 
                                    repetitions, 
                                    difficulty, 
                                    estimated_duration as EstimatedDuration, 
                                    instructions 
                             FROM exercise
                             WHERE id = @Id;
                             """;

        return await ExecuteAsync(async connection =>
            await connection.QueryFirstOrDefaultAsync<Exercise>(query, new { Id = id }));
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