package cn.elift.flutterjpush;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

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
        try {
            final Bundle bundle = intent.getExtras();
            Logger.d(TAG, "[MyReceiver] onReceive - " + intent.getAction() + ", extras: " + printBundle(bundle));
            if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
                Logger.d(TAG, "检测到开机启动，去启动服务");
                initJpush();
            } else if (JPushInterface.ACTION_REGISTRATION_ID.equals(intent.getAction())) {
                String regId = bundle.getString(JPushInterface.EXTRA_REGISTRATION_ID);
                Logger.d(TAG, "[MyReceiver] 接收Registration Id : " + regId);
                ready = true;
                if (channel != null) {
                    channel.invokeMethod("onMessage", bundleToMap(bundle));
                }
            } else if (JPushInterface.ACTION_MESSAGE_RECEIVED.equals(intent.getAction())) {
                Logger.d(TAG, "[MyReceiver] 接收到推送下来的自定义消息: " + bundle.getString(JPushInterface.EXTRA_MESSAGE));
//                processCustomMessage(context, bundle);
                if (channel != null) {
                    channel.invokeMethod("onMessage", bundleToMap(bundle));
                }
            } else if (JPushInterface.ACTION_NOTIFICATION_RECEIVED.equals(intent.getAction())) {
                Logger.d(TAG, "[MyReceiver] 接收到推送下来的通知");
                int notifactionId = bundle.getInt(JPushInterface.EXTRA_NOTIFICATION_ID);
                Logger.d(TAG, "[MyReceiver] 接收到推送下来的通知的ID: " + notifactionId);
                if (channel != null) {
                    channel.invokeMethod("onMessage", bundleToMap(bundle));
                }
            } else if (JPushInterface.ACTION_NOTIFICATION_OPENED.equals(intent.getAction())) {
                Logger.d(TAG, "[MyReceiver] 用户点击打开了通知");
                if (channel != null && registrar.activity() != null) {
                    if (!isAppOnForeground(context)) {
                        bringAppToForeground(context, null);
                    }
                    Logger.d(TAG, channel.toString());
                    Logger.d(TAG, registrar.activity().toString());
                    channel.invokeMethod("onResume", bundleToMap(bundle));
                } else {
                    //打开自定义的Activity
                    bringAppToForeground(context, bundle);
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

    private boolean bringAppToForeground(Context context, Bundle bundle) {
        SharedPreferences prefs = context.getSharedPreferences("JPUSH", MODE_PRIVATE);
        String className = prefs.getString("MAIN_ACTIVITY_CLASSNAME", "");
        Logger.d(TAG, "ActivityClassName----" + className);
        if (className.isEmpty()) {
            return false;
        }
        Class<?> activityClass = null;
        try {
            activityClass = Class.forName(className);
            Intent i = new Intent(context, activityClass);
            if (bundle != null)
                i.putExtras(bundle);
            //i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(i);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return true;
    }

    private boolean isAppOnForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (call.method.equals("getPlatformVersion")) {
            result.success("Android " + android.os.Build.VERSION.RELEASE);
        } else if ("initJpush".equals(call.method)) {
            initJpush(BuildConfig.DEBUG);
            String mainClassName = registrar.activity().getClass().getName();
            SharedPreferences prefs = context.getSharedPreferences("JPUSH", MODE_PRIVATE);
            prefs.edit().putString("MAIN_ACTIVITY_CLASSNAME", mainClassName).apply();
            JPushInterface.getRegistrationID(context);
            result.success(null);
            ready = true;
        } else if ("setAlias".equals(call.method)) {
            String alias = call.arguments();
            setAlias(context, alias);
            result.success(null);
        } else if ("deleteAlias".equals(call.method)) {
//            String alias = call.arguments();
            SharedPreferences prefs = context.getSharedPreferences("JPUSH", MODE_PRIVATE);
            String alias = prefs.getString("ALIAS", "");
            if (!alias.isEmpty()) {
                deleteAlias(context, alias);
            }
            result.success(null);
        } else if ("getAlias".equals(call.method)) {
            getAlias(context);
            result.success(null);
        }
//        else if ("ready".equals(call.method)) {
//            ready = true;
//            result.success(null);
//        }
        else if ("stopPush".equals(call.method)) {
            JPushInterface.stopPush(context);
            result.success(null);
        } else if ("resumePush".equals(call.method)) {
            JPushInterface.resumePush(context);
            result.success(null);
        } else if ("clearCache".equals(call.method)) {
            SharedPreferences prefs = context.getSharedPreferences("JPUSH", MODE_PRIVATE);
            prefs.edit().remove("ALIAS").remove("MAIN_ACTIVITY_CLASSNAME").apply();
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
        SharedPreferences prefs = context.getSharedPreferences("JPUSH", MODE_PRIVATE);
        Boolean debug = prefs.getBoolean("DEBUG", false);
        initJpush(debug);
    }

    private void initJpush(Boolean debug) {
        JPushInterface.init(context);
        JPushInterface.setDebugMode(debug);
        SharedPreferences prefs = context.getSharedPreferences("JPUSH", MODE_PRIVATE);
        prefs.edit().putBoolean("DEBUG", debug);
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
