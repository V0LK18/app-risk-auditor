package com.example.appriskauditor.data.scanner

import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.appriskauditor.domain.model.SpecialAccess

class SpecialAccessDetector(private val context: Context) {

    data class PrecomputedSets(
        val enabledAccessibilityPackages: Set<String>,
        val declaredAccessibilityPackages: Set<String>,
        val activeDeviceAdminPackages: Set<String>,
        val declaredDeviceAdminPackages: Set<String>,
        val enabledNotificationListenerPackages: Set<String>,
        val declaredNotificationListenerPackages: Set<String>
    )

    fun precompute(): PrecomputedSets {
        val pm = context.packageManager
        val enabledAccessibility = getEnabledAccessibilityPackages()
        val declaredAccessibility = getDeclaredAccessibilityPackages(pm)
        val activeDeviceAdmin = getActiveDeviceAdminPackages()
        val declaredDeviceAdmin = getDeclaredDeviceAdminPackages(pm)
        val enabledNotificationListener = getEnabledNotificationListenerPackages()
        val declaredNotificationListener = getDeclaredNotificationListenerPackages(pm)

        Log.d(
            "SpecialAccess",
            "Enabled Accessibility: $enabledAccessibility, Active Admin: $activeDeviceAdmin, Enabled Listener: $enabledNotificationListener"
        )

        return PrecomputedSets(
            enabledAccessibilityPackages = enabledAccessibility,
            declaredAccessibilityPackages = declaredAccessibility,
            activeDeviceAdminPackages = activeDeviceAdmin,
            declaredDeviceAdminPackages = declaredDeviceAdmin,
            enabledNotificationListenerPackages = enabledNotificationListener,
            declaredNotificationListenerPackages = declaredNotificationListener
        )
    }

    fun detectForApp(
        pkgInfo: PackageInfo,
        precomputed: PrecomputedSets
    ): SpecialAccess {
        val packageName = pkgInfo.packageName
        val appInfo = pkgInfo.applicationInfo
        val uid = appInfo?.uid ?: 0
        val reqPermissions = pkgInfo.requestedPermissions?.toSet() ?: emptySet()

        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager

        val overlayDeclared = "android.permission.SYSTEM_ALERT_WINDOW" in reqPermissions
        val overlayGranted = if (overlayDeclared && appOpsManager != null && appInfo != null) {
            checkAppOp(appOpsManager, "android:system_alert_window", uid, packageName)
        } else if (!overlayDeclared) {
            false
        } else null

        val installPackagesDeclared = "android.permission.REQUEST_INSTALL_PACKAGES" in reqPermissions
        val installPackagesAllowed = if (installPackagesDeclared && appOpsManager != null && appInfo != null) {
            checkAppOp(appOpsManager, "android:request_install_packages", uid, packageName)
        } else if (!installPackagesDeclared) {
            false
        } else null

        val usageStatsDeclared = "android.permission.PACKAGE_USAGE_STATS" in reqPermissions
        val usageStatsGranted = if (usageStatsDeclared && appOpsManager != null && appInfo != null) {
            checkAppOp(appOpsManager, "android:get_usage_stats", uid, packageName)
        } else if (!usageStatsDeclared) {
            false
        } else null

        return SpecialAccess(
            accessibilityDeclared = packageName in precomputed.declaredAccessibilityPackages,
            accessibilityEnabled = packageName in precomputed.enabledAccessibilityPackages,
            deviceAdminDeclared = packageName in precomputed.declaredDeviceAdminPackages,
            deviceAdminActive = packageName in precomputed.activeDeviceAdminPackages,
            notificationListenerDeclared = packageName in precomputed.declaredNotificationListenerPackages,
            notificationListenerEnabled = packageName in precomputed.enabledNotificationListenerPackages,
            overlayDeclared = overlayDeclared,
            overlayGranted = overlayGranted,
            installPackagesDeclared = installPackagesDeclared,
            installPackagesAllowed = installPackagesAllowed,
            usageStatsDeclared = usageStatsDeclared,
            usageStatsGranted = usageStatsGranted
        )
    }

    private fun getEnabledAccessibilityPackages(): Set<String> {
        return try {
            val setting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (setting.isNullOrBlank()) {
                emptySet()
            } else {
                setting.split(":")
                    .mapNotNull { ComponentName.unflattenFromString(it.trim())?.packageName }
                    .toSet()
            }
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun getDeclaredAccessibilityPackages(pm: PackageManager): Set<String> {
        return try {
            val intent = Intent("android.accessibilityservice.AccessibilityService")
            val flags = PackageManager.MATCH_DISABLED_COMPONENTS
            val services = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentServices(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentServices(intent, flags)
            }
            services.mapNotNull { it.serviceInfo?.packageName }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun getActiveDeviceAdminPackages(): Set<String> {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            val activeAdmins = dpm?.activeAdmins
            activeAdmins?.mapNotNull { it.packageName }?.toSet() ?: emptySet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun getDeclaredDeviceAdminPackages(pm: PackageManager): Set<String> {
        return try {
            val intent = Intent(android.app.admin.DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED)
            val flags = PackageManager.MATCH_DISABLED_COMPONENTS
            val receivers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryBroadcastReceivers(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.queryBroadcastReceivers(intent, flags)
            }
            receivers.mapNotNull { resolveInfo ->
                val receiverInfo = resolveInfo.activityInfo
                if (receiverInfo?.permission == android.Manifest.permission.BIND_DEVICE_ADMIN) {
                    receiverInfo.packageName
                } else null
            }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun getEnabledNotificationListenerPackages(): Set<String> {
        return try {
            NotificationManagerCompat.getEnabledListenerPackages(context)
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun getDeclaredNotificationListenerPackages(pm: PackageManager): Set<String> {
        return try {
            val intent = Intent("android.service.notification.NotificationListenerService")
            val flags = PackageManager.MATCH_DISABLED_COMPONENTS
            val services = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentServices(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentServices(intent, flags)
            }
            services.mapNotNull { it.serviceInfo?.packageName }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun checkAppOp(
        appOpsManager: AppOpsManager,
        opString: String,
        uid: Int,
        packageName: String
    ): Boolean? {
        // null means state could not be determined (unknown)
        return try {
            @Suppress("DEPRECATION")
            val mode = appOpsManager.checkOpNoThrow(opString, uid, packageName)
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            null // null indicates unknown
        }
    }
}
