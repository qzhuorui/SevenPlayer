package com.qzr.sevenplayer.utils;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.utils
 * @ClassName: ThreadPoolProxyFactory
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/7/11 9:58
 */
public class ThreadPoolProxyFactory {

    static ThreadPoolProxy mNormalThreadPoolProxy;
    static ThreadPoolProxy mDownLoadThreadPoolProxy;
    static ThreadPoolProxy mLongTimeUseThreadPoolProxy;

    /**
     * 得到普通线程池代理对象mNormalThreadPoolProxy
     */
    public static ThreadPoolProxy getNormalThreadPoolProxy() {
        if (mNormalThreadPoolProxy == null) {
            synchronized (ThreadPoolProxyFactory.class) {
                if (mNormalThreadPoolProxy == null) {
                    mNormalThreadPoolProxy = new ThreadPoolProxy(10, 10);
                }
            }
        }
        return mNormalThreadPoolProxy;
    }

    /**
     * 得到普通线程池代理对象mNormalThreadPoolProxy
     */
    public static ThreadPoolProxy getLongTimeUseThreadPoolProxy() {
        if (mLongTimeUseThreadPoolProxy == null) {
            synchronized (ThreadPoolProxyFactory.class) {
                if (mLongTimeUseThreadPoolProxy == null) {
                    mLongTimeUseThreadPoolProxy = new ThreadPoolProxy(2, 2);
                }
            }
        }
        return mLongTimeUseThreadPoolProxy;
    }

    /**
     * 得到下载线程池代理对象mDownLoadThreadPoolProxy
     */
    public static ThreadPoolProxy getDownLoadThreadPoolProxy() {
        if (mDownLoadThreadPoolProxy == null) {
            synchronized (ThreadPoolProxyFactory.class) {
                if (mDownLoadThreadPoolProxy == null) {
                    mDownLoadThreadPoolProxy = new ThreadPoolProxy(5, 5);
                }
            }
        }
        return mDownLoadThreadPoolProxy;
    }
}
