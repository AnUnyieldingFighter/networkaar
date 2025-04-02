package com.retrofits.utiles;

import android.util.Log;

/**
 * Created by Administrator on 2016/3/9.
 */
public class RLog {
    public static boolean DBUG = false;

    public static void e(String tag, Object value) {
        if (!DBUG) {
            return;
        }
        Log.e(tag, value + "");
    }
}