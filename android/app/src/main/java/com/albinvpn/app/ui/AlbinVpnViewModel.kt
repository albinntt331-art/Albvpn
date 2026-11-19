package com.albinvpn.app.ui

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.albinvpn.app.data.*
import com.albinvpn.app.vpn.AlbinVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AlbinVpnViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ProfileRepository(app)

    private val _profiles = MutableStateFlow(repo.loadProfiles())
    val profiles: StateFlow<List<VpnProfile>> = _profiles

    private val _subscriptions = MutableStateFlow(repo.loadSubscriptions())
    val subscriptions: StateFlow<List<Subscription>> = _subscriptions

    private val _settings = MutableStateFlow(repo.loadSettings())
    val settings: StateFlow<AppSettings> = _settings

    private val _selectedProfileId = MutableStateFlow(
        repo.loadSelectedProfileId() ?: AUTO_PROFILE_ID
    )
    val selectedProfileId: StateFlow<String> = _selectedProfileId

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps

    val connectionState = AlbinVpnService.state
    val trafficStats = AlbinVpnService.stats
    val logs = AlbinVpnService.logs

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val myPkg = getApplication<Application>().packageName
            val apps = pm.getInstalledApplications(0)
                .filter { it.packageName != myPkg && (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                .map { InstalledAppInfo(it.packageName, pm.getApplicationLabel(it).toString()) }
                .sortedBy { it.label.lowercase() }
            _installedApps.value = apps
        }
    }

    fun selectProfile(id: String) {
        _selectedProfileId.value = id
        repo.saveSelectedProfileId(id)
    }

    fun togglePriority(id: String) {
        val updated = _profiles.value.map { if (it.id == id) it.copy(isPriority = !it.isPriority) else it }
        _profiles.value = updated
        repo.saveProfiles(updated)
    }

    fun toggleAppSelected(packageName: String) {
        updateSettings { s ->
            val next = if (packageName in s.selectedPackages) s.selectedPackages - packageName else s.selectedPackages + packageName
            s.copy(selectedPackages = next)
        }
    }

    fun importRawKeys(text: String): Int {
        val parsed = UriParser.parseBatch(text)
        if (parsed.isEmpty()) return 0
        val updated = _profiles.value + parsed
        _profiles.value = updated
        repo.saveProfiles(updated)
        return parsed.size
    }

    fun addSubscription(name: String, url: String) {
        val sub = Subscription(name = name.ifBlank { "Subscription" }, url = url.trim())
        val subs = _subscriptions.value + sub
        _subscriptions.value = subs
        repo.saveSubscriptions(subs)
        refreshSubscription(sub)
    }

    fun refreshSubscription(sub: Subscription) {
        viewModelScope.launch {
            runCatching {
                val (updatedSub, newProfiles) = repo.fetchSubscription(sub)
                _subscriptions.value = _subscriptions.value.map { if (it.id == sub.id) updatedSub else it }
                repo.saveSubscriptions(_subscriptions.value)
                val merged = _profiles.value.filter { it.subscriptionId != sub.id } + newProfiles
                _profiles.value = merged
                repo.saveProfiles(merged)
                AlbinVpnService.appendLog("[Sub] ${updatedSub.name}: ${newProfiles.size} servers")
            }.onFailure {
                AlbinVpnService.appendLog("[Sub ERROR] ${it.message}")
            }
        }
    }

    fun deleteProfile(id: String) {
        val updated = _profiles.value.filterNot { it.id == id }
        _profiles.value = updated
        repo.saveProfiles(updated)
        if (_selectedProfileId.value == id) selectProfile(AUTO_PROFILE_ID)
    }

    fun pingAllProfiles() {
        viewModelScope.launch {
            val updated = _profiles.value.map { p -> p.copy(pingMs = repo.measureTcpPing(p.address, p.port)) }
            _profiles.value = updated
            repo.saveProfiles(updated)
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        repo.saveSettings(updated)
    }
}
