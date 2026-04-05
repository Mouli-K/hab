package com.mouli.habittracker.ui.screens.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.mouli.habittracker.model.MascotMood
import com.mouli.habittracker.ui.components.GlassPanel
import com.mouli.habittracker.ui.components.PandaMascot
import com.mouli.habittracker.ui.theme.habBackgroundBrush

@Composable
fun AuthScreen(
    uiState: AuthUiState,
    googleClientId: String,
    onGoogleToken: (String) -> Unit,
    onGoogleFailure: (String) -> Unit
) {
    val context = LocalContext.current
    val googleSignInClient = remember(googleClientId) {
        if (googleClientId.isBlank()) {
            null
        } else {
            GoogleSignIn.getClient(
                context,
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestIdToken(googleClientId)
                    .build()
            )
        }
    }
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.data == null) {
            onGoogleFailure("Google sign-in was canceled before the panda got your answer.")
            return@rememberLauncherForActivityResult
        }

        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        runCatching { task.getResult(ApiException::class.java) }
            .onFailure { error ->
                val message = when (error) {
                    is ApiException -> when (error.statusCode) {
                        CommonStatusCodes.CANCELED -> "Google sign-in was canceled before the panda got your answer."
                        else -> "Google sign-in could not finish. Check that the Android SHA keys and the Web OAuth client are in the same Firebase project."
                    }
                    else -> error.message ?: "Google sign-in could not finish."
                }
                onGoogleFailure(message)
            }
            .onSuccess { account ->
                val token = account.idToken
                if (token.isNullOrBlank()) {
                    onGoogleFailure("The Firebase config is missing a usable web client ID for Google login.")
                } else {
                    onGoogleToken(token)
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(habBackgroundBrush())
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PandaMascot(mood = MascotMood.CHEERING)
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Hab",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "A habit companion that feels more like a best friend than a scoreboard.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))
            GlassPanel(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Your panda can greet you, protect streaks with freezes, and nudge you back with kindness.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = {
                        val client = googleSignInClient
                        if (client == null) {
                            onGoogleFailure("`google-services.json` is linked, but it does not include a web OAuth client for Google sign-in yet.")
                        } else {
                            client.signOut()
                            signInLauncher.launch(client.signInIntent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isBusy
                ) {
                    Text(if (googleSignInClient == null) "Google Login Needs OAuth Setup" else "Continue with Google")
                }
                if (!uiState.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = uiState.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                    )
                }
            }
        }
    }
}
