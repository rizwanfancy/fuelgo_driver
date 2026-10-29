package pk.fuelgo.driver.data.repository

import pk.fuelgo.driver.data.remote.ApiResult
import pk.fuelgo.driver.data.remote.ApiService
import pk.fuelgo.driver.data.remote.dto.DeliverOrderRequest
import pk.fuelgo.driver.data.remote.dto.OrderDto
import pk.fuelgo.driver.data.remote.dto.StatusUpdateRequest
import pk.fuelgo.driver.data.remote.safeApiCall

class OrderRepository(private val api: ApiService) {

    suspend fun getMyJobs(): ApiResult<List<OrderDto>> = safeApiCall { api.getMyJobs() }

    suspend fun updateStatus(id: String, status: String): ApiResult<OrderDto> =
        safeApiCall { api.updateStatus(id, StatusUpdateRequest(status)) }

    suspend fun deliver(id: String, otp: String, actualLitres: Double): ApiResult<OrderDto> =
        safeApiCall { api.deliverOrder(id, DeliverOrderRequest(otp, actualLitres)) }
}
