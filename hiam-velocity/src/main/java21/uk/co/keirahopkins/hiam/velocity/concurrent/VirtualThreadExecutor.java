package uk.co.keirahopkins.hiam.velocity.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class VirtualThreadExecutor implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(VirtualThreadExecutor.class);
    
    private final ExecutorService executor;

    public VirtualThreadExecutor() {
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        logger.info("Set up virtual thread executor for Java 21+");
    }

    public <T> CompletableFuture<T> executeAsync(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executor);
    }

    public CompletableFuture<Void> executeAsync(Runnable task) {
        return CompletableFuture.runAsync(task, executor);
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
