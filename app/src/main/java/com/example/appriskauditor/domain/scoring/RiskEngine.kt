package com.example.appriskauditor.domain.scoring

import com.example.appriskauditor.domain.model.AppInfo
import com.example.appriskauditor.domain.model.Category
import com.example.appriskauditor.domain.model.InstallerSource
import com.example.appriskauditor.domain.model.Reason
import com.example.appriskauditor.domain.model.RiskBand
import com.example.appriskauditor.domain.model.RiskResult
import kotlin.math.min
import kotlin.math.roundToInt

object RiskEngine {

    fun score(app: AppInfo): RiskResult {
        val reasons = mutableListOf<Reason>()

        val sa = app.specialAccess
        val isSideloaded = app.installer == InstallerSource.SIDELOADED_OR_UNKNOWN

        val hasReadSms = app.permissions.any { it.name.endsWith("READ_SMS") && it.granted }
        val hasGrantedSms = app.permissions.any { (it.name.endsWith("READ_SMS") || it.name.endsWith("RECEIVE_SMS") || it.name.endsWith("SEND_SMS")) && it.granted }
        val hasGrantedContactsCallLog = app.permissions.any { (it.name.endsWith("READ_CONTACTS") || it.name.endsWith("READ_CALL_LOG")) && it.granted }
        val hasGrantedMic = app.permissions.any { it.name.endsWith("RECORD_AUDIO") && it.granted }
        val hasInternet = app.permissions.any { it.name.endsWith("INTERNET") && it.granted }
        val hasBoot = app.permissions.any { it.name.endsWith("RECEIVE_BOOT_COMPLETED") && it.granted }

        val hasStrongSignal = evaluateStrongSignal(
            app = app,
            hasReadSms = hasReadSms,
            hasInternet = hasInternet,
            hasBoot = hasBoot,
            hasGrantedSms = hasGrantedSms,
            hasGrantedContactsCallLog = hasGrantedContactsCallLog,
            hasGrantedMic = hasGrantedMic,
            isSideloaded = isSideloaded
        )

        // 1. SPECIAL_ACCESS
        var specialPts = 0.0

        if (sa.accessibilityEnabled) {
            val pts = RiskWeights.ACCESSIBILITY_ENABLED
            specialPts += pts
            reasons.add(
                Reason(
                    text = "Accessibility service is switched ON: this app can read everything on your screen and perform taps",
                    points = pts,
                    category = Category.SPECIAL_ACCESS
                )
            )
        } else if (sa.accessibilityDeclared) {
            val pts = RiskWeights.ACCESSIBILITY_DECLARED
            specialPts += pts
            reasons.add(
                Reason(
                    text = "App requests accessibility service access",
                    points = pts,
                    category = Category.SPECIAL_ACCESS
                )
            )
        }

        if (sa.deviceAdminActive) {
            val pts = RiskWeights.DEVICE_ADMIN_ACTIVE
            specialPts += pts
            reasons.add(
                Reason(
                    text = "Device administrator mode is ACTIVE: gives app deep system management privileges",
                    points = pts,
                    category = Category.SPECIAL_ACCESS
                )
            )
        }

        if (sa.notificationListenerEnabled) {
            val pts = RiskWeights.NOTIFICATION_LISTENER_ENABLED
            specialPts += pts
            reasons.add(
                Reason(
                    text = "Notification listener is ENABLED: app can read all incoming notifications and 2FA codes",
                    points = pts,
                    category = Category.SPECIAL_ACCESS
                )
            )
        }

        if (sa.overlayGranted == true) {
            val pts = if (hasStrongSignal) RiskWeights.OVERLAY_GRANTED_STRONG else RiskWeights.OVERLAY_GRANTED_WEAK
            specialPts += pts
            reasons.add(
                Reason(
                    text = "Screen overlay permission granted: app can draw over other apps",
                    points = pts,
                    category = Category.SPECIAL_ACCESS
                )
            )
        } else if (sa.overlayDeclared) {
            val pts = RiskWeights.OVERLAY_DECLARED_OR_UNKNOWN
            specialPts += pts
            reasons.add(
                Reason(
                    text = "App requests permission to draw over other apps",
                    points = pts,
                    category = Category.SPECIAL_ACCESS
                )
            )
        }

        if (sa.installPackagesDeclared) {
            val pts = if (isSideloaded) RiskWeights.REQUEST_INSTALL_PACKAGES_DECLARED_SIDELOADED else RiskWeights.REQUEST_INSTALL_PACKAGES_DECLARED_NORMAL
            specialPts += pts
            reasons.add(
                Reason(
                    text = "App can request installation of unknown app packages",
                    points = pts,
                    category = Category.SPECIAL_ACCESS
                )
            )
        }

        if (sa.usageStatsGranted == true) {
            val pts = RiskWeights.PACKAGE_USAGE_STATS_GRANTED
            specialPts += pts
            reasons.add(
                Reason(
                    text = "Usage stats access granted: app can track your application usage history",
                    points = pts,
                    category = Category.SPECIAL_ACCESS
                )
            )
        } else if (sa.usageStatsDeclared) {
            val pts = RiskWeights.PACKAGE_USAGE_STATS_DECLARED
            specialPts += pts
            reasons.add(
                Reason(
                    text = "App requests access to track usage statistics",
                    points = pts,
                    category = Category.SPECIAL_ACCESS
                )
            )
        }

        val cappedSpecialPts = min(specialPts, RiskWeights.CAP_SPECIAL_ACCESS.toDouble())

        // Helper for permission checks
        fun evaluatePermission(
            permNameEnd: String,
            fullPts: Int,
            category: Category,
            grantedText: String,
            declaredText: String
        ): Double {
            val perm = app.permissions.firstOrNull { it.name.endsWith(permNameEnd) } ?: return 0.0
            return if (perm.granted) {
                reasons.add(Reason(grantedText, fullPts, category))
                fullPts.toDouble()
            } else {
                val pts = (fullPts * RiskWeights.UNGRANTED_DANGEROUS_FACTOR).roundToInt()
                reasons.add(Reason(declaredText, pts, category))
                fullPts * RiskWeights.UNGRANTED_DANGEROUS_FACTOR
            }
        }

        // 2. SMS
        var smsPts = 0.0
        if (app.isDefaultSmsApp) {
            reasons.add(
                Reason(
                    text = "Default Messages app: it needs SMS access to work (no risk added)",
                    points = 0,
                    category = Category.SMS
                )
            )
        } else {
            smsPts += evaluatePermission("READ_SMS", RiskWeights.READ_SMS_GRANTED, Category.SMS, "Can read your SMS text messages", "Requests permission to read SMS messages")
            smsPts += evaluatePermission("RECEIVE_SMS", RiskWeights.RECEIVE_SMS_GRANTED, Category.SMS, "Can receive incoming text messages", "Requests permission to receive SMS messages")
            smsPts += evaluatePermission("SEND_SMS", RiskWeights.SEND_SMS_GRANTED, Category.SMS, "Can send text messages on your behalf", "Requests permission to send SMS messages")
        }
        val cappedSmsPts = min(smsPts, RiskWeights.CAP_SMS.toDouble())

        // 3. CONTACTS_CALLS
        var contactsPts = 0.0
        if (app.isDefaultDialerApp) {
            reasons.add(
                Reason(
                    text = "Default Phone app: it needs call and contact access to work (no risk added)",
                    points = 0,
                    category = Category.CONTACTS_CALLS
                )
            )
        } else {
            contactsPts += evaluatePermission("READ_CALL_LOG", RiskWeights.READ_CALL_LOG_GRANTED, Category.CONTACTS_CALLS, "Can access your call log history", "Requests permission to access call logs")
            contactsPts += evaluatePermission("READ_CONTACTS", RiskWeights.READ_CONTACTS_GRANTED, Category.CONTACTS_CALLS, "Can access your saved contacts list", "Requests permission to access contacts")
        }
        val cappedContactsPts = min(contactsPts, RiskWeights.CAP_CONTACTS_CALLS.toDouble())

        // 4. CAMERA_MIC
        var cameraMicPts = 0.0
        cameraMicPts += evaluatePermission("CAMERA", RiskWeights.CAMERA_GRANTED, Category.CAMERA_MIC, "Can take photos and record videos using camera", "Requests camera permission")
        cameraMicPts += evaluatePermission("RECORD_AUDIO", RiskWeights.RECORD_AUDIO_GRANTED, Category.CAMERA_MIC, "Can record audio using the microphone", "Requests microphone permission")
        val cappedCameraMicPts = min(cameraMicPts, RiskWeights.CAP_CAMERA_MIC.toDouble())

        // 5. LOCATION
        var locationPts = 0.0
        locationPts += evaluatePermission("ACCESS_FINE_LOCATION", RiskWeights.FINE_LOCATION_GRANTED, Category.LOCATION, "Can access your precise GPS location", "Requests precise location permission")
        locationPts += evaluatePermission("ACCESS_BACKGROUND_LOCATION", RiskWeights.BACKGROUND_LOCATION_GRANTED, Category.LOCATION, "Can access your location in the background", "Requests background location permission")
        val cappedLocationPts = min(locationPts, RiskWeights.CAP_LOCATION.toDouble())

        // 6. PHONE_IDENTITY
        var phonePts = 0.0
        if (!app.isDefaultDialerApp) {
            phonePts += evaluatePermission("READ_PHONE_STATE", RiskWeights.READ_PHONE_STATE_GRANTED, Category.PHONE_IDENTITY, "Can access phone state and device ID", "Requests phone state access")
            phonePts += evaluatePermission("READ_PHONE_NUMBERS", RiskWeights.READ_PHONE_NUMBERS_GRANTED, Category.PHONE_IDENTITY, "Can access your phone numbers", "Requests phone numbers access")
            phonePts += evaluatePermission("CALL_PHONE", RiskWeights.CALL_PHONE_GRANTED, Category.PHONE_IDENTITY, "Can initiate direct phone calls", "Requests direct phone calling capability")
        }
        val cappedPhonePts = min(phonePts, RiskWeights.CAP_PHONE_IDENTITY.toDouble())

        // 7. STORAGE_MEDIA
        val manageStoragePts = evaluatePermission("MANAGE_EXTERNAL_STORAGE", RiskWeights.MANAGE_EXTERNAL_STORAGE_GRANTED, Category.STORAGE_MEDIA, "Can manage and access all files on external storage", "Requests full storage management access")
        var mediaPts = 0.0
        mediaPts += evaluatePermission("READ_MEDIA_IMAGES", RiskWeights.READ_MEDIA_IMAGES_GRANTED, Category.STORAGE_MEDIA, "Can access photos and images", "Requests photo access")
        mediaPts += evaluatePermission("READ_MEDIA_VIDEO", RiskWeights.READ_MEDIA_VIDEO_GRANTED, Category.STORAGE_MEDIA, "Can access videos", "Requests video access")
        mediaPts += evaluatePermission("READ_MEDIA_AUDIO", RiskWeights.READ_MEDIA_AUDIO_GRANTED, Category.STORAGE_MEDIA, "Can access audio files", "Requests audio access")
        mediaPts += evaluatePermission("READ_EXTERNAL_STORAGE", RiskWeights.READ_EXTERNAL_STORAGE_GRANTED, Category.STORAGE_MEDIA, "Can read files from external storage", "Requests external storage reading access")
        val cappedMediaPts = min(mediaPts, RiskWeights.CAP_MEDIA_PERMISSIONS_SUBCAP.toDouble())
        val cappedStoragePts = min(manageStoragePts + cappedMediaPts, RiskWeights.CAP_STORAGE_MEDIA.toDouble())

        // 8. BACKGROUND
        var backgroundPts = 0.0
        backgroundPts += evaluatePermission("RECEIVE_BOOT_COMPLETED", RiskWeights.RECEIVE_BOOT_COMPLETED_GRANTED, Category.BACKGROUND, "Can start automatically when device boots", "Requests boot completion listener")
        backgroundPts += evaluatePermission("FOREGROUND_SERVICE", RiskWeights.FOREGROUND_SERVICE_GRANTED, Category.BACKGROUND, "Can run active foreground background services", "Requests foreground service capability")
        backgroundPts += evaluatePermission("REQUEST_IGNORE_BATTERY_OPTIMIZATIONS", RiskWeights.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS_GRANTED, Category.BACKGROUND, "Can bypass battery optimization restrictions", "Requests battery optimization bypass")
        val cappedBackgroundPts = min(backgroundPts, RiskWeights.CAP_BACKGROUND.toDouble())

        // Cap all plain runtime categories combined at 35
        val sumRuntimeCapped = cappedSmsPts + cappedContactsPts + cappedCameraMicPts +
                cappedLocationPts + cappedPhonePts + cappedStoragePts + cappedBackgroundPts
        val cappedRuntimeTotal = min(sumRuntimeCapped, RiskWeights.CAP_RUNTIME_PERMISSIONS.toDouble())

        val baseSubtotal = cappedSpecialPts + cappedRuntimeTotal

        // Multipliers
        val installerMultiplier = when (app.installer) {
            InstallerSource.SIDELOADED_OR_UNKNOWN -> RiskWeights.MULTIPLIER_SIDELOADED
            InstallerSource.SYSTEM_PREINSTALLED -> RiskWeights.MULTIPLIER_PREINSTALLED
            InstallerSource.PLAY_STORE, InstallerSource.OTHER_STORE -> RiskWeights.MULTIPLIER_STORE
        }

        val isKnownPublisher = RiskWeights.KNOWN_PUBLISHERS.any { app.packageName.lowercase().startsWith(it) }
        val isTrustedSource = app.installer == InstallerSource.PLAY_STORE || app.installer == InstallerSource.SYSTEM_PREINSTALLED

        val publisherMultiplier = if (isKnownPublisher && isTrustedSource) {
            reasons.add(Reason("From a well-known publisher installed through a trusted source", 0, Category.OTHER))
            RiskWeights.MULTIPLIER_KNOWN_PUBLISHER
        } else {
            1.0
        }

        val multipliedSubtotal = baseSubtotal * installerMultiplier * publisherMultiplier

        // Combination Bonuses
        var comboBonusPts = 0

        val hasAccessCombo = sa.accessibilityEnabled || sa.accessibilityDeclared
        val hasOverlayCombo = (sa.overlayGranted == true) || sa.overlayDeclared
        if (hasAccessCombo && hasOverlayCombo) {
            comboBonusPts += RiskWeights.BONUS_ACCESSIBILITY_OVERLAY
            reasons.add(
                Reason(
                    text = "Accessibility and Overlay combination enables potential screen control and tapjacking risk",
                    points = RiskWeights.BONUS_ACCESSIBILITY_OVERLAY,
                    category = Category.SPECIAL_ACCESS
                )
            )
        }

        if (hasReadSms && hasInternet && hasBoot && isSideloaded) {
            comboBonusPts += RiskWeights.BONUS_SMS_INTERNET_BOOT_SIDELOADED
            reasons.add(
                Reason(
                    text = "Sideloaded background service with SMS reading and internet access resembles spyware behavior",
                    points = RiskWeights.BONUS_SMS_INTERNET_BOOT_SIDELOADED,
                    category = Category.SMS
                )
            )
        }

        if (hasReadSms && sa.notificationListenerEnabled) {
            comboBonusPts += RiskWeights.BONUS_SMS_NOTIFICATION_LISTENER
            reasons.add(
                Reason(
                    text = "Combination of SMS reading and notification listener enables dual 2FA interception",
                    points = RiskWeights.BONUS_SMS_NOTIFICATION_LISTENER,
                    category = Category.SMS
                )
            )
        }

        if (sa.deviceAdminActive && app.packageName.lowercase() !in RiskWeights.DEVICE_ADMIN_ALLOWLIST) {
            comboBonusPts += RiskWeights.BONUS_NON_SECURITY_DEVICE_ADMIN
            reasons.add(
                Reason(
                    text = "Device administrator mode is active on an app outside the security allowlist",
                    points = RiskWeights.BONUS_NON_SECURITY_DEVICE_ADMIN,
                    category = Category.SPECIAL_ACCESS
                )
            )
        }

        if (sa.installPackagesDeclared && isSideloaded) {
            comboBonusPts += RiskWeights.BONUS_INSTALL_PACKAGES_SIDELOADED
            reasons.add(
                Reason(
                    text = "Sideloaded app requests permission to install additional unknown packages",
                    points = RiskWeights.BONUS_INSTALL_PACKAGES_SIDELOADED,
                    category = Category.SPECIAL_ACCESS
                )
            )
        }

        // Context Mismatch
        val mismatchReason = ContextMismatch.evaluate(app)
        var mismatchPts = 0
        if (mismatchReason != null) {
            mismatchPts = mismatchReason.points
            reasons.add(mismatchReason)
        }

        val totalRaw = multipliedSubtotal + comboBonusPts + mismatchPts
        var finalScore = min(100, totalRaw.roundToInt())

        // If no strong signal, cap final score at 49
        if (!hasStrongSignal) {
            finalScore = min(49, finalScore)
        }

        if (totalRaw > 0 && finalScore == 0) {
            finalScore = 1
        }

        val band = RiskBand.fromScore(finalScore)

        // Ensure High / Critical apps have at least 3 reasons
        if ((band == RiskBand.HIGH || band == RiskBand.CRITICAL) && reasons.size < 3) {
            val existingTexts = reasons.map { it.text }.toSet()

            val fallbackOptions = listOf(
                Reason("App holds sensitive permissions affecting device security", 5, Category.OTHER),
                Reason("App possesses capabilities to perform background network or system operations", 5, Category.NETWORK),
                Reason("App configuration presents elevated risk characteristics", 5, Category.OTHER),
                Reason("App is configured with multiple privileged system capabilities", 5, Category.SPECIAL_ACCESS)
            )

            for (fallback in fallbackOptions) {
                if (reasons.size >= 3) break
                if (fallback.text !in existingTexts) {
                    reasons.add(fallback)
                }
            }
        }

        val sortedReasons = reasons.sortedByDescending { it.points }

        return RiskResult(
            score = finalScore,
            band = band,
            reasons = sortedReasons
        )
    }

    private fun evaluateStrongSignal(
        app: AppInfo,
        hasReadSms: Boolean,
        hasInternet: Boolean,
        hasBoot: Boolean,
        hasGrantedSms: Boolean,
        hasGrantedContactsCallLog: Boolean,
        hasGrantedMic: Boolean,
        isSideloaded: Boolean
    ): Boolean {
        val sa = app.specialAccess

        if (sa.accessibilityEnabled) return true
        if (sa.deviceAdminActive && app.packageName.lowercase() !in RiskWeights.DEVICE_ADMIN_ALLOWLIST) return true
        if (sa.notificationListenerEnabled && !app.isSystem) return true

        // Combination bonuses firing
        val hasAccessCombo = sa.accessibilityEnabled || sa.accessibilityDeclared
        val hasOverlayCombo = (sa.overlayGranted == true) || sa.overlayDeclared
        if (hasAccessCombo && hasOverlayCombo) return true
        if (hasReadSms && hasInternet && hasBoot && isSideloaded) return true
        if (hasReadSms && sa.notificationListenerEnabled) return true
        if (sa.installPackagesDeclared && isSideloaded) return true

        // Context mismatch
        if (ContextMismatch.evaluate(app) != null) return true

        // Sideloaded + granted sensitive permissions
        if (isSideloaded && (hasGrantedSms || hasGrantedContactsCallLog || hasGrantedMic || sa.accessibilityEnabled || sa.accessibilityDeclared || sa.overlayGranted == true || sa.overlayDeclared)) {
            return true
        }

        return false
    }
}
