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

    public int getValidSize() {
        lock.lock();
        int value = 0;
        if (buffer.length == 0) {
            value = 0;
        } else {
            value = (rear + buffer.length - front) % buffer.length;
        }
        lock.unlock();
        return value;

    }

    public boolean isIkeyFrame(int pos) {
        lock.lock();
        if (getCurrentSize() >= pos) {
            if ((buffer[pos] & 0x1f) == 0x05) {
                lock.unlock();
                return true;
            } else {
                lock.unlock();
                return false;
            }
        } else {
            lock.unlock();
            return true;//当关键帧处理
        }

    }

    //编码
    public byte[] dequeueFrame() {
        int tmp = 0;
        int tmp2, tmp3, tmp4, tmp5, tmp6;
        if (getValidSize() <= 4) {
            return null;
        }
        int pos = 0;
        while (pos < getCurrentSize() - 6) {
            tmp = (pos + front) % buffer.length;
            tmp2 = (pos + front + 1) % buffer.length;
            tmp3 = (pos + front + 2) % buffer.length;
            tmp4 = (pos + front + 3) % buffer.length;
            tmp5 = (pos + front + 4) % buffer.length;
            tmp6 = (pos + front + 5) % buffer.length;
            if (buffer[tmp] == 0x0 && 0x0 == buffer[tmp2] && buffer[tmp3] == 0x0 && 0x01 == buffer[tmp4]) {
                if (pos == 0) {
                    pos = 4;
                    continue;
                }

                if (((byte) (buffer[tmp5] & 0x1f) == 5 || (byte) (buffer[tmp5] & 0x1f) == 1)) {

                    if (buffer[tmp6] < 0) {

                        return dequeue(pos);
                    }
                } else {
                    return dequeue(pos);
                }
            }
            if (buffer[tmp] == 0x0 && 0x0 == buffer[tmp2] && buffer[tmp3] == 0x01) {
                if (pos == 0) {
                    pos = 3;
                    continue;
                }

                if (((byte) (buffer[tmp4] & 0x1f) == 5 || (byte) (buffer[tmp4] & 0x1f) == 1)) {

                    if (buffer[tmp5] < 0) {
                        return dequeue(pos);
                    }
                } else {
                    return dequeue(pos);
                }

            }

            pos++;

        }
        return null;

    }


    public void clearQueue() {
        lock.lock();
        rear = 0;
        front = 0;
        lock.unlock();
    }


}
