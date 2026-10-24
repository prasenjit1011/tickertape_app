package com.tickertape.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.tickertape.app.ui.TickertapeNav
import com.tickertape.app.ui.TickertapeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        val repository = (application as TickertapeApplication).repository
        setContent {
            TickertapeTheme {
                CompositionLocalProvider(LocalRepository provides repository) {
                    TickertapeNav()
                }
            }
        }
    }
}
