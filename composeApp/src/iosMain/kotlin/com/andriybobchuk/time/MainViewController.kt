package com.andriybobchuk.time

import androidx.compose.ui.window.ComposeUIViewController
import com.andriybobchuk.time.app.App
import com.andriybobchuk.time.di.initKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
    }
) { App() }