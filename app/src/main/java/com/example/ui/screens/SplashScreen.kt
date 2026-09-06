package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.localization.AppLanguage
import com.example.data.localization.Strings
import com.example.ui.components.HomeaseSplashBranding
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    language: AppLanguage,
    onTimeout: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(1500)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        HomeaseSplashBranding(
            tagline = Strings.get("tagline", language)
        )
    }
}
