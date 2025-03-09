package com.example.smsviewer

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Telephony
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {
    private lateinit var smsRecyclerView: RecyclerView
    private lateinit var searchEditText: TextInputEditText
    private lateinit var composeFab: FloatingActionButton
    private val smsAdapter = SmsAdapter { message ->
        startActivity(SmsDetailActivity.createIntent(this, message))
    }

    private val smsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == SmsReceiver.ACTION_SMS_RECEIVED) {
                loadAllMessages()
            }
        }
    }

    private val requiredPermissions = arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.POST_NOTIFICATIONS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        smsRecyclerView = findViewById(R.id.smsRecyclerView)
        searchEditText = findViewById(R.id.searchEditText)
        composeFab = findViewById(R.id.composeFab)

        setupRecyclerView()
        setupSearch()
        setupComposeFab()
        requestPermissions()
        
        // For Android 14 and above, check if we're the default SMS app first
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            checkDefaultSmsApp()
        }

        // Register SMS receiver using LocalBroadcastManager
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(smsReceiver, IntentFilter(SmsReceiver.ACTION_SMS_RECEIVED))
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(smsReceiver)
    }

    override fun onResume() {
        super.onResume()
        if (hasRequiredPermissions()) {
            loadAllMessages()
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
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

    private fun setupComposeFab() {
        composeFab.setOnClickListener {
            startActivity(ComposeActivity.createIntent(this))
        }
    }

    private fun requestPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isEmpty()) {
            loadAllMessages()
        } else {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                loadAllMessages()
            } else {
                Toast.makeText(this, "Permissions needed to display messages", Toast.LENGTH_LONG).show()
            }
        }

    private fun loadAllMessages() {
        val messages = mutableListOf<SmsMessage>()
        
        // Load inbox messages
        loadMessages(Telephony.Sms.Inbox.CONTENT_URI, false, messages)
        // Load sent messages
        loadMessages(Telephony.Sms.Sent.CONTENT_URI, true, messages)

        // Sort all messages by date
        messages.sortByDescending { it.date }
        smsAdapter.setMessages(messages)
    }

    private fun loadMessages(uri: Uri, isSent: Boolean, messages: MutableList<SmsMessage>) {
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.STATUS,
            Telephony.Sms.DATE_SENT
        )

        contentResolver.query(uri, projection, null, null, "${Telephony.Sms.DATE} DESC")?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms._ID))
                val address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                val body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY))
                val date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE))
                val status = if (isSent) {
                    cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.STATUS))
                } else 0
                val dateSent = if (isSent) {
                    cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE_SENT))
                } else 0
                val contactName = getContactName(address)
                
                android.util.Log.d("SmsViewer", "Loading message: isSent=$isSent, status=$status, address=$address")
                
                messages.add(SmsMessage(
                    id = id,
                    address = address,
                    contactName = contactName,
                    body = body,
                    date = date,
                    isSent = isSent,
                    deliveryStatus = status,
                    deliveredDate = dateSent
                ))
            }
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

    private fun checkDefaultSmsApp() {
        if (Telephony.Sms.getDefaultSmsPackage(this) != packageName) {
            // This app is not the default SMS app
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // For Android 14 and above, use the new role manager
                val roleManager = getSystemService(android.app.role.RoleManager::class.java)
                if (roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_SMS) &&
                    !roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_SMS)) {
                    startActivityForResult(
                        roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_SMS),
                        1001
                    )
                }
            } else {
                // For older versions
                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                startActivity(intent)
            }
        }
    }
}