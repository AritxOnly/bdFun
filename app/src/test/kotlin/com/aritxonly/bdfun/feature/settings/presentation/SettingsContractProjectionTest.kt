package com.aritxonly.bdfun.feature.settings.presentation

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import com.aritxonly.bdfun.core.domain.SettingsRepository
import com.aritxonly.bdfun.feature.settings.application.MemorySettingsPreferences
import com.aritxonly.bdfun.feature.settings.application.SettingsStateHolder
import com.aritxonly.bdfun.feature.settings.application.TestHistoryCodec
import com.aritxonly.teak.common.chrome.TeakMotion
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class SettingsContractProjectionTest {
    private fun fixture(block: (SettingsBindings, SettingsStateHolder, MemorySettingsPreferences) -> Unit) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val prefs = MemorySettingsPreferences()
        val controller = SettingsStateHolder.create(prefs, TestHistoryCodec, scope)
        val motion = TeakMotion.enabled
        val bindings = SettingsBindings(scope, controller)
        try { block(bindings, controller, prefs) }
        finally { bindings.close(); scope.cancel(); TeakMotion.enabled = motion }
    }

    @Test fun groupsKeepPresentationProjectionWhileApplicationRepositoryRemainsPure() = fixture { view, controller, _ ->
        val settings: SettingsRepository = view
        assertSame(view, settings.appearance); assertSame(view, settings.playback)
        assertSame(view, settings.danmaku); assertSame(view, settings.message); assertSame(view, settings.general)
        assertSame(controller, view.repository); assertNotSame(view, view.repository)
    }

    @Test fun groupedDanmakuReadIsObservedAndUnrelatedSettingDoesNotInvalidateIt() = fixture { view, controller, _ ->
        var changed = 0
        val observer = SnapshotStateObserver { it() }
        observer.start()
        try {
            observer.observeReads(Any(), { changed++ }) { assertTrue(view.danmaku.danmakuOn) }
            controller.backgroundPlay = false
            Snapshot.sendApplyNotifications(); assertEquals(0, changed)
            controller.danmakuOn = false
            Snapshot.sendApplyNotifications(); assertEquals(1, changed)
            assertFalse(view.danmaku.danmakuOn)
        } finally { observer.stop(); observer.clear() }
    }

    @Test fun flatContractReadKeepsComposeObservation() = fixture { view, controller, _ ->
        val settings: SettingsRepository = view
        var changed = 0
        val observer = SnapshotStateObserver { it() }
        observer.start()
        try {
            observer.observeReads(Any(), { changed++ }) { assertTrue(settings.resumePlayback) }
            controller.resumePlayback = false
            Snapshot.sendApplyNotifications()
            assertEquals(1, changed); assertFalse(settings.resumePlayback)
        } finally { observer.stop(); observer.clear() }
    }

    @Test fun callbacksReadLatestValueAndWritesReachControllerSynchronously() = fixture { view, controller, _ ->
        val settings: SettingsRepository = view
        val toggle = { settings.danmakuOn = !settings.danmakuOn }
        controller.danmakuOn = false
        toggle(); assertTrue(controller.danmakuOn); assertTrue(settings.danmakuOn)
        toggle(); assertFalse(controller.danmakuOn); assertFalse(settings.danmakuOn)
        settings.playback.backgroundPlay = false
        assertFalse(controller.backgroundPlay); assertFalse(settings.backgroundPlay)
    }

    @Test fun typedFloatWriteIsImmediateAndCloseFlushesOriginalStorage() = fixture { view, controller, prefs ->
        view.playback.defaultPlaybackSpeed = 1.5f
        assertEquals(1.5f, controller.defaultPlaybackSpeed)
        assertEquals(1.5f, view.defaultPlaybackSpeed)
        view.close()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val restored = SettingsStateHolder.create(prefs, TestHistoryCodec, scope)
            assertEquals(1.5f, restored.defaultPlaybackSpeed)
        } finally { scope.cancel() }
    }
}
