package com.example.appriskauditor.domain.model

data class AppInfo(
    val name: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val installTime: Long,
    val updateTime: Long,
    val installer: InstallerSource,
    val installerPackage: String?,
    val targetSdk: Int,
    val isSystem: Boolean,
    val permissions: List<PermissionInfo>,
    val specialAccess: SpecialAccess,
    val riskResult: RiskResult,
    val isDefaultSmsApp: Boolean = false,
    val isDefaultDialerApp: Boolean = false
)
