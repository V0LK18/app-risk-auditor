package com.example.appriskauditor.data.history

data class Snapshot(
    val packageName: String,
    val versionCode: Long,
    val versionName: String,
    val score: Int,
    val band: String,
    val sensitivePermissions: List<String>,
    val specialAccessFlags: String,
    val firstSeen: Long,
    val lastScanned: Long,
    val lastNotifiedKey: String? = null
)
