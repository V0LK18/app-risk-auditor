package com.example.appriskauditor.ui.detail

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.appriskauditor.R
import com.example.appriskauditor.domain.PermissionCatalog
import com.example.appriskauditor.domain.model.AppInfo
import com.example.appriskauditor.domain.model.Category
import com.example.appriskauditor.domain.model.InstallerSource
import com.example.appriskauditor.domain.model.PermissionInfo
import com.example.appriskauditor.domain.model.ProtectionLevel
import com.example.appriskauditor.domain.model.RiskBand
import com.example.appriskauditor.domain.scoring.DiffResult
import com.example.appriskauditor.ui.components.BandChip
import com.example.appriskauditor.ui.home.AppIconImage
import com.example.appriskauditor.ui.home.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    packageName: String,
    viewModel: HomeViewModel,
    onBackClick: () -> Unit,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val app = remember(uiState.apps, packageName) {
        uiState.apps.firstOrNull { it.packageName == packageName }
    }

    // Refresh scanning when returning to detail screen
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.scanApps()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(app?.name ?: "App Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (app == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.app_not_found),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onNavigateHome) {
                        Text(stringResource(R.string.back_to_home))
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HeaderSection(app = app)

                ScoreBlock(app = app)

                val diff = uiState.diffs[app.packageName]
                if (diff != null && diff.hasChanges) {
                    ChangesSinceLastScanCard(diff = diff)
                }

                WhyFlaggedPanel(app = app)

                SpecialAccessPanel(app = app)

                PermissionsByCategoryPanel(app = app)

                WhatYouCanDoPanel(
                    app = app,
                    onOpenAppSettings = {
                        tryLaunchIntent(context) {
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", app.packageName, null)
                            }
                        }
                    },
                    onUninstall = {
                        tryLaunchIntent(context) {
                            Intent(Intent.ACTION_DELETE).apply {
                                data = Uri.fromParts("package", app.packageName, null)
                            }
                        }
                    },
                    onOpenAccessibility = {
                        tryLaunchIntent(context) {
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        }
                    },
                    onOpenNotificationAccess = {
                        tryLaunchIntent(context) {
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        }
                    },
                    onOpenDeviceAdmin = {
                        tryLaunchIntent(context) {
                            try {
                                Intent("android.settings.DEVICE_ADMIN_SETTINGS")
                            } catch (_: Exception) {
                                Intent(Settings.ACTION_SECURITY_SETTINGS)
                            }
                        }
                    },
                    onOpenOverlay = {
                        tryLaunchIntent(context) {
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                data = Uri.fromParts("package", app.packageName, null)
                            }
                        }
                    }
                )
            }
        }
    }
}

private fun tryLaunchIntent(context: android.content.Context, intentProvider: () -> Intent) {
    try {
        val intent = intentProvider()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun HeaderSection(app: AppInfo) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val installDateStr = remember(app.installTime) { if (app.installTime > 0) dateFormat.format(Date(app.installTime)) else "Unknown" }
    val updateDateStr = remember(app.updateTime) { if (app.updateTime > 0) dateFormat.format(Date(app.updateTime)) else "Unknown" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            AppIconImage(
                packageName = app.packageName,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Version: ${app.versionName} (${app.versionCode})",
                    style = MaterialTheme.typography.labelMedium
                )

                val installerText = when (app.installer) {
                    InstallerSource.PLAY_STORE -> "Play Store"
                    InstallerSource.OTHER_STORE -> if (!app.installerPackage.isNullOrBlank()) app.installerPackage else "Other Store"
                    InstallerSource.SYSTEM_PREINSTALLED -> "System app"
                    InstallerSource.SIDELOADED_OR_UNKNOWN -> "Sideloaded"
                }

                Text(
                    text = "Installer: $installerText",
                    style = MaterialTheme.typography.labelMedium
                )

                Text(
                    text = "Installed: $installDateStr | Updated: $updateDateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Target SDK: ${app.targetSdk}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ChangesSinceLastScanCard(
    diff: DiffResult,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Changes since last scan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (diff.isNewApp) {
                Text(
                    text = "App first installed / detected after baseline scan",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "Score change: ${diff.oldScore} ➔ ${diff.newScore} (delta: ${if (diff.scoreDelta >= 0) "+${diff.scoreDelta}" else "${diff.scoreDelta}"})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            if (diff.addedSensitivePermissions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Added sensitive permissions:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                diff.addedSensitivePermissions.forEach { permName ->
                    val spec = PermissionCatalog.getPermissionSpec(permName)
                    Text(
                        text = "• ${spec.description} ($permName)",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                    )
                }
            }

            if (diff.removedSensitivePermissions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Removed sensitive permissions:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                diff.removedSensitivePermissions.forEach { permName ->
                    val spec = PermissionCatalog.getPermissionSpec(permName)
                    Text(
                        text = "• ${spec.description} ($permName)",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                    )
                }
            }

            if (diff.specialAccessNewlyEnabled) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• Special access capability newly enabled",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (diff.lastScannedTime > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                val formattedTime = remember(diff.lastScannedTime) {
                    SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(diff.lastScannedTime))
                }
                Text(
                    text = "Last scanned baseline: $formattedTime",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ScoreBlock(app: AppInfo) {
    val riskResult = app.riskResult
    val summaryText = when (riskResult.band) {
        RiskBand.LOW -> stringResource(R.string.summary_low)
        RiskBand.MEDIUM -> stringResource(R.string.summary_medium)
        RiskBand.HIGH -> stringResource(R.string.summary_high)
        RiskBand.CRITICAL -> stringResource(R.string.summary_critical)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Risk Score",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                BandChip(riskResult = riskResult)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun WhyFlaggedPanel(app: AppInfo) {
    val reasons = app.riskResult.reasons
    var expanded by remember { mutableStateOf(false) }
    val displayReasons = if (expanded || reasons.size <= 5) reasons else reasons.take(5)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.why_flagged_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (reasons.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_risky_access),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                displayReasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        val dotColor = when {
                            reason.points >= 20 -> Color(0xFFC62828)
                            reason.points >= 10 -> Color(0xFFE65100)
                            else -> Color(0xFFF57F17)
                        }

                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp, end = 12.dp)
                                .size(8.dp)
                                .background(color = dotColor, shape = CircleShape)
                        )

                        Text(
                            text = reason.text,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )

                        if (reason.points > 0) {
                            Text(
                                text = "+${reason.points} pts",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                if (reasons.size > 5) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            if (expanded) stringResource(R.string.show_less)
                            else stringResource(R.string.show_all, reasons.size)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpecialAccessPanel(app: AppInfo) {
    val sa = app.specialAccess

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.special_access_panel_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            SpecialAccessItemRow("Accessibility", sa.accessibilityEnabled, sa.accessibilityDeclared, null)
            SpecialAccessItemRow("Device Administrator", sa.deviceAdminActive, sa.deviceAdminDeclared, null)
            SpecialAccessItemRow("Notification Listener", sa.notificationListenerEnabled, sa.notificationListenerDeclared, null)
            SpecialAccessItemRow("Screen Overlay", sa.overlayGranted == true, sa.overlayDeclared, sa.overlayGranted)
            SpecialAccessItemRow("Install Unknown Packages", sa.installPackagesAllowed == true, sa.installPackagesDeclared, sa.installPackagesAllowed)
            SpecialAccessItemRow("Usage Statistics", sa.usageStatsGranted == true, sa.usageStatsDeclared, sa.usageStatsGranted)
        }
    }
}

@Composable
fun SpecialAccessItemRow(
    title: String,
    isActive: Boolean,
    isDeclared: Boolean,
    unknownState: Boolean?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        val (chipText, chipBg, chipFg) = when {
            isActive -> Triple("ACTIVE", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
            isDeclared && unknownState == null -> Triple("UNKNOWN", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
            isDeclared -> Triple("DECLARED", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
            else -> Triple("NOT DECLARED", MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.outline)
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = chipBg,
            contentColor = chipFg
        ) {
            Text(
                text = chipText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun PermissionsByCategoryPanel(app: AppInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.permissions_by_category_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Category.entries.forEach { category ->
                val categoryPerms = app.permissions.filter { it.category == category }
                if (categoryPerms.isNotEmpty()) {
                    CategoryExpandableSection(category = category, permissions = categoryPerms)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun CategoryExpandableSection(
    category: Category,
    permissions: List<PermissionInfo>
) {
    var expanded by remember { mutableStateOf(false) }
    val grantedCount = permissions.count { it.granted }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${category.displayName} ($grantedCount/${permissions.size} granted)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                permissions.forEach { perm ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = perm.description,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )

                            val (statusText, statusBg, statusFg) = if (perm.granted) {
                                Triple("GRANTED", Color(0xFF2E7D32), Color.White)
                            } else {
                                Triple("NOT GRANTED", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = statusBg,
                                contentColor = statusFg,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = perm.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val protName = when (perm.protectionLevel) {
                                ProtectionLevel.NORMAL -> "Normal"
                                ProtectionLevel.DANGEROUS -> "Dangerous"
                                ProtectionLevel.SIGNATURE -> "Signature"
                                ProtectionLevel.SPECIAL -> "Special"
                                ProtectionLevel.UNKNOWN -> "Unknown"
                            }

                            Text(
                                text = "Level: $protName",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WhatYouCanDoPanel(
    app: AppInfo,
    onOpenAppSettings: () -> Unit,
    onUninstall: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onOpenDeviceAdmin: () -> Unit,
    onOpenOverlay: () -> Unit
) {
    val sa = app.specialAccess

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.what_you_can_do_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (sa.deviceAdminActive) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.device_admin_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Button(
                onClick = onOpenAppSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.open_app_settings))
            }

            if (!app.isSystem && app.installer != InstallerSource.SYSTEM_PREINSTALLED) {
                Button(
                    onClick = onUninstall,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.uninstall_app))
                }
            } else {
                Text(
                    text = stringResource(R.string.system_app_cannot_uninstall),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (sa.accessibilityEnabled) {
                OutlinedButton(
                    onClick = onOpenAccessibility,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.open_accessibility_settings))
                }
            }

            if (sa.notificationListenerEnabled) {
                OutlinedButton(
                    onClick = onOpenNotificationAccess,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.open_notification_access_settings))
                }
            }

            if (sa.deviceAdminActive) {
                OutlinedButton(
                    onClick = onOpenDeviceAdmin,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.open_device_admin_settings))
                }
            }

            if (sa.overlayGranted == true || sa.overlayDeclared) {
                OutlinedButton(
                    onClick = onOpenOverlay,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.open_overlay_settings))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val adviceText = when (app.riskResult.band) {
                RiskBand.HIGH, RiskBand.CRITICAL -> stringResource(R.string.advice_high_critical)
                RiskBand.MEDIUM -> stringResource(R.string.advice_medium)
                RiskBand.LOW -> stringResource(R.string.advice_low)
            }

            Text(
                text = adviceText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
