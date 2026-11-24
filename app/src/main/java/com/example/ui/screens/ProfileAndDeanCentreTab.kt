package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.model.AppPermission
import com.example.model.Institution
import com.example.model.InstitutionMembership
import com.example.model.MemberRole
import com.example.ui.components.ColorfulIconBadge
import com.example.ui.components.InstitutionIdBadge
import com.example.ui.theme.AccentPreset
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseCrimson
import com.example.ui.theme.TealAccent
import com.example.ui.theme.ThemeMode
import com.example.viewmodel.CampusViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileAndDeanCentreTab(
    institution: Institution,
    membership: InstitutionMembership,
    themeMode: ThemeMode,
    mutedCategories: Set<String>,
    viewModel: CampusViewModel,
    onChangeTheme: (ThemeMode) -> Unit,
    onSwitchInstitution: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val preset = AccentPreset.fromKey(institution.accentColorKey)
    var showDeanControlCentre by remember { mutableStateOf(false) }

    val canOpenControlCentre = membership.roleEnum() == MemberRole.DEAN ||
        membership.hasPermission(AppPermission.MANAGE_USERS) ||
        membership.hasPermission(AppPermission.MANAGE_DRIVE) ||
        membership.hasPermission(AppPermission.VIEW_AUDIT)

    if (showDeanControlCentre) {
        DeanControlCentreDialog(
            institution = institution,
            membership = membership,
            viewModel = viewModel,
            onDismiss = { showDeanControlCentre = false }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Identity & Role Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ColorfulIconBadge(icon = Icons.Rounded.Badge, tint = preset.primary, contentDescription = null, size = 50.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(membership.displayName, style = MaterialTheme.typography.titleLarge)
                            Text(membership.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${membership.roleEnum().label} · ${membership.department.ifBlank { "Campus" }}",
                                style = MaterialTheme.typography.labelLarge,
                                color = preset.primary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(institution.name, style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(4.dp))
                                InstitutionIdBadge(institution.institutionId)
                            }
                            Row {
                                IconButton(onClick = { clipboard.setText(AnnotatedString(institution.institutionId)) }) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy Institution ID")
                                }
                                IconButton(onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Join ${institution.name} on NOTICE BOARD using ID: ${institution.institutionId}"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Institution ID"))
                                }) {
                                    Icon(Icons.Rounded.Share, contentDescription = "Share Institution ID")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dean Control Centre Launcher
        if (canOpenControlCentre) {
            item {
                Card(
                    onClick = { showDeanControlCentre = true },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().testTag("open_dean_control_centre_card")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(18.dp)
                    ) {
                        ColorfulIconBadge(
                            icon = Icons.Rounded.AdminPanelSettings,
                            tint = IndigoPrimary,
                            contentDescription = null,
                            size = 48.dp
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dean Control Centre", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Manage departments, student pass, staff/leader permissions, Google Drive, confidential feedback & audit logs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Appearance Theme Mode
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = preset.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Theme Appearance", style = MaterialTheme.typography.titleMedium)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { onChangeTheme(mode) },
                                label = { Text(mode.label) }
                            )
                        }
                    }
                }
            }
        }

        // Notification Category Preferences
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Notifications, contentDescription = null, tint = TealAccent)
                        Spacer(Modifier.width(8.dp))
                        Text("Notice Category Alerts", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "Tap categories to mute or enable in-app alert highlights.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        institution.categories.forEach { cat ->
                            val enabled = !mutedCategories.contains(cat)
                            FilterChip(
                                selected = enabled,
                                onClick = { viewModel.toggleCategoryNotification(cat) },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
            }
        }

        // Google Drive Status Info (Non-Dean View)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                    Icon(
                        Icons.Rounded.CloudDone,
                        contentDescription = null,
                        tint = if (institution.driveConnected) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (institution.driveConnected) "Institution Drive Connected" else "Drive Connection Pending",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = if (institution.driveConnected) {
                                "Folder: ${institution.driveFolderName} · Official attachments accessible without student Google Drive login."
                            } else {
                                "Deans can connect an institution Shared Drive in the Control Centre."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Switch Institution & Sign Out
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onSwitchInstitution,
                    modifier = Modifier.weight(1f).height(50.dp).testTag("switch_institution_button")
                ) {
                    Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Switch Campus")
                }
                Button(
                    onClick = onSignOut,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = RoseCrimson),
                    modifier = Modifier.weight(1f).height(50.dp).testTag("profile_sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Sign Out")
                }
            }
        }
    }
}
