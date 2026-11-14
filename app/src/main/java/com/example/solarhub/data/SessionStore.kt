package com.example.solarhub.data

import android.content.Context

data class SessionUser(
  val name: String,
  val mobile: String
)

/** Remembers the logged-in user (name + mobile number) on this phone. */
class SessionStore(context: Context) {

  private val prefs = context.applicationContext
    .getSharedPreferences("solar_hub_session", Context.MODE_PRIVATE)

  fun load(): SessionUser? {
    val name = prefs.getString("name", null)
    val mobile = prefs.getString("mobile", null)
    return if (!name.isNullOrBlank() && !mobile.isNullOrBlank()) SessionUser(name, mobile) else null
  }

  fun save(user: SessionUser) {
    prefs.edit()
      .putString("name", user.name)
      .putString("mobile", user.mobile)
      .apply()
  }

  fun clear() {
    prefs.edit().clear().apply()
  }
}
