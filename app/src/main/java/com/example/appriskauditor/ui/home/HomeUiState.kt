package com.example.appriskauditor.ui.home

import com.example.appriskauditor.domain.model.AppInfo
import com.example.appriskauditor.domain.model.Category
import com.example.appriskauditor.domain.scoring.DiffResult
import com.example.appriskauditor.domain.summary.DeviceSummaryResult

enum class BandFilter {
    ALL,
    FLAGGED,
    MEDIUM,
    LOW,
    CHANGED
}

enum class SortOption {
    RISK_SCORE,
    NAME,
    RECENTLY_INSTALLED,
    RECENTLY_UPDATED
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val isRescanning: Boolean = false,
    val includeSystemApps: Boolean = false,
    val notifyRiskyApps: Boolean = true,
    val searchQuery: String = "",
    val selectedBandFilter: BandFilter = BandFilter.ALL,
    val selectedCategoryFilter: Category? = null,
    val selectedSortOption: SortOption = SortOption.RISK_SCORE,
    val apps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val diffs: Map<String, DiffResult> = emptyMap(),
    val deviceSummary: DeviceSummaryResult? = null,
    val lastScannedTime: Long = 0L,
    val showFirstRunDialog: Boolean = false,
    val showDemoDialog: Boolean = false,
    val errorMessage: String? = null
)
