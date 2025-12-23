package com.seenubommisetti.resq.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seenubommisetti.resq.data.model.ContactModel
import com.seenubommisetti.resq.data.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SosViewModel @Inject constructor(
    private val repository: ContactRepository
) : ViewModel() {

    private val _contacts = MutableStateFlow<List<ContactModel>>(emptyList())
    val contacts: StateFlow<List<ContactModel>> = _contacts.asStateFlow()

    init {
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            _contacts.value = repository.loadContacts()
        }
    }

    fun addContact(name: String, rawNumber: String) {
        val cleanedNumber = rawNumber.filter { it.isDigit() }.takeLast(10)

        if (cleanedNumber.length == 10) {
            val currentList = _contacts.value

            if (currentList.none { it.number == cleanedNumber }) {
                val newContact = ContactModel(name, cleanedNumber)
                val updatedList = currentList + newContact

                _contacts.value = updatedList
                repository.saveContacts(updatedList)
            }
        }
    }

    fun removeContact(contact: ContactModel) {
        val updatedList = _contacts.value - contact
        _contacts.value = updatedList
        repository.saveContacts(updatedList)
    }
}
