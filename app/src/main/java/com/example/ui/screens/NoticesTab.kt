package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.model.AppPermission
import com.example.model.Institution
import com.example.model.InstitutionMembership
import com.example.model.Notice
import com.example.model.NoticeAttachment
import com.example.model.NoticeLifecycleStatus
import com.example.model.NoticePriority
import com.example.ui.components.ColorfulIconBadge
import com.example.ui.components.FriendlyEmptyState
import com.example.ui.theme.AccentPreset
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.TealAccent
import com.example.viewmodel.CampusViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoticesTab(
    institution: Institution,
    membership: InstitutionMembership,
    notices: List<Notice>,
    onSelectNotice: (Notice) -> Unit,
    onCreateNotice: () -> Unit
) {
    val preset = AccentPreset.fromKey(institution.accentColorKey)
    var boardTab by remember { mutableIntStateOf(0) } // 0=Major, 1=Departments, 2=Pinned, 3=Archives
    var searchQuery by remember { mutableStateOf("") }
    var selectedDept by remember { mutableStateOf(institution.departments.firstOrNull() ?: "CSE") }
    var selectedCategory by remember { mutableStateOf("All") }

    val canCreate = membership.hasPermission(AppPermission.CREATE_NOTICE) ||
        membership.hasPermission(AppPermission.PUBLISH_MAJOR_BOARD) ||
        membership.hasPermission(AppPermission.PUBLISH_DEPT_BOARD)

    val filteredNotices = remember(notices, boardTab, searchQuery, selectedDept, selectedCategory) {
        notices.filter { notice ->
            val isArchivedOrWithdrawn = notice.lifecycleEnum() in listOf(
                NoticeLifecycleStatus.WITHDRAWN,
                NoticeLifecycleStatus.EXPIRED,
                NoticeLifecycleStatus.ARCHIVED
            )
            val boardMatch = when (boardTab) {
                0 -> !isArchivedOrWithdrawn && notice.visibilityTargets.contains("MAJOR_BOARD")
                1 -> !isArchivedOrWithdrawn && (notice.visibilityTargets.contains("DEPT_$selectedDept") ||
                    notice.originDepartment.equals(selectedDept, ignoreCase = true))
                2 -> !isArchivedOrWithdrawn && notice.pinned
                else -> isArchivedOrWithdrawn
            }
            val categoryMatch = selectedCategory == "All" || notice.category.equals(selectedCategory, ignoreCase = true)
            val queryMatch = searchQuery.isBlank() ||
                notice.title.contains(searchQuery.trim(), ignoreCase = true) ||
                notice.description.contains(searchQuery.trim(), ignoreCase = true) ||
                notice.originDepartment.contains(searchQuery.trim(), ignoreCase = true)
            boardMatch && categoryMatch && queryMatch
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = boardTab) {
            listOf("Major Board", "Departments", "Pinned", "Archives").forEachIndexed { idx, title ->
                Tab(
                    selected = boardTab == idx,
                    onClick = { boardTab = idx },
                    text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search notices, departments, circulars...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("notices_search_input")
            )

            if (boardTab == 1) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(institution.departments) { dept ->
                        FilterChip(
                            selected = selectedDept == dept,
                            onClick = { selectedDept = dept },
                            label = { Text(dept) }
                        )
                    }
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == "All",
                        onClick = { selectedCategory = "All" },
                        label = { Text("All Categories") }
                    )
                }
                items(institution.categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }
        }

        if (filteredNotices.isEmpty()) {
            Column(modifier = Modifier.padding(16.dp)) {
                FriendlyEmptyState(
                    icon = Icons.Rounded.Campaign,
                    tint = preset.primary,
                    title = "No Notices Here Yet",
                    subtitle = if (boardTab == 1) {
                        "Department notices for $selectedDept (including Major Board notices mirrored from $selectedDept) appear here."
                    } else {
                        "Try clearing filters or check another board."
                    },
                    actionLabel = if (canCreate) "Publish Notice" else null,
                    onAction = if (canCreate) onCreateNotice else null
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredNotices, key = { it.id }) { notice ->
                    NoticeBoardCard(
                        notice = notice,
                        accent = preset.primary,
                        onClick = { onSelectNotice(notice) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoticeBoardCard(
    notice: Notice,
    accent: Color,
    onClick: () -> Unit
) {
    val priorityTint = when (notice.priorityEnum()) {
        NoticePriority.EMERGENCY -> EmergencyRed
        NoticePriority.URGENT -> AmberWarm
        NoticePriority.NORMAL -> accent
    }
    val dateStr = remember(notice.updatedAt, notice.createdAt) {
        val ts = notice.updatedAt ?: notice.createdAt
        if (ts != null) SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(ts.toDate()) else "Just now"
    }
    val parsedAttachments = remember(notice.attachments) { notice.parsedAttachments() }
    val isMirrored = notice.visibilityTargets.contains("MAJOR_BOARD") &&
        notice.visibilityTargets.any { it.startsWith("DEPT_") }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("notice_card_${notice.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${notice.publisherName} · $dateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = notice.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ChipPill(notice.priorityEnum().label, priorityTint)
                ChipPill(notice.category, accent)
                ChipPill("Origin: ${notice.originDepartment}", TealAccent)
                if (isMirrored) {
                    ChipPill("Mirrored Canonical", EmeraldSuccess)
                }
                if (notice.revisionNumber > 1L) {
                    ChipPill("Rev #${notice.revisionNumber}", AmberWarm)
                }
                if (parsedAttachments.isNotEmpty()) {
                    ChipPill("${parsedAttachments.size} Attachment(s)", accent)
                }
            }
        }
    }
}

@Composable
private fun ChipPill(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.13f)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoticeDetailBottomSheet(
    notice: Notice,
    membership: InstitutionMembership,
    viewModel: CampusViewModel,
    onDismiss: () -> Unit,
    onEdit: (Notice) -> Unit
) {
    val context = LocalContext.current
    val revisions by viewModel.observeRevisions(notice.id).collectAsStateWithLifecycle(emptyList())
    val canEdit = membership.hasPermission(AppPermission.EDIT_NOTICE) || notice.publisherUid == membership.uid
    val canWithdraw = membership.hasPermission(AppPermission.WITHDRAW_ARCHIVE) || membership.hasPermission(AppPermission.EDIT_NOTICE)
    val canPin = membership.hasPermission(AppPermission.PIN_EMERGENCY) || membership.hasPermission(AppPermission.EDIT_NOTICE)
    val attachments = remember(notice.attachments) { notice.parsedAttachments() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${notice.lifecycleEnum().label} · Rev #${notice.revisionNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                if (canEdit) {
                    OutlinedButton(onClick = { onEdit(notice) }) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Revise")
                    }
                }
            }

            Text(notice.title, style = MaterialTheme.typography.headlineMedium)

            Text(
                text = "By ${notice.publisherName} (${notice.publisherRole}) · Origin: ${notice.originDepartment} · Audience: ${notice.targetAudience}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                notice.visibilityTargets.forEach { target ->
                    ChipPill(target.replace("DEPT_", "Dept: ").replace("MAJOR_BOARD", "Major Board"), TealAccent)
                }
            }

            HorizontalDivider()

            Text(notice.description, style = MaterialTheme.typography.bodyLarge)

            if (attachments.isNotEmpty()) {
                HorizontalDivider()
                Text("Official Attachments (${attachments.size})", style = MaterialTheme.typography.titleMedium)
                attachments.forEach { att ->
                    AttachmentPreviewRow(attachment = att, onOpen = {
                        val uriStr = att.localUri.ifBlank { att.driveUrl }
                        if (uriStr.isNotBlank()) {
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(uriStr), att.mimeType)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Open ${att.name}"))
                            }
                        }
                    })
                }
            }

            HorizontalDivider()

            // Individual Read Acknowledgement
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (membership.joinMethod == "STUDENT_PASS") {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AmberWarm.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Shared Student Pass mode: individual read receipts require an individual account.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = { viewModel.acknowledgeNotice(notice, membership) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Acknowledge Read")
                    }
                }
            }

            if (canPin || canWithdraw) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canPin) {
                        OutlinedButton(onClick = {
                            viewModel.updateNoticeStatus(
                                notice = notice,
                                newStatus = notice.lifecycleEnum(),
                                pinned = !notice.pinned,
                                actor = membership,
                                reason = if (notice.pinned) "Unpinned notice" else "Pinned notice"
                            )
                        }) {
                            Icon(Icons.Rounded.PushPin, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (notice.pinned) "Unpin" else "Pin")
                        }
                    }
                    if (canWithdraw && notice.lifecycleEnum() != NoticeLifecycleStatus.WITHDRAWN) {
                        OutlinedButton(onClick = {
                            viewModel.updateNoticeStatus(
                                notice = notice,
                                newStatus = NoticeLifecycleStatus.WITHDRAWN,
                                pinned = false,
                                actor = membership,
                                reason = "Withdrawn by ${membership.displayName}"
                            )
                            onDismiss()
                        }) {
                            Icon(Icons.Rounded.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Withdraw")
                        }
                    }
                    if (canWithdraw && notice.lifecycleEnum() != NoticeLifecycleStatus.ARCHIVED) {
                        OutlinedButton(onClick = {
                            viewModel.updateNoticeStatus(
                                notice = notice,
                                newStatus = NoticeLifecycleStatus.ARCHIVED,
                                pinned = false,
                                actor = membership,
                                reason = "Archived by ${membership.displayName}"
                            )
                            onDismiss()
                        }) {
                            Icon(Icons.Rounded.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Archive")
                        }
                    }
                }
            }

            if (revisions.isNotEmpty()) {
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Revision History", style = MaterialTheme.typography.titleMedium)
                }
                revisions.forEach { rev ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Rev #${rev.revisionNumber} · ${rev.editedByName} (${rev.lifecycleStatus})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(rev.changeSummary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AttachmentPreviewRow(attachment: NoticeAttachment, onOpen: () -> Unit) {
    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(attachment.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${attachment.mimeType} · ${attachment.uploadStatus}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (attachment.uploadStatus == "VERIFIED") EmeraldSuccess else AmberWarm
                    )
                }
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Open attachment")
            }
            if (attachment.mimeType.startsWith("image/") && attachment.localUri.isNotBlank()) {
                AsyncImage(
                    model = attachment.localUri,
                    contentDescription = attachment.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            }
        }
    }
}
