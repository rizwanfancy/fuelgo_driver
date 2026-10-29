package pk.fuelgo.driver.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pk.fuelgo.driver.ui.components.ErrorBanner
import pk.fuelgo.driver.ui.components.FuelGoPrimaryButton
import pk.fuelgo.driver.ui.components.FuelGoTextField

@Composable
fun LoginForm(state: LoginUiState, viewModel: AuthViewModel, apiConfigured: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Driver sign in", style = MaterialTheme.typography.titleLarge)

        FuelGoTextField(
            value = state.username,
            onValueChange = { value -> viewModel.updateLogin { it.copy(username = value) } },
            label = "Email or mobile number",
            keyboardType = KeyboardType.Email,
        )

        FuelGoTextField(
            value = state.password,
            onValueChange = { value -> viewModel.updateLogin { it.copy(password = value) } },
            label = "Password",
            isPassword = true,
            keyboardType = KeyboardType.Password,
        )

        state.errorMessage?.let { ErrorBanner(it) }

        FuelGoPrimaryButton(
            text = "Sign In",
            onClick = { viewModel.login() },
            enabled = apiConfigured,
            isLoading = state.isSubmitting,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )

        Text(
            "Driver accounts are created by your fleet commander. Contact dispatch if you can't sign in.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
