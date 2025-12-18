# android_wifi_p2p

A Flutter plugin that provides access to Android's Wi-Fi P2P (Peer-to-Peer) API, allowing devices to connect directly to each other without an access point. This plugin supports peer discovery, connection management, group creation, and socket-based messaging.

## Features

- **Peer Discovery**: Discover nearby Wi-Fi P2P capable devices.
- **Connection Management**: Connect to and disconnect from peers.
- **Group Management**: Create and remove Wi-Fi P2P groups.
- **Socket Messaging**: Send and receive messages using standard TCP sockets.
- **Event Streams**: Real-time updates for:
  - Wi-Fi P2P state changes (enabled/disabled).
  - Discovered peers list.
  - Connection information.
  - Device information.
  - Incoming socket messages.
- **Device Info**: Retrieve the current device's name and address.

## Platform Support

- ✅ **Android** (API 24+)
- ❌ **iOS** (Not supported)
- ❌ **Web** (Not supported)

## Installation

### 1. Add dependency

Add `android_wifi_p2p` to your `pubspec.yaml`:

```yaml
dependencies:
  android_wifi_p2p: ^1.0.0
```

### 2. Android Manifest Configuration

**Crucial Step:** You must add the following permissions to your app's `android/app/src/main/AndroidManifest.xml` file. These are **not** automatically merged from the plugin.

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="your.package.name">

    <!-- Wi-Fi P2P Permissions -->
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <!-- Android 13+ (API 33+) requirement -->
    <uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES"
        android:usesPermissionFlags="neverForLocation" />

    <application ...>
        ...
    </application>
</manifest>
```

### 3. Requesting Runtime Permissions

In your Flutter app, you must request **Location** (for Android < 13) or **Nearby Devices** (for Android 13+) permissions at runtime before using the plugin. You can use the `permission_handler` package for this.

```dart
import 'package:permission_handler/permission_handler.dart';

Future<void> requestPermissions() async {
  if (await Permission.location.request().isGranted) {
    // Permission granted
  }
  // For Android 13+
  if (await Permission.nearbyWifiDevices.request().isGranted) {
    // Permission granted
  }
}
```

## Usage

Import the package:

```dart
import 'package:android_wifi_p2p/android_wifi_p2p.dart';
```

### Initialization

```dart
final wifiP2p = AndroidWifiP2p();

// Register the broadcast receiver (Must be called first)
await wifiP2p.register();
```

### Discovery & Connection

```dart
// Start discovering peers
await wifiP2p.discoverPeers();

// Listen to discovered peers
wifiP2p.peersStream.listen((peers) {
  for (var peer in peers) {
    print('Found peer: ${peer.deviceName} (${peer.deviceAddress})');
  }
});

// Connect to a peer
await wifiP2p.connect('AA:BB:CC:DD:EE:FF');
```

### Helper Methods

```dart
// Get platform version
final version = await AndroidWifiP2p.getPlatformVersion();

// Get the current device name (e.g., "Pixel 7")
final deviceName = await AndroidWifiP2p.getDeviceName();
print("My Device Name: $deviceName");
```

### Socket Communication

After a connection is established, you can communicate using sockets.

**Group Owner (Server):**
```dart
// Start the server
await wifiP2p.startServer();

// Listen for messages
wifiP2p.messageStream.listen((msg) {
  print("Received from client: $msg");
});
```

**Client:**
```dart
// Get connection info to find the group owner's address
final info = await wifiP2p.requestConnectionInfo();

if (info != null && info.groupOwnerAddress != null) {
  // Connect to the server
  await wifiP2p.connectToServer(info.groupOwnerAddress!);
  
  // Send a message
  await wifiP2p.sendMessage("Hello World!");
}
```

### Clean Up

When you are done (e.g., `dispose()` in your widget):

```dart
await wifiP2p.unregister();
```

## API Reference

### Core Methods
| Method | Description |
|--------|-------------|
| `register()` | Registers the Wi-Fi P2P broadcast receiver. |
| `unregister()` | Unregisters the receiver to release resources. |
| `discoverPeers()` | Starts searching for nearby devices. |
| `stopPeerDiscovery()` | Stops the discovery process. |
| `connect(address)` | Initiates a P2P connection to the specified address. |
| `cancelConnect()` | Cancels an ongoing connection attempt. |
| `createGroup()` | Creates a P2P group with this device as the owner. |
| `removeGroup()` | Removes the current P2P group. |
| `getDeviceName()` | Returns the current device's friendly name. |

### Socket Methods
| Method | Description |
|--------|-------------|
| `startServer({port})` | Starts a socket server on the leader device. |
| `stopServer()` | Stops the socket server. |
| `connectToServer(ip, {port})` | Connects a client to the server's IP. |
| `disconnectFromServer()` | Disconnects the socket. |
| `sendMessage(text)` | Sends a text message to the connected socket. |

### Streams
| Stream | Description |
|--------|-------------|
| `wifiP2pStateStream` | Updates on P2P enabled/disabled state. |
| `peersStream` | List of discovered peers. |
| `connectionInfoStream` | Updates on connection status and group ownership. |
| `thisDeviceStream` | Updates on this device's status. |
| `messageStream` | Incoming text messages from sockets. |

## License

MIT
