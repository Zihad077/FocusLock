package com.example.presentation.blocking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun BlockScreen(
    appName: String,
    usedMinutes: Int,
    limitMinutes: Int,
    emergencyRemaining: Int = 1,
    onWaitClick: () -> Unit,
    onChallengeClick: () -> Unit,
    onEmergencyUnlockClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Locked",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = if (limitMinutes == 0) "APP RESTRICTED" else "TIME'S UP",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = if (limitMinutes == 0) {
                "\"$appName\" is permanently restricted by your rules. Stay focused on what matters!"
            } else {
                "You've reached your daily limit for $appName. Take a step back and breathe."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Usage Today",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (limitMinutes == 0) "Strict Block (0m limit)" else "$usedMinutes / $limitMinutes min",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (limitMinutes == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onWaitClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Return to Home", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            
            Button(
                onClick = onChallengeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Take a Challenge", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            
            OutlinedButton(
                onClick = onEmergencyUnlockClick,
                enabled = emergencyRemaining > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (emergencyRemaining > 0) "Emergency Unlock ($emergencyRemaining left)" else "No Emergency Unlocks Left",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
