package com.example.smsviewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import java.text.SimpleDateFormat
import java.util.*

class SmsDetailActivity : AppCompatActivity() {
    private lateinit var messageTextView: TextView
    private lateinit var senderTextView: TextView
    private lateinit var recipientLabelTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var deliveryStatusTextView: TextView
    private var messageBody: String = ""
    private var sender: String = ""
    private var contactName: String? = null
    private var isSent: Boolean = false
    private var deliveryStatus: Int = 0
    private var deliveredDate: Long = 0
    private var messageId: Long = -1L

    companion object {
        private const val TAG = "SmsViewer_Detail" // Unique tag for SmsDetailActivity logs
        private const val EXTRA_SMS_ID = "extra_sms_id"
        private const val EXTRA_SMS_ADDRESS = "extra_sms_address"
        private const val EXTRA_SMS_CONTACT_NAME = "extra_sms_contact_name"
        private const val EXTRA_SMS_BODY = "extra_sms_body"
        private const val EXTRA_SMS_DATE = "extra_sms_date"
        private const val EXTRA_SMS_IS_SENT = "extra_sms_is_sent"
        private const val EXTRA_SMS_DELIVERY_STATUS = "extra_sms_delivery_status"
        private const val EXTRA_SMS_DELIVERED_DATE = "extra_sms_delivered_date"

        fun createIntent(context: Context, message: SmsMessage): Intent {
            return Intent(context, SmsDetailActivity::class.java).apply {
                putExtra(EXTRA_SMS_ID, message.id)
                putExtra(EXTRA_SMS_ADDRESS, message.address)
                putExtra(EXTRA_SMS_CONTACT_NAME, message.contactName)
                putExtra(EXTRA_SMS_BODY, message.body)
                putExtra(EXTRA_SMS_DATE, message.date)
                putExtra(EXTRA_SMS_IS_SENT, message.isSent)
                putExtra(EXTRA_SMS_DELIVERY_STATUS, message.deliveryStatus)
                putExtra(EXTRA_SMS_DELIVERED_DATE, message.deliveredDate)
            }
        }
    }

    private fun isDefaultSmsApp(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val roleManager = getSystemService(android.app.role.RoleManager::class.java)
            roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_SMS)
        } else {
            Telephony.Sms.getDefaultSmsPackage(this) == packageName
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms_detail)

        android.util.Log.i(TAG, "📱 Opening SMS detail view")

        val toolbar: MaterialToolbar = findViewById(R.id.detailToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        recipientLabelTextView = findViewById(R.id.recipientLabelTextView)
        senderTextView = findViewById(R.id.detailSenderTextView)
        messageTextView = findViewById(R.id.detailMessageTextView)
        dateTextView = findViewById(R.id.detailDateTextView)
        deliveryStatusTextView = findViewById(R.id.deliveryStatusTextView)

        sender = intent.getStringExtra(EXTRA_SMS_ADDRESS) ?: "Unknown"
        contactName = intent.getStringExtra(EXTRA_SMS_CONTACT_NAME)
        messageBody = intent.getStringExtra(EXTRA_SMS_BODY) ?: ""
        val date = intent.getLongExtra(EXTRA_SMS_DATE, 0L)
        isSent = intent.getBooleanExtra(EXTRA_SMS_IS_SENT, false)
        deliveryStatus = intent.getIntExtra(EXTRA_SMS_DELIVERY_STATUS, 0)
        deliveredDate = intent.getLongExtra(EXTRA_SMS_DELIVERED_DATE, 0L)
        messageId = intent.getLongExtra(EXTRA_SMS_ID, -1L)

        android.util.Log.i(TAG, "📨 Detail view: messageId=$messageId, isSent=$isSent, sender=$sender")

        // Mark message as read if it's not sent and not already read
        if (!isSent && messageId != -1L) {
            if (!isDefaultSmsApp()) {
                android.util.Log.e(TAG, "❌ Cannot mark message as read in detail view: not default SMS app")
                Toast.makeText(this, "Cannot mark message as read: app is not default SMS app", Toast.LENGTH_LONG).show()
            } else {
                android.util.Log.i(TAG, "📝 Attempting to mark message $messageId as read in detail view (isSent: $isSent)")
                val values = ContentValues().apply {
                    put(Telephony.Sms.READ, 1)
                }
                
                // Use the appropriate URI based on whether the message is sent or received
                val uri = if (isSent) Telephony.Sms.Sent.CONTENT_URI else Telephony.Sms.Inbox.CONTENT_URI
                
                val result = contentResolver.update(
                    uri,
                    values,
                    "${Telephony.Sms._ID} = ?",
                    arrayOf(messageId.toString())
                )
                
                android.util.Log.i(TAG, "📊 Update result in detail view for ${if (isSent) "Sent" else "Inbox"}: $result")
                
                // Notify MainActivity to refresh the list
                androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(Intent(SmsReceiver.ACTION_SMS_RECEIVED))
            }
        }

        // Set the appropriate label and title based on message type
        recipientLabelTextView.text = if (isSent) "To" else "From"
        val displayName = contactName ?: sender
        senderTextView.text = displayName
        if (contactName != null) {
            supportActionBar?.title = contactName
        }
        messageTextView.text = messageBody
        dateTextView.text = formatDate(date)

        // Show delivery status for sent messages
        if (isSent) {
            deliveryStatusTextView.visibility = View.VISIBLE
            val statusText = when (deliveryStatus) {
                SmsMessage.STATUS_COMPLETE -> {
                    val deliveredTime = if (deliveredDate > 0) {
                        "\nDelivered: ${formatDate(deliveredDate)}"
                    } else ""
                    "✓ Delivered$deliveredTime"
                }
                SmsMessage.STATUS_PENDING -> "⏳ Pending"
                SmsMessage.STATUS_FAILED -> "❌ Failed to deliver"
                else -> null
            }
            if (statusText != null) {
                deliveryStatusTextView.text = statusText
            } else {
                deliveryStatusTextView.visibility = View.GONE
            }
        } else {
            deliveryStatusTextView.visibility = View.GONE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_copy -> {
                copyMessageToClipboard()
                true
            }
            R.id.action_reply -> {
                startActivity(ComposeActivity.createIntent(this, sender, contactName))
                true
            }
            R.id.action_delete -> {
                showDeleteConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDeleteConfirmationDialog() {
        if (!isDefaultSmsApp()) {
            Toast.makeText(this, "Cannot delete message: app is not default SMS app", Toast.LENGTH_LONG).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete Message")
            .setMessage("Are you sure you want to delete this message?")
            .setPositiveButton("Delete") { _, _ ->
                deleteMessage()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteMessage() {
        if (messageId == -1L) {
            Toast.makeText(this, "Cannot delete message: invalid message ID", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Use the appropriate URI based on whether the message is sent or received
            val uri = if (isSent) Telephony.Sms.Sent.CONTENT_URI else Telephony.Sms.Inbox.CONTENT_URI
            
            val result = contentResolver.delete(
                uri,
                "${Telephony.Sms._ID} = ?",
                arrayOf(messageId.toString())
            )

            if (result > 0) {
                Toast.makeText(this, "Message deleted", Toast.LENGTH_SHORT).show()
                // Notify MainActivity to refresh the list
                androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(Intent(SmsReceiver.ACTION_SMS_RECEIVED))
                finish()
            } else {
                Toast.makeText(this, "Failed to delete message", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error deleting message", e)
            Toast.makeText(this, "Error deleting message: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyMessageToClipboard() {
        val displayName = contactName ?: sender
        val prefix = if (isSent) "To" else "From"
        val fullMessage = "$prefix: $displayName\nPhone: $sender\nMessage: $messageBody"
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("SMS Message", fullMessage)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Message copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.detail_menu, menu)
        return true
    }
} 