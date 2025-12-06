package com.example.habitify

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
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

class SignUpActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSignup: MaterialButton
    private lateinit var btnLoginRedirect: MaterialButton
    private lateinit var ivBackArrow: ImageView

    private val TAG = "SignUpActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        // Initialize views
        initializeViews()

        // Set click listeners
        setupClickListeners()

        // Set up window insets
        setupWindowInsets()

        // Log the API URL for debugging
        Log.d(TAG, "Register URL: ${ApiConfig.REGISTER_URL}")

        // Set test data for development (remove in production)
        //setTestData()
    }

    private fun initializeViews() {
        etName = findViewById(R.id.et_name)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnSignup = findViewById(R.id.btn_signup)
        btnLoginRedirect = findViewById(R.id.btn_login_redirect)
        ivBackArrow = findViewById(R.id.iv_back_arrow)

        // Set up password visibility toggle
        val ivPasswordToggle = findViewById<ImageView>(R.id.iv_password_toggle)
        ivPasswordToggle.setOnClickListener {
            togglePasswordVisibility()
        }
    }

    private fun setTestData() {
        // For testing - remove in production
        etName.setText("Test User")
        etEmail.setText("test${System.currentTimeMillis()}@example.com")
        etPassword.setText("password123")
    }

    private fun setupClickListeners() {
        btnSignup.setOnClickListener {
            attemptSignup()
        }

        btnLoginRedirect.setOnClickListener {
            navigateToLogin()
        }

        ivBackArrow.setOnClickListener {
            onBackPressed()
        }
    }

    private fun togglePasswordVisibility() {
        val ivPasswordToggle = findViewById<ImageView>(R.id.iv_password_toggle)

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

    private fun attemptSignup() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Basic validation
        if (!validateInput(name, email, password)) {
            return
        }

        // Show loading state
        btnSignup.text = "Creating account..."
        btnSignup.isEnabled = false

        // Call signup API
        performSignup(name, email, password)
    }

    private fun validateInput(name: String, email: String, password: String): Boolean {
        if (name.isEmpty()) {
            etName.error = "Name is required"
            etName.requestFocus()
            return false
        }

        if (name.length < 2) {
            etName.error = "Name must be at least 2 characters"
            etName.requestFocus()
            return false
        }

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

        // Additional password strength check (optional)
        if (!password.matches(".*[A-Z].*".toRegex()) ||
            !password.matches(".*[a-z].*".toRegex()) ||
            !password.matches(".*\\d.*".toRegex())) {
            etPassword.error = "Password should contain uppercase, lowercase and numbers"
            etPassword.requestFocus()
            return false
        }

        return true
    }

    private fun performSignup(name: String, email: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create request body
                val jsonObject = JSONObject().apply {
                    put("name", name)
                    put("email", email)
                    put("password", password)
                }

                Log.d(TAG, "Sending signup request to: ${ApiConfig.REGISTER_URL}")
                Log.d(TAG, "Request body: ${jsonObject.toString()}")

                val response = makeApiRequest(ApiConfig.REGISTER_URL, jsonObject.toString())

                withContext(Dispatchers.Main) {
                    handleSignupResponse(response, name, email)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Signup error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showError("Network error: ${e.message}")
                    resetSignupButton()
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

    private fun handleSignupResponse(responseBody: String?, name: String, email: String) {
        try {
            if (responseBody == null) {
                showError("No response from server")
                resetSignupButton()
                return
            }

            val jsonObject = JSONObject(responseBody)
            val status = jsonObject.optString("status")
            val message = jsonObject.optString("message")

            if (status == "success") {
                // Signup successful
                val data = jsonObject.optJSONObject("data")

                if (data != null) {
                    val userId = data.optInt("user_id")
                    val userName = data.optString("name")
                    val userEmail = data.optString("email")

                    Log.d(TAG, "User registered: $userName ($userEmail)")

                    // Show success message with user info
                    Toast.makeText(
                        this,
                        "Account created successfully!\nYou can now login with your credentials.",
                        Toast.LENGTH_LONG
                    ).show()

                    // Navigate back to login with pre-filled email
                    navigateToLoginWithEmail(email)

                } else {
                    // Handle case where data is null
                    showSuccessAndNavigate(email)
                }

            } else {
                // Signup failed
                handleSignupError(message)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Response parsing error: ${e.message}", e)

            // Try to parse error anyway
            if (responseBody?.contains("error") == true) {
                showError("Server error: $responseBody")
            } else if (responseBody?.contains("already registered") == true) {
                showError("Email is already registered")
                etEmail.error = "Email already exists"
                etEmail.requestFocus()
            } else {
                showError("Invalid server response format")
            }
            resetSignupButton()
        }
    }

    private fun handleSignupError(message: String) {
        when {
            message.contains("already registered", ignoreCase = true) -> {
                etEmail.error = "Email is already registered"
                etEmail.requestFocus()
                showError("This email is already registered. Please use a different email or login.")
            }
            message.contains("password", ignoreCase = true) -> {
                etPassword.error = "Invalid password format"
                etPassword.requestFocus()
                showError("Please choose a stronger password (min 6 characters with letters and numbers)")
            }
            message.contains("email", ignoreCase = true) -> {
                etEmail.error = "Invalid email format"
                etEmail.requestFocus()
                showError("Please enter a valid email address")
            }
            else -> {
                showError("Signup failed: $message")
            }
        }
        resetSignupButton()
    }

    private fun showSuccessAndNavigate(email: String) {
        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
        navigateToLoginWithEmail(email)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e(TAG, "Error: $message")
    }

    private fun resetSignupButton() {
        btnSignup.text = "Sign Up"
        btnSignup.isEnabled = true
    }

    private fun navigateToLoginWithEmail(email: String) {
        val intent = Intent(this, LoginActivity::class.java).apply {
            putExtra("email", email)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish() // Close signup activity
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        navigateToLogin()
    }
}