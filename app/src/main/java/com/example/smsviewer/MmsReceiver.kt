package com.example.smsviewer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // For now, we'll just handle SMS. MMS implementation can be added later
    }
} 