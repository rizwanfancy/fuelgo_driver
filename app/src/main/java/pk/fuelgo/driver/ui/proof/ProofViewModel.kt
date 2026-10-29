package pk.fuelgo.driver.ui.proof

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pk.fuelgo.driver.data.remote.ApiResult
import pk.fuelgo.driver.data.remote.dto.OrderDto
import pk.fuelgo.driver.data.repository.OrderRepository

const val OTP_LENGTH = 4
private const val MIN_LITRES = 0.0

data class ProofUiState(
    val isLoading: Boolean = true,
    val order: OrderDto? = null,
    val otp: String = "",
    val litresText: String = "",
    val cashCollected: Boolean = false,
    val paymentConfirmed: Boolean = false,
    val transactionId: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val delivered: Boolean = false,
) {
    val actualLitres: Double get() = litresText.toDoubleOrNull() ?: 0.0
    val hasMismatch: Boolean get() = order != null && actualLitres > MIN_LITRES && actualLitres != order.quantityLitres

    private val method: String get() = order?.paymentMethod.orEmpty().lowercase()
    val isCash: Boolean get() = method.contains("cash")
    val isCorporateCredit: Boolean get() = method.contains("credit") || method.contains("corporate")

    // Same payment gate as the web proof screen: cash must be collected, wallets/bank confirmed, credit is invoiced.
    val paymentStepDone: Boolean
        get() = when {
            isCash -> cashCollected
            isCorporateCredit -> true
            else -> paymentConfirmed
        }

    val canSubmit: Boolean
        get() = !isSubmitting && order?.status == "Arrived" && otp.length == OTP_LENGTH && actualLitres > MIN_LITRES && paymentStepDone
}

class ProofViewModel(
    private val orderRepository: OrderRepository,
    private val orderId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProofUiState())
    val uiState: StateFlow<ProofUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = orderRepository.getMyJobs()) {
                is ApiResult.Success -> {
                    val order = result.data.firstOrNull { it.id == orderId }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            order = order,
                            litresText = it.litresText.ifBlank { order?.quantityLitres?.let(::formatLitres).orEmpty() },
                        )
                    }
                }
                is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    fun setOtp(value: String) {
        val digits = value.filter { it.isDigit() }.take(OTP_LENGTH)
        _uiState.update { it.copy(otp = digits, errorMessage = null) }
    }

    fun setLitres(value: String) {
        if (value.length <= 8 && value.all { it.isDigit() || it == '.' } && value.count { it == '.' } <= 1) {
            _uiState.update { it.copy(litresText = value, errorMessage = null) }
        }
    }

    fun setCashCollected(value: Boolean) = _uiState.update { it.copy(cashCollected = value) }
    fun setPaymentConfirmed(value: Boolean) = _uiState.update { it.copy(paymentConfirmed = value) }
    fun setTransactionId(value: String) = _uiState.update { it.copy(transactionId = value) }

    fun submit() {
        val s = _uiState.value
        val order = s.order ?: return
        if (!s.canSubmit) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            when (val result = orderRepository.deliver(order.id, s.otp, s.actualLitres)) {
                is ApiResult.Success -> _uiState.update { it.copy(isSubmitting = false, delivered = true) }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = result.message.ifBlank { "Failed. Check the OTP and try again." })
                }
            }
        }
    }
}

private fun formatLitres(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
