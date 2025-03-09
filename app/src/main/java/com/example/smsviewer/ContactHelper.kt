package com.example.smsviewer

import android.content.Context
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.telephony.PhoneNumberUtils

data class Contact(
    val id: String,
    val name: String,
    val number: String,
    val displayString: String = "$name ($number)"
) {
    val normalizedNumber: String = PhoneNumberUtils.normalizeNumber(number) ?: number
}

class ContactHelper(private val context: Context) {
    fun searchContacts(query: String): List<Contact> {
        val contacts = mutableListOf<Contact>()
        if (query.length < 2) return contacts

        val selection = """
            ${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} LIKE ? OR 
            ${Phone.NUMBER} LIKE ?
        """.trimIndent().replace("\n", " ")

        val selectionArgs = arrayOf("%$query%", "%$query%")
        val projection = arrayOf(
            Phone._ID,
            Phone.DISPLAY_NAME_PRIMARY,
            Phone.NUMBER,
            Phone.TYPE,
            Phone.LABEL
        )

        val contactMap = mutableMapOf<String, Contact>()

        context.contentResolver.query(
            Phone.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndex(Phone._ID)
            val nameColumn = cursor.getColumnIndex(Phone.DISPLAY_NAME_PRIMARY)
            val numberColumn = cursor.getColumnIndex(Phone.NUMBER)
            val typeColumn = cursor.getColumnIndex(Phone.TYPE)
            val labelColumn = cursor.getColumnIndex(Phone.LABEL)

            while (cursor.moveToNext()) {
                val id = cursor.getString(idColumn)
                val name = cursor.getString(nameColumn) ?: continue
                val number = cursor.getString(numberColumn) ?: continue
                val normalizedNumber = PhoneNumberUtils.normalizeNumber(number) ?: number

                // If we already have this number, only replace if this is a mobile number
                val type = cursor.getInt(typeColumn)
                val existingContact = contactMap[normalizedNumber]
                
                if (existingContact == null || type == Phone.TYPE_MOBILE) {
                    val contact = Contact(id, name, number)
                    contactMap[normalizedNumber] = contact
                }
            }
        }

        return contactMap.values.sortedBy { it.name }
    }
} 