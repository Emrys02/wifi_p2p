package com.android.wifi_p2p

import android.os.Handler
import android.os.Looper
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

class SocketManager(private val messageCallback: (ByteArray) -> Unit) {
    companion object {
        const val DEFAULT_PORT = 8888
        const val TAG = "SocketManager"
        const val BUFFER_SIZE = 1024
    }

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private val connectedClients = CopyOnWriteArrayList<Socket>()
    private var isServerRunning = false
    private var isClientRunning = false
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Start a socket server (for group owner)
     */
    fun startServer(port: Int = DEFAULT_PORT): Boolean {
        if (isServerRunning) {
            P2pLogger.d(TAG, "Server already running")
            return false
        }

        try {
            serverSocket = ServerSocket(port)
            isServerRunning = true
            P2pLogger.d(TAG, "Server started on port $port")

            thread {
                try {
                    while (isServerRunning) {
                        P2pLogger.v(TAG, "Waiting for client connection...")
                        val client = serverSocket?.accept() ?: break
                        P2pLogger.d(TAG, "Client connected: ${client.inetAddress.hostAddress}")
                        connectedClients.add(client)
                        handleClient(client)
                    }
                } catch (e: Exception) {
                    if (isServerRunning) {
                        P2pLogger.e(TAG, "Error accepting client", e)
                    } else {
                        P2pLogger.d(TAG, "Server socket closed normally")
                    }
                }
            }

            return true
        } catch (e: Exception) {
            P2pLogger.e(TAG, "Error starting server on port $port", e)
            return false
        }
    }

    /**
     * Stop the socket server
     */
    fun stopServer() {
        P2pLogger.d(TAG, "Stopping server")
        isServerRunning = false
        try {
            serverSocket?.close()
            for (client in connectedClients) {
                if (!client.isClosed) {
                    try {
                        client.close()
                    } catch (e: Exception) {
                        P2pLogger.e(TAG, "Error closing client socket", e)
                    }
                }
            }
            connectedClients.clear()
            P2pLogger.d(TAG, "Server stopped")
        } catch (e: Exception) {
            P2pLogger.e(TAG, "Error stopping server", e)
        }
        serverSocket = null
    }

    /**
     * Connect to a socket server (for client)
     */
    fun connectToServer(
        hostAddress: String,
        port: Int = DEFAULT_PORT,
        timeout: Int = 5000,
        callback: (Boolean) -> Unit
    ) {
        if (isClientRunning) {
            P2pLogger.d(TAG, "Client already running")
            callback(false)
            return
        }

        thread {
            var retryCount = 0
            val maxRetries = 5
            var connected = false

            while (retryCount < maxRetries && !connected) {
                try {
                    P2pLogger.d(TAG, "Connecting to server at $hostAddress:$port (Attempt ${retryCount + 1})")
                    clientSocket = Socket()
                    clientSocket?.connect(InetSocketAddress(hostAddress, port), timeout)
                    isClientRunning = true
                    connected = true
                    P2pLogger.d(TAG, "Connected to server")

                    // Start reading thread
                    thread {
                        try {
                            val inputStream = clientSocket?.getInputStream()
                            val buffer = ByteArray(BUFFER_SIZE)
                            while (isClientRunning) {
                                val bytesRead = inputStream?.read(buffer) ?: -1
                                if (bytesRead == -1) break
                                
                                val data = buffer.copyOf(bytesRead)
                                P2pLogger.d(TAG, "Received ${bytesRead} bytes")
                                mainHandler.post {
                                    messageCallback(data)
                                }
                            }
                        } catch (e: java.net.SocketException) {
                            P2pLogger.d(TAG, "Connection closed: ${e.message}")
                        } catch (e: Exception) {
                            if (isClientRunning) {
                                P2pLogger.e(TAG, "Error reading from server", e)
                            }
                        } finally {
                            disconnectFromServer()
                        }
                    }

                    mainHandler.post { callback(true) }
                } catch (e: Exception) {
                    P2pLogger.e(TAG, "Error connecting to server (Attempt ${retryCount + 1})", e)
                    retryCount++
                    if (retryCount < maxRetries) {
                        try {
                            Thread.sleep(1000) // Wait 1 second before retrying
                        } catch (ie: InterruptedException) {
                            P2pLogger.e(TAG, "Retry sleep interrupted", ie)
                        }
                    }
                }
            }

            if (!connected) {
                P2pLogger.e(TAG, "Failed to connect to server after $maxRetries attempts")
                disconnectFromServer()
                mainHandler.post { callback(false) }
            }
        }
    }

    /**
     * Disconnect from the socket server
     */
    fun disconnectFromServer() {
        P2pLogger.d(TAG, "Disconnecting from server")
        isClientRunning = false
        try {
            clientSocket?.close()
            P2pLogger.d(TAG, "Disconnected from server")
        } catch (e: Exception) {
            P2pLogger.e(TAG, "Error disconnecting from server", e)
        }
        clientSocket = null
    }

    /**
     * Send a message (handles both server and client modes)
     */
    fun sendMessage(data: ByteArray): Boolean {
        P2pLogger.d(TAG, "Sending ${data.size} bytes")
        thread {
            try {
                if (isServerRunning) {
                    P2pLogger.v(TAG, "Broadcasting to ${connectedClients.size} clients")
                    for (client in connectedClients) {
                        try {
                            val outputStream = client.getOutputStream()
                            outputStream.write(data)
                            outputStream.flush()
                        } catch (e: Exception) {
                            P2pLogger.e(TAG, "Error sending to client ${client.inetAddress.hostAddress}", e)
                        }
                    }
                } else if (isClientRunning && clientSocket != null) {
                    val outputStream = clientSocket!!.getOutputStream()
                    outputStream.write(data)
                    outputStream.flush()
                } else {
                    P2pLogger.d(TAG, "Cannot send message: No connection")
                }
            } catch (e: Exception) {
                P2pLogger.e(TAG, "Error sending message", e)
            }
        }
        return true
    }

    /**
     * Handle a connected client in server mode
     */
    private fun handleClient(client: Socket) {
        thread {
            try {
                val inputStream = client.getInputStream()
                val buffer = ByteArray(BUFFER_SIZE)

                while (isServerRunning && !client.isClosed) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break
                    
                    val data = buffer.copyOf(bytesRead)
                    P2pLogger.d(TAG, "Received ${bytesRead} bytes from client ${client.inetAddress.hostAddress}")
                    mainHandler.post {
                        messageCallback(data)
                    }
                }
            } catch (e: java.net.SocketException) {
                P2pLogger.d(TAG, "Client disconnected: ${e.message}")
            } catch (e: Exception) {
                P2pLogger.e(TAG, "Error handling client", e)
            } finally {
                try {
                    client.close()
                } catch (e: Exception) {
                    P2pLogger.e(TAG, "Error closing client socket in finally block", e)
                }
                connectedClients.remove(client)
                P2pLogger.d(TAG, "Client handler finished")
            }
        }
    }

    /**
     * Clean up all resources
     */
    fun cleanup() {
        P2pLogger.d(TAG, "Cleaning up SocketManager")
        stopServer()
        disconnectFromServer()
    }
}
