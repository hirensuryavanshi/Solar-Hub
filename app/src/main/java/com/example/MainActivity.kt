package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.solarhub.data.SessionStore
import com.example.solarhub.data.SessionUser
import com.example.solarhub.data.SolarHubDatabase
import com.example.solarhub.data.SolarHubRepository
import com.example.solarhub.model.UserRole
import com.example.solarhub.ui.components.AuthDialog
import com.example.solarhub.ui.screens.AdminDashboardScreen
import com.example.solarhub.ui.screens.AdminEditContentScreen
import com.example.solarhub.ui.screens.AdminManageDepartmentsScreen
import com.example.solarhub.ui.screens.ContentDetailScreen
import com.example.solarhub.ui.screens.DepartmentScreen
import com.example.solarhub.ui.screens.HomeScreen
import com.example.solarhub.ui.screens.LoginScreen
import com.example.solarhub.ui.screens.UserRequestsScreen
import com.example.solarhub.viewmodel.Screen
import com.example.solarhub.viewmodel.SolarHubViewModel
import com.example.solarhub.viewmodel.SolarHubViewModelFactory
import com.example.ui.theme.SolarHubTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
    )

    val database = SolarHubDatabase.getDatabase(applicationContext)
    val repository = SolarHubRepository(database.solarHubDao())
    val viewModelFactory = SolarHubViewModelFactory(repository)
    val session = SessionStore(applicationContext)

    setContent {
      val viewModel: SolarHubViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = viewModelFactory
      )

      var user by remember { mutableStateOf(session.load()) }
      val currentUser = user

      if (currentUser == null) {
        // First screen: simple login with name and mobile number
        SolarHubTheme {
          LoginScreen(
            onLogin = { name, mobile ->
              val newUser = SessionUser(name, mobile)
              session.save(newUser)
              user = newUser
            }
          )
        }
      } else {
        SolarHubApp(
          viewModel = viewModel,
          user = currentUser,
          onLogout = {
            session.clear()
            viewModel.setUserRole(UserRole.EMPLOYEE)
            user = null
          }
        )
      }
    }
  }
}

@Composable
fun SolarHubApp(
  viewModel: SolarHubViewModel,
  user: SessionUser,
  onLogout: () -> Unit
) {
  val currentScreen by viewModel.currentScreen.collectAsState()
  val userRole by viewModel.userRole.collectAsState()
  var showAuthDialog by remember { mutableStateOf(false) }

  SolarHubTheme {
    Surface(modifier = Modifier.fillMaxSize()) {
      AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "ScreenTransition"
      ) { screen ->
        when (screen) {
          is Screen.Home -> {
            HomeScreen(
              viewModel = viewModel,
              onOpenAuthDialog = { showAuthDialog = true }
            )
          }
          is Screen.DepartmentDetail -> {
            DepartmentScreen(
              departmentId = screen.departmentId,
              viewModel = viewModel
            )
          }
          is Screen.ContentDetail -> {
            ContentDetailScreen(
              contentId = screen.contentId,
              viewModel = viewModel
            )
          }
          is Screen.AdminDashboard -> {
            AdminDashboardScreen(viewModel = viewModel)
          }
          is Screen.AdminEditContent -> {
            AdminEditContentScreen(
              contentId = screen.contentId,
              initialDepartmentId = screen.initialDepartmentId,
              viewModel = viewModel
            )
          }
          is Screen.AdminManageDepartments -> {
            AdminManageDepartmentsScreen(viewModel = viewModel)
          }
          is Screen.UserRequests -> {
            UserRequestsScreen(
              viewModel = viewModel,
              userName = user.name,
              userMobile = user.mobile
            )
          }
        }
      }

      if (showAuthDialog) {
        AuthDialog(
          currentRole = userRole,
          onRoleSelected = { role -> viewModel.setUserRole(role) },
          onAdminLogin = { pin -> viewModel.verifyAndLoginAdmin(pin) },
          onDismiss = { showAuthDialog = false },
          onLogout = onLogout
        )
      }
    }
  }
}
