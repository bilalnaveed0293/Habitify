package com.example.habitify

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var ivAvatar: ImageView
    private lateinit var ivNotifications: ImageView
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var sessionManager: SessionManager

    private val TAG = "MainActivity"

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

        // Initialize views
        initializeViews()

        // Set up user data
        setupUserData()

        // Set up bottom navigation
        setupBottomNavigation()

        // Set up window insets
        setupWindowInsets()

        Log.d(TAG, "User logged in: ${sessionManager.getUserName()}")
    }

    private fun initializeViews() {
        tvWelcome = findViewById(R.id.tv_welcome)
        ivAvatar = findViewById(R.id.iv_avatar)
        ivNotifications = findViewById(R.id.iv_notifications)
        bottomNavigation = findViewById(R.id.bottom_navigation)
    }

    private fun setupUserData() {
        // Get user data from session
        val userName = sessionManager.getUserName()

        // Update welcome message with user's name
        tvWelcome.text = "Welcome $userName!"

        // Load profile picture
        ProfilePictureUtils.loadProfilePictureFromSession(this, ivAvatar, sessionManager)
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

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToAddHabit() {
        showToast("Add habit coming soon!")
    }

    private fun navigateToSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh user data when returning to MainActivity
        if (sessionManager.isLoggedIn()) {
            setupUserData()
        }
    }
}