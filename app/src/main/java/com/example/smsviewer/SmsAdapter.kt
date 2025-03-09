package com.example.smsviewer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

data class SmsMessage(
    val id: Long,
    val address: String,
    val contactName: String?,
    val body: String,
    val date: Long,
    val isSent: Boolean = false
) {
    val displayName: String
        get() = contactName ?: address

    val displayLabel: String
        get() = if (isSent) "To: $displayName" else "From: $displayName"
}

class SmsAdapter(private val onItemClick: (SmsMessage) -> Unit) : RecyclerView.Adapter<SmsAdapter.SmsViewHolder>() {
    private var messages = listOf<SmsMessage>()
    private var filteredMessages = listOf<SmsMessage>()

    class SmsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val senderTextView: TextView = view.findViewById(R.id.senderTextView)
        val messageTextView: TextView = view.findViewById(R.id.messageTextView)
        val dateTextView: TextView = view.findViewById(R.id.dateTextView)
        val cardView: View = view.findViewById(R.id.messageCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.sms_item, parent, false)
        return SmsViewHolder(view)
    }

    override fun onBindViewHolder(holder: SmsViewHolder, position: Int) {
        val message = filteredMessages[position]
        val context = holder.itemView.context

        holder.senderTextView.text = message.displayLabel
        holder.messageTextView.text = message.body
        holder.dateTextView.text = formatDate(message.date)
        
        // Style sent messages differently
        if (message.isSent) {
            holder.cardView.setBackgroundColor(ContextCompat.getColor(context, R.color.sent_message_bg))
            holder.senderTextView.setTextColor(ContextCompat.getColor(context, R.color.sent_message_text))
        } else {
            holder.cardView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
            holder.senderTextView.setTextColor(ContextCompat.getColor(context, android.R.color.black))
        }
        
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