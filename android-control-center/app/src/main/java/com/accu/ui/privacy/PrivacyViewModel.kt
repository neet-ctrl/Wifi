package com.accu.ui.privacy

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accu.data.db.dao.BlockedComponentDao
import com.accu.data.db.dao.PrivacyRuleDao
import com.accu.data.db.entities.BlockedComponentEntity
import com.accu.data.db.entities.PrivacyRuleEntity
import com.accu.data.repositories.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class PrivacyUiState(
    val blockedComponents: List<BlockedComponentEntity> = emptyList(),
    val privacyRules: List<PrivacyRuleEntity> = emptyList(),
    val trackerCount: Int = 0,
    val blockedCount: Int = 0,
    val trackerCategories: List<TrackerCategory> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedTab: PrivacyTab = PrivacyTab.DASHBOARD,
    val snackbarMessage: String? = null,
)

data class TrackerCategory(
    val name: String,
    val trackerCount: Int,
    val description: String,
    val packages: List<String>,
)

enum class PrivacyTab { DASHBOARD, TRACKERS, COMPONENTS, RULES }

@HiltViewModel
class PrivacyViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val blockedComponentDao: BlockedComponentDao,
    private val privacyRuleDao: PrivacyRuleDao,
    private val appRepository: AppRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PrivacyUiState())
    val state: StateFlow<PrivacyUiState> = _state.asStateFlow()

    // Built-in tracker signatures (from Blocker's rules database)
    private val builtInTrackers = mapOf(
        "Analytics" to listOf("com.google.firebase.analytics", "com.amplitude.api", "io.segment", "com.mixpanel", "com.flurry", "com.crashlytics", "com.appsflyer", "io.branch"),
        "Ads" to listOf("com.google.android.gms.ads", "com.facebook.ads", "com.unity3d.ads", "com.applovin", "com.mopub", "com.chartboost"),
        "Social" to listOf("com.facebook.share", "com.twitter.sdk", "com.linkedin.android"),
        "Crash Reporting" to listOf("com.bugsnag", "com.instabug", "io.sentry", "com.rollbar"),
        "Profiling" to listOf("com.contentsquare", "com.hotjar", "io.embrace"),
    )

    init {
        observeBlockedComponents()
        observePrivacyRules()
        buildTrackerCategories()
    }

    private fun observeBlockedComponents() {
        viewModelScope.launch {
            blockedComponentDao.observeAll().collect { list ->
                _state.update { it.copy(blockedComponents = list, blockedCount = list.size, trackerCount = list.count { c -> c.isTracker }, isLoading = false) }
            }
        }
    }

    private fun observePrivacyRules() {
        viewModelScope.launch { privacyRuleDao.observeAll().collect { rules -> _state.update { it.copy(privacyRules = rules) } } }
    }

    private fun buildTrackerCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val installedPkgs = pm.getInstalledPackages(PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS).map { it.packageName }.toSet()
            val categories = builtInTrackers.map { (cat, trackerPkgs) ->
                TrackerCategory(
                    name = cat,
                    trackerCount = trackerPkgs.size,
                    description = "Block $cat trackers",
                    packages = trackerPkgs,
                )
            }
            _state.update { it.copy(trackerCategories = categories) }
        }
    }

    fun blockTrackersInCategory(category: String) {
        viewModelScope.launch {
            val trackers = builtInTrackers[category] ?: return@launch
            var blocked = 0
            trackers.forEach { pkg ->
                try {
                    blockedComponentDao.insert(BlockedComponentEntity(
                        packageName = pkg,
                        componentName = pkg,
                        componentType = "tracker",
                        isTracker = true,
                        ruleSource = "built_in",
                    ))
                    blocked++
                } catch (e: Exception) { Timber.e(e) }
            }
            _state.update { it.copy(snackbarMessage = "Blocked $blocked trackers in $category") }
        }
    }

    fun enableComponent(packageName: String, componentName: String) {
        viewModelScope.launch {
            val ok = appRepository.enableComponent(packageName, componentName)
            _state.update { it.copy(snackbarMessage = if (ok) "Component enabled" else "Failed") }
        }
    }

    fun disableComponent(packageName: String, componentName: String, type: String) {
        viewModelScope.launch {
            val ok = appRepository.disableComponent(packageName, componentName, type)
            _state.update { it.copy(snackbarMessage = if (ok) "Component disabled" else "Failed") }
        }
    }

    fun addPrivacyRule(packageName: String, ruleType: String, ruleName: String) {
        viewModelScope.launch {
            privacyRuleDao.insert(PrivacyRuleEntity(packageName = packageName, ruleType = ruleType, ruleName = ruleName))
        }
    }

    fun deleteRule(rule: PrivacyRuleEntity) { viewModelScope.launch { privacyRuleDao.delete(rule) } }
    fun onTabChange(tab: PrivacyTab) { _state.update { it.copy(selectedTab = tab) } }
    fun onSearch(q: String) { _state.update { it.copy(searchQuery = q) } }
    fun clearSnackbar() { _state.update { it.copy(snackbarMessage = null) } }
}
