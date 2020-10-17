package com.qzr.sevenplayer.encode;

import android.util.Log;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class QueueArray {
    byte[] buffer; //对象数组，
    int front;  //队首下标
    int rear;   //队尾下标

    public Lock lock = new ReentrantLock();

    String name;

    public QueueArray(int size, String name) {
        buffer = new byte[size];
        front = 0;
        rear = 0;
        this.name = name;
    }

    /**
     * 将一个对象追加到队列尾部
     *
     * @param obj 对象
     * @return 队列满时返回false, 否则返回true
     */
    public boolean enqueue(byte[] obj, int len) {
        lock.lock();

        if (buffer.length == 0 || len >= (buffer.length - getCurrentSize())) {
            lock.unlock();
            return false;
        }
        int pos = buffer.length - rear;
        if (pos < len) {
            System.arraycopy(obj, 0, buffer, rear, pos);
            System.arraycopy(obj, pos, buffer, 0, len - pos);
        } else {
            System.arraycopy(obj, 0, buffer, rear, len);
        }

        rear = (rear + len) % buffer.length;
        lock.unlock();
        return true;

    }

    /**
     * 队列头部的第一个对象出队
     *
     * @return 出队的对象，队列空时返回null
     */
    public byte[] dequeue(int len) {
        lock.lock();
        if (rear == front) {
            lock.unlock();
            return null;
        }
        if (len > getCurrentSize()) {
            lock.unlock();
            return null;
        }
        byte[] tempBuffer = new byte[len];

//        long forIn = System.currentTimeMillis();
//
        int pos = buffer.length - front;
        if (pos < len) {

            System.arraycopy(buffer, front, tempBuffer, 0, pos);
            System.arraycopy(buffer, 0, tempBuffer, pos, len - pos);
        } else {
            System.arraycopy(buffer, front, tempBuffer, 0, len);
        }
//        long t = (System.currentTimeMillis() - forIn);
//        if(t>20) {
//            Log.e("", "for delay :" + t);
//        }

        if (tempBuffer.length == 0) {
            Log.e("QueueArray", "front=" + front + " rear=" + rear + " length=" + buffer.length);
        }

        front = (front + len) % buffer.length;

        lock.unlock();

        return tempBuffer;

    }

    private int getCurrentSize() {
        return (rear + buffer.length - front) % buffer.length;
    }

    public void clearQueue() {
        lock.lock();
        rear = 0;
        front = 0;
        lock.unlock();
    }


}
