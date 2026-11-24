package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppPermission
import com.example.model.CampusEvent
import com.example.model.EventKind
import com.example.model.Institution
import com.example.model.InstitutionMembership
import com.example.model.MemberStatus
import com.example.model.Notice
import com.example.model.UiState
import com.example.ui.components.AnimatedColorfulTitle
import com.example.ui.components.ColorfulIconBadge
import com.example.ui.components.FriendlyEmptyState
import com.example.ui.theme.RoseCrimson
import com.example.ui.theme.ThemeMode
import com.example.viewmodel.CampusViewModel

private data class NavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

private val DashboardTabs = listOf(
    NavTab("Home", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    NavTab("Notices", Icons.Rounded.Campaign, Icons.Outlined.Campaign, "nav_notices"),
    NavTab("Events", Icons.Rounded.EmojiEvents, Icons.Outlined.EmojiEvents, "nav_events"),
    NavTab("Feedback", Icons.Rounded.Feedback, Icons.Outlined.Feedback, "nav_feedback"),
    NavTab("Profile", Icons.Rounded.Person, Icons.Outlined.Person, "nav_profile")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScaffold(
    viewModel: CampusViewModel,
    themeMode: ThemeMode,
    onChangeTheme: (ThemeMode) -> Unit,
    onAccentResolved: (String) -> Unit,
    onSwitchInstitution: () -> Unit,
    onSignOut: () -> Unit
) {
    val institutionState by viewModel.institutionState.collectAsStateWithLifecycle()
    val membershipState by viewModel.membershipState.collectAsStateWithLifecycle()
    val noticesState by viewModel.noticesState.collectAsStateWithLifecycle()
    val eventsState by viewModel.eventsState.collectAsStateWithLifecycle()
    val feedbackState by viewModel.feedbackState.collectAsStateWithLifecycle()
    val myRegistrations by viewModel.myRegistrations.collectAsStateWithLifecycle()
    val dismissedEmergency by viewModel.dismissedEmergencyKeys.collectAsStateWithLifecycle()
    val mutedCategories by viewModel.mutedCategories.collectAsStateWithLifecycle()
    val lastSyncLabel by viewModel.lastSyncLabel.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val actionBusy by viewModel.actionBusy.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        val msg = actionMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearActionMessage()
    }

    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedNotice by remember { mutableStateOf<Notice?>(null) }
    var editingNotice by remember { mutableStateOf<Notice?>(null) }
    var showCreateNotice by remember { mutableStateOf(false) }
    var showCreateEventKind by remember { mutableStateOf<EventKind?>(null) }
    var editingEvent by remember { mutableStateOf<CampusEvent?>(null) }

    BackHandler(enabled = currentTab != 0) {
        currentTab = 0
    }

    val inst = (institutionState as? UiState.Success)?.data
    val member = (membershipState as? UiState.Success)?.data

    LaunchedEffect(inst?.accentColorKey) {
        inst?.accentColorKey?.let { onAccentResolved(it) }
    }

    // Gate if suspended / revoked / missing membership
    if (institutionState is UiState.Loading || membershipState is UiState.Loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Loading secure campus space...", style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    if (institutionState is UiState.Error || membershipState is UiState.Error || inst == null || member == null) {
        val errText = (institutionState as? UiState.Error)?.message
            ?: (membershipState as? UiState.Error)?.message
            ?: "Unable to load campus membership."
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            FriendlyEmptyState(
                icon = Icons.Rounded.Block,
                tint = RoseCrimson,
                title = "Campus Access Unavailable",
                subtitle = errText,
                actionLabel = "Switch Institution",
                onAction = onSwitchInstitution
            )
        }
        return
    }

    if (member.statusEnum() != MemberStatus.ACTIVE) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.widthIn(max = 460.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    ColorfulIconBadge(icon = Icons.Rounded.Block, tint = RoseCrimson, contentDescription = null, size = 56.dp)
                    Spacer(Modifier.height(14.dp))
                    Text("Account ${member.statusEnum().label}", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Your access to ${inst.name} has been ${member.statusEnum().label.lowercase()}. Contact the institution Dean for assistance.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onSwitchInstitution) { Text("Switch Campus") }
                }
            }
        }
        return
    }

    selectedNotice?.let { notice ->
        NoticeDetailBottomSheet(
            notice = notice,
            membership = member,
            viewModel = viewModel,
            onDismiss = { selectedNotice = null },
            onEdit = {
                selectedNotice = null
                editingNotice = it
                showCreateNotice = true
            }
        )
    }

    if (showCreateNotice) {
        CreateEditNoticeDialog(
            institution = inst,
            membership = member,
            existingNotice = editingNotice,
            busy = actionBusy,
            viewModel = viewModel,
            onDismiss = {
                showCreateNotice = false
                editingNotice = null
            },
            onOpenExistingDuplicate = { dup ->
                showCreateNotice = false
                editingNotice = null
                selectedNotice = dup
            }
        )
    }

    if (showCreateEventKind != null || editingEvent != null) {
        CreateEditEventDialog(
            institution = inst,
            membership = member,
            initialKind = showCreateEventKind ?: EventKind.ACTIVITY,
            existingEvent = editingEvent,
            busy = actionBusy,
            viewModel = viewModel,
            onDismiss = {
                showCreateEventKind = null
                editingEvent = null
            }
        )
    }

    val notices = (noticesState as? UiState.Success)?.data.orEmpty()
    val events = (eventsState as? UiState.Success)?.data.orEmpty()
    val feedbackList = (feedbackState as? UiState.Success)?.data.orEmpty()

    val canCreateNotice = member.hasPermission(AppPermission.CREATE_NOTICE) ||
        member.hasPermission(AppPermission.PUBLISH_MAJOR_BOARD) ||
        member.hasPermission(AppPermission.PUBLISH_DEPT_BOARD)
    val canCreateEvent = member.hasPermission(AppPermission.MANAGE_EVENTS) ||
        member.hasPermission(AppPermission.MANAGE_SPORTS)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.navigationBars,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        AnimatedColorfulTitle(
                            text = when (currentTab) {
                                0 -> "NOTICE BOARD"
                                1 -> "Campus Boards"
                                2 -> "Activities & Sports"
                                3 -> "Campus Feedback"
                                else -> "Profile & Controls"
                            },
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        DashboardTabs.forEachIndexed { idx, tab ->
                            val selected = currentTab == idx
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = idx },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                when {
                    (currentTab == 0 || currentTab == 1) && canCreateNotice -> {
                        ExtendedFloatingActionButton(
                            onClick = {
                                editingNotice = null
                                showCreateNotice = true
                            },
                            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                            text = { Text("Publish Notice") },
                            modifier = Modifier.testTag("fab_publish_notice")
                        )
                    }
                    currentTab == 2 && canCreateEvent -> {
                        ExtendedFloatingActionButton(
                            onClick = {
                                editingEvent = null
                                showCreateEventKind = EventKind.ACTIVITY
                            },
                            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                            text = { Text("Add Event") },
                            modifier = Modifier.testTag("fab_add_event")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        Spacer(Modifier.height(12.dp))
                        DashboardTabs.forEachIndexed { idx, tab ->
                            val selected = currentTab == idx
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = idx },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "dashboard_tab_switch",
                        modifier = Modifier.widthIn(max = 860.dp).fillMaxSize()
                    ) { tabIdx ->
                        when (tabIdx) {
                            0 -> HomeTab(
                                institution = inst,
                                membership = member,
                                notices = notices,
                                events = events,
                                dismissedEmergencyKeys = dismissedEmergency,
                                lastSyncLabel = lastSyncLabel,
                                onDismissEmergency = { viewModel.dismissEmergencyBanner(it) },
                                onOpenNotice = { selectedNotice = it },
                                onGoToNotices = { currentTab = 1 },
                                onGoToEvents = { currentTab = 2 }
                            )
                            1 -> NoticesTab(
                                institution = inst,
                                membership = member,
                                notices = notices,
                                onSelectNotice = { selectedNotice = it },
                                onCreateNotice = {
                                    editingNotice = null
                                    showCreateNotice = true
                                }
                            )
                            2 -> EventsTab(
                                institution = inst,
                                membership = member,
                                events = events,
                                myRegistrations = myRegistrations,
                                viewModel = viewModel,
                                onCreateEvent = { kind ->
                                    editingEvent = null
                                    showCreateEventKind = kind
                                },
                                onEditEvent = { ev ->
                                    editingEvent = ev
                                }
                            )
                            3 -> FeedbackTab(
                                institution = inst,
                                membership = member,
                                feedbackList = feedbackList,
                                busy = actionBusy,
                                viewModel = viewModel
                            )
                            else -> ProfileAndDeanCentreTab(
                                institution = inst,
                                membership = member,
                                themeMode = themeMode,
                                mutedCategories = mutedCategories,
                                viewModel = viewModel,
                                onChangeTheme = onChangeTheme,
                                onSwitchInstitution = onSwitchInstitution,
                                onSignOut = onSignOut
                            )
                        }
                    }
                }
            }
        }
    }
}
