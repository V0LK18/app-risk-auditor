package com.example.appriskauditor.domain.scoring

import com.example.appriskauditor.data.history.Snapshot
import com.example.appriskauditor.domain.model.AppInfo
import com.example.appriskauditor.domain.model.Category
import com.example.appriskauditor.domain.model.InstallerSource
import com.example.appriskauditor.domain.model.PermissionInfo
import com.example.appriskauditor.domain.model.ProtectionLevel
import com.example.appriskauditor.domain.model.RiskBand
import com.example.appriskauditor.domain.model.RiskResult
import com.example.appriskauditor.domain.model.SpecialAccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionDiffTest {

    private fun createPerm(name: String, granted: Boolean, category: Category): PermissionInfo {
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
        packageName: String = "com.example.app",
        versionCode: Long = 1,
        versionName: String = "1.0",
        permissions: List<PermissionInfo> = emptyList(),
        specialAccess: SpecialAccess = defaultSpecialAccess()
    ): AppInfo {
        val dummy = AppInfo(
            name = "Test App",
            packageName = packageName,
            versionName = versionName,
            versionCode = versionCode,
            installTime = 1000L,
            updateTime = 2000L,
            installer = InstallerSource.PLAY_STORE,
            installerPackage = null,
            targetSdk = 34,
            isSystem = false,
            permissions = permissions,
            specialAccess = specialAccess,
            riskResult = RiskResult(0, RiskBand.LOW, emptyList())
        )
        return dummy.copy(riskResult = RiskEngine.score(dummy))
    }

    private fun createSnapshot(
        packageName: String = "com.example.app",
        versionCode: Long = 1,
        versionName: String = "1.0",
        score: Int = 0,
        band: String = "LOW",
        sensitivePermissions: List<String> = emptyList(),
        specialAccessFlags: String = ""
    ): Snapshot {
        return Snapshot(
            packageName = packageName,
            versionCode = versionCode,
            versionName = versionName,
            score = score,
            band = band,
            sensitivePermissions = sensitivePermissions,
            specialAccessFlags = specialAccessFlags,
            firstSeen = 1000L,
            lastScanned = 2000L
        )
    }

    @Test
    fun testFirstEverScanNoBaseline() {
        val app = createApp(permissions = listOf(createPerm("android.permission.READ_SMS", true, Category.SMS)))
        val diff = PermissionDiff.compute(old = null, new = app, newResult = app.riskResult, isBaselineScan = true)

        assertFalse("Baseline scan should not mark app as new", diff.isNewApp)
        assertTrue("Baseline scan should not report added permissions", diff.addedSensitivePermissions.isEmpty())
        assertFalse("Baseline scan should not report band increased", diff.bandIncreased)
    }

    @Test
    fun testNewAppAfterBaseline() {
        val app = createApp(permissions = listOf(createPerm("android.permission.READ_SMS", true, Category.SMS)))
        val diff = PermissionDiff.compute(old = null, new = app, newResult = app.riskResult, isBaselineScan = false)

        assertTrue("App should be marked as new after baseline", diff.isNewApp)
        assertEquals(1, diff.addedSensitivePermissions.size)
        assertTrue(diff.addedSensitivePermissions.contains("android.permission.READ_SMS"))
    }

    @Test
    fun testNoChange() {
        val app = createApp(permissions = listOf(createPerm("android.permission.READ_SMS", true, Category.SMS)))
        val old = createSnapshot(
            score = app.riskResult.score,
            band = app.riskResult.band.name,
            sensitivePermissions = listOf("android.permission.READ_SMS")
        )

        val diff = PermissionDiff.compute(old = old, new = app, newResult = app.riskResult, isBaselineScan = false)

        assertFalse(diff.isNewApp)
        assertTrue(diff.addedSensitivePermissions.isEmpty())
        assertTrue(diff.removedSensitivePermissions.isEmpty())
        assertEquals(0, diff.scoreDelta)
        assertFalse(diff.bandIncreased)
        assertFalse(diff.specialAccessNewlyEnabled)
    }

    @Test
    fun testPermissionAdded() {
        val perms = listOf(
            createPerm("android.permission.READ_SMS", true, Category.SMS),
            createPerm("android.permission.READ_CONTACTS", true, Category.CONTACTS_CALLS)
        )
        val app = createApp(permissions = perms)
        val old = createSnapshot(
            score = 15,
            band = "LOW",
            sensitivePermissions = listOf("android.permission.READ_SMS")
        )

        val diff = PermissionDiff.compute(old = old, new = app, newResult = app.riskResult, isBaselineScan = false)

        assertEquals(1, diff.addedSensitivePermissions.size)
        assertTrue(diff.addedSensitivePermissions.contains("android.permission.READ_CONTACTS"))
        assertTrue(diff.removedSensitivePermissions.isEmpty())
    }

    @Test
    fun testPermissionRemoved() {
        val perms = listOf(createPerm("android.permission.READ_SMS", true, Category.SMS))
        val app = createApp(permissions = perms)
        val old = createSnapshot(
            score = 25,
            band = "MEDIUM",
            sensitivePermissions = listOf("android.permission.READ_SMS", "android.permission.READ_CONTACTS")
        )

        val diff = PermissionDiff.compute(old = old, new = app, newResult = app.riskResult, isBaselineScan = false)

        assertTrue(diff.addedSensitivePermissions.isEmpty())
        assertEquals(1, diff.removedSensitivePermissions.size)
        assertTrue(diff.removedSensitivePermissions.contains("android.permission.READ_CONTACTS"))
    }

    @Test
    fun testBandIncreased() {
        val app = createApp(
            packageName = "com.risky.app",
            permissions = listOf(
                createPerm("android.permission.READ_SMS", true, Category.SMS),
                createPerm("android.permission.READ_CONTACTS", true, Category.CONTACTS_CALLS),
                createPerm("android.permission.CAMERA", true, Category.CAMERA_MIC)
            )
        )
        val old = createSnapshot(
            score = 10,
            band = "LOW",
            sensitivePermissions = emptyList()
        )

        val diff = PermissionDiff.compute(old = old, new = app, newResult = app.riskResult, isBaselineScan = false)

        assertTrue("Band should be reported as increased", diff.bandIncreased)
        assertTrue("Score delta should be positive", diff.scoreDelta > 0)
    }

    @Test
    fun testSpecialAccessNewlyEnabled() {
        val sa = defaultSpecialAccess().copy(accessibilityEnabled = true)
        val app = createApp(specialAccess = sa)
        val old = createSnapshot(
            score = 0,
            band = "LOW",
            specialAccessFlags = ""
        )

        val diff = PermissionDiff.compute(old = old, new = app, newResult = app.riskResult, isBaselineScan = false)

        assertTrue("Special access should be reported as newly enabled", diff.specialAccessNewlyEnabled)
    }

    @Test
    fun testVersionChangedWithNoPermissionChange() {
        val app = createApp(versionCode = 2, versionName = "2.0")
        val old = createSnapshot(versionCode = 1, versionName = "1.0")

        val diff = PermissionDiff.compute(old = old, new = app, newResult = app.riskResult, isBaselineScan = false)

        assertFalse(diff.isNewApp)
        assertTrue(diff.addedSensitivePermissions.isEmpty())
        assertTrue(diff.removedSensitivePermissions.isEmpty())
        assertFalse(diff.bandIncreased)
    }
}
