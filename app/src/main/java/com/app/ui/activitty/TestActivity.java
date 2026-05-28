package com.app.ui.activitty;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.app.net.manager.down.DownloadFileManager;
import com.app.net.manager.login.LoginManager;
import com.app.net.manager.uploading.FileUtile;
import com.app.net.manager.uploading.UploadingManager;
import com.app.ui.R;
import com.app.ui.bean.TestBean;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.retrofits.net.common.BaseJsonReplace;
import com.retrofits.net.common.RequestBack;
import com.retrofits.utiles.RLog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.RequestBody;

public class TestActivity extends Activity implements RequestBack, View.OnClickListener {


    private LoginManager manager;
    private DownloadFileManager downloadManager;
    private UploadingManager uploadingManager;
    private TextView tvNum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        tvNum = findViewById(R.id.tv_num);
        findViewById(R.id.login_btn).setOnClickListener(this);
        findViewById(R.id.download_btn).setOnClickListener(this);
        findViewById(R.id.up_btn).setOnClickListener(this);
        findViewById(R.id.up_btn_2).setOnClickListener(this);

        RLog.DBUG = true;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.login_btn) {
            //testLogin();
            testJson();
            return;
        }
        if (id == R.id.download_btn) {
            testDownload();
            return;
        }
        if (id == R.id.up_btn) {
            testUploading();
            return;
        }
        if (id == R.id.up_btn_2) {
            testUploading2();
        }
    }

    private void testJson() {
        TestBean bean = new TestBean();
        String json = obj2Json(bean);
        RLog.e("json->", json);
    }

    private String obj2Json(Object obj) {
        if (obj == null) {
            return null;
        }
        String json = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            SimpleModule sm = new SimpleModule();
            sm.addSerializer(String.class, new BaseJsonReplace());
            mapper.registerModule(sm);
            mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
            // 过滤对象的null属性.
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            json = mapper.writeValueAsString(obj);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return json;
    }

    private void testLogin() {
        if (manager == null) {
            manager = new LoginManager(this);
        }
        manager.setData("18868714254", "123456");
        manager.request();
    }

    private void testDownload() {
        if (downloadManager == null) {
            downloadManager = new DownloadFileManager(this);
        }
        downloadManager.request();
    }

    private void testUploading() {
        if (uploadingManager == null) {
            uploadingManager = new UploadingManager(this);
        }
        String path = "/storage/emulated/0/Download/test.pdf";
        uploadingManager.setData(new File(path));
        uploadingManager.request();
    }

    //上传
    private void testUploading2() {
        if (uploadingManager == null) {
            uploadingManager = new UploadingManager(this);
        }
        if (true) {
            File file = new File(getCacheDir(), "video.mp4");
            if (!file.exists()) {
                file = FileUtile.copyAssetsToCache(this, "video.mp4");
            }
            Log.d("文件上传下载：", "复制文件，新文件地址名称：" + file.getPath());
            uploadingManager.request21(file);
            return;
        }
        RequestBody body = getAssetsFileRequestBody(this, "video.mp4");
        uploadingManager.request21(body, "video.mp4");

    }

    // 从 assets 读取文件，直接构建 RequestBody
    private RequestBody getAssetsFileRequestBody(Context context, String assetsFileName) {
        try {
            // 打开 assets 流
            InputStream inputStream = context.getAssets().open(assetsFileName);
            // 读取流到字节数组（适合小/中文件，视频也可以）
            byte[] bytes = toByteArray(inputStream);
            Log.d("文件上传下载：", "文件字节：" + bytes);
            // 返回 RequestBody（视频类型）
            return RequestBody.create(MediaType.parse("video/mp4"), bytes);

        } catch (Exception e) {
            Log.d("文件上传下载：", "文件读取失败：" + e.getMessage());
            return null;
        }
    }

    // 流 → 字节数组
    private byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        input.close();
        return output.toByteArray();
    }

    @Override
    public void onBack(int what, Object obj, String msg, String other) {
        Log.d("文件上传下载：onBack" + what, "obj:" + obj + " msg:" + msg + " other:" + other);
    }

    @Override
    public void onBackProgress(int what, String url, String filePath, long currentLength, long totalLength) {
        Log.d("文件上传下载：what" + what, "url：" + url + " filePath:" + filePath + " " +
                "currentLength:" + currentLength + " totalLength:" + totalLength);
        tvNum.setText(currentLength + "/" + totalLength);
    }


}
