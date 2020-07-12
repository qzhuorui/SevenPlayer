package com.qzr.sevenplayer.utils;

import android.app.Activity;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Size;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.utils
 * @ClassName: CameraUtil
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/7/4 11:02
 */
public class CameraUtil {

    /*
    根据支持的list获取最佳尺寸
     */
    public static Camera.Size getOptimalSize(List<Camera.Size> supportList, final int width, int height) {
        //camera的宽度是大于高度的。这里要保证expectW > expectH
        int expectWidth = Math.max(width, height);
        int expectHeight = Math.min(width, height);
        //根据宽度排列，这里的宽度就是最长的那一边
        Collections.sort(supportList, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size o1, Camera.Size o2) {
                if (o1.width > o2.width) {
                    return 1;
                } else if (o1.width < o2.width) {
                    return -1;
                }
                return 0;
            }
        });

        Camera.Size result = supportList.get(0);
        //判断存在宽或高相等的Size
        boolean widthOrHeight = false;
        for (Camera.Size size : supportList) {
            //宽高相同，直接返回
            if (size.width == expectWidth && size.height == expectHeight) {
                result = size;
                break;
            }
            //仅宽相同，计算高度最接近的Size
            if (size.width == expectWidth) {
                widthOrHeight = true;
                if (Math.abs(result.height - expectHeight) > Math.abs(size.height - expectHeight)) {
                    result = size;
                }
            }
            //高度相同，计算宽度最接近的
            else if (size.height == expectHeight) {
                widthOrHeight = true;
                if (Math.abs(result.width - expectWidth) > Math.abs(size.width - expectWidth)) {
                    result = size;
                }
            }
            //都没有相同的，则计算宽高最接近的
            else if (!widthOrHeight) {
                if (Math.abs(result.width - expectWidth) > Math.abs(size.width - expectWidth)
                        && Math.abs(result.height - expectHeight) > Math.abs(size.height - expectHeight)) {
                    result = size;
                }
            }
        }
        return result;
    }

    public static int getCameraPreviewOritation(Activity activity, int cameraId) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);
        int result;
        int degress = getRotation(activity);
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degress) % 360;
            result = (360 - result) % 360;
        } else {
            result = (cameraInfo.orientation - degress + 360) % 360;
        }
        return result;
    }

    private static int getRotation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degress = 0;
        switch (rotation) {
            case 0: {
                degress = 0;
                break;
            }
            case 90: {
                degress = 90;
                break;
            }
            case 180: {
                degress = 180;
                break;
            }
            case 270: {
                degress = 270;
                break;
            }
        }
        return degress;
    }

    /*
    textview 点击坐标转为 camera对焦坐标
     */
    public static Point convertToCameraPoint(Size screenSize, Point focusPoint) {
        int newX = focusPoint.y * 2000/screenSize.getHeight() - 1000;
        int newY = -focusPoint.x * 2000/screenSize.getWidth() + 1000;
        return new Point(newX, newY);
    }

    public static Rect convertToCameraRect(Point centerPoint, int radius) {
        int left = limit(centerPoint.x - radius, 1000, -1000);
        int right = limit(centerPoint.x + radius, 1000, -1000);
        int top = limit(centerPoint.y - radius, 1000, -1000);
        int bottom = limit(centerPoint.y + radius, 1000, -1000);
        return new Rect(left, top, right, bottom);
    }

    private static int limit(int s, int max, int min) {
        if (s > max) {
            return max;
        }
        if (s < min) {
            return min;
        }
        return s;
    }

}
