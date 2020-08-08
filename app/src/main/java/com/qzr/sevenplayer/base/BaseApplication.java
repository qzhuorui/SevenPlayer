package com.qzr.sevenplayer.base;

import android.app.Application;

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

    private static BaseApplication thisApp;

    @Override
    public void onCreate() {
        super.onCreate();
        thisApp = this;
        CrashHandler.getInstance().init(this);
    }

    public static BaseApplication getInstance() {
        return thisApp;
    }

}
