package cn.elift.flutterjpush;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import cn.jpush.android.api.JPushInterface;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import static android.content.Context.MODE_PRIVATE;
import static cn.elift.flutterjpush.TagAliasOperatorHelper.ACTION_CLEAN;
import static cn.elift.flutterjpush.TagAliasOperatorHelper.ACTION_DELETE;
import static cn.elift.flutterjpush.TagAliasOperatorHelper.ACTION_GET;
import static cn.elift.flutterjpush.TagAliasOperatorHelper.ACTION_SET;
import static cn.elift.flutterjpush.TagAliasOperatorHelper.sequence;
import static cn.elift.flutterjpush.Utils.bundleToMap;
import static cn.elift.flutterjpush.Utils.printBundle;

/**
 * FlutterJpushPlugin
 */
public class FlutterJpushPlugin extends BroadcastReceiver implements MethodCallHandler, PluginRegistry.NewIntentListener {
    private static final String TAG = "JIGUANG-Example";
    private Context context;
    private static Registrar registrar;
    private static MethodChannel channel;
    private static Boolean ready = false;

    //FlutterJpushPlugin 创建两个实例 一个是 FlutterJpushPlugin 由 GeneratedPluginRegistrant创建，
    // 一个是BroadcastReceiver 广播接收者 由 系统创建（用于接收Jpush服务接收到消息时发送的广播，即使前台关闭）
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
        FlutterJpushPlugin.ready = false;
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
            final Bundle bundle = intent.getExtras();
            Logger.d(TAG, "[MyReceiver] onReceive - " + intent.getAction() + ", extras: " + printBundle(bundle));
            if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
                Logger.d(TAG, "检测到开机启动，去启动服务");
//                Intent newIntent = new Intent(context, StartService.class);
//                context.startService(newIntent);
                initJpush();
            } else if (JPushInterface.ACTION_REGISTRATION_ID.equals(intent.getAction())) {
                String regId = bundle.getString(JPushInterface.EXTRA_REGISTRATION_ID);
                Logger.d(TAG, "[MyReceiver] 接收Registration Id : " + regId);
                //send the Registration Id to your server...
                if (channel != null)
                    channel.invokeMethod("onMessage", bundleToMap(bundle));
            } else if (JPushInterface.ACTION_MESSAGE_RECEIVED.equals(intent.getAction())) {
                Logger.d(TAG, "[MyReceiver] 接收到推送下来的自定义消息: " + bundle.getString(JPushInterface.EXTRA_MESSAGE));
//                processCustomMessage(context, bundle);
                if (channel != null)
                    channel.invokeMethod("onMessage", bundleToMap(bundle));
            } else if (JPushInterface.ACTION_NOTIFICATION_RECEIVED.equals(intent.getAction())) {
                Logger.d(TAG, "[MyReceiver] 接收到推送下来的通知");
                int notifactionId = bundle.getInt(JPushInterface.EXTRA_NOTIFICATION_ID);
                Logger.d(TAG, "[MyReceiver] 接收到推送下来的通知的ID: " + notifactionId);
                if (channel != null)
                    channel.invokeMethod("onMessage", bundleToMap(bundle));
            } else if (JPushInterface.ACTION_NOTIFICATION_OPENED.equals(intent.getAction())) {
                Logger.d(TAG, "[MyReceiver] 用户点击打开了通知");
                if (channel != null) {
                    channel.invokeMethod("onResume", bundleToMap(bundle));
                } else {
                    //打开自定义的Activity
                    String className = context.getApplicationContext().getPackageName() + ".MainActivity";
                    try {
                        Class<?> activityClass = Class.forName(className);
                        Intent i = new Intent(context, activityClass);
                        i.putExtras(bundle);
                        //i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        context.startActivity(i);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    //等待MainActivity打开 调用flutter code
                    Thread t1 = new Thread(new Runnable() {
                        public void run() {
                            // code goes here.
                            while (true) {
                                if (!ready) {
                                    try {
                                        Thread.sleep(50);
                                        Logger.d(TAG, "Flutter Jpush Plugin waitting for application to start...");
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    channel.invokeMethod("onResume", bundleToMap(bundle));
                                    break;
                                }
                            }
                        }
                    });
                    t1.start();
                }

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
            initJpush();
            String rid = JPushInterface.getRegistrationID(context);
            if (!rid.isEmpty()) {
                result.success(rid);
            } else {
                result.error("var1", "var2", "var3");
            }
//            ready = true;
        } else if ("setAlias".equals(call.method)) {
            String alias = call.arguments();
            setAlias(context, alias);
        } else if ("deleteAlias".equals(call.method)) {
            String alias = call.arguments();
            deleteAlias(context, alias);
        } else if ("getAlias".equals(call.method)) {
            getAlias(context);
        } else if ("ready".equals(call.method)) {
            ready = true;
            result.success(null);
        } else if ("stopPush".equals(call.method)) {
            JPushInterface.stopPush(context);
            result.success(null);
        } else if ("resumePush".equals(call.method)) {
            JPushInterface.resumePush(context);
            result.success(null);
        } else if ("clearCache".equals(call.method)) {
            SharedPreferences prefs = context.getSharedPreferences("JPUSH", MODE_PRIVATE);
            prefs.edit().clear().apply();
            result.success(null);
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

    private void initJpush() {
        JPushInterface.init(context);
        SharedPreferences prefs = context.getSharedPreferences("JPUSH", MODE_PRIVATE);
        String alias = prefs.getString("ALIAS", null);
        if (alias != null) {
            Log.d(TAG, "setAlias alias" + alias);
            setAlias(context, alias);
        }
    }

    private void setAlias(Context context, String alias) {
        TagAliasOperatorHelper.TagAliasBean tagAliasBean = new TagAliasOperatorHelper.TagAliasBean();
        tagAliasBean.action = ACTION_SET;
        sequence++;
        tagAliasBean.alias = alias;
        tagAliasBean.isAliasAction = true;
        TagAliasOperatorHelper.getInstance().handleAction(context, sequence, tagAliasBean);
        //-----
        SharedPreferences prefs = context.getSharedPreferences("JPUSH", MODE_PRIVATE);
        prefs.edit().putString("ALIAS", alias).apply();
    }

    private void getAlias(Context context) {
        TagAliasOperatorHelper.TagAliasBean tagAliasBean = new TagAliasOperatorHelper.TagAliasBean();
        tagAliasBean.action = ACTION_GET;
        sequence++;
        tagAliasBean.isAliasAction = true;
        TagAliasOperatorHelper.getInstance().handleAction(context, sequence, tagAliasBean);
    }

    private void deleteAlias(Context context, String alias) {
        TagAliasOperatorHelper.TagAliasBean tagAliasBean = new TagAliasOperatorHelper.TagAliasBean();
        tagAliasBean.action = ACTION_DELETE;
        sequence++;
        tagAliasBean.alias = alias;
        tagAliasBean.isAliasAction = true;
        TagAliasOperatorHelper.getInstance().handleAction(context, sequence, tagAliasBean);
        //-----
        SharedPreferences prefs = context.getSharedPreferences("JPUSH", MODE_PRIVATE);
        prefs.edit().remove("ALIAS").apply();
    }

    @Override
    public boolean onNewIntent(Intent intent) {
        return false;
    }
}
