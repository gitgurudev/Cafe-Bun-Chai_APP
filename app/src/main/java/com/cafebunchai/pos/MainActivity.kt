package com.cafebunchai.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cafebunchai.pos.ui.CafePosApp
import com.cafebunchai.pos.ui.theme.CafeBackdrop
import com.cafebunchai.pos.ui.theme.CafeBunChaiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as CafeBunChaiApp
        setContent {
            CafeBunChaiTheme {
                CafeBackdrop {
                    CafePosApp(app.container)
                }
            }
        }
    }
}
