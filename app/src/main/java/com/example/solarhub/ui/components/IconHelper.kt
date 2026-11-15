package com.example.solarhub.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CoPresent
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.solarhub.model.ContentType
import com.example.ui.theme.BadgeExcel
import com.example.ui.theme.BadgeImage
import com.example.ui.theme.BadgeNotice
import com.example.ui.theme.BadgePdf
import com.example.ui.theme.BadgePpt
import com.example.ui.theme.BadgeText
import com.example.ui.theme.BadgeTraining
import com.example.ui.theme.BadgeVideo
import com.example.ui.theme.BadgeWord
import com.example.ui.theme.SolarBlue

object IconHelper {
  fun getDepartmentIcon(iconKey: String): ImageVector {
    return when (iconKey.lowercase()) {
      "factory" -> Icons.Default.Factory
      "verified" -> Icons.Default.Verified
      "inventory" -> Icons.Default.Inventory2
      "hub", "sap" -> Icons.Default.Hub
      "timeline", "ppc" -> Icons.Default.Timeline
      "solar_cell" -> Icons.Default.GridOn
      "solar_module" -> Icons.Default.WbSunny
      "build", "maintenance" -> Icons.Default.Build
      "security", "safety" -> Icons.Default.Security
      "warehouse", "store" -> Icons.Default.Warehouse
      "local_shipping", "dispatch" -> Icons.Default.LocalShipping
      "science", "rnd" -> Icons.Default.Science
      "engineering" -> Icons.Default.Engineering
      "eco", "ehs" -> Icons.Default.Eco
      "groups", "hr" -> Icons.Default.Groups
      "shopping_cart", "purchase" -> Icons.Default.ShoppingCart
      "calendar_month", "planning" -> Icons.Default.CalendarMonth
      else -> Icons.Default.Folder
    }
  }

  fun getContentTypeIcon(type: ContentType): ImageVector {
    return when (type) {
      ContentType.TEXT -> Icons.Default.Description
      ContentType.PDF -> Icons.Default.PictureAsPdf
      ContentType.WORD -> Icons.AutoMirrored.Filled.Article
      ContentType.EXCEL -> Icons.Default.TableChart
      ContentType.PPT -> Icons.Default.CoPresent
      ContentType.IMAGE -> Icons.Default.Image
      ContentType.VIDEO -> Icons.Default.Videocam
      ContentType.TRAINING -> Icons.Default.School
      ContentType.NOTICE -> Icons.Default.Campaign
      ContentType.OTHER -> Icons.Default.AttachFile
    }
  }

  fun getContentTypeColor(type: ContentType): Color {
    return when (type) {
      ContentType.TEXT -> BadgeText
      ContentType.PDF -> BadgePdf
      ContentType.WORD -> BadgeWord
      ContentType.EXCEL -> BadgeExcel
      ContentType.PPT -> BadgePpt
      ContentType.IMAGE -> BadgeImage
      ContentType.VIDEO -> BadgeVideo
      ContentType.TRAINING -> BadgeTraining
      ContentType.NOTICE -> BadgeNotice
      ContentType.OTHER -> SolarBlue
    }
  }

  val AVAILABLE_ICONS = listOf(
    "factory" to "Production / Factory",
    "verified" to "Quality / Check",
    "inventory" to "Packing / Box",
    "hub" to "SAP / System Hub",
    "timeline" to "PPC / Analytics",
    "solar_cell" to "Solar Cell / Grid",
    "solar_module" to "Solar Module / Sun",
    "build" to "Maintenance / Tools",
    "security" to "Safety / Shield",
    "warehouse" to "Store / Warehouse",
    "local_shipping" to "Dispatch / Logistics",
    "science" to "R&D / Research",
    "engineering" to "Engineering",
    "eco" to "EHS / Environment",
    "groups" to "HR & Training / Team",
    "shopping_cart" to "Purchase / Cart",
    "calendar_month" to "Planning & Inventory"
  )
}
