package com.example.silenthours.ui.groups

import android.app.Application // Keep one Application import
import android.content.ContentResolver
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel // Keep one ViewModel import
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.silenthours.data.db.AppDatabase
import com.example.silenthours.data.db.GroupDao
import com.example.silenthours.model.Contact // Import Contact model
import com.example.silenthours.model.Group
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GroupViewModel(application: Application) : ViewModel() {

    private val groupDao: GroupDao = AppDatabase.getDatabase(application).groupDao()

    // For contacts loaded from device
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    val groups: StateFlow<List<Group>> = groupDao.getAllGroups()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Stop collecting after 5s of no subscribers
            initialValue = emptyList()
        )

    fun createGroup(groupName: String) {
        viewModelScope.launch {
            if (groupName.isNotBlank()) {
                val newGroup = Group(name = groupName, contactIds = emptyList())
                groupDao.insert(newGroup)
            }
        }
    }

    fun loadContacts(contentResolver: ContentResolver) {
        viewModelScope.launch {
            val contactList = mutableListOf<Contact>()
            // Using withContext to move the blocking C.R. query to an IO thread
            withContext(Dispatchers.IO) {
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )
                // Querying for contacts that have a phone number
                val cursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null, // No selection criteria for now, get all contacts with phone numbers
                    null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY + " ASC"
                )

                cursor?.use {
                    val idColumn = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val nameColumn = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                    val numberColumn = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)

                    while (it.moveToNext()) {
                        val id = it.getString(idColumn)
                        val name = it.getString(nameColumn)
                        val phoneNumber = it.getString(numberColumn)
                        // Ensure contact ID is unique, as a contact can have multiple phone numbers
                        if (contactList.none { c -> c.id == id }) {
                             contactList.add(Contact(id = id, name = name, phoneNumber = phoneNumber))
                        }
                    }
                }
            }
            _contacts.value = contactList
        }
    }

    fun addContactsToGroup(group: Group, selectedContactIds: List<String>) {
        viewModelScope.launch {
            val currentContactIds = group.contactIds.toMutableSet()
            currentContactIds.addAll(selectedContactIds)
            val updatedGroup = group.copy(contactIds = currentContactIds.toList())
            groupDao.update(updatedGroup)
        }
    }

    fun renameGroup(group: Group, newName: String) {
        viewModelScope.launch {
            if (newName.isNotBlank() && group.name != newName) {
                val updatedGroup = group.copy(name = newName)
                groupDao.update(updatedGroup)
            }
        }
    }

    fun deleteGroup(group: Group) {
        viewModelScope.launch {
            groupDao.deleteGroup(group.id) // Assumes deleteGroup in DAO takes groupId
        }
    }
}

class GroupViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroupViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
