package com.example.presentation.premium
import com.example.network.VerifyResponse
import androidx.compose.foundation.BorderStroke
import com.example.database.isPremiumActive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import com.example.FocusLockApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.network.PremiumApi
import com.example.network.VerifyRequest
import com.example.database.AppDatabase
import com.example.data.AppRepository

import kotlinx.coroutines.flow.first

// State
sealed class VerificationState {
    object Idle : VerificationState()
    object Loading : VerificationState()
    data class Success(val message: String) : VerificationState()
    data class Error(val message: String) : VerificationState()
}

// Data class for plans
data class PremiumPlan(val id: String, val name: String, val price: String, val durationDays: Int, val isRecommended: Boolean = false)

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

    private val api = PremiumApi.create()

    private val _verificationState = MutableStateFlow<VerificationState>(VerificationState.Idle)
    val verificationState: StateFlow<VerificationState> = _verificationState.asStateFlow()

    fun verifyTransaction(txId: String, planId: String) {
        val cleanTxId = txId.trim()
        if (cleanTxId.isBlank()) {
            _verificationState.value = VerificationState.Error("Please enter a valid Transaction ID.")
            return
        }

        viewModelScope.launch {
            _verificationState.value = VerificationState.Loading
            try {
                // Real server-side verification via backend API
                val response = api.verifyTransaction(VerifyRequest(txId = cleanTxId, planId = planId))
                
                if (response.success) {
                    val duration = response.durationDays ?: when (planId) {
                        "weekly" -> 7
                        "monthly" -> 30
                        "quarterly" -> 90
                        "yearly" -> 365
                        else -> 30
                    }
                    val now = System.currentTimeMillis()
                    val currentSettings = repository.userSettings.first()
                    val newExpiry = if (currentSettings.premiumExpiryTimestamp > now) {
                        currentSettings.premiumExpiryTimestamp + (duration * 24L * 60 * 60 * 1000)
                    } else {
                        now + (duration * 24L * 60 * 60 * 1000)
                    }
                    
                    repository.updateSettings(currentSettings.copy(premiumExpiryTimestamp = newExpiry))
                    _verificationState.value = VerificationState.Success(response.message ?: "Premium verified and activated successfully!")
                } else {
                    _verificationState.value = VerificationState.Error(response.error ?: response.message ?: "Transaction verification rejected by server.")
                }
            } catch (e: java.net.UnknownHostException) {
                _verificationState.value = VerificationState.Error("Network error: Unable to reach verification server. Make sure the backend server is deployed and the URL is updated in PremiumApi.kt.")
            } catch (e: java.net.ConnectException) {
                _verificationState.value = VerificationState.Error("Connection failed: Could not connect to verification backend server. Please verify backend status.")
            } catch (e: retrofit2.HttpException) {
                _verificationState.value = VerificationState.Error("Server error (${e.code()}): ${e.message()}")
            } catch (e: Exception) {
                _verificationState.value = VerificationState.Error(e.localizedMessage ?: "Verification failed: Network or server error.")
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
    isDark: Boolean,
    viewModel: PremiumViewModel = viewModel(
        factory = PremiumViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    val verificationState by viewModel.verificationState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    
    var selectedPlan by remember { mutableStateOf("monthly") }
    var txId by remember { mutableStateOf("") }
    
    val plans = listOf(
        PremiumPlan("weekly", "Weekly", "$0.29", 7),
        PremiumPlan("monthly", "Monthly", "$0.49", 30, isRecommended = true),
        PremiumPlan("quarterly", "Quarterly", "$1.50", 90),
        PremiumPlan("yearly", "Yearly", "$5.00", 365)
    )
    
    val binanceId = "1076820124"
    val glassBg = if (isDark) Color(0xFF16243A).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f)
    val glassBorder = if (isDark) Color(0xFF00E5FF).copy(alpha = 0.3f) else Color(0xFF0077D6).copy(alpha = 0.3f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FocusLock Premium") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Remove Ads Forever",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Support the developer and unlock an ad-free experience. Choose a plan, send the exact amount via Binance Pay, and enter your Transaction ID below.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Plans Grid / List
            plans.forEach { plan ->
                PlanCard(
                    plan = plan,
                    isSelected = selectedPlan == plan.id,
                    onClick = { selectedPlan = plan.id },
                    glassBg = glassBg,
                    glassBorder = glassBorder,
                    primaryColor = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Payment Instructions
            Surface(
                color = glassBg,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, glassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Pay to Binance ID", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(binanceId))
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(binanceId, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy ID", tint = MaterialTheme.colorScheme.primary)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = txId,
                        onValueChange = { txId = it; viewModel.resetState() },
                        label = { Text("Transaction ID (TxID / Order ID)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Status and Button
            when (val state = verificationState) {
                is VerificationState.Error -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                is VerificationState.Success -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E676))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(state.message, color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                else -> {}
            }
            
            Button(
                onClick = { viewModel.verifyTransaction(txId, selectedPlan) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = verificationState !is VerificationState.Loading && verificationState !is VerificationState.Success
            ) {
                if (verificationState is VerificationState.Loading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else if (verificationState is VerificationState.Success) {
                    Text("Verified & Activated!")
                } else {
                    Text("Verify Payment")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun PlanCard(
    plan: PremiumPlan,
    isSelected: Boolean,
    onClick: () -> Unit,
    glassBg: Color,
    glassBorder: Color,
    primaryColor: Color
) {
    val borderColor = if (isSelected) primaryColor else glassBorder
    val bgColor = if (isSelected) primaryColor.copy(alpha = 0.1f) else glassBg
    
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(plan.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (plan.isRecommended) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = primaryColor,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "POPULAR", 
                                color = Color.White, 
                                fontSize = 10.sp, 
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("${plan.durationDays} Days Ad-Free", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Text(plan.price, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = primaryColor)
        }
    }
}
