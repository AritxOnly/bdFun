package com.aritxonly.bdfun.feature.onboarding.application

import com.aritxonly.bdfun.core.domain.AppearanceSettings
import com.aritxonly.bdfun.core.domain.GeneralSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val STEP_WELCOME = 0
internal const val STEP_NOTICE = 1
internal const val STEP_BASIC = 2
internal const val STEP_DONE = 3

internal data class OnboardingBasicSettings(
    val darkMode: Int, val colorStyle: Int, val interfaceStyle: Int,
    val bottomBarRaise: Boolean, val scrollTopEnabled: Boolean, val blurEnabled: Boolean,
)

internal data class OnboardingUiState(
    val settings: OnboardingBasicSettings, val step: Int = STEP_WELCOME,
    val backwards: Boolean = false, val accepted: Boolean = false,
    val termsViewed: Boolean = false, val showTerms: Boolean = false,
)

/** 只保存步骤用于重建；勾选、条款与切换方向继续属于本次引导会话。 */
internal class OnboardingStateHolder(
    private val appearance: AppearanceSettings,
    private val general: GeneralSettings,
    changes: Flow<Unit>,
    scope: CoroutineScope,
    initialStep: Int = STEP_WELCOME,
) {
    private val mutable = MutableStateFlow(OnboardingUiState(settings = readSettings(), step = initialStep))
    val state = mutable.asStateFlow()
    private var closed = false
    val isActive get() = !closed
    private val observer = scope.launch { changes.collect { if (!closed) refreshSettings() } }

    fun close() { closed = true; observer.cancel() }
    fun go(next: Int) {
        if (!closed) mutable.update { it.copy(backwards = next < it.step, step = next) }
    }
    fun setAccepted(value: Boolean) {
        if (!closed) mutable.update { it.copy(accepted = value) }
    }
    fun openTerms() {
        if (!closed) mutable.update { it.copy(termsViewed = true, showTerms = true) }
    }
    fun dismissTerms() {
        if (!closed) mutable.update { it.copy(showTerms = false) }
    }
    /** true 才交给 Activity 退出；条款弹层优先于步骤返回。 */
    fun back(): Boolean {
        if (closed) return false
        val current = mutable.value
        return when {
            current.showTerms -> { dismissTerms(); false }
            current.step > STEP_WELCOME -> { go(current.step - 1); false }
            else -> true
        }
    }
    // main 由按钮 enabled 控制继续，不在步骤跳转中另加条款或勾选门槛。
    fun acknowledge(): Boolean {
        if (closed) return false
        general.onboardingAcked = true
        return true
    }
    fun setDarkMode(value: Int) = changeSetting { appearance.darkMode = value }
    fun setColorStyle(value: Int) = changeSetting { appearance.colorStyle = value }
    fun setInterfaceStyle(value: Int) = changeSetting { appearance.interfaceStyle = value }
    fun setBottomBarRaise(value: Boolean) = changeSetting { appearance.bottomBarRaise = value }
    fun setScrollTopEnabled(value: Boolean) = changeSetting { appearance.scrollTopEnabled = value }
    fun setBlurEnabled(value: Boolean) = changeSetting { appearance.blurEnabled = value }

    private fun changeSetting(write: () -> Unit) {
        if (closed) return
        write()
        refreshSettings()
    }
    private fun refreshSettings() { mutable.update { it.copy(settings = readSettings()) } }
    private fun readSettings() = OnboardingBasicSettings(
        appearance.darkMode, appearance.colorStyle, appearance.interfaceStyle,
        appearance.bottomBarRaise, appearance.scrollTopEnabled, appearance.blurEnabled,
    )
}
