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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SportsCricket
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AppPermission
import com.example.model.CampusEvent
import com.example.model.EventKind
import com.example.model.EventStatus
import com.example.model.Institution
import com.example.model.InstitutionMembership
import com.example.ui.components.ColorfulIconBadge
import com.example.ui.components.FriendlyEmptyState
import com.example.ui.components.SearchableDropdownWithCustom
import com.example.ui.theme.AccentPreset
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.TealAccent
import com.example.viewmodel.CampusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsTab(
    institution: Institution,
    membership: InstitutionMembership,
    events: List<CampusEvent>,
    myRegistrations: Set<String>,
    viewModel: CampusViewModel,
    onCreateEvent: (EventKind) -> Unit,
    onEditEvent: (CampusEvent) -> Unit
) {
    val preset = AccentPreset.fromKey(institution.accentColorKey)
    var tabIndex by remember { mutableIntStateOf(0) } // 0=Activities, 1=Sports
    var selectedSubType by remember { mutableStateOf("All") }

    val currentKind = if (tabIndex == 0) EventKind.ACTIVITY else EventKind.SPORT
    val canManage = if (currentKind == EventKind.ACTIVITY) {
        membership.hasPermission(AppPermission.MANAGE_EVENTS)
    } else {
        membership.hasPermission(AppPermission.MANAGE_SPORTS)
    }

    val subTypeOptions = if (currentKind == EventKind.ACTIVITY) institution.activityTypes else institution.sportTypes
    val filtered = remember(events, currentKind, selectedSubType) {
        events.filter {
            it.kindEnum() == currentKind &&
                (selectedSubType == "All" || it.subType.equals(selectedSubType, ignoreCase = true))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = tabIndex) {
            Tab(
                selected = tabIndex == 0,
                onClick = { tabIndex = 0; selectedSubType = "All" },
                text = { Text("Campus Activities") }
            )
            Tab(
                selected = tabIndex == 1,
                onClick = { tabIndex = 1; selectedSubType = "All" },
                text = { Text("Sports & Fixtures") }
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedSubType == "All",
                    onClick = { selectedSubType = "All" },
                    label = { Text("All") }
                )
            }
            items(subTypeOptions) { type ->
                FilterChip(
                    selected = selectedSubType == type,
                    onClick = { selectedSubType = type },
                    label = { Text(type) }
                )
            }
        }

        if (filtered.isEmpty()) {
            Column(modifier = Modifier.padding(16.dp)) {
                FriendlyEmptyState(
                    icon = if (currentKind == EventKind.SPORT) Icons.Rounded.EmojiEvents else Icons.Rounded.EventAvailable,
                    tint = if (currentKind == EventKind.SPORT) TealAccent else preset.primary,
                    title = if (currentKind == EventKind.SPORT) "No Sports Events Posted" else "No Campus Activities Yet",
                    subtitle = if (currentKind == EventKind.SPORT) {
                        "Tournament schedules, teams, fixtures, and live results will show here."
                    } else {
                        "Technical fests, cultural nights, academic seminars, and student-welfare events appear here."
                    },
                    actionLabel = if (canManage) "Create ${currentKind.label}" else null,
                    onAction = if (canManage) { { onCreateEvent(currentKind) } } else null
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { event ->
                    val isRegistered = myRegistrations.contains(event.id)
                    EventCard(
                        event = event,
                        accent = if (event.kindEnum() == EventKind.SPORT) TealAccent else IndigoPrimary,
                        isRegistered = isRegistered,
                        canEdit = canManage,
                        onToggleRegistration = {
                            viewModel.toggleRegistration(event, membership, isRegistered)
                        },
                        onEdit = { onEditEvent(event) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventCard(
    event: CampusEvent,
    accent: Color,
    isRegistered: Boolean,
    canEdit: Boolean,
    onToggleRegistration: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("event_card_${event.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorfulIconBadge(
                    icon = if (event.kindEnum() == EventKind.SPORT) Icons.Rounded.SportsCricket else Icons.Rounded.EventAvailable,
                    tint = accent,
                    contentDescription = null,
                    size = 44.dp
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(event.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${event.subType} · ${event.organizer}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (canEdit) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit event")
                    }
                }
            }

            Text(event.description, style = MaterialTheme.typography.bodyMedium)

            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = accent)
                    Spacer(Modifier.width(4.dp))
                    Text(event.scheduleText, style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = AmberWarm)
                    Spacer(Modifier.width(4.dp))
                    Text(event.venue, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (event.teamsInfo.isNotBlank() || event.fixturesSummary.isNotBlank() || event.resultsSummary.isNotBlank()) {
                HorizontalDivider()
                if (event.teamsInfo.isNotBlank()) {
                    Text("Teams: ${event.teamsInfo}", style = MaterialTheme.typography.bodySmall)
                }
                if (event.fixturesSummary.isNotBlank()) {
                    Text("Fixtures: ${event.fixturesSummary}", style = MaterialTheme.typography.bodySmall)
                }
                if (event.resultsSummary.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(10.dp), color = EmeraldSuccess.copy(alpha = 0.12f)) {
                        Text(
                            text = "Results: ${event.resultsSummary}",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldSuccess,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Eligibility: ${event.eligibility} · ${event.registeredCount} registered",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (event.registrationOpen) {
                    if (isRegistered) {
                        OutlinedButton(onClick = onToggleRegistration) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Registered")
                        }
                    } else {
                        Button(onClick = onToggleRegistration) {
                            Text("Register")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateEditEventDialog(
    institution: Institution,
    membership: InstitutionMembership,
    initialKind: EventKind,
    existingEvent: CampusEvent?,
    busy: Boolean,
    viewModel: CampusViewModel,
    onDismiss: () -> Unit
) {
    var kind by remember { mutableStateOf(existingEvent?.kindEnum() ?: initialKind) }
    var subType by remember {
        mutableStateOf(
            existingEvent?.subType ?: if (kind == EventKind.SPORT) institution.sportTypes.firstOrNull() ?: "Cricket"
            else institution.activityTypes.firstOrNull() ?: "Technical"
        )
    }
    var title by remember { mutableStateOf(existingEvent?.title ?: "") }
    var description by remember { mutableStateOf(existingEvent?.description ?: "") }
    var organizer by remember { mutableStateOf(existingEvent?.organizer ?: membership.displayName) }
    var venue by remember { mutableStateOf(existingEvent?.venue ?: "") }
    var scheduleText by remember { mutableStateOf(existingEvent?.scheduleText ?: "") }
    var eligibility by remember { mutableStateOf(existingEvent?.eligibility ?: "Open to All") }
    var registrationOpen by remember { mutableStateOf(existingEvent?.registrationOpen ?: true) }
    var teamsInfo by remember { mutableStateOf(existingEvent?.teamsInfo ?: "") }
    var fixturesSummary by remember { mutableStateOf(existingEvent?.fixturesSummary ?: "") }
    var resultsSummary by remember { mutableStateOf(existingEvent?.resultsSummary ?: "") }
    var status by remember { mutableStateOf(existingEvent?.statusEnum() ?: EventStatus.UPCOMING) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (existingEvent == null) "New Campus ${kind.label}" else "Edit ${kind.label}") },
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventKind.entries.forEach { k ->
                        FilterChip(
                            selected = kind == k,
                            onClick = {
                                kind = k
                                subType = if (k == EventKind.SPORT) institution.sportTypes.firstOrNull() ?: "Cricket"
                                else institution.activityTypes.firstOrNull() ?: "Technical"
                            },
                            label = { Text(k.label) }
                        )
                    }
                }

                SearchableDropdownWithCustom(
                    label = if (kind == EventKind.SPORT) "Sport Type" else "Activity Type",
                    selectedValue = subType,
                    options = if (kind == EventKind.SPORT) institution.sportTypes else institution.activityTypes,
                    onValueSelected = { subType = it }
                )

                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description *") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = organizer, onValueChange = { organizer = it }, label = { Text("Organizer") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = venue, onValueChange = { venue = it }, label = { Text("Venue *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = scheduleText, onValueChange = { scheduleText = it }, label = { Text("Schedule / Date & Time *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = eligibility, onValueChange = { eligibility = it }, label = { Text("Eligibility") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                if (kind == EventKind.SPORT) {
                    OutlinedTextField(value = teamsInfo, onValueChange = { teamsInfo = it }, label = { Text("Participating Teams") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = fixturesSummary, onValueChange = { fixturesSummary = it }, label = { Text("Fixtures / Match Schedule") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = resultsSummary, onValueChange = { resultsSummary = it }, label = { Text("Results / Standings") }, modifier = Modifier.fillMaxWidth())
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventStatus.entries.forEach { st ->
                        FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st.label) })
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = registrationOpen, onCheckedChange = { registrationOpen = it })
                    Text("Allow student registration", style = MaterialTheme.typography.bodyMedium)
                }

                Button(
                    onClick = {
                        viewModel.saveEvent(
                            existing = existingEvent,
                            kind = kind,
                            subType = subType,
                            title = title,
                            description = description,
                            organizer = organizer,
                            venue = venue,
                            scheduleText = scheduleText,
                            eligibility = eligibility,
                            registrationOpen = registrationOpen,
                            teamsInfo = teamsInfo,
                            fixturesSummary = fixturesSummary,
                            resultsSummary = resultsSummary,
                            status = status,
                            actor = membership,
                            onSuccess = onDismiss
                        )
                    },
                    enabled = !busy && title.trim().length >= 3 && description.trim().length >= 3 && venue.isNotBlank() && scheduleText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (busy) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text(if (existingEvent == null) "Publish ${kind.label}" else "Save Changes")
                }
            }
        }
    }
}
