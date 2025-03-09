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
    
    fun matches(query: String): Boolean {
        val lowerQuery = query.lowercase()
        return when {
            // Check if query matches phone number
            number.contains(query) -> true
            normalizedNumber.contains(query) -> true
            
            // Check various name formats
            name.lowercase().contains(lowerQuery) -> true
            
            // Check for first name or last name matches
            name.split(" ").any { it.lowercase().startsWith(lowerQuery) } -> true
            
            else -> false
        }
    }
}

class ContactHelper(private val context: Context) {
    fun searchContacts(query: String): List<Contact> {
        val contacts = mutableListOf<Contact>()
        if (query.length < 2) return contacts

        // Create a more flexible search pattern for names
        val namePattern = query.split(" ").joinToString("%") { it.trim() }
        
        val selection = """
            ${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} LIKE ? OR 
            ${Phone.NUMBER} LIKE ? OR
            ${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} LIKE ? OR
            ${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} LIKE ?
        """.trimIndent().replace("\n", " ")

        val selectionArgs = arrayOf(
            "%$query%",           // Exact match
            "%$query%",           // Number match
            "% $query%",          // Last name match
            "%$namePattern%"      // Split name match
        )

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
                    // Only add if it actually matches our search criteria
                    if (contact.matches(query)) {
                        contactMap[normalizedNumber] = contact
                    }
                }
            }
        }

        // Sort results with priority:
        // 1. Exact name matches first
        // 2. Name starts with query
        // 3. Contains query anywhere
        // 4. Alphabetical within each group
        return contactMap.values.sortedWith(compareBy<Contact> { !it.name.equals(query, ignoreCase = true) }
            .thenBy { !it.name.startsWith(query, ignoreCase = true) }
            .thenBy { it.name })
    }
} 