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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solarhub.data.SessionManager
import com.example.solarhub.data.SolarHubDatabase
import com.example.solarhub.data.SolarHubRepository
import com.example.solarhub.network.SolarHubApiClient
import com.example.solarhub.network.SolarHubRealtimeClient
import com.example.solarhub.ui.components.AuthDialog
import com.example.solarhub.ui.screens.AdminDashboardScreen
import com.example.solarhub.ui.screens.AdminEditContentScreen
import com.example.solarhub.ui.screens.AdminManageDepartmentsScreen
import com.example.solarhub.ui.screens.ContentDetailScreen
import com.example.solarhub.ui.screens.DepartmentScreen
import com.example.solarhub.ui.screens.HomeScreen
import com.example.solarhub.ui.screens.LoginScreen
import com.example.solarhub.viewmodel.Screen
import com.example.solarhub.viewmodel.SessionUiState
import com.example.solarhub.viewmodel.SolarHubViewModel
import com.example.solarhub.viewmodel.SolarHubViewModelFactory
import com.example.ui.theme.SolarHubTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
    )

    val database = SolarHubDatabase.getDatabase(applicationContext)
    val sessionManager = SessionManager(applicationContext)
    val apiClient = SolarHubApiClient()
    val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val realtimeClient = SolarHubRealtimeClient(applicationContext, apiClient.httpClient, repoScope)
    val repository = SolarHubRepository(
      dao = database.solarHubDao(),
      sessionManager = sessionManager,
      apiClient = apiClient,
      realtimeClient = realtimeClient,
      scope = repoScope
    )
    val viewModelFactory = SolarHubViewModelFactory(repository)

    setContent {
      val viewModel: SolarHubViewModel = viewModel(factory = viewModelFactory)
      SolarHubApp(viewModel = viewModel)
    }
  }
}

@Composable
fun SolarHubApp(viewModel: SolarHubViewModel) {
  val context = LocalContext.current
  val sessionState by viewModel.sessionState.collectAsState()
  val currentScreen by viewModel.currentScreen.collectAsState()
  val userRole by viewModel.userRole.collectAsState()
  val session by viewModel.session.collectAsState()
  val apiBaseUrl by viewModel.apiBaseUrl.collectAsState()
  val isBusy by viewModel.isBusy.collectAsState()
  val toastMessage by viewModel.toastMessage.collectAsState()

  var showAuthDialog by remember { mutableStateOf(false) }

  LaunchedEffect(toastMessage) {
    toastMessage?.let { msg ->
      Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
      viewModel.clearToast()
    }
  }

  SolarHubTheme {
    Surface(modifier = Modifier.fillMaxSize()) {
      when (sessionState) {
        SessionUiState.Loading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }

        SessionUiState.Unauthenticated -> {
          LoginScreen(
            defaultServerUrl = apiBaseUrl,
            isBusy = isBusy,
            onTestConnection = { url, onResult ->
              viewModel.testServerConnection(url, onResult)
            },
            onEmployeeLogin = { name, mobile, serverUrl, onResult ->
              viewModel.loginEmployee(name, mobile, serverUrl, onResult)
            },
            onAdminLogin = { password, serverUrl, onResult ->
              viewModel.loginAdmin(password, serverUrl, onResult)
            }
          )
        }

        is SessionUiState.Authenticated -> {
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
            }
          }

          if (showAuthDialog) {
            AuthDialog(
              currentRole = userRole,
              currentUserName = session?.name ?: "",
              currentUserMobile = session?.mobile ?: "",
              currentServerUrl = apiBaseUrl,
              isOfflineSession = session?.isOfflineSession == true,
              onAdminLogin = { password, onResult ->
                viewModel.loginAdmin(password, null, onResult)
              },
              onUpdateServerUrl = { url ->
                viewModel.updateServerUrl(url)
              },
              onTestConnection = { url, onResult ->
                viewModel.testServerConnection(url, onResult)
              },
              onLogout = {
                viewModel.logout()
                showAuthDialog = false
              },
              onDismiss = { showAuthDialog = false }
            )
          }
        }
      }
    }
  }
}
