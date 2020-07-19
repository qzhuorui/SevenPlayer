package com.qzr.sevenplayer.utils;

import android.os.Environment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.utils
 * @ClassName: StorageUtil
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/7/11 10:29
 */
public class StorageUtil {

    private static final String TAG = "StorageUtil";

    private static String getDirName() {
        return "SevenPlayer";
    }

    public static final ThreadLocal<SimpleDateFormat> spFormat = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyMMdd_HHmmss", Locale.getDefault());
        }
    };

    private static String getSDPath() {
        // 判断是否挂载
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        return Environment.getRootDirectory().getAbsolutePath();
    }

    public static boolean checkDirExist(String path) {
        File mDir = new File(path);
        if (!mDir.exists()) {
            return mDir.mkdirs();
        }
        return true;
    }

    public static File getOutPutImageFile() {
        String timestamp = spFormat.get().format(new Date());
        return new File(getSDPath() + "/" + getDirName() + "/image/", timestamp + ".jpg");
    }

    public static File getOutPutVedioFile() {
        String timestamp = spFormat.get().format(new Date());
        return new File(getSDPath() + "/" + getDirName() + "/video/",timestamp + ".mp4");
    }

}
