package pk.fuelgo.driver.data.repository

import pk.fuelgo.driver.data.remote.ApiResult
import pk.fuelgo.driver.data.remote.ApiService
import pk.fuelgo.driver.data.remote.dto.NotificationDto
import pk.fuelgo.driver.data.remote.safeApiCall

class NotificationRepository(private val api: ApiService) {
    suspend fun getAll(): ApiResult<List<NotificationDto>> = safeApiCall { api.getNotifications() }
    suspend fun markRead(id: String): ApiResult<Unit> = safeApiCall { api.markNotificationRead(id) }
    suspend fun markAllRead(): ApiResult<Unit> = safeApiCall { api.markAllNotificationsRead() }
}
