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
import pk.fuelgo.driver.data.repository.OrderRepository

/** Status cards shown on the Jobs tab, same set as the web portal's job queue. */
val JOB_STATUS_FILTERS = listOf("all", "Assigned", "EnRoute", "Arrived", "Delivered")

private val STATUS_ORDER = mapOf("Arrived" to 0, "EnRoute" to 1, "Assigned" to 2, "Delivered" to 3)

fun statusLabel(status: String): String = when (status) {
    "all" -> "Total"
    "EnRoute" -> "En Route"
    else -> status
}

data class JobsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val jobs: List<OrderDto> = emptyList(),
    val statusFilter: String = "all",
    val busyOrderId: String? = null,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
) {
    val filteredJobs: List<OrderDto>
        get() = if (statusFilter == "all") jobs else jobs.filter { it.status == statusFilter }

    fun countFor(status: String): Int = if (status == "all") jobs.size else jobs.count { it.status == status }
}

/** Sorts in-progress jobs first (Arrived, EnRoute, Assigned) and delivered last. */
internal fun List<OrderDto>.sortedForDriver(): List<OrderDto> =
    sortedWith(compareBy({ STATUS_ORDER[it.status] ?: 9 }, { it.scheduledAt ?: it.createdAt ?: "" }))

class JobsViewModel(private val orderRepository: OrderRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(JobsUiState())
    val uiState: StateFlow<JobsUiState> = _uiState.asStateFlow()

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
            is ApiResult.Success -> _uiState.update {
                it.copy(isLoading = false, isRefreshing = false, jobs = result.data.sortedForDriver(), errorMessage = null)
            }
            is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = result.message) }
        }
    }

    /** Pull-to-refresh / resume. */
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

    fun selectFilter(status: String) =
        _uiState.update { it.copy(statusFilter = if (it.statusFilter == status) "all" else status) }

    fun updateStatus(order: OrderDto, newStatus: String) {
        if (_uiState.value.busyOrderId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(busyOrderId = order.id, actionMessage = null) }
            when (val result = orderRepository.updateStatus(order.id, newStatus)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(busyOrderId = null, actionMessage = "${order.orderNumber} is now ${statusLabel(newStatus)}.") }
                    loadOnce()
                }
                is ApiResult.Error -> _uiState.update { it.copy(busyOrderId = null, actionMessage = result.message) }
            }
        }
    }

    fun consumeActionMessage() = _uiState.update { it.copy(actionMessage = null) }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}
