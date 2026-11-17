package com.veyra

import android.app.Application
import com.veyra.data.local.AppDatabase

class VeyraApp : Application() {
    val db: AppDatabase by lazy { AppDatabase.getInstance(this) }
}
