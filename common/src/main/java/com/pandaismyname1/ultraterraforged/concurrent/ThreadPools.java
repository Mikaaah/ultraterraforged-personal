package com.pandaismyname1.ultraterraforged.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/** Worldgen executor; bootstraps without platform config access for common tests. */
public class ThreadPools {
    private static final int DEFAULT_WORKERS = Math.max(2, Math.min(8, availableProcessors() - 1));
    public static final ThreadPoolExecutor WORLD_GEN = (ThreadPoolExecutor) Executors.newFixedThreadPool(
        DEFAULT_WORKERS, daemonFactory("TNKTerraForged-WorldGen"));

    /** Apply the validated config after the loader/platform and generator are ready. */
    public static synchronized void configure(int requested) {
        int desired = Math.max(1, requested);
        int core = WORLD_GEN.getCorePoolSize();
        if (desired == core) return;
        if (desired > core) {
            WORLD_GEN.setMaximumPoolSize(desired);
            WORLD_GEN.setCorePoolSize(desired);
        } else {
            WORLD_GEN.setCorePoolSize(desired);
            WORLD_GEN.setMaximumPoolSize(desired);
        }
    }

    public static int availableProcessors() {
        return Math.max(2, Runtime.getRuntime().availableProcessors());
    }

    public static ThreadFactory daemonFactory(String name) {
        AtomicInteger counter = new AtomicInteger();
        return (runnable) -> {
            Thread thread = new Thread(runnable, name + "-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }
}
