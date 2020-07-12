package com.qzr.sevenplayer.base;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import butterknife.ButterKnife;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.base
 * @ClassName: BaseActivity
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/7/4 12:44
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected Context mContext;

    public abstract void beforeSetContentView();

    public abstract int getLayoutId();

    public abstract void initView();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //竖屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mContext = this;

        beforeSetContentView();

        setContentView(getLayoutId());

        ButterKnife.bind(this);

        initView();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
            }
        }

    }

}
