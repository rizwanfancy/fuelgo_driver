package pk.fuelgo.driver

import android.content.Context
import pk.fuelgo.driver.data.local.ApiConfigManager
import pk.fuelgo.driver.data.local.ThemePreferenceManager
import pk.fuelgo.driver.data.local.TokenManager
import pk.fuelgo.driver.data.remote.NetworkModule
import pk.fuelgo.driver.data.repository.AuthRepository
import pk.fuelgo.driver.data.repository.DriverRepository
import pk.fuelgo.driver.data.repository.NotificationRepository
import pk.fuelgo.driver.data.repository.OrderRepository

/**
 * Minimal manual dependency container; one instance lives on [FuelGoApp] for the whole
 * process lifetime (same approach as the customer app).
 */
class AppContainer(context: Context) {
    private val tokenManager = TokenManager(context.applicationContext)
    val themePreferenceManager = ThemePreferenceManager(context.applicationContext)
    val apiConfigManager = ApiConfigManager(context.applicationContext)
    private val networkModule = NetworkModule(tokenManager, apiConfigManager)

    val authRepository = AuthRepository(networkModule.apiService, tokenManager, apiConfigManager)
    val driverRepository = DriverRepository(networkModule.apiService)
    val orderRepository = OrderRepository(networkModule.apiService)
    val notificationRepository = NotificationRepository(networkModule.apiService)
}
