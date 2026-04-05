package uk.co.keirahopkins.hiam.paper.async;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class ConcurrentCommandExecutor {
    private final ExecutorService executor;

    public ConcurrentCommandExecutor() {
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public <T> CompletableFuture<T> execute(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executor);
    }

    public CompletableFuture<Void> execute(Runnable task) {
        return CompletableFuture.runAsync(task, executor);
    }

    public void shutdown() {
        executor.shutdown();
    }
}
