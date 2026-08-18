package com.app.net.manager.uploading;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class FileUtile {
    private static String fileDir = "hn_earn";

    //把Assets 读取出来 存到缓存
    public static File copyAssetsToCache(Context context, String assetsName) {
        try {
            File outFile = new File(context.getCacheDir(), assetsName);
            InputStream in = context.getAssets().open(assetsName);
            FileOutputStream out = new FileOutputStream(outFile);
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            in.close();
            out.close();
            return outFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    //保存至私有目录
    public static String getFileToPri(Context context, String suffix) {
        File appDir = new File(context.getExternalCacheDir(), fileDir);
        if (!appDir.isDirectory()) {
            appDir.mkdirs();
        }
        Random random = new Random();
        int randomNumber = random.nextInt(90000);
        String fileName = System.currentTimeMillis() + "_" + randomNumber + "." + suffix;
        return appDir.getPath() + "/" + fileName;
    }
}
