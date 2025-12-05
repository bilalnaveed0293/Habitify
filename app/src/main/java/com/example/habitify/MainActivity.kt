package com.example.habitify

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
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

class MainActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var ivAvatar: ImageView
    private lateinit var ivNotifications: ImageView
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var recyclerViewHabits: RecyclerView
    private lateinit var tabLayoutCategories: TabLayout
    private lateinit var tvEmptyState: TextView
    private lateinit var tvStatsTodo: TextView
    private lateinit var tvStatsCompleted: TextView
    private lateinit var tvStatsFailed: TextView

    private lateinit var sessionManager: SessionManager
    private lateinit var habitAdapter: HabitAdapter

    private var allHabits: List<Habit> = emptyList()
    private var currentCategory: String = "todo"

    private val TAG = "MainActivity"
    private val GET_USER_HABITS_URL = ApiConfig.GET_USER_HABITS_URL
    private val UPDATE_HABIT_STATUS_URL = ApiConfig.UPDATE_HABIT_STATUS_URL


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize SessionManager
        sessionManager = SessionManager(this)

        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            navigateToLogin()
            return
        }
         refreshUserProfile()
        // Initialize views
        initializeViews()

        // Set up user data
        setupUserData()

        // Set up tabs
        setupTabs()

        // Set up recycler view
        setupRecyclerView()

        // Set up bottom navigation
        setupBottomNavigation()

        // Set up window insets
        setupWindowInsets()

        // Load user habits
        loadUserHabits()

        Log.d(TAG, "User logged in: ${sessionManager.getUserName()}")
    }

    private fun initializeViews() {
        tvWelcome = findViewById(R.id.tv_welcome)
        ivAvatar = findViewById(R.id.iv_avatar)
        ivNotifications = findViewById(R.id.iv_notifications)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        recyclerViewHabits = findViewById(R.id.recyclerViewHabits)
        tabLayoutCategories = findViewById(R.id.tabLayoutCategories)
        tvEmptyState = findViewById(R.id.tv_empty_state)
        tvStatsTodo = findViewById(R.id.tv_stats_todo)
        tvStatsCompleted = findViewById(R.id.tv_stats_completed)
        tvStatsFailed = findViewById(R.id.tv_stats_failed)
    }

    private fun setupUserData() {
        // Get user data from session
        val userName = sessionManager.getUserName()

        // Update welcome message with user's name
        tvWelcome.text = "Welcome $userName!"

        // Load profile picture
        ProfilePictureUtils.loadProfilePictureFromSession(this, ivAvatar, sessionManager)
    }

    private fun setupTabs() {
        tabLayoutCategories.addTab(tabLayoutCategories.newTab().setText("To Do").setTag("todo"))
        tabLayoutCategories.addTab(tabLayoutCategories.newTab().setText("Completed").setTag("completed"))
        tabLayoutCategories.addTab(tabLayoutCategories.newTab().setText("Failed").setTag("failed"))

        tabLayoutCategories.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.tag?.let { category ->
                    currentCategory = category.toString()
                    filterHabitsByCategory(currentCategory)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        habitAdapter = HabitAdapter(
            onHabitClick = { habit ->
                // Handle habit click (mark as complete, edit, etc.)
                showHabitDetails(habit)
            },
            onHabitLongClick = { habit ->
                // Handle long click (delete, edit, etc.)
                showHabitOptions(habit)
            },
            onCompleteClick = { habit ->  // NEW: Add completion handler
                showCompletionDialog(habit)
            }
        )

        recyclerViewHabits.layoutManager = LinearLayoutManager(this)
        recyclerViewHabits.adapter = habitAdapter
    }
    private fun showCompletionDialog(habit: Habit) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Complete Habit")
            .setMessage("Mark '${habit.title}' as completed?")
            .setPositiveButton("Mark Complete") { _, _ ->
                updateHabitStatus(habit.id, "completed")
            }
            .setNegativeButton("Mark Failed") { _, _ ->
                updateHabitStatus(habit.id, "failed")
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    // NEW: Function to update habit status
    private fun updateHabitStatus(habitId: Int, status: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = sessionManager.getUserId()

                // Create request body
                val jsonObject = JSONObject().apply {
                    put("user_id", userId)
                    put("habit_id", habitId)
                    put("status", status)
                }

                Log.d(TAG, "Updating habit status: $habitId to $status")
                Log.d(TAG, "API URL: $UPDATE_HABIT_STATUS_URL")
                Log.d(TAG, "Request Body: ${jsonObject.toString()}")

                // Make API call
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonObject.toString().toRequestBody(mediaType)

                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(UPDATE_HABIT_STATUS_URL)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                Log.d(TAG, "Update Status Response Code: ${response.code}")
                Log.d(TAG, "Update Status Response Body: $responseBody")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val success = jsonResponse.getBoolean("success")

                            if (success) {
                                showToast("Habit marked as $status!")

                                // Refresh habits to show updated status
                                loadUserHabits()

                            } else {
                                val message = jsonResponse.getString("message")
                                showToast("Error: $message")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "JSON parsing error: ${e.message}", e)
                            showToast("Failed to update habit")
                        }
                    } else {
                        showToast("Failed to connect to server")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Update status error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast("Network error: ${e.message}")
                }
            }
        }
    }


    private fun loadUserHabits() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = sessionManager.getUserId()

                // Create request body
                val jsonObject = JSONObject().apply {
                    put("user_id", userId)
                }

                Log.d(TAG, "Loading habits for user: $userId")
                Log.d(TAG, "API URL: $GET_USER_HABITS_URL")
                Log.d(TAG, "Request Body: ${jsonObject.toString()}")

                // Make API call using GET method with query parameter
                val encodedUserId = java.net.URLEncoder.encode(userId.toString(), "UTF-8")
                val url = "$GET_USER_HABITS_URL?user_id=$encodedUserId"

                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Accept", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                Log.d(TAG, "Habits Response Code: ${response.code}")
                Log.d(TAG, "Habits Response Body: $responseBody")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val success = jsonResponse.getBoolean("success")

                            if (success) {
                                val data = jsonResponse.getJSONObject("data")
                                val habitsJson = data.getJSONObject("habits")
                                val statsJson = data.getJSONObject("statistics")

                                // Parse habits
                                val habitsList = mutableListOf<Habit>()

                                // Parse todo habits
                                if (habitsJson.has("todo")) {
                                    val todoHabits = habitsJson.getJSONArray("todo")
                                    for (i in 0 until todoHabits.length()) {
                                        val habitJson = todoHabits.getJSONObject(i)
                                        habitsList.add(parseHabitFromJson(habitJson, "todo"))
                                    }
                                }

                                // Parse completed habits
                                if (habitsJson.has("completed")) {
                                    val completedHabits = habitsJson.getJSONArray("completed")
                                    for (i in 0 until completedHabits.length()) {
                                        val habitJson = completedHabits.getJSONObject(i)
                                        habitsList.add(parseHabitFromJson(habitJson, "completed"))
                                    }
                                }

                                // Parse failed habits
                                if (habitsJson.has("failed")) {
                                    val failedHabits = habitsJson.getJSONArray("failed")
                                    for (i in 0 until failedHabits.length()) {
                                        val habitJson = failedHabits.getJSONObject(i)
                                        habitsList.add(parseHabitFromJson(habitJson, "failed"))
                                    }
                                }

                                allHabits = habitsList

                                // Update statistics
                                tvStatsTodo.text = statsJson.getInt("todo_count").toString()
                                tvStatsCompleted.text = statsJson.getInt("completed_count").toString()
                                tvStatsFailed.text = statsJson.getInt("failed_count").toString()

                                // Filter by current category
                                filterHabitsByCategory(currentCategory)

                            } else {
                                val message = jsonResponse.getString("message")
                                showToast("Failed to load habits: $message")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "JSON parsing error: ${e.message}", e)
                            showToast("Failed to parse habits data")
                        }
                    } else {
                        showToast("Failed to connect to server. Response code: ${response.code}")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Load habits error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast("Network error: ${e.message}")
                }
            }
        }
    }

    private fun parseHabitFromJson(json: JSONObject, category: String): Habit {
        return Habit(
            id = json.getInt("id"),
            title = json.getString("title"),
            description = json.optString("description", null),
            frequency = json.getString("frequency"),
            colorCode = json.getString("color_code"),
            iconName = json.getString("icon_name"),
            currentStreak = json.getInt("current_streak"),
            longestStreak = json.getInt("longest_streak"),
            todayStatus = json.getString("today_status"),
            category = category,
            createdAt = json.getString("created_at"),
            createdAtFormatted = json.optString("created_at_formatted", ""),
            todayCompletedAt = json.optString("today_completed_at", null)
        )
    }

    private fun filterHabitsByCategory(category: String) {
        val filteredHabits = allHabits.filter { it.category == category }
        habitAdapter.updateHabits(filteredHabits)

        // Show/hide empty state
        if (filteredHabits.isEmpty()) {
            recyclerViewHabits.visibility = android.view.View.GONE
            tvEmptyState.visibility = android.view.View.VISIBLE
            tvEmptyState.text = when (category) {
                "todo" -> "No habits to do today. Great job!"
                "completed" -> "No habits completed yet. Keep going!"
                "failed" -> "No failed habits. Perfect!"
                else -> "No habits found"
            }
        } else {
            recyclerViewHabits.visibility = android.view.View.VISIBLE
            tvEmptyState.visibility = android.view.View.GONE
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Already on home
                    true
                }
                R.id.nav_add -> {
                    navigateToAddHabit()
                    true
                }
                R.id.nav_settings -> {
                    navigateToSettings()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun showHabitDetails(habit: Habit) {
        // TODO: Implement habit details dialog or activity
        showToast("Clicked: ${habit.title}")
    }


    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    } companion object {
        private const val ADD_HABIT_REQUEST_CODE = 1001
    }

    private fun navigateToAddHabit() {
        val intent = Intent(this, AddHabitActivity::class.java)
        startActivityForResult(intent, ADD_HABIT_REQUEST_CODE)
    }
    private fun navigateToSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh user data and habits when returning to MainActivity
        if (sessionManager.isLoggedIn()) {
            refreshUserProfile() // Refresh profile first
            setupUserData() // Then update UI
            loadUserHabits() // Then load habits
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_HABIT_REQUEST_CODE && resultCode == RESULT_OK) {
            // Refresh habits when returning from AddHabitActivity
            loadUserHabits()
            showToast("Habit added successfully!")
        }
    }
    private fun refreshUserProfile() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = sessionManager.getUserId()

                // Create request body
                val jsonObject = JSONObject().apply {
                    put("user_id", userId)
                }

                val url = ApiConfig.GET_PROFILE_URL

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonObject.toString().toRequestBody(mediaType)

                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(url)
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
                                val user = data.getJSONObject("user")

                                // Update session with fresh data
                                sessionManager.saveUserSession(
                                    userId = user.getInt("id"),
                                    userName = user.getString("name"),
                                    userEmail = user.getString("email"),
                                    userToken = sessionManager.getUserToken(),
                                    theme = user.optString("theme", "system"),
                                    phone = user.optString("phone", ""),
                                    profilePicture = user.optString("profile_picture", null),
                                    profilePictureUrl = user.optString("profile_picture_url", null)
                                )

                                // Update UI
                                setupUserData()

                                Log.d(TAG, "Profile refreshed successfully")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing profile: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing profile: ${e.message}")
            }
        }
    }
    private fun showHabitOptions(habit: Habit) {
        val options = arrayOf("Mark Complete", "Mark Failed", "Delete Habit", "Cancel")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(habit.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> updateHabitStatus(habit.id, "completed")
                    1 -> updateHabitStatus(habit.id, "failed")
                    2 -> showDeleteConfirmation(habit)
                    3 -> { /* Cancel */ }
                }
            }
            .show()
    }

    private fun showDeleteConfirmation(habit: Habit) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Habit")
            .setMessage("Are you sure you want to delete '${habit.title}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteHabit(habit.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteHabit(habitId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = sessionManager.getUserId()

                // Create request body
                val jsonObject = JSONObject().apply {
                    put("user_id", userId)
                    put("habit_id", habitId)
                }

                Log.d(TAG, "Deleting habit: $habitId")
                Log.d(TAG, "API URL: ${ApiConfig.DELETE_HABIT_URL}")
                Log.d(TAG, "Request Body: ${jsonObject.toString()}")

                // Make API call
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonObject.toString().toRequestBody(mediaType)

                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(ApiConfig.DELETE_HABIT_URL)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                Log.d(TAG, "Delete Response Code: ${response.code}")
                Log.d(TAG, "Delete Response Body: $responseBody")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val success = jsonResponse.getBoolean("success")

                            if (success) {
                                showToast("Habit deleted successfully!")

                                // Remove the habit from the list and update UI
                                allHabits = allHabits.filter { it.id != habitId }
                                filterHabitsByCategory(currentCategory)

                            } else {
                                val message = jsonResponse.getString("message")
                                showToast("Error: $message")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "JSON parsing error: ${e.message}", e)
                            showToast("Failed to delete habit")
                        }
                    } else {
                        showToast("Failed to connect to server")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Delete habit error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast("Network error: ${e.message}")
                }
            }
        }
    }
}