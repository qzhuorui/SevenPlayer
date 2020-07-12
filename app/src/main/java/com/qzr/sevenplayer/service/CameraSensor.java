package com.qzr.sevenplayer.service;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.service
 * @ClassName: CameraSensor
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/7/5 13:38
 */
public class CameraSensor implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mSensor;

    private int lastX, lastY, lastZ;

    private CameraSensorListener mCameraSensorListener;

    public CameraSensor(Context context) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mCameraSensorListener = null;
        reSetParams();
    }

    private void reSetParams() {
        lastX = 0;
        lastY = 0;
        lastZ = 0;
    }

    public void startCameraSensor() {
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        reSetParams();
    }

    public void stopCameraSensor() {
        mSensorManager.unregisterListener(this, mSensor);
    }

    /*
    方向改变时
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == null) {
            return;
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            int x = (int) event.values[0];
            int y = (int) event.values[1];
            int z = (int) event.values[2];

            int px = Math.abs(lastX - x);
            int py = Math.abs(lastY - y);
            int pz = Math.abs(lastZ - z);

            lastX = x;
            lastY = y;
            lastZ = z;

            if (px > 2.5 || py > 2.5 || pz > 2.5) {
                if (mCameraSensorListener != null) {
                    mCameraSensorListener.onRock();
                }
            }
        }
    }

    /*
    精度改变
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void setCameraSensorListener(CameraSensorListener listener) {
        mCameraSensorListener = listener;
    }

    public interface CameraSensorListener {
        void onRock();
    }
}
