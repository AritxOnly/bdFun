package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable

@Composable
internal fun rememberPlayerBluetoothLatency(): Int {
    val settings = LocalVideoPageState.current.settings
    val connected = rememberBluetoothAudioConnected()
    return if (settings.bluetoothLatencyEnabled && connected) settings.bluetoothLatencyMs else 0
}
