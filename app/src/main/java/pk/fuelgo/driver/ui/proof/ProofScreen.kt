package pk.fuelgo.driver.ui.proof

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import pk.fuelgo.driver.data.remote.dto.OrderDto
import pk.fuelgo.driver.ui.components.EmptyState
import pk.fuelgo.driver.ui.components.ErrorBanner
import pk.fuelgo.driver.ui.components.FuelGoPrimaryButton
import pk.fuelgo.driver.ui.components.FuelGoTextField
import pk.fuelgo.driver.ui.components.LoadingState
import pk.fuelgo.driver.ui.components.SectionCard
import pk.fuelgo.driver.ui.components.SuccessBanner
import pk.fuelgo.driver.ui.fuelGoViewModel
import pk.fuelgo.driver.ui.theme.FuelAction
import pk.fuelgo.driver.ui.util.pkr

private const val SUCCESS_REDIRECT_MS = 1500L

@Composable
fun ProofScreen(orderId: String, onDone: () -> Unit, onBack: () -> Unit) {
    val viewModel = fuelGoViewModel { ProofViewModel(it.orderRepository, orderId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.delivered) {
        if (state.delivered) {
            delay(SUCCESS_REDIRECT_MS)
            onDone()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val order = state.order
        when {
            state.isLoading -> LoadingState("Loading delivery…")
            order == null -> {
                state.errorMessage?.let { ErrorBanner(it, onRetry = viewModel::load) }
                EmptyState(
                    title = "No active delivery",
                    message = "Go back to the job queue and select an active order.",
                    actionLabel = "Back to jobs",
                    onAction = onBack,
                )
            }
            order.status != "Arrived" && !state.delivered -> EmptyState(
                title = "Not ready for delivery",
                message = "Mark the job as Arrived before confirming delivery.",
                actionLabel = "Back to job",
                onAction = onBack,
            )
            else -> ProofForm(order, state, viewModel)
        }
    }
}

@Composable
private fun ProofForm(order: OrderDto, state: ProofUiState, viewModel: ProofViewModel) {
    SectionCard {
        Text("Proof of delivery", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(order.orderNumber, style = MaterialTheme.typography.titleLarge)
        Text(
            "${order.fuelTypeName} · ${order.quantityLitres.toInt()} L ordered · ${pkr(order.totalAmount)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("${order.deliveryAddress}, ${order.deliveryCity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    SectionCard {
        Text("1. Customer OTP", style = MaterialTheme.typography.titleMedium)
        Text(
            "Ask the customer for the $OTP_LENGTH-digit code shown in their app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
        )
        OtpField(value = state.otp, onChange = viewModel::setOtp)
    }

    SectionCard {
        Text("2. Fuel dispensed", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 10.dp))
        FuelGoTextField(
            value = state.litresText,
            onValueChange = viewModel::setLitres,
            label = "Actual litres dispensed",
            keyboardType = KeyboardType.Decimal,
        )
        if (state.hasMismatch) {
            Text(
                "Differs from the ${order.quantityLitres.toInt()} L ordered. The customer will be billed for the dispensed amount.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    SectionCard {
        Text("3. Payment", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
        Text("Method: ${order.paymentMethod}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        when {
            state.isCash -> CheckRow("I collected ${pkr(order.totalAmount)} in cash", state.cashCollected, viewModel::setCashCollected)
            state.isCorporateCredit -> Text("Billed to the corporate account; nothing to collect.", modifier = Modifier.padding(top = 8.dp))
            else -> {
                CheckRow("Payment received / confirmed", state.paymentConfirmed, viewModel::setPaymentConfirmed)
                FuelGoTextField(
                    value = state.transactionId,
                    onValueChange = viewModel::setTransactionId,
                    label = "Transaction ID (optional)",
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }

    state.errorMessage?.let { ErrorBanner(it) }
    if (state.delivered) SuccessBanner("Delivery confirmed! Returning to your jobs…")

    FuelGoPrimaryButton(
        text = "Confirm delivery",
        onClick = viewModel::submit,
        enabled = state.canSubmit && !state.delivered,
        isLoading = state.isSubmitting,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, modifier = Modifier.padding(start = 4.dp))
    }
}

/** Four-box numeric OTP entry backed by a single text field (so paste and IME autofill work). */
@Composable
private fun OtpField(value: String, onChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        cursorBrush = SolidColor(Color_Transparent),
        textStyle = TextStyle(color = Color_Transparent),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(OTP_LENGTH) { index ->
                        val digit = value.getOrNull(index)?.toString().orEmpty()
                        val focused = index == value.length
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(width = 56.dp, height = 64.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                .border(
                                    BorderStroke(if (focused) 2.dp else 1.dp, if (focused) FuelAction else MaterialTheme.colorScheme.outline),
                                    RoundedCornerShape(12.dp),
                                ),
                        ) {
                            Text(digit, fontSize = MaterialTheme.typography.headlineMedium.fontSize, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                        }
                    }
                }
                // The real (invisible) field sits on top so taps open the keyboard.
                Box(modifier = Modifier.matchParentSize()) { inner() }
            }
        },
    )
}

private val Color_Transparent = androidx.compose.ui.graphics.Color.Transparent
