package com.example.silenthours.ui.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.silenthours.model.Contact
import com.example.silenthours.ui.theme.SilentHoursTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPickerScreen(
    availableContacts: List<Contact>,
    initiallySelectedContactIds: Set<String>, // Use Set for efficient lookup
    onContactsSelected: (List<String>) -> Unit,
    onCancel: () -> Unit
) {
    var selectedContactIds by remember { mutableStateOf(initiallySelectedContactIds) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Contacts") },
                actions = {
                    TextButton(onClick = { onContactsSelected(selectedContactIds.toList()) }) {
                        Text("Done")
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onCancel) { // Using TextButton for "Cancel" like "Done"
                        Text("Cancel")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(availableContacts, key = { contact -> contact.id }) { contact ->
                ContactPickerItem(
                    contact = contact,
                    isSelected = selectedContactIds.contains(contact.id),
                    onSelectionChanged = { contactId, isSelected ->
                        selectedContactIds = if (isSelected) {
                            selectedContactIds + contactId
                        } else {
                            selectedContactIds - contactId
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ContactPickerItem(
    contact: Contact,
    isSelected: Boolean,
    onSelectionChanged: (contactId: String, isSelected: Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectionChanged(contact.id, !isSelected) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = contact.name, style = MaterialTheme.typography.bodyLarge)
            contact.phoneNumber?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Checkbox(
            checked = isSelected,
            onCheckedChange = { newSelectionState ->
                onSelectionChanged(contact.id, newSelectionState)
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ContactPickerScreenPreview() {
    SilentHoursTheme {
        val sampleContacts = listOf(
            Contact(id = "1", name = "Alice Wonderland", phoneNumber = "111-222-3333"),
            Contact(id = "2", name = "Bob The Builder", phoneNumber = "444-555-6666"),
            Contact(id = "3", name = "Charlie Brown", phoneNumber = "777-888-9999")
        )
        ContactPickerScreen(
            availableContacts = sampleContacts,
            initiallySelectedContactIds = setOf("2"),
            onContactsSelected = {},
            onCancel = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ContactPickerItemPreview() {
    SilentHoursTheme {
        ContactPickerItem(
            contact = Contact(id = "1", name = "Alice Wonderland", phoneNumber = "111-222-3333"),
            isSelected = true,
            onSelectionChanged = { _, _ -> }
        )
    }
}
