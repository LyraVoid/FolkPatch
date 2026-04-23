package me.bmax.apatch.util

import android.util.Log
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONException

object SuAuditLog {
    private const val TAG = "SuAuditLog"
    private const val PREF_KEY = "su_audit_log"
    private const val MAX_ENTRIES = 200

    sealed class AuditEntry {
        abstract val timestamp: Long
        abstract val uid: Int

        data class KernelEntry(
            override val timestamp: Long,
            override val uid: Int,
            val pid: Int,
            val tgid: Int,
            val toUid: Int,
            val scontext: String,
            val comm: String,
        ) : AuditEntry()

        data class AppEntry(
            override val timestamp: Long,
            override val uid: Int,
            val packageName: String,
            val action: String,
        ) : AuditEntry()
    }

    private fun addEntry(entry: AuditEntry.AppEntry) {
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

            while (jsonArray.length() > MAX_ENTRIES) {
                jsonArray.remove(0)
            }

            prefs.edit().putString(PREF_KEY, jsonArray.toString()).apply()
            Log.d(TAG, "Logged ${entry.action} for ${entry.packageName} uid=${entry.uid}")
        }
    }

    fun logGrant(packageName: String, uid: Int) {
        addEntry(AuditEntry.AppEntry(System.currentTimeMillis(), uid, packageName, "GRANT"))
    }

    fun logRevoke(packageName: String, uid: Int) {
        addEntry(AuditEntry.AppEntry(System.currentTimeMillis(), uid, packageName, "REVOKE"))
    }

    fun logExclude(packageName: String, uid: Int) {
        addEntry(AuditEntry.AppEntry(System.currentTimeMillis(), uid, packageName, "EXCLUDE"))
    }

    fun getKernelEntries(): List<AuditEntry.KernelEntry> {
        val jsonStr = try {
            Natives.suAuditList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read kernel audit log", e)
            return emptyList()
        }

        if (jsonStr.isEmpty() || jsonStr == "[]") return emptyList()

        val entries = mutableListOf<AuditEntry.KernelEntry>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                entries.add(
                    AuditEntry.KernelEntry(
                        timestamp = obj.optLong("ts", 0), // sequence number, used for ordering
                        uid = obj.getInt("uid"),
                        pid = obj.getInt("pid"),
                        tgid = obj.getInt("tgid"),
                        toUid = obj.getInt("to_uid"),
                        scontext = obj.optString("sctx", ""),
                        comm = obj.optString("comm", ""),
                    )
                )
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Failed to parse kernel audit log", e)
        }

        return entries.filter { it.uid != 0 }.reversed()
    }

    fun getAppEntries(): List<AuditEntry.AppEntry> {
        synchronized(this) {
            val prefs = APApplication.sharedPreferences
            val jsonStr = prefs.getString(PREF_KEY, "[]") ?: "[]"
            val jsonArray = JSONArray(jsonStr)

            val entries = mutableListOf<AuditEntry.AppEntry>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                entries.add(
                    AuditEntry.AppEntry(
                        timestamp = obj.getLong("ts"),
                        uid = obj.getInt("uid"),
                        packageName = obj.getString("pkg"),
                        action = obj.getString("act"),
                    )
                )
            }
            return entries.reversed()
        }
    }

    fun getAllEntries(): List<AuditEntry> {
        val kernel = getKernelEntries()
        val app = getAppEntries()
        return (kernel + app).sortedByDescending { it.timestamp }
    }

    fun clearEntries() {
        synchronized(this) {
            APApplication.sharedPreferences.edit().remove(PREF_KEY).apply()
        }
        try {
            Natives.suAuditClear()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear kernel audit log", e)
        }
        Log.d(TAG, "Audit log cleared")
    }
}
