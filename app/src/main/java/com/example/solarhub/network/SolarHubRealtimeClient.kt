package com.example.solarhub.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.solarhub.model.ContentItem
import com.example.solarhub.model.Department
import com.example.solarhub.model.SyncConnectionState
import com.example.solarhub.model.UserRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.math.min

sealed interface RealtimeEvent {
  data class Connected(val serverTime: Long) : RealtimeEvent
  data class DepartmentUpdated(val department: Department, val serverTime: Long) : RealtimeEvent
  data class DepartmentDeleted(val id: String, val serverTime: Long) : RealtimeEvent
  data class ContentUpdated(val item: ContentItem, val serverTime: Long) : RealtimeEvent
  data class ContentUnpublished(val id: String, val serverTime: Long) : RealtimeEvent
  data class ContentDeleted(val id: String, val serverTime: Long) : RealtimeEvent
  data class RequestUpdated(val request: UserRequest, val serverTime: Long) : RealtimeEvent
  data class RequestDeleted(val id: String, val serverTime: Long) : RealtimeEvent
}

class SolarHubRealtimeClient(
  private val context: Context,
  private val httpClient: OkHttpClient,
  private val scope: CoroutineScope
) {
  private val _connectionState = MutableStateFlow(SyncConnectionState.DISCONNECTED)
  val connectionState: StateFlow<SyncConnectionState> = _connectionState.asStateFlow()

  private val _events = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 64)
  val events: SharedFlow<RealtimeEvent> = _events.asSharedFlow()

  private var webSocket: WebSocket? = null
  private var currentBaseUrl: String? = null
  private var currentToken: String? = null
  private var reconnectJob: Job? = null
  private var retryAttempt = 0
  private var isStarted = false
  private var networkCallback: ConnectivityManager.NetworkCallback? = null

  fun start(baseUrl: String, token: String) {
    currentBaseUrl = baseUrl.trimEnd('/')
    currentToken = token
    isStarted = true
    retryAttempt = 0
    registerNetworkCallback()
    openWebSocket()
  }

  fun stop() {
    isStarted = false
    reconnectJob?.cancel()
    reconnectJob = null
    webSocket?.close(1000, "Logged out")
    webSocket = null
    unregisterNetworkCallback()
    _connectionState.value = SyncConnectionState.DISCONNECTED
  }

  fun updateConnectionState(state: SyncConnectionState) {
    _connectionState.value = state
  }

  private fun openWebSocket() {
    val baseUrl = currentBaseUrl ?: return
    val token = currentToken ?: return
    if (!isStarted) return

    webSocket?.cancel()
    _connectionState.value = SyncConnectionState.CONNECTING

    val wsBase = when {
      baseUrl.startsWith("https://") -> "wss://" + baseUrl.removePrefix("https://")
      baseUrl.startsWith("http://") -> "ws://" + baseUrl.removePrefix("http://")
      else -> "ws://$baseUrl"
    }
    val encodedToken = URLEncoder.encode(token, "UTF-8")
    val request = Request.Builder()
      .url("$wsBase/ws?token=$encodedToken")
      .build()

    webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: Response) {
        retryAttempt = 0
        _connectionState.value = SyncConnectionState.LIVE
      }

      override fun onMessage(webSocket: WebSocket, text: String) {
        try {
          val obj = JSONObject(text)
          val type = obj.optString("type")
          val serverTime = obj.optLong("serverTime", System.currentTimeMillis())
          val data = obj.optJSONObject("data")

          val event: RealtimeEvent? = when (type) {
            "CONNECTED" -> RealtimeEvent.Connected(serverTime)
            "DEPARTMENT_UPDATED" -> data?.let {
              RealtimeEvent.DepartmentUpdated(SolarHubApiClient.parseDepartment(it), serverTime)
            }
            "DEPARTMENT_DELETED" -> data?.let {
              RealtimeEvent.DepartmentDeleted(it.getString("id"), serverTime)
            }
            "CONTENT_UPDATED" -> data?.let {
              RealtimeEvent.ContentUpdated(SolarHubApiClient.parseContentItem(it), serverTime)
            }
            "CONTENT_UNPUBLISHED" -> data?.let {
              RealtimeEvent.ContentUnpublished(it.getString("id"), serverTime)
            }
            "CONTENT_DELETED" -> data?.let {
              RealtimeEvent.ContentDeleted(it.getString("id"), serverTime)
            }
            "REQUEST_CREATED", "REQUEST_UPDATED" -> data?.let {
              RealtimeEvent.RequestUpdated(SolarHubApiClient.parseUserRequest(it), serverTime)
            }
            "REQUEST_DELETED" -> data?.let {
              RealtimeEvent.RequestDeleted(it.getString("id"), serverTime)
            }
            else -> null
          }

          if (event != null) {
            scope.launch(Dispatchers.Default) {
              _events.emit(event)
            }
          }
        } catch (_: Exception) {
          // ignore malformed messages
        }
      }

      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        if (isStarted && code != 4001) {
          _connectionState.value = SyncConnectionState.OFFLINE
          scheduleReconnect()
        } else {
          _connectionState.value = SyncConnectionState.DISCONNECTED
        }
      }

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        if (isStarted) {
          _connectionState.value = SyncConnectionState.OFFLINE
          scheduleReconnect()
        }
      }
    })
  }

  private fun scheduleReconnect() {
    if (!isStarted) return
    reconnectJob?.cancel()
    reconnectJob = scope.launch(Dispatchers.IO) {
      val delayMs = min(30_000L, 1_500L * (1L shl min(retryAttempt, 4)))
      retryAttempt++
      delay(delayMs)
      if (isStarted) {
        openWebSocket()
      }
    }
  }

  private fun registerNetworkCallback() {
    if (networkCallback != null) return
    try {
      val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
      val req = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()
      val cb = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
          if (isStarted && _connectionState.value != SyncConnectionState.LIVE) {
            retryAttempt = 0
            openWebSocket()
          }
        }

        override fun onLost(network: Network) {
          if (isStarted) {
            _connectionState.value = SyncConnectionState.OFFLINE
          }
        }
      }
      cm.registerNetworkCallback(req, cb)
      networkCallback = cb
    } catch (_: Exception) {
      // Ignore on Robolectric/environments without ConnectivityManager permission
    }
  }

  private fun unregisterNetworkCallback() {
    val cb = networkCallback ?: return
    try {
      val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
      cm?.unregisterNetworkCallback(cb)
    } catch (_: Exception) {
    } finally {
      networkCallback = null
    }
  }
}
