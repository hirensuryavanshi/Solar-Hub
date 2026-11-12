package com.example.solarhub.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solarhub.model.Department
import com.example.solarhub.ui.components.IconHelper
import com.example.solarhub.viewmodel.SolarHubViewModel
import com.example.ui.theme.BackgroundLightGrey
import com.example.ui.theme.SolarBlue
import com.example.ui.theme.SolarBlueLight
import com.example.ui.theme.SolarNavyDark
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManageDepartmentsScreen(
  viewModel: SolarHubViewModel,
  modifier: Modifier = Modifier
) {
  BackHandler {
    viewModel.navigateBack()
  }

  val context = LocalContext.current
  val departments by viewModel.departments.collectAsState()

  var showAddDialog by remember { mutableStateOf(false) }
  var departmentToEdit by remember { mutableStateOf<Department?>(null) }
  var departmentToDelete by remember { mutableStateOf<Department?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text("Manage Departments", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        navigationIcon = {
          IconButton(
            onClick = { viewModel.navigateBack() },
            modifier = Modifier.testTag("admin_manage_depts_back")
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SolarNavyDark)
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showAddDialog = true },
        containerColor = SolarBlue,
        contentColor = Color.White,
        modifier = Modifier.testTag("fab_add_department")
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add Department")
      }
    },
    containerColor = BackgroundLightGrey
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      item {
        Text(
          text = "All Manufacturing Departments (${departments.size})",
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Admins can add new departments, rename them, or delete departments and their associated files.",
          fontSize = 12.sp,
          color = TextSecondary
        )
      }

      items(departments, key = { it.id }) { dept ->
        Card(
          modifier = Modifier.fillMaxWidth().testTag("manage_dept_card_${dept.id}"),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceCard),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(CircleShape)
                  .background(SolarBlueLight),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = IconHelper.getDepartmentIcon(dept.iconKey),
                  contentDescription = null,
                  tint = SolarBlue,
                  modifier = Modifier.size(22.dp)
                )
              }
              Spacer(modifier = Modifier.width(14.dp))
              Column {
                Text(
                  text = dept.name,
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  color = TextPrimary
                )
                Text(
                  text = "Icon: ${dept.iconKey}",
                  fontSize = 11.sp,
                  color = TextMuted
                )
              }
            }

            Row {
              IconButton(onClick = { departmentToEdit = dept }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Department", tint = SolarNavyDark)
              }
              IconButton(onClick = { departmentToDelete = dept }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Department", tint = Color.Red)
              }
            }
          }
        }
      }
    }

    // Add Department Dialog
    if (showAddDialog) {
      DepartmentDialog(
        title = "Add New Department",
        initialName = "",
        initialIconKey = "factory",
        onConfirm = { name, icon ->
          if (name.isNotBlank()) {
            viewModel.addDepartment(name, icon)
            Toast.makeText(context, "Added department: $name", Toast.LENGTH_SHORT).show()
          }
          showAddDialog = false
        },
        onDismiss = { showAddDialog = false }
      )
    }

    // Edit Department Dialog
    departmentToEdit?.let { dept ->
      DepartmentDialog(
        title = "Edit Department",
        initialName = dept.name,
        initialIconKey = dept.iconKey,
        onConfirm = { name, icon ->
          if (name.isNotBlank()) {
            viewModel.updateDepartment(dept.copy(name = name.trim(), iconKey = icon))
            Toast.makeText(context, "Updated department: $name", Toast.LENGTH_SHORT).show()
          }
          departmentToEdit = null
        },
        onDismiss = { departmentToEdit = null }
      )
    }

    // Delete Department Confirmation Dialog
    departmentToDelete?.let { dept ->
      AlertDialog(
        onDismissRequest = { departmentToDelete = null },
        title = { Text("Delete Department?") },
        text = {
          Text("Are you sure you want to delete '${dept.name}'? This will also remove any published documents under this department.")
        },
        confirmButton = {
          Button(
            onClick = {
              viewModel.deleteDepartment(dept.id)
              Toast.makeText(context, "Department '${dept.name}' removed", Toast.LENGTH_SHORT).show()
              departmentToDelete = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
          ) {
            Text("Delete")
          }
        },
        dismissButton = {
          TextButton(onClick = { departmentToDelete = null }) {
            Text("Cancel")
          }
        }
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentDialog(
  title: String,
  initialName: String,
  initialIconKey: String,
  onConfirm: (String, String) -> Unit,
  onDismiss: () -> Unit
) {
  var name by remember { mutableStateOf(initialName) }
  var iconKey by remember { mutableStateOf(initialIconKey) }
  var iconDropdownExpanded by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title, fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Department Name") },
          placeholder = { Text("e.g. Automation & Robotics") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_dept_dialog_name")
        )

        ExposedDropdownMenuBox(
          expanded = iconDropdownExpanded,
          onExpandedChange = { iconDropdownExpanded = it }
        ) {
          OutlinedTextField(
            value = iconKey,
            onValueChange = {},
            readOnly = true,
            label = { Text("Icon Selection") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = iconDropdownExpanded) },
            leadingIcon = {
              Icon(
                imageVector = IconHelper.getDepartmentIcon(iconKey),
                contentDescription = null,
                tint = SolarBlue
              )
            },
            modifier = Modifier.fillMaxWidth().menuAnchor()
          )

          ExposedDropdownMenu(
            expanded = iconDropdownExpanded,
            onDismissRequest = { iconDropdownExpanded = false }
          ) {
            IconHelper.AVAILABLE_ICONS.forEach { (key, label) ->
              DropdownMenuItem(
                text = {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = IconHelper.getDepartmentIcon(key),
                      contentDescription = null,
                      tint = SolarBlue,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(label)
                  }
                },
                onClick = {
                  iconKey = key
                  iconDropdownExpanded = false
                }
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(name, iconKey) },
        colors = ButtonDefaults.buttonColors(containerColor = SolarNavyDark),
        modifier = Modifier.testTag("button_confirm_dept_dialog")
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
