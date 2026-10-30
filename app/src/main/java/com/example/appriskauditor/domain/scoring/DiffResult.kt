package com.example.appriskauditor.domain.scoring

data class DiffResult(
    val isNewApp: Boolean,
    val addedSensitivePermissions: List<String>,
    val removedSensitivePermissions: List<String>,
    val scoreDelta: Int,
    val bandIncreased: Boolean,
    val specialAccessNewlyEnabled: Boolean,
    val oldScore: Int = 0,
    val newScore: Int = 0,
    val lastScannedTime: Long = 0L
) {
    val hasChanges: Boolean
        get() = isNewApp || addedSensitivePermissions.isNotEmpty() || removedSensitivePermissions.isNotEmpty() || scoreDelta != 0 || bandIncreased || specialAccessNewlyEnabled
}
