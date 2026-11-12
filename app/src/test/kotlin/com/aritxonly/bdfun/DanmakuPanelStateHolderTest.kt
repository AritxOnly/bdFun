package com.aritxonly.bdfun

import com.aritxonly.bdfun.feature.settings.application.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class DanmakuPanelStateHolderTest {
    private class Fixture : AutoCloseable {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val prefs = MemorySettingsPreferences()
        val repository = SettingsStateHolder.create(prefs, TestHistoryCodec, scope)
        val holder = DanmakuPanelStateHolder(repository.danmaku)
        override fun close() { repository.flush(); scope.cancel() }
    }
    @Test fun openingToolsKeepsDraftAndIndependentExpansionUntilCompositionIsRecreated() {
        Fixture().use { f ->
            val h = f.holder
            h.changeKeyword("draft"); h.toggleKeywords(); h.toggleList(); h.toggleKeywords()
            assertEquals(DanmakuPanelUiState(false, true, "draft"), h.state.value)
            h.toggleKeywords(); assertEquals("draft", h.state.value.keyword)
            assertEquals(DanmakuPanelUiState(), DanmakuPanelStateHolder(f.repository.danmaku).state.value)
        }
    }
    @Test fun keywordsTrimAndDeduplicateWithoutEnablingFilterAndBlankInputRemains() {
        Fixture().use { f ->
            val h = f.holder
            h.changeKeyword("   "); h.addKeyword(); assertEquals("   ", h.state.value.keyword)
            assertTrue(f.repository.danmakuKeywords.isEmpty())
            h.changeKeyword("  Hello  "); h.addKeyword()
            assertEquals(setOf("Hello"), f.repository.danmakuKeywords); assertEquals("", h.state.value.keyword)
            h.changeKeyword("Hello"); h.addKeyword(); assertEquals(setOf("Hello"), f.repository.danmakuKeywords)
            h.changeKeyword("hello"); h.addKeyword(); assertEquals(listOf("Hello", "hello"), f.repository.danmakuKeywords.toList())
            assertFalse(f.repository.danmakuKeywordFilter)
        }
    }
    @Test fun clearingWordsPreservesDraftAndAllOtherTools() {
        Fixture().use { f ->
            f.repository.danmakuKeywords = setOf("x"); f.repository.danmakuKeywordFilter = true
            f.holder.changeKeyword("unsent"); f.holder.toggleKeywords(); f.holder.toggleList()
            val before = f.holder.state.value; f.holder.clearKeywords()
            assertTrue(f.repository.danmakuKeywords.isEmpty()); assertTrue(f.repository.danmakuKeywordFilter)
            assertEquals(before, f.holder.state.value)
        }
    }
    @Test fun resetMatchesOriginalEighteenFieldsAndCollapsesBothToolsWithoutResettingUnrelatedPreferences() {
        Fixture().use { f ->
            val r = f.repository
            r.danmakuOn = false; r.danmakuScaleInline = 1.6f; r.danmakuScaleFullscreen = 1.7f
            r.danmakuSpeedPref = 1.8f; r.danmakuArea = 0.5f; r.danmakuOpacity = 0.3f; r.danmakuStroke = 4f
            r.danmakuLowRefresh = false; r.danmakuDedupe = true; r.danmakuSubtitleGuard = true; r.danmakuPortraitGuard = true
            r.danmakuBlockScroll = true; r.danmakuBlockFixed = true; r.danmakuBlockColor = true
            r.danmakuBlockAdvanced = true; r.danmakuBlockCount = true; r.danmakuKeywordFilter = true; r.danmakuKeywords = setOf("x")
            r.danmakuEnabled = false; r.danmakuScale = 1.4f; r.danmakuSpeed = 1.3f; r.hotDanmakuEnabled = true
            f.holder.toggleKeywords(); f.holder.toggleList(); f.holder.changeKeyword("draft"); f.holder.reset()
            assertEquals(DanmakuPanelUiState(), f.holder.state.value)
            assertEquals(listOf(true, 1f, 1f, 1f, 1f, 1f, 1.5f, true, false, false, false,
                false, false, false, false, false, false, emptySet<String>()),
                listOf(r.danmakuOn, r.danmakuScaleInline, r.danmakuScaleFullscreen, r.danmakuSpeedPref, r.danmakuArea,
                    r.danmakuOpacity, r.danmakuStroke, r.danmakuLowRefresh, r.danmakuDedupe, r.danmakuSubtitleGuard,
                    r.danmakuPortraitGuard, r.danmakuBlockScroll, r.danmakuBlockFixed, r.danmakuBlockColor,
                    r.danmakuBlockAdvanced, r.danmakuBlockCount, r.danmakuKeywordFilter, r.danmakuKeywords))
            assertFalse(r.danmakuEnabled); assertEquals(1.4f, r.danmakuScale); assertEquals(1.3f, r.danmakuSpeed)
            assertTrue(r.hotDanmakuEnabled)
        }
    }
    @Test fun resetWritesTheSamePreferencesSoRecreatedSettingsReadTheValues() {
        Fixture().use { f ->
            f.repository.danmakuKeywords = setOf("persisted"); f.repository.danmakuBlockScroll = true
            f.holder.reset(); f.repository.flush()
            val restored = SettingsStateHolder.create(f.prefs, TestHistoryCodec, f.scope)
            assertTrue(restored.danmakuKeywords.isEmpty()); assertFalse(restored.danmakuBlockScroll)
            assertEquals(1.5f, restored.danmakuStroke); assertTrue(restored.danmakuOn)
        }
    }
}
