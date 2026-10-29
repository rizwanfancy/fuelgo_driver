package pk.fuelgo.driver.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pk.fuelgo.driver.config.AppConfig
import pk.fuelgo.driver.data.remote.ApiResult
import pk.fuelgo.driver.data.remote.dto.OrderDto
import pk.fuelgo.driver.data.repository.DriverRepository
import pk.fuelgo.driver.data.repository.OrderRepository

val INCIDENT_TYPES = listOf("Vehicle breakdown", "Accident", "Fuel spill", "Customer unreachable", "Road blocked", "Other")
val INCIDENT_SEVERITIES = listOf("Low", "Medium", "High", "Critical")

data class JobDetailUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val order: OrderDto? = null,
    val isBusy: Boolean = false,
    val showFailConfirm: Boolean = false,
    val showIncidentDialog: Boolean = false,
    val isReportingIncident: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
)

/**
 * Detail for one job. [orderId] = null means "the Active tab": follow whichever job is
 * currently in progress (Arrived > EnRoute > Assigned) so the screen advances on its own
 * as the driver moves through the stages.
 */
class JobDetailViewModel(
    private val orderRepository: OrderRepository,
    private val driverRepository: DriverRepository,
    private val orderId: String?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(JobDetailUiState())
    val uiState: StateFlow<JobDetailUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    init {
        pollJob = viewModelScope.launch {
            while (isActive) {
                loadOnce()
                delay(AppConfig.JOB_POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun loadOnce() {
        when (val result = orderRepository.getMyJobs()) {
            is ApiResult.Success -> {
                val jobs = result.data.sortedForDriver()
                val order = if (orderId != null) {
                    jobs.firstOrNull { it.id == orderId }
                } else {
                    jobs.firstOrNull { it.status != "Delivered" }
                }
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, order = order, errorMessage = null) }
            }
            is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = result.message) }
        }
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadOnce()
        }
    }

    fun retry() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch { loadOnce() }
    }

    fun advance(newStatus: String) {
        val order = _uiState.value.order ?: return
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, actionMessage = null, showFailConfirm = false) }
            when (val result = orderRepository.updateStatus(order.id, newStatus)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isBusy = false, order = result.data, actionMessage = "Status updated: ${statusLabel(newStatus)}.") }
                    loadOnce()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isBusy = false, actionMessage = result.message) }
            }
        }
    }

    fun setFailConfirm(show: Boolean) = _uiState.update { it.copy(showFailConfirm = show) }
    fun setIncidentDialog(show: Boolean) = _uiState.update { it.copy(showIncidentDialog = show) }
    fun consumeActionMessage() = _uiState.update { it.copy(actionMessage = null) }

    fun reportIncident(type: String, severity: String, description: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isReportingIncident = true) }
            val profile = driverRepository.getProfile()
            if (profile !is ApiResult.Success) {
                _uiState.update { it.copy(isReportingIncident = false, actionMessage = (profile as ApiResult.Error).message) }
                return@launch
            }
            val orderNote = _uiState.value.order?.orderNumber?.let { "[$it] " }.orEmpty()
            when (val result = driverRepository.reportIncident(profile.data.driverId, type, severity, orderNote + description)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isReportingIncident = false, showIncidentDialog = false, actionMessage = "Issue reported to dispatch.")
                }
                is ApiResult.Error -> _uiState.update { it.copy(isReportingIncident = false, actionMessage = result.message) }
            }
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}
