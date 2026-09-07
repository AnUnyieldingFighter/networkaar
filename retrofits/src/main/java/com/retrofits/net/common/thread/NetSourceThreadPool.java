package com.retrofits.net.common.thread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class NetSourceThreadPool {

    private ExecutorService executor;

    private static final int MAX_NUM_POOL_SIZE = 3;

    // 直接创建单例，避免多个线程第一次调用时生成多个线程池。
    private static final NetSourceThreadPool INSTANCE = new NetSourceThreadPool();
    // 任务可能在不同线程提交或取消，使用线程安全的 Map。
    private final Map<String, Future> futures = new ConcurrentHashMap<>();

    private NetSourceThreadPool() {
        executor = createExecutor();
    }

    public static NetSourceThreadPool getInstance() {
        return INSTANCE;
    }

    public synchronized void execute(Runnable task) {
        getExecutor().execute(task);
    }

    //提交任务
    public synchronized Future<?> submit(String key, Runnable task) {
        Future<?> future = getExecutor().submit(task);
        futures.put(key, future);
        removeComplete();
        return future;
    }

    //获取在线程池的所有任务key
    public List<String> getTaskName() {
        return new ArrayList<>(futures.keySet());
    }

    //获取所的Future
    public HashMap<String, Future> getTaskAll() {
        // 返回副本，避免外部代码直接修改内部任务表。
        return new HashMap<>(futures);
    }

    //获取指定任务的Future
    public Future getTask(String key) {
        return futures.get(key);
    }

    //停止所有任务
    public void stopTaskAll() {
        for (Future future : futures.values()) {
            future.cancel(true);
        }
        futures.clear();
    }

    //停止指定任务
    public void stopTask(String key) {
        Future future = futures.remove(key);
        if (future == null) {
            return;
        }
        future.cancel(true);
    }

    //移除完成的任务
    private void removeComplete() {
        for (Map.Entry<String, Future> entry : futures.entrySet()) {
            Future future = entry.getValue();
            if (future.isDone()) {
                futures.remove(entry.getKey(), future);
            }
        }
    }

    //停止线程
    public synchronized void stop() {
        executor.shutdown();
        futures.clear();
    }

    public synchronized void stopNow() {
        executor.shutdownNow();
        futures.clear();
    }

    // stop/stopNow 后再次提交任务时，自动创建新的线程池。
    private ExecutorService getExecutor() {
        if (executor == null || executor.isShutdown()) {
            executor = createExecutor();
        }
        return executor;
    }

    private ExecutorService createExecutor() {
        PriorityThreadFactory threadFactory = new PriorityThreadFactory("http-data",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        return Executors.newFixedThreadPool(MAX_NUM_POOL_SIZE, threadFactory);
    }

}
