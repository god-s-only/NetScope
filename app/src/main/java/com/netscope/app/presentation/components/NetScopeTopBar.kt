package com.netscope.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netscope.app.domain.model.HttpMethod
import com.netscope.app.domain.model.StatusCategory
import com.netscope.app.presentation.theme.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetScopeTopBar(
    title: String,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
        },
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = NetScopeSurface,
        ),
    )
}


@Composable
fun StatusChip(code: Int?, category: StatusCategory) {
    val (bg, fg) = when (category) {
        StatusCategory.SUCCESS      -> StatusSuccess to StatusSuccessText
        StatusCategory.CLIENT_ERROR,
        StatusCategory.SERVER_ERROR,
        StatusCategory.ERROR        -> StatusError to StatusErrorText
        StatusCategory.REDIRECT     -> StatusWarning to StatusWarningText
        StatusCategory.PENDING      -> StatusPending to StatusPendingText
        else                        -> NetScopeSurface2 to TextSecondary
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = code?.toString() ?: "···",
            color = fg,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}


@Composable
fun MethodChip(method: HttpMethod) {
    val color = when (method) {
        HttpMethod.GET     -> Color(0xFF58A6FF)
        HttpMethod.POST    -> Color(0xFF3FB950)
        HttpMethod.PUT     -> Color(0xFFD29922)
        HttpMethod.DELETE  -> Color(0xFFFF5370)
        HttpMethod.PATCH   -> Color(0xFFBB8EFF)
        else               -> TextSecondary
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = method.name,
            color = color,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}


@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = TextTertiary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}


@Composable
fun StatCard(
    label: String,
    value: String,
    valueColor: Color = NetScopePrimary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(NetScopeSurface)
            .padding(16.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = valueColor,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
    }
}


@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextSecondary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary,
        )
    }
}


fun formatBytes(bytes: Long): String = when {
    bytes >= 1_048_576 -> "${"%.1f".format(bytes / 1_048_576.0)} MB"
    bytes >= 1_024     -> "${"%.1f".format(bytes / 1_024.0)} KB"
    else               -> "$bytes B"
}

fun formatDuration(ms: Long): String = when {
    ms >= 1_000 -> "${"%.2f".format(ms / 1_000.0)}s"
    else        -> "${ms}ms"
}