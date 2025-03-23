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
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import android.content.ContentValues
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SmsViewer_Main" // Unique tag for MainActivity logs
    }

    private lateinit var smsRecyclerView: RecyclerView
    private lateinit var searchEditText: TextInputEditText
    private lateinit var composeFab: ExtendedFloatingActionButton
    private val smsAdapter = SmsAdapter { message ->
        if (!message.isSent && !message.isRead) {
            markMessageAsRead(message.id, message.isSent)
        }
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

        // Set status bar color to match header
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = getColor(R.color.purple_500)

        smsRecyclerView = findViewById(R.id.smsRecyclerView)
        searchEditText = findViewById(R.id.searchEditText)
        composeFab = findViewById(R.id.composeFab)

        setupRecyclerView()
        setupSearch()
        setupComposeFab()
        requestPermissions()
        
        // Check if we're the default SMS app
        checkDefaultSmsApp()

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_set_default -> {
                checkDefaultSmsApp()
                true
            }
            else -> super.onOptionsItemSelected(item)
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
        android.util.Log.i(TAG, "📥 Loading messages from ${if (isSent) "Sent" else "Inbox"}")
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.STATUS,
            Telephony.Sms.DATE_SENT,
            Telephony.Sms.READ
        )

        contentResolver.query(uri, projection, null, null, "${Telephony.Sms.DATE} DESC")?.use { cursor ->
            var count = 0
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
                val isRead = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1
                val contactName = getContactName(address)
                
                android.util.Log.v(TAG, "📨 Message: id=$id, isSent=$isSent, status=$status, isRead=$isRead, address=$address")
                
                messages.add(SmsMessage(
                    id = id,
                    address = address,
                    contactName = contactName,
                    body = body,
                    date = date,
                    isSent = isSent,
                    deliveryStatus = status,
                    deliveredDate = dateSent,
                    isRead = isRead
                ))
                count++
            }
            android.util.Log.i(TAG, "✅ Loaded $count messages from ${if (isSent) "Sent" else "Inbox"}")
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
        if (!isDefaultSmsApp()) {
            // This app is not the default SMS app
            val message = """
                This app needs to be the default SMS app to:
                • Mark messages as read
                • Send and receive SMS
                • Handle delivery reports
                
                Would you like to set it as default now?
            """.trimIndent()
            
            android.util.Log.i(TAG, "📱 App is not default SMS app")
            
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Set as Default SMS App")
                .setMessage(message)
                .setPositiveButton("Set as Default") { _, _ ->
                    requestDefaultSmsRole()
                }
                .setNegativeButton("Not Now") { _, _ ->
                    Toast.makeText(
                        this,
                        "You can set this app as default SMS app later from Settings",
                        Toast.LENGTH_LONG
                    ).show()
                    android.util.Log.i(TAG, "ℹ️ User chose not to set as default now")
                }
                .setNeutralButton("Learn More") { _, _ ->
                    showDefaultSmsAppInfo()
                }
                .show()
        } else {
            android.util.Log.i(TAG, "✅ App is default SMS app")
        }
    }

    private fun showDefaultSmsAppInfo() {
        val message = """
            Why does this app need to be the default SMS app?
            
            • To mark messages as read when you open them
            • To properly handle SMS delivery reports
            • To send and receive SMS messages
            • To manage your SMS inbox
            
            You can set this app as default SMS app anytime from:
            Settings > Apps > Default apps > SMS app
            
            Would you like to try setting it as default now?
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("About Default SMS App")
            .setMessage(message)
            .setPositiveButton("Set as Default") { _, _ ->
                requestDefaultSmsRole()
            }
            .setNegativeButton("Maybe Later", null)
            .show()
    }

    private fun requestDefaultSmsRole() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // For Android 14 and above, use the new role manager
            val roleManager = getSystemService(android.app.role.RoleManager::class.java)
            if (roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_SMS)) {
                val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_SMS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivityForResult(intent, 1001)
            } else {
                Toast.makeText(this, "SMS role is not available on this device", Toast.LENGTH_LONG).show()
                android.util.Log.e(TAG, "❌ SMS role is not available on this device")
            }
        } else {
            // For older versions
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            when (resultCode) {
                RESULT_OK -> {
                    Toast.makeText(this, "App is now set as default SMS app", Toast.LENGTH_SHORT).show()
                    android.util.Log.i(TAG, "✅ User granted default SMS app role")
                    // Refresh the UI to reflect the new status
                    loadAllMessages()
                }
                RESULT_CANCELED -> {
                    val message = """
                        App is not set as default SMS app.
                        Some features will be limited:
                        • Cannot mark messages as read
                        • Cannot send/receive SMS
                        • Cannot handle delivery reports
                        
                        You can set this app as default SMS app later from:
                        Settings > Apps > Default apps > SMS app
                    """.trimIndent()
                    
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Limited Functionality")
                        .setMessage(message)
                        .setPositiveButton("Try Again") { _, _ ->
                            requestDefaultSmsRole()
                        }
                        .setNegativeButton("OK", null)
                        .show()
                    
                    android.util.Log.w(TAG, "⚠️ User did not grant default SMS app role")
                }
                else -> {
                    Toast.makeText(this, "Failed to set as default SMS app. Please try again.", Toast.LENGTH_LONG).show()
                    android.util.Log.e(TAG, "❌ Failed to set as default SMS app. Result code: $resultCode")
                }
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

    private fun markMessageAsRead(messageId: Long, isSent: Boolean = false) {
        if (!isDefaultSmsApp()) {
            android.util.Log.e(TAG, "❌ Cannot mark message as read: not default SMS app")
            Toast.makeText(this, "Cannot mark message as read: app is not default SMS app", Toast.LENGTH_LONG).show()
            return
        }

        android.util.Log.i(TAG, "📝 Attempting to mark message $messageId as read (isSent: $isSent)")
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
        
        android.util.Log.i(TAG, "📊 Update result for ${if (isSent) "Sent" else "Inbox"}: $result")
        
        // Refresh the message list to show updated read status
        loadAllMessages()
    }
}