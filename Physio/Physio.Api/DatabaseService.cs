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