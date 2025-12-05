package com.example.habitify

import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import java.util.*
import java.util.concurrent.TimeUnit

class AddHabitActivity : AppCompatActivity() {

    private lateinit var ivBackArrow: ImageView
    private lateinit var etSearch: EditText
    private lateinit var chipGroupCategories: LinearLayout
    private lateinit var recyclerViewHabits: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var btnAddCustom: MaterialButton

    private lateinit var sessionManager: SessionManager
    private var habitAdapter: PredefinedHabitAdapter? = null

    private var predefinedHabits: List<PredefinedHabit> = emptyList()
    private var filteredHabits: List<PredefinedHabit> = emptyList()
    private var categories: List<String> = emptyList()
    private var selectedCategory: String = "all"

    // Custom habit variables
    private var selectedColorCode: String = "#4CAF50"
    private var selectedIconName: String = "default"
    private var selectedReminderTime: String = "09:00:00"
    private var selectedFrequency: String = "daily"
    private var selectedHabitCategory: String = "custom"

    private val CREATE_HABIT_URL = ApiConfig.CREATE_HABIT_URL
    private val SAVE_CUSTOM_HABIT_URL = ApiConfig.SAVE_CUSTOM_HABIT_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_habit)

        sessionManager = SessionManager(this)
        initializeViews()

        habitAdapter = PredefinedHabitAdapter(
            onHabitClick = { habit ->
                if (habit.isCustom) {
                    showCustomHabitOptions(habit)
                } else {
                    showPredefinedHabitOptions(habit)
                }
            },
            onEditClick = { habit ->
                if (habit.isCustom) {
                    showEditCustomHabitDialog(habit)
                }
            }
        )

        recyclerViewHabits.layoutManager = LinearLayoutManager(this)
        recyclerViewHabits.adapter = habitAdapter

        setupClickListeners()
        setupWindowInsets()
        loadPredefinedHabits()
    }

    private fun initializeViews() {
        ivBackArrow = findViewById(R.id.iv_back_arrow)
        etSearch = findViewById(R.id.et_search)
        chipGroupCategories = findViewById(R.id.chipGroupCategories)
        recyclerViewHabits = findViewById(R.id.recyclerViewHabits)
        tvEmptyState = findViewById(R.id.tv_empty_state)
        btnAddCustom = findViewById(R.id.btn_add_custom)
    }

    private fun setupClickListeners() {
        ivBackArrow.setOnClickListener {
            finish()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterHabits()
            }
        })

        btnAddCustom.setOnClickListener {
            showCustomHabitDialog()
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
                val userId = sessionManager.getUserId()

                val url = "${ApiConfig.GET_PREDEFINED_HABITS_URL}?search=$encodedSearch&category=$selectedCategory&user_id=$userId"

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

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            if (jsonResponse.getBoolean("success")) {
                                val data = jsonResponse.getJSONObject("data")
                                val habitsArray = data.getJSONArray("habits")

                                val habitsList = mutableListOf<PredefinedHabit>()
                                for (i in 0 until habitsArray.length()) {
                                    val habitJson = habitsArray.getJSONObject(i)

                                    habitsList.add(PredefinedHabit(
                                        id = habitJson.getInt("id"),
                                        title = habitJson.getString("title"),
                                        description = habitJson.optString("description", null),
                                        category = habitJson.optString("category", "general"),
                                        iconName = habitJson.optString("icon_name", "default"),
                                        colorCode = habitJson.optString("color_code", "#4CAF50"),
                                        frequency = habitJson.optString("frequency", "daily"),
                                        suggestedCount = habitJson.optInt("suggested_count", 0),
                                        isCustom = habitJson.optBoolean("is_custom", false),
                                        customHabitId = habitJson.optInt("custom_habit_id", 0)
                                    ))
                                }

                                predefinedHabits = habitsList
                                filterHabits()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
                "custom" -> "Custom"
                else -> category.replaceFirstChar { it.uppercase() }
            }

            chipText.text = displayName
            chip.tag = category

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
        habitAdapter?.updateHabits(filteredHabits)

        if (filteredHabits.isEmpty()) {
            recyclerViewHabits.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE
        } else {
            recyclerViewHabits.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
        }
    }

    private fun showPredefinedHabitOptions(habit: PredefinedHabit) {
        val options = arrayOf("Add to My Habits", "Edit Before Adding", "Cancel")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(habit.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> addHabitToUser(habit)
                    1 -> showEditPredefinedHabitDialog(habit)
                    2 -> { /* Cancel */ }
                }
            }
            .show()
    }

    private fun showCustomHabitOptions(habit: PredefinedHabit) {
        val options = arrayOf("Add to My Habits", "Edit Custom Habit", "Delete Custom Habit", "Cancel")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(habit.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> addHabitToUser(habit)
                    1 -> showEditCustomHabitDialog(habit)
                    2 -> showDeleteConfirmationDialog(habit)
                    3 -> { /* Cancel */ }
                }
            }
            .show()
    }

    // MAIN CUSTOM HABIT DIALOG
    private fun showCustomHabitDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_habit, null)

        // Get all the views
        val etHabitTitle = dialogView.findViewById<EditText>(R.id.et_habit_title)
        val etHabitDescription = dialogView.findViewById<EditText>(R.id.et_habit_description)
        val tvReminderTime = dialogView.findViewById<TextView>(R.id.tv_reminder_time)
        val switchReminder = dialogView.findViewById<Switch>(R.id.switch_reminder)
        val btnSetTime = dialogView.findViewById<MaterialButton>(R.id.btn_set_time)
        val btnSaveCustom = dialogView.findViewById<MaterialButton>(R.id.btn_save_custom)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val rgFrequency = dialogView.findViewById<RadioGroup>(R.id.rg_frequency)

        // Hide "Save as Habit" button
        dialogView.findViewById<MaterialButton>(R.id.btn_save_habit)?.visibility = View.GONE

        // Setup category chips
        setupCategoryChips(dialogView)

        // Setup color palette
        setupColorPalette(dialogView)

        // Setup icon palette
        setupIconPalette(dialogView)

        // Setup time picker
        btnSetTime.setOnClickListener {
            showTimePickerDialog(tvReminderTime)
        }

        // Setup frequency
        rgFrequency.setOnCheckedChangeListener { _, checkedId ->
            selectedFrequency = if (checkedId == R.id.rb_daily) "daily" else "weekly"
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSaveCustom.setOnClickListener {
            val title = etHabitTitle.text.toString().trim()
            val description = etHabitDescription.text.toString().trim()

            if (title.isEmpty()) {
                etHabitTitle.error = "Habit title is required"
                return@setOnClickListener
            }

            createCustomHabit(
                title = title,
                description = description,
                category = selectedHabitCategory,
                iconName = selectedIconName,
                colorCode = selectedColorCode,
                frequency = selectedFrequency,
                reminderTime = selectedReminderTime,
                isReminderEnabled = switchReminder.isChecked,
                dialog = dialog
            )
        }

        dialog.show()
    }

    private fun setupCategoryChips(dialogView: View) {
        // Get all category chip views
        val chips = listOf(
            dialogView.findViewById<TextView>(R.id.tv_category_custom),
            dialogView.findViewById<TextView>(R.id.tv_category_mindfulness),
            dialogView.findViewById<TextView>(R.id.tv_category_health),
            dialogView.findViewById<TextView>(R.id.tv_category_fitness),
            dialogView.findViewById<TextView>(R.id.tv_category_learning)
        )

        // Set click listeners
        chips.forEach { chip ->
            chip?.setOnClickListener {
                // Reset all chips
                chips.forEach { c ->
                    c?.setBackgroundResource(R.drawable.rounded_chip)
                    c?.setTextColor(getColor(R.color.primary_text))
                }

                // Select clicked chip
                chip.setBackgroundResource(R.drawable.rounded_chip_selected)
                chip.setTextColor(getColor(R.color.white))

                // Update selected category
                selectedHabitCategory = when (chip.id) {
                    R.id.tv_category_custom -> "custom"
                    R.id.tv_category_mindfulness -> "mindfulness"
                    R.id.tv_category_health -> "health"
                    R.id.tv_category_fitness -> "fitness"
                    R.id.tv_category_learning -> "learning"
                    else -> "custom"
                }
            }
        }
    }

    private fun setupColorPalette(dialogView: View) {
        val llColorPalette = dialogView.findViewById<LinearLayout>(R.id.ll_category_chips)
        llColorPalette?.removeAllViews()

        val colors = listOf(
            "#4CAF50", "#2196F3", "#FF9800", "#9C27B0",
            "#FF5722", "#673AB7", "#F44336", "#3F51B5",
            "#FFC107", "#009688", "#795548", "#607D8B"
        )

        colors.forEach { color ->
            val colorView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(48.dpToPx(), 48.dpToPx()).apply {
                    marginEnd = 8
                }
                background = ContextCompat.getDrawable(this@AddHabitActivity, R.drawable.circle_background)
                setColorFilter(Color.parseColor(color))
                tag = color

                if (color == selectedColorCode) {
                    setImageResource(R.drawable.ic_check_circle)
                    imageTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
                }

                setOnClickListener {
                    selectedColorCode = color
                    setupColorPalette(dialogView)
                    // Update icon colors too
                    setupIconPalette(dialogView)
                }
            }

            llColorPalette?.addView(colorView)
        }
    }

    private fun setupIconPalette(dialogView: View) {
        val llIconPalette = dialogView.findViewById<LinearLayout>(R.id.ll_category_chips)
        llIconPalette?.removeAllViews()

        val icons = listOf(
            Pair("meditation", R.drawable.ic_meditation),
            Pair("water", R.drawable.ic_water),
            Pair("exercise", R.drawable.ic_exercise),
            Pair("read", R.drawable.ic_book),
            Pair("journal", R.drawable.ic_journal),
            Pair("learn", R.drawable.ic_learn),
            Pair("nosugar", R.drawable.ic_no_sugar),
            Pair("sleep", R.drawable.ic_sleep),
            Pair("gratitude", R.drawable.ic_gratitude),
            Pair("walk", R.drawable.ic_walk),
            Pair("default", R.drawable.ic_logo)
        )

        icons.forEach { (iconName, iconRes) ->
            val iconView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(48.dpToPx(), 48.dpToPx()).apply {
                    marginEnd = 8
                }
                setImageResource(iconRes)
                setColorFilter(Color.parseColor(selectedColorCode))
                tag = iconName
                background = if (iconName == selectedIconName) {
                    ContextCompat.getDrawable(this@AddHabitActivity, R.drawable.icon_selected_background)
                } else {
                    null
                }

                setOnClickListener {
                    selectedIconName = iconName
                    setupIconPalette(dialogView)
                }
            }

            llIconPalette?.addView(iconView)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun showTimePickerDialog(timeTextView: TextView) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                selectedReminderTime = String.format("%02d:%02d:00", selectedHour, selectedMinute)

                val amPm = if (selectedHour < 12) "AM" else "PM"
                val displayHour = if (selectedHour % 12 == 0) 12 else selectedHour % 12
                timeTextView.text = String.format("%02d:%02d %s", displayHour, selectedMinute, amPm)
            },
            hour,
            minute,
            false
        ).show()
    }

    private fun createCustomHabit(
        title: String,
        description: String,
        category: String,
        iconName: String,
        colorCode: String,
        frequency: String,
        reminderTime: String,
        isReminderEnabled: Boolean,
        dialog: androidx.appcompat.app.AlertDialog?
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = sessionManager.getUserId()

                val jsonObject = JSONObject().apply {
                    put("user_id", userId)
                    put("title", title)
                    put("description", description)
                    put("category", category)
                    put("icon_name", iconName)
                    put("color_code", colorCode)
                    put("frequency", frequency)
                    put("reminder_time", reminderTime)
                    put("reminder_enabled", if (isReminderEnabled) 1 else 0)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonObject.toString().toRequestBody(mediaType)

                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(SAVE_CUSTOM_HABIT_URL)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                withContext(Dispatchers.Main) {
                    dialog?.dismiss()

                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val success = jsonResponse.getBoolean("success")

                            if (success) {
                                showToast("Custom habit created and added to your habits!")
                                setResult(RESULT_OK)
                                finish()
                            } else {
                                val message = jsonResponse.getString("message")
                                showToast("Error: $message")
                            }
                        } catch (e: Exception) {
                            showToast("Failed to create custom habit")
                        }
                    } else {
                        showToast("Failed to connect to server")
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    dialog?.dismiss()
                    showToast("Network error: ${e.message}")
                }
            }
        }
    }

    private fun showEditPredefinedHabitDialog(habit: PredefinedHabit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_habit, null)

        val etHabitTitle = dialogView.findViewById<EditText>(R.id.et_habit_title)
        val etHabitDescription = dialogView.findViewById<EditText>(R.id.et_habit_description)
        val tvReminderTime = dialogView.findViewById<TextView>(R.id.tv_reminder_time)
        val switchReminder = dialogView.findViewById<Switch>(R.id.switch_reminder)
        val btnSetTime = dialogView.findViewById<MaterialButton>(R.id.btn_set_time)
        val btnSaveCustom = dialogView.findViewById<MaterialButton>(R.id.btn_save_custom)
        val btnSaveHabit = dialogView.findViewById<MaterialButton>(R.id.btn_save_habit)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val rgFrequency = dialogView.findViewById<RadioGroup>(R.id.rg_frequency)

        // Hide "Save as Custom", show "Save and Add"
        btnSaveCustom.visibility = View.GONE
        btnSaveHabit.text = "Save and Add to Habits"

        // Hide category chips for predefined habit editing
        hideCategoryChips(dialogView)

        // Set initial values
        etHabitTitle.setText(habit.title)
        etHabitDescription.setText(habit.description ?: "")
        selectedColorCode = habit.colorCode
        selectedIconName = habit.iconName
        selectedFrequency = habit.frequency

        if (habit.frequency == "daily") {
            rgFrequency.check(R.id.rb_daily)
        } else {
            rgFrequency.check(R.id.rb_weekly)
        }

        // Setup palettes
        setupColorPalette(dialogView)
        setupIconPalette(dialogView)

        btnSetTime.setOnClickListener {
            showTimePickerDialog(tvReminderTime)
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSaveHabit.setOnClickListener {
            val title = etHabitTitle.text.toString().trim()
            val description = etHabitDescription.text.toString().trim()

            if (title.isEmpty()) {
                etHabitTitle.error = "Habit title is required"
                return@setOnClickListener
            }

            // Create modified habit and add to user
            val modifiedHabit = PredefinedHabit(
                id = habit.id,
                title = title,
                description = if (description.isNotEmpty()) description else null,
                category = habit.category,
                iconName = selectedIconName,
                colorCode = selectedColorCode,
                frequency = selectedFrequency,
                suggestedCount = habit.suggestedCount,
                isCustom = false,
                customHabitId = null
            )

            addHabitToUser(modifiedHabit)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun hideCategoryChips(dialogView: View) {
        // Hide category chips
        dialogView.findViewById<TextView>(R.id.tv_category_custom)?.visibility = View.GONE
        dialogView.findViewById<TextView>(R.id.tv_category_mindfulness)?.visibility = View.GONE
        dialogView.findViewById<TextView>(R.id.tv_category_health)?.visibility = View.GONE
        dialogView.findViewById<TextView>(R.id.tv_category_fitness)?.visibility = View.GONE
        dialogView.findViewById<TextView>(R.id.tv_category_learning)?.visibility = View.GONE

        // Hide category label
        val categoryContainer = dialogView.findViewById<LinearLayout>(R.id.ll_category_chips)?.parent as? HorizontalScrollView
        val parent = categoryContainer?.parent as? LinearLayout
        parent?.let {
            for (i in 0 until it.childCount) {
                val child = it.getChildAt(i)
                if (child is TextView && child.text == "Category") {
                    child.visibility = View.GONE
                    // Hide the category container too
                    if (i + 1 < it.childCount) {
                        it.getChildAt(i + 1).visibility = View.GONE
                    }
                    break
                }
            }
        }
    }

    private fun addHabitToUser(habit: PredefinedHabit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = sessionManager.getUserId()

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

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val success = jsonResponse.getBoolean("success")

                            if (success) {
                                showToast("Habit added to your habits!")
                                setResult(RESULT_OK)
                                finish()
                            } else {
                                val message = jsonResponse.getString("message")
                                showToast("Error: $message")
                            }
                        } catch (e: Exception) {
                            showToast("Failed to add habit")
                        }
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Network error: ${e.message}")
                }
            }
        }
    }

    private fun showEditCustomHabitDialog(habit: PredefinedHabit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_custom_habit, null)

        val etHabitTitle = dialogView.findViewById<EditText>(R.id.et_habit_title)
        val etHabitDescription = dialogView.findViewById<EditText>(R.id.et_habit_description)
        val tvReminderTime = dialogView.findViewById<TextView>(R.id.tv_reminder_time)
        val switchReminder = dialogView.findViewById<Switch>(R.id.switch_reminder)
        val btnSetTime = dialogView.findViewById<MaterialButton>(R.id.btn_set_time)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btn_save)
        val btnDelete = dialogView.findViewById<MaterialButton>(R.id.btn_delete)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)

        etHabitTitle.setText(habit.title)
        etHabitDescription.setText(habit.description ?: "")

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSetTime.setOnClickListener {
            showTimePickerDialog(tvReminderTime)
        }

        btnSave.setOnClickListener {
            val title = etHabitTitle.text.toString().trim()
            val description = etHabitDescription.text.toString().trim()

            if (title.isEmpty()) {
                etHabitTitle.error = "Habit title is required"
                return@setOnClickListener
            }

            updateCustomHabit(
                customHabitId = habit.customHabitId ?: 0,
                title = title,
                description = description,
                category = habit.category,
                iconName = habit.iconName,
                colorCode = habit.colorCode,
                frequency = habit.frequency,
                reminderTime = selectedReminderTime,
                isReminderEnabled = switchReminder.isChecked,
                dialog = dialog
            )
        }

        btnDelete.setOnClickListener {
            dialog.dismiss()
            showDeleteConfirmationDialog(habit)
        }

        dialog.show()
    }

    private fun updateCustomHabit(
        customHabitId: Int,
        title: String,
        description: String,
        category: String,
        iconName: String,
        colorCode: String,
        frequency: String,
        reminderTime: String,
        isReminderEnabled: Boolean,
        dialog: androidx.appcompat.app.AlertDialog?
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = sessionManager.getUserId()

                val jsonObject = JSONObject().apply {
                    put("user_id", userId)
                    put("custom_habit_id", customHabitId)
                    put("title", title)
                    put("description", description)
                    put("category", category)
                    put("icon_name", iconName)
                    put("color_code", colorCode)
                    put("frequency", frequency)
                    put("reminder_time", reminderTime)
                    put("reminder_enabled", if (isReminderEnabled) 1 else 0)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonObject.toString().toRequestBody(mediaType)

                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(SAVE_CUSTOM_HABIT_URL)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                withContext(Dispatchers.Main) {
                    dialog?.dismiss()

                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val success = jsonResponse.getBoolean("success")

                            if (success) {
                                showToast("Custom habit updated!")
                                loadPredefinedHabits()
                            }
                        } catch (e: Exception) {
                            showToast("Failed to update habit")
                        }
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    dialog?.dismiss()
                    showToast("Network error: ${e.message}")
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(habit: PredefinedHabit) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Custom Habit")
            .setMessage("Are you sure you want to delete '${habit.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                deleteCustomHabit(habit.customHabitId ?: 0)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCustomHabit(customHabitId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = sessionManager.getUserId()

                val jsonObject = JSONObject().apply {
                    put("user_id", userId)
                    put("custom_habit_id", customHabitId)
                    put("is_active", 0)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonObject.toString().toRequestBody(mediaType)

                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(SAVE_CUSTOM_HABIT_URL)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build()

                val response = client.newCall(request).execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        showToast("Custom habit deleted!")
                        loadPredefinedHabits()
                    }
                }

            } catch (e: Exception) {
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