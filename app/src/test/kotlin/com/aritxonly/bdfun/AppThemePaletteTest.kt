package com.aritxonly.bdfun

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class AppThemePaletteTest {
    @Test fun separateContainersReadTheirOwnSettingsWithoutCopyingGlobalStyle() {
        var firstStyle = 0
        val first = AppThemePalette { firstStyle }
        val second = AppThemePalette { 1 }
        assertEquals(Color(0xFFFB7299), first.brand)
        assertEquals(Color(0xFF3482FF), second.brand)
        firstStyle = 1
        assertEquals(second.brand, first.brand)
        firstStyle = 0
        assertEquals(Color(0xFFE45C82), first.brandDeep)
        assertEquals(Color(0xFF1D63D8), second.brandDeep)
    }
    @Test fun settingsChangeInvalidatesComposeReaderWithoutWritingAnotherStyleState() {
        val style = mutableIntStateOf(0)
        val palette = AppThemePalette { style.intValue }
        var invalidations = 0
        val observer = SnapshotStateObserver { it() }
        observer.start()
        try {
            observer.observeReads(Unit, { invalidations++ }) { assertEquals(Color(0xFFFB7299), palette.brand) }
            style.intValue = 1
            Snapshot.sendApplyNotifications()
            assertEquals(1, invalidations)
            assertEquals(Color(0xFF3482FF), palette.brand)
        } finally { observer.stop(); observer.clear() }
    }
}
