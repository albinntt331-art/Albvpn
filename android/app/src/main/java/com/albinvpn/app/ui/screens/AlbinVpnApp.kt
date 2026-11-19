package com.albinvpn.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.albinvpn.app.data.*
import com.albinvpn.app.ui.AlbinVpnViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbinVpnApp(
    vm: AlbinVpnViewModel,
    onToggleVpn: () -> Unit
) {
    var currentTab by remember { mutableIntStateOf(0) }
    val settings by vm.settings.collectAsState()
    val profiles by vm.profiles.collectAsState()
    val selectedId by vm.selectedProfileId.collectAsState()
    val connState by vm.connectionState.collectAsState()
    val stats by vm.trafficStats.collectAsState()
    val subs by vm.subscriptions.collectAsState()
    val installedApps by vm.installedApps.collectAsState()
    val logs by vm.logs.collectAsState()

    val str = remember(settings.language) { stringsFor(settings.language) }
    val selectedProfile = profiles.firstOrNull { it.id == selectedId }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Shield, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Albin-vpn",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.4).sp
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        onClick = { currentTab = 3 },
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = if (settings.vpnMode == VpnMode.TUN) "TUN" else "Proxy Apps",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0, onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Rounded.Shield, null) }, label = { Text(str.navHome) }
                )
                NavigationBarItem(
                    selected = currentTab == 1, onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Rounded.Public, null) }, label = { Text(str.navServers) }
                )
                NavigationBarItem(
                    selected = currentTab == 2, onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Rounded.Apps, null) }, label = { Text(str.navApps) }
                )
                NavigationBarItem(
                    selected = currentTab == 3, onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Rounded.Tune, null) }, label = { Text(str.navSettings) }
                )
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = {
                (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 24 })
                    .togetherWith(fadeOut(tween(150)))
            },
            modifier = Modifier.padding(padding).fillMaxSize(),
            label = "tabTransition"
        ) { tab ->
            when (tab) {
                0 -> HomeScreen(
                    str = str,
                    connState = connState,
                    stats = stats,
                    selectedId = selectedId,
                    activeProfile = selectedProfile,
                    settings = settings,
                    onToggleVpn = onToggleVpn,
                    onOpenServers = { currentTab = 1 },
                    onModeChange = { m -> vm.updateSettings { it.copy(vpnMode = m) } }
                )
                1 -> ServersScreen(
                    str = str,
                    profiles = profiles,
                    subscriptions = subs,
                    selectedId = selectedId,
                    onSelect = vm::selectProfile,
                    onTogglePriority = vm::togglePriority,
                    onDelete = vm::deleteProfile,
                    onImportRaw = vm::importRawKeys,
                    onAddSub = vm::addSubscription,
                    onRefreshSub = vm::refreshSubscription,
                    onPingAll = vm::pingAllProfiles
                )
                2 -> AppsRoutingScreen(
                    str = str,
                    settings = settings,
                    apps = installedApps,
                    onToggleApp = vm::toggleAppSelected,
                    onPolicyChange = { p -> vm.updateSettings { it.copy(perAppPolicy = p) } }
                )
                3 -> SettingsScreen(
                    str = str,
                    settings = settings,
                    logs = logs,
                    onUpdate = vm::updateSettings
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    str: UiStrings,
    connState: ConnectionState,
    stats: TrafficStats,
    selectedId: String,
    activeProfile: VpnProfile?,
    settings: AppSettings,
    onToggleVpn: () -> Unit,
    onOpenServers: () -> Unit,
    onModeChange: (VpnMode) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isConnected = connState == ConnectionState.CONNECTED
    val isConnecting = connState == ConnectionState.CONNECTING
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    val inf = rememberInfiniteTransition(label = "orbAnim")
    val orbitAngle by inf.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(if (isConnecting) 1400 else 5200, easing = LinearEasing)),
        label = "orbit"
    )
    val pulse1 by inf.animateFloat(
        initialValue = 1.0f, targetValue = if (isConnected) 1.28f else 1.05f,
        animationSpec = infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse1"
    )
    val pulse2 by inf.animateFloat(
        initialValue = 1.06f, targetValue = if (isConnected) 1.44f else 1.09f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse2"
    )
    val btnScale by animateFloatAsState(
        targetValue = if (isConnected) 1.03f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "btnScale"
    )
    val auraAlpha by animateFloatAsState(
        targetValue = if (isConnected) 0.34f else if (isConnecting) 0.24f else 0.09f,
        animationSpec = tween(700),
        label = "auraAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = auraAlpha), Color.Transparent),
                    center = Offset(540f, 520f),
                    radius = 850f
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Google-style 2-mode pill selector: TUN vs Proxy Apps
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = settings.vpnMode == VpnMode.TUN,
                    onClick = { onModeChange(VpnMode.TUN) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    icon = { Icon(Icons.Rounded.VpnLock, null, Modifier.size(18.dp)) }
                ) { Text(str.modeTun, maxLines = 1) }
                SegmentedButton(
                    selected = settings.vpnMode == VpnMode.PROXY_APPS,
                    onClick = { onModeChange(VpnMode.PROXY_APPS) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    icon = { Icon(Icons.Rounded.Apps, null, Modifier.size(18.dp)) }
                ) { Text(str.modeProxyApps, maxLines = 1) }
            }

            // Expressive Multi-Layer Animated Power Orb
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(258.dp)) {
                    Box(
                        modifier = Modifier
                            .size(214.dp)
                            .scale(pulse2)
                            .clip(CircleShape)
                            .background(primary.copy(alpha = if (isConnected) 0.09f else 0.03f))
                    )
                    Box(
                        modifier = Modifier
                            .size(192.dp)
                            .scale(pulse1)
                            .clip(CircleShape)
                            .background(primary.copy(alpha = if (isConnected) 0.16f else 0.06f))
                    )
                    // Rotating Luminous Sweep Ring
                    Box(
                        modifier = Modifier
                            .size(184.dp)
                            .rotate(orbitAngle)
                            .border(
                                width = if (isConnected || isConnecting) 3.dp else 1.5.dp,
                                brush = Brush.sweepGradient(
                                    listOf(primary, primary.copy(alpha = 0.08f), MaterialTheme.colorScheme.tertiary, primary)
                                ),
                                shape = RoundedCornerShape(52.dp)
                            )
                    )
                    // Interactive Squircle Button
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleVpn()
                        },
                        shape = RoundedCornerShape(48.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .size(170.dp)
                            .scale(btnScale)
                            .clip(RoundedCornerShape(48.dp))
                            .background(
                                if (isConnected) Brush.linearGradient(listOf(primary, primaryContainer))
                                else Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainer)
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val fg = if (isConnected) MaterialTheme.colorScheme.onPrimary else primary
                            Icon(Icons.Rounded.PowerSettingsNew, null, tint = fg, modifier = Modifier.size(58.dp))
                            Spacer(Modifier.height(6.dp))
                            AnimatedContent(targetState = connState, label = "stateTxt") { st ->
                                Text(
                                    text = when (st) {
                                        ConnectionState.DISCONNECTED -> str.connect
                                        ConnectionState.CONNECTING -> str.connecting
                                        ConnectionState.CONNECTED -> str.connected
                                        ConnectionState.ERROR -> str.error
                                    },
                                    color = fg,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                            }
                            if (isConnected) {
                                val h = stats.connectedSeconds / 3600
                                val m = (stats.connectedSeconds % 3600) / 60
                                val s = stats.connectedSeconds % 60
                                Text("%02d:%02d:%02d".format(h, m, s), color = fg.copy(alpha = 0.85f), fontSize = 13.sp)
                            }
                        }
                    }
                }

                stats.errorMessage?.let { err ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                } ?: Text(
                    text = if (isConnected && stats.activeServerName.isNotBlank()) stats.activeServerName
                    else if (settings.vpnMode == VpnMode.TUN) str.modeTunDesc else str.modeProxyAppsDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Live Smooth Bézier Traffic Sparklines
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TrafficCard(str.download, "%.1f Mbps".format(stats.rxMbps), stats.rxHistory, primary, Modifier.weight(1f))
                TrafficCard(str.upload, "%.1f Mbps".format(stats.txMbps), stats.txHistory, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
            }

            // Google-style Active Server Pill Card
            Surface(
                onClick = onOpenServers,
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, if (isConnected) primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (selectedId == AUTO_PROFILE_ID) Icons.Rounded.AutoAwesome else Icons.Rounded.Public,
                                null,
                                tint = primary
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedId == AUTO_PROFILE_ID) str.autoServerTitle
                            else activeProfile?.name ?: str.autoServerTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (selectedId == AUTO_PROFILE_ID) str.autoServerDesc
                            else activeProfile?.let { "${it.protocol.uppercase()} • ${it.address}:${it.port}" } ?: str.noServersYet,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun TrafficCard(title: String, value: String, points: List<Float>, accent: Color, modifier: Modifier) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent)
            Spacer(Modifier.height(6.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(34.dp)) {
                val maxVal = (points.maxOrNull() ?: 1f).coerceAtLeast(1f)
                val step = if (points.size > 1) size.width / (points.size - 1) else size.width
                val line = Path()
                val fill = Path()
                points.forEachIndexed { i, v ->
                    val x = i * step
                    val y = size.height - (v / maxVal) * (size.height * 0.85f) - 2f
                    if (i == 0) {
                        line.moveTo(x, y); fill.moveTo(x, size.height); fill.lineTo(x, y)
                    } else {
                        val prevX = (i - 1) * step
                        val prevY = size.height - (points[i - 1] / maxVal) * (size.height * 0.85f) - 2f
                        val cx = (prevX + x) / 2f
                        line.cubicTo(cx, prevY, cx, y, x, y)
                        fill.cubicTo(cx, prevY, cx, y, x, y)
                    }
                }
                fill.lineTo(size.width, size.height); fill.close()
                drawPath(fill, Brush.verticalGradient(listOf(accent.copy(alpha = 0.35f), Color.Transparent)))
                drawPath(line, accent, style = Stroke(width = 3f, cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
fun ServersScreen(
    str: UiStrings,
    profiles: List<VpnProfile>,
    subscriptions: List<Subscription>,
    selectedId: String,
    onSelect: (String) -> Unit,
    onTogglePriority: (String) -> Unit,
    onDelete: (String) -> Unit,
    onImportRaw: (String) -> Int,
    onAddSub: (String, String) -> Unit,
    onRefreshSub: (Subscription) -> Unit,
    onPingAll: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var showAddSub by remember { mutableStateOf(false) }
    var showAddKey by remember { mutableStateOf(false) }
    var subName by remember { mutableStateOf("") }
    var subUrl by remember { mutableStateOf("") }
    var keyText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { showAddSub = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Link, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(str.addSub)
            }
            FilledTonalButton(onClick = { showAddKey = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.VpnKey, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(str.addKey)
            }
            IconButton(onClick = { clipboard.getText()?.text?.let { onImportRaw(it) } }) { Icon(Icons.Rounded.ContentPaste, str.paste) }
            IconButton(onClick = onPingAll) { Icon(Icons.Rounded.Speed, str.pingAll) }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            // Built-in automatic "Auto" best-server selector ALWAYS at top!
            item {
                val isAuto = selectedId == AUTO_PROFILE_ID
                Surface(
                    onClick = { onSelect(AUTO_PROFILE_ID) },
                    shape = RoundedCornerShape(24.dp),
                    color = if (isAuto) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.5.dp, if (isAuto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), modifier = Modifier.size(42.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(str.autoServerTitle, fontWeight = FontWeight.Bold)
                            Text(str.autoServerDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isAuto) Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (subscriptions.isNotEmpty()) {
                item { Text(str.subscriptions, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                items(subscriptions, key = { it.id }) { sub ->
                    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(sub.name, fontWeight = FontWeight.Bold)
                                Text(sub.url, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { onRefreshSub(sub) }) { Icon(Icons.Rounded.Refresh, null) }
                        }
                    }
                }
            }

            item { Text("${str.servers} (${profiles.size})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
            items(profiles, key = { it.id }) { p ->
                val selected = p.id == selectedId
                Surface(
                    onClick = { onSelect(p.id) },
                    shape = RoundedCornerShape(22.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onTogglePriority(p.id) }) {
                            Icon(
                                if (p.isPriority) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = str.priorityOnly,
                                tint = if (p.isPriority) Color(0xFFFFCA28) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(p.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${p.protocol.uppercase()} • ${p.address}:${p.port}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        p.pingMs?.let { ms ->
                            Text("${ms}ms", fontWeight = FontWeight.Bold, color = Color(0xFF69F0AE), fontSize = 12.sp)
                            Spacer(Modifier.width(4.dp))
                        }
                        IconButton(onClick = { onDelete(p.id) }) { Icon(Icons.Rounded.DeleteOutline, null) }
                    }
                }
            }
        }
    }

    if (showAddSub) {
        AlertDialog(
            onDismissRequest = { showAddSub = false },
            title = { Text(str.addSub) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = subName, onValueChange = { subName = it }, label = { Text(str.subName) })
                    OutlinedTextField(value = subUrl, onValueChange = { subUrl = it }, label = { Text(str.subUrl) })
                }
            },
            confirmButton = { Button(onClick = { onAddSub(subName, subUrl); subName = ""; subUrl = ""; showAddSub = false }) { Text(str.add) } },
            dismissButton = { TextButton(onClick = { showAddSub = false }) { Text(str.cancel) } }
        )
    }

    if (showAddKey) {
        AlertDialog(
            onDismissRequest = { showAddKey = false },
            title = { Text(str.addKey) },
            text = { OutlinedTextField(value = keyText, onValueChange = { keyText = it }, label = { Text("vless://, vmess://, trojan://, ss://...") }, minLines = 3) },
            confirmButton = { Button(onClick = { onImportRaw(keyText); keyText = ""; showAddKey = false }) { Text(str.importBtn) } },
            dismissButton = { TextButton(onClick = { showAddKey = false }) { Text(str.cancel) } }
        )
    }
}

@Composable
fun AppsRoutingScreen(
    str: UiStrings,
    settings: AppSettings,
    apps: List<InstalledAppInfo>,
    onToggleApp: (String) -> Unit,
    onPolicyChange: (PerAppPolicy) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(str.appsRoutingTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(str.appsRoutingHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = settings.perAppPolicy == PerAppPolicy.ONLY_SELECTED,
                onClick = { onPolicyChange(PerAppPolicy.ONLY_SELECTED) },
                shape = SegmentedButtonDefaults.itemShape(0, 2)
            ) { Text(str.onlySelectedApps, maxLines = 1, fontSize = 12.sp) }
            SegmentedButton(
                selected = settings.perAppPolicy == PerAppPolicy.EXCLUDE_SELECTED,
                onClick = { onPolicyChange(PerAppPolicy.EXCLUDE_SELECTED) },
                shape = SegmentedButtonDefaults.itemShape(1, 2)
            ) { Text(str.excludeSelectedApps, maxLines = 1, fontSize = 12.sp) }
        }

        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            placeholder = { Text(str.searchApps) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "${str.selectedCount}: ${settings.selectedPackages.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.packageName }) { app ->
                val checked = app.packageName in settings.selectedPackages
                Surface(
                    onClick = { onToggleApp(app.packageName) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(app.label, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        Checkbox(checked = checked, onCheckedChange = { onToggleApp(app.packageName) })
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    str: UiStrings,
    settings: AppSettings,
    logs: List<String>,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Language Selector (RU / EN)
        item {
            Text(str.language, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AppLanguage.entries.forEachIndexed { idx, lang ->
                    SegmentedButton(
                        selected = settings.language == lang,
                        onClick = { onUpdate { it.copy(language = lang) } },
                        shape = SegmentedButtonDefaults.itemShape(idx, AppLanguage.entries.size)
                    ) { Text(lang.nativeLabel) }
                }
            }
        }

        // Theme & Accent
        item {
            Text(str.appearance, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val labels = listOf(str.themeSystem, str.themeLight, str.themeDark)
                ThemeMode.entries.forEachIndexed { idx, mode ->
                    SegmentedButton(
                        selected = settings.themeMode == mode,
                        onClick = { onUpdate { it.copy(themeMode = mode) } },
                        shape = SegmentedButtonDefaults.itemShape(idx, ThemeMode.entries.size)
                    ) { Text(labels[idx]) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(str.accentColor, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                AccentShade.entries.forEach { shade ->
                    val selected = settings.accentShade == shade
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(shade.seedHex))
                            .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.onBackground else Color.White.copy(alpha = 0.25f), CircleShape)
                            .clickable { onUpdate { it.copy(accentShade = shade) } },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White)
                    }
                }
            }
        }

        item { Text(str.coreAndNetwork, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item { SettingSwitchCard(str.fragmentTitle, str.fragmentDesc, settings.fragmentEnabled) { v -> onUpdate { it.copy(fragmentEnabled = v) } } }
        item { SettingSwitchCard(str.muxTitle, str.muxDesc, settings.muxEnabled) { v -> onUpdate { it.copy(muxEnabled = v) } } }
        item { SettingSwitchCard(str.sniffingTitle, str.sniffingDesc, settings.sniffingEnabled) { v -> onUpdate { it.copy(sniffingEnabled = v) } } }
        item { SettingSwitchCard(str.ipv6Title, str.ipv6Desc, settings.ipv6Enabled) { v -> onUpdate { it.copy(ipv6Enabled = v) } } }

        item {
            Text(str.logsTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    logs.takeLast(18).forEach { line -> Text(line, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchCard(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}
