package com.example.maps_test_1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterPageActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_page)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Access the EditText views correctly using findViewById
        val nameTextInputLayout = findViewById<TextInputLayout>(R.id.name_input_design)
        val nameEditText = nameTextInputLayout.editText

        val phoneTextInputLayout = findViewById<TextInputLayout>(R.id.phoneNumber_input_design)
        val phoneEditText = phoneTextInputLayout.editText

        val emailTextInputLayout = findViewById<TextInputLayout>(R.id.email_input_design)
        val emailEditText = emailTextInputLayout.editText

        val passwordTextInputLayout = findViewById<TextInputLayout>(R.id.password_input_design)
        val passwordEditText = passwordTextInputLayout.editText
        val registerButton = findViewById<Button>(R.id.registerButton)

        registerButton.setOnClickListener {
            val name = nameEditText?.text.toString().trim()
            val phone = phoneEditText?.text.toString().trim()
            val email = emailEditText?.text.toString().trim()
            val password = passwordEditText?.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerUser(name, phone, email, password)
        }
    }

    private fun registerUser(name: String, phone: String, email: String, password: String) {
        // Register user with Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener

                    // Save user details to Firestore
                    val user = hashMapOf(
                        "name" to name,
                        "phone" to phone,
                        "email" to email
                    )

                    // Store user details under the user's UID in Firestore
                    db.collection("users").document(userId)
                        .set(user)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                            // Navigate to the login page after successful registration
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error saving user data", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}