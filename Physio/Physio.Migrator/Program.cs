using System.Net.Sockets;
using System.Reflection;
using DbUp;
using Microsoft.Extensions.Configuration;
using Npgsql;
using Polly;

const string scriptPath = "Physio.Migrator.Scripts";
var retryDelays = new[]
    { TimeSpan.FromSeconds(1), TimeSpan.FromSeconds(2), TimeSpan.FromSeconds(5), TimeSpan.FromSeconds(10) };

var assembly = Assembly.GetEntryAssembly()!;
var configuration = new ConfigurationBuilder()
    .SetBasePath(Directory.GetCurrentDirectory())
    .AddJsonFile("appsettings.json", false)
    .AddEnvironmentVariables()
    .AddUserSecrets(assembly)
    .AddCommandLine(args)
    .Build();

var connectionString = configuration.GetConnectionString("PhysioDatabase");

var migrationSection = configuration.GetSection("MigrationVariables");
var variables = new Dictionary<string, string>()
{
    { "ApplicationUsername", migrationSection.GetRequiredSection("ApplicationUsername").Value! },
    { "ApplicationPassword", migrationSection.GetRequiredSection("ApplicationPassword").Value! }
};

Policy
    .Handle<SocketException>(exception => exception.Message.Contains("No connection could be made"))
    .WaitAndRetry(retryDelays)
    .Execute(() =>
    {
        var connection = new NpgsqlConnectionStringBuilder(connectionString);

        EnsureDatabase.For.PostgresqlDatabase(connection.ConnectionString);

        var upgradeEngine =
            DeployChanges.To
                .PostgresqlDatabase(connection.ConnectionString)
                .WithScriptsEmbeddedInAssembly(Assembly.GetEntryAssembly(),
                    resource => string.IsNullOrWhiteSpace(scriptPath) || resource.StartsWith(scriptPath))
                .WithVariables(variables)
                .LogToConsole()
                .Build();

        var result = upgradeEngine.PerformUpgrade();

        if (!result.Successful) throw new ApplicationException("Error during migration", result.Error);
    });