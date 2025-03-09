package com.example.smsviewer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar
import java.text.SimpleDateFormat
import java.util.*

class SmsDetailActivity : AppCompatActivity() {
    companion object {
        private const val EXTRA_SMS_ID = "extra_sms_id"
        private const val EXTRA_SMS_ADDRESS = "extra_sms_address"
        private const val EXTRA_SMS_BODY = "extra_sms_body"
        private const val EXTRA_SMS_DATE = "extra_sms_date"

        fun createIntent(context: Context, message: SmsMessage): Intent {
            return Intent(context, SmsDetailActivity::class.java).apply {
                putExtra(EXTRA_SMS_ID, message.id)
                putExtra(EXTRA_SMS_ADDRESS, message.address)
                putExtra(EXTRA_SMS_BODY, message.body)
                putExtra(EXTRA_SMS_DATE, message.date)
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

        val senderTextView: TextView = findViewById(R.id.detailSenderTextView)
        val messageTextView: TextView = findViewById(R.id.detailMessageTextView)
        val dateTextView: TextView = findViewById(R.id.detailDateTextView)

        val address = intent.getStringExtra(EXTRA_SMS_ADDRESS) ?: "Unknown"
        val body = intent.getStringExtra(EXTRA_SMS_BODY) ?: ""
        val date = intent.getLongExtra(EXTRA_SMS_DATE, 0L)

        senderTextView.text = address
        messageTextView.text = body
        dateTextView.text = formatDate(date)
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMMM dd, yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
} 