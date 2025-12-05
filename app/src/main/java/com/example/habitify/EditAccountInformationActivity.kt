package com.example.habitify

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class EditAccountInformationActivity : AppCompatActivity() {

    private lateinit var ivBackArrow: ImageView
    private lateinit var ivProfilePicture: ImageView
    private lateinit var btnChangeProfilePicture: MaterialButton
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var btnSaveChanges: MaterialButton
    private lateinit var btnContinue: MaterialButton

    private lateinit var sessionManager: SessionManager
    private var selectedImageUri: Uri? = null
    private var currentName: String = ""
    private var currentEmail: String = ""
    private var currentPhone: String = ""

    private val TAG = "EditAccountInfoActivity"
    private val UPDATE_PROFILE_URL = ApiConfig.UPDATE_PROFILE_URL
    private val UPLOAD_PROFILE_PICTURE_URL = ApiConfig.UPLOAD_PROFILE_PICTURE_URL

    // Activity result launchers
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                // Show selected image
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .into(ivProfilePicture)

                // Upload the image
                uploadProfilePicture(uri)
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri?.let { uri ->
                // Show captured image
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .into(ivProfilePicture)

                // Upload the image
                uploadProfilePicture(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_account_information)

        // Initialize SessionManager
        sessionManager = SessionManager(this)

        // Initialize views
        initializeViews()

        // Load current user data
        loadUserData()

        // Set up click listeners
        setupClickListeners()

        // Set up window insets
        setupWindowInsets()
    }

    private fun initializeViews() {
        ivBackArrow = findViewById(R.id.iv_back_arrow)
        ivProfilePicture = findViewById(R.id.iv_profile_picture)
        btnChangeProfilePicture = findViewById(R.id.btn_change_profile_picture)
        etName = findViewById(R.id.et_name)
        etEmail = findViewById(R.id.et_email)
        etPhone = findViewById(R.id.et_phone)
        btnSaveChanges = findViewById(R.id.btn_save_changes)
        btnContinue = findViewById(R.id.btn_continue)
    }

    private fun loadUserData() {
        // Get user data from session
        currentName = sessionManager.getUserName()
        currentEmail = sessionManager.getUserEmail()
        currentPhone = sessionManager.getUserPhone()

        // Set data to fields
        etName.setText(currentName)
        etEmail.setText(currentEmail)
        etPhone.setText(currentPhone)

        // Load profile picture
        ProfilePictureUtils.loadProfilePictureFromSession(this, ivProfilePicture, sessionManager)
    }

    private fun setupClickListeners() {
        // Back arrow
        ivBackArrow.setOnClickListener {
            onBackPressed()
        }

        // Change profile picture button
        btnChangeProfilePicture.setOnClickListener {
            showImageSourceDialog()
        }

        // Save Changes button
        btnSaveChanges.setOnClickListener {
            saveAccountChanges()
        }

        // Continue button
        btnContinue.setOnClickListener {
            navigateToMainActivity()
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Change Profile Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                    2 -> { /* Cancel */ }
                }
            }
            .show()
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            // Request camera permission
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(intent)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun saveAccountChanges() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()

        // Check if anything changed
        val hasChanges = name != currentName || email != currentEmail || phone != currentPhone

        if (!hasChanges) {
            showToast("No changes to save")
            return
        }

        // Validation
        if (!validateInput(name, email, phone)) {
            return
        }

        // Show loading
        btnSaveChanges.isEnabled = false
        btnSaveChanges.text = "Saving..."

        // Save changes to server
        updateAccountOnServer(name, email, phone)
    }

    private fun validateInput(name: String, email: String, phone: String): Boolean {
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

        // Optional phone validation
        if (phone.isNotEmpty() && !android.util.Patterns.PHONE.matcher(phone).matches()) {
            etPhone.error = "Enter a valid phone number"
            etPhone.requestFocus()
            return false
        }

        return true
    }

    private fun updateAccountOnServer(name: String, email: String, phone: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get current user ID
                val userId = sessionManager.getUserId()

                // Create request body
                val jsonObject = JSONObject().apply {
                    put("user_id", userId)
                    put("name", name)
                    put("email", email)
                    put("phone", phone)
                }

                Log.d(TAG, "Updating profile for user: $userId")
                Log.d(TAG, "API URL: $UPDATE_PROFILE_URL")
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
                    .url(UPDATE_PROFILE_URL)
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
                                // Update local session
                                sessionManager.updateUserName(name)
                                if (phone.isNotEmpty()) {
                                    // Add this method to SessionManager if not exists
                                    // Or use reflection to call it
                                    try {
                                        val updatePhoneMethod = sessionManager::class.java.getMethod("updateUserPhone", String::class.java)
                                        updatePhoneMethod.invoke(sessionManager, phone)
                                    } catch (e: Exception) {
                                        // Method doesn't exist, you need to add it
                                        Log.e(TAG, "updateUserPhone method not found")
                                    }
                                }

                                showToast("Profile updated successfully!")
                                currentName = name
                                currentEmail = email
                                currentPhone = phone

                            } else {
                                showToast("Error: $message")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "JSON parsing error: ${e.message}")
                            showToast("Server response error")
                        }
                    } else {
                        showToast("Failed to connect to server")
                    }

                    // Reset button state
                    btnSaveChanges.isEnabled = true
                    btnSaveChanges.text = "Save Changes"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Update error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast("Network error: ${e.message}")
                    btnSaveChanges.isEnabled = true
                    btnSaveChanges.text = "Save Changes"
                }
            }
        }
    }

    private fun uploadProfilePicture(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = sessionManager.getUserId()

                // Create temporary file from URI
                val inputStream = contentResolver.openInputStream(uri)
                val tempFile = File(cacheDir, "temp_profile_pic.jpg")
                val outputStream = FileOutputStream(tempFile)

                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()

                // Create request body
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("user_id", userId.toString())
                    .addFormDataPart(
                        "profile_picture",
                        "profile_picture.jpg",
                        tempFile.asRequestBody("image/jpeg".toMediaType())
                    )
                    .build()

                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(UPLOAD_PROFILE_PICTURE_URL)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                Log.d(TAG, "Upload Response Code: ${response.code}")
                Log.d(TAG, "Upload Response Body: $responseBody")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val success = jsonResponse.getBoolean("success")
                            val message = jsonResponse.getString("message")

                            if (success) {
                                val data = jsonResponse.getJSONObject("data")
                                val profilePicturePath = data.getString("profile_picture")
                                val profilePictureUrl = data.getString("profile_picture_url")

                                // Save to session
                                sessionManager.saveProfilePicture(profilePicturePath, profilePictureUrl)

                                withContext(Dispatchers.Main) {
                                    // Update the image view
                                    ProfilePictureUtils.loadProfilePicture(
                                        this@EditAccountInformationActivity,
                                        ivProfilePicture,
                                        profilePictureUrl,
                                        useCircleCrop = true
                                    )

                                    showToast("Profile picture updated successfully!")
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    showToast("Error: $message")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "JSON parsing error: ${e.message}")
                            withContext(Dispatchers.Main) {
                                showToast("Upload failed")
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            showToast("Failed to upload image")
                        }
                    }

                    // Clean up temp file
                    tempFile.delete()

                }
            } catch (e: Exception) {
                    Log.e(TAG, "Upload error: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        showToast("Upload failed: ${e.message}")
                    }
                }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    showToast("Camera permission denied")
                }
            }
        }
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

    companion object {
        private const val CAMERA_PERMISSION_CODE = 1001
    }
}