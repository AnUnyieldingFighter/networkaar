package com.retrofits.net.common;

/**
 * Created by Administrator on 2016/1/15.
 */
public interface RequestBack {
    /**
     * Http请求
     * activity回调
     *
     * @param what
     * @param obj
     * @param msg
     */
    void onBack(int what, Object obj, String msg, String other);

    /**
     * 进度
     *
     * @param what          1：开始 2：进行中 3：完成 4：出错
     * @param url           下载的url
     * @param filePath      本地保存的path或者上传时的文件path
     * @param currentLength 进度
     * @param totalLength   总大小
     */
    void onBackProgress(int what, String url, String filePath,
                        long currentLength, long totalLength);
}
