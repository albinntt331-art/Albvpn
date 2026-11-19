package com.albinvpn.app.data

import kotlinx.serialization.Serializable
import java.util.UUID

const val AUTO_PROFILE_ID = "__AUTO_BEST__"

@Serializable
enum class AppLanguage(val code: String, val nativeLabel: String) {
    RU("ru", "Русский"),
    EN("en", "English")
}

@Serializable
enum class VpnMode {
    TUN,        // Strict full-device VPN: all traffic forced into VPN
    PROXY_APPS  // Per-app routing: selected apps only or exclude selected apps
}

@Serializable
enum class PerAppPolicy {
    ONLY_SELECTED,
    EXCLUDE_SELECTED
}

@Serializable
enum class CoreEngineType {
    AUTO, XRAY, SING_BOX
}

@Serializable
enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

@Serializable
enum class AccentShade(val seedHex: Long) {
    PURPLE(0xFF7C4DFF),
    OCEAN_BLUE(0xFF1A73E8),
    EMERALD(0xFF00897B),
    ROSE(0xFFD81B60),
    AMBER(0xFFEF6C00),
    SYSTEM_DYNAMIC(0xFF7C4DFF)
}

@Serializable
data class VpnProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val protocol: String,
    val address: String,
    val port: Int,
    val rawUri: String,
    val subscriptionId: String? = null,
    val pingMs: Int? = null,
    val isPriority: Boolean = false,
    val params: Map<String, String> = emptyMap()
)

@Serializable
data class Subscription(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val lastUpdated: Long = System.currentTimeMillis(),
    val usedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val expireTimestamp: Long = 0L
)

@Serializable
data class AppSettings(
    val language: AppLanguage = AppLanguage.RU,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentShade: AccentShade = AccentShade.PURPLE,
    val coreEngine: CoreEngineType = CoreEngineType.AUTO,
    val vpnMode: VpnMode = VpnMode.TUN,
    val perAppPolicy: PerAppPolicy = PerAppPolicy.ONLY_SELECTED,
    val selectedPackages: Set<String> = emptySet(),
    val tunMtu: Int = 1500,
    val ipv6Enabled: Boolean = false,
    val sniffingEnabled: Boolean = true,
    val fakeDnsEnabled: Boolean = false,
    val remoteDns: String = "1.1.1.1",
    val fragmentEnabled: Boolean = false,
    val fragmentLength: String = "10-30",
    val fragmentInterval: String = "10-20",
    val muxEnabled: Boolean = false,
    val muxConcurrency: Int = 8
)

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR
}

data class TrafficStats(
    val rxMbps: Float = 0f,
    val txMbps: Float = 0f,
    val rxHistory: List<Float> = List(22) { 0f },
    val txHistory: List<Float> = List(22) { 0f },
    val connectedSeconds: Long = 0L,
    val activeServerName: String = "",
    val errorMessage: String? = null
)

data class InstalledAppInfo(
    val packageName: String,
    val label: String
)
