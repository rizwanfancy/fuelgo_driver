package pk.fuelgo.driver.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pk.fuelgo.driver.data.remote.ApiResult
import pk.fuelgo.driver.data.repository.AuthRepository

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
)

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    fun updateLogin(transform: (LoginUiState) -> LoginUiState) {
        _loginState.update { transform(it).copy(errorMessage = null) }
    }

    fun login() {
        val state = _loginState.value
        if (state.username.isBlank() || state.password.isBlank()) {
            _loginState.update { it.copy(errorMessage = "Enter your email or mobile number, and your password.") }
            return
        }
        viewModelScope.launch {
            _loginState.update { it.copy(isSubmitting = true, errorMessage = null) }
            when (val result = authRepository.login(state.username.trim(), state.password)) {
                is ApiResult.Success -> _loginState.update { it.copy(isSubmitting = false, isLoggedIn = true) }
                is ApiResult.Error -> _loginState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
            }
        }
    }
}
