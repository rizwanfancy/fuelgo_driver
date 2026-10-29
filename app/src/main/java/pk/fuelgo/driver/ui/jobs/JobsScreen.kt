package pk.fuelgo.driver.ui.jobs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pk.fuelgo.driver.data.remote.dto.OrderDto
import pk.fuelgo.driver.ui.components.EmptyState
import pk.fuelgo.driver.ui.components.ErrorBanner
import pk.fuelgo.driver.ui.components.FuelGoOutlinedButton
import pk.fuelgo.driver.ui.components.FuelGoPrimaryButton
import pk.fuelgo.driver.ui.components.LoadingState
import pk.fuelgo.driver.ui.components.RefreshBox
import pk.fuelgo.driver.ui.components.RefreshOnResume
import pk.fuelgo.driver.ui.components.StatusChip
import pk.fuelgo.driver.ui.fuelGoViewModel
import pk.fuelgo.driver.ui.theme.FuelAction
import pk.fuelgo.driver.ui.util.formatDateTime
import pk.fuelgo.driver.ui.util.formatTime
import pk.fuelgo.driver.ui.util.pkr
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun JobsScreen(onOpenJob: (String) -> Unit, onOpenProof: (String) -> Unit) {
    val viewModel = fuelGoViewModel { JobsViewModel(it.orderRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    RefreshOnResume(viewModel::refresh)

    RefreshBox(isRefreshing = state.isRefreshing, onRefresh = viewModel::refresh) {
        when {
            state.isLoading -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                LoadingState("Loading your jobs…")
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text("Your jobs", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.errorMessage?.let { msg -> item { ErrorBanner(msg, onRetry = viewModel::retry) } }
                state.actionMessage?.let { msg ->
                    item {
                        Text(msg, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { viewModel.consumeActionMessage() })
                    }
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(JOB_STATUS_FILTERS) { status ->
                            StatusTile(
                                label = statusLabel(status),
                                count = state.countFor(status),
                                selected = state.statusFilter == status,
                                onClick = { viewModel.selectFilter(status) },
                            )
                        }
                    }
                }
                if (state.filteredJobs.isEmpty()) {
                    item {
                        EmptyState(
                            title = if (state.jobs.isEmpty()) "No jobs assigned" else "No jobs in this status",
                            message = if (state.jobs.isEmpty()) "New assignments from dispatch will show up here. Pull down to refresh." else "Tap the status card again to see all jobs.",
                        )
                    }
                } else {
                    items(state.filteredJobs, key = { it.id }) { job ->
                        JobCard(
                            job = job,
                            isBusy = state.busyOrderId == job.id,
                            onOpen = { onOpenJob(job.id) },
                            onStartRide = { viewModel.updateStatus(job, "EnRoute") },
                            onArrived = { viewModel.updateStatus(job, "Arrived") },
                            onProof = { onOpenProof(job.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusTile(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = if (selected) BorderStroke(2.dp, FuelAction) else null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.width(92.dp).padding(12.dp)) {
            Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = if (selected) FuelAction else MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun JobCard(
    job: OrderDto,
    isBusy: Boolean,
    onOpen: () -> Unit,
    onStartRide: () -> Unit,
    onArrived: () -> Unit,
    onProof: () -> Unit,
) {
    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(job.orderNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${job.fuelTypeName} · ${job.quantityLitres.toInt()} L · ${pkr(job.totalAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusChip(job.status)
            }
            Text("${job.deliveryAddress}, ${job.deliveryCity}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Payment: ${job.paymentMethod}" +
                    (job.eta?.takeIf { it.isNotBlank() }?.let { " · ETA ${formatTime(it)}" } ?: "") +
                    (job.scheduledAt?.takeIf { it.isNotBlank() }?.let { " · Scheduled ${formatDateTime(it)}" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                when (job.status) {
                    "Assigned" -> FuelGoPrimaryButton(text = if (isBusy) "Starting…" else "Start ride", onClick = onStartRide, enabled = !isBusy, modifier = Modifier.weight(1f))
                    "EnRoute" -> FuelGoPrimaryButton(text = if (isBusy) "Updating…" else "Mark arrived", onClick = onArrived, enabled = !isBusy, modifier = Modifier.weight(1f))
                    "Arrived" -> FuelGoPrimaryButton(text = "Confirm delivery", onClick = onProof, modifier = Modifier.weight(1f))
                }
                FuelGoOutlinedButton(text = "Details", onClick = onOpen, modifier = if (job.status == "Delivered") Modifier.fillMaxWidth() else Modifier)
            }
        }
    }
}
