package com.example.maps_test_1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EmergencyContactsAdapter(
    private val contacts: MutableList<EmergencyContact>,
    private val editCallback: (EmergencyContact) -> Unit,
    private val deleteCallback: (EmergencyContact) -> Unit
) : RecyclerView.Adapter<EmergencyContactsAdapter.ContactViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.items_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.name.text = contact.name
        holder.phone.text = contact.phone
        holder.email.text = contact.email

        holder.editButton.setOnClickListener { editCallback(contact) }
        holder.deleteButton.setOnClickListener { deleteCallback(contact) }
    }

    override fun getItemCount() = contacts.size

    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tv_name)
        val phone: TextView = itemView.findViewById(R.id.tv_phone)
        val email: TextView = itemView.findViewById(R.id.tv_email)
        val editButton: ImageButton = itemView.findViewById(R.id.btn_edit)
        val deleteButton: ImageButton = itemView.findViewById(R.id.btn_delete)
    }
}
