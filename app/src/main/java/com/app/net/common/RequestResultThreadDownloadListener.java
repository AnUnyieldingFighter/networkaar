package com.app.net.common;

import android.os.Handler;
import android.telephony.SignalStrength;

import com.retrofits.net.manager.BaseManager;
import com.retrofits.net.manager.TaskResultThreadListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * 下载
 * Created by Administrator on 2017/6/14.
 */

public class RequestResultThreadDownloadListener extends TaskResultThreadListener<ResponseBody> {
    private String url;
    private String filePath;

    public RequestResultThreadDownloadListener(BaseManager baseManager, Call<ResponseBody> call) {
        super(call);
        this.baseManager = baseManager;
    }

    public RequestResultThreadDownloadListener(BaseManager baseManager, Call<ResponseBody> call, Object reqObj) {
        super(call, reqObj);
        this.baseManager = baseManager;
    }

    public RequestResultThreadDownloadListener(BaseManager baseManager, Call<ResponseBody> call, String other, Object reqObj) {
        super(call, other, reqObj);
        this.baseManager = baseManager;
    }


    public void setDownloadFile(String url, String filePath) {
        this.url = url;
        this.filePath = filePath;
    }

    //1正在下载 2:下载完成
    private int downloadType;
    //true 停止下载
    private boolean isStop;

    public void onStopDownload() {
        onStop();
    }

    //停止下载文件
    @Override
    protected void onStop() {
        super.onStop();
        if (isStop) {
            return;
        }
        isStop = true;
        if (downloadType == 2) {
            return;
        }
        if (downloadType == 0) {
            onStopListener();
        }
    }

    private void onStopListener() {
        BaseManager.Progress listener = baseManager.getProgress();
        Handler h = baseManager.getHandleCall();
        //1：开始 2：进行中 3：完成 4：出错 5 停止
        onBack(h, listener, 5, url, filePath, currentLength, totalLength, "stop");

    }

    @Override
    public void onRequestResult(Call<ResponseBody> call, Response<ResponseBody> response) {
        onDownloadFile(response);
    }

    long currentLength = 0;
    long totalLength = 0;

    //下载文件
    protected void onDownloadFile(Response<ResponseBody> response) {
        if (isStop) {
            return;
        }
        downloadType = 1;
        currentLength = 0;
        totalLength = 0;
        String path = filePath + ".l";
        File file = new File(path);
        InputStream is = response.body().byteStream(); //获取下载输入流
        totalLength = response.body().contentLength();
        BaseManager.Progress listener = baseManager.getProgress(false);
        Handler h = baseManager.getHandleCall();
        onBack(h, listener, 1, url, filePath, 0, totalLength, "start");

        boolean isContinue = false;
        if (file.exists() && file.length() > 0) {
            //是断点下载
            isContinue = true;
        }
        try {
            //输出流
            OutputStream os = new FileOutputStream(file, isContinue);
            int len;
            byte[] buff = new byte[1024];
            while ((len = is.read(buff)) != -1) {
                if (isStop) {
                    onStopListener();
                    break;
                }
                os.write(buff, 0, len);
                currentLength += len;

                //当百分比为100时下载结束，调用结束回调，并传出下载后的本地路径
                if (currentLength == totalLength) {
                    //下载完成
                    file.renameTo(new File(filePath));
                    onBack(h, listener, 3, url, filePath, currentLength, totalLength, "completed");
                } else {
                    //计算当前下载百分比，并经由回调传出
                    onBack(h, listener, 2, url, filePath, currentLength, totalLength, "pro");
                }
            }
            os.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
            is = null;
            onBack(h, listener, 4, url, filePath, currentLength, totalLength, e.getMessage());

        }
        downloadType = 2;
    }

    private void onBack(Handler h, BaseManager.Progress listener, int what, String url, String filePath,
                        long progress, long total, String msg) {
        if (listener != null) {
            listener.onProgress(what, url, filePath, progress, total, msg);
        }

    }

}