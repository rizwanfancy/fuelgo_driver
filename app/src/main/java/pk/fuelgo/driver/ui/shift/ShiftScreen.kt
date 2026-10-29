package pk.fuelgo.driver.ui.shift

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pk.fuelgo.driver.ui.components.ErrorBanner
import pk.fuelgo.driver.ui.components.FuelGoOutlinedButton
import pk.fuelgo.driver.ui.components.FuelGoPrimaryButton
import pk.fuelgo.driver.ui.components.LoadingState
import pk.fuelgo.driver.ui.components.RefreshBox
import pk.fuelgo.driver.ui.components.RefreshOnResume
import pk.fuelgo.driver.ui.components.SectionCard
import pk.fuelgo.driver.ui.fuelGoViewModel
import pk.fuelgo.driver.ui.jobs.IncidentDialog
import pk.fuelgo.driver.ui.theme.StatusSuccess
import pk.fuelgo.driver.ui.util.formatDateTime

@Composable
fun ShiftScreen() {
    val viewModel = fuelGoViewModel { ShiftViewModel(it.driverRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    RefreshOnResume(viewModel::refresh)

    RefreshBox(isRefreshing = state.isRefreshing, onRefresh = viewModel::refresh) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val profile = state.profile
            when {
                state.isLoading -> LoadingState("Loading your shift…")
                profile == null -> state.errorMessage?.let { ErrorBanner(it, onRetry = { viewModel.load() }) }
                else -> {
                    val active = state.activeShift
                    SectionCard {
                        Text("Duty status", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (profile.isOnDuty) "On duty" else "Off duty",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (profile.isOnDuty) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        active?.actualStart?.let {
                            Text("Started ${formatDateTime(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(12.dp))
                        if (active == null) {
                            FuelGoPrimaryButton(text = "Start shift", onClick = viewModel::startShift, isLoading = state.isBusy, modifier = Modifier.fillMaxWidth())
                        } else {
                            FuelGoOutlinedButton(text = "End shift", onClick = viewModel::endShift, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth())
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Available for new jobs", fontWeight = FontWeight.Medium)
                                Text("Dispatch only assigns drivers who are available.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = profile.isAvailable, onCheckedChange = viewModel::setAvailability, enabled = !state.isBusy)
                        }
                    }

                    state.actionMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }

                    SectionCard {
                        Text("Recent shifts", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                        if (state.shifts.isEmpty()) {
                            Text("No shifts yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        state.shifts.take(RECENT_LIMIT).forEach { shift ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(shift.shiftDate ?: "", fontWeight = FontWeight.Medium)
                                    Text(
                                        (shift.actualStart?.let { formatDateTime(it) } ?: "-") + " → " + (shift.actualEnd?.let { formatDateTime(it) } ?: "ongoing"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(shift.status ?: "", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    SectionCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Incidents", style = MaterialTheme.typography.titleMedium)
                            FuelGoOutlinedButton(text = "Report issue", onClick = { viewModel.setIncidentDialog(true) })
                        }
                        Spacer(Modifier.height(8.dp))
                        if (state.incidents.isEmpty()) {
                            Text("No incidents reported.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        state.incidents.take(RECENT_LIMIT).forEach { incident ->
                            Column(modifier = Modifier.padding(vertical = 5.dp)) {
                                Text("${incident.incidentType ?: "Incident"} · ${incident.severity ?: ""}", fontWeight = FontWeight.Medium)
                                incident.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                incident.createdAt?.let { Text(formatDateTime(it), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showIncidentDialog) {
        IncidentDialog(
            isSubmitting = state.isReportingIncident,
            onDismiss = { viewModel.setIncidentDialog(false) },
            onSubmit = viewModel::reportIncident,
        )
    }
}

private const val RECENT_LIMIT = 10
