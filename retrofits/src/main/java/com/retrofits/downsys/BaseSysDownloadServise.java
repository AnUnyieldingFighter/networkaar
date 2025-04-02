package com.retrofits.downsys;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.retrofits.utiles.Json;
import com.retrofits.utiles.RLog;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


/**
 * 系统的下载服务
 * Created by guom on 2019/1/4.
 */

abstract class BaseSysDownloadServise extends Service {

    private DownloadManager manager;
    private DownloadChangeObserver downloadObserver;
    private ArrayList<String> downloadIds = new ArrayList();

    //注册监听
    protected void register() {
        downloadObserver = new DownloadChangeObserver();
        downloadObserver.registerContentObserver();
        downLoadBroadcast = new DownLoadBroadcast();
        downLoadBroadcast.registerReceiver(this);
    }

    //解除注册
    protected void unRegister() {
        downLoadHandler.removeCallbacksAndMessages(null);
        if (downloadObserver != null) {
            downloadObserver.unregisterContentObserver();
        }
        if (downLoadBroadcast != null) {
            downLoadBroadcast.unregisterReceiver(this);
        }
    }

    protected DownloadManager getDownloadManager() {
        if (manager == null) {
            manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        }
        return manager;
    }

    protected void onDownloads(String url, String fileName, String title, String description, String mimeType) {
        //查询下载
        SysDownBean bean = onDownloadCheck(url);
        if (bean == null) {
            //去下载
            onDownload(url, fileName, title, description, mimeType);
            return;
        }
        switch (bean.status) {
            case DownloadManager.STATUS_RUNNING:
                //正在下载
                return;
            case DownloadManager.STATUS_SUCCESSFUL:
                //下载完成
                File file = new File(bean.fileName);
                if (!file.exists()) {
                    break;
                }
                Message msg = downLoadHandler.obtainMessage(2);
                msg.obj = bean;
                downLoadHandler.sendMessageDelayed(msg, 1 * 500);
                return;
            default:
                getDownloadManager().remove(bean.id);
                break;
        }
        //去下载
        onDownload(url, fileName, title, description, mimeType);
    }

    //查询下载
    protected SysDownBean onDownloadCheck(String downloadUrl) {
        return onDownloadCheck(downloadUrl, -100);
    }

    protected SysDownBean onDownloadCheck(long downloadId) {
        return onDownloadCheck("-100", downloadId);
    }

    //查询数据库下载数据
    protected SysDownBean onDownloadCheck(String downloadUrl, long downloadId) {
        DownloadManager.Query query = new DownloadManager.Query();

        Cursor cursor = getDownloadManager().query(query);
        if (cursor == null) {
            e("onDownloadCheck", "没有查询到");
            return null;
        }
        SysDownBean downloadBean = null;
        while (cursor.moveToNext()) {
            SysDownBean bean = onReadCursor(cursor);
            if (downloadUrl.equals(bean.url) || downloadId == bean.id) {
                downloadBean = bean;
                break;
            }
        }
        cursor.close();
        return downloadBean;
    }

    protected SysDownBean onReadCursor(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID));
        //标题
        String title = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE));
        //描述
        String describe = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION));
        //存储下载文件的Uri
        String uri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
        //
        String fileName = null;
        if (!TextUtils.isEmpty(uri)) {
            fileName = Uri.parse(uri).getPath();
        }
        String url = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI));
        int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
        int downloadProgress = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
        int downloadSize = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
        String mediaType = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE));
        //
        SysDownBean downloadBean = new SysDownBean();
        downloadBean.id = id;
        //标题
        downloadBean.title = title;
        downloadBean.mediaType = mediaType;
        //描述
        downloadBean.describe = describe;
        //存储下载文件的Uri
        downloadBean.uri = uri;
        downloadBean.fileName = fileName;
        //下载url
        downloadBean.url = url;
        downloadBean.downloadProgress = downloadProgress;
        downloadBean.downloadSize = downloadSize;
        downloadBean.status = status;
        e("下载", "id=" + id + " describe" + describe + " uri=" + uri + " fileName=" + fileName +
                " url=" + url + " hint:" + downloadBean.getStateHint());
        return downloadBean;

    }

    /**
     * @param url         下载url
     * @param fileName    完整的文件名称（绝对路径，带后缀：.apk,.mp3,.png...）
     * @param title       通知标题
     * @param description 下载描述
     * @param mimeType    文件类型
     */
    private void onDownload(String url, String fileName, String title,
                            String description, String mimeType) {
        //
        DownloadManager.Request request = getRequest(url, fileName, title,
                description, mimeType);
        long downloadId = getDownloadManager().enqueue(request);
        downloadIds.add(String.valueOf(downloadId));
    }

    //获取下载请求头
    protected DownloadManager.Request getRequest(String url, String fileName, String title,
                                                 String description, String mimeType) {
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        // 设置通知的标题和描述
        request.setTitle(title);
        request.setDescription(description);
        //指定该下载文件为APK文件
        request.setMimeType(mimeType);
        //设置下载文件的保存位置
        File saveFile = new File(fileName);
        uri = Uri.fromFile(saveFile);
        request.setDestinationUri(uri);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
        request.setVisibleInDownloadsUi(true);  //显示下载界面
        request.allowScanningByMediaScanner();  //准许被系统扫描到
        return request;
    }

    private DownLoadBroadcast downLoadBroadcast;
    private OnProgressListener progressListener;
    //
    private DownLoadHandler downLoadHandler = new DownLoadHandler();

    //下载ui刷新
    class DownLoadHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            handleMessageUi(msg);
        }
    }

    protected void handleMessageUi(Message msg) {
        SysDownBean bean = (SysDownBean) msg.obj;
        switch (msg.what) {
            case 1:
                //下载中
                float progress = bean.downloadProgress;
                float size = bean.downloadSize;
                float p = (progress / size) * 100;
                if (progressListener == null) {
                    return;
                }
                progressListener.onProgress(bean, p, 1);
                break;
            case 2:
                //下载完成
                e("下载完成", Json.obj2Json(bean));
                downloadIds.remove(String.valueOf(bean.id));
                if (progressListener != null) {
                    progressListener.onProgress(bean, 100, 0);
                }
                //
                onDownloadComplete(bean);
                break;
        }
    }

    //下载完成
    protected void onDownloadComplete(SysDownBean bean) {

    }

    //监听下载进度
    class DownloadChangeObserver extends ContentObserver {
        private ScheduledExecutorService scheduledExecutorService;
        private RunnableQuery progressRunnable = new RunnableQuery();
        private boolean isRegister;

        private DownloadChangeObserver() {
            super(downLoadHandler);
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        }

        /**
         * 当所监听的Uri发生改变时，就会回调此方法
         *
         * @param selfChange 此值意义不大, 一般情况下该回调值false
         */
        @Override
        public void onChange(boolean selfChange) {
            //在子线程中查询
            e("onChange", "下载进度查询");
            if (scheduledExecutorService.isShutdown()) {
                e("onChange", "scheduledExecutorService停止");
                return;
            }
            scheduledExecutorService.execute(progressRunnable);
        }

        private void registerContentObserver() {
            if (isRegister) {
                return;
            }
            isRegister = true;
            Uri uri = Uri.parse("content://downloads/my_downloads");
            getContentResolver().registerContentObserver(uri, true, this);

        }

        private void unregisterContentObserver() {
            if (!isRegister) {
                return;
            }
            onClose();
            isRegister = false;
            getContentResolver().unregisterContentObserver(this);
        }

        private void onClose() {
            if (scheduledExecutorService.isShutdown()) {
                return;
            }
            scheduledExecutorService.shutdown();
        }

    }

    class RunnableQuery implements Runnable {


        @Override
        public void run() {
            onDownloadQuery();
        }

        /**
         * 通过query查询下载状态，包括已下载数据大小，总大小，下载状态
         *
         * @return
         */
        private void onDownloadQuery() {
            int size = downloadIds.size();
            if (size == 0) {
                return;
            }
            long[] ids = new long[size];
            for (int i = 0; i < size; i++) {
                String id = downloadIds.get(i);
                ids[i] = getStringToLong(id, -100);
            }
            DownloadManager.Query query = new DownloadManager.Query()
                    .setFilterById(ids);
            Cursor cursor = getDownloadManager().query(query);
            if (cursor == null) {
                e("onDownloadQuery", "onDownloadQuery查询失败");
                return;
            }
            while (cursor.moveToNext()) {
                SysDownBean bean = onReadCursor(cursor);
                Message msg = downLoadHandler.obtainMessage(1);
                msg.obj = bean;
                downLoadHandler.sendMessage(msg);
            }
            cursor.close();
        }
    }

    private long getStringToLong(String number, long defaultInt) {
        if (TextUtils.isEmpty(number)) {
            return defaultInt;
        }
        try {
            return Long.parseLong(number);
        } catch (Exception e) {
            return defaultInt;
        }
    }

    //接受下载完成广播
    class DownLoadBroadcast extends BroadcastReceiver {
        private boolean isRegister;

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // 获取下载完成对应的下载ID, 这里下载完成指的不是下载成功, 下载失败也算是下载完成,
            long downId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
            e("DownLoadBroadcast", "接受下载完成广播" + action + " id="
                    + downId);
            switch (action) {
                case DownloadManager.ACTION_DOWNLOAD_COMPLETE:
                    //完成
                    if (!downloadIds.contains(String.valueOf(downId))) {
                        return;
                    }
                    SysDownBean bean = onDownloadCheck(downId);
                    //
                    Message msg = downLoadHandler.obtainMessage(2);
                    msg.obj = bean;
                    downLoadHandler.sendMessageDelayed(msg, 1 * 500);
                    break;
                case DownloadManager.ACTION_NOTIFICATION_CLICKED:

                    break;
            }
        }

        public void registerReceiver(Context context) {
            if (isRegister) {
                return;
            }
            //try catch 安全扫描用
            try {
                isRegister = true;
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
                intentFilter.addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED);
                context.registerReceiver(this, intentFilter);
            } catch (Exception e) {
                e.printStackTrace();
                RLog.e("registerReceiver", e.getMessage());
            }
        }

        public void unregisterReceiver(Context context) {
            if (!isRegister) {
                return;
            }
            isRegister = false;
            context.unregisterReceiver(this);
        }

    }


    /**
     * @param progressListener
     */
    public void setOnProgressListener(OnProgressListener progressListener) {
        this.progressListener = progressListener;
    }


    public interface OnProgressListener {
        /**
         * @param bean     实例
         * @param progress 下载进度 百分数
         * @param state    0完成 1下载中 2下载出错
         */
        void onProgress(SysDownBean bean, float progress, int state);

    }

    private void e(String tag, String value) {
        RLog.e(tag, value);
    }
}

