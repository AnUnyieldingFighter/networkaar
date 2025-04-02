package com.retrofits.net.common;

/**
 * Created by 郭敏 on 2018/3/7 0007.
 */

public interface ProgressListener {
    /**
     * 1：开始 2：进行中 3：完成 4：出错 5:停止下载
     *
     * @param url      下载的url
     * @param filePath 本地保存的path或者上传时的文件path
     * @param progress 已经下载或上传字节数
     * @param total    总字节数
     */
    void onProgress(int what, String url, String filePath,
                    long progress, long total);
}
