package com.veyra.app.ui

import android.app.Application
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.veyra.app.data.health.HealthConnectRepository
import com.veyra.app.data.health.HealthDashboardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HealthConnectRepository(application)
    private val _state = MutableStateFlow<HealthDashboardState>(HealthDashboardState.Loading)
    val state: StateFlow<HealthDashboardState> = _state.asStateFlow()
    val permissions: Set<String>
        get() = repository.requestPermissions

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = HealthDashboardState.Loading
            when (repository.availability) {
                HealthConnectClient.SDK_AVAILABLE -> loadWhenPermitted()
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                    _state.value = HealthDashboardState.ProviderUpdateRequired
                }
                else -> _state.value = HealthDashboardState.Unavailable
            }
        }
    }

    fun onPermissionsResult() {
        refresh()
    }

    private suspend fun loadWhenPermitted() {
        if (!repository.hasRequiredPermissions()) {
            _state.value = HealthDashboardState.NeedsPermissions
            return
        }
        _state.value = runCatching { repository.readSnapshot() }
            .fold(
                onSuccess = { HealthDashboardState.Ready(it) },
                onFailure = {
                    HealthDashboardState.Error(
                        it.message?.takeIf(String::isNotBlank) ?: "Health data could not be read"
                    )
                }
            )
    }
}
