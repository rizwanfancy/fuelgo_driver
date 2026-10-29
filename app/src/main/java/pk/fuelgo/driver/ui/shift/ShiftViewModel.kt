package pk.fuelgo.driver.ui.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pk.fuelgo.driver.data.remote.ApiResult
import pk.fuelgo.driver.data.remote.dto.DriverIncidentDto
import pk.fuelgo.driver.data.remote.dto.DriverProfileDto
import pk.fuelgo.driver.data.remote.dto.DriverShiftDto
import pk.fuelgo.driver.data.repository.DriverRepository

private const val ACTIVE_SHIFT = "Active"

data class ShiftUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val profile: DriverProfileDto? = null,
    val shifts: List<DriverShiftDto> = emptyList(),
    val incidents: List<DriverIncidentDto> = emptyList(),
    val isBusy: Boolean = false,
    val showIncidentDialog: Boolean = false,
    val isReportingIncident: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
) {
    val activeShift: DriverShiftDto? get() = shifts.firstOrNull { it.status == ACTIVE_SHIFT }
}

class ShiftViewModel(private val driverRepository: DriverRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ShiftUiState())
    val uiState: StateFlow<ShiftUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        val s = _uiState.value
        if (s.isLoading || s.isRefreshing) return
        load(silent = true)
    }

    fun load(silent: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = !silent, isRefreshing = silent, errorMessage = null) }
            when (val profile = driverRepository.getProfile()) {
                is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = profile.message) }
                is ApiResult.Success -> {
                    val id = profile.data.driverId
                    val shifts = (driverRepository.getShifts(id) as? ApiResult.Success)?.data.orEmpty()
                    val incidents = (driverRepository.getIncidents(id) as? ApiResult.Success)?.data.orEmpty()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            profile = profile.data,
                            shifts = shifts.sortedByDescending { s -> s.actualStart ?: s.createdAt ?: "" },
                            incidents = incidents.sortedByDescending { i -> i.createdAt ?: "" },
                        )
                    }
                }
            }
        }
    }

    fun startShift() = runAction("Shift started. You're on duty.") { driverRepository.startShift(it) }

    fun endShift() {
        val shift = _uiState.value.activeShift ?: return
        runAction("Shift ended.") { driverRepository.endShift(it, shift.id) }
    }

    fun setAvailability(available: Boolean) =
        runAction(if (available) "You're available for new jobs." else "You're marked unavailable.") {
            driverRepository.setAvailability(it, available)
        }

    private fun runAction(successMessage: String, call: suspend (driverId: String) -> ApiResult<*>) {
        val driverId = _uiState.value.profile?.driverId ?: return
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, actionMessage = null) }
            when (val result = call(driverId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isBusy = false, actionMessage = successMessage) }
                    load(silent = true)
                }
                is ApiResult.Error -> _uiState.update { it.copy(isBusy = false, actionMessage = result.message) }
            }
        }
    }

    fun setIncidentDialog(show: Boolean) = _uiState.update { it.copy(showIncidentDialog = show) }
    fun consumeActionMessage() = _uiState.update { it.copy(actionMessage = null) }

    fun reportIncident(type: String, severity: String, description: String) {
        val driverId = _uiState.value.profile?.driverId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isReportingIncident = true) }
            when (val result = driverRepository.reportIncident(driverId, type, severity, description)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isReportingIncident = false, showIncidentDialog = false, actionMessage = "Issue reported to dispatch.") }
                    load(silent = true)
                }
                is ApiResult.Error -> _uiState.update { it.copy(isReportingIncident = false, actionMessage = result.message) }
            }
        }
    }
}
