package com.qzr.sevenplayer.base;

import android.app.Application;
import android.content.Context;

import com.qzr.sevenplayer.utils.CrashHandler;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.base
 * @ClassName: BaseApplication
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/7/5 10:36
 */
public class BaseApplication extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        CrashHandler.getInstance().init(mContext);
    }

    public static Context getContext() {
        return mContext;
    }

}
