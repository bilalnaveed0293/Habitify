package com.example.habitify

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
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

class VerificationCodeActivity : AppCompatActivity() {

    private lateinit var etCode1: EditText
    private lateinit var etCode2: EditText
    private lateinit var etCode3: EditText
    private lateinit var etCode4: EditText
    private lateinit var etCode5: EditText
    private lateinit var etCode6: EditText
    private lateinit var btnVerifyCode: MaterialButton
    private lateinit var tvResendCode: TextView
    private lateinit var tvCountdown: TextView
    private lateinit var ivBackArrow: ImageView
    private lateinit var tvEmailDisplay: TextView

    private var userEmail: String = ""
    private var storedResetCode: String = ""
    private var isResendEnabled = false
    private var countDownTimer: CountDownTimer? = null
    private var timeLeftMillis: Long = 300000 // 5 minutes

    private val TAG = "VerificationCodeActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_verification_code)

        // Get data from intent
        userEmail = intent.getStringExtra("email") ?: ""
        storedResetCode = intent.getStringExtra("reset_code") ?: ""

        // Initialize views
        initializeViews()

        // Set click listeners
        setupClickListeners()

        // Set up window insets
        setupWindowInsets()

        // Start countdown timer
        startCountdownTimer()

        // Set email display
        tvEmailDisplay.text = "Enter code from notification"

        // Auto-focus first field
        etCode1.requestFocus()

        Log.d(TAG, "Verification for: $userEmail")
        Log.d(TAG, "Stored code for testing: $storedResetCode")
    }

    private fun initializeViews() {
        etCode1 = findViewById(R.id.et_code_1)
        etCode2 = findViewById(R.id.et_code_2)
        etCode3 = findViewById(R.id.et_code_3)
        etCode4 = findViewById(R.id.et_code_4)
        etCode5 = findViewById(R.id.et_code_5)
        etCode6 = findViewById(R.id.et_code_6)
        btnVerifyCode = findViewById(R.id.btn_verify_code)
        tvResendCode = findViewById(R.id.tv_resend_code)
        tvCountdown = findViewById(R.id.tv_countdown)
        ivBackArrow = findViewById(R.id.iv_back_arrow)
        tvEmailDisplay = findViewById(R.id.tv_email_display)

        // Set up code input listeners for auto-focus
        setupCodeInputListeners()
    }

    private fun setupCodeInputListeners() {
        val codeFields = listOf(etCode1, etCode2, etCode3, etCode4, etCode5, etCode6)

        codeFields.forEachIndexed { index, editText ->
            editText.setOnKeyListener { _, keyCode, event ->
                if (event.action == android.view.KeyEvent.ACTION_DOWN && keyCode == android.view.KeyEvent.KEYCODE_DEL) {
                    if (editText.text.isEmpty() && index > 0) {
                        codeFields[index - 1].requestFocus()
                        codeFields[index - 1].text.clear()
                    }
                }
                false
            }

            editText.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    if (s?.length == 1 && index < codeFields.size - 1) {
                        codeFields[index + 1].requestFocus()
                    }
                    checkIfAllFieldsFilled()
                }
            })
        }
    }

    private fun checkIfAllFieldsFilled() {
        val allFilled = listOf(etCode1, etCode2, etCode3, etCode4, etCode5, etCode6)
            .all { it.text.length == 1 }

        btnVerifyCode.isEnabled = allFilled
    }

    private fun setupClickListeners() {
        btnVerifyCode.setOnClickListener {
            verifyCode()
        }

        tvResendCode.setOnClickListener {
            if (isResendEnabled) {
                resendCode()
            }
        }

        ivBackArrow.setOnClickListener {
            navigateBack()
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun startCountdownTimer() {
        countDownTimer = object : CountDownTimer(timeLeftMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftMillis = millisUntilFinished
                val minutes = millisUntilFinished / 60000
                val seconds = (millisUntilFinished % 60000) / 1000

                tvCountdown.text = String.format("%02d:%02d", minutes, seconds)

                if (millisUntilFinished < 60000) { // Less than 1 minute
                    tvCountdown.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                }
            }

            override fun onFinish() {
                tvCountdown.text = "00:00"
                isResendEnabled = true
                tvResendCode.setTextColor(resources.getColor(R.color.login_button_purple))
                tvResendCode.isEnabled = true
            }
        }.start()
    }

    private fun verifyCode() {
        val enteredCode = buildString {
            append(etCode1.text)
            append(etCode2.text)
            append(etCode3.text)
            append(etCode4.text)
            append(etCode5.text)
            append(etCode6.text)
        }

        if (enteredCode.length != 6) {
            showToast("Please enter all 6 digits")
            return
        }

        // Show loading state
        btnVerifyCode.isEnabled = false

        // Verify code (local verification for now - in real app, verify with server)
        if (enteredCode == storedResetCode) {
            // Code verified successfully
            showToast("Code verified successfully!")

            // Navigate to reset password
            navigateToResetPassword()

        } else {
            showToast("Invalid code. Please try again.")
            resetCodeFields()
            btnVerifyCode.isEnabled = true
        }
    }

    private fun resetCodeFields() {
        listOf(etCode1, etCode2, etCode3, etCode4, etCode5, etCode6).forEach {
            it.text.clear()
        }
        etCode1.requestFocus()
    }

    private fun resendCode() {
        // Show loading state
        tvResendCode.text = "Resending..."
        tvResendCode.isEnabled = false

        // Simulate resending code (in real app, call API)
        CoroutineScope(Dispatchers.IO).launch {
            // Simulate network delay
            kotlinx.coroutines.delay(1000)

            withContext(Dispatchers.Main) {
                // Generate new code
                storedResetCode = (100000..999999).random().toString()

                showToast("New code sent via notification")

                // Reset countdown
                timeLeftMillis = 300000
                startCountdownTimer()
                isResendEnabled = false
                tvResendCode.setTextColor(resources.getColor(R.color.login_hint_text))
                tvResendCode.isEnabled = false

                // Clear and refocus
                resetCodeFields()

                // Send new notification
                sendResendNotification(storedResetCode)
            }
        }
    }

    private fun sendResendNotification(newCode: String) {
        // In a real app, you would send a new push notification
        // For now, just show a toast with the new code
        showToast("New code: $newCode")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun navigateToResetPassword() {
        val intent = Intent(this, ResetPasswordActivity::class.java).apply {
            putExtra("email", userEmail)
            putExtra("verified_code", storedResetCode)
        }
        startActivity(intent)
        finish()
    }

    private fun navigateBack() {
        val intent = Intent(this, ForgotPasswordActivity::class.java).apply {
            putExtra("email", userEmail)
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}