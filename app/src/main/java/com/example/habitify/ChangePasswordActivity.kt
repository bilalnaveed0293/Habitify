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

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var ivBackArrow: ImageView
    private lateinit var etCurrentPassword: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnUpdatePassword: MaterialButton
    private lateinit var btnSavePassword: MaterialButton
    private lateinit var ivCurrentPasswordToggle: ImageView
    private lateinit var ivNewPasswordToggle: ImageView
    private lateinit var ivConfirmPasswordToggle: ImageView

    private lateinit var sessionManager: SessionManager

    private val TAG = "ChangePasswordActivity"
    private val CHANGE_PASSWORD_URL = ApiConfig.CHANGE_PASSWORD_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_password)

        // Initialize SessionManager
        sessionManager = SessionManager(this)

        // Initialize views
        initializeViews()

        // Set up click listeners
        setupClickListeners()

        // Set up window insets
        setupWindowInsets()
    }

    private fun initializeViews() {
        ivBackArrow = findViewById(R.id.iv_back_arrow)
        etCurrentPassword = findViewById(R.id.et_current_password)
        etNewPassword = findViewById(R.id.et_new_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        btnUpdatePassword = findViewById(R.id.btn_update_password)
        btnSavePassword = findViewById(R.id.btn_save_password)

        // Password visibility toggles
        ivCurrentPasswordToggle = findViewById(R.id.iv_current_password_toggle)
        ivNewPasswordToggle = findViewById(R.id.iv_new_password_toggle)
        ivConfirmPasswordToggle = findViewById(R.id.iv_confirm_password_toggle)
    }

    private fun setupClickListeners() {
        // Back arrow
        ivBackArrow.setOnClickListener {
            onBackPressed()
        }

        // Password visibility toggles
        setupPasswordVisibilityToggle(etCurrentPassword, ivCurrentPasswordToggle)
        setupPasswordVisibilityToggle(etNewPassword, ivNewPasswordToggle)
        setupPasswordVisibilityToggle(etConfirmPassword, ivConfirmPasswordToggle)

        // Update Password button
        btnUpdatePassword.setOnClickListener {
            changePassword()
        }

        // Save Password button
        btnSavePassword.setOnClickListener {
            navigateToMainActivity()
        }
    }

    private fun setupPasswordVisibilityToggle(passwordEditText: EditText, toggleImageView: ImageView) {
        var isPasswordVisible = false

        toggleImageView.setOnClickListener {
            if (isPasswordVisible) {
                // Hide password
                passwordEditText.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
                toggleImageView.setImageResource(R.drawable.ic_visibility_off)
            } else {
                // Show password
                passwordEditText.transformationMethod = android.text.method.HideReturnsTransformationMethod.getInstance()
                toggleImageView.setImageResource(R.drawable.ic_visibility_off)
            }
            isPasswordVisible = !isPasswordVisible
            passwordEditText.setSelection(passwordEditText.text.length)
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun changePassword() {
        val currentPassword = etCurrentPassword.text.toString().trim()
        val newPassword = etNewPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        if (!validatePasswords(currentPassword, newPassword, confirmPassword)) {
            return
        }

        // Show loading
        btnUpdatePassword.isEnabled = false
        btnUpdatePassword.text = "Updating..."

        // Change password on server
        changePasswordOnServer(currentPassword, newPassword, confirmPassword)
    }

    private fun validatePasswords(currentPassword: String, newPassword: String, confirmPassword: String): Boolean {
        if (currentPassword.isEmpty()) {
            etCurrentPassword.error = "Current password is required"
            etCurrentPassword.requestFocus()
            return false
        }

        if (newPassword.isEmpty()) {
            etNewPassword.error = "New password is required"
            etNewPassword.requestFocus()
            return false
        }

        if (newPassword.length < 6) {
            etNewPassword.error = "Password must be at least 6 characters"
            etNewPassword.requestFocus()
            return false
        }

        // Password strength validation (optional)
        if (!isValidPassword(newPassword)) {
            etNewPassword.error = "Password should include uppercase, lowercase and numbers"
            etNewPassword.requestFocus()
            return false
        }

        if (confirmPassword.isEmpty()) {
            etConfirmPassword.error = "Please confirm your password"
            etConfirmPassword.requestFocus()
            return false
        }

        if (newPassword != confirmPassword) {
            etConfirmPassword.error = "Passwords do not match"
            etConfirmPassword.requestFocus()
            return false
        }

        if (currentPassword == newPassword) {
            etNewPassword.error = "New password must be different from current password"
            etNewPassword.requestFocus()
            return false
        }

        return true
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 6 &&
                password.matches(".*[A-Z].*".toRegex()) &&
                password.matches(".*[a-z].*".toRegex()) &&
                password.matches(".*\\d.*".toRegex())
    }

    private fun changePasswordOnServer(currentPassword: String, newPassword: String, confirmPassword: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get current user ID from session
                val userId = sessionManager.getUserId()

                // Create request body following your API pattern
                val jsonObject = JSONObject().apply {
                    put("user_id", userId)
                    put("current_password", currentPassword)
                    put("new_password", newPassword)
                    put("confirm_password", confirmPassword)
                }

                Log.d(TAG, "Changing password for user ID: $userId")
                Log.d(TAG, "API URL: $CHANGE_PASSWORD_URL")
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
                    .url(CHANGE_PASSWORD_URL)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
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
                            val message = jsonResponse.getString("message")

                            if (success) {
                                showToast("Password changed successfully!")

                                // Clear fields
                                etCurrentPassword.text.clear()
                                etNewPassword.text.clear()
                                etConfirmPassword.text.clear()

                                // Optional: Show success dialog
                                showSuccessDialog()

                            } else {
                                showToast("Error: $message")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "JSON parsing error: ${e.message}")
                            showToast("Server response error")
                        }
                    } else {
                        showToast("Failed to connect to server. Please try again.")
                    }

                    // Reset button state
                    btnUpdatePassword.isEnabled = true
                    btnUpdatePassword.text = "Update Password"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Password change error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast("Network error: ${e.message}")
                    btnUpdatePassword.isEnabled = true
                    btnUpdatePassword.text = "Update Password"
                }
            }
        }
    }

    private fun showSuccessDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Success")
            .setMessage("Your password has been changed successfully!")
            .setPositiveButton("OK") { _, _ ->
                // Optional: Navigate back
                // navigateToMainActivity()
            }
            .setCancelable(false)
            .show()
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}