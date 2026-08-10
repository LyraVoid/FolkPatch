package me.bmax.apatch.util;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.util.Log;

import rikka.shizuku.Shizuku;

/**
 * 处理旧版 Shizuku 客户端通过广播请求 Binder 的机制
 * （rikka.shizuku.intent.action.REQUEST_BINDER），与官方 Manager 对齐。
 */
public final class ShellBinderRequestHandler {

    private static final String TAG = "ShellBinder";
    private static final String ACTION_REQUEST_BINDER = "rikka.shizuku.intent.action.REQUEST_BINDER";

    private ShellBinderRequestHandler() {
    }

    public static boolean handleRequest(Context context, Intent intent) {
        if (!ACTION_REQUEST_BINDER.equals(intent.getAction())) {
            return false;
        }

        Bundle data = intent.getBundleExtra("data");
        if (data == null) {
            return false;
        }
        IBinder binder = data.getBinder("binder");
        if (binder == null) {
            return false;
        }

        IBinder shizukuBinder = Shizuku.getBinder();
        if (shizukuBinder == null) {
            Log.w(TAG, "Binder not received or Shizuku service not running");
        }

        Parcel parcel = Parcel.obtain();
        try {
            parcel.writeStrongBinder(shizukuBinder);
            parcel.writeString(context.getApplicationInfo().sourceDir);
            binder.transact(1, parcel, null, IBinder.FLAG_ONEWAY);
            return true;
        } catch (Throwable tr) {
            Log.e(TAG, "failed to send binder", tr);
            return false;
        } finally {
            parcel.recycle();
        }
    }
}
