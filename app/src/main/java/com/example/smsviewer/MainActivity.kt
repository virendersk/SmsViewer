package com.example.smsviewer

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {
    private lateinit var smsRecyclerView: RecyclerView
    private lateinit var searchEditText: TextInputEditText
    private val smsAdapter = SmsAdapter { message ->
        startActivity(SmsDetailActivity.createIntent(this, message))
    }

    private val requiredPermissions = arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_CONTACTS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        smsRecyclerView = findViewById(R.id.smsRecyclerView)
        searchEditText = findViewById(R.id.searchEditText)

        setupRecyclerView()
        setupSearch()
        requestPermissions()
    }

    private fun setupRecyclerView() {
        smsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = smsAdapter
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                smsAdapter.filter(s?.toString() ?: "")
            }
        })
    }

    private fun requestPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isEmpty()) {
            loadSmsInbox()
        } else {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                loadSmsInbox()
            } else {
                Toast.makeText(this, "Permissions needed to display messages", Toast.LENGTH_LONG).show()
            }
        }

    private fun loadSmsInbox() {
        val uri: Uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "address", "body", "date")

        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, "date DESC")
        cursor?.use { 
            val messages = mutableListOf<SmsMessage>()
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"))
                val address = cursor.getString(cursor.getColumnIndexOrThrow("address"))
                val body = cursor.getString(cursor.getColumnIndexOrThrow("body"))
                val date = cursor.getLong(cursor.getColumnIndexOrThrow("date"))
                val contactName = getContactName(address)
                
                messages.add(SmsMessage(id, address, contactName, body, date))
            }
            smsAdapter.setMessages(messages)
        } ?: run {
            Toast.makeText(this, "No SMS messages found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getContactName(phoneNumber: String): String? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        
        return contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
            } else null
        }
    }
}