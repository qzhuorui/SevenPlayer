package com.qzr.sevenplayer.manager;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.manager
 * @ClassName: QzrMicManager
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/7/12 12:59
 */
public class QzrMicManager {

    private static final String TAG = "QzrMicManager";

    private boolean mMicStarted = false;

    private CopyOnWriteArraySet<OnPcmDataGetListener> pcmDataListeners;

    public static QzrMicManager getInstance() {
        return QzrMicManagerHolder.qzrMicManager;
    }

    private static class QzrMicManagerHolder {
        private static QzrMicManager qzrMicManager = new QzrMicManager();
    }

    public QzrMicManager() {

    }

    public void startMic() {
        if (mMicStarted) {
            return;
        }
        pcmDataListeners = new CopyOnWriteArraySet<>();



    }

    public synchronized void addPcmDataGetCallback(OnPcmDataGetListener pcmDataGetListener) {
        if (pcmDataListeners != null) {
            pcmDataListeners.add(pcmDataGetListener);
        }
    }

    public synchronized void removePcmDataGetCallback(OnPcmDataGetListener pcmDataGetListener) {
        if (pcmDataListeners != null && pcmDataGetListener != null) {
            pcmDataListeners.remove(pcmDataGetListener);
        }
    }

    public interface OnPcmDataGetListener {
        public void feedPcmData(int len, byte[] data);
    }

}
