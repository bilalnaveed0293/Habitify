package com.example.habitify

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.View
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    private lateinit var ivBackArrow: ImageView
    private lateinit var ivAvatar: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var switchNotifications: SwitchMaterial

    private lateinit var cardManageAccount: CardView
    private lateinit var cardLanguages: CardView
    private lateinit var cardHelpCenter: CardView
    private lateinit var cardPushNotifications: CardView
    private lateinit var cardLogout: CardView
    private lateinit var cardThemes: CardView

    private lateinit var sessionManager: SessionManager

    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 100
    private val PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        // Initialize SessionManager
        sessionManager = SessionManager(this)

        // Initialize views
        initializeViews()

        // Set up user data
        setupUserData()

        // Set up click listeners
        setupClickListeners()

        // Set up window insets
        setupWindowInsets()

        // Load saved notification preference
        loadNotificationPreference()
    }

    private fun initializeViews() {
        ivBackArrow = findViewById(R.id.iv_back_arrow)
        ivAvatar = findViewById(R.id.iv_avatar)
        tvUsername = findViewById(R.id.tv_username)
        switchNotifications = findViewById(R.id.switch_notifications)

        // Get CardViews by finding them directly (more reliable approach)
        cardManageAccount = findViewById(R.id.card_manage_account)
        cardLanguages = findViewById(R.id.card_languages)
        cardHelpCenter = findViewById(R.id.card_help_center)
        cardPushNotifications = findViewById(R.id.card_push_notifications)
        cardLogout = findViewById(R.id.card_logout)
        cardThemes = findViewById(R.id.card_themes)
    }

    private fun setupUserData() {
        // Get user data from session
        val userName = sessionManager.getUserName()

        // Update username display
        tvUsername.text = userName

        // Load profile picture
        ProfilePictureUtils.loadProfilePictureFromSession(this, ivAvatar, sessionManager)
    }

    private fun setupClickListeners() {
        // Back arrow
        ivBackArrow.setOnClickListener {
            onBackPressed()
        }

        // Manage Account
        cardManageAccount.setOnClickListener {
            navigateToManageAccount()
        }

        // Languages
        cardLanguages.setOnClickListener {
            showInDevelopmentMessage("Language Selection")
        }

        // Help Center
        cardHelpCenter.setOnClickListener {
            showInDevelopmentMessage("Help Center")
        }

        // Push Notifications switch
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            handleNotificationToggle(isChecked)
        }

        // Push Notifications card (optional - if user clicks anywhere on card)
        cardPushNotifications.setOnClickListener {
            // Toggle the switch when card is clicked
            switchNotifications.isChecked = !switchNotifications.isChecked
        }

        // Logout
        cardLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        // Themes
        cardThemes.setOnClickListener {
            showInDevelopmentMessage("Theme Selection")
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadNotificationPreference() {
        // Load saved notification preference from SessionManager or SharedPreferences
        val prefs = getSharedPreferences("HabitifyPrefs", MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
        switchNotifications.isChecked = notificationsEnabled
    }

    private fun handleNotificationToggle(isEnabled: Boolean) {
        if (isEnabled) {
            // Request notification permission if not granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Request permission
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                } else {
                    // Permission already granted
                    saveNotificationPreference(true)
                    showToast("Notifications enabled")
                }
            } else {
                // For older versions, just save preference
                saveNotificationPreference(true)
                showToast("Notifications enabled")
            }
        } else {
            // Disable notifications
            saveNotificationPreference(false)
            showToast("Notifications disabled")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    saveNotificationPreference(true)
                    showToast("Notification permission granted")
                } else {
                    // Permission denied
                    switchNotifications.isChecked = false
                    saveNotificationPreference(false)
                    showToast("Notification permission denied")
                }
            }
        }
    }

    private fun saveNotificationPreference(isEnabled: Boolean) {
        // Save notification preference to SharedPreferences
        val prefs = getSharedPreferences("HabitifyPrefs", MODE_PRIVATE)
        prefs.edit().putBoolean("notifications_enabled", isEnabled).apply()
    }

    private fun navigateToManageAccount() {
        val intent = Intent(this, ManageAccountActivity::class.java)
        startActivity(intent)
    }

    private fun showInDevelopmentMessage(featureName: String) {
        AlertDialog.Builder(this)
            .setTitle("Coming Soon")
            .setMessage("$featureName is currently in development. Stay tuned!")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                performLogout()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun performLogout() {
        // Clear user session
        sessionManager.clearSession()

        // Show logout message
        showToast("Logged out successfully")

        // Navigate to LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh user data when returning to settings
        setupUserData()
    }
}