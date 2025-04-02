package com.retrofits.downsys;

import android.app.DownloadManager;

/**
 * Created by guom on 2019/1/7.
 */

public class SysDownBean {
    //下载id
    public long id;
    //标题
    public String title;
    //描述
    public String describe;
    //存储下载文件的Uri
    public String uri;
    //文件路径
    public String fileName;
    //下载url
    public String url;
    //已下载
    public int downloadProgress;
    //下载总长度
    public int downloadSize;
    //状态
    public int status;
    public String mediaType;

    //获取下载描述
    public String getStateHint() {
        String hint = "";
        switch (status) {
            case DownloadManager.STATUS_PAUSED:
                //下载暂停
                hint = "下载暂停";
                break;
            case DownloadManager.STATUS_PENDING:
                //下载延迟
                hint = "下载延迟";
                break;
            case DownloadManager.STATUS_RUNNING:
                //正在下载
                hint = "正在下载";
                break;
            case DownloadManager.STATUS_SUCCESSFUL:
                //下载完成
                hint = "下载完成";
                break;
            case DownloadManager.STATUS_FAILED:
                //下载失败
                hint = "下载失败";
                break;

        }
        return hint;
    }
}
