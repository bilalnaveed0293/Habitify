package com.example.habitify

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val sharedPreferences: SharedPreferences
    private val editor: SharedPreferences.Editor

    companion object {
        private const val PREF_NAME = "HabitifySession"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_USER_ID = "userId"
        private const val KEY_USER_NAME = "userName"
        private const val KEY_USER_EMAIL = "userEmail"
        private const val KEY_USER_TOKEN = "userToken"
        private const val KEY_USER_THEME = "userTheme"
    }

    init {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
        editor.apply()
    }

    // Save user session
    fun saveUserSession(
        userId: Int,
        userName: String,
        userEmail: String,
        userToken: String,
        theme: String = "system"
    ) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putInt(KEY_USER_ID, userId)
        editor.putString(KEY_USER_NAME, userName)
        editor.putString(KEY_USER_EMAIL, userEmail)
        editor.putString(KEY_USER_TOKEN, userToken)
        editor.putString(KEY_USER_THEME, theme)
        editor.apply()
    }

    // Get user session
    fun getUserSession(): UserSession? {
        return if (isLoggedIn()) {
            UserSession(
                userId = sharedPreferences.getInt(KEY_USER_ID, 0),
                userName = sharedPreferences.getString(KEY_USER_NAME, "") ?: "",
                userEmail = sharedPreferences.getString(KEY_USER_EMAIL, "") ?: "",
                userToken = sharedPreferences.getString(KEY_USER_TOKEN, "") ?: "",
                theme = sharedPreferences.getString(KEY_USER_THEME, "system") ?: "system"
            )
        } else {
            null
        }
    }

    // Check if user is logged in
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // Get user name
    fun getUserName(): String {
        return sharedPreferences.getString(KEY_USER_NAME, "User") ?: "User"
    }

    // Get user email
    fun getUserEmail(): String {
        return sharedPreferences.getString(KEY_USER_EMAIL, "") ?: ""
    }

    // Get user ID
    fun getUserId(): Int {
        return sharedPreferences.getInt(KEY_USER_ID, 0)
    }

    // Get user token
    fun getUserToken(): String {
        return sharedPreferences.getString(KEY_USER_TOKEN, "") ?: ""
    }

    // Clear session (logout)
    fun clearSession() {
        editor.clear()
        editor.apply()
    }

    // Update user name
    fun updateUserName(name: String) {
        editor.putString(KEY_USER_NAME, name)
        editor.apply()
    }

    // Update theme
    fun updateTheme(theme: String) {
        editor.putString(KEY_USER_THEME, theme)
        editor.apply()
    }

    // Get theme
    fun getTheme(): String {
        return sharedPreferences.getString(KEY_USER_THEME, "system") ?: "system"
    }
}

// Data class for user session
data class UserSession(
    val userId: Int,
    val userName: String,
    val userEmail: String,
    val userToken: String,
    val theme: String
)