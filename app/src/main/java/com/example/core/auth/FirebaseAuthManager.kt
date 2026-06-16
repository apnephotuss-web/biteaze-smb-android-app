package com.example.core.auth

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.core.sync.CloudSyncManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("syncpos_settings", Context.MODE_PRIVATE)

    val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "FirebaseApp is not initialized or google-services.json is missing: ${e.message}")
            null
        }

    val firestore: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }

    val currentUser: FirebaseUser?
        get() = auth?.currentUser

    fun isFirebaseAvailable(): Boolean {
        return try {
            auth != null && auth?.app != null && firestore != null
        } catch (e: Exception) {
            false
        }
    }

    fun isUserLoggedIn(): Boolean {
        return isFirebaseAvailable() && auth?.currentUser != null
    }

    fun getLoggedInPhoneNumber(): String {
        return if (isUserLoggedIn()) {
            prefs.getString("logged_in_phone", "Authenticated User") ?: "Authenticated User"
        } else {
            "Not Logged In"
        }
    }

    suspend fun signUp(phone: String, email: String, password: String, category: String): Boolean {
        if (!isFirebaseAvailable()) throw Exception("Firebase is not initialized.")
        val cleanPhone = phone.trim()
        val cleanEmail = email.trim()
        
        try {
            // Check if phone already registered
            val phoneDoc = firestore!!.collection("user_phones").document(cleanPhone).get().await()
            if (phoneDoc.exists()) {
                throw Exception("This phone number is already registered.")
            }
        } catch (e: Exception) {
            if (e.message?.contains("offline") == true || e.message?.contains("UNAVAILABLE") == true) {
                 throw Exception("Could not connect to Firestore. Please ensure your Firestore Database is created in the Firebase Console and your device has internet.")
            }
            // Proceed to create if we can't fetch but it's not a known existence error (allow first time setup if rules block read)
        }

        try {
            // 1. Create User
            val result = auth!!.createUserWithEmailAndPassword(cleanEmail, password).await()
            val uid = result.user?.uid ?: throw Exception("Failed to get User ID")

            // 2. Map Phone -> Email for future logins
            firestore!!.collection("user_phones").document(cleanPhone)
                .set(mapOf("email" to cleanEmail))
                .await()

            // 3. Save User Profile
            firestore!!.collection("users").document(uid)
                .set(mapOf(
                    "phone" to cleanPhone,
                    "email" to cleanEmail,
                    "category" to category
                ))
                .await()

            // 4. Save Category Locally
            prefs.edit()
                .putString("business_category", category)
                .putString("logged_in_phone", cleanPhone)
                .apply()

            return true
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("configuration not found")) {
                throw Exception("Auth failed. Please ensure 'Email/Password' Sign-in method is enabled in Firebase Console.")
            } else if (msg.contains("offline") || msg.contains("UNAVAILABLE")) {
                throw Exception("Firestore offline. Make sure the Firestore Database is created in the Firebase Console.")
            }
            throw Exception("Failed to sign up: $msg")
        }
    }

    suspend fun signIn(phone: String, password: String): Boolean {
        if (!isFirebaseAvailable()) throw Exception("Firebase is not initialized.")
        val cleanPhone = phone.trim()

        try {
            // 1. Lookup Email by Phone
            val phoneDoc = firestore!!.collection("user_phones").document(cleanPhone).get().await()
            if (!phoneDoc.exists()) {
                throw Exception("Phone number not registered. Please sign up.")
            }
            
            val email = phoneDoc.getString("email") ?: throw Exception("Invalid mapping for phone number.")

            // 2. Sign In
            val result = auth!!.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("Failed to authenticate.")

            // 3. Fetch User Profile to get Category
            val userDoc = firestore!!.collection("users").document(uid).get().await()
            val category = userDoc.getString("category") ?: "F&B"

            // 4. Save Category Locally
            prefs.edit()
                .putString("business_category", category)
                .putString("logged_in_phone", cleanPhone)
                .apply()

            return true
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("offline") || msg.contains("UNAVAILABLE")) {
                throw Exception("Could not connect to Firestore. Please ensure your Firestore Database is created in the Firebase Console and your device has internet.")
            } else if (msg.contains("configuration not found")) {
                throw Exception("Auth failed. Please ensure 'Email/Password' Sign-in method is enabled in Firebase Console.")
            }
            throw Exception("Failed to sign in: $msg")
        }
    }

    suspend fun resetPassword(email: String): Boolean {
        if (!isFirebaseAvailable()) throw Exception("Firebase is not initialized.")
        try {
            auth!!.sendPasswordResetEmail(email.trim()).await()
            return true
        } catch (e: Exception) {
            throw Exception("Failed to send reset email: ${e.message}")
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
            prefs.edit().remove("logged_in_phone").apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
