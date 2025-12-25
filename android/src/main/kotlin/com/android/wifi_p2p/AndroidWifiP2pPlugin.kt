package com.android.wifi_p2p

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.*
import android.os.Build
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

class AndroidWifiP2pPlugin :
    FlutterPlugin,
    MethodCallHandler {
    companion object {
        const val TAG = "Plugin"
    }

    private lateinit var channel: MethodChannel
    private lateinit var stateEventChannel: EventChannel
    private lateinit var peersEventChannel: EventChannel
    private lateinit var connectionInfoEventChannel: EventChannel
    private lateinit var thisDeviceEventChannel: EventChannel
    private lateinit var messageEventChannel: EventChannel

    private var context: Context? = null
    private var wifiP2pManager: WifiP2pManager? = null
    private var wifiP2pChannel: WifiP2pManager.Channel? = null
    private var broadcastReceiver: WifiP2pBroadcastReceiver? = null
    private var socketManager: SocketManager? = null

    private var thisDevice: WifiP2pDevice? = null

    private var stateEventSink: EventChannel.EventSink? = null
    private var peersEventSink: EventChannel.EventSink? = null
    private var connectionInfoEventSink: EventChannel.EventSink? = null
    private var thisDeviceEventSink: EventChannel.EventSink? = null
    private var messageEventSink: EventChannel.EventSink? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        P2pLogger.d(TAG, "onAttachedToEngine")
        context = flutterPluginBinding.applicationContext

        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "android_wifi_p2p")
        channel.setMethodCallHandler(this)

        stateEventChannel =
            EventChannel(flutterPluginBinding.binaryMessenger, "android_wifi_p2p/state")
        stateEventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(
                arguments: Any?,
                events: EventChannel.EventSink?
            ) {
                P2pLogger.d(TAG, "stateEventChannel onListen")
                stateEventSink = events
            }

            override fun onCancel(arguments: Any?) {
                P2pLogger.d(TAG, "stateEventChannel onCancel")
                stateEventSink = null
            }
        })

        peersEventChannel =
            EventChannel(flutterPluginBinding.binaryMessenger, "android_wifi_p2p/peers")
        peersEventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(
                arguments: Any?,
                events: EventChannel.EventSink?
            ) {
                P2pLogger.d(TAG, "peersEventChannel onListen")
                peersEventSink = events
            }

            override fun onCancel(arguments: Any?) {
                P2pLogger.d(TAG, "peersEventChannel onCancel")
                peersEventSink = null
            }
        })

        connectionInfoEventChannel =
            EventChannel(
                flutterPluginBinding.binaryMessenger,
                "android_wifi_p2p/connection_info"
            )
        connectionInfoEventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(
                arguments: Any?,
                events: EventChannel.EventSink?
            ) {
                P2pLogger.d(TAG, "connectionInfoEventChannel onListen")
                connectionInfoEventSink = events
            }

            override fun onCancel(arguments: Any?) {
                P2pLogger.d(TAG, "connectionInfoEventChannel onCancel")
                connectionInfoEventSink = null
            }
        })

        thisDeviceEventChannel =
            EventChannel(
                flutterPluginBinding.binaryMessenger,
                "android_wifi_p2p/this_device"
            )
        thisDeviceEventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(
                arguments: Any?,
                events: EventChannel.EventSink?
            ) {
                P2pLogger.d(TAG, "thisDeviceEventChannel onListen")
                thisDeviceEventSink = events
            }

            override fun onCancel(arguments: Any?) {
                P2pLogger.d(TAG, "thisDeviceEventChannel onCancel")
                thisDeviceEventSink = null
            }
        })

        messageEventChannel =
            EventChannel(
                flutterPluginBinding.binaryMessenger,
                "android_wifi_p2p/message"
            )
        messageEventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(
                arguments: Any?,
                events: EventChannel.EventSink?
            ) {
                P2pLogger.d(TAG, "messageEventChannel onListen")
                messageEventSink = events
            }

            override fun onCancel(arguments: Any?) {
                P2pLogger.d(TAG, "messageEventChannel onCancel")
                messageEventSink = null
            }
        })

        // Initialize socket manager
        socketManager = SocketManager { data ->
            P2pLogger.d(TAG, "Received message from SocketManager: ${data.size} bytes")
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                messageEventSink?.success(data)
            }
        }
    }

    override fun onMethodCall(
        call: MethodCall,
        result: Result
    ) {
        P2pLogger.d(TAG, "onMethodCall: ${call.method}, args: ${call.arguments}")
        when (call.method) {
            "getPlatformVersion" -> {
                val version = Build.VERSION.SDK_INT
                P2pLogger.d(TAG, "getPlatformVersion result: $version")
                result.success(version)
            }

            "getDeviceName" -> {
                val deviceName = thisDevice?.deviceName
                P2pLogger.d(TAG, "getDeviceName result: $deviceName")
                result.success(deviceName)
            }

            "register" -> {
                register()
                result.success(null)
            }

            "unregister" -> {
                unregister()
                result.success(null)
            }

            "discoverPeers" -> {
                discoverPeers(result)
            }

            "stopPeerDiscovery" -> {
                stopPeerDiscovery(result)
            }

            "connect" -> {
                val deviceAddress = call.argument<String>("deviceAddress")
                if (deviceAddress != null) {
                    connect(deviceAddress, result)
                } else {
                    P2pLogger.e(TAG, "connect failed: Device address is required")
                    result.error("INVALID_ARGUMENT", "Device address is required", null)
                }
            }

            "cancelConnect" -> {
                cancelConnect(result)
            }

            "createGroup" -> {
                createGroup(result)
            }

            "removeGroup" -> {
                removeGroup(result)
            }

            "requestConnectionInfo" -> {
                requestConnectionInfo(result)
            }

            "requestGroupInfo" -> {
                requestGroupInfo(result)
            }

            "requestPeers" -> {
                requestPeers(result)
            }

            "startServer" -> {
                val port = call.argument<Int>("port") ?: SocketManager.DEFAULT_PORT
                startServer(port, result)
            }

            "stopServer" -> {
                stopServer(result)
            }

            "connectToServer" -> {
                val hostAddress = call.argument<String>("hostAddress")
                val port = call.argument<Int>("port") ?: SocketManager.DEFAULT_PORT
                val timeout = call.argument<Int>("timeout") ?: 5000
                if (hostAddress != null) {
                    connectToServer(hostAddress, port, timeout, result)
                } else {
                    P2pLogger.e(TAG, "connectToServer failed: Host address is required")
                    result.error("INVALID_ARGUMENT", "Host address is required", null)
                }
            }

            "disconnectFromServer" -> {
                disconnectFromServer(result)
            }

            "sendMessage" -> {
                val message = call.arguments as ByteArray
                if (message != null) {
                    sendMessage(message, result)
                } else {
                    P2pLogger.e(TAG, "sendMessage failed: Message is required")
                    result.error("INVALID_ARGUMENT", "Message is required", null)
                }
            }

            else -> {
                P2pLogger.e(TAG, "Method not implemented: ${call.method}")
                result.notImplemented()
            }
        }
    }

    private fun register() {
        P2pLogger.d(TAG, "registering Wi-Fi P2P")
        val ctx = context ?: return
        wifiP2pManager = ctx.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        wifiP2pChannel = wifiP2pManager?.initialize(ctx, ctx.mainLooper, null)

        val intentFilter =
            IntentFilter().apply {
                addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            }

        broadcastReceiver =
            WifiP2pBroadcastReceiver(
                wifiP2pManager,
                wifiP2pChannel,
                this
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ctx.registerReceiver(broadcastReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            ctx.registerReceiver(broadcastReceiver, intentFilter)
        }
        P2pLogger.d(TAG, "Wi-Fi P2P registered")
    }

    private fun unregister() {
        P2pLogger.d(TAG, "unregistering Wi-Fi P2P")
        val ctx = context ?: return
        broadcastReceiver?.let {
            try {
                ctx.unregisterReceiver(it)
                P2pLogger.d(TAG, "Receiver unregistered")
            } catch (e: IllegalArgumentException) {
                P2pLogger.e(TAG, "Receiver was not registered", e)
            }
        }
        broadcastReceiver = null
        wifiP2pChannel = null
        wifiP2pManager = null
    }

    private fun discoverPeers(result: Result) {
        val manager = wifiP2pManager
        val channel = wifiP2pChannel
        if (manager == null || channel == null) {
            P2pLogger.e(TAG, "discoverPeers failed: Wi-Fi P2P not initialized")
            result.error("NOT_INITIALIZED", "Wi-Fi P2P not initialized", null)
            return
        }

        P2pLogger.d(TAG, "Initiating peer discovery")
        manager.discoverPeers(
            channel,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    P2pLogger.d(TAG, "discoverPeers onSuccess")
                    result.success(true)
                }

                override fun onFailure(reasonCode: Int) {
                    P2pLogger.e(TAG, "discoverPeers onFailure: $reasonCode")
                    result.success(false)
                }
            }
        )
    }

    private fun stopPeerDiscovery(result: Result) {
        val manager = wifiP2pManager
        val channel = wifiP2pChannel
        if (manager == null || channel == null) {
            P2pLogger.e(TAG, "stopPeerDiscovery failed: Wi-Fi P2P not initialized")
            result.error("NOT_INITIALIZED", "Wi-Fi P2P not initialized", null)
            return
        }

        P2pLogger.d(TAG, "Stopping peer discovery")
        manager.stopPeerDiscovery(
            channel,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    P2pLogger.d(TAG, "stopPeerDiscovery onSuccess")
                    result.success(true)
                }

                override fun onFailure(reasonCode: Int) {
                    P2pLogger.e(TAG, "stopPeerDiscovery onFailure: $reasonCode")
                    result.success(false)
                }
            }
        )
    }

    private fun connect(
        deviceAddress: String,
        result: Result
    ) {
        val manager = wifiP2pManager
        val channel = wifiP2pChannel
        if (manager == null || channel == null) {
            P2pLogger.e(TAG, "connect failed: Wi-Fi P2P not initialized")
            result.error("NOT_INITIALIZED", "Wi-Fi P2P not initialized", null)
            return
        }

        val config = WifiP2pConfig()
        config.deviceAddress = deviceAddress
        P2pLogger.d(TAG, "Connecting to $deviceAddress")

        manager.connect(
            channel,
            config,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    P2pLogger.d(TAG, "connect onSuccess")
                    result.success(true)
                }

                override fun onFailure(reasonCode: Int) {
                    P2pLogger.e(TAG, "connect onFailure: $reasonCode")
                    result.success(false)
                }
            }
        )
    }

    private fun cancelConnect(result: Result) {
        val manager = wifiP2pManager
        val channel = wifiP2pChannel
        if (manager == null || channel == null) {
            P2pLogger.e(TAG, "cancelConnect failed: Wi-Fi P2P not initialized")
            result.error("NOT_INITIALIZED", "Wi-Fi P2P not initialized", null)
            return
        }

        P2pLogger.d(TAG, "Cancelling connection")
        manager.cancelConnect(
            channel,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    P2pLogger.d(TAG, "cancelConnect onSuccess")
                    result.success(true)
                }

                override fun onFailure(reasonCode: Int) {
                    P2pLogger.e(TAG, "cancelConnect onFailure: $reasonCode")
                    result.success(false)
                }
            }
        )
    }

    private fun createGroup(result: Result) {
        val manager = wifiP2pManager
        val channel = wifiP2pChannel
        if (manager == null || channel == null) {
            P2pLogger.e(TAG, "createGroup failed: Wi-Fi P2P not initialized")
            result.error("NOT_INITIALIZED", "Wi-Fi P2P not initialized", null)
            return
        }

        P2pLogger.d(TAG, "Creating group")
        manager.createGroup(
            channel,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    P2pLogger.d(TAG, "createGroup onSuccess")
                    result.success(true)
                }

                override fun onFailure(reasonCode: Int) {
                    P2pLogger.e(TAG, "createGroup onFailure: $reasonCode")
                    result.success(false)
                }
            }
        )
    }

    private fun removeGroup(result: Result) {
        val manager = wifiP2pManager
        val channel = wifiP2pChannel
        if (manager == null || channel == null) {
            P2pLogger.e(TAG, "removeGroup failed: Wi-Fi P2P not initialized")
            result.error("NOT_INITIALIZED", "Wi-Fi P2P not initialized", null)
            return
        }

        P2pLogger.d(TAG, "Removing group")
        manager.removeGroup(
            channel,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    P2pLogger.d(TAG, "removeGroup onSuccess")
                    result.success(true)
                }

                override fun onFailure(reasonCode: Int) {
                    P2pLogger.e(TAG, "removeGroup onFailure: $reasonCode")
                    result.success(false)
                }
            }
        )
    }

    private fun requestConnectionInfo(result: Result) {
        val manager = wifiP2pManager
        val channel = wifiP2pChannel
        if (manager == null || channel == null) {
            P2pLogger.e(TAG, "requestConnectionInfo failed: Wi-Fi P2P not initialized")
            result.error("NOT_INITIALIZED", "Wi-Fi P2P not initialized", null)
            return
        }

        P2pLogger.d(TAG, "Requesting connection info")
        manager.requestConnectionInfo(channel) { info ->
            P2pLogger.d(TAG, "Connection info received: $info")
            if (info != null) {
                result.success(wifiP2pInfoToMap(info))
            } else {
                result.success(null)
            }
        }
    }

    private fun requestGroupInfo(result: Result) {
        val manager = wifiP2pManager
        val channel = wifiP2pChannel
        if (manager == null || channel == null) {
            P2pLogger.e(TAG, "requestGroupInfo failed: Wi-Fi P2P not initialized")
            result.error("NOT_INITIALIZED", "Wi-Fi P2P not initialized", null)
            return
        }

        P2pLogger.d(TAG, "Requesting group info")
        manager.requestGroupInfo(channel) { group ->
            P2pLogger.d(TAG, "Group info received: $group")
            if (group != null) {
                result.success(wifiP2pGroupToMap(group))
            } else {
                result.success(null)
            }
        }
    }

    private fun requestPeers(result: Result) {
        val manager = wifiP2pManager
        val channel = wifiP2pChannel
        if (manager == null || channel == null) {
            P2pLogger.e(TAG, "requestPeers failed: Wi-Fi P2P not initialized")
            result.error("NOT_INITIALIZED", "Wi-Fi P2P not initialized", null)
            return
        }

        P2pLogger.d(TAG, "Requesting peers")
        manager.requestPeers(channel) { peers ->
            P2pLogger.d(TAG, "Peers received: ${peers.deviceList.size} devices")
            val deviceList = peers.deviceList.map { wifiP2pDeviceToMap(it) }
            result.success(deviceList)
        }
    }

    fun onStateChanged(state: Int) {
        P2pLogger.d(TAG, "onStateChanged: $state")
        stateEventSink?.success(if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) 0 else 1)
    }

    fun onPeersChanged(peers: WifiP2pDeviceList) {
        P2pLogger.d(TAG, "onPeersChanged: ${peers.deviceList.size} peers")
        val deviceList = peers.deviceList.map { wifiP2pDeviceToMap(it) }
        peersEventSink?.success(deviceList)
    }

    fun onConnectionChanged(info: WifiP2pInfo) {
        P2pLogger.d(TAG, "onConnectionChanged - groupFormed: ${info.groupFormed}, isGroupOwner: ${info.isGroupOwner}, groupOwnerAddress: ${info.groupOwnerAddress?.hostAddress}")
        connectionInfoEventSink?.success(wifiP2pInfoToMap(info))

        if (info.groupFormed) {
            if (info.isGroupOwner) {
                P2pLogger.d(TAG, "Starting server as Group Owner")
                socketManager?.startServer()
            } else {
                val address = info.groupOwnerAddress?.hostAddress
                if (address != null) {
                    P2pLogger.d(TAG, "Connecting to server at $address")
                    socketManager?.connectToServer(address, SocketManager.DEFAULT_PORT, 5000) { success ->
                        P2pLogger.d(TAG, "Socket connection result: $success")
                    }
                } else {
                    P2pLogger.d(TAG, "Group owner address is null, retrying...")
                    
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        wifiP2pManager?.requestConnectionInfo(wifiP2pChannel) { retryInfo ->
                            if (retryInfo?.groupOwnerAddress != null) {
                                P2pLogger.d(TAG, "Retry successful, connecting to ${retryInfo.groupOwnerAddress.hostAddress}")
                                socketManager?.connectToServer(
                                    retryInfo.groupOwnerAddress.hostAddress,
                                    SocketManager.DEFAULT_PORT,
                                    5000
                                ) { success ->
                                    P2pLogger.d(TAG, "Socket connection result (retry): $success")
                                }
                            } else {
                                P2pLogger.e(TAG, "Retry failed: Group owner address still null")
                            }
                        }
                    }, 1000)
                }
            }
        } else {
            P2pLogger.d(TAG, "Group not formed, cleaning up socket manager")
            socketManager?.cleanup()
        }
    }


    fun onThisDeviceChanged(device: WifiP2pDevice) {
        P2pLogger.d(TAG, "onThisDeviceChanged: ${device.deviceName}")
        thisDevice = device
        thisDeviceEventSink?.success(wifiP2pDeviceToMap(device))
    }

    private fun wifiP2pDeviceToMap(device: WifiP2pDevice): Map<String, Any?> {
        return mapOf(
            "deviceName" to device.deviceName,
            "deviceAddress" to device.deviceAddress,
            "primaryDeviceType" to device.primaryDeviceType,
            "secondaryDeviceType" to device.secondaryDeviceType,
            "status" to device.status,
            "isWpsSupported" to (device.wpsPbcSupported() || device.wpsKeypadSupported() || device.wpsDisplaySupported()),
            "wpsConfigMethods" to 0
        )
    }

    private fun wifiP2pInfoToMap(info: WifiP2pInfo): Map<String, Any?> {
        return mapOf(
            "groupFormed" to info.groupFormed,
            "isGroupOwner" to info.isGroupOwner,
            "groupOwnerAddress" to info.groupOwnerAddress?.hostAddress
        )
    }

    private fun wifiP2pGroupToMap(group: WifiP2pGroup): Map<String, Any?> {
        return mapOf(
            "networkName" to group.networkName,
            "passphrase" to group.passphrase,
            "isGroupOwner" to group.isGroupOwner,
            "owner" to group.owner?.let { wifiP2pDeviceToMap(it) },
            "clientList" to group.clientList.map { wifiP2pDeviceToMap(it) },
            "interfaceName" to group.`interface`
        )
    }

    private fun startServer(
        port: Int,
        result: Result
    ) {
        val manager = socketManager
        if (manager == null) {
            P2pLogger.e(TAG, "startServer failed: Socket manager not initialized")
            result.error("NOT_INITIALIZED", "Socket manager not initialized", null)
            return
        }

        val success = manager.startServer(port)
        result.success(success)
    }

    private fun stopServer(result: Result) {
        val manager = socketManager
        if (manager == null) {
            P2pLogger.e(TAG, "stopServer failed: Socket manager not initialized")
            result.error("NOT_INITIALIZED", "Socket manager not initialized", null)
            return
        }

        manager.stopServer()
        result.success(true)
    }

    private fun connectToServer(
        hostAddress: String,
        port: Int,
        timeout: Int,
        result: Result
    ) {
        val manager = socketManager
        if (manager == null) {
            P2pLogger.e(TAG, "connectToServer failed: Socket manager not initialized")
            result.error("NOT_INITIALIZED", "Socket manager not initialized", null)
            return
        }

        manager.connectToServer(hostAddress, port, timeout) { success ->
            result.success(success)
        }
    }

    private fun disconnectFromServer(result: Result) {
        val manager = socketManager
        if (manager == null) {
            P2pLogger.e(TAG, "disconnectFromServer failed: Socket manager not initialized")
            result.error("NOT_INITIALIZED", "Socket manager not initialized", null)
            return
        }

        manager.disconnectFromServer()
        result.success(true)
    }

    private fun sendMessage(
        message: ByteArray,
        result: Result
    ) {
        val manager = socketManager
        if (manager == null) {
            P2pLogger.e(TAG, "sendMessage failed: Socket manager not initialized")
            result.error("NOT_INITIALIZED", "Socket manager not initialized", null)
            return
        }

        val success = manager.sendMessage(message)
        result.success(success)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        P2pLogger.d(TAG, "onDetachedFromEngine")
        unregister()
        socketManager?.destroy()
        socketManager = null
        channel.setMethodCallHandler(null)
        stateEventChannel.setStreamHandler(null)
        peersEventChannel.setStreamHandler(null)
        connectionInfoEventChannel.setStreamHandler(null)
        thisDeviceEventChannel.setStreamHandler(null)
        messageEventChannel.setStreamHandler(null)
    }
}

class WifiP2pBroadcastReceiver(
    private val manager: WifiP2pManager?,
    private val channel: WifiP2pManager.Channel?,
    private val plugin: AndroidWifiP2pPlugin
) : BroadcastReceiver() {
    companion object {
        const val TAG = "BroadcastReceiver"
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        P2pLogger.d(TAG, "onReceive: ${intent.action}")
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                P2pLogger.d(TAG, "WIFI_P2P_STATE_CHANGED_ACTION: $state")
                plugin.onStateChanged(state)
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                P2pLogger.d(TAG, "WIFI_P2P_PEERS_CHANGED_ACTION")
                manager?.requestPeers(channel) { peers ->
                    plugin.onPeersChanged(peers)
                }
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                P2pLogger.d(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION")
                manager?.requestConnectionInfo(channel) { info ->
                    if (info != null) {
                        plugin.onConnectionChanged(info)
                    }
                }
            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                P2pLogger.d(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION")
                val device: WifiP2pDevice? =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            WifiP2pManager.EXTRA_WIFI_P2P_DEVICE,
                            WifiP2pDevice::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                    }
                device?.let { plugin.onThisDeviceChanged(it) }
            }
        }
    }
}
