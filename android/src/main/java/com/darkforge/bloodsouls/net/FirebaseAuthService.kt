
package com.darkforge.bloodsouls.net

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseAuthService {

    private const val TAG = "FirebaseAuthService"
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    var lastGoogleSignInWasNewUser: Boolean = false
        private set

    interface AuthCallback {
        fun onSuccess(userId: String)
        fun onError(error: String)
    }

    fun isAvailable(): Boolean {
        return try {
            val available = FirebaseAuth.getInstance() != null
            Log.d(TAG, "Checking Firebase availability: $available")
            available
        } catch (e: Exception) {
            Log.w(TAG, "Firebase unavailable: ${e.message}")
            false
        }
    }

    fun registerWithFirebase(email: String, passwordHash: String, callback: AuthCallback) {
        Log.i(TAG, "registerWithFirebase initiated for $email")

        try {
            auth.createUserWithEmailAndPassword(email, passwordHash)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: run { callback.onError("Firebase user ID is missing")
                        return@addOnSuccessListener }
                    Log.i(TAG, "Firebase user created successfully. UID: $uid")
                    saveUserToFirestore(uid, email)
                    callback.onSuccess(uid)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Firebase registration failed: ${exception.message}", exception)
                    callback.onError(exception.message ?: "Firebase registration failed")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Firebase createUserWithEmailAndPassword: ${e.message}", e)
            callback.onError(e.message ?: "Firebase not initialized")
        }
    }

    fun loginWithFirebase(email: String, passwordHash: String, callback: AuthCallback) {
        Log.i(TAG, "loginWithFirebase initiated for $email")

        try {
            auth.signInWithEmailAndPassword(email, passwordHash)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: run { callback.onError("Firebase user ID is missing")
                            return@addOnSuccessListener
                        }
                    Log.i(TAG, "Firebase login successful. UID: $uid")
                    callback.onSuccess(uid)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Firebase login failed: ${exception.message}", exception)
                    callback.onError(exception.message ?: "Firebase login failed")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Firebase signInWithEmailAndPassword: ${e.message}", e)
            callback.onError(e.message ?: "Firebase not initialized")
        }
    }

    fun signInWithGoogleCredential(idToken: String, callback: AuthCallback) {
        Log.i(TAG, "Starting Google Firebase authentication")

        if (idToken.isBlank()) {
            callback.onError("Google ID token is empty")
            return
        }

        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)

            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {

                        val user = auth.currentUser

                        if (user == null) {
                            callback.onError("Firebase user is null")
                            return@addOnCompleteListener
                        }

                        val uid = user.uid
                        val email = user.email ?: ""

                        lastGoogleSignInWasNewUser =
                            task.result?.additionalUserInfo?.isNewUser == true

                        Log.i(TAG, "Google Firebase authentication successful")
                        Log.i(TAG, "Firebase UID: $uid")
                        Log.i(TAG, "Firebase email: $email")
                        Log.i(TAG, "Google account is new: $lastGoogleSignInWasNewUser")

                        if (email.isNotBlank()) {
                            saveUserToFirestore(uid, email)
                        }

                        callback.onSuccess(uid)

                    } else {
                        val exception = task.exception
                        Log.e(TAG, "Firebase Google authentication failed", exception)
                        callback.onError(exception?.localizedMessage ?: "Unknown Firebase authentication error")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Google authentication exception", e)
            callback.onError(
                e.localizedMessage ?: "Google authentication failed"
            )
        }
    }

    private fun saveUserToFirestore(uid: String, email: String) {
        Log.d(TAG, "saveUserToFirestore saving document for UID: $uid ($email)")

        try {
            val userMap = hashMapOf(
                "email" to email,
                "created_at" to System.currentTimeMillis()
            )

            db.collection("users")
                .document(uid)
                .set(userMap)
                .addOnSuccessListener {
                    Log.i(TAG, "Firestore user record created successfully for $uid")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to save user record to Firestore: ${e.message}", e)
                }

        } catch (e: Exception) {
            Log.e(TAG, "Firestore write exception: ${e.message}", e)
        }
    }

    fun saveSettingsToFirestore(uid: String, bloodIntensity: String, language: String, difficulty: String) {
        Log.d(TAG, "saveSettingsToFirestore saving document for UID: $uid")

        try {
            val settingsMap = hashMapOf(
                "blood_intensity" to bloodIntensity,
                "language" to language,
                "difficulty" to difficulty,
                "updated_at" to System.currentTimeMillis()
            )

            db.collection("users")
                .document(uid)
                .collection("settings")
                .document("preferences")
                .set(settingsMap)
                .addOnSuccessListener {
                    Log.i(TAG, "Firestore user preferences updated successfully for $uid")
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to save preferences to Firestore: ${e.message}", e)
                }

        } catch (e: Exception) {
            Log.e(TAG, "Firestore preferences write exception: ${e.message}", e)
        }
    }

    fun signOut() {
        auth.signOut()
        lastGoogleSignInWasNewUser = false
        Log.i(TAG, "Firebase user signed out")
    }
}
