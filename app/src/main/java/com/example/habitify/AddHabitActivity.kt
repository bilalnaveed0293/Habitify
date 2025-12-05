package com.example.habitify

import android.app.TimePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AddHabitActivity : AppCompatActivity() {

    private lateinit var ivBackArrow: ImageView
    private lateinit var etSearch: EditText
    private lateinit var chipGroupCategories: LinearLayout
    private lateinit var recyclerViewHabits: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var btnAddCustom: MaterialButton

    // Custom habit dialog views
    private lateinit var customHabitDialog: View
    private lateinit var etCustomTitle: EditText
    private lateinit var etCustomDescription: EditText
    private lateinit var tvReminderTime: TextView
    private lateinit var switchReminder: Switch
    private lateinit var btnSetTime: MaterialButton
    private lateinit var btnSaveCustom: MaterialButton
    private lateinit var btnCancelCustom: MaterialButton

    private lateinit var sessionManager: SessionManager
    private var habitAdapter: PredefinedHabitAdapter? = null // Make nullable

    private var predefinedHabits: List<PredefinedHabit> = emptyList()
    private var filteredHabits: List<PredefinedHabit> = emptyList()
    private var categories: List<String> = emptyList()
    private var selectedCategory: String = "all"
    private var selectedReminderTime: String = "09:00:00"
    private var isReminderEnabled: Boolean = true

    private val TAG = "AddHabitActivity"
    private val GET_PREDEFINED_HABITS_URL = ApiConfig.GET_PREDEFINED_HABITS_URL
    private val CREATE_HABIT_URL = ApiConfig.CREATE_HABIT_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_habit)

        // Initialize SessionManager
        sessionManager = SessionManager(this)

        // Initialize views
        initializeViews()

        // Debug: Check if views are found
        Log.d(TAG, "RecyclerView found: ${::recyclerViewHabits.isInitialized}")
        Log.d(TAG, "Empty TextView found: ${::tvEmptyState.isInitialized}")

        // Initialize RecyclerView adapter FIRST
        habitAdapter = PredefinedHabitAdapter(emptyList()) { habit ->
            showAddHabitConfirmation(habit)
        }

        // Set up RecyclerView
        recyclerViewHabits.layoutManager = LinearLayoutManager(this)
        recyclerViewHabits.adapter = habitAdapter

        // Debug: Verify RecyclerView setup
        Log.d(TAG, "RecyclerView layout manager set: ${recyclerViewHabits.layoutManager != null}")
        Log.d(TAG, "RecyclerView adapter set: ${recyclerViewHabits.adapter != null}")

        // Set up click listeners
        setupClickListeners()

        // Set up window insets
        setupWindowInsets()

        // Load predefined habits
        loadPredefinedHabits()
    }

    private fun initializeViews() {
        ivBackArrow = findViewById(R.id.iv_back_arrow)
        etSearch = findViewById(R.id.et_search)
        chipGroupCategories = findViewById(R.id.chipGroupCategories)
        recyclerViewHabits = findViewById(R.id.recyclerViewHabits)
        tvEmptyState = findViewById(R.id.tv_empty_state)
        btnAddCustom = findViewById(R.id.btn_add_custom)

        // Custom habit dialog views
        customHabitDialog = layoutInflater.inflate(R.layout.dialog_custom_habit, null)
        etCustomTitle = customHabitDialog.findViewById(R.id.et_custom_title)
        etCustomDescription = customHabitDialog.findViewById(R.id.et_custom_description)
        tvReminderTime = customHabitDialog.findViewById(R.id.tv_reminder_time)
        switchReminder = customHabitDialog.findViewById(R.id.switch_reminder)
        btnSetTime = customHabitDialog.findViewById(R.id.btn_set_time)
        btnSaveCustom = customHabitDialog.findViewById(R.id.btn_save_custom)
        btnCancelCustom = customHabitDialog.findViewById(R.id.btn_cancel_custom)
    }

    private fun setupClickListeners() {
        // Back arrow
        ivBackArrow.setOnClickListener {
            onBackPressed()
        }

        // Search functionality
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterHabits()
            }
        })

        // Add custom habit button
        btnAddCustom.setOnClickListener {
            showCustomHabitDialog()
        }

        // Set up custom habit dialog buttons
        btnSetTime.setOnClickListener {
            showTimePicker()
        }

        btnSaveCustom.setOnClickListener {
            saveCustomHabit()
        }

        btnCancelCustom.setOnClickListener {
            hideCustomHabitDialog()
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadPredefinedHabits() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val searchQuery = etSearch.text.toString()
                val encodedSearch = java.net.URLEncoder.encode(searchQuery, "UTF-8")
                val url = "$GET_PREDEFINED_HABITS_URL?search=$encodedSearch&category=$selectedCategory"

                Log.d(TAG, "Loading predefined habits from: $url")

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

                Log.d(TAG, "Response Code: ${response.code}")
                Log.d(TAG, "Response Body: $responseBody")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val success = jsonResponse.getBoolean("success")

                            if (success) {
                                val data = jsonResponse.getJSONObject("data")
                                val habitsArray = data.getJSONArray("habits")

                                // Debug: Log number of habits received
                                Log.d(TAG, "Number of habits received: ${habitsArray.length()}")

                                // Parse habits
                                val habitsList = mutableListOf<PredefinedHabit>()
                                for (i in 0 until habitsArray.length()) {
                                    val habitJson = habitsArray.getJSONObject(i)
                                    habitsList.add(parsePredefinedHabit(habitJson))

                                    // Debug: Log each habit
                                    Log.d(TAG, "Habit ${i + 1}: ${habitJson.getString("title")}")
                                }

                                // Debug: Log parsed habits count
                                Log.d(TAG, "Parsed habits count: ${habitsList.size}")

                                // Parse categories
                                val categoriesArray = data.optJSONArray("categories")
                                val categoriesList = mutableListOf<String>()
                                categoriesList.add("all")
                                if (categoriesArray != null) {
                                    for (i in 0 until categoriesArray.length()) {
                                        categoriesList.add(categoriesArray.getString(i))
                                    }
                                }

                                predefinedHabits = habitsList
                                categories = categoriesList

                                // Debug: Log before setting up categories
                                Log.d(TAG, "Setting up ${categories.size} categories")

                                // Setup categories filter
                                setupCategoriesFilter()

                                // Filter habits
                                filterHabits()

                                // Debug: Log after filtering
                                Log.d(TAG, "Filtered habits count: ${filteredHabits.size}")

                            } else {
                                val message = jsonResponse.getString("message")
                                Log.e(TAG, "API returned error: $message")
                                showToast("Failed to load habits: $message")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "JSON parsing error: ${e.message}", e)
                            showToast("Failed to parse habits data: ${e.message}")
                        }
                    } else {
                        Log.e(TAG, "HTTP error: ${response.code}")
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

    private fun parsePredefinedHabit(json: JSONObject): PredefinedHabit {
        return PredefinedHabit(
            id = json.getInt("id"),
            title = json.getString("title"),
            description = json.optString("description", null),
            category = json.optString("category", "general"),
            iconName = json.optString("icon_name", "default"),
            colorCode = json.optString("color_code", "#4CAF50"),
            frequency = json.optString("frequency", "daily"),
            suggestedCount = json.optInt("suggested_count", 0)
        )
    }

    private fun setupCategoriesFilter() {
        chipGroupCategories.removeAllViews()

        for (category in categories) {
            val chip = layoutInflater.inflate(R.layout.item_category_chip, chipGroupCategories, false) as MaterialCardView
            val chipText = chip.findViewById<TextView>(R.id.tv_category)

            val displayName = when (category) {
                "all" -> "All"
                "mindfulness" -> "Mindfulness"
                "health" -> "Health"
                "fitness" -> "Fitness"
                "learning" -> "Learning"
                else -> category.replaceFirstChar { it.uppercase() }
            }

            chipText.text = displayName
            chip.tag = category

            // Set selected state
            if (category == selectedCategory) {
                chip.setCardBackgroundColor(getColor(R.color.primary_color))
                chipText.setTextColor(getColor(R.color.white))
            } else {
                chip.setCardBackgroundColor(getColor(R.color.chip_background))
                chipText.setTextColor(getColor(R.color.primary_text))
            }

            chip.setOnClickListener {
                selectedCategory = category
                setupCategoriesFilter()
                loadPredefinedHabits()
            }

            chipGroupCategories.addView(chip)
        }
    }

    private fun filterHabits() {
        val searchQuery = etSearch.text.toString().toLowerCase(Locale.getDefault())

        filteredHabits = if (searchQuery.isEmpty()) {
            predefinedHabits
        } else {
            predefinedHabits.filter {
                it.title.toLowerCase(Locale.getDefault()).contains(searchQuery) ||
                        (it.description?.toLowerCase(Locale.getDefault())?.contains(searchQuery) == true)
            }
        }

        updateHabitList()
    }

    private fun updateHabitList() {
        // Debug: Log before updating
        Log.d(TAG, "Updating habit list with ${filteredHabits.size} habits")

        habitAdapter?.updateHabits(filteredHabits)

        // Debug: Check adapter and recycler view
        Log.d(TAG, "Habit adapter is null: ${habitAdapter == null}")
        Log.d(TAG, "RecyclerView visibility: ${recyclerViewHabits.visibility}")
        Log.d(TAG, "Empty state visibility: ${tvEmptyState.visibility}")

        // Show/hide empty state
        if (filteredHabits.isEmpty()) {
            recyclerViewHabits.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE
            tvEmptyState.text = "No habits found. Try a different search."
            Log.d(TAG, "Showing empty state")
        } else {
            recyclerViewHabits.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
            Log.d(TAG, "Showing ${filteredHabits.size} habits in RecyclerView")
        }
    }

    private fun showAddHabitConfirmation(habit: PredefinedHabit) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Add Habit")
            .setMessage("Add \"${habit.title}\" to your habits?")
            .setPositiveButton("Add") { _, _ ->
                addPredefinedHabit(habit)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun addPredefinedHabit(habit: PredefinedHabit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = sessionManager.getUserId()

                // Create request body
                val jsonObject = JSONObject().apply {
                    put("user_id", userId)
                    put("title", habit.title)
                    put("description", habit.description ?: "")
                    put("color_code", habit.colorCode)
                    put("icon_name", habit.iconName)
                    put("frequency", habit.frequency)
                    put("reminder_time", "09:00:00")
                    put("reminder_enabled", 1)
                }

                Log.d(TAG, "Adding habit: ${habit.title}")
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
                    .url(CREATE_HABIT_URL)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                Log.d(TAG, "Add Habit Response Code: ${response.code}")
                Log.d(TAG, "Add Habit Response Body: $responseBody")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val success = jsonResponse.getBoolean("success")

                            if (success) {
                                showToast("Habit added successfully!")

                                // Close activity and return to main
                                setResult(RESULT_OK)
                                finish()

                            } else {
                                val message = jsonResponse.getString("message")
                                showToast("Error: $message")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "JSON parsing error: ${e.message}", e)
                            showToast("Failed to add habit")
                        }
                    } else {
                        showToast("Failed to connect to server. Response code: ${response.code}")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Add habit error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast("Network error: ${e.message}")
                }
            }
        }
    }

    private fun showCustomHabitDialog() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(customHabitDialog)
            .setCancelable(false)
            .create()

        dialog.show()

        // Initialize dialog
        etCustomTitle.text.clear()
        etCustomDescription.text.clear()
        tvReminderTime.text = "09:00 AM"
        switchReminder.isChecked = true
        selectedReminderTime = "09:00:00"
        isReminderEnabled = true
    }

    private fun hideCustomHabitDialog() {
        // Find and dismiss the dialog
        val parentView = customHabitDialog.parent as? ViewGroup
        parentView?.let {
            val dialog = it.parent as? androidx.appcompat.app.AlertDialog
            dialog?.dismiss()
        }
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                // Format time
                selectedReminderTime = String.format("%02d:%02d:00", selectedHour, selectedMinute)

                // Display in 12-hour format
                val amPm = if (selectedHour < 12) "AM" else "PM"
                val displayHour = if (selectedHour % 12 == 0) 12 else selectedHour % 12
                tvReminderTime.text = String.format("%02d:%02d %s", displayHour, selectedMinute, amPm)
            },
            hour,
            minute,
            false // 24-hour format
        )

        timePickerDialog.show()
    }

    private fun saveCustomHabit() {
        val title = etCustomTitle.text.toString().trim()
        val description = etCustomDescription.text.toString().trim()

        if (title.isEmpty()) {
            etCustomTitle.error = "Habit title is required"
            etCustomTitle.requestFocus()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = sessionManager.getUserId()

                // Create request body
                val jsonObject = JSONObject().apply {
                    put("user_id", userId)
                    put("title", title)
                    put("description", if (description.isNotEmpty()) description else "")
                    put("color_code", "#4CAF50") // Default green color
                    put("icon_name", "default")
                    put("frequency", "daily")
                    put("reminder_time", if (isReminderEnabled) selectedReminderTime else "09:00:00")
                    put("reminder_enabled", if (isReminderEnabled) 1 else 0)
                }

                Log.d(TAG, "Adding custom habit: $title")

                // Make API call
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonObject.toString().toRequestBody(mediaType)

                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(CREATE_HABIT_URL)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                Log.d(TAG, "Add Custom Habit Response Code: ${response.code}")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val success = jsonResponse.getBoolean("success")

                            if (success) {
                                showToast("Custom habit added successfully!")

                                // Close dialog
                                hideCustomHabitDialog()

                                // Close activity and return to main
                                setResult(RESULT_OK)
                                finish()

                            } else {
                                val message = jsonResponse.getString("message")
                                showToast("Error: $message")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "JSON parsing error: ${e.message}", e)
                            showToast("Failed to add habit")
                        }
                    } else {
                        showToast("Failed to connect to server")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Add custom habit error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast("Network error: ${e.message}")
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}