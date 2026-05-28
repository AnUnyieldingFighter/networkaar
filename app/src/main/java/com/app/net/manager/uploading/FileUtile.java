package com.app.net.manager.uploading;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtile {
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
}
