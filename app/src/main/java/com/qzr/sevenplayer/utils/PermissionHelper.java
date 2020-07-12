package com.qzr.sevenplayer.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

import com.qzr.sevenplayer.interf.PermissionInterface;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.utils
 * @ClassName: PermissionHelper
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/7/4 10:06
 */
public class PermissionHelper {
    private Activity mActivity;
    private PermissionInterface mPermissionInterface;

    public PermissionHelper(Activity mActivity, PermissionInterface mPermissionInterface) {
        this.mActivity = mActivity;
        this.mPermissionInterface = mPermissionInterface;
    }

    public static boolean hasPermission(Context context, String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void requestPermission(String permission, int callBackCode) {
        if (hasPermission(mActivity, permission)) {
            mPermissionInterface.requestPermissionSuccess(callBackCode);
        } else {
            ActivityCompat.requestPermissions(mActivity, new String[]{permission}, callBackCode);
        }
    }

    //接管结果
    public void requestPermissionResult(int requestCode, String[] permission, int[] grantResults) {
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_GRANTED) {
                mPermissionInterface.requestPermissionSuccess(requestCode);
            } else {
                mPermissionInterface.requestPermissionFail(requestCode);
            }
        }
    }

}
