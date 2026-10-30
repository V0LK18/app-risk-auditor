package com.example.appriskauditor.ui.home

import android.Manifest
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appriskauditor.R
import com.example.appriskauditor.domain.model.AppInfo
import com.example.appriskauditor.domain.model.Category
import com.example.appriskauditor.domain.model.InstallerSource
import com.example.appriskauditor.domain.model.SpecialAccess
import com.example.appriskauditor.domain.scoring.DiffResult
import com.example.appriskauditor.domain.summary.DeviceSummaryResult
import com.example.appriskauditor.ui.components.BandChip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    onAppClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var topMenuExpanded by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // Auto-rescan on foreground return
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                viewModel.onForegroundStart()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (uiState.showFirstRunDialog) {
        FirstRunExplainerDialog(
            onDismiss = { viewModel.dismissFirstRunDialog() }
        )
    }

    if (uiState.showDemoDialog) {
        DemoSimulateChangeDialog(
            apps = uiState.apps,
            onSelectApp = { viewModel.simulatePermissionChangeForDemo(it) },
            onDismiss = { viewModel.dismissDemoDialog() }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    Box {
                        IconButton(onClick = { topMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }

                        DropdownMenu(
                            expanded = topMenuExpanded,
                            onDismissRequest = { topMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Notify me about risky apps", style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Switch(
                                            checked = uiState.notifyRiskyApps,
                                            onCheckedChange = { checked ->
                                                topMenuExpanded = false
                                                if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                }
                                                viewModel.setNotifyRiskyApps(checked)
                                            }
                                        )
                                    }
                                },
                                onClick = { }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.rescan_now)) },
                                onClick = {
                                    topMenuExpanded = false
                                    viewModel.scanApps(isRescan = true)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Simulate change (demo)") },
                                onClick = {
                                    topMenuExpanded = false
                                    viewModel.showDemoDialog()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.about_app)) },
                                onClick = {
                                    topMenuExpanded = false
                                    viewModel.showAboutDialog()
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        val hasActiveFilters = uiState.searchQuery.isNotBlank() ||
                uiState.selectedBandFilter != BandFilter.ALL ||
                uiState.selectedCategoryFilter != null

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isRescanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Scanning and scoring apps...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "An error occurred",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 1. System apps toggle card
                        item(key = "system_apps_toggle") {
                            SystemAppsToggleCard(
                                includeSystemApps = uiState.includeSystemApps,
                                onIncludeSystemAppsChange = { viewModel.setIncludeSystemApps(it) },
                                totalAppsCount = uiState.apps.size,
                                apps = uiState.apps
                            )
                        }

                        // 2. Collapsible Device summary card
                        item(key = "device_summary") {
                            CollapsibleDeviceSummaryCard(
                                summary = uiState.deviceSummary,
                                lastScannedTime = uiState.lastScannedTime,
                                selectedCategory = uiState.selectedCategoryFilter,
                                onCategoryClick = { viewModel.setCategoryFilter(it) },
                                onClearCategoryFilter = { viewModel.setCategoryFilter(null) }
                            )
                        }

                        // 3. Top 3 riskiest apps strip (visible ONLY when no search or filters active)
                        if (uiState.apps.isNotEmpty() && !hasActiveFilters) {
                            item(key = "top_3_strip") {
                                Top3RiskiestAppsStrip(
                                    apps = uiState.apps,
                                    onAppClick = onAppClick
                                )
                            }
                        }

                        // 4. Sticky Header for Search & Filter controls
                        stickyHeader(key = "search_and_filters") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 2.dp
                            ) {
                                SearchAndFilterSection(
                                    searchQuery = uiState.searchQuery,
                                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                    selectedBand = uiState.selectedBandFilter,
                                    onBandSelect = { viewModel.setBandFilter(it) },
                                    selectedSort = uiState.selectedSortOption,
                                    onSortSelect = { viewModel.setSortOption(it) },
                                    hasActiveFilters = hasActiveFilters,
                                    onClearFilters = { viewModel.clearFilters() }
                                )
                            }
                        }

                        // 5. App List or Empty State
                        if (uiState.filteredApps.isEmpty()) {
                            item(key = "empty_state") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = if (hasActiveFilters) stringResource(R.string.no_apps_match) else "No installed apps found",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (hasActiveFilters) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(onClick = { viewModel.clearFilters() }) {
                                                Text(stringResource(R.string.clear_filter))
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            items(
                                items = uiState.filteredApps,
                                key = { it.packageName }
                            ) { app ->
                                AppItemRow(
                                    app = app,
                                    diff = uiState.diffs[app.packageName],
                                    onClick = { onAppClick(app.packageName) }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SystemAppsToggleCard(
    includeSystemApps: Boolean,
    onIncludeSystemAppsChange: (Boolean) -> Unit,
    totalAppsCount: Int,
    apps: List<AppInfo>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.include_system_apps),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = includeSystemApps,
                    onCheckedChange = onIncludeSystemAppsChange
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.total_apps, totalAppsCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            val accessibilityEnabledCount = apps.count { it.specialAccess.accessibilityEnabled }
            val deviceAdminActiveCount = apps.count { it.specialAccess.deviceAdminActive }
            val notificationListenerEnabledCount = apps.count { it.specialAccess.notificationListenerEnabled }

            Text(
                text = "Accessibility ON: $accessibilityEnabledCount | Device Admin ACTIVE: $deviceAdminActiveCount | Notification Access ON: $notificationListenerEnabledCount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun CollapsibleDeviceSummaryCard(
    summary: DeviceSummaryResult?,
    lastScannedTime: Long,
    selectedCategory: Category?,
    onCategoryClick: (Category) -> Unit,
    onClearCategoryFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (summary == null) return
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.device_summary_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }

                if (lastScannedTime > 0) {
                    val formattedTime = remember(lastScannedTime) {
                        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(lastScannedTime))
                    }
                    Text(
                        text = stringResource(R.string.last_scanned, formattedTime),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4 Band Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BandCountBadge("LOW", summary.lowCount, Color(0xFF2E7D32), Modifier.weight(1f))
                BandCountBadge("MED", summary.mediumCount, Color(0xFFF57F17), Modifier.weight(1f))
                BandCountBadge("HIGH", summary.highCount, Color(0xFFE65100), Modifier.weight(1f))
                BandCountBadge("CRIT", summary.criticalCount, Color(0xFFC62828), Modifier.weight(1f))
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    summary.categorySummaries.forEach { catSummary ->
                        val isSelected = selectedCategory == catSummary.category
                        val total = summary.totalApps
                        val progress = if (total > 0) catSummary.appsWithGrantedCount.toFloat() / total else 0f

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCategoryClick(catSummary.category) }
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(vertical = 3.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = catSummary.category.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.width(130.dp)
                            )

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "${catSummary.appsWithGrantedCount}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (selectedCategory != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = onClearCategoryFilter,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(stringResource(R.string.clear_filter))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BandCountBadge(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SearchAndFilterSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedBand: BandFilter,
    onBandSelect: (BandFilter) -> Unit,
    selectedSort: SortOption,
    onSortSelect: (SortOption) -> Unit,
    hasActiveFilters: Boolean,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Search text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search apps") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Filter chips and Sort dropdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                item {
                    FilterChip(
                        selected = selectedBand == BandFilter.ALL,
                        onClick = { onBandSelect(BandFilter.ALL) },
                        label = { Text(stringResource(R.string.filter_all), maxLines = 1, softWrap = false) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedBand == BandFilter.FLAGGED,
                        onClick = { onBandSelect(BandFilter.FLAGGED) },
                        label = { Text(stringResource(R.string.filter_flagged), maxLines = 1, softWrap = false) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedBand == BandFilter.MEDIUM,
                        onClick = { onBandSelect(BandFilter.MEDIUM) },
                        label = { Text(stringResource(R.string.filter_medium), maxLines = 1, softWrap = false) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedBand == BandFilter.LOW,
                        onClick = { onBandSelect(BandFilter.LOW) },
                        label = { Text(stringResource(R.string.filter_low), maxLines = 1, softWrap = false) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedBand == BandFilter.CHANGED,
                        onClick = { onBandSelect(BandFilter.CHANGED) },
                        label = { Text("Changed", maxLines = 1, softWrap = false) }
                    )
                }
            }

            Box {
                IconButton(onClick = { sortMenuExpanded = true }) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sort_risk_score)) },
                        onClick = { onSortSelect(SortOption.RISK_SCORE); sortMenuExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sort_name)) },
                        onClick = { onSortSelect(SortOption.NAME); sortMenuExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sort_recently_installed)) },
                        onClick = { onSortSelect(SortOption.RECENTLY_INSTALLED); sortMenuExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sort_recently_updated)) },
                        onClick = { onSortSelect(SortOption.RECENTLY_UPDATED); sortMenuExpanded = false }
                    )
                }
            }
        }

        if (hasActiveFilters) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                AssistChip(
                    onClick = onClearFilters,
                    label = { Text(stringResource(R.string.clear_filter)) },
                    leadingIcon = { Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }
    }
}

@Composable
fun FirstRunExplainerDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.first_run_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text("• ", fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.first_run_bullet_1), style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.Top) {
                    Text("• ", fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.first_run_bullet_2), style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.Top) {
                    Text("• ", fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.first_run_bullet_3), style = MaterialTheme.typography.bodyMedium)
                }

                HorizontalDivider()

                Text(
                    text = stringResource(R.string.first_run_privacy_note),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = stringResource(R.string.first_run_permission_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.got_it))
            }
        }
    )
}

@Composable
fun DemoSimulateChangeDialog(
    apps: List<AppInfo>,
    onSelectApp: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Simulate Change (Demo)") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Pick an app to remove 1 stored permission from its baseline snapshot. The next rescan will show it as 'UPDATED +1 permission'!",
                    style = MaterialTheme.typography.bodySmall
                )

                HorizontalDivider()

                apps.take(15).forEach { app ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectApp(app.packageName) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIconImage(packageName = app.packageName, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(app.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun Top3RiskiestAppsStrip(
    apps: List<AppInfo>,
    onAppClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val top3 = remember(apps) { apps.sortedByDescending { it.riskResult.score }.take(3) }
    if (top3.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.top_3_riskiest),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                top3.forEach { app ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onAppClick(app.packageName) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AppIconImage(
                                packageName = app.packageName,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = app.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            BandChip(riskResult = app.riskResult)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppItemRow(
    app: AppInfo,
    diff: DiffResult?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPermissions = app.permissions.size
    val grantedPermissions = app.permissions.count { it.granted }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        AppIconImage(
            packageName = app.packageName,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = app.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            val installerText = getInstallerLabel(app.installer, app.installerPackage)
            Text(
                text = "$installerText • $grantedPermissions / $totalPermissions permissions",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )

            if (diff != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (diff.isNewApp) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = "NEW",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (diff.addedSensitivePermissions.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ) {
                            Text(
                                text = "UPDATED +${diff.addedSensitivePermissions.size} perms",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (diff.specialAccessNewlyEnabled) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ) {
                            Text(
                                text = "Access ON",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            SpecialAccessChipRow(
                specialAccess = app.specialAccess,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        BandChip(
            riskResult = app.riskResult,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
    }
}

data class SpecialAccessChipData(
    val label: String,
    val isActive: Boolean
)

fun getSpecialAccessChips(specialAccess: SpecialAccess): List<SpecialAccessChipData> {
    val chips = mutableListOf<SpecialAccessChipData>()

    // Accessibility
    if (specialAccess.accessibilityEnabled) {
        chips.add(SpecialAccessChipData("Accessibility ON", isActive = true))
    } else if (specialAccess.accessibilityDeclared) {
        chips.add(SpecialAccessChipData("Can request accessibility", isActive = false))
    }

    // Device Admin
    if (specialAccess.deviceAdminActive) {
        chips.add(SpecialAccessChipData("Device admin ACTIVE", isActive = true))
    } else if (specialAccess.deviceAdminDeclared) {
        chips.add(SpecialAccessChipData("Can request device admin", isActive = false))
    }

    // Notification Listener
    if (specialAccess.notificationListenerEnabled) {
        chips.add(SpecialAccessChipData("Notification access ON", isActive = true))
    } else if (specialAccess.notificationListenerDeclared) {
        chips.add(SpecialAccessChipData("Can request notification access", isActive = false))
    }

    // Overlay
    if (specialAccess.overlayGranted == true) {
        chips.add(SpecialAccessChipData("Overlay ON", isActive = true))
    } else if (specialAccess.overlayDeclared) {
        chips.add(SpecialAccessChipData("Can request overlay", isActive = false))
    }

    // Install Packages
    if (specialAccess.installPackagesAllowed == true) {
        chips.add(SpecialAccessChipData("Install packages ON", isActive = true))
    } else if (specialAccess.installPackagesDeclared) {
        chips.add(SpecialAccessChipData("Can request install packages", isActive = false))
    }

    // Usage Stats
    if (specialAccess.usageStatsGranted == true) {
        chips.add(SpecialAccessChipData("Usage stats ON", isActive = true))
    } else if (specialAccess.usageStatsDeclared) {
        chips.add(SpecialAccessChipData("Can request usage stats", isActive = false))
    }

    return chips
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpecialAccessChipRow(
    specialAccess: SpecialAccess,
    modifier: Modifier = Modifier
) {
    val chips = remember(specialAccess) { getSpecialAccessChips(specialAccess) }
    if (chips.isEmpty()) return

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        chips.forEach { chip ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (chip.isActive) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                contentColor = if (chip.isActive) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            ) {
                Text(
                    text = chip.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (chip.isActive) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun AppIconImage(
    packageName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val iconBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = packageName) {
        value = withContext(Dispatchers.IO) {
            try {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                drawable.toImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    if (iconBitmap != null) {
        Image(
            bitmap = iconBitmap!!,
            contentDescription = null,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun Drawable.toImageBitmap(): ImageBitmap {
    if (this is BitmapDrawable && this.bitmap != null) {
        return this.bitmap.asImageBitmap()
    }
    val width = if (intrinsicWidth > 0) intrinsicWidth else 1
    val height = if (intrinsicHeight > 0) intrinsicHeight else 1
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap.asImageBitmap()
}

private fun getInstallerLabel(installer: InstallerSource, installerPackage: String?): String {
    return when (installer) {
        InstallerSource.PLAY_STORE -> "Play Store"
        InstallerSource.OTHER_STORE -> if (!installerPackage.isNullOrBlank()) installerPackage else "Other Store"
        InstallerSource.SYSTEM_PREINSTALLED -> "System app"
        InstallerSource.SIDELOADED_OR_UNKNOWN -> "Sideloaded"
    }
}
