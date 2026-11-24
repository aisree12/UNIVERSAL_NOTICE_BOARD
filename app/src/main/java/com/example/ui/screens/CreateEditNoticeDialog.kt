package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.camera.DocumentScannerDialog
import com.example.model.Institution
import com.example.model.InstitutionMembership
import com.example.model.Notice
import com.example.model.NoticeAttachment
import com.example.model.NoticeLifecycleStatus
import com.example.model.NoticePriority
import com.example.ui.components.SearchableDropdownWithCustom
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TealAccent
import com.example.viewmodel.CampusViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateEditNoticeDialog(
    institution: Institution,
    membership: InstitutionMembership,
    existingNotice: Notice?,
    busy: Boolean,
    viewModel: CampusViewModel,
    onDismiss: () -> Unit,
    onOpenExistingDuplicate: (Notice) -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(existingNotice?.title ?: "") }
    var description by remember { mutableStateOf(existingNotice?.description ?: "") }
    var category by remember { mutableStateOf(existingNotice?.category ?: institution.categories.firstOrNull() ?: "Circular") }
    var priority by remember { mutableStateOf(existingNotice?.priorityEnum() ?: NoticePriority.NORMAL) }
    var originDepartment by remember {
        mutableStateOf(
            existingNotice?.originDepartment
                ?: membership.department.takeIf { it.isNotBlank() }
                ?: institution.departments.firstOrNull()
                ?: "Administration"
        )
    }
    var targetAudience by remember { mutableStateOf(existingNotice?.targetAudience ?: "All Students") }
    var targetMajorBoard by remember { mutableStateOf(existingNotice?.visibilityTargets?.contains("MAJOR_BOARD") ?: true) }
    var selectedDepts by remember {
        mutableStateOf(
            existingNotice?.visibilityTargets?.filter { it.startsWith("DEPT_") }?.map { it.removePrefix("DEPT_") }?.toSet()
                ?: setOf(originDepartment)
        )
    }
    var pinned by remember { mutableStateOf(existingNotice?.pinned ?: false) }
    var changeSummary by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }

    val attachments = remember {
        mutableStateListOf<NoticeAttachment>().apply {
            existingNotice?.parsedAttachments()?.let { addAll(it) }
        }
    }

    val duplicateMatch = remember(title, category, originDepartment) {
        viewModel.findLikelyDuplicate(title, category, originDepartment, existingNotice?.id)
    }

    // Zero-permission Photo Picker (Play Policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            attachments.add(resolveAttachmentFromUri(context, uri, institution.driveConnected))
        }
    }

    // Zero-permission Document Picker for PDF, Word, Excel, Images
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            attachments.add(resolveAttachmentFromUri(context, uri, institution.driveConnected))
        }
    }

    if (showScanner) {
        DocumentScannerDialog(
            onDismiss = { showScanner = false },
            onPdfCreated = { pdfAttachment ->
                attachments.add(pdfAttachment)
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (existingNotice == null) "Publish Notice" else "Revise Canonical Notice") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    }
                )
            },
            modifier = Modifier.fillMaxSize().systemBarsPadding()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Notice Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("notice_title_input")
                )

                AnimatedVisibility(visible = duplicateMatch != null) {
                    duplicateMatch?.let { dup ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = AmberWarm.copy(alpha = 0.14f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(Icons.Rounded.Warning, contentDescription = null, tint = AmberWarm)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Likely Duplicate Detected", style = MaterialTheme.typography.titleSmall, color = AmberWarm)
                                    Text(
                                        "\"${dup.title}\" already exists in ${dup.originDepartment}. Revise the canonical record instead?",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                TextButton(onClick = { onOpenExistingDuplicate(dup) }) {
                                    Text("Open")
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notice Content / Details *") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("notice_body_input")
                )

                SearchableDropdownWithCustom(
                    label = "Category",
                    selectedValue = category,
                    options = institution.categories,
                    onValueSelected = { category = it },
                    testTag = "notice_category_dropdown"
                )

                SearchableDropdownWithCustom(
                    label = "Originating Department",
                    selectedValue = originDepartment,
                    options = listOf("Administration") + institution.departments,
                    onValueSelected = { originDepartment = it },
                    testTag = "notice_department_dropdown"
                )

                SearchableDropdownWithCustom(
                    label = "Target Audience",
                    selectedValue = targetAudience,
                    options = listOf("All Students", "Faculty & Staff", "1st Year", "2nd Year", "3rd Year", "Final Year", "Hostel Residents"),
                    onValueSelected = { targetAudience = it }
                )

                Text("Priority", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NoticePriority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.label) }
                        )
                    }
                }

                Text("Board Visibility Targets", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = targetMajorBoard, onCheckedChange = { targetMajorBoard = it })
                    Text("Major Board (Institution-wide)", style = MaterialTheme.typography.bodyMedium)
                }

                if (targetMajorBoard && originDepartment != "Administration" && originDepartment.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = TealAccent.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                            Icon(Icons.Rounded.SyncAlt, contentDescription = null, tint = TealAccent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Canonical Mirroring: Publishing to Major Board automatically mirrors to $originDepartment board without duplicating files.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    institution.departments.forEach { dept ->
                        FilterChip(
                            selected = selectedDepts.contains(dept),
                            onClick = {
                                selectedDepts = if (selectedDepts.contains(dept)) selectedDepts - dept else selectedDepts + dept
                            },
                            label = { Text(dept) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = pinned, onCheckedChange = { pinned = it })
                    Text("Pin to top of notice feed", style = MaterialTheme.typography.bodyMedium)
                }

                if (existingNotice != null) {
                    OutlinedTextField(
                        value = changeSummary,
                        onValueChange = { changeSummary = it },
                        label = { Text("Revision Summary (e.g. Updated exam date)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // CameraX Scan + Pickers
                Text("Documents & Attachments", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showScanner = true },
                        modifier = Modifier.testTag("open_camera_scanner_button")
                    ) {
                        Icon(Icons.Rounded.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Scan PDF")
                    }
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Photo Picker")
                    }
                    OutlinedButton(
                        onClick = {
                            documentPickerLauncher.launch(
                                arrayOf(
                                    "application/pdf",
                                    "image/*",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                )
                            )
                        }
                    ) {
                        Icon(Icons.Rounded.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pick File")
                    }
                }

                attachments.forEachIndexed { idx, att ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(att.name, style = MaterialTheme.typography.titleSmall)
                                Text("${att.mimeType} · ${att.uploadStatus} (${att.storageTarget})", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { attachments.removeAt(idx) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Remove attachment")
                            }
                        }
                    }
                }

                val buildTargets = {
                    buildList {
                        if (targetMajorBoard) add("MAJOR_BOARD")
                        selectedDepts.forEach { add("DEPT_$it") }
                    }.ifEmpty { listOf("MAJOR_BOARD") }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            viewModel.publishOrUpdateNotice(
                                existingNotice = existingNotice,
                                title = title,
                                description = description,
                                category = category,
                                priority = priority,
                                originDepartment = originDepartment,
                                requestedTargets = buildTargets(),
                                targetAudience = targetAudience,
                                lifecycleStatus = NoticeLifecycleStatus.DRAFT,
                                pinned = pinned,
                                attachments = attachments.toList(),
                                publisher = membership,
                                changeSummary = changeSummary,
                                onSuccess = onDismiss
                            )
                        },
                        enabled = !busy && title.trim().length >= 3 && description.trim().length >= 3,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Draft")
                    }
                    Button(
                        onClick = {
                            viewModel.publishOrUpdateNotice(
                                existingNotice = existingNotice,
                                title = title,
                                description = description,
                                category = category,
                                priority = priority,
                                originDepartment = originDepartment,
                                requestedTargets = buildTargets(),
                                targetAudience = targetAudience,
                                lifecycleStatus = NoticeLifecycleStatus.PUBLISHED,
                                pinned = pinned,
                                attachments = attachments.toList(),
                                publisher = membership,
                                changeSummary = changeSummary,
                                onSuccess = onDismiss
                            )
                        },
                        enabled = !busy && title.trim().length >= 3 && description.trim().length >= 3,
                        modifier = Modifier.weight(1f).testTag("publish_notice_button")
                    ) {
                        if (busy) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text(if (existingNotice == null) "Publish Now" else "Save Revision")
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

private fun resolveAttachmentFromUri(
    context: android.content.Context,
    uri: Uri,
    driveConnected: Boolean
): NoticeAttachment {
    var name = "Document_${System.currentTimeMillis()}"
    var size = 0L
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
            if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
        }
    }
    val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
    return NoticeAttachment(
        id = UUID.randomUUID().toString(),
        name = name,
        mimeType = mime,
        sizeBytes = size,
        storageTarget = if (driveConnected) "DRIVE_REFERENCE" else "DEVICE_URI",
        localUri = uri.toString(),
        uploadStatus = "VERIFIED"
    )
}
