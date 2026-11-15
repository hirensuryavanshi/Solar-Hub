package com.example

import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import com.example.solarhub.data.CloudSync
import com.example.solarhub.data.SessionStore
import com.example.solarhub.data.SessionUser
import com.example.solarhub.data.SolarHubDatabase
import com.example.solarhub.data.SolarHubRepository
import com.example.solarhub.model.UserRole
import com.example.solarhub.ui.components.AuthDialog
import com.example.solarhub.ui.screens.AdminDashboardScreen
import com.example.solarhub.ui.screens.AdminEditContentScreen
import com.example.solarhub.ui.screens.AdminManageDepartmentsScreen
import com.example.solarhub.ui.screens.AdminUsersScreen
import com.example.solarhub.ui.screens.NotificationsScreen
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
    val cloud = CloudSync(applicationContext, database.solarHubDao())
    val viewModelFactory = SolarHubViewModelFactory(repository, cloud)
    val session = SessionStore(applicationContext)

    setContent {
      val viewModel: SolarHubViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = viewModelFactory
      )

      var user by remember { mutableStateOf(session.load()) }
      var blockedMessage by remember { mutableStateOf<String?>(null) }
      val currentUser = user

      if (currentUser == null) {
        // First screen: simple login with name and mobile number
        SolarHubTheme {
          LoginScreen(
            onLogin = { name, mobile ->
              viewModel.registerLogin(name, mobile) { blocked ->
                if (blocked) {
                  blockedMessage = "This number is blocked by the admin. Please contact the admin."
                } else {
                  val newUser = SessionUser(name, mobile)
                  session.save(newUser)
                  blockedMessage = null
                  user = newUser
                }
              }
            },
            message = blockedMessage
          )
        }
      } else {
        SolarHubApp(
          viewModel = viewModel,
          user = currentUser,
          session = session,
          onLogout = {
            session.clear()
            viewModel.setUserRole(UserRole.EMPLOYEE)
            user = null
          },
          onBlocked = {
            session.clear()
            viewModel.setUserRole(UserRole.EMPLOYEE)
            blockedMessage = "This number is blocked by the admin. Please contact the admin."
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
  session: SessionStore,
  onLogout: () -> Unit,
  onBlocked: () -> Unit
) {
  val currentScreen by viewModel.currentScreen.collectAsState()
  val userRole by viewModel.userRole.collectAsState()
  var showAuthDialog by remember { mutableStateOf(false) }

  // Every few seconds: download new posts, upload admin changes, check if blocked
  LaunchedEffect(user.mobile) {
    while (true) {
      viewModel.syncNow(user.name, user.mobile)
      delay(15_000)
    }
  }
  val syncMessage by viewModel.syncMessage.collectAsState()
  val toastContext = LocalContext.current
  LaunchedEffect(syncMessage) {
    syncMessage?.let {
      Toast.makeText(toastContext, it, Toast.LENGTH_LONG).show()
      viewModel.clearSyncMessage()
    }
  }
  val savedUser by viewModel.observeUser(user.mobile).collectAsState(initial = null)
  LaunchedEffect(savedUser?.isBlocked) {
    if (savedUser?.isBlocked == true) onBlocked()
  }

  // Notification bell: published posts newer than the last time the user opened the bell
  val published by viewModel.allPublished.collectAsState()
  var seenAt by remember { mutableStateOf(session.loadSeenAt()) }
  val unreadCount = published.count { it.content.publishedAt > seenAt }

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
              onOpenAuthDialog = { showAuthDialog = true },
              unreadCount = unreadCount,
              onOpenNotifications = { viewModel.navigateTo(Screen.Notifications) }
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
          is Screen.AdminUsers -> {
            AdminUsersScreen(viewModel = viewModel)
          }
          is Screen.Notifications -> {
            NotificationsScreen(
              viewModel = viewModel,
              seenAt = seenAt,
              onSeen = {
                val now = System.currentTimeMillis()
                session.saveSeenAt(now)
                seenAt = now
              }
            )
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
