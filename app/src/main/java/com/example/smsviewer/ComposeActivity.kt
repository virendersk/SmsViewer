package com.example.smsviewer

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsManager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText

class ComposeActivity : AppCompatActivity() {
    private lateinit var recipientEditText: MaterialAutoCompleteTextView
    private lateinit var messageEditText: TextInputEditText
    private lateinit var sendButton: MaterialButton
    private lateinit var contactHelper: ContactHelper
    private var selectedContact: Contact? = null

    companion object {
        private const val EXTRA_RECIPIENT = "extra_recipient"
        private const val EXTRA_RECIPIENT_NAME = "extra_recipient_name"
        private const val DELIVERED_ACTION = "android.provider.Telephony.SMS_DELIVERED"

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
        contactHelper = ContactHelper(this)

        setupContactSuggestions()

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

    private fun setupContactSuggestions() {
        val adapter = ContactSuggestionsAdapter(this)
        recipientEditText.setAdapter(adapter)
        
        recipientEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                selectedContact = null
            }
        })

        recipientEditText.setOnItemClickListener { _, _, position, _ ->
            val contact = adapter.getItem(position) as Contact
            selectedContact = contact
            recipientEditText.setText(contact.displayString)
            recipientEditText.setSelection(recipientEditText.text?.length ?: 0)
        }
    }

    private fun sendMessage() {
        val recipient = selectedContact?.number ?: recipientEditText.text?.toString()
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
            // Extract just the number if it's a display string format
            val phoneNumber = if (recipient.contains("(") && recipient.contains(")")) {
                recipient.substringAfter("(").substringBefore(")")
            } else {
                recipient
            }

            // Store the message first to get its ID
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, phoneNumber.trim())
                put(Telephony.Sms.BODY, message)
                put(Telephony.Sms.DATE, System.currentTimeMillis())
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_PENDING)
            }
            
            val messageUri = contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
            val messageId = messageUri?.lastPathSegment?.toLongOrNull() ?: -1

            // Create pending intents for delivery reports
            val deliveryIntent = Intent(DELIVERED_ACTION).apply {
                putExtra("message_id", messageId)
            }
            val deliveryPI = PendingIntent.getBroadcast(
                this, messageId.toInt(),
                deliveryIntent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )

            // Send the message with delivery report request
            val smsManager = SmsManager.getDefault()
            val messages = smsManager.divideMessage(message)
            
            if (messages.size > 1) {
                val deliveryIntents = ArrayList<PendingIntent>()
                for (i in messages.indices) {
                    deliveryIntents.add(deliveryPI)
                }
                smsManager.sendMultipartTextMessage(
                    phoneNumber.trim(), null, messages,
                    null, deliveryIntents
                )
            } else {
                smsManager.sendTextMessage(
                    phoneNumber.trim(), null, message,
                    null, deliveryPI
                )
            }

            Toast.makeText(this, "Message sent", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send message: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private class ContactSuggestionsAdapter(context: Context) : ArrayAdapter<Contact>(context, android.R.layout.simple_dropdown_item_1line) {
        private val contactHelper = ContactHelper(context)
        private val suggestions = mutableListOf<Contact>()

        override fun getCount(): Int = suggestions.size
        override fun getItem(position: Int): Contact = suggestions[position]

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(android.R.layout.simple_dropdown_item_1line, parent, false)
            
            val contact = getItem(position)
            (view as TextView).text = contact.displayString
            
            return view
        }

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val filterResults = FilterResults()
                    if (constraint != null) {
                        suggestions.clear()
                        suggestions.addAll(contactHelper.searchContacts(constraint.toString()))
                        filterResults.values = suggestions
                        filterResults.count = suggestions.size
                    }
                    return filterResults
                }

                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    if (results != null && results.count > 0) {
                        notifyDataSetChanged()
                    } else {
                        notifyDataSetInvalidated()
                    }
                }
            }
        }
    }
} 