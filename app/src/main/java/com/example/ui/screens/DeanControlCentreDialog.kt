package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppPermission
import com.example.model.Institution
import com.example.model.InstitutionMembership
import com.example.model.MemberRole
import com.example.model.MemberStatus
import com.example.ui.components.InstitutionIdBadge
import com.example.ui.theme.AccentPreset
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseCrimson
import com.example.ui.theme.TealAccent
import com.example.viewmodel.CampusViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeanControlCentreDialog(
    institution: Institution,
    membership: InstitutionMembership,
    viewModel: CampusViewModel,
    onDismiss: () -> Unit
) {
    val isDean = membership.roleEnum() == MemberRole.DEAN
    val members by viewModel.membersState.collectAsStateWithLifecycle()
    val invites by viewModel.invitesState.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogsState.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Profile & Boards", "Student Pass", "Accounts", "Drive", "Audit Logs")

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Dean Control Centre") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close Control Centre")
                        }
                    }
                )
            },
            modifier = Modifier.fillMaxSize().systemBarsPadding()
        ) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                PrimaryScrollableTabRow(selectedTabIndex = activeTab, edgePadding = 12.dp) {
                    tabs.forEachIndexed { idx, label ->
                        Tab(selected = activeTab == idx, onClick = { activeTab = idx }, text = { Text(label) })
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (activeTab) {
                        0 -> ProfileAndDepartmentsPane(institution, membership, isDean, viewModel)
                        1 -> StudentPassPane(institution, membership, isDean, viewModel)
                        2 -> AccountsAndPermissionsPane(institution, membership, members, invites, viewModel)
                        3 -> GoogleDrivePane(institution, membership, isDean, viewModel)
                        4 -> AuditLogsPane(auditLogs)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileAndDepartmentsPane(
    institution: Institution,
    membership: InstitutionMembership,
    isDean: Boolean,
    viewModel: CampusViewModel
) {
    var name by remember(institution.name) { mutableStateOf(institution.name) }
    var tagline by remember(institution.tagline) { mutableStateOf(institution.tagline) }
    var email by remember(institution.contactEmail) { mutableStateOf(institution.contactEmail) }
    var phone by remember(institution.contactPhone) { mutableStateOf(institution.contactPhone) }
    var accent by remember(institution.accentColorKey) { mutableStateOf(AccentPreset.fromKey(institution.accentColorKey)) }
    var confEnabled by remember(institution.confidentialFeedbackEnabled) { mutableStateOf(institution.confidentialFeedbackEnabled) }
    val departments = remember(institution.departments) { mutableStateListOf(*institution.departments.toTypedArray()) }
    val categories = remember(institution.categories) { mutableStateListOf(*institution.categories.toTypedArray()) }
    var newDept by remember { mutableStateOf("") }
    var newCat by remember { mutableStateOf("") }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Permanent ID:", style = MaterialTheme.typography.labelLarge)
        InstitutionIdBadge(institution.institutionId)
    }

    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Institution Display Name") }, enabled = isDean, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = tagline, onValueChange = { tagline = it }, label = { Text("Tagline") }, enabled = isDean, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Contact Email") }, enabled = isDean, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Contact Phone") }, enabled = isDean, singleLine = true, modifier = Modifier.fillMaxWidth())

    Text("Branding Accent", style = MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AccentPreset.entries.forEach { preset ->
            FilterChip(
                selected = accent == preset,
                onClick = { if (isDean) accent = preset },
                leadingIcon = { Box(Modifier.size(12.dp).clip(CircleShape).background(preset.primary)) },
                label = { Text(preset.label) }
            )
        }
    }

    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.padding(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Allow New Confidential Feedback", style = MaterialTheme.typography.titleSmall)
                Text("Disabling pauses new confidential submissions while preserving history.", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = confEnabled, onCheckedChange = { if (isDean) confEnabled = it }, enabled = isDean)
        }
    }

    Text("Departments (${departments.size})", style = MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        departments.forEach { dept ->
            FilterChip(
                selected = true,
                onClick = { if (isDean && departments.size > 1) departments.remove(dept) },
                trailingIcon = if (isDean && departments.size > 1) {
                    { Icon(Icons.Rounded.Close, contentDescription = "Remove $dept", modifier = Modifier.size(14.dp)) }
                } else null,
                label = { Text(dept) }
            )
        }
    }
    if (isDean) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = newDept, onValueChange = { newDept = it }, label = { Text("Add Department") }, singleLine = true, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (newDept.isNotBlank() && !departments.contains(newDept.trim())) {
                    departments.add(newDept.trim())
                    newDept = ""
                }
            }) { Icon(Icons.Rounded.Add, contentDescription = "Add department") }
        }
    }

    Text("Notice Categories (${categories.size})", style = MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        categories.forEach { cat ->
            FilterChip(
                selected = true,
                onClick = { if (isDean && categories.size > 1) categories.remove(cat) },
                trailingIcon = if (isDean && categories.size > 1) {
                    { Icon(Icons.Rounded.Close, contentDescription = "Remove $cat", modifier = Modifier.size(14.dp)) }
                } else null,
                label = { Text(cat) }
            )
        }
    }
    if (isDean) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = newCat, onValueChange = { newCat = it }, label = { Text("Add Category") }, singleLine = true, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (newCat.isNotBlank() && !categories.contains(newCat.trim())) {
                    categories.add(newCat.trim())
                    newCat = ""
                }
            }) { Icon(Icons.Rounded.Add, contentDescription = "Add category") }
        }
    }

    if (isDean) {
        Button(
            onClick = {
                viewModel.updateInstitutionSettings(
                    name = name,
                    tagline = tagline,
                    contactEmail = email,
                    contactPhone = phone,
                    accentColorKey = accent.key,
                    departments = departments.toList(),
                    categories = categories.toList(),
                    confidentialFeedbackEnabled = confEnabled,
                    actor = membership,
                    auditSummary = "Updated profile, branding, departments, and confidentialFeedbackEnabled=$confEnabled"
                )
            },
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_dean_settings_button")
        ) {
            Text("Save Institution Settings")
        }
    }
}

@Composable
private fun StudentPassPane(
    institution: Institution,
    membership: InstitutionMembership,
    isDean: Boolean,
    viewModel: CampusViewModel
) {
    var enabled by remember(institution.studentPassEnabled) { mutableStateOf(institution.studentPassEnabled) }
    var newPass by remember { mutableStateOf("") }

    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Key, contentDescription = null, tint = AmberWarm)
                Spacer(Modifier.width(8.dp))
                Text("Student Entry Pass", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "Passcodes are stored strictly as salted SHA-256 hashes (never plaintext). Changing the pass invalidates the old pass for new student joins.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Enable Student Entry Pass", style = MaterialTheme.typography.titleSmall)
                Switch(checked = enabled, onCheckedChange = { if (isDean) enabled = it }, enabled = isDean)
            }
            OutlinedTextField(
                value = newPass,
                onValueChange = { newPass = it },
                label = { Text("New Student Entry Pass (min 4 chars)") },
                singleLine = true,
                enabled = isDean && enabled,
                modifier = Modifier.fillMaxWidth().testTag("rotate_student_pass_input")
            )
            if (isDean) {
                Button(
                    onClick = { viewModel.updateStudentPass(enabled, newPass, membership) },
                    enabled = !enabled || newPass.trim().length >= 4,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Student Pass Policy")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountsAndPermissionsPane(
    institution: Institution,
    membership: InstitutionMembership,
    members: List<InstitutionMembership>,
    invites: List<com.example.model.MemberInvite>,
    viewModel: CampusViewModel
) {
    var inviteEmail by remember { mutableStateOf("") }
    var inviteName by remember { mutableStateOf("") }
    var inviteDesignation by remember { mutableStateOf("") }
    var inviteDept by remember { mutableStateOf(institution.departments.firstOrNull() ?: "CSE") }
    var inviteRole by remember { mutableStateOf(MemberRole.STAFF) }
    val selectedPerms = remember { mutableStateListOf(*AppPermission.defaultStaffPermissions.toTypedArray()) }

    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.PersonAdd, contentDescription = null, tint = TealAccent)
                Spacer(Modifier.width(8.dp))
                Text("Create Admin / Staff / Student Leader Account", style = MaterialTheme.typography.titleMedium)
            }
            OutlinedTextField(value = inviteEmail, onValueChange = { inviteEmail = it }, label = { Text("Google Email *") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("invite_email_input"))
            OutlinedTextField(value = inviteName, onValueChange = { inviteName = it }, label = { Text("Full Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = inviteDesignation, onValueChange = { inviteDesignation = it }, label = { Text("Designation (e.g. HOD, Sports Coordinator)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = inviteDept, onValueChange = { inviteDept = it }, label = { Text("Department") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            Text("Role", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(MemberRole.ADMIN, MemberRole.STAFF, MemberRole.STUDENT_LEADER).forEach { role ->
                    FilterChip(
                        selected = inviteRole == role,
                        onClick = {
                            inviteRole = role
                            selectedPerms.clear()
                            when (role) {
                                MemberRole.ADMIN -> selectedPerms.addAll(AppPermission.defaultAdminPermissions)
                                MemberRole.STAFF -> selectedPerms.addAll(AppPermission.defaultStaffPermissions)
                                MemberRole.STUDENT_LEADER -> selectedPerms.addAll(AppPermission.defaultStudentLeaderPermissions)
                                else -> {}
                            }
                        },
                        label = { Text(role.label) }
                    )
                }
            }

            Text("Granular Permissions", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val available = if (inviteRole == MemberRole.STUDENT_LEADER) {
                    AppPermission.entries.filter { it.studentSideAllowed }
                } else {
                    AppPermission.entries
                }
                available.forEach { perm ->
                    val checked = selectedPerms.contains(perm.key)
                    FilterChip(
                        selected = checked,
                        onClick = {
                            if (checked) selectedPerms.remove(perm.key) else selectedPerms.add(perm.key)
                        },
                        label = { Text(perm.label) }
                    )
                }
            }

            Button(
                onClick = {
                    viewModel.inviteMember(
                        email = inviteEmail,
                        displayName = inviteName,
                        designation = inviteDesignation,
                        department = inviteDept,
                        role = inviteRole,
                        permissions = selectedPerms.toList(),
                        scopedDepartments = listOf(inviteDept),
                        actor = membership
                    ) {
                        inviteEmail = ""
                        inviteName = ""
                        inviteDesignation = ""
                    }
                },
                enabled = inviteEmail.contains("@"),
                modifier = Modifier.fillMaxWidth().testTag("send_invite_button")
            ) {
                Text("Provision Account Invite")
            }
        }
    }

    if (invites.isNotEmpty()) {
        Text("Invitations (${invites.size})", style = MaterialTheme.typography.titleMedium)
        invites.forEach { inv ->
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.padding(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${inv.displayName} (${inv.email})", style = MaterialTheme.typography.titleSmall)
                        Text("${MemberRole.fromRaw(inv.role).label} · ${inv.department} · ${inv.status}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    Text("Active & Managed Members (${members.size})", style = MaterialTheme.typography.titleMedium)
    members.forEach { m ->
        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.padding(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(m.displayName, style = MaterialTheme.typography.titleSmall)
                    Text("${m.roleEnum().label} · ${m.department} · ${m.status}", style = MaterialTheme.typography.bodySmall)
                }
                if (m.roleEnum() != MemberRole.DEAN) {
                    val nextStatus = if (m.statusEnum() == MemberStatus.ACTIVE) MemberStatus.SUSPENDED else MemberStatus.ACTIVE
                    OutlinedButton(onClick = {
                        viewModel.updateMemberAccess(
                            target = m,
                            newRole = m.roleEnum(),
                            newPermissions = m.permissions,
                            newStatus = nextStatus,
                            newDepartment = m.department,
                            actor = membership
                        )
                    }) {
                        Text(if (nextStatus == MemberStatus.SUSPENDED) "Suspend" else "Reactivate")
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleDrivePane(
    institution: Institution,
    membership: InstitutionMembership,
    isDean: Boolean,
    viewModel: CampusViewModel
) {
    var driveEmail by remember(institution.driveAccountEmail) { mutableStateOf(institution.driveAccountEmail) }
    var folderName by remember(institution.driveFolderName) { mutableStateOf(institution.driveFolderName.ifBlank { "${institution.name} Shared Notices" }) }
    var folderId by remember(institution.driveFolderId) { mutableStateOf(institution.driveFolderId) }
    var confirmSwitchDialog by remember { mutableStateOf(false) }

    if (confirmSwitchDialog) {
        AlertDialog(
            onDismissRequest = { confirmSwitchDialog = false },
            title = { Text("Confirm Drive Connection Switch") },
            text = {
                Text("New uploads will target \"$folderName\" ($driveEmail). Existing notice file references remain preserved and are never deleted.")
            },
            confirmButton = {
                Button(onClick = {
                    confirmSwitchDialog = false
                    viewModel.updateDriveConnection(true, driveEmail, folderName, folderId, membership)
                }) { Text("Confirm Switch") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSwitchDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = if (institution.driveConnected) EmeraldSuccess else AmberWarm)
                Spacer(Modifier.width(8.dp))
                Text("Institution Google Drive", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "College login uses Firebase Auth; Google Drive is strictly separate for official attachments. Prefer an institution-controlled Workspace Shared Drive. Quotas and API limits apply.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(value = driveEmail, onValueChange = { driveEmail = it }, label = { Text("Drive Manager / Workspace Email") }, enabled = isDean, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = folderName, onValueChange = { folderName = it }, label = { Text("Shared Drive / Folder Name") }, enabled = isDean, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = folderId, onValueChange = { folderId = it }, label = { Text("Shared Drive Folder ID (Optional)") }, enabled = isDean, singleLine = true, modifier = Modifier.fillMaxWidth())

            if (isDean) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { confirmSwitchDialog = true },
                        enabled = driveEmail.contains("@") && folderName.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (institution.driveConnected) "Switch Drive" else "Connect Drive")
                    }
                    if (institution.driveConnected) {
                        OutlinedButton(
                            onClick = { viewModel.updateDriveConnection(false, "", "", "", membership) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Disconnect", color = RoseCrimson)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditLogsPane(auditLogs: List<com.example.model.AuditLog>) {
    if (auditLogs.isEmpty()) {
        Text("No audit entries recorded yet.", style = MaterialTheme.typography.bodyMedium)
    } else {
        auditLogs.forEach { log ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(log.actionType, style = MaterialTheme.typography.labelMedium, color = TealAccent)
                        Text(log.actorRole, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(log.targetSummary, style = MaterialTheme.typography.titleSmall)
                    Text("${log.actorName}: ${log.details}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
