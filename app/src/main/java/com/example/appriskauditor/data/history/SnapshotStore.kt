package com.example.appriskauditor.data.history

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class SnapshotStore(context: Context) {

    private val prefs = context.getSharedPreferences("app_snapshots_prefs", Context.MODE_PRIVATE)

    suspend fun hasBaseline(): Boolean = withContext(Dispatchers.IO) {
        prefs.getBoolean("has_baseline", false)
    }

    suspend fun setBaselineCreated() = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean("has_baseline", true).apply()
    }

    suspend fun loadAll(): Map<String, Snapshot> = withContext(Dispatchers.IO) {
        try {
            val jsonString = prefs.getString("snapshots_json", null) ?: return@withContext emptyMap()
            val root = JSONObject(jsonString)
            val result = mutableMapOf<String, Snapshot>()

            val keys = root.keys()
            while (keys.hasNext()) {
                val pkg = keys.next()
                val obj = root.getJSONObject(pkg)

                val sensitivePermsJson = obj.optJSONArray("sensitivePermissions")
                val sensitivePerms = mutableListOf<String>()
                if (sensitivePermsJson != null) {
                    for (i in 0 until sensitivePermsJson.length()) {
                        sensitivePerms.add(sensitivePermsJson.getString(i))
                    }
                }

                val snapshot = Snapshot(
                    packageName = obj.getString("packageName"),
                    versionCode = obj.optLong("versionCode", 0L),
                    versionName = obj.optString("versionName", ""),
                    score = obj.optInt("score", 0),
                    band = obj.optString("band", "LOW"),
                    sensitivePermissions = sensitivePerms,
                    specialAccessFlags = obj.optString("specialAccessFlags", ""),
                    firstSeen = obj.optLong("firstSeen", System.currentTimeMillis()),
                    lastScanned = obj.optLong("lastScanned", System.currentTimeMillis()),
                    lastNotifiedKey = if (obj.has("lastNotifiedKey")) obj.getString("lastNotifiedKey") else null
                )
                result[pkg] = snapshot
            }
            result
        } catch (e: Exception) {
            Log.e("SnapshotStore", "Error loading snapshots, resetting store: ${e.message}")
            prefs.edit().remove("snapshots_json").apply()
            emptyMap()
        }
    }

    suspend fun saveAll(snapshots: Map<String, Snapshot>) = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            for ((pkg, snapshot) in snapshots) {
                val obj = JSONObject()
                obj.put("packageName", snapshot.packageName)
                obj.put("versionCode", snapshot.versionCode)
                obj.put("versionName", snapshot.versionName)
                obj.put("score", snapshot.score)
                obj.put("band", snapshot.band)

                val sensitivePermsJson = JSONArray()
                snapshot.sensitivePermissions.forEach { sensitivePermsJson.put(it) }
                obj.put("sensitivePermissions", sensitivePermsJson)

                obj.put("specialAccessFlags", snapshot.specialAccessFlags)
                obj.put("firstSeen", snapshot.firstSeen)
                obj.put("lastScanned", snapshot.lastScanned)
                if (snapshot.lastNotifiedKey != null) {
                    obj.put("lastNotifiedKey", snapshot.lastNotifiedKey)
                }

                root.put(pkg, obj)
            }
            prefs.edit().putString("snapshots_json", root.toString()).putBoolean("has_baseline", true).apply()
        } catch (e: Exception) {
            Log.e("SnapshotStore", "Error saving snapshots: ${e.message}")
        }
    }

    suspend fun removePackagePermissionForDemo(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val currentSnapshots = loadAll().toMutableMap()
        val snapshot = currentSnapshots[packageName] ?: return@withContext false
        if (snapshot.sensitivePermissions.isEmpty()) return@withContext false

        val newPerms = snapshot.sensitivePermissions.drop(1)
        val updatedSnapshot = snapshot.copy(sensitivePermissions = newPerms)
        currentSnapshots[packageName] = updatedSnapshot

        saveAll(currentSnapshots)
        true
    }
}
