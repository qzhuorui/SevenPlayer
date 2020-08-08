package com.qzr.sevenplayer.utils;

import android.content.Context;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.utils
 * @ClassName: CrashHandler
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/8/1 12:38
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "CrashHandler";

    private DateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");

    private Context mContext;
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    private static CrashHandler _instance = new CrashHandler();

    public static CrashHandler getInstance() {
        return _instance;
    }

    public void init(Context context) {
        this.mContext = context;
        //系统默认UncaughtExceptionHandler
        this.mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        //设置该CrashHandler为系统默认的
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (!handleException(e) && mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(t, e);
        } else {
            Log.e(TAG, "CrashHandler: system exit");
        }
    }

    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        // 使用Toast来显示异常信息
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(mContext, "很抱歉，程序出现异常。", Toast.LENGTH_LONG)
                        .show();
                Looper.loop();
            }
        }.start();

        // 保存日志文件
        String str = saveCrashInfo2File(ex);

        return true;
    }

    private String saveCrashInfo2File(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        sb.append(getStackTraceString(ex));
        try {
            String time = formatter.format(new Date());

            String fileName = "CRS_" + time + ".txt";

            File cacheDir = new File(StorageUtil.getOutPutLogFile().getPath());
            if (!cacheDir.exists()) {
                if (!cacheDir.mkdirs()) {
                    return null;
                }
            }

            File filePath = new File(cacheDir + File.separator + fileName);

            FileOutputStream fos = new FileOutputStream(filePath);
            fos.write(sb.toString().getBytes());
            fos.close();

            return fileName;
        } catch (Exception e) {
            Log.e(TAG, "an error occured while writing file...", e);
        }
        return null;
    }

    private String getStackTraceString(Throwable ex) {
        if (ex == null) {
            return "";
        }

        Throwable t = ex;
        while (t != null) {
            if (t instanceof UnknownHostException) {
                return "";
            }
            t = t.getCause();
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }


}
