package com.example.smsviewer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        try {
            // Notify MainActivity to refresh
            context?.sendBroadcast(Intent(ACTION_SMS_RECEIVED))
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error processing SMS", e)
        }
    }

    companion object {
        const val ACTION_SMS_RECEIVED = "com.example.smsviewer.SMS_RECEIVED"
    }
} 