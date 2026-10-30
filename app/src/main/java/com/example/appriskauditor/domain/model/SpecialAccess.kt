package com.example.appriskauditor.domain.model

data class SpecialAccess(
    val accessibilityDeclared: Boolean,
    val accessibilityEnabled: Boolean,
    val deviceAdminDeclared: Boolean,
    val deviceAdminActive: Boolean,
    val notificationListenerDeclared: Boolean,
    val notificationListenerEnabled: Boolean,
    val overlayDeclared: Boolean,
    val overlayGranted: Boolean?,          // null = could not be determined (unknown)
    val installPackagesDeclared: Boolean,
    val installPackagesAllowed: Boolean?,  // null = could not be determined (unknown)
    val usageStatsDeclared: Boolean,
    val usageStatsGranted: Boolean?        // null = could not be determined (unknown)
)
