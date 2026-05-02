package com.netscope.app.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary         = NetScopePrimary,
    onPrimary       = Color(0xFF003731),
    secondary       = NetScopeSecondary,
    onSecondary     = Color(0xFF003731),
    background      = NetScopeBackground,
    onBackground    = TextPrimary,
    surface         = NetScopeSurface,
    onSurface       = TextPrimary,
    surfaceVariant  = NetScopeSurface2,
    onSurfaceVariant= TextSecondary,
    error           = NetScopeError,
    onError         = Color.White,
    outline         = TextTertiary,
)

@Composable
fun NetScopeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = NetScopeTypography,
        content     = content,
    )
}