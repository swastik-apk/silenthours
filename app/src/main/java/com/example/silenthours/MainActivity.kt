package com.example.silenthours

import android.Manifest
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Settings as SettingsIcon // Alias to avoid conflict
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.silenthours.ui.groups.GroupListScreen
import com.example.silenthours.ui.groups.GroupViewModel
import com.example.silenthours.ui.groups.GroupViewModelFactory
import com.example.silenthours.ui.screens.IntroScreen
import com.example.silenthours.ui.settings.SettingsScreen
import com.example.silenthours.ui.settings.SettingsViewModel
import com.example.silenthours.ui.settings.SettingsViewModelFactory
import com.example.silenthours.ui.theme.SilentHoursTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SilentHoursTheme {
                MainContent()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainContent() {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ANSWER_PHONE_CALLS
            // MODIFY_AUDIO_SETTINGS is not a runtime permission, so not requested here.
        )
    )
    val context = LocalContext.current
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.GROUPS) }
    val application = LocalContext.current.applicationContext as Application

    // Hoist SettingsViewModel to be available for TopAppBar icon
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(application))
    val isSilencingEffectivelyActive by settingsViewModel.isCurrentlySilencingActive.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (permissionsState.allPermissionsGranted) {
                TopAppBar(
                    title = { Text(if (currentScreen == AppScreen.GROUPS) "Groups" else "Settings") },
                    actions = {
                        if (isSilencingEffectivelyActive) {
                            Icon(
                                Icons.Filled.DoNotDisturbOn,
                                contentDescription = "Silencing is Active",
                                tint = MaterialTheme.colorScheme.primary // Or another distinct color
                            )
                            Spacer(modifier = Modifier.width(8.dp)) // Add some spacing
                        }
                        IconButton(onClick = {
                            currentScreen = if (currentScreen == AppScreen.GROUPS) AppScreen.SETTINGS else AppScreen.GROUPS
                        }) {
                            Icon(
                                imageVector = if (currentScreen == AppScreen.GROUPS) SettingsIcon else Icons.Filled.Groups,
                                contentDescription = if (currentScreen == AppScreen.GROUPS) "Open Settings" else "Open Groups"
                            )
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Apply scaffold padding here (includes TopAppBar height)
                // .padding(16.dp), // Content-specific padding should be inside each screen
            verticalArrangement = Arrangement.Center, // This might be too restrictive for screen content
            horizontalAlignment = Alignment.CenterHorizontally // Same as above
        ) {
            when {
                // 1. All permissions granted
                permissionsState.allPermissionsGranted -> {
                    when (currentScreen) {
                        AppScreen.GROUPS -> {
                            val groupViewModel: GroupViewModel = viewModel(factory = GroupViewModelFactory(application))
                            GroupListScreen(viewModel = groupViewModel)
                        }
                        AppScreen.SETTINGS -> {
                            // settingsViewModel is already hoisted
                            SettingsScreen(viewModel = settingsViewModel)
                        }
                    }
                }
                // 2. Permissions permanently denied (and already requested at least once)
                !permissionsState.allPermissionsGranted && permissionsState.permissionRequested && !permissionsState.shouldShowRationale -> {
                    // Centering this specific message content
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Permissions have been permanently denied. " +
                                    "This app needs Contacts, Call Log, and Phone State access to function. " +
                                    "ANSWER_PHONE_CALLS is also needed to silence calls effectively. " +
                                    "You can enable them in app settings."
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = Uri.fromParts("package", context.packageName, null)
                            context.startActivity(intent)
                        }) {
                            Text("Open Settings")
                        }
                    }
                }
                // 3. Rationale should be shown (denied once, can ask again)
                permissionsState.shouldShowRationale -> {
                     // Centering this specific message content
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Contacts, Call Log, and Phone State permissions are important for this app. " +
                             "ANSWER_PHONE_CALLS is also needed to silence calls effectively. " +
                             "Please grant them to use the core features. If you deny again, you may need to go to settings.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                            Text("Request Permissions Again")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.data = Uri.fromParts("package", context.packageName, null)
                                context.startActivity(intent)
                            }) {
                                Text("Open Settings")
                            }
                    }
                }
                // 4. Initial state / Not yet requested or ready to request
                else -> {
                     // Centering this specific message content (IntroScreen typically handles its own alignment)
                    Column(modifier = Modifier.fillMaxSize().padding(0.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        IntroScreen(
                            onGetStartedClick = {
                                permissionsState.launchMultiplePermissionRequest()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

enum class AppScreen {
    GROUPS,
    SETTINGS
}