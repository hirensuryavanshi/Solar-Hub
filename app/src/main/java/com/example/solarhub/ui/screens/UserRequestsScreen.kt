package com.example.solarhub.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solarhub.data.UserRequestEntity
import com.example.solarhub.model.UserRole
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

object RequestStatus {
  const val PENDING = "PENDING"
  const val IN_PROGRESS = "IN_PROGRESS"
  const val DONE = "DONE"

  val all = listOf(PENDING, IN_PROGRESS, DONE)

  fun label(status: String): String = when (status) {
    PENDING -> "Pending"
    IN_PROGRESS -> "In Progress"
    DONE -> "Done"
    else -> status
  }

  fun color(status: String): Color = when (status) {
    PENDING -> Color(0xFFD97706)
    IN_PROGRESS -> Color(0xFF2563EB)
    DONE -> Color(0xFF16A34A)
    else -> Color(0xFF64748B)
  }
}

/** Card shown on the Home screen and Admin dashboard to open the requests screen. */
@Composable
fun RequestsEntryCard(
  isAdmin: Boolean,
  pendingCount: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("requests_entry_card"),
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
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFE0F2FE)
      ) {
        Icon(
          imageVector = Icons.Default.SupportAgent,
          contentDescription = null,
          tint = SolarBlue,
          modifier = Modifier
            .padding(10.dp)
            .size(26.dp)
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = if (isAdmin) "User Requests" else "Need something? Send a Request",
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          color = TextPrimary
        )
        Text(
          text = if (isAdmin) {
            if (pendingCount > 0) "$pendingCount pending request(s) waiting for you" else "No pending requests"
          } else {
            "Tell the admin what information or document you need."
          },
          fontSize = 12.sp,
          color = TextSecondary
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserRequestsScreen(
  viewModel: SolarHubViewModel,
  userName: String,
  userMobile: String,
  modifier: Modifier = Modifier
) {
  BackHandler {
    viewModel.navigateBack()
  }

  val context = LocalContext.current
  val userRole by viewModel.userRole.collectAsState()
  val allRequests by viewModel.allRequests.collectAsState()
  val isAdmin = userRole == UserRole.ADMIN
  val requests = if (isAdmin) allRequests else allRequests.filter { it.userMobile == userMobile }

  var message by remember { mutableStateOf("") }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = if (isAdmin) "User Requests" else "My Requests",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          )
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
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      if (!isAdmin) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(
                text = "What do you need?",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Write your requirement. The admin will see it and work on it.",
                fontSize = 12.sp,
                color = TextSecondary
              )
              Spacer(modifier = Modifier.height(10.dp))
              OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                placeholder = { Text("e.g. Please add the latest SOP for lamination") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("input_request_message")
              )
              Spacer(modifier = Modifier.height(10.dp))
              Button(
                onClick = {
                  if (message.isBlank()) {
                    Toast.makeText(context, "Please write your requirement", Toast.LENGTH_SHORT).show()
                  } else {
                    viewModel.submitRequest(userName, userMobile, message)
                    message = ""
                    Toast.makeText(context, "Request sent to admin", Toast.LENGTH_SHORT).show()
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SolarNavyDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("button_submit_request")
              ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Request")
              }
            }
          }
        }
      }

      item {
        Text(
          text = if (isAdmin) "All requests (${requests.size})" else "My previous requests (${requests.size})",
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          color = TextPrimary
        )
      }

      if (requests.isEmpty()) {
        item {
          Text(
            text = if (isAdmin) "No requests yet." else "You have not sent any request yet.",
            fontSize = 13.sp,
            color = TextMuted
          )
        }
      } else {
        items(requests, key = { it.id }) { request ->
          RequestCard(
            request = request,
            isAdmin = isAdmin,
            onSave = { status, reply ->
              viewModel.updateRequest(request.id, status, reply)
              Toast.makeText(context, "Request updated", Toast.LENGTH_SHORT).show()
            },
            onDelete = { viewModel.deleteRequest(request.id) }
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestCard(
  request: UserRequestEntity,
  isAdmin: Boolean,
  onSave: (String, String) -> Unit,
  onDelete: () -> Unit
) {
  val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
  var status by remember(request.id, request.status) { mutableStateOf(request.status) }
  var reply by remember(request.id, request.adminReply) { mutableStateOf(request.adminReply) }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(modifier = Modifier.weight(1f)) {
          if (isAdmin) {
            Text(
              text = "${request.userName}  -  ${request.userMobile}",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = TextPrimary
            )
          }
          Text(
            text = dateFormat.format(Date(request.createdAt)),
            fontSize = 11.sp,
            color = TextMuted
          )
        }
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = RequestStatus.color(request.status).copy(alpha = 0.14f)
        ) {
          Text(
            text = RequestStatus.label(request.status),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = RequestStatus.color(request.status),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = request.message,
        fontSize = 14.sp,
        color = TextPrimary,
        lineHeight = 20.sp
      )

      if (!isAdmin && request.adminReply.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFEFF6FF)) {
          Text(
            text = "Admin: ${request.adminReply}",
            fontSize = 13.sp,
            color = TextPrimary,
            modifier = Modifier
              .fillMaxWidth()
              .padding(10.dp)
          )
        }
      }

      if (isAdmin) {
        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          RequestStatus.all.forEach { option ->
            FilterChip(
              selected = status == option,
              onClick = { status = option },
              label = { Text(RequestStatus.label(option), fontSize = 12.sp) }
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = reply,
          onValueChange = { reply = it },
          label = { Text("Reply to user (optional)") },
          minLines = 1,
          maxLines = 3,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Button(
            onClick = { onSave(status, reply) },
            colors = ButtonDefaults.buttonColors(containerColor = SolarNavyDark),
            modifier = Modifier.weight(1f)
          ) {
            Text("Save")
          }
          OutlinedButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete request", tint = Color(0xFFDC2626))
          }
        }
      }
    }
  }
}
