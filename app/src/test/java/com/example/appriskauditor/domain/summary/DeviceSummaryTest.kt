package com.example.appriskauditor.domain.summary

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

class DeviceSummaryTest {

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
        packageName: String = "com.example.app",
        score: Int = 10,
        band: RiskBand = RiskBand.LOW,
        permissions: List<PermissionInfo> = emptyList(),
        specialAccess: SpecialAccess = defaultSpecialAccess()
    ): AppInfo {
        return AppInfo(
            name = name,
            packageName = packageName,
            versionName = "1.0",
            versionCode = 1,
            installTime = 0,
            updateTime = 0,
            installer = InstallerSource.PLAY_STORE,
            installerPackage = null,
            targetSdk = 34,
            isSystem = false,
            permissions = permissions,
            specialAccess = specialAccess,
            riskResult = RiskResult(score, band, emptyList())
        )
    }

    private fun createPerm(name: String, granted: Boolean, category: Category): PermissionInfo {
        return PermissionInfo(
            name = name,
            granted = granted,
            protectionLevel = ProtectionLevel.DANGEROUS,
            category = category,
            description = "Test permission"
        )
    }

    @Test
    fun testEmptyList() {
        val summary = DeviceSummary.compute(emptyList())
        assertEquals(0, summary.totalApps)
        assertEquals(0, summary.lowCount)
        assertEquals(0, summary.mediumCount)
        assertEquals(0, summary.highCount)
        assertEquals(0, summary.criticalCount)
        assertEquals(9, summary.categorySummaries.size)
    }

    @Test
    fun testOneApp() {
        val app = createApp(
            name = "Single App",
            packageName = "com.single.app",
            score = 30,
            band = RiskBand.MEDIUM,
            permissions = listOf(createPerm("android.permission.CAMERA", granted = true, category = Category.CAMERA_MIC))
        )

        val summary = DeviceSummary.compute(listOf(app))
        assertEquals(1, summary.totalApps)
        assertEquals(1, summary.mediumCount)

        val cameraSummary = summary.categorySummaries.first { it.category == Category.CAMERA_MIC }
        assertEquals(1, cameraSummary.appsWithGrantedCount)
        assertTrue(cameraSummary.appsWithGrantedPackageNames.contains("com.single.app"))
    }

    @Test
    fun testGrantedVsDeclaredOnly() {
        val grantedApp = createApp(
            packageName = "com.granted.app",
            permissions = listOf(createPerm("android.permission.READ_SMS", granted = true, category = Category.SMS))
        )
        val declaredApp = createApp(
            packageName = "com.declared.app",
            permissions = listOf(createPerm("android.permission.READ_SMS", granted = false, category = Category.SMS))
        )

        val summary = DeviceSummary.compute(listOf(grantedApp, declaredApp))
        val smsSummary = summary.categorySummaries.first { it.category == Category.SMS }

        assertEquals(1, smsSummary.appsWithGrantedCount)
        assertEquals(1, smsSummary.appsWithDeclaredOnlyCount)
        assertTrue(smsSummary.appsWithGrantedPackageNames.contains("com.granted.app"))
    }

    @Test
    fun testSpecialAccessEnabledCountsUnderSpecialAccess() {
        val saApp = createApp(
            packageName = "com.sa.app",
            specialAccess = defaultSpecialAccess().copy(accessibilityEnabled = true)
        )

        val summary = DeviceSummary.compute(listOf(saApp))
        val saSummary = summary.categorySummaries.first { it.category == Category.SPECIAL_ACCESS }

        assertEquals(1, saSummary.appsWithGrantedCount)
        assertTrue(saSummary.appsWithGrantedPackageNames.contains("com.sa.app"))
    }

    @Test
    fun testBandCounts() {
        val lowApp = createApp(packageName = "com.low", score = 10, band = RiskBand.LOW)
        val medApp = createApp(packageName = "com.med", score = 30, band = RiskBand.MEDIUM)
        val highApp = createApp(packageName = "com.high", score = 60, band = RiskBand.HIGH)
        val critApp = createApp(packageName = "com.crit", score = 85, band = RiskBand.CRITICAL)

        val summary = DeviceSummary.compute(listOf(lowApp, medApp, highApp, critApp))

        assertEquals(4, summary.totalApps)
        assertEquals(1, summary.lowCount)
        assertEquals(1, summary.mediumCount)
        assertEquals(1, summary.highCount)
        assertEquals(1, summary.criticalCount)
    }
}
