package pk.fuelgo.driver.data.remote.dto

import kotlinx.serialization.Serializable

// Mirrors Backend/FuelGo.Application/Drivers/DriverDtos.cs

@Serializable
data class DriverProfileDto(
    val driverId: String,
    val userId: String = "",
    val employeeId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val licenseNumber: String = "",
    val licenseExpiry: String? = null,
    val isAvailable: Boolean = false,
    val isOnDuty: Boolean = false,
    val rating: Double? = null,
    val totalDeliveries: Int = 0,
    val assignedTankerId: String? = null,
    val assignedTankerPlate: String? = null,
    val assignedTankerModel: String? = null,
    val isActive: Boolean = true,
    val createdAt: String? = null,
)

@Serializable
data class UpdateDriverRequest(
    val licenseNumber: String? = null,
    val licenseExpiry: String? = null,
    val isAvailable: Boolean? = null,
)

@Serializable
data class DriverShiftDto(
    val id: String,
    val driverId: String = "",
    val shiftDate: String? = null,
    val plannedStart: String? = null,
    val plannedEnd: String? = null,
    val actualStart: String? = null,
    val actualEnd: String? = null,
    val status: String? = null,
    val createdAt: String? = null,
)

// DateOnly serialises as "yyyy-MM-dd"; DateTime as ISO-8601.
@Serializable
data class StartShiftRequest(
    val shiftDate: String,
    val plannedStart: String,
    val plannedEnd: String,
)

@Serializable
data class EndShiftRequest(
    val actualEnd: String,
)

@Serializable
data class DriverIncidentDto(
    val id: String,
    val driverId: String = "",
    val incidentType: String? = null,
    val severity: String? = null,
    val description: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class CreateIncidentRequest(
    val incidentType: String,
    val severity: String,
    val description: String,
)
