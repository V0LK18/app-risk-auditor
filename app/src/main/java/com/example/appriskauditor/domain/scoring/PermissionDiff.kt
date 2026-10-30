package com.example.appriskauditor.domain.scoring

import com.example.appriskauditor.data.history.Snapshot
import com.example.appriskauditor.domain.model.AppInfo
import com.example.appriskauditor.domain.model.Category
import com.example.appriskauditor.domain.model.ProtectionLevel
import com.example.appriskauditor.domain.model.RiskBand
import com.example.appriskauditor.domain.model.RiskResult

object PermissionDiff {

    private val SENSITIVE_CATEGORIES = setOf(
        Category.SPECIAL_ACCESS,
        Category.SMS,
        Category.CONTACTS_CALLS,
        Category.CAMERA_MIC,
        Category.LOCATION,
        Category.PHONE_IDENTITY,
        Category.STORAGE_MEDIA
    )

    fun isSensitivePermission(protectionLevel: ProtectionLevel, category: Category): Boolean {
        return protectionLevel == ProtectionLevel.DANGEROUS ||
                protectionLevel == ProtectionLevel.SPECIAL ||
                category in SENSITIVE_CATEGORIES
    }

    fun extractSensitivePermissions(app: AppInfo): List<String> {
        return app.permissions
            .filter { isSensitivePermission(it.protectionLevel, it.category) }
            .map { it.name }
            .distinct()
            .sorted()
    }

    fun encodeSpecialAccessFlags(app: AppInfo): String {
        val sa = app.specialAccess
        val flags = mutableListOf<String>()
        if (sa.accessibilityEnabled) flags.add("ACCESSIBILITY")
        if (sa.deviceAdminActive) flags.add("DEVICE_ADMIN")
        if (sa.notificationListenerEnabled) flags.add("NOTIFICATION_LISTENER")
        if (sa.overlayGranted == true) flags.add("OVERLAY")
        if (sa.installPackagesAllowed == true) flags.add("INSTALL_PACKAGES")
        if (sa.usageStatsGranted == true) flags.add("USAGE_STATS")
        return flags.joinToString(",")
    }

    fun compute(
        old: Snapshot?,
        new: AppInfo,
        newResult: RiskResult,
        isBaselineScan: Boolean = false
    ): DiffResult {
        if (isBaselineScan) {
            return DiffResult(
                isNewApp = false,
                addedSensitivePermissions = emptyList(),
                removedSensitivePermissions = emptyList(),
                scoreDelta = 0,
                bandIncreased = false,
                specialAccessNewlyEnabled = false,
                oldScore = newResult.score,
                newScore = newResult.score,
                lastScannedTime = new.updateTime
            )
        }

        if (old == null) {
            val newSensitive = extractSensitivePermissions(new)
            return DiffResult(
                isNewApp = true,
                addedSensitivePermissions = newSensitive,
                removedSensitivePermissions = emptyList(),
                scoreDelta = newResult.score,
                bandIncreased = false,
                specialAccessNewlyEnabled = false,
                oldScore = 0,
                newScore = newResult.score,
                lastScannedTime = new.installTime
            )
        }

        val newSensitive = extractSensitivePermissions(new)
        val oldSensitive = old.sensitivePermissions

        val added = (newSensitive - oldSensitive.toSet()).sorted()
        val removed = (oldSensitive - newSensitive.toSet()).sorted()

        val scoreDelta = newResult.score - old.score
        val oldBand = try { RiskBand.valueOf(old.band) } catch (_: Exception) { RiskBand.LOW }
        val bandIncreased = newResult.band.ordinal > oldBand.ordinal

        val oldSpecialFlags = if (old.specialAccessFlags.isBlank()) emptySet() else old.specialAccessFlags.split(",").toSet()
        val newSpecialFlags = if (encodeSpecialAccessFlags(new).isBlank()) emptySet() else encodeSpecialAccessFlags(new).split(",").toSet()
        val specialAccessNewlyEnabled = (newSpecialFlags - oldSpecialFlags).isNotEmpty()

        return DiffResult(
            isNewApp = false,
            addedSensitivePermissions = added,
            removedSensitivePermissions = removed,
            scoreDelta = scoreDelta,
            bandIncreased = bandIncreased,
            specialAccessNewlyEnabled = specialAccessNewlyEnabled,
            oldScore = old.score,
            newScore = newResult.score,
            lastScannedTime = old.lastScanned
        )
    }
}
