import 'dart:async';

import 'package:flutter/services.dart';
import 'package:meta/meta.dart';

typedef Future<dynamic> MessageHandler(Map<String, dynamic> message);

class FlutterJpush {
  static const MethodChannel _channel = const MethodChannel('flutter_jpush');

  MessageHandler _onMessage;
  MessageHandler _onLaunch;
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

  static Future<String> test() async {
    final String result = await _channel.invokeMethod('test');
    return result;
  }

  /// Sets up [MessageHandler] for incoming messages.
  void configure({
    MessageHandler onMessage,
    MessageHandler onLaunch,
    MessageHandler onResume,
  }) {
    _onMessage = onMessage;
    _onLaunch = onLaunch;
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
      case "onLaunch":
        return _onLaunch(call.arguments.cast<String, dynamic>());
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
