package pk.fuelgo.driver.ui.util

import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val pkrFormat = NumberFormat.getNumberInstance(Locale("en", "PK")).apply { maximumFractionDigits = 0 }
private val timeFormat = DateTimeFormatter.ofPattern("hh:mm a")
private val dateTimeFormat = DateTimeFormatter.ofPattern("d MMM, hh:mm a")

fun pkr(amount: Double) = "PKR ${pkrFormat.format(amount)}"

private fun parseLocal(raw: String): LocalDateTime? =
    runCatching { OffsetDateTime.parse(raw).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime() }
        .recoverCatching { LocalDateTime.parse(raw) }
        .getOrNull()

/** "04:30 PM" in the device time zone; falls back to the raw text if unparseable. */
fun formatTime(raw: String): String = parseLocal(raw)?.format(timeFormat) ?: raw.take(16)

/** "29 Oct, 04:30 PM" in the device time zone. */
fun formatDateTime(raw: String): String = parseLocal(raw)?.format(dateTimeFormat) ?: raw.take(16).replace("T", " ")
