/// Represents information about a Wi-Fi P2P group.
class WifiP2pInfo {
  /// Whether a group has been formed.
  final bool groupFormed;

  /// Whether this device is the group owner.
  final bool isGroupOwner;

  /// The group owner address, or null if not available.
  final String? groupOwnerAddress;

  const WifiP2pInfo({
    required this.groupFormed,
    required this.isGroupOwner,
    this.groupOwnerAddress,
  });

  /// Creates a [WifiP2pInfo] from a JSON map.
  factory WifiP2pInfo.fromJson(Map<String, dynamic> json) {
    return WifiP2pInfo(
      groupFormed: json['groupFormed'] as bool,
      isGroupOwner: json['isGroupOwner'] as bool,
      groupOwnerAddress: json['groupOwnerAddress'] as String?,
    );
  }

  /// Converts this info to a JSON map.
  Map<String, dynamic> toJson() {
    return {
      'groupFormed': groupFormed,
      'isGroupOwner': isGroupOwner,
      'groupOwnerAddress': groupOwnerAddress,
    };
  }

  @override
  String toString() {
    return 'WifiP2pInfo(groupFormed: $groupFormed, isGroupOwner: $isGroupOwner, groupOwnerAddress: $groupOwnerAddress)';
  }
}
