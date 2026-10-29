package pk.fuelgo.driver.ui.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pk.fuelgo.driver.data.local.ApiConfigManager
import pk.fuelgo.driver.data.remote.ApiResult
import pk.fuelgo.driver.data.repository.AuthRepository

data class ApiConfigUiState(
    val baseUrlInput: String = "",
    val tenantSlugInput: String = "",
    val isTesting: Boolean = false,
    val testResultMessage: String? = null,
    val testResultIsSuccess: Boolean? = null,
    val saveMessage: String? = null,
)

class ApiConfigViewModel(
    private val apiConfigManager: ApiConfigManager,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApiConfigUiState())
    val uiState: StateFlow<ApiConfigUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val baseUrl = apiConfigManager.currentBaseUrl().orEmpty()
            val tenant = apiConfigManager.currentTenantSlug()
            _uiState.update { it.copy(baseUrlInput = baseUrl, tenantSlugInput = tenant) }
        }
    }

    fun setBaseUrlInput(value: String) =
        _uiState.update { it.copy(baseUrlInput = value, testResultMessage = null, saveMessage = null) }

    fun setTenantSlugInput(value: String) =
        _uiState.update { it.copy(tenantSlugInput = value, testResultMessage = null, saveMessage = null) }

    fun resetToBlank() =
        _uiState.update {
            it.copy(baseUrlInput = "", tenantSlugInput = ApiConfigManager.DEFAULT_TENANT_SLUG, testResultMessage = null, saveMessage = null)
        }

    fun save() {
        val s = _uiState.value
        if (s.baseUrlInput.isBlank()) {
            _uiState.update { it.copy(saveMessage = "Server URL can't be empty.") }
            return
        }
        viewModelScope.launch {
            apiConfigManager.saveBaseUrl(s.baseUrlInput)
            apiConfigManager.saveTenantSlug(s.tenantSlugInput)
            _uiState.update { it.copy(saveMessage = "Saved. New requests will use this server.") }
        }
    }

    /** Saves whatever is currently typed, then makes one real (unauthenticated) call to confirm it's reachable. */
    fun testConnection() {
        val s = _uiState.value
        if (s.baseUrlInput.isBlank()) {
            _uiState.update { it.copy(testResultMessage = "Enter a server URL first.", testResultIsSuccess = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, testResultMessage = null, testResultIsSuccess = null) }
            apiConfigManager.saveBaseUrl(s.baseUrlInput)
            apiConfigManager.saveTenantSlug(s.tenantSlugInput)

            when (val result = authRepository.checkConnection()) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isTesting = false, testResultMessage = "Connected successfully.", testResultIsSuccess = true)
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isTesting = false, testResultMessage = result.message, testResultIsSuccess = false)
                }
            }
        }
    }
}
