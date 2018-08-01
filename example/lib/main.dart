import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_jpush/flutter_jpush.dart';

void main() => runApp(new MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => new _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  String _topText = '';

  final FlutterJpush _flutterJpush = new FlutterJpush();

  @override
  void initState() {
    super.initState();
    initPlatformState();
    _initJPush();
    _flutterJpush.configure(
      onMessage: (Map<String, dynamic> message) {
        print("onMessage: $message");
//        _showItemDialog(message);
      },
      onLaunch: (Map<String, dynamic> message) {
        print("onLaunch: $message");
//        _navigateToItemDetail(message);
      },
      onResume: (Map<String, dynamic> message) {
        print("onResume: $message");
//        _navigateToItemDetail(message);
      },
    );
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await FlutterJpush.platformVersion;
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
      _topText = _platformVersion;
    });
  }

  void _test() async {
    String result = await FlutterJpush.test();
    print(result);
    setState(() {
      _topText = result;
    });
  }

  Future _initJPush() async {
    //init JPush
    dynamic result = await FlutterJpush.initJpush();
    print(result);
  }

  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      home: new Scaffold(
        appBar: new AppBar(
          title: const Text('Plugin example app'),
        ),
        body: new Material(
          child: Column(
            children: <Widget>[
              new Center(
                child: new Text('Running on: $_topText\n'),
              ),
              new RaisedButton(
                child: new Text('init JPush'),
                onPressed: _initJPush,
              ),
              new RaisedButton(
                child: new Text('test'),
                onPressed: _test,
              )
            ],
          ),
        ),
      ),
    );
  }
}
