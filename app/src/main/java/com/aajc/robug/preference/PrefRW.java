package com.aajc.robug.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class PrefRW {

    private static final String TAG = "RW";
    private static final String PRE_ROOT_NAME = "nRis";

    public static void write(Context ctx, String identification, String data) {

        SharedPreferences settings = ctx.getSharedPreferences(PRE_ROOT_NAME,
                Context.MODE_PRIVATE);
        Log.d(TAG, "writing <" + identification + ">: " + data);
        settings.edit().putString(identification, data).commit();
    }

    public static void remove(Context ctx, String identification) {

        SharedPreferences settings = ctx.getSharedPreferences(PRE_ROOT_NAME,
                Context.MODE_PRIVATE);
        Log.d(TAG, "removing: <" + identification + ">");
        settings.edit().remove(identification).commit();
    }

    public static String read(Context ctx, String identification) {
        String defaultString = "";
        SharedPreferences settings = ctx.getSharedPreferences(PRE_ROOT_NAME,
                Context.MODE_PRIVATE);
        String info = settings.getString(identification, defaultString);
        Log.d(TAG, "read <" + identification + ">: " + info);
        return info;
    }
}
