package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.feature.onboarding.application.*

internal class OnboardingActions(private val controller: OnboardingStateHolder, private val onAccept: () -> Unit) {
    fun go(step: Int) = controller.go(step)
    fun setAccepted(value: Boolean) = controller.setAccepted(value)
    fun openTerms() = controller.openTerms()
    fun dismissTerms() = controller.dismissTerms()
    fun complete() { if (controller.acknowledge()) onAccept() }
    fun setDarkMode(value: Int) = controller.setDarkMode(value)
    fun setColorStyle(value: Int) {
        if (!controller.isActive) return
        controller.setColorStyle(value)
    }
    fun setInterfaceStyle(value: Int) {
        if (!controller.isActive) return
        controller.setInterfaceStyle(value)
        com.aritxonly.teak.common.theme.TeakUiStyle.style = value
    }
    fun setBottomBarRaise(value: Boolean) = controller.setBottomBarRaise(value)
    fun setScrollTopEnabled(value: Boolean) = controller.setScrollTopEnabled(value)
    fun setBlurEnabled(value: Boolean) = controller.setBlurEnabled(value)
}
