package com.example.smsviewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import java.text.SimpleDateFormat
import java.util.*

class SmsDetailActivity : AppCompatActivity() {
    private lateinit var messageTextView: TextView
    private lateinit var senderTextView: TextView
    private lateinit var recipientLabelTextView: TextView
    private var messageBody: String = ""
    private var sender: String = ""
    private var contactName: String? = null
    private var isSent: Boolean = false

    companion object {
        private const val EXTRA_SMS_ID = "extra_sms_id"
        private const val EXTRA_SMS_ADDRESS = "extra_sms_address"
        private const val EXTRA_SMS_CONTACT_NAME = "extra_sms_contact_name"
        private const val EXTRA_SMS_BODY = "extra_sms_body"
        private const val EXTRA_SMS_DATE = "extra_sms_date"
        private const val EXTRA_SMS_IS_SENT = "extra_sms_is_sent"

        fun createIntent(context: Context, message: SmsMessage): Intent {
            return Intent(context, SmsDetailActivity::class.java).apply {
                putExtra(EXTRA_SMS_ID, message.id)
                putExtra(EXTRA_SMS_ADDRESS, message.address)
                putExtra(EXTRA_SMS_CONTACT_NAME, message.contactName)
                putExtra(EXTRA_SMS_BODY, message.body)
                putExtra(EXTRA_SMS_DATE, message.date)
                putExtra(EXTRA_SMS_IS_SENT, message.isSent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms_detail)

        val toolbar: MaterialToolbar = findViewById(R.id.detailToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        recipientLabelTextView = findViewById(R.id.recipientLabelTextView)
        senderTextView = findViewById(R.id.detailSenderTextView)
        messageTextView = findViewById(R.id.detailMessageTextView)
        val dateTextView: TextView = findViewById(R.id.detailDateTextView)

        sender = intent.getStringExtra(EXTRA_SMS_ADDRESS) ?: "Unknown"
        contactName = intent.getStringExtra(EXTRA_SMS_CONTACT_NAME)
        messageBody = intent.getStringExtra(EXTRA_SMS_BODY) ?: ""
        val date = intent.getLongExtra(EXTRA_SMS_DATE, 0L)
        isSent = intent.getBooleanExtra(EXTRA_SMS_IS_SENT, false)

        // Set the appropriate label and title based on message type
        recipientLabelTextView.text = if (isSent) "To" else "From"
        val displayName = contactName ?: sender
        senderTextView.text = displayName
        if (contactName != null) {
            supportActionBar?.title = contactName
        }
        messageTextView.text = messageBody
        dateTextView.text = formatDate(date)
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
            else -> super.onOptionsItemSelected(item)
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
        val sdf = SimpleDateFormat("MMMM dd, yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
} 