package com.example.habitify

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
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

class LoginActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnSignup: MaterialButton
    private lateinit var tvForgotPassword: TextView

    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        sessionManager = SessionManager(this)
        if (sessionManager.isLoggedIn()) {
            navigateToMainActivity()
            return
        }
        // Initialize views
        initializeViews()

        // Set click listeners
        setupClickListeners()

        // Set up window insets
        setupWindowInsets()

        // Log the API URL for debugging
        Log.d(TAG, "Login URL: ${ApiConfig.LOGIN_URL}")
    }

    private fun initializeViews() {
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        btnSignup = findViewById(R.id.btn_signup)
        tvForgotPassword = findViewById(R.id.tv_forgot_password)

        // Set up password visibility toggle
        val ivPasswordToggle = findViewById<android.widget.ImageView>(R.id.iv_password_toggle)
        ivPasswordToggle.setOnClickListener {
            togglePasswordVisibility()
        }

        // Set test credentials (for development only - remove in production)
        setTestCredentials()
    }

    private fun setTestCredentials() {
        // For testing with your PHP backend
        // These should match the test data in your register.php
        etEmail.setText("test@example.com")
        etPassword.setText("password123")
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            attemptLogin()
        }

        btnSignup.setOnClickListener {
            navigateToSignUp()
        }

        tvForgotPassword.setOnClickListener {
            navigateToForgotPassword()
        }
    }

    private fun togglePasswordVisibility() {
        val ivPasswordToggle = findViewById<android.widget.ImageView>(R.id.iv_password_toggle)
        val etPassword = findViewById<EditText>(R.id.et_password)

        if (etPassword.transformationMethod == android.text.method.HideReturnsTransformationMethod.getInstance()) {
            // Hide password
            etPassword.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
            ivPasswordToggle.setImageResource(R.drawable.ic_visibility_off)
            ivPasswordToggle.contentDescription = "Show password"
        } else {
            // Show password
            etPassword.transformationMethod = android.text.method.HideReturnsTransformationMethod.getInstance()
            ivPasswordToggle.setImageResource(R.drawable.ic_visibility_off)
            ivPasswordToggle.contentDescription = "Hide password"
        }

        // Move cursor to end
        etPassword.setSelection(etPassword.text.length)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun attemptLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Basic validation
        if (!validateInput(email, password)) {
            return
        }

        // Show loading state
        btnLogin.text = "Logging in..."
        btnLogin.isEnabled = false

        // Call login API
        performLogin(email, password)
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            etEmail.requestFocus()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Enter a valid email"
            etEmail.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            etPassword.requestFocus()
            return false
        }

        if (password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            etPassword.requestFocus()
            return false
        }

        return true
    }

    private fun performLogin(email: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create request body
                val jsonObject = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                }

                Log.d(TAG, "Sending login request to: ${ApiConfig.LOGIN_URL}")
                Log.d(TAG, "Request body: ${jsonObject.toString()}")

                val response = makeApiRequest(ApiConfig.LOGIN_URL, jsonObject.toString())

                withContext(Dispatchers.Main) {
                    handleLoginResponse(response, email)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Login error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showError("Network error: ${e.message}")
                    resetLoginButton()
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
        val responseBody = response.body?.string()

        Log.d(TAG, "Response code: ${response.code}")
        Log.d(TAG, "Response body: $responseBody")

        return if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
            responseBody
        } else {
            throw Exception("API request failed: ${response.code} - ${response.message}")
        }
    }

    private fun handleLoginResponse(responseBody: String?, email: String) {
        try {
            if (responseBody == null) {
                showError("No response from server")
                resetLoginButton()
                return
            }

            val jsonObject = JSONObject(responseBody)
            val status = jsonObject.optString("status")
            val message = jsonObject.optString("message")

            if (status == "success") {
                // Login successful
                val data = jsonObject.optJSONObject("data")

                if (data != null) {
                    val user = data.optJSONObject("user")
                    val token = data.optString("token")

                    val userId = user?.optInt("id") ?: 0
                    val userName = user?.optString("name") ?: "User"
                    val userEmail = user?.optString("email") ?: email
                    val userTheme = user?.optString("theme") ?: "system"

                    // Save user session
                    sessionManager.saveUserSession(
                        userId = userId,
                        userName = userName,
                        userEmail = userEmail,
                        userToken = token,
                        theme = userTheme
                    )

                    Log.d(TAG, "User session saved: $userName")
                }

                // Show success message
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()

                // Navigate to main activity
                navigateToMainActivity()

            } else {
                // Login failed
                showError("Login failed: $message")
                resetLoginButton()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Response parsing error: ${e.message}", e)
            showError("Invalid server response format")
            resetLoginButton()
        }
    }


    private fun saveUserData(userId: Int, userName: String, userEmail: String, userToken: String) {
        // For now, just log it. You should save to SharedPreferences or Room
        Log.d(TAG, "User logged in: $userName ($userEmail)")
        Log.d(TAG, "Token: $userToken")

        // TODO: Implement proper user session management
        // SharedPreferences or Room database
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e(TAG, "Error: $message")
    }

    private fun resetLoginButton() {
        btnLogin.text = "Log In"
        btnLogin.isEnabled = true
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }


    private fun navigateToSignUp() {
        val intent = Intent(this, SignUpActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToForgotPassword() {
        val intent = Intent(this, ForgotPasswordActivity::class.java)
        startActivity(intent)
    }
}