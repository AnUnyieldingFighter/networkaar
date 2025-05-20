package com.app.ui.activitty;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.app.net.manager.down.DownloadFileManager;
import com.app.net.manager.login.LoginManager;
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

import java.io.File;
import java.io.IOException;

public class TestActivity extends Activity implements RequestBack, View.OnClickListener {


    private LoginManager manager;
    private DownloadFileManager downloadManager;
    private UploadingManager uploadingManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        findViewById(R.id.login_btn).setOnClickListener(this);
        findViewById(R.id.download_btn).setOnClickListener(this);
        findViewById(R.id.up_btn).setOnClickListener(this);
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

    @Override
    public void onBack(int what, Object obj, String msg, String other) {
        Log.e("onBack" + what, "obj:" + obj + " msg:" + msg + " other:" + other);
    }

    private int c = -1;

    @Override
    public void onBackProgress(int what, String url, String filePath, long currentLength, long totalLength) {
      /*  if (c == what) {
            return;
        }
        c = what;*/
        Log.e("what" + what, "url：" + url + " filePath:" + filePath + " " +
                "currentLength:" + currentLength + " totalLength:" + totalLength);
    }


}
