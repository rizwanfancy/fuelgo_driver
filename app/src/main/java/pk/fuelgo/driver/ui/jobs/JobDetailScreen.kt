package pk.fuelgo.driver.ui.jobs

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pk.fuelgo.driver.data.remote.dto.OrderDto
import pk.fuelgo.driver.ui.components.EmptyState
import pk.fuelgo.driver.ui.components.ErrorBanner
import pk.fuelgo.driver.ui.components.FuelGoOutlinedButton
import pk.fuelgo.driver.ui.components.FuelGoPrimaryButton
import pk.fuelgo.driver.ui.components.LoadingState
import pk.fuelgo.driver.ui.components.PillChip
import pk.fuelgo.driver.ui.components.RefreshBox
import pk.fuelgo.driver.ui.components.RefreshOnResume
import pk.fuelgo.driver.ui.components.SectionCard
import pk.fuelgo.driver.ui.components.StatusChip
import pk.fuelgo.driver.ui.fuelGoViewModel
import pk.fuelgo.driver.ui.theme.FuelAction
import pk.fuelgo.driver.ui.theme.StatusSuccess
import pk.fuelgo.driver.ui.util.formatTime
import pk.fuelgo.driver.ui.util.pkr

private data class Stage(val label: String, val detail: String)

private val STAGES = listOf(
    Stage("Accept job", "Assignment received from dispatch."),
    Stage("Start ride", "Begin navigation to the customer."),
    Stage("Arrived", "Mark arrival at the customer location."),
    Stage("Dispense fuel", "Enter litres dispensed and confirm with the customer's OTP."),
)

/** Number of completed stages for each order status. */
private fun completedStages(status: String): Int = when (status) {
    "Assigned" -> 1
    "EnRoute" -> 2
    "Arrived" -> 3
    "Delivered" -> 4
    else -> 0
}

/** [orderId] null = the Active tab (follows the job currently in progress). */
@Composable
fun JobDetailScreen(orderId: String?, onOpenProof: (String) -> Unit, onGoToJobs: () -> Unit) {
    val viewModel = fuelGoViewModel { JobDetailViewModel(it.orderRepository, it.driverRepository, orderId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    RefreshOnResume(viewModel::refresh)

    RefreshBox(isRefreshing = state.isRefreshing, onRefresh = viewModel::refresh) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val order = state.order
            when {
                state.isLoading -> LoadingState("Loading job…")
                order == null -> {
                    state.errorMessage?.let { ErrorBanner(it, onRetry = viewModel::retry) }
                    EmptyState(
                        title = if (orderId == null) "No active job" else "Job not found",
                        message = if (orderId == null) "Accept a job from the queue and it will appear here." else "This job may have been completed or reassigned.",
                        actionLabel = "Back to job queue",
                        onAction = onGoToJobs,
                    )
                }
                else -> {
                    OrderSummaryCard(order)
                    StagesCard(order.status)

                    SectionCard {
                        Text("Delivery destination", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                        DeliveryMap(
                            latitude = order.deliveryLatitude,
                            longitude = order.deliveryLongitude,
                            title = "${order.deliveryAddress}, ${order.deliveryCity}",
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 10.dp)) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = FuelAction, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("${order.deliveryAddress}, ${order.deliveryCity}", style = MaterialTheme.typography.bodyMedium)
                        }
                        FuelGoOutlinedButton(
                            text = "Open in Maps (navigate)",
                            onClick = { startNavigation(context, order) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    when (order.status) {
                        "Assigned" -> FuelGoPrimaryButton(
                            text = "Start ride",
                            onClick = { viewModel.advance("EnRoute") },
                            isLoading = state.isBusy,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        "EnRoute" -> FuelGoPrimaryButton(
                            text = "Mark arrived",
                            onClick = { viewModel.advance("Arrived") },
                            isLoading = state.isBusy,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        "Arrived" -> {
                            FuelGoPrimaryButton(
                                text = "Confirm delivery (OTP)",
                                onClick = { onOpenProof(order.id) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            FuelGoOutlinedButton(
                                text = "Delivery failed",
                                onClick = { viewModel.setFailConfirm(true) },
                                enabled = !state.isBusy,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        "Delivered" -> Text("Delivered. Nice work!", color = StatusSuccess, fontWeight = FontWeight.Bold)
                    }

                    if (order.status != "Delivered") {
                        FuelGoOutlinedButton(text = "Flag an issue", onClick = { viewModel.setIncidentDialog(true) }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            if (order != null) state.errorMessage?.let { ErrorBanner(it, onRetry = viewModel::retry) }
            state.actionMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
        }
    }

    if (state.showFailConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.setFailConfirm(false) },
            title = { Text("Mark delivery as failed?") },
            text = { Text("Use this only if you can't complete the delivery (e.g. customer unreachable). Dispatch will be notified.") },
            confirmButton = { TextButton(onClick = { viewModel.advance("Failed") }) { Text("Yes, mark failed") } },
            dismissButton = { TextButton(onClick = { viewModel.setFailConfirm(false) }) { Text("Cancel") } },
        )
    }

    if (state.showIncidentDialog) {
        IncidentDialog(
            isSubmitting = state.isReportingIncident,
            onDismiss = { viewModel.setIncidentDialog(false) },
            onSubmit = viewModel::reportIncident,
        )
    }
}

@Composable
private fun OrderSummaryCard(order: OrderDto) {
    SectionCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(order.orderNumber, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${order.fuelTypeName} · ${order.quantityLitres.toInt()} L @ ${pkr(order.pricePerLitre)}/L",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusChip(order.status)
        }
        Spacer(Modifier.height(10.dp))
        DetailLine("Order total", pkr(order.totalAmount), bold = true)
        DetailLine("Payment", order.paymentMethod)
        order.eta?.takeIf { it.isNotBlank() }?.let { DetailLine("ETA", formatTime(it)) }
    }
}

@Composable
private fun DetailLine(label: String, value: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(value, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Medium)
    }
}

@Composable
private fun StagesCard(status: String) {
    val done = completedStages(status)
    SectionCard {
        Text("Delivery stages", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        STAGES.forEachIndexed { index, stage ->
            val isDone = index < done
            val isCurrent = index == done
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                Box(
                    modifier = Modifier.size(28.dp).background(
                        when {
                            isDone -> StatusSuccess
                            isCurrent -> FuelAction
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        CircleShape,
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isDone) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("${index + 1}", color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(stage.label, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, color = if (isCurrent) FuelAction else MaterialTheme.colorScheme.onSurface)
                    Text(stage.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
internal fun IncidentDialog(isSubmitting: Boolean, onDismiss: () -> Unit, onSubmit: (type: String, severity: String, description: String) -> Unit) {
    var type by remember { mutableStateOf(INCIDENT_TYPES.first()) }
    var severity by remember { mutableStateOf("Medium") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report an issue") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Type", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(INCIDENT_TYPES) { PillChip(it, selected = type == it, onClick = { type = it }) }
                }
                Text("Severity", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(INCIDENT_SEVERITIES) { PillChip(it, selected = severity == it, onClick = { severity = it }) }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("What happened?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSubmitting && description.trim().length >= 3,
                onClick = { onSubmit(type, severity, description.trim()) },
            ) { Text(if (isSubmitting) "Sending…" else "Send report") }
        },
        dismissButton = { TextButton(enabled = !isSubmitting, onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Turn-by-turn navigation to the destination (Google Maps if present, otherwise any maps app). */
private fun startNavigation(context: Context, order: OrderDto) {
    val hasCoords = order.deliveryLatitude != 0.0 || order.deliveryLongitude != 0.0
    val destination = if (hasCoords) "${order.deliveryLatitude},${order.deliveryLongitude}" else Uri.encode("${order.deliveryAddress}, ${order.deliveryCity}")
    val navUri = Uri.parse("google.navigation:q=$destination")
    val geoUri = Uri.parse("geo:0,0?q=$destination")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, navUri))
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
        } catch (_: ActivityNotFoundException) {
            // No maps app installed; the in-app map above still shows the destination.
        }
    }
}
