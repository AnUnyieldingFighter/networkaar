package com.retrofits.net.common.thread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class NetSourceThreadPool {

    private ExecutorService executor;

    private final int MAX_NUM_POOL_SIZE = 3;

    private static NetSourceThreadPool instance;
    private HashMap<String, Future> futures = new HashMap<>();

    private NetSourceThreadPool() {
        PriorityThreadFactory threadFactory = new PriorityThreadFactory("http-data",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        executor = Executors.newFixedThreadPool(MAX_NUM_POOL_SIZE, threadFactory);
    }

    public static NetSourceThreadPool getInstance() {
        if (instance == null) {
            instance = new NetSourceThreadPool();
        }
        return instance;
    }

    public void execute(Runnable task) {
        if (executor != null && !executor.isShutdown()) {
            executor.execute(task);
        }
    }

    //提交任务
    public Future<?> submit(String key, Runnable task) {
        Future<?> future = null;
        if (executor != null && !executor.isShutdown()) {
            future = executor.submit(task);
            futures.put(key, future);
        }
        removeComplete();
        return future;
    }

    //获取在线程池的所有任务key
    public List<String> getTaskName() {
        Set<String> kyes = futures.keySet();
        List<String> names = new ArrayList<>();
        for (String key : kyes) {
            names.add(key);
        }
        return names;
    }

    //获取所的Future
    public HashMap<String, Future> getTaskAll() {
        return futures;
    }

    //获取指定任务的Future
    public Future getTask(String key) {
        Future future = futures.get(key);
        return future;
    }

    //停止所有任务
    public void stopTaskAll() {
        Set<String> kyes = futures.keySet();
        for (String key : kyes) {
            Future future = futures.get(key);
            if (future == null) {
                continue;
            }
            future.cancel(true);
        }
        futures.clear();
    }

    //停止指定任务
    public void stopTask(String key) {
        Future future = futures.get(key);
        if (future == null) {
            return;
        }
        future.cancel(true);
        futures.remove(key);
    }

    //移除完成的任务
    private void removeComplete() {
        Iterator it = futures.keySet().iterator();
        while (it.hasNext()) {
            Object key = it.next();
            Future future = futures.get(key);
            if (future == null || future.isDone()) {
                it.remove();
                continue;
            }
        }
    }

    //停止线程
    public void stop() {
        if (executor == null) {
            return;
        }
        executor.shutdown();
        futures.clear();
        executor = null;
    }

    public void stopNow() {
        if (executor == null) {
            return;
        }
        executor.shutdownNow();
        futures.clear();
        executor = null;
    }

}
