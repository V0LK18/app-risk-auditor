package com.example.appriskauditor.domain

import com.example.appriskauditor.domain.model.Category

data class PermissionSpec(
    val category: Category,
    val description: String
)

object PermissionCatalog {

    val map: Map<String, PermissionSpec> = mapOf(
        // SPECIAL_ACCESS
        "android.permission.SYSTEM_ALERT_WINDOW" to PermissionSpec(
            Category.SPECIAL_ACCESS,
            "Allows drawing over other apps or screens"
        ),
        "android.permission.BIND_ACCESSIBILITY_SERVICE" to PermissionSpec(
            Category.SPECIAL_ACCESS,
            "Allows full user interface access and control via accessibility"
        ),
        "android.permission.BIND_DEVICE_ADMIN" to PermissionSpec(
            Category.SPECIAL_ACCESS,
            "Allows acting as a system device administrator"
        ),
        "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" to PermissionSpec(
            Category.SPECIAL_ACCESS,
            "Allows reading and interacting with all system notifications"
        ),
        "android.permission.REQUEST_INSTALL_PACKAGES" to PermissionSpec(
            Category.SPECIAL_ACCESS,
            "Allows requesting the installation of new app packages"
        ),
        "android.permission.PACKAGE_USAGE_STATS" to PermissionSpec(
            Category.SPECIAL_ACCESS,
            "Allows tracking app usage history and statistics"
        ),

        // SMS
        "android.permission.READ_SMS" to PermissionSpec(
            Category.SMS,
            "Can read your text messages"
        ),
        "android.permission.RECEIVE_SMS" to PermissionSpec(
            Category.SMS,
            "Can receive incoming text messages"
        ),
        "android.permission.SEND_SMS" to PermissionSpec(
            Category.SMS,
            "Can send text messages on your behalf"
        ),

        // CONTACTS_CALLS
        "android.permission.READ_CONTACTS" to PermissionSpec(
            Category.CONTACTS_CALLS,
            "Can access your saved contacts list"
        ),
        "android.permission.READ_CALL_LOG" to PermissionSpec(
            Category.CONTACTS_CALLS,
            "Can access your phone call history"
        ),

        // CAMERA_MIC
        "android.permission.CAMERA" to PermissionSpec(
            Category.CAMERA_MIC,
            "Can take photos and record videos using the camera"
        ),
        "android.permission.RECORD_AUDIO" to PermissionSpec(
            Category.CAMERA_MIC,
            "Can record sound using the device microphone"
        ),

        // LOCATION
        "android.permission.ACCESS_FINE_LOCATION" to PermissionSpec(
            Category.LOCATION,
            "Can access your precise GPS location"
        ),
        "android.permission.ACCESS_COARSE_LOCATION" to PermissionSpec(
            Category.LOCATION,
            "Can access your approximate location"
        ),
        "android.permission.ACCESS_BACKGROUND_LOCATION" to PermissionSpec(
            Category.LOCATION,
            "Can access your location even when the app is in the background"
        ),

        // PHONE_IDENTITY
        "android.permission.READ_PHONE_STATE" to PermissionSpec(
            Category.PHONE_IDENTITY,
            "Can access phone state and unique device identifiers"
        ),
        "android.permission.READ_PHONE_NUMBERS" to PermissionSpec(
            Category.PHONE_IDENTITY,
            "Can access phone numbers assigned to the device"
        ),
        "android.permission.CALL_PHONE" to PermissionSpec(
            Category.PHONE_IDENTITY,
            "Can initiate phone calls directly without user interaction"
        ),

        // STORAGE_MEDIA
        "android.permission.READ_MEDIA_IMAGES" to PermissionSpec(
            Category.STORAGE_MEDIA,
            "Can access photos and images on your device"
        ),
        "android.permission.READ_MEDIA_VIDEO" to PermissionSpec(
            Category.STORAGE_MEDIA,
            "Can access videos stored on your device"
        ),
        "android.permission.READ_MEDIA_AUDIO" to PermissionSpec(
            Category.STORAGE_MEDIA,
            "Can access audio files stored on your device"
        ),
        "android.permission.READ_EXTERNAL_STORAGE" to PermissionSpec(
            Category.STORAGE_MEDIA,
            "Can read files from external shared storage"
        ),
        "android.permission.MANAGE_EXTERNAL_STORAGE" to PermissionSpec(
            Category.STORAGE_MEDIA,
            "Can manage and modify all files on shared storage"
        ),

        // BACKGROUND
        "android.permission.RECEIVE_BOOT_COMPLETED" to PermissionSpec(
            Category.BACKGROUND,
            "Can automatically launch tasks when the device finishes booting"
        ),
        "android.permission.FOREGROUND_SERVICE" to PermissionSpec(
            Category.BACKGROUND,
            "Can run active tasks continuously in the background"
        ),
        "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" to PermissionSpec(
            Category.BACKGROUND,
            "Can request to bypass battery saving restrictions"
        ),

        // NETWORK
        "android.permission.INTERNET" to PermissionSpec(
            Category.NETWORK,
            "Can connect to the internet and transfer network data"
        ),
        "android.permission.ACCESS_NETWORK_STATE" to PermissionSpec(
            Category.NETWORK,
            "Can view details about current network connections"
        )
    )

    fun getPermissionSpec(permissionName: String): PermissionSpec {
        return map[permissionName] ?: PermissionSpec(
            category = Category.OTHER,
            description = "Requested permission for app functionality"
        )
    }

    fun isSpecialPermission(permissionName: String): Boolean {
        return permissionName in SPECIAL_PROTECTION_PERMISSIONS
    }

    private val SPECIAL_PROTECTION_PERMISSIONS = setOf(
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.PACKAGE_USAGE_STATS"
    )
}
