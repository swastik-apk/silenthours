package com.example.silenthours.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.silenthours.ui.theme.SilentHoursTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IntroScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun introScreen_displaysWelcomeMessage() {
        composeTestRule.setContent {
            SilentHoursTheme {
                IntroScreen(onSignUpClick = {}, onLoginClick = {})
            }
        }
        composeTestRule.onNodeWithText("Welcome to SilentHours!").assertIsDisplayed()
    }

    @Test
    fun introScreen_displaysPhoneNumberTextField() {
        composeTestRule.setContent {
            SilentHoursTheme {
                IntroScreen(onSignUpClick = {}, onLoginClick = {})
            }
        }
        // The label "Phone Number" is associated with the OutlinedTextField
        composeTestRule.onNodeWithText("Phone Number").assertIsDisplayed()
    }

    @Test
    fun introScreen_displaysSignUpButton() {
        composeTestRule.setContent {
            SilentHoursTheme {
                IntroScreen(onSignUpClick = {}, onLoginClick = {})
            }
        }
        composeTestRule.onNodeWithText("Sign Up").assertIsDisplayed()
    }

    @Test
    fun introScreen_displaysLoginButton() {
        composeTestRule.setContent {
            SilentHoursTheme {
                IntroScreen(onSignUpClick = {}, onLoginClick = {})
            }
        }
        composeTestRule.onNodeWithText("Log In").assertIsDisplayed()
    }
}
