package com.qzr.sevenplayer.utils;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.service
 * @ClassName: HandlerProcess
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/7/5 12:52
 */
public class HandlerProcess {
    private static final String TAG = "HandlerProcess";

    HandlerThread mThread;

    private Handler mBgHandler;
    private Handler mMainHandler;

    public static HandlerProcess getInstance() {
        return HandlerProcessHolder.handlerProcess;
    }

    private static class HandlerProcessHolder {
        private static HandlerProcess handlerProcess = new HandlerProcess();
    }

    private HandlerProcess() {
        mThread = new HandlerThread(TAG, Process.THREAD_PRIORITY_MORE_FAVORABLE);
        mThread.start();

        mBgHandler = new Handler(mThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.obj instanceof ObjectInfo) {
                    ObjectInfo info = (ObjectInfo) msg.obj;
                    info.callback.handleMsg(msg.what, info.obj);
                }
            }
        };

        mMainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.obj instanceof ObjectInfo) {
                    ObjectInfo info = (ObjectInfo) msg.obj;
                    info.callback.handleMsg(msg.what, info.obj);
                }
            }
        };
    }

    public void post(int what, long delay, HandlerCallback callback) {
        post(what, null, delay, callback);
    }

    public void post(int what, Object obj, long delay, HandlerCallback callback) {
        mMainHandler.removeMessages(what);
        if (callback == null) {
            mMainHandler.sendMessageDelayed(mMainHandler.obtainMessage(what, obj), delay);
        } else {
            mMainHandler.sendMessageDelayed(mMainHandler.obtainMessage(what, new ObjectInfo(mMainHandler, obj, callback)), delay);
        }
    }

    public void postBG(int what, long delay, HandlerCallback callback) {
        postBG(what, null, delay, callback);
    }

    public void postBG(int what, Object obj, long delay, HandlerCallback callback) {
        mBgHandler.removeMessages(what);
        if (callback == null) {
            mBgHandler.sendMessageDelayed(mBgHandler.obtainMessage(what, obj), delay);
        } else {
            mBgHandler.sendMessageDelayed(mBgHandler.obtainMessage(what, new ObjectInfo(mBgHandler, obj, callback)), delay);
        }
    }

    public void postDelayedOnMain(Runnable r, int delayMs) {
        mMainHandler.removeCallbacks(r);
        mMainHandler.postDelayed(r, delayMs);
    }

    public void postDelayedOnBg(Runnable r, int delayMs) {
        mBgHandler.removeCallbacks(r);
        mBgHandler.postDelayed(r, delayMs);
    }

    public void removeMainRunnable(Runnable r) {
        mMainHandler.removeCallbacks(r);
    }

    public void removeBgRunnable(Runnable r) {
        mBgHandler.removeCallbacks(r);
    }

    public boolean hasMainMessage(int what) {
        return mMainHandler.hasMessages(what);
    }

    public void removeMainMessage(int what) {
        mMainHandler.removeMessages(what);
    }

    public void removeBgMessage(int what) {
        mBgHandler.removeMessages(what);
    }

    private static class ObjectInfo {
        Handler handler;
        Object obj;
        HandlerCallback callback;

        ObjectInfo(Handler handler, Object obj) {
            this.handler = handler;
            this.obj = obj;
        }

        ObjectInfo(Handler handler, Object obj, HandlerCallback callback) {
            this.handler = handler;
            this.obj = obj;
            this.callback = callback;
        }
    }

    public interface HandlerCallback {
        void handleMsg(int what, Object o);
    }
}
