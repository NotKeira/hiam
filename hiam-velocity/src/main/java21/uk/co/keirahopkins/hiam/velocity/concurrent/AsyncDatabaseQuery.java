package uk.co.keirahopkins.hiam.velocity.concurrent;

import uk.co.keirahopkins.hiam.velocity.database.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class AsyncDatabaseQuery {
    private final VirtualThreadExecutor executor;
    private final DatabaseManager databaseManager;

    public AsyncDatabaseQuery(DatabaseManager databaseManager) {
        this.executor = new VirtualThreadExecutor();
        this.databaseManager = databaseManager;
    }

    public <T> CompletableFuture<T> executeQuery(Function<Connection, T> query) {
        return executor.executeAsync(() -> {
            try (Connection connection = databaseManager.getConnection()) {
                return query.apply(connection);
            } catch (SQLException e) {
                throw new RuntimeException("Database query failed", e);
            }
        });
    }

    public CompletableFuture<Void> executeUpdate(Function<Connection, Void> update) {
        return executor.executeAsync(() -> {
            try (Connection connection = databaseManager.getConnection()) {
                return update.apply(connection);
            } catch (SQLException e) {
                throw new RuntimeException("Database update failed", e);
            }
        });
    }

    public void shutdown() {
        executor.close();
    }
}
