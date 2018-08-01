package cn.elift.flutterjpush;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import cn.jpush.android.api.JPushInterface;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import static cn.elift.flutterjpush.Utils.bundleToMap;
import static cn.elift.flutterjpush.Utils.printBundle;

/**
 * FlutterJpushPlugin
 */
public class FlutterJpushPlugin extends BroadcastReceiver implements MethodCallHandler {
    private static final String TAG = "JIGUANG-Example";
    private Context context;
    private static Registrar registrar;
    private static MethodChannel channel;

    public FlutterJpushPlugin() {
    }

    public FlutterJpushPlugin(Context context) {
        this.context = context;
    }

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_jpush");
        FlutterJpushPlugin.registrar = registrar;
        FlutterJpushPlugin.channel = channel;
        channel.setMethodCallHandler(new FlutterJpushPlugin(registrar.context()));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
//        if (action.equals(FlutterFirebaseInstanceIDService.ACTION_TOKEN)) {
//            String token = intent.getStringExtra(FlutterFirebaseInstanceIDService.EXTRA_TOKEN);
//            channel.invokeMethod("onToken", token);
//        } else if (action.equals(FlutterFirebaseMessagingService.ACTION_REMOTE_MESSAGE)) {
//            RemoteMessage message =
//                    intent.getParcelableExtra(FlutterFirebaseMessagingService.EXTRA_REMOTE_MESSAGE);
//            channel.invokeMethod("onMessage", message.getData());
//        }
        try {
            Bundle bundle = intent.getExtras();
            Logger.d(TAG, "[MyReceiver] onReceive - " + intent.getAction() + ", extras: " + printBundle(bundle));

            if (JPushInterface.ACTION_REGISTRATION_ID.equals(intent.getAction())) {
                String regId = bundle.getString(JPushInterface.EXTRA_REGISTRATION_ID);
                Logger.d(TAG, "[MyReceiver] 接收Registration Id : " + regId);
                //send the Registration Id to your server...
                channel.invokeMethod("onMessage", bundleToMap(bundle));
            } else if (JPushInterface.ACTION_MESSAGE_RECEIVED.equals(intent.getAction())) {
                Logger.d(TAG, "[MyReceiver] 接收到推送下来的自定义消息: " + bundle.getString(JPushInterface.EXTRA_MESSAGE));
//                processCustomMessage(context, bundle);
                channel.invokeMethod("onMessage", bundleToMap(bundle));
            } else if (JPushInterface.ACTION_NOTIFICATION_RECEIVED.equals(intent.getAction())) {
                Logger.d(TAG, "[MyReceiver] 接收到推送下来的通知");
                int notifactionId = bundle.getInt(JPushInterface.EXTRA_NOTIFICATION_ID);
                Logger.d(TAG, "[MyReceiver] 接收到推送下来的通知的ID: " + notifactionId);
                channel.invokeMethod("onMessage", bundleToMap(bundle));
            } else if (JPushInterface.ACTION_NOTIFICATION_OPENED.equals(intent.getAction())) {
                Logger.d(TAG, "[MyReceiver] 用户点击打开了通知");

//                //打开自定义的Activity
//                Intent i = new Intent(context, TestActivity.class);
//                i.putExtras(bundle);
//                //i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP );
//                context.startActivity(i);

            } else if (JPushInterface.ACTION_RICHPUSH_CALLBACK.equals(intent.getAction())) {
                Logger.d(TAG, "[MyReceiver] 用户收到到RICH PUSH CALLBACK: " + bundle.getString(JPushInterface.EXTRA_EXTRA));
                //在这里根据 JPushInterface.EXTRA_EXTRA 的内容处理代码，比如打开新的Activity， 打开一个网页等..

            } else if (JPushInterface.ACTION_CONNECTION_CHANGE.equals(intent.getAction())) {
                boolean connected = intent.getBooleanExtra(JPushInterface.EXTRA_CONNECTION_CHANGE, false);
                Logger.w(TAG, "[MyReceiver]" + intent.getAction() + " connected state change to " + connected);
            } else {
                Logger.d(TAG, "[MyReceiver] Unhandled intent - " + intent.getAction());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (call.method.equals("getPlatformVersion")) {
            result.success("Android " + android.os.Build.VERSION.RELEASE);
        } else if ("initJpush".equals(call.method)) {
            JPushInterface.init(context);
            String rid = JPushInterface.getRegistrationID(context);
            if (!rid.isEmpty()) {
                result.success(rid);
            } else {
                result.error("var1", "var2", "var3");
            }
        } else if ("test".equals(call.method)) {
            ApplicationInfo app = null;
            try {
                app = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
                Bundle bundle = app.metaData;
                for (String key : bundle.keySet()) {
                    Log.d("TEST bundle ", key + " - " + bundle.get(key));
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            result.success("success");
        } else {
            result.notImplemented();
        }
    }
}
