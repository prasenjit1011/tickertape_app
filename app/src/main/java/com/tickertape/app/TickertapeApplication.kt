package com.tickertape.app

import android.app.Application
import androidx.compose.runtime.staticCompositionLocalOf
import com.tickertape.app.data.Repository

class TickertapeApplication : Application() {
    lateinit var repository: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = Repository(this)
    }
}

val LocalRepository = staticCompositionLocalOf<Repository> {
    error("Repository is not available")
}
