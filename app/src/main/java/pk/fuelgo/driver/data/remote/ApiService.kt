package pk.fuelgo.driver.data.remote

import pk.fuelgo.driver.data.remote.dto.ApiMessageDto
import pk.fuelgo.driver.data.remote.dto.AuthResponseDto
import pk.fuelgo.driver.data.remote.dto.ChangePasswordRequest
import pk.fuelgo.driver.data.remote.dto.CreateIncidentRequest
import pk.fuelgo.driver.data.remote.dto.DeliverOrderRequest
import pk.fuelgo.driver.data.remote.dto.DriverIncidentDto
import pk.fuelgo.driver.data.remote.dto.DriverProfileDto
import pk.fuelgo.driver.data.remote.dto.DriverShiftDto
import pk.fuelgo.driver.data.remote.dto.EndShiftRequest
import pk.fuelgo.driver.data.remote.dto.LoginRequest
import pk.fuelgo.driver.data.remote.dto.NotificationDto
import pk.fuelgo.driver.data.remote.dto.OrderDto
import pk.fuelgo.driver.data.remote.dto.RefreshTokenRequest
import pk.fuelgo.driver.data.remote.dto.StartShiftRequest
import pk.fuelgo.driver.data.remote.dto.StatusUpdateRequest
import pk.fuelgo.driver.data.remote.dto.UpdateDriverRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// Endpoint surface used by the driver app, matching Backend/FuelGo.Api/Controllers.
// The tenant "slug" header and Authorization bearer token are attached by NetworkModule's
// interceptors, so none of these calls pass them explicitly.
interface ApiService {

    // --- Auth ---------------------------------------------------------
    @POST("Auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponseDto>

    @POST("Auth/refresh-token")
    suspend fun refreshToken(@Body body: RefreshTokenRequest): Response<AuthResponseDto>

    @POST("Auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): Response<ApiMessageDto>

    // --- Driver orders ---------------------------------------------------
    /** Assigned / EnRoute / Arrived / Delivered orders for the logged-in driver. */
    @GET("orders/driver/my")
    suspend fun getMyJobs(): Response<List<OrderDto>>

    @PATCH("orders/{id}/status")
    suspend fun updateStatus(@Path("id") id: String, @Body body: StatusUpdateRequest): Response<OrderDto>

    @POST("orders/{id}/deliver")
    suspend fun deliverOrder(@Path("id") id: String, @Body body: DeliverOrderRequest): Response<OrderDto>

    // --- Driver profile / shifts / incidents ---------------------------------
    @GET("drivers/me")
    suspend fun getMyProfile(): Response<DriverProfileDto>

    @PUT("drivers/{id}")
    suspend fun updateDriver(@Path("id") id: String, @Body body: UpdateDriverRequest): Response<DriverProfileDto>

    @GET("drivers/{id}/shifts")
    suspend fun getShifts(@Path("id") id: String): Response<List<DriverShiftDto>>

    @POST("drivers/{id}/shifts/start")
    suspend fun startShift(@Path("id") id: String, @Body body: StartShiftRequest): Response<DriverShiftDto>

    @PUT("drivers/{id}/shifts/{shiftId}/end")
    suspend fun endShift(
        @Path("id") id: String,
        @Path("shiftId") shiftId: String,
        @Body body: EndShiftRequest,
    ): Response<DriverShiftDto>

    @GET("drivers/{id}/incidents")
    suspend fun getIncidents(@Path("id") id: String): Response<List<DriverIncidentDto>>

    @POST("drivers/{id}/incidents")
    suspend fun createIncident(@Path("id") id: String, @Body body: CreateIncidentRequest): Response<DriverIncidentDto>

    // --- Notifications ----------------------------------------------------
    @GET("notifications")
    suspend fun getNotifications(): Response<List<NotificationDto>>

    @PATCH("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): Response<Unit>

    @PATCH("notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<Unit>
}
