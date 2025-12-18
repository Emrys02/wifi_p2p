/// Represents a Wi-Fi P2P device.
class WifiP2pDevice {
  /// The device name.
  final String deviceName;

  /// The device MAC address.
  final String deviceAddress;

  /// The primary device type.
  final String? primaryDeviceType;

  /// The secondary device type.
  final String? secondaryDeviceType;

  /// The device status.
  final WifiP2pDeviceStatus status;

  /// Whether WPS is supported.
  final bool isWpsSupported;

  /// The WPS configuration methods.
  final int wpsConfigMethods;

  const WifiP2pDevice({
    required this.deviceName,
    required this.deviceAddress,
    required this.primaryDeviceType,
    required this.secondaryDeviceType,
    required this.status,
    required this.isWpsSupported,
    required this.wpsConfigMethods,
  });

  /// Creates a [WifiP2pDevice] from a JSON map.
  factory WifiP2pDevice.fromJson(Map<String, dynamic> json) {
    return WifiP2pDevice(
      deviceName: json['deviceName'] as String,
      deviceAddress: json['deviceAddress'] as String,
      primaryDeviceType: json['primaryDeviceType'] as String?,
      secondaryDeviceType: json['secondaryDeviceType'] as String?,
      status: WifiP2pDeviceStatus.values[json['status'] as int],
      isWpsSupported: json['isWpsSupported'] as bool,
      wpsConfigMethods: json['wpsConfigMethods'] as int,
    );
  }

  /// Converts this device to a JSON map.
  Map<String, dynamic> toJson() {
    return {
      'deviceName': deviceName,
      'deviceAddress': deviceAddress,
      'primaryDeviceType': primaryDeviceType,
      'secondaryDeviceType': secondaryDeviceType,
      'status': status.index,
      'isWpsSupported': isWpsSupported,
      'wpsConfigMethods': wpsConfigMethods,
    };
  }

  @override
  String toString() {
    return 'WifiP2pDevice(deviceName: $deviceName, deviceAddress: $deviceAddress, status: $status)';
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) || other is WifiP2pDevice && runtimeType == other.runtimeType && deviceAddress == other.deviceAddress;

  @override
  int get hashCode => deviceAddress.hashCode;
}

/// The status of a Wi-Fi P2P device.
enum WifiP2pDeviceStatus {
  /// Device is connected.
  connected,

  /// Device is invited.
  invited,

  /// Device connection failed.
  failed,

  /// Device is available.
  available,

  /// Device is unavailable.
  unavailable,
}
