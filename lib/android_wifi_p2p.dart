import 'android_wifi_p2p_platform_interface.dart';
import 'models/models.dart';

export 'models/models.dart';

/// The main interface for the Android Wi-Fi P2P plugin.
///
/// This class provides methods to interact with the Android Wi-Fi P2P (Wi-Fi Direct) API.
/// It allows for discovering peers, connecting to devices, creating groups, and transferring data
/// via standard TCP sockets.
class AndroidWifiP2p {
  /// Gets the Android SDK version of the device.
  ///
  /// This can be useful for determining if certain features (like [connect] on newer Android versions)
  /// behave differently.
  Future<int?> getPlatformVersion() {
    return AndroidWifiP2pPlatform.instance.getPlatformVersion();
  }

  /// Gets the friendly name of this device.
  ///
  /// Returns the device name (e.g., "Pixel 7") as a [String] if available.
  /// Returns `null` if the device name could not be retrieved or if the plugin hasn't been recognized yet.
  Future<String?> getDeviceName() {
    return AndroidWifiP2pPlatform.instance.getDeviceName();
  }

  /// Registers the Wi-Fi P2P framework and broadcast receiver.
  ///
  /// **Crucial**: This method must be called before attempting to use any other Wi-Fi P2P methods.
  /// It initializes the internal `WifiP2pManager` and registers a `BroadcastReceiver` to listen
  /// for system Wi-Fi P2P intent actions.
  Future<void> register() {
    return AndroidWifiP2pPlatform.instance.register();
  }

  /// Unregisters the Wi-Fi P2P framework.
  ///
  /// This should be called when the plugin is no longer needed (e.g., in `dispose()`) to
  /// release system resources and unregister the `BroadcastReceiver`.
  Future<void> unregister() {
    return AndroidWifiP2pPlatform.instance.unregister();
  }

  /// Initiates a search for nearby Wi-Fi P2P enabled devices.
  ///
  /// This method is asynchronous and returns immediately with a boolean indicating success of the *initiation*.
  /// The actual discovery happens in the background. Use [peersStream] to get the list of discovered devices.
  ///
  /// **Note**: Requires location permissions on Android 12 and below, and `NEARBY_WIFI_DEVICES` on Android 13+.
  ///
  /// Returns `true` if discovery started successfully, `false` otherwise.
  Future<bool> discoverPeers() {
    return AndroidWifiP2pPlatform.instance.discoverPeers();
  }

  /// Stops an ongoing peer discovery process.
  ///
  /// It is good practice to stop discovery after finding the desired peer or when initiating a connection,
  /// to save battery and reduce interference.
  ///
  /// Returns `true` if discovery was successfully stopped, `false` otherwise.
  Future<bool> stopPeerDiscovery() {
    return AndroidWifiP2pPlatform.instance.stopPeerDiscovery();
  }

  /// Initiates a connection to a specific device.
  ///
  /// [deviceAddress] is the MAC address of the peer device, usually obtained from [peersStream].
  ///
  /// This call initiates the protocol negotiation (WPS PBC) to form a group.
  ///
  /// Returns `true` if the connection initiation request was accepted by the framework, `false` otherwise.
  /// **Note**: A return value of `true` does not mean the devices are connected, just that the attempt started.
  /// Listen to [connectionInfoStream] for the final connection state.
  Future<bool> connect(String deviceAddress) {
    return AndroidWifiP2pPlatform.instance.connect(deviceAddress);
  }

  /// Cancels any ongoing peer-to-peer group negotiation.
  ///
  /// If a connection attempt is in progress but not yet established, this can cancel it.
  ///
  /// Returns `true` if the cancellation was successful, `false` otherwise.
  Future<bool> cancelConnect() {
    return AndroidWifiP2pPlatform.instance.cancelConnect();
  }

  /// Creates a persistent Wi-Fi P2P group with this device as the Group Owner (GO).
  ///
  /// This allows other devices to find and connect to this device directly without negotiation.
  ///
  /// Returns `true` if the group creation request was successful, `false` otherwise.
  Future<bool> createGroup() {
    return AndroidWifiP2pPlatform.instance.createGroup();
  }

  /// Removes the current Wi-Fi P2P group.
  ///
  /// If this device is the Group Owner, the group is destroyed and all clients are disconnected.
  /// If this device is a client, it disconnects from the group.
  ///
  /// Returns `true` if the group removal request was successful, `false` otherwise.
  Future<bool> removeGroup() {
    return AndroidWifiP2pPlatform.instance.removeGroup();
  }

  /// Requests the current Wi-Fi P2P connection information.
  ///
  /// Although connection updates are available via [connectionInfoStream], this method allows
  /// strictly polling the current state.
  ///
  /// Returns a [WifiP2pInfo] object if available, containing details like group owner IP and formation status.
  Future<WifiP2pInfo?> requestConnectionInfo() {
    return AndroidWifiP2pPlatform.instance.requestConnectionInfo();
  }

  /// Requests information about the current Wi-Fi P2P group.
  ///
  /// Returns a [WifiP2pGroup] object if this device is part of a group, `null` otherwise.
  /// The group object contains the network name (SSID), passphrase, and list of connected client devices.
  Future<WifiP2pGroup?> requestGroupInfo() {
    return AndroidWifiP2pPlatform.instance.requestGroupInfo();
  }

  /// Requests the most recent list of reachable peers.
  ///
  /// Although peer updates are broadcast via [peersStream], this method allows
  /// polling the latest cached list from the system.
  Future<List<WifiP2pDevice>> requestPeers() {
    return AndroidWifiP2pPlatform.instance.requestPeers();
  }

  /// Stream of global Wi-Fi P2P state changes.
  ///
  /// Emits [WifiP2pState.enabled] when the Wi-Fi P2P framework is ready and enabled,
  /// and [WifiP2pState.disabled] if it is turned off (e.g., Wi-Fi disabled in settings).
  Stream<WifiP2pState> get wifiP2pStateStream {
    return AndroidWifiP2pPlatform.instance.wifiP2pStateStream;
  }

  /// Stream of discovered peers.
  ///
  /// Emits a list of [WifiP2pDevice] whenever the system detects a change in the found devices.
  /// This typically updates after calling [discoverPeers].
  Stream<List<WifiP2pDevice>> get peersStream {
    return AndroidWifiP2pPlatform.instance.peersStream;
  }

  /// Stream of connection status updates.
  ///
  /// Emits a [WifiP2pInfo] object whenever a connection is established or torn down.
  /// Use `info.groupFormed` to check if a valid connection exists.
  Stream<WifiP2pInfo> get connectionInfoStream {
    return AndroidWifiP2pPlatform.instance.connectionInfoStream;
  }

  /// Stream of this device's own information.
  ///
  /// Emits a [WifiP2pDevice] object representing the current device when its status
  /// or name changes.
  Stream<WifiP2pDevice> get thisDeviceStream {
    return AndroidWifiP2pPlatform.instance.thisDeviceStream;
  }

  /// Sends a byte array message through the socket connection.
  ///
  /// - This works bi-directionally:
  ///     - If called by the **Group Owner**, it broadcasts the message to all connected clients.
  ///     - If called by a **Client**, it sends the message to the Group Owner.
  ///
  /// Returns `true` if the message was successfully dispatched to the socket output stream.
  Future<bool> sendMessage(List<int> message) {
    return AndroidWifiP2pPlatform.instance.sendMessage(message);
  }

  /// Stream of incoming byte array messages.
  ///
  /// Emits any data received over the socket connection as `List<int>`.
  Stream<List<int>> get messageStream {
    return AndroidWifiP2pPlatform.instance.messageStream;
  }
}
