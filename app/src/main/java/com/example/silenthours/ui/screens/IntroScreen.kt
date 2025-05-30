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
    onSignUpClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var phoneNumber by remember { mutableStateOf("") }

    Column(
        modifier = modifier // Apply the modifier passed from MainActivity
            .fillMaxSize()
            .padding(16.dp), // This padding will be applied inside the scaffold's padding
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to SilentHours!",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onSignUpClick) {
                Text("Sign Up")
            }
            Button(onClick = onLoginClick) {
                Text("Log In")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun IntroScreenPreview() {
    SilentHoursTheme {
        IntroScreen(onSignUpClick = {}, onLoginClick = {}, modifier = Modifier.padding(16.dp)) // Added padding for preview consistency
    }
}
