package com.example.maps_test_1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var rememberMeCheckBox: CheckBox
    private lateinit var loginButton: Button
    private lateinit var registerTextView: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_page)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Find views
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        rememberMeCheckBox = findViewById(R.id.rememberMeCheckBox)
        loginButton = findViewById(R.id.loginButton)
        registerTextView = findViewById(R.id.registerTextView)

        // Load saved login credentials if "Remember Me" was checked
        loadRememberedCredentials()

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            } else {
                loginUser(email, password)
            }
        }

        registerTextView.setOnClickListener {
            val intent = Intent(this, RegisterPageActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()

                    // Save credentials if "Remember Me" is checked
                    if (rememberMeCheckBox.isChecked) {
                        saveCredentials(email, password)
                    } else {
                        clearSavedCredentials()
                    }

                    // Navigate to main/home activity
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveCredentials(email: String, password: String) {
        val sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("email", email)
        editor.putString("password", password)
        editor.putBoolean("rememberMe", true)
        editor.apply()
    }

    private fun loadRememberedCredentials() {
        val sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        val rememberedEmail = sharedPreferences.getString("email", "")
        val rememberedPassword = sharedPreferences.getString("password", "")
        val rememberMeChecked = sharedPreferences.getBoolean("rememberMe", false)

        if (rememberMeChecked) {
            emailEditText.setText(rememberedEmail)
            passwordEditText.setText(rememberedPassword)
            rememberMeCheckBox.isChecked = true
        }
    }

    private fun clearSavedCredentials() {
        val sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
    }
}