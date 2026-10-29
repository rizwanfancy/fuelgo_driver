package pk.fuelgo.driver.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pk.fuelgo.driver.ui.components.AvatarCircle
import pk.fuelgo.driver.ui.components.ErrorBanner
import pk.fuelgo.driver.ui.components.FuelGoOutlinedButton
import pk.fuelgo.driver.ui.components.FuelGoPrimaryButton
import pk.fuelgo.driver.ui.components.FuelGoTextField
import pk.fuelgo.driver.ui.components.LoadingState
import pk.fuelgo.driver.ui.components.RefreshBox
import pk.fuelgo.driver.ui.components.RefreshOnResume
import pk.fuelgo.driver.ui.components.SectionCard
import pk.fuelgo.driver.ui.fuelGoViewModel

@Composable
fun ProfileScreen(onLoggedOut: () -> Unit, onOpenServerSettings: () -> Unit) {
    val viewModel = fuelGoViewModel { ProfileViewModel(it.driverRepository, it.authRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.loggedOut) {
        if (state.loggedOut) onLoggedOut()
    }

    RefreshOnResume(viewModel::refresh)

    RefreshBox(isRefreshing = state.isRefreshing, onRefresh = viewModel::refresh) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.isLoading) {
                LoadingState("Loading your profile…")
            } else {
                val profile = state.profile
                if (profile != null) {
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarCircle(initials = initialsFor(profile.firstName, profile.lastName))
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text("${profile.firstName} ${profile.lastName}".trim(), style = MaterialTheme.typography.titleMedium)
                                Text("Employee ${profile.employeeId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        ProfileRow("Email", profile.email)
                        ProfileRow("Phone", profile.phoneNumber)
                        ProfileRow("Licence no.", profile.licenseNumber)
                        profile.licenseExpiry?.let { ProfileRow("Licence expiry", it.take(10)) }
                        ProfileRow("Rating", profile.rating?.let { "%.1f / 5".format(it) } ?: "No ratings yet")
                        ProfileRow("Total deliveries", profile.totalDeliveries.toString())
                    }

                    SectionCard {
                        Text("Assigned vehicle", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                        if (profile.assignedTankerPlate.isNullOrBlank()) {
                            Text("No tanker assigned yet. Contact dispatch.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            ProfileRow("Plate", profile.assignedTankerPlate)
                            profile.assignedTankerModel?.takeIf { it.isNotBlank() }?.let { ProfileRow("Model", it) }
                        }
                    }
                } else if (state.errorMessage != null) {
                    ErrorBanner(state.errorMessage.orEmpty(), onRetry = { viewModel.load() })
                }

                SectionCard {
                    FuelGoOutlinedButton(text = "Change password", onClick = { viewModel.openChangePassword() }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    FuelGoOutlinedButton(text = "Server settings", onClick = onOpenServerSettings, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    FuelGoPrimaryButton(text = "Log Out", onClick = { viewModel.logout() }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (state.showChangePassword) {
        ChangePasswordDialog(
            currentPassword = state.currentPassword,
            newPassword = state.newPassword,
            isSubmitting = state.isChangingPassword,
            errorMessage = state.changePasswordMessage,
            onCurrentPasswordChange = viewModel::setCurrentPassword,
            onNewPasswordChange = viewModel::setNewPassword,
            onDismiss = { viewModel.closeChangePassword() },
            onSubmit = { viewModel.submitChangePassword() },
        )
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ChangePasswordDialog(
    currentPassword: String,
    newPassword: String,
    isSubmitting: Boolean,
    errorMessage: String?,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FuelGoTextField(
                    value = currentPassword,
                    onValueChange = onCurrentPasswordChange,
                    label = "Current password",
                    isPassword = true,
                    keyboardType = KeyboardType.Password,
                )
                FuelGoTextField(
                    value = newPassword,
                    onValueChange = onNewPasswordChange,
                    label = "New password (min. 8 characters)",
                    isPassword = true,
                    keyboardType = KeyboardType.Password,
                )
                errorMessage?.let { ErrorBanner(it) }
            }
        },
        confirmButton = {
            TextButton(enabled = !isSubmitting, onClick = onSubmit) {
                Text(if (isSubmitting) "Saving…" else "Save")
            }
        },
        dismissButton = { TextButton(enabled = !isSubmitting, onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun initialsFor(firstName: String, lastName: String): String {
    val parts = listOf(firstName, lastName).filter { it.isNotBlank() }
    if (parts.isEmpty()) return "FG"
    return parts.joinToString("") { it.first().uppercase() }.take(2)
}
