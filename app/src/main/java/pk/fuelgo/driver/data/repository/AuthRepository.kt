package pk.fuelgo.driver.data.repository

import kotlinx.coroutines.flow.Flow
import pk.fuelgo.driver.data.local.ApiConfigManager
import pk.fuelgo.driver.data.local.TokenManager
import pk.fuelgo.driver.data.remote.ApiResult
import pk.fuelgo.driver.data.remote.ApiService
import pk.fuelgo.driver.data.remote.dto.ApiMessageDto
import pk.fuelgo.driver.data.remote.dto.AuthResponseDto
import pk.fuelgo.driver.data.remote.dto.ChangePasswordRequest
import pk.fuelgo.driver.data.remote.dto.LoginRequest
import pk.fuelgo.driver.data.remote.safeApiCall

// Same role aliases the web portal maps to its "driver" role (auth.service.ts normalizeRole).
private val DRIVER_ROLES = setOf("driver", "operator", "deliveryagent", "tankerdriver")

private fun normalizeRole(value: String?): String =
    value.orEmpty().trim().lowercase().replace(" ", "").replace("_", "").replace("-", "")

class AuthRepository(
    private val api: ApiService,
    private val tokenManager: TokenManager,
    private val apiConfigManager: ApiConfigManager,
) {
    val isLoggedIn: Flow<Boolean> = tokenManager.isLoggedInFlow
    val userName: Flow<String?> = tokenManager.userNameFlow
    val userEmail: Flow<String?> = tokenManager.userEmailFlow

    suspend fun login(username: String, password: String): ApiResult<AuthResponseDto> {
        val tenantSlug = apiConfigManager.currentTenantSlug()
        val result = safeApiCall {
            api.login(LoginRequest(tenantSlug = tenantSlug, username = username, password = password))
        }
        if (result !is ApiResult.Success) return result

        // Customers / admins can authenticate against the same endpoint; keep them out of this app.
        val user = result.data.user
        val candidates = listOf(user.userType?.code, user.userType?.name) + user.roles
        if (candidates.none { normalizeRole(it) in DRIVER_ROLES }) {
            return ApiResult.Error("This app is for FuelGo drivers. Please use the customer app or the web portal for this account.")
        }

        tokenManager.saveSession(result.data)
        return result
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<ApiMessageDto> =
        safeApiCall { api.changePassword(ChangePasswordRequest(currentPassword, newPassword)) }

    suspend fun logout() {
        tokenManager.clear()
    }
}
