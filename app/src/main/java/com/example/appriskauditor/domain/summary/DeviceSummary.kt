package com.example.appriskauditor.domain.summary

import com.example.appriskauditor.domain.model.AppInfo
import com.example.appriskauditor.domain.model.Category
import com.example.appriskauditor.domain.model.RiskBand
import com.example.appriskauditor.domain.model.RiskResult

data class CategorySummary(
    val category: Category,
    val appsWithGrantedCount: Int,
    val appsWithDeclaredOnlyCount: Int,
    val appsWithGrantedPackageNames: Set<String>
)

data class DeviceSummaryResult(
    val totalApps: Int,
    val lowCount: Int,
    val mediumCount: Int,
    val highCount: Int,
    val criticalCount: Int,
    val categorySummaries: List<CategorySummary>
)

object DeviceSummary {

    fun compute(
        apps: List<AppInfo>,
        results: Map<String, RiskResult> = emptyMap()
    ): DeviceSummaryResult {
        val totalApps = apps.size

        var lowCount = 0
        var mediumCount = 0
        var highCount = 0
        var criticalCount = 0

        for (app in apps) {
            val band = results[app.packageName]?.band ?: app.riskResult.band
            when (band) {
                RiskBand.LOW -> lowCount++
                RiskBand.MEDIUM -> mediumCount++
                RiskBand.HIGH -> highCount++
                RiskBand.CRITICAL -> criticalCount++
            }
        }

        val allNineCategories = listOf(
            Category.SPECIAL_ACCESS,
            Category.SMS,
            Category.CONTACTS_CALLS,
            Category.CAMERA_MIC,
            Category.LOCATION,
            Category.PHONE_IDENTITY,
            Category.STORAGE_MEDIA,
            Category.BACKGROUND,
            Category.NETWORK
        )

        val summaries = allNineCategories.map { category ->
            val grantedAppPkgs = mutableSetOf<String>()
            var declaredOnlyCount = 0

            for (app in apps) {
                val (hasGranted, hasDeclared) = evaluateAppForCategory(app, category)
                if (hasGranted) {
                    grantedAppPkgs.add(app.packageName)
                } else if (hasDeclared) {
                    declaredOnlyCount++
                }
            }

            CategorySummary(
                category = category,
                appsWithGrantedCount = grantedAppPkgs.size,
                appsWithDeclaredOnlyCount = declaredOnlyCount,
                appsWithGrantedPackageNames = grantedAppPkgs
            )
        }

        return DeviceSummaryResult(
            totalApps = totalApps,
            lowCount = lowCount,
            mediumCount = mediumCount,
            highCount = highCount,
            criticalCount = criticalCount,
            categorySummaries = summaries
        )
    }

    private fun evaluateAppForCategory(app: AppInfo, category: Category): Pair<Boolean, Boolean> {
        if (category == Category.SPECIAL_ACCESS) {
            val sa = app.specialAccess
            val saActive = sa.accessibilityEnabled ||
                    sa.deviceAdminActive ||
                    sa.notificationListenerEnabled ||
                    sa.overlayGranted == true ||
                    sa.installPackagesAllowed == true ||
                    sa.usageStatsGranted == true

            val saDeclared = sa.accessibilityDeclared ||
                    sa.deviceAdminDeclared ||
                    sa.notificationListenerDeclared ||
                    sa.overlayDeclared ||
                    sa.installPackagesDeclared ||
                    sa.usageStatsDeclared

            val permGranted = app.permissions.any { it.category == Category.SPECIAL_ACCESS && it.granted }
            val permDeclared = app.permissions.any { it.category == Category.SPECIAL_ACCESS }

            val hasGranted = saActive || permGranted
            val hasDeclared = !hasGranted && (saDeclared || permDeclared)
            return Pair(hasGranted, hasDeclared)
        } else {
            val hasGranted = app.permissions.any { it.category == category && it.granted }
            val hasDeclared = !hasGranted && app.permissions.any { it.category == category }
            return Pair(hasGranted, hasDeclared)
        }
    }
}
