package me.bmax.apatch.util;

import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import moe.shizuku.api.BinderContainer;
import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuApiConstants;
import rikka.shizuku.ShizukuProvider;

/**
 * 管理端 Shizuku Provider。
 *
 * 在 {@link ShizukuProvider} 的 sendBinder / getBinder 之外补充 sendUserService 处理，
 * 与官方 Shizuku Manager 的 ShizukuManagerProvider 对齐：
 * 第三方应用的 UserService 进程启动后，通过本 provider 把它的
 * service binder 交给管理端，同时取回 server binder。缺少该处理会导致 UserService
 * 无法完成初始化（"server binder not received"）。
 */
public class ShizukuManagerProvider extends ShizukuProvider {

    private static final String TAG = "ShizukuProvider";
    private static final String EXTRA_BINDER = "moe.shizuku.privileged.api.intent.extra.BINDER";

    @Override
    public boolean onCreate() {
        disableAutomaticSuiInitialization();
        return super.onCreate();
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if ("sendUserService".equals(method)) {
            return handleSendUserService(extras);
        }
        return super.call(method, arg, extras);
    }

    private Bundle handleSendUserService(Bundle args) {
        if (args == null) return null;
        try {
            // The bundle crosses processes; the app class (BinderContainer) is not
            // resolvable by the default (bootstrap) classloader used by Parcel.
            args.setClassLoader(BinderContainer.class.getClassLoader());
            String token = args.getString(ShizukuApiConstants.USER_SERVICE_ARG_TOKEN);
            BinderContainer container = args.getParcelable(EXTRA_BINDER);
            if (token == null || container == null || container.binder == null) {
                Log.w(TAG, "sendUserService: missing token or binder");
                return null;
            }
            final IBinder userServiceBinder = container.binder;
            final String userServiceToken = token;

            final CountDownLatch latch = new CountDownLatch(1);
            final Bundle[] reply = new Bundle[1];

            Shizuku.OnBinderReceivedListener listener = new Shizuku.OnBinderReceivedListener() {
                @Override
                public void onBinderReceived() {
                    try {
                        Bundle options = new Bundle();
                        options.putString(ShizukuApiConstants.USER_SERVICE_ARG_TOKEN, userServiceToken);
                        Shizuku.attachUserService(userServiceBinder, options);

                        Bundle result = new Bundle();
                        result.setClassLoader(BinderContainer.class.getClassLoader());
                        result.putParcelable(EXTRA_BINDER, new BinderContainer(Shizuku.getBinder()));
                        reply[0] = result;
                    } catch (Throwable tr) {
                        Log.e(TAG, "attachUserService " + userServiceToken + " failed", tr);
                        reply[0] = null;
                    }
                    Shizuku.removeBinderReceivedListener(this);
                    latch.countDown();
                }
            };

            // Sticky: if the server binder is already available it is invoked
            // immediately, otherwise when the binder arrives (max 5s).
            Shizuku.addBinderReceivedListenerSticky(listener);

            try {
                if (latch.await(5, TimeUnit.SECONDS)) {
                    return reply[0];
                }
                Log.e(TAG, "Binder not received in 5s");
                return null;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        } catch (Throwable tr) {
            Log.e(TAG, "sendUserService failed", tr);
            return null;
        }
    }
}
