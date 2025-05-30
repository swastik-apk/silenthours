package com.example.silenthours.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.ui.tooling.preview.Preview
import com.example.silenthours.ui.theme.SilentHoursTheme

@Composable
fun IntroScreen(
    onGetStartedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Phone number field removed as per new focus on permissions first.
    // var phoneNumber by remember { mutableStateOf("") }

    Column(
        modifier = modifier // Apply the modifier passed from MainActivity
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp), // Increased padding for more breathing room
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to SilentHours!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(48.dp)) // Increased spacing

        // Phone number field removed
        // OutlinedTextField(
        //     value = phoneNumber,
        //     onValueChange = { phoneNumber = it },
        //     label = { Text("Phone Number") },
        //     modifier = Modifier.fillMaxWidth(),
        //     singleLine = true
        // )

        Spacer(modifier = Modifier.height(40.dp)) // Increased spacing (adjust as needed after removing text field)

        Button(
            onClick = onGetStartedClick,
            modifier = Modifier.fillMaxWidth(0.8f) // Button takes 80% of width
        ) {
            Text("Get Started")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun IntroScreenPreview() {
    SilentHoursTheme {
        IntroScreen(
            onGetStartedClick = {},
            modifier = Modifier.padding(16.dp) // Preview uses its own padding for isolation
        )
    }
}
