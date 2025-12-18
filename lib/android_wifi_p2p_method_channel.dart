import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'android_wifi_p2p_platform_interface.dart';
import 'models/models.dart';

/// An implementation of [AndroidWifiP2pPlatform] that uses method channels.
class MethodChannelAndroidWifiP2p extends AndroidWifiP2pPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('android_wifi_p2p');

  /// The event channel for Wi-Fi P2P state changes.
  @visibleForTesting
  final stateEventChannel = const EventChannel('android_wifi_p2p/state');

  /// The event channel for peer list updates.
  @visibleForTesting
  final peersEventChannel = const EventChannel('android_wifi_p2p/peers');

  /// The event channel for connection info updates.
  @visibleForTesting
  final connectionInfoEventChannel = const EventChannel(
    'android_wifi_p2p/connection_info',
  );

  /// The event channel for this device info updates.
  @visibleForTesting
  final thisDeviceEventChannel = const EventChannel(
    'android_wifi_p2p/this_device',
  );

  /// The event channel for incoming messages.
  @visibleForTesting
  final messageEventChannel = const EventChannel('android_wifi_p2p/message');

  Stream<WifiP2pState>? _wifiP2pStateStream;
  Stream<List<WifiP2pDevice>>? _peersStream;
  Stream<WifiP2pInfo>? _connectionInfoStream;
  Stream<WifiP2pDevice>? _thisDeviceStream;

  @override
  Future<int?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<int>('getPlatformVersion');
    return version;
  }

  @override
  Future<String?> getDeviceName() async {
    final name = await methodChannel.invokeMethod<String>('getDeviceName');
    return name;
  }

  @override
  Future<void> register() async {
    await methodChannel.invokeMethod<void>('register');
  }

  @override
  Future<void> unregister() async {
    await methodChannel.invokeMethod<void>('unregister');
  }

  @override
  Future<bool> discoverPeers() async {
    final result = await methodChannel.invokeMethod<bool>('discoverPeers');
    return result ?? false;
  }

  @override
  Future<bool> stopPeerDiscovery() async {
    final result = await methodChannel.invokeMethod<bool>('stopPeerDiscovery');
    return result ?? false;
  }

  @override
  Future<bool> connect(String deviceAddress) async {
    final result = await methodChannel.invokeMethod<bool>('connect', {
      'deviceAddress': deviceAddress,
    });
    return result ?? false;
  }

  @override
  Future<bool> cancelConnect() async {
    final result = await methodChannel.invokeMethod<bool>('cancelConnect');
    return result ?? false;
  }

  @override
  Future<bool> createGroup() async {
    final result = await methodChannel.invokeMethod<bool>('createGroup');
    return result ?? false;
  }

  @override
  Future<bool> removeGroup() async {
    final result = await methodChannel.invokeMethod<bool>('removeGroup');
    return result ?? false;
  }

  @override
  Future<WifiP2pInfo?> requestConnectionInfo() async {
    final result = await methodChannel.invokeMethod<Map>(
      'requestConnectionInfo',
    );
    if (result == null) return null;
    return WifiP2pInfo.fromJson(Map<String, dynamic>.from(result));
  }

  @override
  Future<WifiP2pGroup?> requestGroupInfo() async {
    final result = await methodChannel.invokeMethod<Map>('requestGroupInfo');
    if (result == null) return null;
    return WifiP2pGroup.fromJson(Map<String, dynamic>.from(result));
  }

  @override
  Future<List<WifiP2pDevice>> requestPeers() async {
    final result = await methodChannel.invokeMethod<List>('requestPeers');
    if (result == null) return [];
    return result
        .map((e) => WifiP2pDevice.fromJson(Map<String, dynamic>.from(e)))
        .toList();
  }

  @override
  Stream<WifiP2pState> get wifiP2pStateStream {
    _wifiP2pStateStream ??= stateEventChannel.receiveBroadcastStream().map(
      (event) => WifiP2pStateExtension.fromInt(event as int),
    );
    return _wifiP2pStateStream!;
  }

  @override
  Stream<List<WifiP2pDevice>> get peersStream {
    _peersStream ??= peersEventChannel.receiveBroadcastStream().map((event) {
      final list = event as List;
      return list
          .map((e) => WifiP2pDevice.fromJson(Map<String, dynamic>.from(e)))
          .toList();
    });
    return _peersStream!;
  }

  @override
  Stream<WifiP2pInfo> get connectionInfoStream {
    _connectionInfoStream ??= connectionInfoEventChannel
        .receiveBroadcastStream()
        .map((event) => WifiP2pInfo.fromJson(Map<String, dynamic>.from(event)));
    return _connectionInfoStream!;
  }

  @override
  Stream<WifiP2pDevice> get thisDeviceStream {
    _thisDeviceStream ??= thisDeviceEventChannel.receiveBroadcastStream().map(
      (event) => WifiP2pDevice.fromJson(Map<String, dynamic>.from(event)),
    );
    return _thisDeviceStream!;
  }

  @override
  Future<bool> startServer({int port = 8888}) async {
    final result = await methodChannel.invokeMethod<bool>('startServer', {
      'port': port,
    });
    return result ?? false;
  }

  @override
  Future<bool> stopServer() async {
    final result = await methodChannel.invokeMethod<bool>('stopServer');
    return result ?? false;
  }

  @override
  Future<bool> connectToServer(
    String hostAddress, {
    int port = 8888,
    int timeout = 5000,
  }) async {
    final result = await methodChannel.invokeMethod<bool>('connectToServer', {
      'hostAddress': hostAddress,
      'port': port,
      'timeout': timeout,
    });
    return result ?? false;
  }

  @override
  Future<bool> disconnectFromServer() async {
    final result = await methodChannel.invokeMethod<bool>(
      'disconnectFromServer',
    );
    return result ?? false;
  }

  @override
  Future<bool> sendMessage(List<int> message) async {
    final success = await methodChannel.invokeMethod<bool>('sendMessage', {
      'message': message,
    });
    return success ?? false;
  }

  @override
  Stream<List<int>> get messageStream =>
      messageEventChannel.receiveBroadcastStream().map((event) {
        if (event is List) {
          return event.cast<int>();
        } else {
          return [];
        }
      });
}
