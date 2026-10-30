package com.example.appriskauditor.domain.scoring

object RiskWeights {
    // Category Caps
    const val CAP_SPECIAL_ACCESS = 45
    const val CAP_RUNTIME_PERMISSIONS = 35
    const val CAP_SMS = 25
    const val CAP_CONTACTS_CALLS = 15
    const val CAP_CAMERA_MIC = 10
    const val CAP_LOCATION = 12
    const val CAP_PHONE_IDENTITY = 8
    const val CAP_STORAGE_MEDIA = 12
    const val CAP_MEDIA_PERMISSIONS_SUBCAP = 8
    const val CAP_BACKGROUND = 8

    // SPECIAL_ACCESS Weights
    const val ACCESSIBILITY_ENABLED = 30
    const val ACCESSIBILITY_DECLARED = 10
    const val DEVICE_ADMIN_ACTIVE = 25
    const val NOTIFICATION_LISTENER_ENABLED = 20
    const val OVERLAY_GRANTED_STRONG = 20
    const val OVERLAY_GRANTED_WEAK = 8
    const val OVERLAY_DECLARED_OR_UNKNOWN = 8
    const val REQUEST_INSTALL_PACKAGES_DECLARED_SIDELOADED = 15
    const val REQUEST_INSTALL_PACKAGES_DECLARED_NORMAL = 5
    const val PACKAGE_USAGE_STATS_GRANTED = 10
    const val PACKAGE_USAGE_STATS_DECLARED = 5

    // SMS Weights
    const val READ_SMS_GRANTED = 15
    const val RECEIVE_SMS_GRANTED = 15
    const val SEND_SMS_GRANTED = 10

    // CONTACTS_CALLS Weights
    const val READ_CALL_LOG_GRANTED = 10
    const val READ_CONTACTS_GRANTED = 8

    // CAMERA_MIC Weights
    const val CAMERA_GRANTED = 6
    const val RECORD_AUDIO_GRANTED = 6

    // LOCATION Weights
    const val FINE_LOCATION_GRANTED = 5
    const val BACKGROUND_LOCATION_GRANTED = 8

    // PHONE_IDENTITY Weights
    const val READ_PHONE_STATE_GRANTED = 3
    const val READ_PHONE_NUMBERS_GRANTED = 3
    const val CALL_PHONE_GRANTED = 4

    // STORAGE_MEDIA Weights
    const val MANAGE_EXTERNAL_STORAGE_GRANTED = 12
    const val READ_MEDIA_IMAGES_GRANTED = 3
    const val READ_MEDIA_VIDEO_GRANTED = 3
    const val READ_MEDIA_AUDIO_GRANTED = 3
    const val READ_EXTERNAL_STORAGE_GRANTED = 3

    // BACKGROUND Weights
    const val RECEIVE_BOOT_COMPLETED_GRANTED = 3
    const val FOREGROUND_SERVICE_GRANTED = 2
    const val REQUEST_IGNORE_BATTERY_OPTIMIZATIONS_GRANTED = 4

    // Ungranted Dangerous Permission Factor
    const val UNGRANTED_DANGEROUS_FACTOR = 0.25

    // Installer Multipliers
    const val MULTIPLIER_SIDELOADED = 1.25
    const val MULTIPLIER_STORE = 1.0
    const val MULTIPLIER_PREINSTALLED = 0.5
    const val MULTIPLIER_KNOWN_PUBLISHER = 0.6

    // Combination Bonuses
    const val BONUS_ACCESSIBILITY_OVERLAY = 20
    const val BONUS_SMS_INTERNET_BOOT_SIDELOADED = 25
    const val BONUS_SMS_NOTIFICATION_LISTENER = 15
    const val BONUS_NON_SECURITY_DEVICE_ADMIN = 20
    const val BONUS_INSTALL_PACKAGES_SIDELOADED = 10

    // Context Mismatch Bonuses
    const val BONUS_CONTEXT_MISMATCH_NORMAL = 20
    const val BONUS_CONTEXT_MISMATCH_HIGH = 25

    // Known Publishers Allowlist
    val KNOWN_PUBLISHERS = setOf(
        "com.google.android.",
        "com.android.",
        "com.whatsapp",
        "org.telegram.messenger",
        "com.microsoft.",
        "com.facebook.",
        "com.instagram.android",
        "com.samsung.",
        "com.oplus.",
        "com.heytap.",
        "com.coloros.",
        "com.realme."
    )

    // Device Admin Security Allowlist
    val DEVICE_ADMIN_ALLOWLIST = setOf(
        "com.google.android.apps.work.clouddpc",
        "com.microsoft.intune",
        "com.google.android.gms",
        "com.samsung.knox",
        "com.airwatch.androidagent",
        "com.mobileiron"
    )
}
