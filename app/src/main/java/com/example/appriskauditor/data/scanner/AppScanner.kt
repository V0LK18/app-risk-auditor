package com.example.appriskauditor.data.scanner

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import android.telecom.TelecomManager
import android.util.Log
import com.example.appriskauditor.domain.PermissionCatalog
import com.example.appriskauditor.domain.model.AppInfo
import com.example.appriskauditor.domain.model.InstallerSource
import com.example.appriskauditor.domain.model.PermissionInfo
import com.example.appriskauditor.domain.model.ProtectionLevel
import com.example.appriskauditor.domain.model.RiskBand
import com.example.appriskauditor.domain.model.RiskResult
import com.example.appriskauditor.domain.scoring.RiskEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppScanner(private val context: Context) {

    private val specialAccessDetector = SpecialAccessDetector(context)

    suspend fun scan(includeSystem: Boolean = false): List<AppInfo> = withContext(Dispatchers.Default) {
        val pm = context.packageManager
        val packages = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }
        } catch (_: Exception) {
            emptyList()
        }

        // Query default apps
        val defaultSmsPackage = try {
            Telephony.Sms.getDefaultSmsPackage(context)
        } catch (_: Exception) {
            null
        }

        val defaultDialerPackage = try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            telecomManager?.getDefaultDialerPackage()
        } catch (_: Exception) {
            null
        }

        // Compute special access sets ONCE per scan
        val precomputedSets = specialAccessDetector.precompute()

        val result = mutableListOf<AppInfo>()

        for (pkgInfo in packages) {
            try {
                val appInfo = pkgInfo.applicationInfo ?: continue
                val flags = appInfo.flags
                val isSystemApp = (flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystemApp = (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

                if (!includeSystem && isSystemApp && !isUpdatedSystemApp) {
                    continue
                }

                val appName = try {
                    pm.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    pkgInfo.packageName
                }

                val versionName = pkgInfo.versionName ?: ""
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pkgInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pkgInfo.versionCode.toLong()
                }

                val sourceDir = appInfo.sourceDir ?: ""
                val isNonDataApp = sourceDir.isNotEmpty() && !sourceDir.startsWith("/data/app")
                val isPreloadDataApp = sourceDir.startsWith("/data/app/") && !sourceDir.contains("~~")
                val isPlatformSigned = try {
                    @Suppress("DEPRECATION")
                    pm.checkSignatures(pkgInfo.packageName, "android") == PackageManager.SIGNATURE_MATCH
                } catch (_: Exception) {
                    false
                }

                val isPreinstalled = isSystemApp || isUpdatedSystemApp || isNonDataApp || isPreloadDataApp || isPlatformSigned

                val installerPkg = getInstallerPackage(pm, pkgInfo.packageName)
                val installerSource = when {
                    installerPkg == "com.android.vending" -> InstallerSource.PLAY_STORE
                    !installerPkg.isNullOrBlank() -> InstallerSource.OTHER_STORE
                    isPreinstalled -> InstallerSource.SYSTEM_PREINSTALLED
                    else -> InstallerSource.SIDELOADED_OR_UNKNOWN
                }

                if (installerSource == InstallerSource.SIDELOADED_OR_UNKNOWN) {
                    Log.d(
                        "InstallerClass",
                        "Package: ${pkgInfo.packageName}, sourceDir: $sourceDir, flags: $flags, installerPackage: $installerPkg, classification: $installerSource"
                    )
                }

                val permissionsList = mutableListOf<PermissionInfo>()
                val reqPermissions = pkgInfo.requestedPermissions
                val reqFlags = pkgInfo.requestedPermissionsFlags

                if (reqPermissions != null && reqFlags != null) {
                    val count = minOf(reqPermissions.size, reqFlags.size)
                    for (i in 0 until count) {
                        val permName = reqPermissions[i]
                        val permFlag = reqFlags[i]
                        val isGranted = (permFlag and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0

                        val protLevel = getProtectionLevel(pm, permName)
                        val spec = PermissionCatalog.getPermissionSpec(permName)

                        permissionsList.add(
                            PermissionInfo(
                                name = permName,
                                granted = isGranted,
                                protectionLevel = protLevel,
                                category = spec.category,
                                description = spec.description
                            )
                        )
                    }
                }

                val specialAccess = specialAccessDetector.detectForApp(pkgInfo, precomputedSets)

                val isDefaultSms = pkgInfo.packageName == defaultSmsPackage
                val isDefaultDialer = pkgInfo.packageName == defaultDialerPackage

                val placeholderAppInfo = AppInfo(
                    name = appName,
                    packageName = pkgInfo.packageName,
                    versionName = versionName,
                    versionCode = versionCode,
                    installTime = pkgInfo.firstInstallTime,
                    updateTime = pkgInfo.lastUpdateTime,
                    installer = installerSource,
                    installerPackage = installerPkg,
                    targetSdk = appInfo.targetSdkVersion,
                    isSystem = isSystemApp,
                    permissions = permissionsList,
                    specialAccess = specialAccess,
                    riskResult = RiskResult(0, RiskBand.LOW, emptyList()),
                    isDefaultSmsApp = isDefaultSms,
                    isDefaultDialerApp = isDefaultDialer
                )

                val computedRisk = RiskEngine.score(placeholderAppInfo)
                val parsedAppInfo = placeholderAppInfo.copy(riskResult = computedRisk)

                result.add(parsedAppInfo)
            } catch (_: Exception) {
                // Skip bad or unreadable package
                continue
            }
        }

        result.sortByDescending { it.riskResult.score }

        val top10 = result.take(10)
        for (app in top10) {
            Log.d(
                "RiskScore",
                "App: ${app.name} (${app.packageName}) - Score: ${app.riskResult.score}, Band: ${app.riskResult.band}, Reasons: ${app.riskResult.reasons.map { "${it.text} (${it.points}pts)" }}"
            )
        }

        result
    }

    private fun getInstallerPackage(pm: PackageManager, packageName: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(packageName)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getProtectionLevel(pm: PackageManager, permissionName: String): ProtectionLevel {
        if (PermissionCatalog.isSpecialPermission(permissionName)) {
            return ProtectionLevel.SPECIAL
        }

        return try {
            @Suppress("DEPRECATION")
            val permInfo = pm.getPermissionInfo(permissionName, 0)

            @Suppress("DEPRECATION")
            val base = permInfo.protectionLevel and android.content.pm.PermissionInfo.PROTECTION_MASK_BASE
            when (base) {
                android.content.pm.PermissionInfo.PROTECTION_NORMAL -> ProtectionLevel.NORMAL
                android.content.pm.PermissionInfo.PROTECTION_DANGEROUS -> ProtectionLevel.DANGEROUS
                android.content.pm.PermissionInfo.PROTECTION_SIGNATURE -> ProtectionLevel.SIGNATURE
                else -> ProtectionLevel.UNKNOWN
            }
        } catch (_: Exception) {
            ProtectionLevel.UNKNOWN
        }
    }
}
