package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CampusEvent
import com.example.model.EventKind
import com.example.model.EventStatus
import com.example.model.Institution
import com.example.model.InstitutionMembership
import com.example.model.Notice
import com.example.model.NoticeLifecycleStatus
import com.example.model.NoticePriority
import com.example.ui.components.AnimatedColorfulTitle
import com.example.ui.components.AnimatedSectionReveal
import com.example.ui.components.ColorfulIconBadge
import com.example.ui.components.FriendlyEmptyState
import com.example.ui.components.InstitutionIdBadge
import com.example.ui.components.SyncStatusPill
import com.example.ui.theme.AccentPreset
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.TealAccent

@Composable
fun HomeTab(
    institution: Institution,
    membership: InstitutionMembership,
    notices: List<Notice>,
    events: List<CampusEvent>,
    dismissedEmergencyKeys: Set<String>,
    lastSyncLabel: String,
    onDismissEmergency: (Notice) -> Unit,
    onOpenNotice: (Notice) -> Unit,
    onGoToNotices: () -> Unit,
    onGoToEvents: () -> Unit
) {
    val preset = AccentPreset.fromKey(institution.accentColorKey)
    val activeNotices = notices.filter {
        it.lifecycleEnum() in listOf(NoticeLifecycleStatus.PUBLISHED, NoticeLifecycleStatus.UPDATED)
    }
    val emergencyNotices = activeNotices.filter {
        it.priorityEnum() == NoticePriority.EMERGENCY &&
            !dismissedEmergencyKeys.contains("${it.id}:${it.revisionNumber}")
    }
    val majorNotices = activeNotices.filter { it.visibilityTargets.contains("MAJOR_BOARD") }
    val deptKey = "DEPT_${membership.department}"
    val deptNotices = activeNotices.filter {
        it.visibilityTargets.contains(deptKey) || it.originDepartment.equals(membership.department, ignoreCase = true)
    }
    val upcomingEvents = events.filter { it.statusEnum() in listOf(EventStatus.UPCOMING, EventStatus.ONGOING) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Institution Hero Card
        item {
            AnimatedSectionReveal(0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    preset.primary.copy(alpha = 0.16f),
                                    preset.secondary.copy(alpha = 0.10f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .border(1.dp, preset.primary.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                ColorfulIconBadge(
                                    icon = Icons.Rounded.AccountBalance,
                                    tint = preset.primary,
                                    contentDescription = null,
                                    size = 46.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    AnimatedColorfulTitle(
                                        text = institution.name,
                                        style = MaterialTheme.typography.headlineMedium,
                                        maxLines = 2
                                    )
                                    Text(
                                        text = institution.tagline,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            InstitutionIdBadge(institution.institutionId)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = preset.secondary.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = membership.roleEnum().label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = preset.secondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            SyncStatusPill(isOffline = false, lastSyncLabel = lastSyncLabel)
                        }
                    }
                }
            }
        }

        // Animated Emergency Banner(s)
        items(emergencyNotices, key = { "emg_${it.id}" }) { emg ->
            val pulse by rememberInfiniteTransition(label = "emg_pulse").animateFloat(
                initialValue = 0.72f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                label = "emg_alpha"
            )
            AnimatedVisibility(
                visible = true,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    onClick = { onOpenNotice(emg) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, EmergencyRed.copy(alpha = pulse), RoundedCornerShape(20.dp))
                        .testTag("emergency_banner_${emg.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = "Emergency alert",
                            tint = EmergencyRed,
                            modifier = Modifier.size(28.dp).alpha(pulse)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "EMERGENCY · REV #${emg.revisionNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmergencyRed,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = emg.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { onDismissEmergency(emg) }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Dismiss alert", tint = EmergencyRed)
                        }
                    }
                }
            }
        }

        // Today & Coming Soon Carousel
        item {
            AnimatedSectionReveal(1) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = AmberWarm, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Today & Coming Soon", style = MaterialTheme.typography.titleLarge)
                        }
                        TextButton(onClick = onGoToEvents) {
                            Text("All Events")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    if (activeNotices.isEmpty() && upcomingEvents.isEmpty()) {
                        FriendlyEmptyState(
                            icon = Icons.Rounded.Campaign,
                            tint = preset.primary,
                            title = "Your Campus Board is Ready",
                            subtitle = "Official circulars, department updates, and upcoming campus events will appear here as soon as they are published."
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(activeNotices.take(4), key = { "today_n_${it.id}" }) { notice ->
                                TodayHighlightCard(
                                    badgeText = notice.category,
                                    title = notice.title,
                                    subtitle = "${notice.originDepartment} · ${notice.targetAudience}",
                                    tint = when (notice.priorityEnum()) {
                                        NoticePriority.EMERGENCY -> EmergencyRed
                                        NoticePriority.URGENT -> AmberWarm
                                        NoticePriority.NORMAL -> preset.primary
                                    },
                                    icon = if (notice.pinned) Icons.Rounded.PushPin else Icons.Rounded.Campaign,
                                    onClick = { onOpenNotice(notice) }
                                )
                            }
                            items(upcomingEvents.take(4), key = { "today_e_${it.id}" }) { ev ->
                                TodayHighlightCard(
                                    badgeText = ev.subType,
                                    title = ev.title,
                                    subtitle = "${ev.scheduleText} · ${ev.venue}",
                                    tint = if (ev.kindEnum() == EventKind.SPORT) TealAccent else IndigoPrimary,
                                    icon = if (ev.kindEnum() == EventKind.SPORT) Icons.Rounded.EmojiEvents else Icons.Rounded.EventAvailable,
                                    onClick = onGoToEvents
                                )
                            }
                        }
                    }
                }
            }
        }

        // Major Board Latest
        if (majorNotices.isNotEmpty()) {
            item {
                AnimatedSectionReveal(2) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Major Board", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = onGoToNotices) {
                            Text("View All (${majorNotices.size})")
                        }
                    }
                }
            }
            items(majorNotices.take(3), key = { "major_${it.id}" }) { notice ->
                NoticeCompactCard(notice = notice, accent = preset.primary, onClick = { onOpenNotice(notice) })
            }
        }

        // Department Updates
        if (deptNotices.isNotEmpty()) {
            item {
                AnimatedSectionReveal(3) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${membership.department.ifBlank { "Department" }} Updates", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = onGoToNotices) { Text("Department Board") }
                    }
                }
            }
            items(deptNotices.take(3), key = { "dept_${it.id}" }) { notice ->
                NoticeCompactCard(notice = notice, accent = preset.secondary, onClick = { onOpenNotice(notice) })
            }
        }
    }
}

@Composable
private fun TodayHighlightCard(
    badgeText: String,
    title: String,
    subtitle: String,
    tint: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.width(240.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorfulIconBadge(icon = icon, tint = tint, contentDescription = null, size = 34.dp, iconSize = 18.dp)
                Spacer(Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = tint.copy(alpha = 0.14f)) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = tint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NoticeCompactCard(
    notice: Notice,
    accent: Color,
    onClick: () -> Unit
) {
    val priorityTint = when (notice.priorityEnum()) {
        NoticePriority.EMERGENCY -> EmergencyRed
        NoticePriority.URGENT -> AmberWarm
        NoticePriority.NORMAL -> accent
    }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            ColorfulIconBadge(
                icon = if (notice.pinned) Icons.Rounded.PushPin else Icons.Rounded.Campaign,
                tint = priorityTint,
                contentDescription = null,
                size = 40.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notice.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${notice.originDepartment} · ${notice.category} · Rev #${notice.revisionNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
