package com.example.presentation.premium

import android.app.Application
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.FocusLockApplication
import com.example.data.AppRepository
import com.example.database.isPremiumActive
import com.example.network.BinancePlan
import com.example.network.BinancePremiumManager
import com.example.network.BinanceVerificationResult
import com.example.ui.theme.LiquidBackground
import com.example.ui.theme.liquidGlass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class VerificationState {
    object Idle : VerificationState()
    object Loading : VerificationState()
    data class Success(val message: String) : VerificationState()
    data class Error(val message: String) : VerificationState()
}

class PremiumViewModel(private val repository: AppRepository) : ViewModel() {

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PremiumViewModel::class.java)) {
                val repository = (application as FocusLockApplication).repository
                @Suppress("UNCHECKED_CAST")
                return PremiumViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    val userSettings = repository.userSettings

    private val _verificationState = MutableStateFlow<VerificationState>(VerificationState.Idle)
    val verificationState: StateFlow<VerificationState> = _verificationState.asStateFlow()

    fun verifyPayment(context: Context, txId: String, planId: String) {
        val cleanTxId = txId.trim()
        if (cleanTxId.isBlank()) {
            _verificationState.value = VerificationState.Error("Please enter your Binance Pay Order ID or Transaction ID.")
            return
        }

        viewModelScope.launch {
            _verificationState.value = VerificationState.Loading
            when (val result = BinancePremiumManager.verifyPayment(context, cleanTxId, planId)) {
                is BinanceVerificationResult.Success -> {
                    val now = System.currentTimeMillis()
                    val currentSettings = repository.userSettings.first()
                    val durationMillis = result.durationDays * 24L * 60 * 60 * 1000

                    val newExpiry = if (currentSettings.isPremiumActive && currentSettings.premiumExpiryTimestamp > now) {
                        currentSettings.premiumExpiryTimestamp + durationMillis
                    } else {
                        now + durationMillis
                    }

                    repository.updateSettings(
                        currentSettings.copy(
                            isPremium = true,
                            premiumPlanId = result.planId,
                            premiumTxId = result.txId,
                            premiumActivationTime = now,
                            premiumExpiryTimestamp = newExpiry
                        )
                    )
                    _verificationState.value = VerificationState.Success(result.message)
                }
                is BinanceVerificationResult.Error -> {
                    _verificationState.value = VerificationState.Error(result.message)
                }
            }
        }
    }

    fun resetState() {
        _verificationState.value = VerificationState.Idle
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    onBack: () -> Unit,
    isDark: Boolean = true,
    viewModel: PremiumViewModel = viewModel(
        factory = PremiumViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    val verificationState by viewModel.verificationState.collectAsState()
    val settings by viewModel.userSettings.collectAsState(initial = null)
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var selectedPlanId by remember { mutableStateOf("monthly") }
    val selectedPlan = BinancePremiumManager.PLANS.find { it.id == selectedPlanId } ?: BinancePremiumManager.PLANS[1]
    var txId by remember { mutableStateOf("") }
    var copyMessage by remember { mutableStateOf<String?>(null) }
    var showTutorial by remember { mutableStateOf(true) }
    var showCommonErrors by remember { mutableStateOf(false) }

    val primaryCyan = Color(0xFF00E5FF)
    val secondaryGreen = Color(0xFF47C28C)
    val warningAmber = Color(0xFFFFB74D)

    LiquidBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "FocusLock Premium",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Active status banner if already premium
                val isCurrentlyActive = settings?.isPremiumActive == true
                if (isCurrentlyActive) {
                    val expiry = settings?.premiumExpiryTimestamp ?: 0L
                    val expiryFormatted = if (expiry > 0L) {
                        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(expiry))
                    } else {
                        "Permanent Active"
                    }
                    val daysRemaining = if (expiry > System.currentTimeMillis()) {
                        ((expiry - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
                    } else 0

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(shape = RoundedCornerShape(20.dp), isElevated = true)
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(secondaryGreen.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = secondaryGreen)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    "Premium Active",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = secondaryGreen
                                )
                                Text(
                                    if (expiry > 0L) "Expires: $expiryFormatted ($daysRemaining days remaining)" else "Ad-Free Focus Experience Active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // Premium Hero Section
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(primaryCyan.copy(alpha = 0.15f))
                            .border(1.dp, primaryCyan.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Diamond,
                            contentDescription = null,
                            tint = primaryCyan,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Remove All Ads Forever",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Zero distractions • Direct Binance Verification • Instant On-Device Activation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center
                    )
                }

                // All Four Plan Cards
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    BinancePremiumManager.PLANS.forEach { plan ->
                        val isSelected = selectedPlanId == plan.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(
                                    shape = RoundedCornerShape(18.dp),
                                    isElevated = isSelected,
                                    isHighlight = isSelected,
                                    borderWidth = if (isSelected) 1.5.dp else 0.8.dp
                                )
                                .clickable {
                                    selectedPlanId = plan.id
                                    viewModel.resetState()
                                }
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            selectedPlanId = plan.id
                                            viewModel.resetState()
                                        },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = primaryCyan,
                                            unselectedColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "${plan.emoji} ${plan.name}",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            if (plan.isRecommended) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(primaryCyan.copy(alpha = 0.2f))
                                                        .border(0.5.dp, primaryCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        "RECOMMENDED",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = primaryCyan
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            "${plan.durationText} • ${plan.benefit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        plan.priceDisplay,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = primaryCyan
                                    )
                                    Text(
                                        "USDT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Payment Card (Binance Pay UID, Amount, Input, Verify)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(shape = RoundedCornerShape(20.dp), isElevated = true)
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "BINANCE PAY PAYMENT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = primaryCyan
                            )
                            Text(
                                "Send ${selectedPlan.priceDisplay} USDT",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        // Binance Pay UID Copy Box
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0D1726))
                                .border(0.8.dp, Color(0x4080D8FF), RoundedCornerShape(12.dp))
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(BinancePremiumManager.RECEIVER_PAY_ID))
                                    copyMessage = "Binance Pay ID copied: ${BinancePremiumManager.RECEIVER_PAY_ID}"
                                }
                                .padding(14.dp)
                        ) {
                            Text(
                                "Binance Pay UID / Pay ID (Tap to copy)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    BinancePremiumManager.RECEIVER_PAY_ID,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = primaryCyan
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "Copy",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = primaryCyan
                                    )
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = primaryCyan, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        if (copyMessage != null) {
                            Text(
                                copyMessage!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = secondaryGreen
                            )
                        }

                        // Transaction ID input
                        OutlinedTextField(
                            value = txId,
                            onValueChange = {
                                txId = it
                                viewModel.resetState()
                            },
                            label = { Text("Transaction ID / Binance Pay Order ID") },
                            placeholder = { Text("e.g. 2389104829104821") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryCyan,
                                unfocusedBorderColor = Color(0x4080D8FF),
                                focusedLabelColor = primaryCyan
                            )
                        )

                        // Verification State feedback
                        when (val state = verificationState) {
                            is VerificationState.Error -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            state.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            is VerificationState.Success -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(secondaryGreen.copy(alpha = 0.15f))
                                        .border(1.dp, secondaryGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = secondaryGreen)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            state.message,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = secondaryGreen
                                        )
                                    }
                                }
                            }
                            else -> {}
                        }

                        // Verify Action Button
                        Button(
                            onClick = { viewModel.verifyPayment(context, txId, selectedPlanId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            enabled = verificationState !is VerificationState.Loading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryCyan,
                                contentColor = Color(0xFF001F2B)
                            )
                        ) {
                            if (verificationState is VerificationState.Loading) {
                                CircularProgressIndicator(
                                    color = Color(0xFF001F2B),
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Verifying with Binance...", fontWeight = FontWeight.Bold)
                            } else {
                                Text(
                                    "Verify Payment",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                // In-App Payment Tutorial (11 Steps)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(shape = RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTutorial = !showTutorial },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = primaryCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Payment Tutorial & Guide",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Icon(
                                if (showTutorial) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = primaryCyan
                            )
                        }

                        AnimatedVisibility(visible = showTutorial) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 6.dp)
                            ) {
                                val steps = listOf(
                                    "1. Select a Premium plan.",
                                    "2. Copy the Binance Pay UID/Pay ID (582910482).",
                                    "3. Open Binance.",
                                    "4. Send the exact USDT amount shown.",
                                    "5. Complete the payment.",
                                    "6. Copy the Transaction ID.",
                                    "7. Return to FocusLock.",
                                    "8. Enter the Transaction ID.",
                                    "9. Tap Verify Payment.",
                                    "10. Wait for verification.",
                                    "11. Premium activates automatically after successful verification."
                                )

                                steps.forEach { step ->
                                    Text(
                                        text = step,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Common Errors to Avoid
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(shape = RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCommonErrors = !showCommonErrors },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = warningAmber, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Common Payment Errors to Avoid",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Icon(
                                if (showCommonErrors) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = warningAmber
                            )
                        }

                        AnimatedVisibility(visible = showCommonErrors) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 6.dp)
                            ) {
                                val errors = listOf(
                                    "• Wrong Amount: Sending less or more than the exact plan price (e.g. sending 0.50 instead of 0.49 USDT).",
                                    "• Invalid TxID: Entering an internal ref or mistyping the Binance Pay Order ID.",
                                    "• Incomplete Payment: Verifying before Binance finishes processing the transaction.",
                                    "• Wrong Currency: Sending BTC, ETH, or BNB instead of USDT.",
                                    "• Wrong Recipient: Sending payment to an incorrect Binance Pay UID instead of 582910482."
                                )

                                errors.forEach { err ->
                                    Text(
                                        text = err,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = warningAmber.copy(alpha = 0.9f),
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
