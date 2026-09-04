package com.cafebunchai.pos.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CafeBackdrop(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Cream),
    ) {
        content()
    }
}
