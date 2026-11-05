package com.darkforge.bloodsouls.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.darkforge.bloodsouls.net.FirebaseAuthService
import org.json.JSONObject

data class ProgressData(
    val slotId: Int = 1,
    val mapName: String = "sewer_map.tmx",
    val posX: Float = 500f,
    val posY: Float = 500f,
    val difficulty: String = "CURSED",
    val health: Int = 100,
    val timestamp: Long = System.currentTimeMillis()
)

class UserProgressManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("BloodSoulsProgress", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "UserProgressManager"
    }

    /**
     * Saves user progress both locally on the device AND to the remote Firebase Firestore database.
     */
    fun saveProgress(progress: ProgressData, userId: String? = null) {
        Log.i(TAG, "Saving user progress for Slot #${progress.slotId} - Map: ${progress.mapName}, Difficulty: ${progress.difficulty}, HP: ${progress.health}")

        // 1. Save locally on device
        val json = JSONObject().apply {
            put("slot_id", progress.slotId)
            put("map_name", progress.mapName)
            put("pos_x", progress.posX.toDouble())
            put("pos_y", progress.posY.toDouble())
            put("difficulty", progress.difficulty)
            put("health", progress.health)
            put("timestamp", progress.timestamp)
        }

        prefs.edit().putString("save_slot_${progress.slotId}", json.toString()).apply()
        Log.d(TAG, "Successfully wrote local save file for Slot #${progress.slotId}")

        // 2. Save remotely to Firebase Firestore Database
        if (!userId.isNullOrEmpty()) {
            Log.i(TAG, "Syncing progress to Firebase Firestore for UID: $userId")
            FirebaseAuthService.saveSettingsToFirestore(
                uid = userId,
                bloodIntensity = progress.mapName,
                language = progress.difficulty,
                difficulty = "Health_${progress.health}"
            )
        }
    }

    /**
     * Loads user progress from local device storage.
     */
    fun loadProgress(slotId: Int): ProgressData {
        Log.d(TAG, "Loading user progress for Slot #$slotId")
        val rawJson = prefs.getString("save_slot_$slotId", null)
        if (rawJson.isNullOrEmpty()) {
            Log.w(TAG, "No existing save data found for Slot #$slotId. Returning default starting progress.")
            return ProgressData(slotId = slotId)
        }

        return try {
            val json = JSONObject(rawJson)
            val progress = ProgressData(
                slotId = json.optInt("slot_id", slotId),
                mapName = json.optString("map_name", "sewer_map.tmx"),
                posX = json.optDouble("pos_x", 500.0).toFloat(),
                posY = json.optDouble("pos_y", 500.0).toFloat(),
                difficulty = json.optString("difficulty", "CURSED"),
                health = json.optInt("health", 100),
                timestamp = json.optLong("timestamp", System.currentTimeMillis())
            )
            Log.i(TAG, "Loaded save slot #$slotId: Map=${progress.mapName}, Difficulty=${progress.difficulty}, HP=${progress.health}")
            progress
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing save slot #$slotId JSON data: ${e.message}", e)
            ProgressData(slotId = slotId)
        }
    }
}
