package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AppPermission
import com.example.model.FeedbackItem
import com.example.model.FeedbackStatus
import com.example.model.Institution
import com.example.model.InstitutionMembership
import com.example.ui.components.ColorfulIconBadge
import com.example.ui.components.FriendlyEmptyState
import com.example.ui.components.SearchableDropdownWithCustom
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseCrimson
import com.example.ui.theme.TealAccent
import com.example.viewmodel.CampusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackTab(
    institution: Institution,
    membership: InstitutionMembership,
    feedbackList: List<FeedbackItem>,
    busy: Boolean,
    viewModel: CampusViewModel
) {
    val canReviewNormal = membership.hasPermission(AppPermission.VIEW_FEEDBACK)
    val canReviewConf = membership.hasPermission(AppPermission.VIEW_CONFIDENTIAL_FEEDBACK)
    val canReviewAny = canReviewNormal || canReviewConf

    var subTab by remember { mutableIntStateOf(0) } // 0=Submit, 1=My History, 2=Review Queue
    var confidential by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("Suggestions") }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var activeReviewItem by remember { mutableStateOf<FeedbackItem?>(null) }

    activeReviewItem?.let { item ->
        FeedbackReviewDialog(
            item = item,
            onDismiss = { activeReviewItem = null },
            onSave = { status, reply ->
                viewModel.respondToFeedback(item, status, reply, membership)
                activeReviewItem = null
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = subTab) {
            Tab(selected = subTab == 0, onClick = { subTab = 0 }, text = { Text("Submit") })
            Tab(selected = subTab == 1, onClick = { subTab = 1 }, text = { Text("My Submissions") })
            if (canReviewAny) {
                Tab(selected = subTab == 2, onClick = { subTab = 2 }, text = { Text("Review Queue") })
            }
        }

        when (subTab) {
            0 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ColorfulIconBadge(icon = Icons.Rounded.Feedback, tint = IndigoPrimary, contentDescription = null)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("Campus Feedback", style = MaterialTheme.typography.titleMedium)
                                        Text("Normal feedback is always available. Confidential feedback follows Dean policy.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (institution.confidentialFeedbackEnabled) TealAccent.copy(alpha = 0.1f) else AmberWarm.copy(alpha = 0.12f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = TealAccent)
                                                Spacer(Modifier.width(6.dp))
                                                Text("Confidential Feedback", style = MaterialTheme.typography.titleSmall)
                                            }
                                            Text(
                                                text = if (institution.confidentialFeedbackEnabled) {
                                                    "Restricted to Dean & authorized reviewers. Identity is audited, not anonymous."
                                                } else {
                                                    "New confidential submissions are currently paused by the Dean. Historical records remain preserved."
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = confidential && institution.confidentialFeedbackEnabled,
                                            onCheckedChange = { confidential = it },
                                            enabled = institution.confidentialFeedbackEnabled,
                                            modifier = Modifier.testTag("confidential_feedback_switch")
                                        )
                                    }
                                }

                                SearchableDropdownWithCustom(
                                    label = "Category",
                                    selectedValue = category,
                                    options = listOf("Suggestions", "Facilities", "Department Issue", "Event Feedback", "Safety & Welfare"),
                                    onValueSelected = { category = it }
                                )

                                OutlinedTextField(
                                    value = subject,
                                    onValueChange = { subject = it },
                                    label = { Text("Subject *") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("feedback_subject_input")
                                )

                                OutlinedTextField(
                                    value = message,
                                    onValueChange = { message = it },
                                    label = { Text("Details *") },
                                    minLines = 4,
                                    modifier = Modifier.fillMaxWidth().testTag("feedback_message_input")
                                )

                                Button(
                                    onClick = {
                                        viewModel.submitFeedback(
                                            member = membership,
                                            confidential = confidential && institution.confidentialFeedbackEnabled,
                                            category = category,
                                            subject = subject,
                                            message = message
                                        ) {
                                            subject = ""
                                            message = ""
                                            confidential = false
                                            subTab = 1
                                        }
                                    },
                                    enabled = !busy && subject.trim().length >= 3 && message.trim().length >= 5,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("submit_feedback_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (confidential && institution.confidentialFeedbackEnabled) "Submit Confidentially" else "Submit Feedback")
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                val mine = feedbackList.filter { it.authorUid == membership.uid }
                if (mine.isEmpty()) {
                    Column(Modifier.padding(16.dp)) {
                        FriendlyEmptyState(
                            icon = Icons.Rounded.Feedback,
                            tint = IndigoPrimary,
                            title = "No Submissions Yet",
                            subtitle = "Track the review status and replies to your feedback right here."
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(mine, key = { it.id }) { item ->
                            FeedbackCard(item = item, showAuthor = false, canReview = false, onReview = {})
                        }
                    }
                }
            }

            2 -> {
                if (feedbackList.isEmpty()) {
                    Column(Modifier.padding(16.dp)) {
                        FriendlyEmptyState(
                            icon = Icons.Rounded.Shield,
                            tint = TealAccent,
                            title = "No Feedback in Queue",
                            subtitle = "Authorized feedback items appear here."
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(feedbackList, key = { it.id }) { item ->
                            val allowed = (!item.confidential && canReviewNormal) || (item.confidential && canReviewConf)
                            FeedbackCard(
                                item = item,
                                showAuthor = true,
                                canReview = allowed,
                                onReview = { activeReviewItem = item }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackCard(
    item: FeedbackItem,
    showAuthor: Boolean,
    canReview: Boolean,
    onReview: () -> Unit
) {
    val statusColor = when (item.statusEnum()) {
        FeedbackStatus.SUBMITTED -> IndigoPrimary
        FeedbackStatus.UNDER_REVIEW -> AmberWarm
        FeedbackStatus.IN_PROGRESS -> TealAccent
        FeedbackStatus.RESOLVED -> EmeraldSuccess
        FeedbackStatus.CLOSED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = RoundedCornerShape(8.dp), color = statusColor.copy(alpha = 0.14f)) {
                        Text(
                            text = item.statusEnum().label,
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (item.confidential) {
                        Surface(shape = RoundedCornerShape(8.dp), color = RoseCrimson.copy(alpha = 0.14f)) {
                            Text(
                                text = "Confidential",
                                style = MaterialTheme.typography.labelMedium,
                                color = RoseCrimson,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Text(item.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(item.subject, style = MaterialTheme.typography.titleMedium)
            Text(item.message, style = MaterialTheme.typography.bodyMedium)
            if (showAuthor) {
                Text(
                    text = "Submitted by ${item.authorName} (${item.authorEmail})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (item.staffReply.isNotBlank()) {
                Surface(shape = RoundedCornerShape(12.dp), color = EmeraldSuccess.copy(alpha = 0.10f), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Response (${item.reviewedByName}):", style = MaterialTheme.typography.labelMedium, color = EmeraldSuccess)
                        Text(item.staffReply, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (canReview) {
                OutlinedButton(onClick = onReview, modifier = Modifier.align(Alignment.End)) {
                    Text("Update Status / Reply")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeedbackReviewDialog(
    item: FeedbackItem,
    onDismiss: () -> Unit,
    onSave: (FeedbackStatus, String) -> Unit
) {
    var status by remember { mutableStateOf(item.statusEnum()) }
    var reply by remember { mutableStateOf(item.staffReply) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review Feedback") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(item.subject, style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FeedbackStatus.entries.forEach { st ->
                        FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st.label) })
                    }
                }
                OutlinedTextField(
                    value = reply,
                    onValueChange = { reply = it },
                    label = { Text("Official Response") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(status, reply) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
