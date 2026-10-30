package com.example.appriskauditor.domain.scoring

import com.example.appriskauditor.domain.model.AppInfo
import com.example.appriskauditor.domain.model.Category
import com.example.appriskauditor.domain.model.InstallerSource
import com.example.appriskauditor.domain.model.PermissionInfo
import com.example.appriskauditor.domain.model.ProtectionLevel
import com.example.appriskauditor.domain.model.RiskBand
import com.example.appriskauditor.domain.model.RiskResult
import com.example.appriskauditor.domain.model.SpecialAccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskEngineTest {

    private fun createPerm(name: String, granted: Boolean, category: Category = Category.OTHER): PermissionInfo {
        return PermissionInfo(
            name = name,
            granted = granted,
            protectionLevel = ProtectionLevel.DANGEROUS,
            category = category,
            description = "Test permission"
        )
    }

    private fun defaultSpecialAccess(): SpecialAccess {
        return SpecialAccess(
            accessibilityDeclared = false,
            accessibilityEnabled = false,
            deviceAdminDeclared = false,
            deviceAdminActive = false,
            notificationListenerDeclared = false,
            notificationListenerEnabled = false,
            overlayDeclared = false,
            overlayGranted = false,
            installPackagesDeclared = false,
            installPackagesAllowed = false,
            usageStatsDeclared = false,
            usageStatsGranted = false
        )
    }

    private fun createApp(
        name: String = "Test App",
        packageName: String = "com.example.testapp",
        installer: InstallerSource = InstallerSource.PLAY_STORE,
        permissions: List<PermissionInfo> = emptyList(),
        specialAccess: SpecialAccess = defaultSpecialAccess(),
        isDefaultSmsApp: Boolean = false,
        isDefaultDialerApp: Boolean = false
    ): AppInfo {
        val dummy = AppInfo(
            name = name,
            packageName = packageName,
            versionName = "1.0",
            versionCode = 1,
            installTime = 0,
            updateTime = 0,
            installer = installer,
            installerPackage = null,
            targetSdk = 34,
            isSystem = false,
            permissions = permissions,
            specialAccess = specialAccess,
            riskResult = RiskResult(0, RiskBand.LOW, emptyList()),
            isDefaultSmsApp = isDefaultSmsApp,
            isDefaultDialerApp = isDefaultDialerApp
        )
        return dummy.copy(riskResult = RiskEngine.score(dummy))
    }

    @Test
    fun testBenignApp() {
        val app = createApp(
            name = "Benign App",
            permissions = listOf(createPerm("android.permission.INTERNET", granted = true, category = Category.NETWORK))
        )
        val result = RiskEngine.score(app)
        assertEquals(0, result.score)
        assertEquals(RiskBand.LOW, result.band)
    }

    @Test
    fun testGoogleLikeAppWithCommonPermissionsNoStrongSignalScoreCap() {
        val perms = listOf(
            createPerm("android.permission.READ_SMS", granted = true, category = Category.SMS),
            createPerm("android.permission.READ_CONTACTS", granted = true, category = Category.CONTACTS_CALLS),
            createPerm("android.permission.CAMERA", granted = true, category = Category.CAMERA_MIC),
            createPerm("android.permission.RECORD_AUDIO", granted = true, category = Category.CAMERA_MIC),
            createPerm("android.permission.ACCESS_FINE_LOCATION", granted = true, category = Category.LOCATION),
            createPerm("android.permission.READ_PHONE_STATE", granted = true, category = Category.PHONE_IDENTITY),
            createPerm("android.permission.READ_MEDIA_IMAGES", granted = true, category = Category.STORAGE_MEDIA),
            createPerm("android.permission.RECEIVE_BOOT_COMPLETED", granted = true, category = Category.BACKGROUND)
        )
        val googleApp = createApp(
            name = "Google Maps",
            packageName = "com.google.android.apps.maps",
            installer = InstallerSource.PLAY_STORE,
            permissions = perms
        )
        val result = RiskEngine.score(googleApp)
        assertTrue("Google app without strong signal must score <= 49 (got ${result.score})", result.score <= 49)
    }

    @Test
    fun testTrojanStyleProfileStillCritical() {
        val sa = defaultSpecialAccess().copy(
            accessibilityEnabled = true,
            overlayGranted = true
        )
        val perms = listOf(
            createPerm("android.permission.READ_SMS", granted = true, category = Category.SMS),
            createPerm("android.permission.INTERNET", granted = true, category = Category.NETWORK),
            createPerm("android.permission.RECEIVE_BOOT_COMPLETED", granted = true, category = Category.BACKGROUND)
        )
        val app = createApp(
            name = "Trojan App",
            installer = InstallerSource.SIDELOADED_OR_UNKNOWN,
            permissions = perms,
            specialAccess = sa
        )
        val result = RiskEngine.score(app)
        assertEquals(RiskBand.CRITICAL, result.band)
        assertTrue("Trojan profile should reach score >= 75", result.score >= 75)
        assertTrue("High or Critical risk apps must have at least 3 reasons", result.reasons.size >= 3)
    }

    @Test
    fun testSideloadedFlashlightAppWithSmsReadIsHighOrCritical() {
        val perms = listOf(
            createPerm("android.permission.READ_SMS", granted = true, category = Category.SMS),
            createPerm("android.permission.READ_CONTACTS", granted = true, category = Category.CONTACTS_CALLS)
        )
        val app = createApp(
            name = "Flashlight Utility",
            packageName = "com.flashlight.app",
            installer = InstallerSource.SIDELOADED_OR_UNKNOWN,
            permissions = perms
        )
        val result = RiskEngine.score(app)
        assertTrue("Sideloaded flashlight app with SMS read should be HIGH or CRITICAL (got ${result.score})", result.score >= 50)
        assertTrue("Should contain a context mismatch reason", result.reasons.any { it.text.contains("flashlight", ignoreCase = true) })
    }

    @Test
    fun testDefaultSmsAppDoesNotGetSmsPoints() {
        val perms = listOf(
            createPerm("android.permission.READ_SMS", granted = true, category = Category.SMS),
            createPerm("android.permission.RECEIVE_SMS", granted = true, category = Category.SMS),
            createPerm("android.permission.SEND_SMS", granted = true, category = Category.SMS)
        )
        val defaultSmsApp = createApp(
            name = "Messages",
            packageName = "com.google.android.apps.messaging",
            installer = InstallerSource.PLAY_STORE,
            permissions = perms,
            isDefaultSmsApp = true
        )
        val nonDefaultSmsApp = createApp(
            name = "ThirdParty Messages",
            packageName = "com.thirdparty.sms",
            installer = InstallerSource.SIDELOADED_OR_UNKNOWN,
            permissions = perms,
            isDefaultSmsApp = false
        )

        val defaultResult = RiskEngine.score(defaultSmsApp)
        val nonDefaultResult = RiskEngine.score(nonDefaultSmsApp)

        assertTrue("Default SMS app should score 0 from SMS permissions", defaultResult.score == 0)
        assertTrue("Non-default sideloaded SMS app should score higher", nonDefaultResult.score > defaultResult.score)
        assertTrue("Default SMS app should include default SMS reason", defaultResult.reasons.any { it.text.contains("Default Messages app", ignoreCase = true) })
    }

    @Test
    fun testSideloadedVsKnownPublisherScoreDifference() {
        val perms = listOf(
            createPerm("android.permission.READ_SMS", granted = true, category = Category.SMS),
            createPerm("android.permission.READ_CONTACTS", granted = true, category = Category.CONTACTS_CALLS),
            createPerm("android.permission.CAMERA", granted = true, category = Category.CAMERA_MIC)
        )

        val knownPublisherApp = createApp(
            name = "Google Photos",
            packageName = "com.google.android.apps.photos",
            installer = InstallerSource.PLAY_STORE,
            permissions = perms
        )

        val sideloadedApp = createApp(
            name = "Unknown Photos",
            packageName = "com.unknown.photos",
            installer = InstallerSource.SIDELOADED_OR_UNKNOWN,
            permissions = perms
        )

        val knownResult = RiskEngine.score(knownPublisherApp)
        val sideloadedResult = RiskEngine.score(sideloadedApp)

        assertTrue(
            "Sideloaded app (${sideloadedResult.score}) should score clearly higher than trusted known publisher app (${knownResult.score})",
            sideloadedResult.score > knownResult.score + 10
        )
    }

    @Test
    fun testNoStrongSignalScoreCappedAt49() {
        val perms = listOf(
            createPerm("android.permission.READ_SMS", granted = true, category = Category.SMS),
            createPerm("android.permission.READ_CONTACTS", granted = true, category = Category.CONTACTS_CALLS),
            createPerm("android.permission.CAMERA", granted = true, category = Category.CAMERA_MIC),
            createPerm("android.permission.RECORD_AUDIO", granted = true, category = Category.CAMERA_MIC),
            createPerm("android.permission.ACCESS_FINE_LOCATION", granted = true, category = Category.LOCATION),
            createPerm("android.permission.MANAGE_EXTERNAL_STORAGE", granted = true, category = Category.STORAGE_MEDIA)
        )
        val app = createApp(
            name = "Heavy Permission App",
            packageName = "com.ordinary.heavyapp",
            installer = InstallerSource.PLAY_STORE,
            permissions = perms
        )
        val result = RiskEngine.score(app)
        assertTrue("App without strong signals must be capped at 49 (got ${result.score})", result.score <= 49)
    }

    @Test
    fun testDeviceAdminAllowlist() {
        val sa = defaultSpecialAccess().copy(deviceAdminActive = true)

        val nonSecurityApp = createApp(
            packageName = "com.ordinary.app",
            specialAccess = sa
        )
        val nonSecResult = RiskEngine.score(nonSecurityApp)

        val securityApp = createApp(
            packageName = "com.google.android.apps.work.clouddpc",
            specialAccess = sa
        )
        val secResult = RiskEngine.score(securityApp)

        assertTrue(
            "Non-security device admin app should have higher score than allowlisted app",
            nonSecResult.score > secResult.score
        )
    }

    @Test
    fun testDeclaredNotGrantedFactor() {
        val grantedApp = createApp(
            permissions = listOf(createPerm("android.permission.READ_SMS", granted = true, category = Category.SMS))
        )
        val ungrantedApp = createApp(
            permissions = listOf(createPerm("android.permission.READ_SMS", granted = false, category = Category.SMS))
        )

        val grantedResult = RiskEngine.score(grantedApp)
        val ungrantedResult = RiskEngine.score(ungrantedApp)

        assertEquals(15, grantedResult.score)
        assertEquals(4, ungrantedResult.score) // 15 * 0.25 = 3.75 -> rounded to 4
    }

    @Test
    fun testCapsAndScoreNeverExceeds100() {
        val sa = defaultSpecialAccess().copy(
            accessibilityEnabled = true,
            deviceAdminActive = true,
            notificationListenerEnabled = true,
            overlayGranted = true,
            installPackagesDeclared = true,
            usageStatsGranted = true
        )
        val perms = listOf(
            createPerm("android.permission.READ_SMS", granted = true),
            createPerm("android.permission.RECEIVE_SMS", granted = true),
            createPerm("android.permission.SEND_SMS", granted = true),
            createPerm("android.permission.READ_CONTACTS", granted = true),
            createPerm("android.permission.READ_CALL_LOG", granted = true),
            createPerm("android.permission.CAMERA", granted = true),
            createPerm("android.permission.RECORD_AUDIO", granted = true),
            createPerm("android.permission.ACCESS_FINE_LOCATION", granted = true),
            createPerm("android.permission.ACCESS_BACKGROUND_LOCATION", granted = true),
            createPerm("android.permission.MANAGE_EXTERNAL_STORAGE", granted = true)
        )

        val app = createApp(
            name = "Super High Risk App",
            installer = InstallerSource.SIDELOADED_OR_UNKNOWN,
            permissions = perms,
            specialAccess = sa
        )

        val result = RiskEngine.score(app)
        assertEquals(100, result.score)
        assertEquals(RiskBand.CRITICAL, result.band)
    }

    @Test
    fun testBandBoundaries() {
        assertEquals(RiskBand.LOW, RiskBand.fromScore(0))
        assertEquals(RiskBand.LOW, RiskBand.fromScore(24))
        assertEquals(RiskBand.MEDIUM, RiskBand.fromScore(25))
        assertEquals(RiskBand.MEDIUM, RiskBand.fromScore(49))
        assertEquals(RiskBand.HIGH, RiskBand.fromScore(50))
        assertEquals(RiskBand.HIGH, RiskBand.fromScore(74))
        assertEquals(RiskBand.CRITICAL, RiskBand.fromScore(75))
        assertEquals(RiskBand.CRITICAL, RiskBand.fromScore(100))
    }

    @Test
    fun testReasonsSortedByPointsDescending() {
        val sa = defaultSpecialAccess().copy(
            accessibilityEnabled = true,
            deviceAdminActive = true
        )
        val perms = listOf(
            createPerm("android.permission.READ_SMS", granted = true, category = Category.SMS)
        )
        val app = createApp(
            installer = InstallerSource.SIDELOADED_OR_UNKNOWN,
            permissions = perms,
            specialAccess = sa
        )

        val result = RiskEngine.score(app)
        assertTrue("Reasons should not be empty", result.reasons.isNotEmpty())

        for (i in 0 until result.reasons.size - 1) {
            assertTrue(
                "Reasons must be sorted by points descending",
                result.reasons[i].points >= result.reasons[i + 1].points
            )
        }
    }
}
