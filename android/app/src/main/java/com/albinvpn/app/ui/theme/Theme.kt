package com.albinvpn.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.albinvpn.app.data.AccentShade
import com.albinvpn.app.data.ThemeMode

private val PurpleDark = darkColorScheme(
    primary = Color(0xFFD5B8FF),
    onPrimary = Color(0xFF2D0A6E),
    primaryContainer = Color(0xFF4F2B9E),
    onPrimaryContainer = Color(0xFFEDE2FF),
    secondary = Color(0xFFCBB6F2),
    secondaryContainer = Color(0xFF392B56),
    tertiary = Color(0xFFEFB8C8),
    background = Color(0xFF0D0A14),
    surface = Color(0xFF0D0A14),
    surfaceContainer = Color(0xFF171324),
    surfaceContainerHigh = Color(0xFF221C33),
    outlineVariant = Color(0xFF382F52)
)

private val PurpleLight = lightColorScheme(
    primary = Color(0xFF6B3FD1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9DDFF),
    onPrimaryContainer = Color(0xFF22005D),
    secondary = Color(0xFF625B71),
    secondaryContainer = Color(0xFFE8DEF8),
    background = Color(0xFFFCF8FF),
    surface = Color(0xFFFCF8FF),
    surfaceContainer = Color(0xFFF2EBFA),
    surfaceContainerHigh = Color(0xFFE9DFF6),
    outlineVariant = Color(0xFFD4C6E8)
)

private val BlueDark = darkColorScheme(
    primary = Color(0xFF9ECAFF), onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D), onPrimaryContainer = Color(0xFFD1E4FF),
    background = Color(0xFF0B0F17), surface = Color(0xFF0B0F17),
    surfaceContainer = Color(0xFF151B27), surfaceContainerHigh = Color(0xFF1F2737),
    outlineVariant = Color(0xFF2F3A4F)
)

private val BlueLight = lightColorScheme(
    primary = Color(0xFF0061A4), onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF), onPrimaryContainer = Color(0xFF001D36),
    background = Color(0xFFF8FAFF), surface = Color(0xFFF8FAFF),
    surfaceContainer = Color(0xFFECF1FA), surfaceContainerHigh = Color(0xFFE1E8F5)
)

private val EmeraldDark = darkColorScheme(
    primary = Color(0xFF5CE6D3), onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048), onPrimaryContainer = Color(0xFF7BFCEB),
    background = Color(0xFF091211), surface = Color(0xFF091211),
    surfaceContainer = Color(0xFF121E1D), surfaceContainerHigh = Color(0xFF1B2B29),
    outlineVariant = Color(0xFF2A423F)
)

private val EmeraldLight = lightColorScheme(
    primary = Color(0xFF006A60), onPrimary = Color.White,
    primaryContainer = Color(0xFF74F8E5), onPrimaryContainer = Color(0xFF00201C),
    background = Color(0xFFF4FCFA), surface = Color(0xFFF4FCFA),
    surfaceContainer = Color(0xFFE7F3F0), surfaceContainerHigh = Color(0xFFDBEBE7)
)

private val RoseDark = darkColorScheme(
    primary = Color(0xFFFFB2BD), onPrimary = Color(0xFF5F1124),
    primaryContainer = Color(0xFF83223A), onPrimaryContainer = Color(0xFFFFD9DE),
    background = Color(0xFF150C10), surface = Color(0xFF150C10),
    surfaceContainer = Color(0xFF22151B), surfaceContainerHigh = Color(0xFF2F1D25),
    outlineVariant = Color(0xFF4A2E3A)
)

private val RoseLight = lightColorScheme(
    primary = Color(0xFFB53B55), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9DE), onPrimaryContainer = Color(0xFF3F0013),
    background = Color(0xFFFFF8F8), surface = Color(0xFFFFF8F8),
    surfaceContainer = Color(0xFFFCEBEF), surfaceContainerHigh = Color(0xFFF6DFE5)
)

private val AmberDark = darkColorScheme(
    primary = Color(0xFFFFC064), onPrimary = Color(0xFF472A00),
    primaryContainer = Color(0xFF663E00), onPrimaryContainer = Color(0xFFFFDDB3),
    background = Color(0xFF140F09), surface = Color(0xFF140F09),
    surfaceContainer = Color(0xFF211910), surfaceContainerHigh = Color(0xFF2E2317),
    outlineVariant = Color(0xFF4A3926)
)

private val AmberLight = lightColorScheme(
    primary = Color(0xFF965A00), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDB3), onPrimaryContainer = Color(0xFF2A1700),
    background = Color(0xFFFFF9F4), surface = Color(0xFFFFF9F4),
    surfaceContainer = Color(0xFFF8EDE1), surfaceContainerHigh = Color(0xFFF0E1D0)
)

@Composable
fun AlbinVpnTheme(
    themeMode: ThemeMode,
    accentShade: AccentShade,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme: ColorScheme = if (
        accentShade == AccentShade.SYSTEM_DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    ) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        when (accentShade) {
            AccentShade.PURPLE, AccentShade.SYSTEM_DYNAMIC -> if (darkTheme) PurpleDark else PurpleLight
            AccentShade.OCEAN_BLUE -> if (darkTheme) BlueDark else BlueLight
            AccentShade.EMERALD -> if (darkTheme) EmeraldDark else EmeraldLight
            AccentShade.ROSE -> if (darkTheme) RoseDark else RoseLight
            AccentShade.AMBER -> if (darkTheme) AmberDark else AmberLight
        }
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
