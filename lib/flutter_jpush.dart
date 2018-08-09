import 'dart:async';

import 'package:flutter/services.dart';
import 'package:meta/meta.dart';

typedef Future<dynamic> MessageHandler(Map<String, dynamic> message);

class FlutterJpush {
  static const MethodChannel _channel = const MethodChannel('flutter_jpush');

  MessageHandler _onMessage;
  MessageHandler _onResume;
  String _registrationID;

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<dynamic> initJpush() async {
    final dynamic result = await _channel.invokeMethod('initJpush');
    return result;
  }

  static void stopPush() async {
    await _channel.invokeMethod('stopPush');
  }

  static void resumePush() async {
    await _channel.invokeMethod('resumePush');
  }

  //根据用户别名发消息给指定用户
  static void setAlias(String alias) async {
    await _channel.invokeMethod('setAlias', alias);
  }

  static void deleteAlias(String alias) async {
    await _channel.invokeMethod('deleteAlias', alias);
  }

  static void getAlias() async {
    await _channel.invokeMethod('getAlias');
  }

  //如果用户登录设置过一次别名，jpush plugin 将把别名存放在文件中，
  // 下次打开，自动设置已有的别名，如果logout请先deleteAlias再清除jpush缓存clearCache，
  static void clearCache() async {
    await _channel.invokeMethod('clearCache');
  }

  //flutter中jpush配置完成调用ready
//  static void ready() async {
//    await _channel.invokeMethod('ready');
//  }

  static Future<String> test() async {
    final String result = await _channel.invokeMethod('test');
    return result;
  }

  /// Sets up [MessageHandler] for incoming messages.
  void configure({
    MessageHandler onMessage,
    MessageHandler onResume,
  }) {
    _onMessage = onMessage;
    _onResume = onResume;
    _channel.setMethodCallHandler(_handleMethod);
//    _channel.invokeMethod('configure');
  }

  Future<Null> _handleMethod(MethodCall call) async {
    switch (call.method) {
      case "onToken":
//        final String token = call.arguments;
//        if (_registrationID != token) {
//          _registrationID = token;
//          _tokenStreamController.add(_registrationID);
//        }
        return null;
      case "onIosSettingsRegistered":
//        _iosSettingsStreamController.add(new IosNotificationSettings._fromMap(
//            call.arguments.cast<String, bool>()));
//        return null;
      case "onMessage":
        return _onMessage(call.arguments.cast<String, dynamic>());
      case "onResume":
        return _onResume(call.arguments.cast<String, dynamic>());
      default:
        throw new UnsupportedError("Unrecognized JSON message");
    }
  }
}

class IosNotificationSettings {
  final bool sound;
  final bool alert;
  final bool badge;

  const IosNotificationSettings({
    this.sound: true,
    this.alert: true,
    this.badge: true,
  });

  IosNotificationSettings._fromMap(Map<String, bool> settings)
      : sound = settings['sound'],
        alert = settings['alert'],
        badge = settings['badge'];

  @visibleForTesting
  Map<String, dynamic> toMap() {
    return <String, bool>{'sound': sound, 'alert': alert, 'badge': badge};
  }

  @override
  String toString() => 'PushNotificationSettings ${toMap()}';
}
