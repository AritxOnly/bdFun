package com.aritxonly.bdfun.ui

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * 蓝牙音频检测：只用 AudioManager.getDevices（不需要 BLUETOOTH_CONNECT 等任何权限）。
 * 返回当前是否有蓝牙音频输出设备。
 */
fun hasBluetoothAudio(context: Context): Boolean {
    val am = context.getSystemService(AudioManager::class.java) ?: return false
    return runCatching {
        am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { device ->
            when (device.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                -> true
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_BLE_SPEAKER,
                -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                else -> false
            }
        }
    }.getOrDefault(false)
}

/** 订阅蓝牙设备增减，返回实时状态。 */
@Composable
fun rememberBluetoothAudioConnected(): Boolean {
    val context = LocalContext.current
    var connected by remember { mutableStateOf(hasBluetoothAudio(context)) }

    DisposableEffect(context) {
        val am = context.getSystemService(AudioManager::class.java)
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                connected = hasBluetoothAudio(context)
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                connected = hasBluetoothAudio(context)
            }
        }
        am?.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        connected = hasBluetoothAudio(context)
        onDispose {
            runCatching { am?.unregisterAudioDeviceCallback(callback) }
        }
    }
    return connected
}
