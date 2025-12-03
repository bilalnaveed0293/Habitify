package com.example.habitify

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
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

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSavePassword: MaterialButton
    private lateinit var ivBackArrow: ImageView

    private var userEmail: String = ""
    private var verifiedCode: String = ""

    private val TAG = "ResetPasswordActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reset_password)

        // Get data from intent
        userEmail = intent.getStringExtra("email") ?: ""
        verifiedCode = intent.getStringExtra("verified_code") ?: ""

        // Initialize views
        initializeViews()

        // Set click listeners
        setupClickListeners()

        // Set up window insets
        setupWindowInsets()

        Log.d(TAG, "Reset password for: $userEmail")
    }

    private fun initializeViews() {
        etNewPassword = findViewById(R.id.et_new_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        btnSavePassword = findViewById(R.id.btn_save_password)
        ivBackArrow = findViewById(R.id.iv_back_arrow)
    }

    private fun setupClickListeners() {
        btnSavePassword.setOnClickListener {
            attemptPasswordReset()
        }

        ivBackArrow.setOnClickListener {
            navigateToVerification()
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun attemptPasswordReset() {
        val newPassword = etNewPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        // Basic validation
        if (newPassword.isEmpty()) {
            showToast("New password is required")
            return
        }

        if (newPassword.length < 6) {
            showToast("Password must be at least 6 characters")
            return
        }

        if (confirmPassword.isEmpty()) {
            showToast("Please confirm your password")
            return
        }

        if (newPassword != confirmPassword) {
            showToast("Passwords do not match")
            return
        }

        // Show loading state
        btnSavePassword.isEnabled = false

        // Reset password (local for now - in real app, call API)
        simulatePasswordReset(newPassword)
    }

    private fun simulatePasswordReset(newPassword: String) {
        CoroutineScope(Dispatchers.IO).launch {
            // Simulate network delay
            kotlinx.coroutines.delay(1000)

            withContext(Dispatchers.Main) {
                // Success
                showToast("Password reset successfully!")

                // Navigate to login
                navigateToLogin()
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            putExtra("email", userEmail)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finishAffinity() // Close all activities
    }

    private fun navigateToVerification() {
        val intent = Intent(this, VerificationCodeActivity::class.java).apply {
            putExtra("email", userEmail)
        }
        startActivity(intent)
        finish()
    }
}