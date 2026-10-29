package pk.fuelgo.driver.data.repository

import pk.fuelgo.driver.data.remote.ApiResult
import pk.fuelgo.driver.data.remote.ApiService
import pk.fuelgo.driver.data.remote.dto.CreateIncidentRequest
import pk.fuelgo.driver.data.remote.dto.DriverIncidentDto
import pk.fuelgo.driver.data.remote.dto.DriverProfileDto
import pk.fuelgo.driver.data.remote.dto.DriverShiftDto
import pk.fuelgo.driver.data.remote.dto.EndShiftRequest
import pk.fuelgo.driver.data.remote.dto.StartShiftRequest
import pk.fuelgo.driver.data.remote.dto.UpdateDriverRequest
import pk.fuelgo.driver.data.remote.safeApiCall
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private const val DEFAULT_SHIFT_HOURS = 8L

class DriverRepository(private val api: ApiService) {

    suspend fun getProfile(): ApiResult<DriverProfileDto> = safeApiCall { api.getMyProfile() }

    suspend fun setAvailability(driverId: String, available: Boolean): ApiResult<DriverProfileDto> =
        safeApiCall { api.updateDriver(driverId, UpdateDriverRequest(isAvailable = available)) }

    suspend fun getShifts(driverId: String): ApiResult<List<DriverShiftDto>> =
        safeApiCall { api.getShifts(driverId) }

    /** Starts a shift now, planned for [DEFAULT_SHIFT_HOURS] hours. Marks the driver on duty server-side. */
    suspend fun startShift(driverId: String): ApiResult<DriverShiftDto> {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val body = StartShiftRequest(
            shiftDate = LocalDate.now().toString(),
            plannedStart = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            plannedEnd = now.plusHours(DEFAULT_SHIFT_HOURS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        )
        return safeApiCall { api.startShift(driverId, body) }
    }

    suspend fun endShift(driverId: String, shiftId: String): ApiResult<DriverShiftDto> {
        val body = EndShiftRequest(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
        return safeApiCall { api.endShift(driverId, shiftId, body) }
    }

    suspend fun getIncidents(driverId: String): ApiResult<List<DriverIncidentDto>> =
        safeApiCall { api.getIncidents(driverId) }

    suspend fun reportIncident(driverId: String, type: String, severity: String, description: String): ApiResult<DriverIncidentDto> =
        safeApiCall { api.createIncident(driverId, CreateIncidentRequest(type, severity, description)) }
}
