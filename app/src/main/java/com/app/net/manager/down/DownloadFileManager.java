package com.app.net.manager.down;

import com.app.net.common.RequestResultThreadDownloadListener;
import com.app.net.common.UrlManger;
import com.retrofits.net.common.BaseNetSource;
import com.retrofits.net.common.RequestBack;
import com.retrofits.net.manager.BaseManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * 下载
 * Created by Administrator on 2016/9/7.
 */
public class DownloadFileManager extends BaseManager {

    private String fileUrl = "http://img-smarthos.hztywl.cn/EDU_COURSE_201908_mPn7ZJR_uqZKQ.pdf";
    private String fialePath = "/storage/emulated/0/Download/test.pdf";

    public DownloadFileManager(RequestBack requestBack) {
        super(requestBack);
    }

    public void setData(String fileUrl, String fialePath) {
        this.fileUrl = fileUrl;
        this.fialePath = fialePath;
    }

    private RequestResultThreadDownloadListener listener;

    public void onStop() {
        if (listener == null) {
            return;
        }
        listener.onStopDownload(call);
    }

    private Call<ResponseBody> call;

    public void request() {
        BaseNetSource source = new BaseNetSource();
        Retrofit retrofit = source.getRetrofit(new UrlManger());
        DownloadApi service = retrofit.create(DownloadApi.class);
        //断点下载
        //Call<ResponseBody> call = service.download("bytes=" + 536 + "-", fileUrl);
        //下载
        call = service.download(fileUrl);
        listener = new RequestResultThreadDownloadListener(
                this, call);
        listener.setDownloadFile(fileUrl, fialePath);
        listener.start();
    }

    public void request2() {
        BaseNetSource source = new BaseNetSource();
        Retrofit retrofit = source.getRetrofit(new UrlManger());
        source.setProgressListener(2, getProgress(false), fialePath);
        DownloadApi service = retrofit.create(DownloadApi.class);
        //下载
        call = service.download2(fileUrl);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                // 在这里写文件，和上面一样
                new Thread(() -> saveFile(response.body(), fialePath)).start();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });


    }

    // 保存文件
    private boolean saveFile(ResponseBody body, String path) {
        try {
            File file = new File(path);
            InputStream is = body.byteStream();
            FileOutputStream fos = new FileOutputStream(file);

            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }

            fos.flush();
            fos.close();
            is.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
