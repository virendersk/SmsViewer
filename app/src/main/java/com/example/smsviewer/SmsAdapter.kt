package com.example.smsviewer

import android.provider.Telephony
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
    val isSent: Boolean = false,
    val deliveryStatus: Int = 0,
    val deliveredDate: Long = 0,
    val isRead: Boolean = false
) {
    val displayName: String
        get() = contactName ?: address

    val displayLabel: String
        get() = if (isSent) "To: $displayName" else "From: $displayName"

    val isDelivered: Boolean
        get() = deliveryStatus == Telephony.Sms.STATUS_COMPLETE

    companion object {
        const val STATUS_NONE = 0
        const val STATUS_COMPLETE = Telephony.Sms.STATUS_COMPLETE
        const val STATUS_PENDING = Telephony.Sms.STATUS_PENDING
        const val STATUS_FAILED = Telephony.Sms.STATUS_FAILED
    }
}

class SmsAdapter(private val onItemClick: (SmsMessage) -> Unit) : RecyclerView.Adapter<SmsAdapter.SmsViewHolder>() {
    private var messages = listOf<SmsMessage>()
    private var filteredMessages = listOf<SmsMessage>()

    class SmsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val senderTextView: TextView = view.findViewById(R.id.senderTextView)
        val messageTextView: TextView = view.findViewById(R.id.messageTextView)
        val dateTextView: TextView = view.findViewById(R.id.dateTextView)
        val cardView: View = view.findViewById(R.id.messageCard)
        val deliveryStatusView: ImageView = view.findViewById(R.id.deliveryStatusView)
        val readStatusView: ImageView = view.findViewById(R.id.readStatusView)
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
        
        // Set default visibility to GONE
        holder.deliveryStatusView.visibility = View.GONE
        holder.readStatusView.visibility = View.GONE
        
        // Style sent messages differently and show delivery status
        if (message.isSent) {
            holder.cardView.setBackgroundColor(ContextCompat.getColor(context, R.color.sent_message_bg))
            holder.senderTextView.setTextColor(ContextCompat.getColor(context, R.color.sent_message_text))
            
            // Show delivery status icon for sent messages
            when (message.deliveryStatus) {
                SmsMessage.STATUS_COMPLETE -> {
                    holder.deliveryStatusView.setImageResource(R.drawable.ic_delivered)
                    holder.deliveryStatusView.contentDescription = "Message delivered"
                    holder.deliveryStatusView.visibility = View.VISIBLE
                }
                SmsMessage.STATUS_PENDING -> {
                    holder.deliveryStatusView.setImageResource(R.drawable.ic_pending)
                    holder.deliveryStatusView.contentDescription = "Delivery pending"
                    holder.deliveryStatusView.visibility = View.VISIBLE
                }
                SmsMessage.STATUS_FAILED -> {
                    holder.deliveryStatusView.setImageResource(R.drawable.ic_failed)
                    holder.deliveryStatusView.contentDescription = "Delivery failed"
                    holder.deliveryStatusView.visibility = View.VISIBLE
                }
            }
        } else {
            holder.cardView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
            holder.senderTextView.setTextColor(ContextCompat.getColor(context, android.R.color.black))
            
            // Show read status for received messages
            if (!message.isRead) {
                holder.readStatusView.setImageResource(R.drawable.ic_unread)
                holder.readStatusView.contentDescription = "Unread message"
                holder.readStatusView.visibility = View.VISIBLE
            }
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