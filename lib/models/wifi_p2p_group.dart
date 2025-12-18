import 'wifi_p2p_device.dart';

/// Represents a Wi-Fi P2P group.
class WifiP2pGroup {
  /// The network name.
  final String networkName;

  /// The passphrase.
  final String? passphrase;

  /// Whether this device is the group owner.
  final bool isGroupOwner;

  /// The group owner device.
  final WifiP2pDevice? owner;

  /// The list of client devices.
  final List<WifiP2pDevice> clientList;

  /// The network interface name.
  final String? interfaceName;

  const WifiP2pGroup({
    required this.networkName,
    this.passphrase,
    required this.isGroupOwner,
    this.owner,
    required this.clientList,
    this.interfaceName,
  });

  /// Creates a [WifiP2pGroup] from a JSON map.
  factory WifiP2pGroup.fromJson(Map<String, dynamic> json) {
    final clientListJson = json['clientList'] as List<dynamic>?;
    final clientList = clientListJson
            ?.map((e) => WifiP2pDevice.fromJson(e as Map<String, dynamic>))
            .toList() ??
        [];

    final ownerJson = json['owner'] as Map<String, dynamic>?;
    final owner =
        ownerJson != null ? WifiP2pDevice.fromJson(ownerJson) : null;

    return WifiP2pGroup(
      networkName: json['networkName'] as String,
      passphrase: json['passphrase'] as String?,
      isGroupOwner: json['isGroupOwner'] as bool,
      owner: owner,
      clientList: clientList,
      interfaceName: json['interfaceName'] as String?,
    );
  }

  /// Converts this group to a JSON map.
  Map<String, dynamic> toJson() {
    return {
      'networkName': networkName,
      'passphrase': passphrase,
      'isGroupOwner': isGroupOwner,
      'owner': owner?.toJson(),
      'clientList': clientList.map((e) => e.toJson()).toList(),
      'interfaceName': interfaceName,
    };
  }

  @override
  String toString() {
    return 'WifiP2pGroup(networkName: $networkName, isGroupOwner: $isGroupOwner, clientCount: ${clientList.length})';
  }
}
