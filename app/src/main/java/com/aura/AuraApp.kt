package com.aura

import android.app.Application
import com.aura.data.local.AppDatabase

class AuraApp : Application() {
    val db: AppDatabase by lazy { AppDatabase.getInstance(this) }
}
