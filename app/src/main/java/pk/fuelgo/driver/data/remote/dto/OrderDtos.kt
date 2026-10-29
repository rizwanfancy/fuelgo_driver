package pk.fuelgo.driver.data.remote.dto

import kotlinx.serialization.Serializable

// Mirrors Backend/FuelGo.Application/Orders/OrderDtos.cs (the parts a driver uses).

@Serializable
data class OrderDto(
    val id: String,
    val orderNumber: String = "",
    val customerId: String = "",
    val fuelTypeName: String = "",
    val fuelTypeCode: String = "",
    val quantityLitres: Double = 0.0,
    val pricePerLitre: Double = 0.0,
    val totalAmount: Double = 0.0,
    val status: String = "Assigned",
    val paymentMethod: String = "",
    val deliveryAddress: String = "",
    val deliveryCity: String = "",
    val deliveryLatitude: Double = 0.0,
    val deliveryLongitude: Double = 0.0,
    val scheduledAt: String? = null,
    val eta: String? = null,
    val deliveredAt: String? = null,
    val createdAt: String? = null,
    val driverName: String? = null,
    val cancellationReason: String? = null,
)

/** PATCH orders/{id}/status. The backend only allows Assigned→EnRoute, EnRoute→Arrived, Arrived→Failed. */
@Serializable
data class StatusUpdateRequest(
    val status: String,
    val notes: String? = null,
)

/** POST orders/{id}/deliver. The only way to reach "Delivered"; the backend verifies the OTP. */
@Serializable
data class DeliverOrderRequest(
    val otpCode: String,
    val actualQuantityDispensed: Double,
    val meterReadingPhotoUrl: String? = null,
)
