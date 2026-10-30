package com.example.appriskauditor.ui.home

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appriskauditor.data.alerts.RiskyAppAlertWorker
import com.example.appriskauditor.data.history.Snapshot
import com.example.appriskauditor.data.history.SnapshotStore
import com.example.appriskauditor.data.scanner.AppScanner
import com.example.appriskauditor.domain.model.AppInfo
import com.example.appriskauditor.domain.model.Category
import com.example.appriskauditor.domain.model.RiskBand
import com.example.appriskauditor.domain.scoring.DiffResult
import com.example.appriskauditor.domain.scoring.PermissionDiff
import com.example.appriskauditor.domain.summary.DeviceSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val appScanner = AppScanner(application)
    private val snapshotStore = SnapshotStore(application)
    private val prefs = application.getSharedPreferences("app_risk_auditor_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        checkFirstRun()
        val notifyEnabled = prefs.getBoolean("notify_risky_apps", true)
        _uiState.update { it.copy(notifyRiskyApps = notifyEnabled) }
        if (notifyEnabled) {
            RiskyAppAlertWorker.schedule(application)
        }
        scanApps()
    }

    private fun checkFirstRun() {
        val completed = prefs.getBoolean("is_first_run_completed", false)
        if (!completed) {
            _uiState.update { it.copy(showFirstRunDialog = true) }
        }
        val savedLastScanned = prefs.getLong("last_scanned_time", 0L)
        _uiState.update { it.copy(lastScannedTime = savedLastScanned) }
    }

    fun dismissFirstRunDialog() {
        prefs.edit().putBoolean("is_first_run_completed", true).apply()
        _uiState.update { it.copy(showFirstRunDialog = false) }
    }

    fun showAboutDialog() {
        _uiState.update { it.copy(showFirstRunDialog = true) }
    }

    fun showDemoDialog() {
        _uiState.update { it.copy(showDemoDialog = true) }
    }

    fun dismissDemoDialog() {
        _uiState.update { it.copy(showDemoDialog = false) }
    }

    fun setNotifyRiskyApps(enabled: Boolean) {
        prefs.edit().putBoolean("notify_risky_apps", enabled).apply()
        _uiState.update { it.copy(notifyRiskyApps = enabled) }
        if (enabled) {
            RiskyAppAlertWorker.schedule(getApplication())
        } else {
            RiskyAppAlertWorker.cancel(getApplication())
        }
    }

    fun simulatePermissionChangeForDemo(packageName: String) {
        viewModelScope.launch {
            val success = snapshotStore.removePackagePermissionForDemo(packageName)
            if (success) {
                _uiState.update { it.copy(showDemoDialog = false) }
                scanApps(isRescan = true)
            }
        }
    }

    fun onForegroundStart() {
        val now = System.currentTimeMillis()
        if (now - _uiState.value.lastScannedTime >= 60_000L && !_uiState.value.isLoading && !_uiState.value.isRescanning) {
            scanApps(isRescan = true)
        }
    }

    fun setIncludeSystemApps(include: Boolean) {
        if (_uiState.value.includeSystemApps == include) return
        _uiState.update { it.copy(includeSystemApps = include) }
        scanApps()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val updated = state.copy(searchQuery = query)
            updated.copy(filteredApps = computeFilteredApps(updated))
        }
    }

    fun setBandFilter(filter: BandFilter) {
        _uiState.update { state ->
            val updated = state.copy(selectedBandFilter = filter)
            updated.copy(filteredApps = computeFilteredApps(updated))
        }
    }

    fun setCategoryFilter(category: Category?) {
        _uiState.update { state ->
            val updated = if (state.selectedCategoryFilter == category) {
                state.copy(selectedCategoryFilter = null)
            } else {
                state.copy(selectedCategoryFilter = category)
            }
            updated.copy(filteredApps = computeFilteredApps(updated))
        }
    }

    fun setSortOption(option: SortOption) {
        _uiState.update { state ->
            val updated = state.copy(selectedSortOption = option)
            updated.copy(filteredApps = computeFilteredApps(updated))
        }
    }

    fun clearFilters() {
        _uiState.update { state ->
            val updated = state.copy(
                searchQuery = "",
                selectedBandFilter = BandFilter.ALL,
                selectedCategoryFilter = null
            )
            updated.copy(filteredApps = computeFilteredApps(updated))
        }
    }

    fun scanApps(isRescan: Boolean = false) {
        viewModelScope.launch {
            if (isRescan) {
                _uiState.update { it.copy(isRescanning = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            try {
                val hasBaseline = snapshotStore.hasBaseline()
                val previousSnapshots = snapshotStore.loadAll()

                val scannedApps = appScanner.scan(includeSystem = _uiState.value.includeSystemApps)
                val summary = DeviceSummary.compute(scannedApps)
                val now = System.currentTimeMillis()

                val diffsMap = mutableMapOf<String, DiffResult>()
                val updatedSnapshots = mutableMapOf<String, Snapshot>()

                for (app in scannedApps) {
                    val oldSnapshot = previousSnapshots[app.packageName]
                    val diff = PermissionDiff.compute(
                        old = oldSnapshot,
                        new = app,
                        newResult = app.riskResult,
                        isBaselineScan = !hasBaseline
                    )

                    diffsMap[app.packageName] = diff

                    if (diff.hasChanges) {
                        Log.d("Diff", "App: ${app.packageName}, Diff: $diff")
                    }

                    val updatedSnapshot = Snapshot(
                        packageName = app.packageName,
                        versionCode = app.versionCode,
                        versionName = app.versionName,
                        score = app.riskResult.score,
                        band = app.riskResult.band.name,
                        sensitivePermissions = PermissionDiff.extractSensitivePermissions(app),
                        specialAccessFlags = PermissionDiff.encodeSpecialAccessFlags(app),
                        firstSeen = oldSnapshot?.firstSeen ?: now,
                        lastScanned = now,
                        lastNotifiedKey = oldSnapshot?.lastNotifiedKey
                    )
                    updatedSnapshots[app.packageName] = updatedSnapshot
                }

                snapshotStore.saveAll(updatedSnapshots)
                snapshotStore.setBaselineCreated()
                prefs.edit().putLong("last_scanned_time", now).apply()

                _uiState.update { state ->
                    val updated = state.copy(
                        isLoading = false,
                        isRescanning = false,
                        apps = scannedApps,
                        diffs = diffsMap,
                        deviceSummary = summary,
                        lastScannedTime = now
                    )
                    updated.copy(filteredApps = computeFilteredApps(updated))
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRescanning = false,
                        errorMessage = e.message ?: "Failed to scan apps"
                    )
                }
            }
        }
    }

    private fun computeFilteredApps(state: HomeUiState): List<AppInfo> {
        var list = state.apps

        // 1. Search Filter
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }

        // 2. Band / Changed Filter
        list = when (state.selectedBandFilter) {
            BandFilter.ALL -> list
            BandFilter.FLAGGED -> list.filter { it.riskResult.band == RiskBand.HIGH || it.riskResult.band == RiskBand.CRITICAL }
            BandFilter.MEDIUM -> list.filter { it.riskResult.band == RiskBand.MEDIUM }
            BandFilter.LOW -> list.filter { it.riskResult.band == RiskBand.LOW }
            BandFilter.CHANGED -> list.filter { state.diffs[it.packageName]?.hasChanges == true }
        }

        // 3. Category Filter
        if (state.selectedCategoryFilter != null) {
            val targetCategory = state.selectedCategoryFilter
            val grantedPkgs = state.deviceSummary?.categorySummaries
                ?.firstOrNull { it.category == targetCategory }
                ?.appsWithGrantedPackageNames ?: emptySet()

            list = list.filter { it.packageName in grantedPkgs }
        }

        // 4. Sorting
        return when (state.selectedSortOption) {
            SortOption.RISK_SCORE -> list.sortedByDescending { it.riskResult.score }
            SortOption.NAME -> list.sortedBy { it.name.lowercase() }
            SortOption.RECENTLY_INSTALLED -> list.sortedByDescending { it.installTime }
            SortOption.RECENTLY_UPDATED -> list.sortedByDescending { it.updateTime }
        }
    }
}
