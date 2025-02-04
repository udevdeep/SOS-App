package com.example.maps_test_1

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EmergencyContactsActivity : AppCompatActivity() {

    private lateinit var contactsAdapter: EmergencyContactsAdapter
    private val contactsList = mutableListOf<EmergencyContact>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_contacts)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        val addContactButton: ImageButton = findViewById(R.id.btn_add_contact)
        val backButton: ImageButton = findViewById(R.id.btn_back)

        contactsAdapter = EmergencyContactsAdapter(contactsList, ::showContactDialog, ::deleteContact)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = contactsAdapter

        loadContacts()

        addContactButton.setOnClickListener {
            if (contactsList.size < 7) {
                showContactDialog(null)
            } else {
                showMaxContactsReachedMessage()
            }
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun showContactDialog(contact: EmergencyContact?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_contact, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_name)
        val etPhone = dialogView.findViewById<EditText>(R.id.et_phone)
        val etEmail = dialogView.findViewById<EditText>(R.id.et_email)

        if (contact != null) {
            etName.setText(contact.name)
            etPhone.setText(contact.phone)
            etEmail.setText(contact.email)
        }

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle(if (contact == null) "Add Emergency Contact" else "Edit Contact")
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                val email = etEmail.text.toString().trim()

                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    if (contact == null) {
                        saveContactToFirebase(name, phone, email)
                    } else {
                        updateContact(contact.id, name, phone, email)
                    }
                } else {
                    Toast.makeText(this, "Name and Phone are required!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        alertDialog.show()
    }

    private fun saveContactToFirebase(name: String, phone: String, email: String) {
        val userId = auth.currentUser?.uid ?: return

        val contactId = db.collection("users").document(userId)
            .collection("emergency_contacts").document().id

        val contact = EmergencyContact(contactId, name, phone, email)

        db.collection("users").document(userId)
            .collection("emergency_contacts").document(contactId)
            .set(contact)
            .addOnSuccessListener {
                contactsList.add(contact)
                contactsAdapter.notifyDataSetChanged()
                Toast.makeText(this, "Contact saved!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateContact(contactId: String, name: String, phone: String, email: String) {
        val userId = auth.currentUser?.uid ?: return

        val updatedData = mapOf(
            "name" to name,
            "phone" to phone,
            "email" to email
        )

        db.collection("users").document(userId)
            .collection("emergency_contacts").document(contactId)
            .update(updatedData)
            .addOnSuccessListener {
                val index = contactsList.indexOfFirst { it.id == contactId }
                if (index != -1) {
                    contactsList[index] = EmergencyContact(contactId, name, phone, email)
                    contactsAdapter.notifyItemChanged(index)
                }
                Toast.makeText(this, "Contact updated!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteContact(contact: EmergencyContact) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("emergency_contacts").document(contact.id)
            .delete()
            .addOnSuccessListener {
                contactsList.remove(contact)
                contactsAdapter.notifyDataSetChanged()
                Toast.makeText(this, "Contact deleted!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadContacts() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("emergency_contacts")
            .get()
            .addOnSuccessListener { result ->
                contactsList.clear()
                for (document in result) {
                    val contact = document.toObject(EmergencyContact::class.java)
                    contactsList.add(contact)
                }
                contactsAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading contacts: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showMaxContactsReachedMessage() {
        AlertDialog.Builder(this)
            .setMessage("You can only add up to 7 emergency contacts.")
            .setPositiveButton("OK", null)
            .show()
    }
}