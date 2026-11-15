package com.example.solarhub.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solarhub.data.AppUserEntity
import com.example.solarhub.viewmodel.Screen
import com.example.solarhub.viewmodel.SolarHubViewModel
import com.example.ui.theme.BackgroundLightGrey
import com.example.ui.theme.SolarBlue
import com.example.ui.theme.SolarNavyDark
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Card on the Admin dashboard that opens the user list. */
@Composable
fun UsersEntryCard(
  totalUsers: Int,
  blockedUsers: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("users_entry_card"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFDCFCE7)) {
        Icon(
          imageVector = Icons.Default.Person,
          contentDescription = null,
          tint = Color(0xFF166534),
          modifier = Modifier
            .padding(10.dp)
            .size(26.dp)
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "User Data",
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          color = TextPrimary
        )
        Text(
          text = "$totalUsers user(s) logged in • $blockedUsers blocked",
          fontSize = 12.sp,
          color = TextSecondary
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
  viewModel: SolarHubViewModel,
  modifier: Modifier = Modifier
) {
  BackHandler { viewModel.navigateBack() }

  val users by viewModel.allUsers.collectAsState()
  val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
  var userToDelete by remember { mutableStateOf<AppUserEntity?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text("User Data", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        navigationIcon = {
          IconButton(onClick = { viewModel.navigateBack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SolarNavyDark)
      )
    },
    containerColor = BackgroundLightGrey
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
          ) {
            CountBox("Total Users", users.size, SolarBlue)
            CountBox("Active", users.count { !it.isBlocked }, Color(0xFF16A34A))
            CountBox("Blocked", users.count { it.isBlocked }, Color(0xFFDC2626))
          }
        }
      }

      if (users.isEmpty()) {
        item {
          Text(
            text = "No user has logged in yet.",
            color = TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(8.dp)
          )
        }
      }

      items(users, key = { it.mobile }) { user ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceCard),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = user.name,
                  fontWeight = FontWeight.Bold,
                  fontSize = 16.sp,
                  color = TextPrimary
                )
                Text(text = "Mobile: ${user.mobile}", fontSize = 13.sp, color = TextSecondary)
              }
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (user.isBlocked) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
              ) {
                Text(
                  text = if (user.isBlocked) "BLOCKED" else "ACTIVE",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (user.isBlocked) Color(0xFFB91C1C) else Color(0xFF166534),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "First login: ${dateFormat.format(Date(user.createdAt))}",
              fontSize = 11.sp,
              color = TextMuted
            )
            Text(
              text = "Last active: ${dateFormat.format(Date(user.lastLoginAt))}",
              fontSize = 11.sp,
              color = TextMuted
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              if (user.isBlocked) {
                Button(
                  onClick = { viewModel.setUserBlocked(user.mobile, false) },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                  shape = RoundedCornerShape(10.dp)
                ) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Unblock")
                }
              } else {
                Button(
                  onClick = { viewModel.setUserBlocked(user.mobile, true) },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                  shape = RoundedCornerShape(10.dp)
                ) {
                  Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Block")
                }
              }
              OutlinedButton(
                onClick = { userToDelete = user },
                shape = RoundedCornerShape(10.dp)
              ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Remove")
              }
            }
          }
        }
      }
    }
  }

  val target = userToDelete
  if (target != null) {
    AlertDialog(
      onDismissRequest = { userToDelete = null },
      title = { Text("Remove user?") },
      text = { Text("${target.name} (${target.mobile}) will be removed from this list. They can log in again.") },
      confirmButton = {
        TextButton(onClick = {
          viewModel.deleteUser(target.mobile)
          userToDelete = null
        }) { Text("Remove") }
      },
      dismissButton = { TextButton(onClick = { userToDelete = null }) { Text("Cancel") } }
    )
  }
}

@Composable
private fun CountBox(label: String, value: Int, color: Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = value.toString(), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = color)
    Text(text = label, fontSize = 12.sp, color = TextSecondary)
  }
}
