package com.example.smsviewer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ComposeActivity : AppCompatActivity() {
    private lateinit var recipientEditText: TextInputEditText
    private lateinit var messageEditText: TextInputEditText
    private lateinit var sendButton: MaterialButton

    companion object {
        private const val EXTRA_RECIPIENT = "extra_recipient"
        private const val EXTRA_RECIPIENT_NAME = "extra_recipient_name"

        fun createIntent(context: Context, recipient: String? = null, recipientName: String? = null): Intent {
            return Intent(context, ComposeActivity::class.java).apply {
                putExtra(EXTRA_RECIPIENT, recipient)
                putExtra(EXTRA_RECIPIENT_NAME, recipientName)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compose)

        val toolbar: MaterialToolbar = findViewById(R.id.composeToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        recipientEditText = findViewById(R.id.recipientEditText)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)

        // Pre-fill recipient if provided
        intent.getStringExtra(EXTRA_RECIPIENT)?.let { recipient ->
            recipientEditText.setText(recipient)
            val recipientName = intent.getStringExtra(EXTRA_RECIPIENT_NAME)
            if (recipientName != null) {
                recipientEditText.hint = recipientName
            }
        }

        sendButton.setOnClickListener {
            sendMessage()
        }
    }

    private fun sendMessage() {
        val recipient = recipientEditText.text?.toString()
        val message = messageEditText.text?.toString()

        if (recipient.isNullOrBlank()) {
            recipientEditText.error = "Please enter a recipient"
            return
        }

        if (message.isNullOrBlank()) {
            messageEditText.error = "Please enter a message"
            return
        }

        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(recipient, null, message, null, null)
            Toast.makeText(this, "Message sent", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send message: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
} 