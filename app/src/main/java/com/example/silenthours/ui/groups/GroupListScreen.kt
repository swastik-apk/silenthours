package com.example.silenthours.ui.groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import android.app.Application // Required for PreviewGroupViewModel
import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // For ContentResolver
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.silenthours.model.Contact // Required for ContactPickerScreen
import com.example.silenthours.model.Group
import com.example.silenthours.ui.theme.SilentHoursTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


// Define different states for the screen
private enum class ScreenState {
    GroupList,
    ContactPicker
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(viewModel: GroupViewModel) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val availableContacts by viewModel.contacts.collectAsStateWithLifecycle()

    var currentScreenState by remember { mutableStateOf(ScreenState.GroupList) }
    var groupForContactEditing by remember { mutableStateOf<Group?>(null) }

    // States for dialogs
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showRenameGroupDialog by remember { mutableStateOf<Group?>(null) } // Holds group to rename
    var showDeleteGroupDialog by remember { mutableStateOf<Group?>(null) } // Holds group to delete

    var textFieldValue by rememberSaveable { mutableStateOf("") } // For rename/create dialogs
    val context = LocalContext.current

    when (currentScreenState) {
        ScreenState.GroupList -> {
            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(onClick = { showCreateGroupDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Create New Group")
                    }
                }
            ) { paddingValues ->
                Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                    if (groups.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No groups yet. Tap the '+' button to create one!")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(groups, key = { group -> group.id }) { group ->
                                GroupItem(
                                    group = group,
                                    onAddContactsClicked = {
                                        viewModel.loadContacts(context.contentResolver)
                                        groupForContactEditing = group
                                        currentScreenState = ScreenState.ContactPicker
                                    },
                                    onRenameClicked = { groupToRename ->
                                        textFieldValue = groupToRename.name // Pre-fill for rename
                                        showRenameGroupDialog = groupToRename
                                    },
                                    onDeleteClicked = { groupToDelete ->
                                        showDeleteGroupDialog = groupToDelete
                                    }
                                )
                            }
                        }
                    }
                }

                // Create Group Dialog
                if (showCreateGroupDialog) {
                    EditGroupNameDialog(
                        dialogTitle = "Create New Group",
                        initialValue = "", // Blank for new group
                        onConfirm = { name ->
                            viewModel.createGroup(name)
                            showCreateGroupDialog = false
                        },
                        onDismiss = { showCreateGroupDialog = false }
                    )
                }

                // Rename Group Dialog
                showRenameGroupDialog?.let { groupToRename ->
                    EditGroupNameDialog(
                        dialogTitle = "Rename Group",
                        initialValue = groupToRename.name,
                        onConfirm = { newName ->
                            viewModel.renameGroup(groupToRename, newName)
                            showRenameGroupDialog = null
                        },
                        onDismiss = { showRenameGroupDialog = null }
                    )
                }

                // Delete Group Confirmation Dialog
                showDeleteGroupDialog?.let { groupToDelete ->
                    AlertDialog(
                        onDismissRequest = { showDeleteGroupDialog = null },
                        title = { Text("Delete Group") },
                        text = { Text("Are you sure you want to delete group \"${groupToDelete.name}\"? This action cannot be undone.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.deleteGroup(groupToDelete)
                                    showDeleteGroupDialog = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showDeleteGroupDialog = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
        ScreenState.ContactPicker -> {
            groupForContactEditing?.let { groupToEdit ->
                ContactPickerScreen(
                    availableContacts = availableContacts,
                    initiallySelectedContactIds = groupToEdit.contactIds.toSet(),
                    onContactsSelected = { selectedIds ->
                        viewModel.addContactsToGroup(groupToEdit, selectedIds)
                        currentScreenState = ScreenState.GroupList
                        groupForContactEditing = null
                    },
                    onCancel = {
                        currentScreenState = ScreenState.GroupList
                        groupForContactEditing = null
                    }
                )
            } ?: run { // Should not happen if logic is correct
                currentScreenState = ScreenState.GroupList
            }
        }
    }
}

@Composable
fun GroupItem(
    group: Group,
    onAddContactsClicked: () -> Unit,
    onRenameClicked: () -> Unit,
    onDeleteClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp) // Reduced padding for more space for icons
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = group.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Contacts: ${group.contactIds.size}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row { // Row for action icons
                IconButton(onClick = onAddContactsClicked) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = "Add/Edit Contacts")
                }
                IconButton(onClick = onRenameClicked) {
                    Icon(Icons.Filled.Edit, contentDescription = "Rename Group")
                }
                IconButton(onClick = onDeleteClicked) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Group")
                }
            }
        }
    }
}

@Composable
fun EditGroupNameDialog(
    dialogTitle: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var textState by remember(initialValue) { mutableStateOf(initialValue) } // Keying remember to initialValue

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            OutlinedTextField(
                value = textState,
                onValueChange = { textState = it },
                label = { Text("Group Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (textState.isNotBlank()) {
                        onConfirm(textState)
                    }
                },
                enabled = textState.isNotBlank() // Disable if name is blank
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


// --- Preview ---
class PreviewGroupViewModel(application: Application) : GroupViewModel(application) {
    private val _previewGroupsFlow = MutableStateFlow(listOf(
        Group(id = 1, name = "Family", contactIds = listOf("c1", "c2")),
        Group(id = 2, name = "Work Friends", contactIds = listOf("c3")),
        Group(id = 3, name = "Weekend Cycling", contactIds = emptyList())
    ))
    override val groups: StateFlow<List<Group>> = _previewGroupsFlow

    private val _previewContactsFlow = MutableStateFlow(listOf(
        Contact(id = "c1", name = "Alice", phoneNumber = "111"),
        Contact(id = "c2", name = "Bob", phoneNumber = "222"),
        Contact(id = "c3", name = "Charlie", phoneNumber = "333"),
        Contact(id = "c4", name = "David", phoneNumber = "444")
    ))
    override val contacts: StateFlow<List<Contact>> = _previewContactsFlow

    override fun createGroup(groupName: String) {
        val newId = (_previewGroupsFlow.value.maxOfOrNull { it.id } ?: 0) + 1
        _previewGroupsFlow.value = _previewGroupsFlow.value + Group(id = newId, name = groupName, contactIds = emptyList())
    }

    override fun loadContacts(contentResolver: android.content.ContentResolver) { /* No-op */ }
    override fun addContactsToGroup(group: Group, selectedContactIds: List<String>) {
         val updatedGroups = _previewGroupsFlow.value.map {
            if (it.id == group.id) it.copy(contactIds = (it.contactIds + selectedContactIds).distinct()) else it
        }
        _previewGroupsFlow.value = updatedGroups
    }
    override fun renameGroup(group: Group, newName: String) {
        val updatedGroups = _previewGroupsFlow.value.map {
            if (it.id == group.id) it.copy(name = newName) else it
        }
        _previewGroupsFlow.value = updatedGroups
    }
    override fun deleteGroup(group: Group) {
        _previewGroupsFlow.value = _previewGroupsFlow.value.filterNot { it.id == group.id }
    }
}

@Preview(showBackground = true)
@Composable
fun GroupListScreenPreview() {
    SilentHoursTheme {
        val previewViewModel = PreviewGroupViewModel(Application())
        GroupListScreen(viewModel = previewViewModel)
    }
}

@Preview(showBackground = true)
@Composable
fun GroupListScreenEmptyPreview() {
    SilentHoursTheme {
        val emptyVM = object : GroupViewModel(Application()) {
            override val groups: StateFlow<List<Group>> = MutableStateFlow(emptyList())
            override val contacts: StateFlow<List<Contact>> = MutableStateFlow(emptyList())
            override fun createGroup(groupName: String) {}
            override fun loadContacts(contentResolver: android.content.ContentResolver) {}
            override fun addContactsToGroup(group: Group, selectedContactIds: List<String>) {}
            override fun renameGroup(group: Group, newName: String) {}
            override fun deleteGroup(group: Group) {}
        }
        GroupListScreen(viewModel = emptyVM)
    }
}

@Preview
@Composable
fun GroupItemPreview() {
    SilentHoursTheme {
        GroupItem(
            group = Group(id = 1, name = "Test Group", contactIds = listOf("1","2")),
            onAddContactsClicked = { },
            onRenameClicked = { },
            onDeleteClicked = { }
        )
    }
}
