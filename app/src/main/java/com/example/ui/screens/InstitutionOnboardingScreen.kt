package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AddBusiness
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.InstitutionIdGenerator
import com.example.model.Institution
import com.example.model.InstitutionCode
import com.example.model.MemberRole
import com.example.model.UiState
import com.example.model.UserInstitutionLink
import com.example.ui.components.AnimatedColorfulTitle
import com.example.ui.components.AnimatedSectionReveal
import com.example.ui.components.ColorfulIconBadge
import com.example.ui.components.InstitutionIdBadge
import com.example.ui.theme.AccentPreset
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.RoseCrimson
import com.example.ui.theme.TealAccent
import com.example.viewmodel.OnboardingViewModel

private enum class OnboardingMode {
    CHOICE,
    CREATE_INSTITUTION,
    JOIN_INSTITUTION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstitutionOnboardingScreen(
    viewModel: OnboardingViewModel,
    userEmail: String,
    userDisplayName: String,
    onInstitutionReady: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val joinedState by viewModel.joinedInstitutions.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val errorMsg by viewModel.errorMessage.collectAsStateWithLifecycle()
    val createdInst by viewModel.createdInstitution.collectAsStateWithLifecycle()
    val resolvedInst by viewModel.resolvedInstitution.collectAsStateWithLifecycle()

    var mode by remember { mutableStateOf(OnboardingMode.CHOICE) }

    BackHandler(enabled = mode != OnboardingMode.CHOICE) {
        viewModel.clearError()
        viewModel.clearResolvedInstitution()
        mode = OnboardingMode.CHOICE
    }

    createdInst?.let { created ->
        CreatedInstitutionCelebrationDialog(
            institutionCode = created,
            onContinue = { viewModel.dismissCreatedCelebration(onInstitutionReady) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedColorfulTitle(
                        text = when (mode) {
                            OnboardingMode.CHOICE -> "NOTICE BOARD"
                            OnboardingMode.CREATE_INSTITUTION -> "New Institution"
                            OnboardingMode.JOIN_INSTITUTION -> "Join Campus"
                        },
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    if (mode != OnboardingMode.CHOICE) {
                        IconButton(onClick = {
                            viewModel.clearError()
                            viewModel.clearResolvedInstitution()
                            mode = OnboardingMode.CHOICE
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("onboarding_sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Sign out")
                    }
                }
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                        )
                    )
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedContent(
                targetState = mode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboarding_mode_transition",
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
            ) { currentMode ->
                when (currentMode) {
                    OnboardingMode.CHOICE -> ChoicePortal(
                        userEmail = userEmail,
                        joinedState = joinedState,
                        onSelectJoined = { viewModel.selectInstitution(it.institutionId, onInstitutionReady) },
                        onCreateClick = {
                            viewModel.clearError()
                            mode = OnboardingMode.CREATE_INSTITUTION
                        },
                        onJoinClick = {
                            viewModel.clearError()
                            mode = OnboardingMode.JOIN_INSTITUTION
                        }
                    )

                    OnboardingMode.CREATE_INSTITUTION -> CreateInstitutionForm(
                        defaultEmail = userEmail,
                        defaultDeanName = userDisplayName,
                        busy = busy,
                        errorMessage = errorMsg,
                        onSubmit = { name, tagline, email, phone, deanName, pass, depts, accent ->
                            viewModel.createInstitution(name, tagline, email, phone, deanName, pass, depts, accent)
                        }
                    )

                    OnboardingMode.JOIN_INSTITUTION -> JoinInstitutionForm(
                        defaultDisplayName = userDisplayName,
                        resolvedCode = resolvedInst,
                        busy = busy,
                        errorMessage = errorMsg,
                        onLookup = { viewModel.lookupInstitution(it) },
                        onResetLookup = { viewModel.clearResolvedInstitution() },
                        onJoinStudent = { code, pass, name, dept ->
                            viewModel.joinAsStudent(code, pass, name, dept, onInstitutionReady)
                        },
                        onJoinRole = { code, role ->
                            viewModel.joinWithEntrance(code, role, onInstitutionReady)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChoicePortal(
    userEmail: String,
    joinedState: UiState<List<UserInstitutionLink>>,
    onSelectJoined: (UserInstitutionLink) -> Unit,
    onCreateClick: () -> Unit,
    onJoinClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedSectionReveal(0) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    ColorfulIconBadge(icon = Icons.Rounded.Verified, tint = EmeraldSuccess, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Signed in as", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(userEmail, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }

        // Joined Institutions Quick Switcher
        when (joinedState) {
            is UiState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Checking your campus memberships...", style = MaterialTheme.typography.bodyMedium)
                }
            }
            is UiState.Success -> {
                if (joinedState.data.isNotEmpty()) {
                    AnimatedSectionReveal(1) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Your Campuses",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            joinedState.data.forEach { link ->
                                val preset = AccentPreset.fromKey(link.accentColorKey)
                                Card(
                                    onClick = { onSelectJoined(link) },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("joined_institution_${link.institutionId}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        ColorfulIconBadge(
                                            icon = Icons.Rounded.AccountBalance,
                                            tint = preset.primary,
                                            contentDescription = null,
                                            size = 46.dp
                                        )
                                        Spacer(Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(link.institutionName, style = MaterialTheme.typography.titleMedium)
                                            Spacer(Modifier.height(4.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                InstitutionIdBadge(link.institutionId)
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = preset.secondary.copy(alpha = 0.14f)
                                                ) {
                                                    Text(
                                                        text = MemberRole.fromRaw(link.role).label,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = preset.secondary,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = "Open campus", tint = preset.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is UiState.Error -> {
                Text(joinedState.message, color = RoseCrimson, style = MaterialTheme.typography.bodySmall)
            }
        }

        AnimatedSectionReveal(2) {
            Text(
                text = "Establish or Enter an Institution",
                style = MaterialTheme.typography.titleMedium
            )
        }

        AnimatedSectionReveal(3) {
            Card(
                onClick = onCreateClick,
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_new_institution_card")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(20.dp)
                ) {
                    ColorfulIconBadge(
                        icon = Icons.Rounded.AddBusiness,
                        tint = IndigoPrimary,
                        contentDescription = null,
                        size = 52.dp,
                        iconSize = 26.dp
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Create New Institution", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "For the Dean establishing a new campus space. Automatically generates a permanent Institution ID (NB-NAME-SUFFIX).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        AnimatedSectionReveal(4) {
            Card(
                onClick = onJoinClick,
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("join_existing_institution_card")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(20.dp)
                ) {
                    ColorfulIconBadge(
                        icon = Icons.Rounded.Groups,
                        tint = TealAccent,
                        contentDescription = null,
                        size = 52.dp,
                        iconSize = 26.dp
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Join Existing Institution", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Enter your Institution ID and access via Dean, Admin, Staff, Student Leader, or Student Entry Pass.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateInstitutionForm(
    defaultEmail: String,
    defaultDeanName: String,
    busy: Boolean,
    errorMessage: String?,
    onSubmit: (
        name: String,
        tagline: String,
        email: String,
        phone: String,
        deanName: String,
        studentPass: String,
        departments: List<String>,
        accentColorKey: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var tagline by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(defaultEmail) }
    var phone by remember { mutableStateOf("") }
    var deanName by remember { mutableStateOf(defaultDeanName) }
    var studentPass by remember { mutableStateOf("") }
    var useEngineeringTemplate by remember { mutableStateOf(true) }
    var customDepts by remember { mutableStateOf("Arts, Science, Commerce, Management") }
    var selectedAccent by remember { mutableStateOf(AccentPreset.INDIGO) }
    var ownershipDeclared by remember { mutableStateOf(false) }

    val previewSlug = remember(name) {
        if (name.isBlank()) "NB-CAMPUS-XXXXX" else "NB-${InstitutionIdGenerator.normalizeSlug(name)}-XXXXX"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = IndigoPrimary.copy(alpha = 0.09f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Dean-First Self-Registration", style = MaterialTheme.typography.titleSmall, color = IndigoPrimary)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Your account becomes the founding Dean authority for this institution. Permanent ID preview:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                InstitutionIdBadge(previewSlug)
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Official Institution Name *") },
            placeholder = { Text("e.g. BLDE Engineering College") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("institution_name_input")
        )

        OutlinedTextField(
            value = tagline,
            onValueChange = { tagline = it },
            label = { Text("Tagline / Campus Motto") },
            placeholder = { Text("Official Digital Notice Board") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = deanName,
            onValueChange = { deanName = it },
            label = { Text("Dean Display Name *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Recovery & Contact Email *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Contact Phone (Optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = studentPass,
            onValueChange = { studentPass = it },
            label = { Text("Initial Student Entry Pass (Optional, min 4 chars)") },
            placeholder = { Text("Students enter this to join") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("initial_student_pass_input")
        )

        Text("Department Starter Template", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = useEngineeringTemplate,
                onClick = { useEngineeringTemplate = true },
                label = { Text("Engineering (CSE, ISE, ECE...)") }
            )
            FilterChip(
                selected = !useEngineeringTemplate,
                onClick = { useEngineeringTemplate = false },
                label = { Text("Custom") }
            )
        }
        if (!useEngineeringTemplate) {
            OutlinedTextField(
                value = customDepts,
                onValueChange = { customDepts = it },
                label = { Text("Comma-separated departments") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Text("Institution Accent Theme", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AccentPreset.entries.forEach { preset ->
                FilterChip(
                    selected = selectedAccent == preset,
                    onClick = { selectedAccent = preset },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(preset.primary)
                        )
                    },
                    label = { Text(preset.label) }
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { ownershipDeclared = !ownershipDeclared }
                .padding(vertical = 4.dp)
        ) {
            Checkbox(
                checked = ownershipDeclared,
                onCheckedChange = { ownershipDeclared = it },
                modifier = Modifier.testTag("ownership_declaration_checkbox")
            )
            Text(
                text = "I declare I am authorized to establish this official institution space.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        AnimatedVisibility(visible = errorMessage != null) {
            Surface(shape = RoundedCornerShape(12.dp), color = RoseCrimson.copy(alpha = 0.12f)) {
                Text(
                    text = errorMessage.orEmpty(),
                    color = RoseCrimson,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Button(
            onClick = {
                val depts = if (useEngineeringTemplate) {
                    Institution.DEFAULT_DEPARTMENTS
                } else {
                    customDepts.split(",").map { it.trim() }.filter { it.isNotBlank() }
                }
                onSubmit(name, tagline, email, phone, deanName, studentPass, depts, selectedAccent.key)
            },
            enabled = !busy && ownershipDeclared && name.trim().length >= 2,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .minimumInteractiveComponentSize()
                .testTag("submit_create_institution_button")
        ) {
            if (busy) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Reserving Unique ID...")
            } else {
                Text("Create Institution & Dean Space")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JoinInstitutionForm(
    defaultDisplayName: String,
    resolvedCode: InstitutionCode?,
    busy: Boolean,
    errorMessage: String?,
    onLookup: (String) -> Unit,
    onResetLookup: () -> Unit,
    onJoinStudent: (InstitutionCode, String, String, String) -> Unit,
    onJoinRole: (InstitutionCode, MemberRole) -> Unit
) {
    var institutionIdInput by remember { mutableStateOf("") }
    var selectedEntrance by remember { mutableStateOf(MemberRole.STUDENT) }
    var studentPassInput by remember { mutableStateOf("") }
    var studentNameInput by remember { mutableStateOf(defaultDisplayName) }
    var studentDeptInput by remember { mutableStateOf("CSE") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (resolvedCode == null) {
            Text(
                "Enter the Institution ID shared by your Dean (e.g. NB-BLDE-7K4P2).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = institutionIdInput,
                onValueChange = { institutionIdInput = it.uppercase() },
                label = { Text("Institution ID") },
                placeholder = { Text("NB-CAMPUS-XXXXX") },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                modifier = Modifier.fillMaxWidth().testTag("join_institution_id_input")
            )
            AnimatedVisibility(visible = errorMessage != null) {
                Surface(shape = RoundedCornerShape(12.dp), color = RoseCrimson.copy(alpha = 0.12f)) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = RoseCrimson,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            Button(
                onClick = { onLookup(institutionIdInput) },
                enabled = !busy && institutionIdInput.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("lookup_institution_button")
            ) {
                if (busy) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Verify Institution ID")
                }
            }
        } else {
            val preset = AccentPreset.fromKey(resolvedCode.accentColorKey)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().border(1.dp, preset.primary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    ColorfulIconBadge(icon = Icons.Rounded.AccountBalance, tint = preset.primary, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(resolvedCode.name, style = MaterialTheme.typography.titleMedium)
                        Text(resolvedCode.tagline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        InstitutionIdBadge(resolvedCode.institutionId)
                    }
                    TextButton(onClick = onResetLookup) { Text("Change") }
                }
            }

            Text("Select Entrance", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    MemberRole.STUDENT to Icons.Rounded.School,
                    MemberRole.STUDENT_LEADER to Icons.Rounded.Badge,
                    MemberRole.STAFF to Icons.Rounded.Groups,
                    MemberRole.ADMIN to Icons.Rounded.AdminPanelSettings,
                    MemberRole.DEAN to Icons.Rounded.Key
                ).forEach { (role, icon) ->
                    FilterChip(
                        selected = selectedEntrance == role,
                        onClick = { selectedEntrance = role },
                        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(role.label) }
                    )
                }
            }

            if (selectedEntrance == MemberRole.STUDENT) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = AmberWarm.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Shared Student Pass grants student read/feedback access. Note: shared student pass does not track individual read receipts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                OutlinedTextField(
                    value = studentPassInput,
                    onValueChange = { studentPassInput = it },
                    label = { Text("Dean Student Entry Pass") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("student_pass_input")
                )
                OutlinedTextField(
                    value = studentNameInput,
                    onValueChange = { studentNameInput = it },
                    label = { Text("Your Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = studentDeptInput,
                    onValueChange = { studentDeptInput = it },
                    label = { Text("Department (e.g. CSE, ECE)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = TealAccent.copy(alpha = 0.10f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (selectedEntrance == MemberRole.DEAN) {
                            "Dean entrance verifies your existing founding Dean membership."
                        } else {
                            "${selectedEntrance.label} entrance verifies your individual invitation created by the Dean for your signed-in Google email."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            AnimatedVisibility(visible = errorMessage != null) {
                Surface(shape = RoundedCornerShape(12.dp), color = RoseCrimson.copy(alpha = 0.12f)) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = RoseCrimson,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    if (selectedEntrance == MemberRole.STUDENT) {
                        onJoinStudent(resolvedCode, studentPassInput, studentNameInput, studentDeptInput)
                    } else {
                        onJoinRole(resolvedCode, selectedEntrance)
                    }
                },
                enabled = !busy,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("confirm_join_institution_button")
            ) {
                if (busy) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Enter ${resolvedCode.name}")
                }
            }
        }
    }
}

@Composable
private fun CreatedInstitutionCelebrationDialog(
    institutionCode: InstitutionCode,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onContinue,
        icon = {
            ColorfulIconBadge(
                icon = Icons.Rounded.CheckCircle,
                tint = EmeraldSuccess,
                contentDescription = "Institution Created",
                size = 54.dp
            )
        },
        title = {
            AnimatedColorfulTitle(
                text = "Institution Space Ready!",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${institutionCode.name} is now live. Save and share this permanent Institution ID with your staff and students:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = institutionCode.institutionId,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row {
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(institutionCode.institutionId))
                                copied = true
                            }) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy Institution ID")
                            }
                            IconButton(onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Join ${institutionCode.name} on NOTICE BOARD using Institution ID: ${institutionCode.institutionId}"
                                    )
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Institution ID"))
                            }) {
                                Icon(Icons.Rounded.Share, contentDescription = "Share Institution ID")
                            }
                        }
                    }
                }
                if (copied) {
                    Text("Copied to clipboard!", style = MaterialTheme.typography.labelMedium, color = EmeraldSuccess)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onContinue,
                modifier = Modifier.testTag("enter_new_institution_button")
            ) {
                Text("Open Dean Dashboard")
            }
        }
    )
}
