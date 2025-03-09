package com.example.smsviewer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

data class SmsMessage(
    val id: Long,
    val address: String,
    val contactName: String?,
    val body: String,
    val date: Long
) {
    val displayName: String
        get() = contactName ?: address
}

class SmsAdapter(private val onItemClick: (SmsMessage) -> Unit) : RecyclerView.Adapter<SmsAdapter.SmsViewHolder>() {
    private var messages = listOf<SmsMessage>()
    private var filteredMessages = listOf<SmsMessage>()

    class SmsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val senderTextView: TextView = view.findViewById(R.id.senderTextView)
        val messageTextView: TextView = view.findViewById(R.id.messageTextView)
        val dateTextView: TextView = view.findViewById(R.id.dateTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.sms_item, parent, false)
        return SmsViewHolder(view)
    }

    override fun onBindViewHolder(holder: SmsViewHolder, position: Int) {
        val message = filteredMessages[position]
        holder.senderTextView.text = message.displayName
        holder.messageTextView.text = message.body
        holder.dateTextView.text = formatDate(message.date)
        
        holder.itemView.setOnClickListener {
            onItemClick(message)
        }
    }

    override fun getItemCount() = filteredMessages.size

    fun setMessages(newMessages: List<SmsMessage>) {
        messages = newMessages
        filteredMessages = newMessages
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        filteredMessages = if (query.isEmpty()) {
            messages
        } else {
            messages.filter {
                it.displayName.contains(query, ignoreCase = true) ||
                it.body.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
} 