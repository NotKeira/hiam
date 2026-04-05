package uk.co.keirahopkins.hiam.gate.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GateAuthenticationExecutor {
    private final ExecutorService executor;

    public GateAuthenticationExecutor() {
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void submitAuthCheck(Runnable task) {
        executor.submit(task);
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
