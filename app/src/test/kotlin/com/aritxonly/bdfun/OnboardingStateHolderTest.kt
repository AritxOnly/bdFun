package com.aritxonly.bdfun

import com.aritxonly.bdfun.feature.onboarding.application.*

import com.aritxonly.bdfun.feature.settings.application.MemorySettingsPreferences
import com.aritxonly.bdfun.feature.settings.application.SettingsStateHolder
import com.aritxonly.bdfun.feature.settings.application.TestHistoryCodec
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import org.junit.Assert.*
import org.junit.Test

class OnboardingStateHolderTest {
    private class Fixture : AutoCloseable {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val prefs = MemorySettingsPreferences()
        val repository = SettingsStateHolder.create(prefs, TestHistoryCodec, scope)
        fun holder(step: Int = STEP_WELCOME) = OnboardingStateHolder(repository.appearance,
            repository.general, repository.state.map { Unit }, scope, step)
        override fun close() { scope.cancel() }
    }
    @Test fun initialStateProjectsExistingPreferencesWithoutAcknowledging() {
        Fixture().use { f ->
            f.repository.darkMode = 2; f.repository.colorStyle = 1; f.repository.interfaceStyle = 1
            f.repository.bottomBarRaise = false; f.repository.scrollTopEnabled = false; f.repository.blurEnabled = false
            val state = f.holder().state.value
            assertEquals(OnboardingBasicSettings(2, 1, 1, false, false, false), state.settings)
            assertEquals(STEP_WELCOME, state.step)
            assertFalse(state.accepted); assertFalse(state.termsViewed); assertFalse(state.showTerms)
            assertFalse(state.backwards); assertFalse(f.repository.onboardingAcked)
        }
    }
    @Test fun stepDirectionAndFlagsMatchOriginalGoIncludingSameStep() {
        Fixture().use { f ->
            val holder = f.holder()
            holder.go(STEP_NOTICE); assertFalse(holder.state.value.backwards)
            holder.setAccepted(true); holder.openTerms(); holder.dismissTerms()
            holder.go(STEP_BASIC); assertTrue(holder.state.value.accepted); assertTrue(holder.state.value.termsViewed)
            assertFalse(holder.back()); assertEquals(STEP_NOTICE, holder.state.value.step)
            assertTrue(holder.state.value.backwards)
            holder.go(STEP_NOTICE); assertFalse(holder.state.value.backwards)
        }
    }
    @Test fun backDismissesTermsBeforeGoingBackAndOnlyWelcomeExits() {
        Fixture().use { f ->
            val holder = f.holder(); holder.go(STEP_NOTICE); holder.openTerms()
            assertFalse(holder.back()); assertEquals(STEP_NOTICE, holder.state.value.step)
            assertFalse(holder.state.value.showTerms); assertTrue(holder.state.value.termsViewed)
            assertFalse(holder.back()); assertEquals(STEP_WELCOME, holder.state.value.step)
            val welcome = holder.state.value
            assertTrue(holder.back()); assertEquals(welcome, holder.state.value)
        }
    }
    @Test fun openingTermsMarksViewedImmediatelyWithoutAccepting() {
        Fixture().use { f ->
            val holder = f.holder(); holder.openTerms()
            assertTrue(holder.state.value.termsViewed); assertTrue(holder.state.value.showTerms)
            assertFalse(holder.state.value.accepted)
            holder.dismissTerms(); assertTrue(holder.state.value.termsViewed)
            assertFalse(holder.state.value.showTerms)
        }
    }
    @Test fun consentAndGoDoNotIntroduceTermsReadingOrScrollRequirement() {
        Fixture().use { f ->
            val holder = f.holder(); holder.setAccepted(true)
            assertFalse(holder.state.value.termsViewed)
            holder.setAccepted(false); holder.go(STEP_BASIC)
            assertEquals(STEP_BASIC, holder.state.value.step)
            assertFalse(holder.state.value.accepted)
            assertFalse(f.repository.onboardingAcked)
        }
    }
    @Test fun restoringOnlyStepResetsTransientFlagsAndRetainsPersistentSettings() {
        Fixture().use { f ->
            val original = f.holder(); original.setAccepted(true); original.openTerms()
            original.go(STEP_DONE); original.go(STEP_BASIC)
            original.setDarkMode(2); val saved = original.state.value.step
            original.close(); val restored = f.holder(saved).state.value
            assertEquals(STEP_BASIC, restored.step); assertEquals(2, restored.settings.darkMode)
            assertFalse(restored.accepted); assertFalse(restored.termsViewed)
            assertFalse(restored.showTerms); assertFalse(restored.backwards)
        }
    }
    @Test fun restorationDoesNotClampPreviouslySavedStep() {
        Fixture().use { f ->
            val holder = f.holder(99); assertEquals(99, holder.state.value.step)
            assertFalse(holder.back()); assertEquals(98, holder.state.value.step)
            val negative = f.holder(-1); assertTrue(negative.back()); assertEquals(-1, negative.state.value.step)
        }
    }
    @Test fun allSixControlsWriteExistingGroupsAndRetainTheirBounds() {
        Fixture().use { f ->
            val holder = f.holder()
            holder.setDarkMode(9); holder.setColorStyle(8); holder.setInterfaceStyle(7)
            holder.setBottomBarRaise(false); holder.setScrollTopEnabled(false); holder.setBlurEnabled(false)
            assertEquals(9, f.repository.darkMode); assertEquals(1, f.repository.colorStyle)
            assertEquals(1, f.repository.interfaceStyle)
            assertEquals(OnboardingBasicSettings(9, 1, 1, false, false, false), holder.state.value.settings)
            assertFalse(f.repository.onboardingAcked)
        }
    }
    @Test fun externalPreferenceChangesRefreshVisibleValues() {
        Fixture().use { f ->
            val holder = f.holder()
            f.repository.darkMode = 2; f.repository.colorStyle = 1; f.repository.interfaceStyle = 1
            f.repository.bottomBarRaise = false; f.repository.scrollTopEnabled = false; f.repository.blurEnabled = false
            assertEquals(OnboardingBasicSettings(2, 1, 1, false, false, false), holder.state.value.settings)
        }
    }
    @Test fun acknowledgmentKeepsRepeatedCompletionAllowedAndVersionOwnedByActivity() {
        Fixture().use { f ->
            f.repository.termsAckedVersion = "before"
            val holder = f.holder(STEP_DONE)
            assertTrue(holder.acknowledge()); assertTrue(holder.acknowledge())
            assertTrue(f.repository.onboardingAcked)
            assertEquals("before", f.repository.termsAckedVersion)
        }
    }
    @Test fun closedControllerIgnoresFutureNavigationSettingsAndAcknowledgment() {
        Fixture().use { f ->
            val holder = f.holder(); holder.close(); val before = holder.state.value
            val writes = f.prefs.applies
            holder.go(STEP_DONE); holder.setAccepted(true); holder.openTerms(); holder.dismissTerms()
            holder.setDarkMode(2); holder.setColorStyle(1); holder.setInterfaceStyle(1)
            holder.setBottomBarRaise(false); holder.setScrollTopEnabled(false); holder.setBlurEnabled(false)
            assertFalse(holder.back()); assertFalse(holder.acknowledge())
            assertEquals(before, holder.state.value); assertEquals(writes, f.prefs.applies)
            f.repository.darkMode = 2; assertEquals(before, holder.state.value)
        }
    }
}
