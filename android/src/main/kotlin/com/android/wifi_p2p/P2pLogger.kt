package com.android.wifi_p2p

import android.util.Log

object P2pLogger {
    private const val TAG = "AndroidWifiP2p"

    fun d(tag: String, message: String) {
        Log.d(TAG, "[$tag] $message")
    }

    fun v(tag: String, message: String) {
        Log.v(TAG, "[$tag] $message")
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(TAG, "[$tag] ERROR: $message", throwable)
        } else {
            Log.e(TAG, "[$tag] ERROR: $message")
        }
    }
}
