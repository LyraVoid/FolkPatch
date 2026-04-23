package me.bmax.apatch.util

import android.util.Log
import me.bmax.apatch.APApplication
import org.json.JSONArray
import org.json.JSONObject

object SuAuditLog {
    private const val TAG = "SuAuditLog"
    private const val PREF_KEY = "su_audit_log"
    private const val MAX_ENTRIES = 200

    data class SuAuditEntry(
        val timestamp: Long,
        val packageName: String,
        val uid: Int,
        val action: String,
    )

    private fun addEntry(entry: SuAuditEntry) {
        synchronized(this) {
            val prefs = APApplication.sharedPreferences
            val jsonStr = prefs.getString(PREF_KEY, "[]") ?: "[]"
            val jsonArray = JSONArray(jsonStr)

            val jsonObj = JSONObject().apply {
                put("ts", entry.timestamp)
                put("pkg", entry.packageName)
                put("uid", entry.uid)
                put("act", entry.action)
            }
            jsonArray.put(jsonObj)

            // Trim to max entries (keep newest)
            while (jsonArray.length() > MAX_ENTRIES) {
                jsonArray.remove(0)
            }

            prefs.edit().putString(PREF_KEY, jsonArray.toString()).apply()
            Log.d(TAG, "Logged ${entry.action} for ${entry.packageName} uid=${entry.uid}")
        }
    }

    fun logGrant(packageName: String, uid: Int) {
        addEntry(SuAuditEntry(System.currentTimeMillis(), packageName, uid, "GRANT"))
    }

    fun logRevoke(packageName: String, uid: Int) {
        addEntry(SuAuditEntry(System.currentTimeMillis(), packageName, uid, "REVOKE"))
    }

    fun logExclude(packageName: String, uid: Int) {
        addEntry(SuAuditEntry(System.currentTimeMillis(), packageName, uid, "EXCLUDE"))
    }

    fun getEntries(): List<SuAuditEntry> {
        synchronized(this) {
            val prefs = APApplication.sharedPreferences
            val jsonStr = prefs.getString(PREF_KEY, "[]") ?: "[]"
            val jsonArray = JSONArray(jsonStr)

            val entries = mutableListOf<SuAuditEntry>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                entries.add(
                    SuAuditEntry(
                        timestamp = obj.getLong("ts"),
                        packageName = obj.getString("pkg"),
                        uid = obj.getInt("uid"),
                        action = obj.getString("act"),
                    )
                )
            }
            // Return in reverse chronological order (newest first)
            return entries.reversed()
        }
    }

    fun clearEntries() {
        synchronized(this) {
            APApplication.sharedPreferences.edit().remove(PREF_KEY).apply()
            Log.d(TAG, "Audit log cleared")
        }
    }
}
