package com.example.habitify

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ManageAccountActivity : AppCompatActivity() {

    private lateinit var ivBackArrow: ImageView
    private lateinit var cardEditInfo: CardView
    private lateinit var cardChangePassword: CardView
    private lateinit var btnContinue: android.widget.Button

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_manage_account)

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
        cardEditInfo = findViewById(R.id.card_edit_info)
        cardChangePassword = findViewById(R.id.card_change_password)
        btnContinue = findViewById(R.id.btn_continue)
    }

    private fun setupClickListeners() {
        // Back arrow
        ivBackArrow.setOnClickListener {
            onBackPressed()
        }

        // Edit Account Information card
        cardEditInfo.setOnClickListener {
            navigateToEditAccountInformation()
        }

        // Change Password card
        cardChangePassword.setOnClickListener {
            navigateToChangePassword()
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

    private fun navigateToEditAccountInformation() {
        val intent = Intent(this, EditAccountInformationActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToChangePassword() {
        val intent = Intent(this, ChangePasswordActivity::class.java)
        startActivity(intent)
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