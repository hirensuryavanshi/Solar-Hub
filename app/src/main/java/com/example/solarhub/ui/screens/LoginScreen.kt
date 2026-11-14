package com.example.solarhub.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SolarBlue
import com.example.ui.theme.SolarGold
import com.example.ui.theme.SolarNavyDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

/** First screen: simple login with name and mobile number (no OTP). */
@Composable
fun LoginScreen(
  onLogin: (name: String, mobile: String) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var mobile by remember { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }

  Box(modifier = Modifier.fillMaxSize()) {
    SolarLoginBackground(modifier = Modifier.fillMaxSize())

    Column(
      modifier = Modifier
        .fillMaxSize()
        .systemBarsPadding()
        .imePadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Spacer(modifier = Modifier.height(24.dp))

      Box(
        modifier = Modifier
          .size(72.dp)
          .clip(CircleShape)
          .background(Color(0x331E3E62))
          .border(2.dp, SolarGold, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.WbSunny,
          contentDescription = null,
          tint = SolarGold,
          modifier = Modifier.size(40.dp)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "SOLAR HUB",
        fontSize = 30.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color.White
      )
      Text(
        text = "Solar Manufacturing Knowledge",
        fontSize = 13.sp,
        color = Color(0xFFBAE6FD)
      )

      Spacer(modifier = Modifier.height(28.dp))

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF5FFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Text(
            text = "Login",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Enter your name and mobile number to continue.",
            fontSize = 13.sp,
            color = TextSecondary
          )

          Spacer(modifier = Modifier.height(16.dp))

          OutlinedTextField(
            value = name,
            onValueChange = {
              name = it
              error = null
            },
            label = { Text("Your Name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = SolarBlue) },
            keyboardOptions = KeyboardOptions(
              capitalization = KeyboardCapitalization.Words,
              imeAction = ImeAction.Next
            ),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("login_input_name")
          )

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = mobile,
            onValueChange = { input ->
              mobile = input.filter { it.isDigit() }.take(10)
              error = null
            },
            label = { Text("Mobile Number") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = SolarBlue) },
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Phone,
              imeAction = ImeAction.Done
            ),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("login_input_mobile")
          )

          if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = error ?: "",
              color = Color(0xFFDC2626),
              fontSize = 12.sp
            )
          }

          Spacer(modifier = Modifier.height(18.dp))

          Button(
            onClick = {
              val cleanName = name.trim()
              if (cleanName.length < 2) {
                error = "Please enter your name."
              } else if (mobile.length != 10) {
                error = "Please enter a 10 digit mobile number."
              } else {
                onLogin(cleanName, mobile)
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = SolarNavyDark),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("login_button")
          ) {
            Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

private fun mix(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/** Sky, sun and a field of solar panels, drawn with code (no image file needed). */
@Composable
private fun SolarLoginBackground(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    // Sky gradient: night blue at the top to sunrise orange near the horizon
    drawRect(
      brush = Brush.verticalGradient(
        colors = listOf(
          Color(0xFF0B192C),
          Color(0xFF1E3E62),
          Color(0xFF0284C7),
          Color(0xFFF59E0B)
        ),
        startY = 0f,
        endY = h * 0.8f
      )
    )

    // Sun glow
    val sunCenter = Offset(w * 0.78f, h * 0.17f)
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(Color(0xAAFDE68A), Color(0x00F59E0B)),
        center = sunCenter,
        radius = w * 0.5f
      ),
      radius = w * 0.5f,
      center = sunCenter
    )
    drawCircle(color = Color(0xFFFCD34D), radius = w * 0.085f, center = sunCenter)

    // Sun rays
    for (i in 0 until 12) {
      val angle = Math.toRadians(i * 30.0)
      val start = Offset(
        sunCenter.x + (cos(angle) * w * 0.115f).toFloat(),
        sunCenter.y + (sin(angle) * w * 0.115f).toFloat()
      )
      val end = Offset(
        sunCenter.x + (cos(angle) * w * 0.16f).toFloat(),
        sunCenter.y + (sin(angle) * w * 0.16f).toFloat()
      )
      drawLine(
        color = Color(0xCCFCD34D),
        start = start,
        end = end,
        strokeWidth = 5f,
        cap = StrokeCap.Round
      )
    }

    // Solar panel field in perspective at the bottom
    val top = h * 0.74f
    val topLeft = w * 0.18f
    val topRight = w * 0.82f
    val bottomLeft = -w * 0.15f
    val bottomRight = w * 1.15f

    val panel = Path().apply {
      moveTo(topLeft, top)
      lineTo(topRight, top)
      lineTo(bottomRight, h)
      lineTo(bottomLeft, h)
      close()
    }
    drawPath(
      path = panel,
      brush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0B2A52), Color(0xFF071A33)),
        startY = top,
        endY = h
      )
    )

    val lineColor = Color(0x8838BDF8)
    val columns = 8
    for (i in 0..columns) {
      val t = i.toFloat() / columns
      drawLine(
        color = lineColor,
        start = Offset(mix(topLeft, topRight, t), top),
        end = Offset(mix(bottomLeft, bottomRight, t), h),
        strokeWidth = 2f
      )
    }
    val rows = 6
    for (j in 0..rows) {
      val f = (j.toFloat() / rows)
      val eased = f * f
      val y = mix(top, h, eased)
      val left = mix(topLeft, bottomLeft, eased)
      val right = mix(topRight, bottomRight, eased)
      drawLine(
        color = lineColor,
        start = Offset(left, y),
        end = Offset(right, y),
        strokeWidth = 2f
      )
    }
  }
}
