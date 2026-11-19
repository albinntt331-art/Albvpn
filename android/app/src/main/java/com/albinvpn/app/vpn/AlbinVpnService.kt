package com.albinvpn.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.TrafficStats as AndroidTrafficStats
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.albinvpn.app.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AlbinVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.albinvpn.START"
        const val ACTION_STOP = "com.albinvpn.STOP"
        const val EXTRA_PROFILE_ID = "profile_id"
        private const val CHANNEL_ID = "albinvpn_channel"

        private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
        val state: StateFlow<ConnectionState> = _state

        private val _stats = MutableStateFlow(TrafficStats())
        val stats: StateFlow<TrafficStats> = _stats

        private val _logs = MutableStateFlow(listOf("Albin-vpn готов к подключению."))
        val logs: StateFlow<List<String>> = _logs

        fun appendLog(msg: String) {
            _logs.value = (_logs.value + msg).takeLast(250)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tunInterface: ParcelFileDescriptor? = null
    private var activeEngine: VpnCoreEngine? = null
    private var monitorJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopVpn(); stopSelf() }
            ACTION_START -> startVpn(intent.getStringExtra(EXTRA_PROFILE_ID))
        }
        return START_STICKY
    }

    private fun startVpn(requestedId: String?) {
        val repo = ProfileRepository(this)
        val allProfiles = repo.loadProfiles()
        if (allProfiles.isEmpty()) {
            val err = "Нет серверов. Добавьте подписку или ключ."
            appendLog("[VPN ERROR] $err")
            _stats.value = _stats.value.copy(errorMessage = err)
            _state.value = ConnectionState.ERROR
            return
        }

        _state.value = ConnectionState.CONNECTING
        _stats.value = _stats.value.copy(errorMessage = null)
        startForeground(1, buildNotification("Поиск лучшего сервера..."))

        scope.launch {
            val settings = repo.loadSettings()
            val targetProfile: VpnProfile? = if (requestedId == null || requestedId == AUTO_PROFILE_ID) {
                pickBestAutoProfile(repo, allProfiles)
            } else {
                allProfiles.firstOrNull { it.id == requestedId } ?: pickBestAutoProfile(repo, allProfiles)
            }

            if (targetProfile == null) {
                val err = "Ни один сервер не отвечает. Проверьте ключи и интернет."
                appendLog("[VPN ERROR] $err")
                _stats.value = _stats.value.copy(errorMessage = err)
                _state.value = ConnectionState.ERROR
                stopVpn(keepError = true)
                return@launch
            }

            val displayName = if (requestedId == null || requestedId == AUTO_PROFILE_ID) {
                "Авто → ${targetProfile.name}"
            } else {
                targetProfile.name
            }
            appendLog("[Albin-vpn] Выбран сервер: $displayName (${targetProfile.protocol.uppercase()})")
            startForeground(1, buildNotification("Подключение: $displayName"))

            val builder = Builder()
                .setSession("Albin-vpn")
                .setMtu(settings.tunMtu)
                .addAddress("10.0.0.2", 30)
                .addRoute("0.0.0.0", 0)
                .addDnsServer(settings.remoteDns.ifBlank { "1.1.1.1" })
                .addDnsServer("8.8.8.8")

            if (settings.ipv6Enabled) {
                builder.addAddress("fd00::2", 126)
                builder.addRoute("::", 0)
            }

            // Always exclude Albin-vpn itself to prevent VPN routing loop!
            runCatching { builder.addDisallowedApplication(packageName) }

            // Only 2 modes: strict TUN (all apps) vs PROXY_APPS (per-app routing)
            if (settings.vpnMode == VpnMode.PROXY_APPS && settings.selectedPackages.isNotEmpty()) {
                when (settings.perAppPolicy) {
                    PerAppPolicy.ONLY_SELECTED -> {
                        settings.selectedPackages.filter { it != packageName }.forEach { pkg ->
                            runCatching { builder.addAllowedApplication(pkg) }
                        }
                        appendLog("[Mode] Proxy Apps (только выбранные: ${settings.selectedPackages.size})")
                    }
                    PerAppPolicy.EXCLUDE_SELECTED -> {
                        settings.selectedPackages.forEach { pkg ->
                            runCatching { builder.addDisallowedApplication(pkg) }
                        }
                        appendLog("[Mode] Proxy Apps (исключено: ${settings.selectedPackages.size})")
                    }
                }
            } else {
                appendLog("[Mode] TUN (Строгий — весь трафик строго в VPN)")
            }

            tunInterface = builder.establish()
            val tunFd = tunInterface?.fd ?: -1
            if (tunFd < 0) {
                val err = "Не удалось открыть TUN-интерфейс Android."
                appendLog("[VPN ERROR] $err")
                _stats.value = _stats.value.copy(errorMessage = err)
                _state.value = ConnectionState.ERROR
                stopVpn(keepError = true)
                return@launch
            }

            val configJson = ConfigBuilder.buildXrayConfig(targetProfile, settings)
            val engine = XrayCoreEngine()
            activeEngine = engine

            val started = engine.start(
                context = this@AlbinVpnService,
                configJson = configJson,
                tunFd = tunFd,
                protectFd = { fd -> protect(fd) },
                onLog = { appendLog(it) }
            )

            if (started) {
                _state.value = ConnectionState.CONNECTED
                _stats.value = _stats.value.copy(activeServerName = displayName, errorMessage = null)
                startForeground(1, buildNotification("Защищено: $displayName"))
                startTrafficMonitor()
            } else {
                val err = "Ядро Xray не запустилось. Откройте Logs в Настройках."
                _stats.value = _stats.value.copy(errorMessage = err)
                _state.value = ConnectionState.ERROR
                stopVpn(keepError = true)
            }
        }
    }

    private suspend fun pickBestAutoProfile(repo: ProfileRepository, profiles: List<VpnProfile>): VpnProfile? = coroutineScope {
        val priority = profiles.filter { it.isPriority }
        val pool = if (priority.isNotEmpty()) priority else profiles
        val measured = pool.map { p ->
            async { p to repo.measureTcpPing(p.address, p.port) }
        }.awaitAll()

        val bestFromPool = measured.filter { it.second != null }.minByOrNull { it.second!! }?.first
        if (bestFromPool != null) return@coroutineScope bestFromPool

        // Fallback to non-priority servers if all priority servers are offline
        if (priority.isNotEmpty()) {
            val rest = profiles.filterNot { it.isPriority }.map { p ->
                async { p to repo.measureTcpPing(p.address, p.port) }
            }.awaitAll()
            rest.filter { it.second != null }.minByOrNull { it.second!! }?.first ?: profiles.firstOrNull()
        } else {
            profiles.firstOrNull()
        }
    }

    private fun startTrafficMonitor() {
        monitorJob?.cancel()
        val startMs = System.currentTimeMillis()
        var lastRx = AndroidTrafficStats.getTotalRxBytes().coerceAtLeast(0L)
        var lastTx = AndroidTrafficStats.getTotalTxBytes().coerceAtLeast(0L)
        monitorJob = scope.launch {
            while (isActive && _state.value == ConnectionState.CONNECTED) {
                delay(1000)
                val nowRx = AndroidTrafficStats.getTotalRxBytes().coerceAtLeast(0L)
                val nowTx = AndroidTrafficStats.getTotalTxBytes().coerceAtLeast(0L)
                val rxMbps = ((nowRx - lastRx).coerceAtLeast(0L) * 8f) / 1_000_000f
                val txMbps = ((nowTx - lastTx).coerceAtLeast(0L) * 8f) / 1_000_000f
                lastRx = nowRx
                lastTx = nowTx
                val cur = _stats.value
                _stats.value = cur.copy(
                    rxMbps = rxMbps,
                    txMbps = txMbps,
                    rxHistory = cur.rxHistory.drop(1) + rxMbps,
                    txHistory = cur.txHistory.drop(1) + txMbps,
                    connectedSeconds = (System.currentTimeMillis() - startMs) / 1000L
                )
            }
        }
    }

    private fun stopVpn(keepError: Boolean = false) {
        monitorJob?.cancel()
        activeEngine?.stop()
        activeEngine = null
        runCatching { tunInterface?.close() }
        tunInterface = null
        if (!keepError) {
            _state.value = ConnectionState.DISCONNECTED
            _stats.value = TrafficStats()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun buildNotification(text: String): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Albin-vpn", NotificationManager.IMPORTANCE_LOW))
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Albin-vpn")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() { stopVpn(); super.onDestroy() }
}
