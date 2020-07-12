package com.qzr.sevenplayer.interf;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.interf
 * @ClassName: PermissionInterface
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/7/4 10:08
 */
public interface PermissionInterface {

    void requestPermissionSuccess(int callBackCode);

    void requestPermissionFail(int callBackCode);
}
