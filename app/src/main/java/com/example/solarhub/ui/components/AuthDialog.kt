package com.example.solarhub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solarhub.model.UserRole
import com.example.ui.theme.SolarBlue
import com.example.ui.theme.SolarNavyDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AuthDialog(
  currentRole: UserRole,
  currentUserName: String,
  currentUserMobile: String,
  currentServerUrl: String,
  isOfflineSession: Boolean = false,
  onAdminLogin: (String, (Boolean, String?) -> Unit) -> Unit,
  onUpdateServerUrl: (String) -> Unit,
  onTestConnection: (String, (Boolean, String) -> Unit) -> Unit,
  onLogout: () -> Unit,
  onDismiss: () -> Unit
) {
  var adminPin by remember { mutableStateOf("") }
  var serverUrl by remember { mutableStateOf(currentServerUrl) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var isSubmitting by remember { mutableStateOf(false) }
  var isTestingServer by remember { mutableStateOf(false) }
  var serverTestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(SolarNavyDark),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (currentRole == UserRole.ADMIN) Icons.Default.AdminPanelSettings else Icons.Default.Person,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = currentUserName.ifBlank { "SOLAR HUB User" },
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = TextPrimary
          )
          Text(
            text = buildString {
              append(if (currentRole == UserRole.ADMIN) "ADMIN (Server Verified)" else "EMPLOYEE")
              if (currentUserMobile.isNotBlank()) append(" • $currentUserMobile")
              if (isOfflineSession) append(" • Offline Mode")
            },
            fontSize = 11.sp,
            color = TextSecondary
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        if (currentRole != UserRole.ADMIN) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF8FAFC)
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = SolarNavyDark, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Switch to Admin Access", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
              }
              Spacer(modifier = Modifier.height(6.dp))
              OutlinedTextField(
                value = adminPin,
                onValueChange = {
                  adminPin = it
                  errorMessage = null
                },
                label = { Text("Admin Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                isError = errorMessage != null,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("input_admin_pin")
              )
              if (errorMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(errorMessage ?: "", color = Color(0xFFDC2626), fontSize = 11.sp)
              }
              Spacer(modifier = Modifier.height(8.dp))
              Button(
                onClick = {
                  if (adminPin.isBlank()) {
                    errorMessage = "Enter admin password"
                  } else {
                    isSubmitting = true
                    onAdminLogin(adminPin) { ok, err ->
                      isSubmitting = false
                      if (ok) onDismiss() else errorMessage = err ?: "Incorrect password"
                    }
                  }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = SolarNavyDark),
                modifier = Modifier.fillMaxWidth().testTag("button_confirm_role")
              ) {
                if (isSubmitting) {
                  CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                } else {
                  Text("Verify & Switch to Admin")
                }
              }
            }
          }
        }

        HorizontalDivider()

        Column {
          Text("Centralized Server URL", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TextSecondary)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = serverUrl,
            onValueChange = {
              serverUrl = it
              serverTestResult = null
            },
            leadingIcon = { Icon(Icons.Default.CloudDone, contentDescription = null, tint = SolarBlue) },
            placeholder = { Text("https://your-solarhub-server.com") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = {
                isTestingServer = true
                serverTestResult = null
                onTestConnection(serverUrl) { ok, msg ->
                  isTestingServer = false
                  serverTestResult = ok to msg
                }
              },
              enabled = !isTestingServer && serverUrl.isNotBlank(),
              modifier = Modifier.weight(1f)
            ) {
              Text(if (isTestingServer) "Testing..." else "Test Server", fontSize = 12.sp)
            }
            Button(
              onClick = { onUpdateServerUrl(serverUrl) },
              colors = ButtonDefaults.buttonColors(containerColor = SolarBlue),
              modifier = Modifier.weight(1f)
            ) {
              Text("Save & Sync", fontSize = 12.sp)
            }
          }
          serverTestResult?.let { (ok, msg) ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = msg,
              fontSize = 11.sp,
              color = if (ok) Color(0xFF15803D) else Color(0xFFDC2626)
            )
          }
        }
      }
    },
    confirmButton = {
      OutlinedButton(
        onClick = onLogout,
        modifier = Modifier.testTag("button_logout")
      ) {
        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFDC2626))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Sign Out", color = Color(0xFFDC2626))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}
