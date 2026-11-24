package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AnimatedGradientSpectrum
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.UrgentAmber

/**
 * Animated colorful display title with smoothly sweeping multi-stop gradient
 * and a spring entrance bounce.
 */
@Composable
fun AnimatedColorfulTitle(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineLarge,
    maxLines: Int = 2,
    colors: List<Color> = AnimatedGradientSpectrum
) {
    val infiniteTransition = rememberInfiniteTransition(label = "title_gradient_sweep")
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_offset"
    )

    var visible by remember(text) { mutableStateOf(false) }
    LaunchedEffect(text) { visible = true }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.94f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "title_entrance_scale"
    )

    val shift = shimmerProgress * 900f
    val brush = Brush.linearGradient(
        colors = colors,
        start = Offset(shift - 450f, 0f),
        end = Offset(shift + 450f, 180f)
    )

    Text(
        text = text,
        style = style.copy(brush = brush),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.scale(scale)
    )
}

/**
 * Staggered spring reveal wrapper for dashboard cards and sections.
 */
@Composable
fun AnimatedSectionReveal(
    delayIndex: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 260, delayMillis = (delayIndex * 45).coerceAtMost(240))
        ) + slideInVertically(
            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
            initialOffsetY = { it / 6 }
        ),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Tinted squircle icon badge for vibrant visual scanning.
 */
@Composable
fun ColorfulIconBadge(
    icon: ImageVector,
    tint: Color,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: Dp = 22.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(13.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(tint.copy(alpha = 0.22f), tint.copy(alpha = 0.10f))
                )
            )
            .border(1.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Monospace Institution ID badge pill.
 */
@Composable
fun InstitutionIdBadge(
    institutionId: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Text(
            text = institutionId,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

/**
 * Sync & Offline Cache status indicator pill (Section 22).
 */
@Composable
fun SyncStatusPill(
    isOffline: Boolean,
    lastSyncLabel: String,
    modifier: Modifier = Modifier
) {
    val color = if (isOffline) UrgentAmber else EmeraldSuccess
    val icon = if (isOffline) Icons.Rounded.CloudOff else Icons.Rounded.CloudDone
    val text = if (isOffline) "Cached Offline · $lastSyncLabel" else "Live Sync · $lastSyncLabel"
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.14f),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = if (isOffline) "Offline cache" else "Live sync active",
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Friendly empty state card with icon badge and guidance.
 */
@Composable
fun FriendlyEmptyState(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            ColorfulIconBadge(
                icon = icon,
                tint = tint,
                contentDescription = null,
                size = 56.dp,
                iconSize = 28.dp
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = onAction,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

/**
 * Searchable prefilled dropdown with sensible defaults + "Other / Custom Entry" (Sections 20 & 21).
 */
@Composable
fun SearchableDropdownWithCustom(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "searchable_dropdown",
    allowCustomEntry: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var customEntryMode by remember { mutableStateOf(false) }
    var customText by remember(selectedValue) { mutableStateOf(selectedValue) }

    Column(modifier = modifier) {
        if (customEntryMode) {
            OutlinedTextField(
                value = customText,
                onValueChange = {
                    customText = it
                    onValueSelected(it)
                },
                label = { Text("$label (Custom)") },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { customEntryMode = false }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Back to preset list")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTag}_custom_input")
            )
        } else {
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedValue,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(label) },
                    trailingIcon = {
                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Open $label options")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(testTag)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .minimumInteractiveComponentSize()
                        .clickable { expanded = true }
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.width(280.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search $label...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                    val filtered = options.filter {
                        it.contains(searchQuery.trim(), ignoreCase = true)
                    }
                    filtered.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            leadingIcon = if (option == selectedValue) {
                                { Icon(Icons.Rounded.Check, contentDescription = null) }
                            } else null,
                            onClick = {
                                onValueSelected(option)
                                expanded = false
                                searchQuery = ""
                            }
                        )
                    }
                    if (allowCustomEntry) {
                        DropdownMenuItem(
                            text = { Text("Other / Enter custom...") },
                            leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                            onClick = {
                                expanded = false
                                customEntryMode = true
                                customText = ""
                            }
                        )
                    }
                }
            }
        }
    }
}
