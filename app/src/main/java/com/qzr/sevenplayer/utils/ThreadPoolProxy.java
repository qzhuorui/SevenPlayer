package com.qzr.sevenplayer.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.utils
 * @ClassName: ThreadPoolProxy
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/7/11 9:58
 */
public class ThreadPoolProxy {

    private static final String TAG = "ThreadPoolProxy";

    private ThreadPoolExecutor mThreadPoolExecutor;
    private ScheduledThreadPoolExecutor mScheduledThreadPoolExecutor;

    private int mCorePoolSize;
    private int mMaxmumPoolSize;

    private Map<Runnable, ScheduledFuture> repeatTasks;

    public ThreadPoolProxy(int mCorePoolSize, int mMaxmumPoolSize) {
        this.mCorePoolSize = mCorePoolSize;
        this.mMaxmumPoolSize = mMaxmumPoolSize;
    }

    private void initThreadPoolExecutor() {
        if (mThreadPoolExecutor == null || mThreadPoolExecutor.isShutdown() || mThreadPoolExecutor.isTerminated()) {
            synchronized (ThreadPoolProxy.class) {
                if (mThreadPoolExecutor == null || mThreadPoolExecutor.isShutdown() || mThreadPoolExecutor.isTerminated()) {
                    long keepAliveTime = 3000;
                    TimeUnit timeUnit = TimeUnit.MILLISECONDS;
                    BlockingQueue<Runnable> workQueue = new LinkedBlockingDeque<>();
                    ThreadFactory threadFactory = new SimpleThreadFactory();
                    RejectedExecutionHandler handler = new ThreadPoolExecutor.AbortPolicy();

                    mThreadPoolExecutor = new ThreadPoolExecutor(mCorePoolSize, mMaxmumPoolSize, keepAliveTime, timeUnit, workQueue, threadFactory, handler);

                    if (mScheduledThreadPoolExecutor == null) {
                        mScheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(mCorePoolSize, threadFactory, handler);
                        repeatTasks = new HashMap<>(4);
                    }
                }
            }
        }
    }

    public void execute(Runnable task) {
        initThreadPoolExecutor();
        mThreadPoolExecutor.execute(task);
    }

    public void remove(Runnable task) {
        initThreadPoolExecutor();
        mThreadPoolExecutor.remove(task);
    }

    public void executeRepeatTask(Runnable task, int delay, int period, TimeUnit timeUnit) {
        initThreadPoolExecutor();
        ScheduledFuture future = mScheduledThreadPoolExecutor.scheduleAtFixedRate(task, delay, period, timeUnit);
        repeatTasks.put(task, future);
    }

    public void cancelRepeatTask(Runnable task) {
        if (task == null) {
            throw new RuntimeException("cancelRepeatTask task is null");
        }
        initThreadPoolExecutor();
        ScheduledFuture future = repeatTasks.get(task);
        if (future == null) {
            return;
        }
        future.cancel(true);
        repeatTasks.remove(task);
        mScheduledThreadPoolExecutor.remove(task);
    }

    class SimpleThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, TAG);
        }
    }
}
