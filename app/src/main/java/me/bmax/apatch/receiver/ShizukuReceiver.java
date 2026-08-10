package me.bmax.apatch.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import me.bmax.apatch.util.ShellBinderRequestHandler;

/**
 * 响应旧版 Shizuku 客户端的 Binder 请求广播，与官方 Manager 对齐。
 */
public class ShizukuReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("rikka.shizuku.intent.action.REQUEST_BINDER".equals(intent.getAction())) {
            ShellBinderRequestHandler.handleRequest(context, intent);
        }
    }
}
