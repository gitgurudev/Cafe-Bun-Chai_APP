package com.cafebunchai.pos.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    val state: Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.transform { user ->
        if (user == null) {
            emit(AuthState.SignedOut)
        } else {
            val role = fetchRole(user.uid)
            emit(
                AuthState.SignedIn(
                    StaffSession(
                        uid = user.uid,
                        email = user.email.orEmpty(),
                        isAdmin = role == "admin",
                    ),
                ),
            )
        }
    }

    suspend fun signIn(email: String, password: String) {
        try {
            auth.signInWithEmailAndPassword(email.trim(), password).await()
        } catch (_: FirebaseAuthInvalidUserException) {
            error("No account for this email.")
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            error("Wrong email or password.")
        }
    }

    fun signOut() {
        auth.signOut()
    }

    private suspend fun fetchRole(uid: String): String {
        val snap = runCatching {
            db.collection("users").document(uid).get().await()
        }.getOrNull() ?: return "staff"
        if (snap == null || !snap.exists()) return "staff"
        return snap.getString("role")?.trim()?.lowercase() ?: "staff"
    }
}
