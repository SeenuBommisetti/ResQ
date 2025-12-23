package com.seenubommisetti.resq.data

import android.content.Context
import androidx.core.content.edit
import com.seenubommisetti.resq.data.model.ContactModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val PREFS_NAME = "sos_prefs"
    private val KEY_CONTACTS = "contacts"

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveContacts(contacts: List<ContactModel>) {
        val set = contacts.map { "${it.name}|${it.number}" }.toSet()
        prefs.edit {
            putStringSet(KEY_CONTACTS, set)
        }
    }

    fun loadContacts(): List<ContactModel> {
        val set = prefs.getStringSet(KEY_CONTACTS, emptySet()) ?: emptySet()

        return set.mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size == 2) {
                ContactModel(parts[0], parts[1])
            } else {
                null
            }
        }
    }
}