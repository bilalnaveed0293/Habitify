package com.example.habitify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var btnResetPassword: MaterialButton
    private lateinit var ivBackArrow: ImageView

    private val TAG = "ForgotPasswordActivity"
    private val CHANNEL_ID = "habitify_reset_channel"
    private val NOTIFICATION_ID = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        // Create notification channel
        createNotificationChannel()

        // Initialize views
        initializeViews()

        // Set click listeners
        setupClickListeners()

        // Set up window insets
        setupWindowInsets()

        // Set test data for development (remove in production)
        setTestData()
    }

    private fun initializeViews() {
        etEmail = findViewById(R.id.et_email)
        btnResetPassword = findViewById(R.id.btn_reset_password)
        ivBackArrow = findViewById(R.id.iv_back_arrow)
    }

    private fun setTestData() {
        // For testing - remove in production
        etEmail.setText("test@example.com")
    }

    private fun setupClickListeners() {
        btnResetPassword.setOnClickListener {
            attemptPasswordReset()
        }

        ivBackArrow.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Password Reset Notifications"
            val description = "Notifications for password reset codes"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            channel.enableVibration(true)
            channel.vibrationPattern = longArrayOf(0, 500, 250, 500)

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun attemptPasswordReset() {
        val email = etEmail.text.toString().trim()

        // Basic validation
        if (email.isEmpty()) {
            showToast("Email is required")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Please enter a valid email")
            return
        }

        // Show loading state
        btnResetPassword.isEnabled = false

        // Check if email exists and send notification
        checkEmailAndSendNotification(email)
    }

    private fun checkEmailAndSendNotification(email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create request body
                val jsonObject = JSONObject().apply {
                    put("email", email)
                }

                Log.d(TAG, "Checking email: $email")

                // Call API to check if email exists
                val response = makeApiRequest(ApiConfig.CHECK_EMAIL_URL, jsonObject.toString())

                withContext(Dispatchers.Main) {
                    handleEmailCheckResponse(response, email)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Email check error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast("Network error. Please check your connection.")
                    btnResetPassword.isEnabled = true
                }
            }
        }
    }

    private fun makeApiRequest(url: String, jsonBody: String): String? {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val response = client.newCall(request).execute()
        return response.body?.string()
    }

    private fun handleEmailCheckResponse(responseBody: String?, email: String) {
        try {
            if (responseBody == null) {
                showToast("No response from server")
                btnResetPassword.isEnabled = true
                return
            }

            val jsonObject = JSONObject(responseBody)
            val status = jsonObject.optString("status", "error")
            val message = jsonObject.optString("message", "Unknown error")

            if (status == "success") {
                // Email exists, generate and send notification
                val data = jsonObject.optJSONObject("data")
                val userId = data?.optInt("user_id", 0)
                val userName = data?.optString("name", "User")

                // Generate a 6-digit code
                val resetCode = generateResetCode()

                // Send notification with the code
                sendNotificationWithCode(resetCode, userName.toString())

                // Navigate to verification with code
                navigateToVerification(email, resetCode)

            } else {
                // Email doesn't exist or error
                showToast("Email not found or error: $message")
                btnResetPassword.isEnabled = true
            }

        } catch (e: Exception) {
            Log.e(TAG, "Response parsing error: ${e.message}", e)
            showToast("Error processing response")
            btnResetPassword.isEnabled = true
        }
    }

    private fun generateResetCode(): String {
        // Generate a 6-digit code
        return (100000..999999).random().toString()
    }

    private fun sendNotificationWithCode(resetCode: String, userName: String) {
        // Create and show notification
        val notificationManager = ContextCompat.getSystemService(
            this,
            NotificationManager::class.java
        ) as NotificationManager

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Add notification icon
            .setContentTitle("Habitify Password Reset")
            .setContentText("Your reset code: $resetCode")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Hello $userName! Use this code to reset your password: $resetCode\n\nThis code will expire in 5 minutes.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)

        // Also show toast for easy access during development
        showToast("Check your notifications for the reset code!")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun navigateToVerification(email: String, resetCode: String) {
        val intent = Intent(this, VerificationCodeActivity::class.java).apply {
            putExtra("email", email)
            putExtra("reset_code", resetCode) // For verification
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}