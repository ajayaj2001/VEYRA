package com.veyra

import android.app.Application
import com.veyra.data.local.VeyraDatabase

class VeyraApp : Application() {
    val database: VeyraDatabase by lazy { VeyraDatabase.getInstance(this) }
    val healthConnectManager by lazy { com.veyra.health.HealthConnectManager(this) }
}
