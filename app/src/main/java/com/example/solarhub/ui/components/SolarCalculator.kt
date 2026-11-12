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
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.example.solarhub.model.SolarCalculatorMode
import com.example.ui.theme.BorderLight
import com.example.ui.theme.SolarBlue
import com.example.ui.theme.SolarBlueDark
import com.example.ui.theme.SolarBlueLight
import com.example.ui.theme.SolarGold
import com.example.ui.theme.SolarNavyDark
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.DecimalFormat

@Composable
fun SolarCalculatorCard(
  modifier: Modifier = Modifier
) {
  var mode by remember { mutableStateOf(SolarCalculatorMode.MW_TO_QUANTITY) }
  var mwInput by remember { mutableStateOf("10") }
  var quantityInput by remember { mutableStateOf("") }
  var wattInput by remember { mutableStateOf("550") }

  val numberFormat = remember { DecimalFormat("#,##0.##") }
  val integerFormat = remember { DecimalFormat("#,###") }

  // Calculation computation
  val calculatedResult: String = remember(mode, mwInput, quantityInput, wattInput) {
    val watt = wattInput.toDoubleOrNull()
    if (watt == null || watt <= 0.0) {
      return@remember "Enter valid Module Watt (Wp)"
    }

    when (mode) {
      SolarCalculatorMode.MW_TO_QUANTITY -> {
        val mw = mwInput.toDoubleOrNull()
        if (mw != null && mw > 0) {
          // Module Quantity = (MW * 1,000,000) / Module Watt
          val qty = (mw * 1_000_000.0) / watt
          "${integerFormat.format(qty)} Modules"
        } else {
          "—"
        }
      }
      SolarCalculatorMode.QUANTITY_TO_MW -> {
        val qty = quantityInput.toDoubleOrNull()
        if (qty != null && qty > 0) {
          // MW = (Module Quantity * Module Watt) / 1,000,000
          val mw = (qty * watt) / 1_000_000.0
          "${numberFormat.format(mw)} MW"
        } else {
          "—"
        }
      }
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
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
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
          Column {
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

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFEF3C7))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.WbSunny,
              contentDescription = null,
              tint = SolarGold,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Formula Tool",
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color(0xFF92400E)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Tabs: A vs B
      TabRow(
        selectedTabIndex = if (mode == SolarCalculatorMode.MW_TO_QUANTITY) 0 else 1,
        containerColor = Color(0xFFF1F5F9),
        contentColor = SolarNavyDark,
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
      ) {
        Tab(
          selected = mode == SolarCalculatorMode.MW_TO_QUANTITY,
          onClick = { mode = SolarCalculatorMode.MW_TO_QUANTITY },
          modifier = Modifier.testTag("calc_tab_mw_to_qty"),
          text = {
            Text(
              text = "MW → Module Quantity",
              fontWeight = if (mode == SolarCalculatorMode.MW_TO_QUANTITY) FontWeight.Bold else FontWeight.Normal,
              fontSize = 13.sp
            )
          }
        )
        Tab(
          selected = mode == SolarCalculatorMode.QUANTITY_TO_MW,
          onClick = { mode = SolarCalculatorMode.QUANTITY_TO_MW },
          modifier = Modifier.testTag("calc_tab_qty_to_mw"),
          text = {
            Text(
              text = "Quantity → MW",
              fontWeight = if (mode == SolarCalculatorMode.QUANTITY_TO_MW) FontWeight.Bold else FontWeight.Normal,
              fontSize = 13.sp
            )
          }
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Inputs
      if (mode == SolarCalculatorMode.MW_TO_QUANTITY) {
        OutlinedTextField(
          value = mwInput,
          onValueChange = { mwInput = it },
          label = { Text("Plant Capacity (MW)") },
          placeholder = { Text("e.g. 10.0") },
          trailingIcon = { Text("MW", color = TextMuted, modifier = Modifier.padding(end = 12.dp)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("calc_input_mw")
        )
      } else {
        OutlinedTextField(
          value = quantityInput,
          onValueChange = { quantityInput = it },
          label = { Text("Module Quantity (Pcs)") },
          placeholder = { Text("e.g. 18,182") },
          trailingIcon = { Text("Pcs", color = TextMuted, modifier = Modifier.padding(end = 12.dp)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("calc_input_quantity")
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = wattInput,
        onValueChange = { wattInput = it },
        label = { Text("Module Rating (Watt / Wp)") },
        placeholder = { Text("e.g. 550") },
        trailingIcon = { Text("Wp", color = TextMuted, modifier = Modifier.padding(end = 12.dp)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("calc_input_watt")
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Calculation Result Box
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFFF8FAFC))
          .padding(16.dp)
      ) {
        Column {
          Text(
            text = if (mode == SolarCalculatorMode.MW_TO_QUANTITY) "Required Module Quantity:" else "Total Plant Output:",
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = calculatedResult,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (calculatedResult.contains("valid") || calculatedResult == "—") TextSecondary else SolarBlueDark
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = if (mode == SolarCalculatorMode.MW_TO_QUANTITY)
              "Formula: (MW × 1,000,000) / Module Watt"
            else
              "Formula: (Module Quantity × Module Watt) / 1,000,000",
            fontSize = 11.sp,
            color = TextMuted
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Action buttons: Clear & Reset
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
            mode = SolarCalculatorMode.MW_TO_QUANTITY
            mwInput = "10"
            quantityInput = "18182"
            wattInput = "550"
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
