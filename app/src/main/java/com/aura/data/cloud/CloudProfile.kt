package com.aura.data.cloud

import android.content.Context
import android.os.Bundle
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.aura.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

internal object CloudProfile {
    suspend fun signIn(context: Context): String {
        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.let { return it.uid }
        val option = GetSignInWithGoogleOption.Builder(context.getString(R.string.default_web_client_id)).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(context).getCredential(context, request).credential
        check(credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
        val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
        return checkNotNull(auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await().user).uid
    }

    suspend fun signOut(context: Context) {
        FirebaseAuth.getInstance().signOut()
        // Forgetting the chosen Google account is best effort; a slow provider must not hold up sign-out.
        withTimeoutOrNull(5_000) {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
    }

    suspend fun loadName(uid: String): String? {
        val profile = document(uid).get().await()
        // A profile saved before setup became mandatory may have no name; treat it as incomplete.
        return profile.getString("basic.name")?.takeIf { it.isNotBlank() }
    }

    suspend fun save(basic: Bundle, body: Bundle, goals: Bundle) {
        val uid = checkNotNull(FirebaseAuth.getInstance().currentUser).uid
        document(uid).set(mapOf(
            "basic" to basic.fields(),
            "body" to body.fields(),
            "goals" to goals.fields(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )).await()
    }

    private fun document(uid: String) = FirebaseFirestore.getInstance().collection("profiles").document(uid)

    @Suppress("DEPRECATION")
    private fun Bundle.fields() = keySet().associateWith { get(it) }
}
