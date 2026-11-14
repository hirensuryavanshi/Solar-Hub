package com.example.solarhub.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solarhub.model.UserRole
import com.example.ui.theme.BorderLight
import com.example.ui.theme.SolarBlue
import com.example.ui.theme.SolarBlueLight
import com.example.ui.theme.SolarNavyDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AuthDialog(
  currentRole: UserRole,
  onRoleSelected: (UserRole) -> Unit,
  onAdminLogin: (String) -> Boolean,
  onDismiss: () -> Unit,
  onLogout: (() -> Unit)? = null
) {
  val context = LocalContext.current
  var selectedTab by remember { mutableStateOf(currentRole) }
  var adminPin by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

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
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          text = "User Access & Roles",
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          color = TextPrimary
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Text(
          text = "Select your role to access SOLAR HUB platform capabilities:",
          fontSize = 13.sp,
          color = TextSecondary
        )

        // Employee Role Card
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              selectedTab = UserRole.EMPLOYEE
              errorMessage = null
            }
            .testTag("role_option_employee"),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (selectedTab == UserRole.EMPLOYEE) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)
          ),
          border = androidx.compose.foundation.BorderStroke(
            width = if (selectedTab == UserRole.EMPLOYEE) 2.dp else 1.dp,
            color = if (selectedTab == UserRole.EMPLOYEE) SolarBlue else BorderLight
          )
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFFDBEAFE)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Person, contentDescription = null, tint = SolarBlue, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("EMPLOYEE (Read-Only)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
              Text("View all published information, read text, watch videos, view & download documents.", fontSize = 11.sp, color = TextSecondary)
            }
            if (selectedTab == UserRole.EMPLOYEE) {
              Icon(Icons.Default.Check, contentDescription = null, tint = SolarBlue, modifier = Modifier.size(20.dp))
            }
          }
        }

        // Admin Role Card
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              selectedTab = UserRole.ADMIN
              errorMessage = null
            }
            .testTag("role_option_admin"),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (selectedTab == UserRole.ADMIN) Color(0xFFF0FDF4) else Color(0xFFF8FAFC)
          ),
          border = androidx.compose.foundation.BorderStroke(
            width = if (selectedTab == UserRole.ADMIN) 2.dp else 1.dp,
            color = if (selectedTab == UserRole.ADMIN) Color(0xFF16A34A) else BorderLight
          )
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFFDCFCE7)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("ADMIN (Full CMS Control)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
              Text("Create, publish, edit, unpublish, delete documents, and manage departments.", fontSize = 11.sp, color = TextSecondary)
            }
            if (selectedTab == UserRole.ADMIN) {
              Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
            }
          }
        }

        // If Admin selected, prompt for security PIN
        if (selectedTab == UserRole.ADMIN) {
          Column {
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
              Text(
                text = errorMessage ?: "",
                color = Color.Red,
                fontSize = 11.sp
              )
            }

          }
        }

        if (onLogout != null) {
          TextButton(
            onClick = {
              onLogout()
              onDismiss()
            }
          ) {
            Text("Log out / Change user", color = Color.Red, fontSize = 12.sp)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (selectedTab == UserRole.EMPLOYEE) {
            onRoleSelected(UserRole.EMPLOYEE)
            Toast.makeText(context, "Active Role: Employee (Read-Only)", Toast.LENGTH_SHORT).show()
            onDismiss()
          } else {
            // Validate Admin PIN
            val success = onAdminLogin(adminPin)
            if (success) {
              Toast.makeText(context, "Admin Access Granted", Toast.LENGTH_SHORT).show()
              onDismiss()
            } else {
              errorMessage = "Incorrect password"
            }
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = SolarNavyDark),
        modifier = Modifier.testTag("button_confirm_role")
      ) {
        Text("Confirm Role")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
