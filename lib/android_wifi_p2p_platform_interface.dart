import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'android_wifi_p2p_method_channel.dart';
import 'models/models.dart';

abstract class AndroidWifiP2pPlatform extends PlatformInterface {
  /// Constructs a AndroidWifiP2pPlatform.
  AndroidWifiP2pPlatform() : super(token: _token);

  static final Object _token = Object();

  static AndroidWifiP2pPlatform _instance = MethodChannelAndroidWifiP2p();

  /// The default instance of [AndroidWifiP2pPlatform] to use.
  ///
  /// Defaults to [MethodChannelAndroidWifiP2p].
  static AndroidWifiP2pPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [AndroidWifiP2pPlatform] when
  /// they register themselves.
  static set instance(AndroidWifiP2pPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<int?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  Future<String?> getDeviceName() {
    throw UnimplementedError('getDeviceName() has not been implemented.');
  }

  /// Registers the Wi-Fi P2P framework.
  /// Must be called before using any other Wi-Fi P2P methods.
  Future<void> register() {
    throw UnimplementedError('register() has not been implemented.');
  }

  /// Unregisters the Wi-Fi P2P framework.
  Future<void> unregister() {
    throw UnimplementedError('unregister() has not been implemented.');
  }

  /// Initiates peer discovery.
  Future<bool> discoverPeers() {
    throw UnimplementedError('discoverPeers() has not been implemented.');
  }

  /// Stops an ongoing peer discovery.
  Future<bool> stopPeerDiscovery() {
    throw UnimplementedError('stopPeerDiscovery() has not been implemented.');
  }

  /// Connects to a device with the specified address.
  Future<bool> connect(String deviceAddress) {
    throw UnimplementedError('connect() has not been implemented.');
  }

  /// Cancels any ongoing connection attempt.
  Future<bool> cancelConnect() {
    throw UnimplementedError('cancelConnect() has not been implemented.');
  }

  /// Creates a Wi-Fi P2P group with this device as the group owner.
  Future<bool> createGroup() {
    throw UnimplementedError('createGroup() has not been implemented.');
  }

  /// Removes the current Wi-Fi P2P group.
  Future<bool> removeGroup() {
    throw UnimplementedError('removeGroup() has not been implemented.');
  }

  /// Requests the current connection info.
  Future<WifiP2pInfo?> requestConnectionInfo() {
    throw UnimplementedError('requestConnectionInfo() has not been implemented.');
  }

  /// Requests the current group info.
  Future<WifiP2pGroup?> requestGroupInfo() {
    throw UnimplementedError('requestGroupInfo() has not been implemented.');
  }

  /// Requests the list of discovered peers.
  Future<List<WifiP2pDevice>> requestPeers() {
    throw UnimplementedError('requestPeers() has not been implemented.');
  }

  /// Stream of Wi-Fi P2P state changes.
  Stream<WifiP2pState> get wifiP2pStateStream {
    throw UnimplementedError('wifiP2pStateStream has not been implemented.');
  }

  /// Stream of peer list updates.
  Stream<List<WifiP2pDevice>> get peersStream {
    throw UnimplementedError('peersStream has not been implemented.');
  }

  /// Stream of connection info updates.
  Stream<WifiP2pInfo> get connectionInfoStream {
    throw UnimplementedError('connectionInfoStream has not been implemented.');
  }

  /// Stream of this device info updates.
  Stream<WifiP2pDevice> get thisDeviceStream {
    throw UnimplementedError('thisDeviceStream has not been implemented.');
  }

  /// Sends a message through the socket connection.
  Future<bool> sendMessage(List<int> message);

  /// Stream of incoming messages.
  Stream<List<int>> get messageStream {
    throw UnimplementedError('messageStream has not been implemented.');
  }
}
