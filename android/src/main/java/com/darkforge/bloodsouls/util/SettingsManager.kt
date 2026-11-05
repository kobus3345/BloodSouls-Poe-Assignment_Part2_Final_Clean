package com.darkforge.bloodsouls.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("BloodSoulsPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "SettingsManager"
        const val KEY_BLOOD_INTENSITY = "key_blood_intensity"
        const val KEY_LANGUAGE = "key_language"
        const val KEY_MUSIC_VOLUME = "key_music_volume"
        const val KEY_LOGGED_IN_USER = "key_logged_in_user"
        const val KEY_AUTH_TOKEN = "key_auth_token"
        const val KEY_DIFFICULTY = "key_difficulty"
        const val KEY_USER_EMAIL = "key_user_email"
    }

    var bloodIntensity: String
        get() = prefs.getString(KEY_BLOOD_INTENSITY, "HIGH") ?: "HIGH"
        set(value) {
            Log.d(TAG, "Saving Blood Intensity setting: $value")
            prefs.edit().putString(KEY_BLOOD_INTENSITY, value).apply()
        }

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "ENGLISH") ?: "ENGLISH"
        set(value) {
            Log.d(TAG, "Saving Language preference: $value")
            prefs.edit().putString(KEY_LANGUAGE, value).apply()
        }

    var musicVolume: Int
        get() = prefs.getInt(KEY_MUSIC_VOLUME, 80)
        set(value) {
            Log.d(TAG, "Saving Music Volume: $value")
            prefs.edit().putInt(KEY_MUSIC_VOLUME, value).apply()
        }

    var difficulty: String
        get() = prefs.getString(KEY_DIFFICULTY, "CURSED") ?: "CURSED"
        set(value) {
            Log.d(TAG, "Saving Game Difficulty setting: $value")
            prefs.edit().putString(KEY_DIFFICULTY, value).apply()
        }

    var loggedInUser: String?
        get() = prefs.getString(KEY_LOGGED_IN_USER, null)
        set(value) {
            Log.d(TAG, "Updating Logged-In User session: $value")
            prefs.edit().putString(KEY_LOGGED_IN_USER, value).apply()
        }

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) {
            Log.d(TAG, "Updating Saved User Email session: $value")
            prefs.edit().putString(KEY_USER_EMAIL, value).apply()
        }

    var authToken: String?
        get() = prefs.getString(KEY_AUTH_TOKEN, null)
        set(value) {
            Log.d(TAG, "Updating Auth Token: ${value?.take(10)}...")
            prefs.edit().putString(KEY_AUTH_TOKEN, value).apply()
        }

    var fontSize: Float
        get() = prefs.getFloat("font_size", 14f)
        set(value) = prefs.edit().putFloat("font_size", value).apply()

    fun isLoggedIn(): Boolean {
        val loggedIn = !loggedInUser.isNullOrEmpty()
        Log.d(TAG, "Checking login status: $loggedIn (user: $loggedInUser)")
        return loggedIn
    }

    fun logout() {
        Log.i(TAG, "Logging out user: $loggedInUser. Clearing session tokens.")
        prefs.edit()
            .remove(KEY_LOGGED_IN_USER)
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_USER_EMAIL)
            .putFloat("font_size", 14f)
            .apply()

        Log.i(TAG, "Logout successful. Font size reset to 14sp.")
    }
}
