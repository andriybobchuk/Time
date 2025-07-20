package com.andriybobchuk.time.app

import androidx.compose.runtime.Composable
import com.andriybobchuk.time.core.presentation.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    AppTheme {
        NavigationHost()
    }
}

