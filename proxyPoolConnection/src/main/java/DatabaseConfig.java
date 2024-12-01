import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class DatabaseConfig {

    private static HikariDataSource dataSource;

    public static void configure(String dbHost) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://" + dbHost + ":5432/votaciones");
        config.setUsername("postgres");
        config.setPassword("postgres");
        config.setMaximumPoolSize(10); // Tamaño máximo de  l pool
        config.setMinimumIdle(2); // Mínimo de conexiones inactivas
        config.setIdleTimeout(30000); // Tiempo de espera para liberar conexiones inactivas (ms)
        config.setMaxLifetime(1800000); // Tiempo máximo de vida de una conexión (ms)
        config.setConnectionTimeout(20000); // Tiempo máximo para obtener una conexión (ms)
        config.setConnectionTestQuery("SELECT 1");
        config.addDataSourceProperty("logger", "com.zaxxer.hikari.HikariLogger");
        config.addDataSourceProperty("logLevel", "TRACE");

        if (dataSource != null) {
            dataSource.close();
        }

        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;  
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}