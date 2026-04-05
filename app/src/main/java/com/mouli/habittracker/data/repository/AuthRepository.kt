package com.mouli.habittracker.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val firebaseAuth: FirebaseAuth
) {
    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        _authState.value = auth.currentUser
    }

    private val _authState = MutableStateFlow(firebaseAuth.currentUser)
    val authState: StateFlow<FirebaseUser?> = _authState.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener(authListener)
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential).await().user
            ?: error("Google sign-in returned without a user.")
    }

    suspend fun continueWithAnonymous(): Result<FirebaseUser> = runCatching {
        firebaseAuth.currentUser ?: firebaseAuth.signInAnonymously().await().user
            ?: error("Anonymous sign-in returned without a user.")
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}

