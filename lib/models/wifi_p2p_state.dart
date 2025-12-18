/// Represents the state of Wi-Fi P2P.
enum WifiP2pState {
  /// Wi-Fi P2P is enabled.
  enabled,

  /// Wi-Fi P2P is disabled.
  disabled,
}

/// Extension methods for [WifiP2pState].
extension WifiP2pStateExtension on WifiP2pState {
  /// Creates a [WifiP2pState] from an integer value.
  static WifiP2pState fromInt(int value) {
    return WifiP2pState.values[value];
  }

  /// Converts this state to an integer value.
  int toInt() {
    return index;
  }
}
