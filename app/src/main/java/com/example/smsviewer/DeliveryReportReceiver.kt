package com.example.smsviewer

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log

class DeliveryReportReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val messageId = intent.getLongExtra("message_id", -1L)
        if (messageId == -1L) return

        when (resultCode) {
            Activity.RESULT_OK -> {
                // Message was delivered successfully
                updateMessageStatus(context, messageId, Telephony.Sms.STATUS_COMPLETE)
                Log.d("DeliveryReport", "Message $messageId delivered successfully")
            }
            SmsManager.RESULT_ERROR_GENERIC_FAILURE,
            SmsManager.RESULT_ERROR_NO_SERVICE,
            SmsManager.RESULT_ERROR_NULL_PDU,
            SmsManager.RESULT_ERROR_RADIO_OFF -> {
                // Message delivery failed
                updateMessageStatus(context, messageId, Telephony.Sms.STATUS_FAILED)
                Log.d("DeliveryReport", "Message $messageId delivery failed: $resultCode")
            }
        }

        // Notify MainActivity to refresh the message list
        context.sendBroadcast(Intent(SmsReceiver.ACTION_SMS_RECEIVED))
    }

    private fun updateMessageStatus(context: Context, messageId: Long, status: Int) {
        val values = ContentValues().apply {
            put(Telephony.Sms.STATUS, status)
            if (status == Telephony.Sms.STATUS_COMPLETE) {
                put(Telephony.Sms.DATE_SENT, System.currentTimeMillis())
            }
        }

        context.contentResolver.update(
            Telephony.Sms.CONTENT_URI,
            values,
            "${Telephony.Sms._ID} = ?",
            arrayOf(messageId.toString())
        )
    }
} 