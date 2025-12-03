package com.example.habitify

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY: Long = 1500 // 1.5 seconds
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SessionManager
        sessionManager = SessionManager(this)

        hideActionBar()
        setContentView(R.layout.activity_splash)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Check login state and navigate
        Handler(Looper.getMainLooper()).postDelayed({
            checkLoginAndNavigate()
        }, SPLASH_DELAY)
    }

    private fun hideActionBar() {
        supportActionBar?.hide()
    }

    private fun checkLoginAndNavigate() {
        if (sessionManager.isLoggedIn()) {
            // User is already logged in, go to MainActivity
            navigateToMainActivity()
        } else {
            // User is not logged in, go to LoginActivity
            navigateToLogin()
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}