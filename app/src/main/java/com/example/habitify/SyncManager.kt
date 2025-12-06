package com.example.habitify

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SyncManager(private val context: Context) {

    private val TAG = "SyncManager"
    private val sessionManager = SessionManager(context)

    // Function to update habit status with instant UI refresh
    fun updateHabitStatus(habitId: Int, status: String, onSuccess: (Habit) -> Unit, onError: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = sessionManager.getUserId()

                val jsonObject = JSONObject().apply {
                    put("user_id", userId)
                    put("habit_id", habitId)
                    put("status", status)
                }

                Log.d(TAG, "Updating habit status: $habitId to $status")

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonObject.toString().toRequestBody(mediaType)

                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(ApiConfig.UPDATE_HABIT_STATUS_URL)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val success = jsonResponse.getBoolean("success")

                            if (success) {
                                val data = jsonResponse.getJSONObject("data")
                                val habitJson = data.getJSONObject("habit")

                                // Parse the updated habit
                                val updatedHabit = parseHabitFromJson(habitJson, status)
                                onSuccess(updatedHabit)

                                Log.d(TAG, "Habit status updated successfully")
                            } else {
                                val message = jsonResponse.getString("message")
                                onError("Error: $message")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "JSON parsing error: ${e.message}", e)
                            onError("Failed to update habit")
                        }
                    } else {
                        onError("Failed to connect to server")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Network error: ${e.message}")
                }
            }
        }
    }

    private fun parseHabitFromJson(json: JSONObject, category: String? = null): Habit {
        return Habit(
            id = json.getInt("id"),
            title = json.getString("title"),
            description = json.optString("description", null),
            frequency = json.getString("frequency"),
            colorCode = json.optString("color_code", "#4CAF50"),
            iconName = json.optString("icon_name", "default"),
            currentStreak = json.optInt("current_streak", 0),
            longestStreak = json.optInt("longest_streak", 0),
            todayStatus = json.optString("today_status", "todo"),
            category = category ?: when (json.optString("today_status", "todo")) {
                "completed" -> "completed"
                "failed" -> "failed"
                else -> "todo"
            },
            createdAt = json.optString("created_at", ""),
            createdAtFormatted = json.optString("created_at_formatted", ""),
            todayCompletedAt = json.optString("today_completed_at", null)
        )
    }
}