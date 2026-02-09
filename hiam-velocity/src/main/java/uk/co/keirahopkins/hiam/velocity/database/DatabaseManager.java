package uk.co.keirahopkins.hiam.velocity.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.keirahopkins.hiam.velocity.config.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    
    private final Config config;
    private final HikariDataSource dataSource;

    public DatabaseManager(Config config) {
        this.config = config;
        HikariConfig hikariConfig = new HikariConfig();
        
        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s",
                config.getDatabaseHost(),
                config.getDatabasePort(),
                config.getDatabaseName());

        StringBuilder params = new StringBuilder();
        if (config.getDatabaseSsl()) {
            params.append("ssl=true&sslmode=require");
        }

        String schema = config.getDatabaseSchema();
        if (schema != null && !schema.isBlank()) {
            if (params.length() > 0) {
                params.append("&");
            }
            params.append("currentSchema=").append(schema);
        }

        if (params.length() > 0) {
            jdbcUrl += "?" + params;
        }
        
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setDriverClassName("org.postgresql.Driver");
        hikariConfig.setUsername(config.getDatabaseUser());
        hikariConfig.setPassword(config.getDatabasePassword());
        hikariConfig.setMaximumPoolSize(config.getDatabasePoolSize());
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);
        hikariConfig.setPoolName("HelixIAM-Pool");
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        this.dataSource = new HikariDataSource(hikariConfig);
        logger.info("Database connection pool initialized");
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void runMigrations() throws SQLException, IOException {
        if (!config.getDatabaseInitDb()) {
            logger.info("Database migrations skipped (initDb=false)");
            return;
        }

        logger.info("Running database migrations...");
        
        String sql = loadSqlFromResource("/sql/001-init.sql");
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            String schema = config.getDatabaseSchema();
            if (schema != null && !schema.isBlank()) {
                if (!schema.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                    throw new SQLException("Invalid schema name: " + schema);
                }
                stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
                stmt.execute("SET search_path TO " + schema);
            }
            stmt.execute(sql);
            logger.info("Database migrations completed successfully");
        } catch (SQLException e) {
            logger.error("Failed to run database migrations", e);
            throw e;
        }
    }

    private String loadSqlFromResource(String resourcePath) throws IOException {
        InputStream is = getClass().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IOException("SQL resource not found: " + resourcePath);
        }
        
        StringBuilder sql = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sql.append(line).append("\n");
            }
        }
        
        return sql.toString();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }

    public boolean isConnected() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }
}
