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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SolarBlueDark
import com.example.ui.theme.SolarBlueLight
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.DecimalFormat
import java.util.Locale

/**
 * One simple calculator:
 *  - type Plant Capacity (MW) + Module Watt  -> Module Quantity appears
 *  - type Module Quantity + Module Watt      -> Plant Capacity (MW) appears
 */
@Composable
fun SolarCalculatorCard(
  modifier: Modifier = Modifier
) {
  var mwInput by remember { mutableStateOf("10") }
  var quantityInput by remember { mutableStateOf("18182") }
  var wattInput by remember { mutableStateOf("550") }
  var lastEditedMw by remember { mutableStateOf(true) }

  val integerFormat = remember { DecimalFormat("#,###") }
  val numberFormat = remember { DecimalFormat("#,##0.####") }

  fun cleanNumber(text: String): String {
    var dotSeen = false
    val builder = StringBuilder()
    for (ch in text) {
      if (ch.isDigit()) {
        builder.append(ch)
      } else if (ch == '.' && !dotSeen) {
        dotSeen = true
        builder.append(ch)
      }
    }
    return builder.toString()
  }

  fun formatMw(value: Double): String {
    return String.format(Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
  }

  // Recalculate the "other" field
  fun recalculate(fromMw: Boolean, watt: String) {
    val w = watt.toDoubleOrNull()
    if (fromMw) {
      val mw = mwInput.toDoubleOrNull()
      quantityInput = if (w != null && w > 0.0 && mw != null && mw > 0.0) {
        Math.round(mw * 1_000_000.0 / w).toString()
      } else {
        ""
      }
    } else {
      val qty = quantityInput.toDoubleOrNull()
      mwInput = if (w != null && w > 0.0 && qty != null && qty > 0.0) {
        formatMw(qty * w / 1_000_000.0)
      } else {
        ""
      }
    }
  }

  val summary: String? = run {
    val w = wattInput.toDoubleOrNull()
    val mw = mwInput.toDoubleOrNull()
    val qty = quantityInput.toDoubleOrNull()
    if (w == null || w <= 0.0 || mw == null || qty == null || mw <= 0.0 || qty <= 0.0) {
      null
    } else if (lastEditedMw) {
      "Required Module Quantity: ${integerFormat.format(qty)} Modules"
    } else {
      "Total Plant Output: ${numberFormat.format(mw)} MW"
    }
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("solar_calculator_card"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp)
    ) {
      // Header
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SolarBlueLight),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Calculate,
            contentDescription = null,
            tint = SolarBlueDark,
            modifier = Modifier.size(24.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Solar Calculator",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Text(
            text = "Quick manufacturing capacity sizing",
            fontSize = 12.sp,
            color = TextSecondary
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "Enter MW and Watt to get Module Quantity. Or enter Module Quantity and Watt to get MW.",
        fontSize = 12.sp,
        color = TextSecondary
      )

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
        value = mwInput,
        onValueChange = {
          mwInput = cleanNumber(it)
          lastEditedMw = true
          recalculate(true, wattInput)
        },
        label = { Text("Plant Capacity (MW)") },
        placeholder = { Text("e.g. 10") },
        trailingIcon = { Text("MW", color = TextMuted, modifier = Modifier.padding(end = 12.dp)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("calc_input_mw")
      )

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = quantityInput,
        onValueChange = {
          quantityInput = cleanNumber(it)
          lastEditedMw = false
          recalculate(false, wattInput)
        },
        label = { Text("Module Quantity (Pcs)") },
        placeholder = { Text("e.g. 18182") },
        trailingIcon = { Text("Pcs", color = TextMuted, modifier = Modifier.padding(end = 12.dp)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("calc_input_quantity")
      )

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = wattInput,
        onValueChange = {
          wattInput = cleanNumber(it)
          recalculate(lastEditedMw, wattInput)
        },
        label = { Text("Module Rating (Watt / Wp)") },
        placeholder = { Text("e.g. 550") },
        trailingIcon = { Text("Wp", color = TextMuted, modifier = Modifier.padding(end = 12.dp)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("calc_input_watt")
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Result box
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFFF8FAFC))
          .padding(14.dp)
      ) {
        Column {
          Text(
            text = summary ?: "Enter values to see the result",
            fontSize = if (summary != null) 18.sp else 14.sp,
            fontWeight = if (summary != null) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (summary != null) SolarBlueDark else TextSecondary
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "MW to Quantity = (MW × 1,000,000) / Watt\nQuantity to MW = (Quantity × Watt) / 1,000,000",
            fontSize = 11.sp,
            color = TextMuted,
            lineHeight = 16.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = {
            mwInput = ""
            quantityInput = ""
          },
          modifier = Modifier
            .weight(1f)
            .testTag("calc_button_clear")
        ) {
          Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Clear")
        }

        OutlinedButton(
          onClick = {
            mwInput = "10"
            quantityInput = "18182"
            wattInput = "550"
            lastEditedMw = true
          },
          modifier = Modifier
            .weight(1f)
            .testTag("calc_button_reset")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Reset")
        }
      }
    }
  }
}
