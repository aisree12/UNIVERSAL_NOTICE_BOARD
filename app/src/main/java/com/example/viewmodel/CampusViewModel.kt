package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CampusRepository
import com.example.data.InstitutionIdGenerator
import com.example.data.UserPreferencesDataStore
import com.example.data.toFriendlyFirestoreMessage
import com.example.model.AppPermission
import com.example.model.AuditLog
import com.example.model.CampusEvent
import com.example.model.EventKind
import com.example.model.EventStatus
import com.example.model.FeedbackItem
import com.example.model.FeedbackStatus
import com.example.model.Institution
import com.example.model.InstitutionMembership
import com.example.model.MemberInvite
import com.example.model.MemberRole
import com.example.model.MemberStatus
import com.example.model.Notice
import com.example.model.NoticeAttachment
import com.example.model.NoticeLifecycleStatus
import com.example.model.NoticePriority
import com.example.model.NoticeReadReceipt
import com.example.model.NoticeRevision
import com.example.model.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CampusViewModel(
    private val repository: CampusRepository,
    private val prefs: UserPreferencesDataStore,
    private val currentUserId: String,
    private val institutionId: String
) : ViewModel() {

    private val _lastSyncLabel = MutableStateFlow(currentTimeLabel())
    val lastSyncLabel: StateFlow<String> = _lastSyncLabel.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _actionBusy = MutableStateFlow(false)
    val actionBusy: StateFlow<Boolean> = _actionBusy.asStateFlow()

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    private fun currentTimeLabel(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    private fun <T> Flow<T>.toUiStateFlow(tag: String, fallbackError: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> {
            _lastSyncLabel.value = currentTimeLabel()
            UiState.Success(it)
        }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(tag, fallbackError, root)
                emit(UiState.Error(root.toFriendlyFirestoreMessage(fallbackError)))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val institutionState: StateFlow<UiState<Institution?>> =
        repository.observeInstitution(institutionId)
            .toUiStateFlow("CampusVM", "Failed to load institution settings")

    val membershipState: StateFlow<UiState<InstitutionMembership?>> =
        repository.observeMembership(institutionId, currentUserId)
            .toUiStateFlow("CampusVM", "Failed to load membership")

    val noticesState: StateFlow<UiState<List<Notice>>> =
        repository.observeNotices(institutionId)
            .toUiStateFlow("CampusVM", "Failed to load notices")

    val eventsState: StateFlow<UiState<List<CampusEvent>>> =
        repository.observeEvents(institutionId)
            .toUiStateFlow("CampusVM", "Failed to load campus events")

    val myRegistrations: StateFlow<Set<String>> =
        repository.observeMyEventRegistrations(institutionId, currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(emptySet())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    @OptIn(ExperimentalCoroutinesApi::class)
    val feedbackState: StateFlow<UiState<List<FeedbackItem>>> =
        membershipState.flatMapLatest { mState ->
            val member = (mState as? UiState.Success)?.data
            if (member == null || member.status != MemberStatus.ACTIVE.name) {
                flowOf(emptyList())
            } else {
                val myFlow = repository.observeMyFeedback(institutionId, currentUserId)
                val normalFlow = if (member.hasPermission(AppPermission.VIEW_FEEDBACK)) {
                    repository.observeNormalFeedback(institutionId)
                } else flowOf(emptyList())
                val confFlow = if (member.hasPermission(AppPermission.VIEW_CONFIDENTIAL_FEEDBACK)) {
                    repository.observeConfidentialFeedback(institutionId)
                } else flowOf(emptyList())

                combine(myFlow, normalFlow, confFlow) { mine, normal, conf ->
                    (mine + normal + conf)
                        .distinctBy { it.id }
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
            }
        }.toUiStateFlow("CampusVM", "Failed to load feedback")

    @OptIn(ExperimentalCoroutinesApi::class)
    val membersState: StateFlow<List<InstitutionMembership>> =
        membershipState.flatMapLatest { mState ->
            val member = (mState as? UiState.Success)?.data
            if (member?.hasPermission(AppPermission.MANAGE_USERS) == true) {
                repository.observeMembers(institutionId)
            } else flowOf(emptyList())
        }
            .catch { error ->
                val hasFsError = generateSequence<Throwable>(error) { it.cause }.any { it is FirebaseFirestoreException }
                if (error is CancellationException && !hasFsError) {
                    throw error
                }
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val invitesState: StateFlow<List<MemberInvite>> =
        membershipState.flatMapLatest { mState ->
            val member = (mState as? UiState.Success)?.data
            if (member?.hasPermission(AppPermission.MANAGE_USERS) == true) {
                repository.observeInvites(institutionId)
            } else flowOf(emptyList())
        }
            .catch { error ->
                val hasFsError = generateSequence<Throwable>(error) { it.cause }.any { it is FirebaseFirestoreException }
                if (error is CancellationException && !hasFsError) {
                    throw error
                }
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val auditLogsState: StateFlow<List<AuditLog>> =
        membershipState.flatMapLatest { mState ->
            val member = (mState as? UiState.Success)?.data
            if (member?.hasPermission(AppPermission.VIEW_AUDIT) == true) {
                repository.observeAuditLogs(institutionId)
            } else flowOf(emptyList())
        }
            .catch { error ->
                val hasFsError = generateSequence<Throwable>(error) { it.cause }.any { it is FirebaseFirestoreException }
                if (error is CancellationException && !hasFsError) {
                    throw error
                }
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val dismissedEmergencyKeys: StateFlow<Set<String>> =
        prefs.dismissedEmergencyKeys.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val mutedCategories: StateFlow<Set<String>> =
        prefs.mutedCategories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    fun dismissEmergencyBanner(notice: Notice) {
        viewModelScope.launch {
            prefs.dismissEmergencyBanner(notice.id, notice.revisionNumber)
        }
    }

    fun toggleCategoryNotification(category: String) {
        viewModelScope.launch {
            prefs.toggleCategoryNotification(category)
        }
    }

    fun observeRevisions(noticeId: String): Flow<List<NoticeRevision>> =
        repository.observeNoticeRevisions(institutionId, noticeId)
            .catch { emit(emptyList()) }

    fun observeReadReceipts(noticeId: String): Flow<List<NoticeReadReceipt>> =
        repository.observeNoticeReadReceipts(institutionId, noticeId)
            .catch { emit(emptyList()) }

    /**
     * Section 13 Duplicate Detection: compares normalized title, category, and originating department
     * against recent non-archived notices.
     */
    fun findLikelyDuplicate(
        title: String,
        category: String,
        originDepartment: String,
        editingNoticeId: String?
    ): Notice? {
        val notices = (noticesState.value as? UiState.Success)?.data ?: return null
        val candidateNorm = InstitutionIdGenerator.normalizeNoticeTitle(title)
        if (candidateNorm.length < 4) return null
        return notices.firstOrNull { existing ->
            existing.id != editingNoticeId &&
                existing.lifecycleStatus !in listOf(NoticeLifecycleStatus.WITHDRAWN.name, NoticeLifecycleStatus.ARCHIVED.name) &&
                existing.category.equals(category, ignoreCase = true) &&
                existing.originDepartment.equals(originDepartment, ignoreCase = true) &&
                (existing.normalizedTitle == candidateNorm ||
                    (candidateNorm.length >= 8 && (existing.normalizedTitle.contains(candidateNorm) || candidateNorm.contains(existing.normalizedTitle))))
        }
    }

    fun publishOrUpdateNotice(
        existingNotice: Notice?,
        title: String,
        description: String,
        category: String,
        priority: NoticePriority,
        originDepartment: String,
        requestedTargets: List<String>,
        targetAudience: String,
        lifecycleStatus: NoticeLifecycleStatus,
        pinned: Boolean,
        attachments: List<NoticeAttachment>,
        publisher: InstitutionMembership,
        changeSummary: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _actionBusy.value = true
            val result = repository.publishOrUpdateNotice(
                instId = institutionId,
                existingNotice = existingNotice,
                title = title,
                description = description,
                category = category,
                priority = priority,
                originDepartment = originDepartment,
                requestedTargets = requestedTargets,
                targetAudience = targetAudience,
                lifecycleStatus = lifecycleStatus,
                pinned = pinned,
                attachments = attachments,
                publisher = publisher,
                changeSummary = changeSummary
            )
            _actionBusy.value = false
            result.onSuccess {
                _actionMessage.value = if (existingNotice == null) {
                    "Notice ${lifecycleStatus.label.lowercase()} successfully."
                } else {
                    "Canonical notice updated (Rev #${existingNotice.revisionNumber + 1})."
                }
                onSuccess()
            }.onFailure { err ->
                _actionMessage.value = err.toFriendlyFirestoreMessage("Failed to save notice.")
            }
        }
    }

    fun updateNoticeStatus(
        notice: Notice,
        newStatus: NoticeLifecycleStatus,
        pinned: Boolean,
        actor: InstitutionMembership,
        reason: String
    ) {
        viewModelScope.launch {
            _actionBusy.value = true
            val res = repository.updateNoticeStatus(institutionId, notice, newStatus, pinned, actor, reason)
            _actionBusy.value = false
            res.onSuccess {
                _actionMessage.value = "Notice marked ${newStatus.label}."
            }.onFailure { err ->
                _actionMessage.value = err.toFriendlyFirestoreMessage("Failed to update notice status.")
            }
        }
    }

    fun acknowledgeNotice(notice: Notice, member: InstitutionMembership) {
        if (member.joinMethod == "STUDENT_PASS") {
            _actionMessage.value = "Shared Student Pass does not track individual read receipts."
            return
        }
        viewModelScope.launch {
            val res = repository.markNoticeAcknowledged(institutionId, notice.id, member)
            res.onSuccess {
                _actionMessage.value = "Notice acknowledged."
            }.onFailure { err ->
                _actionMessage.value = err.toFriendlyFirestoreMessage("Could not save acknowledgement.")
            }
        }
    }

    fun saveEvent(
        existing: CampusEvent?,
        kind: EventKind,
        subType: String,
        title: String,
        description: String,
        organizer: String,
        venue: String,
        scheduleText: String,
        eligibility: String,
        registrationOpen: Boolean,
        teamsInfo: String,
        fixturesSummary: String,
        resultsSummary: String,
        status: EventStatus,
        actor: InstitutionMembership,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _actionBusy.value = true
            val res = repository.saveCampusEvent(
                instId = institutionId,
                existing = existing,
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
                actor = actor
            )
            _actionBusy.value = false
            res.onSuccess {
                _actionMessage.value = if (existing == null) "${kind.label} created." else "${kind.label} updated."
                onSuccess()
            }.onFailure { err ->
                _actionMessage.value = err.toFriendlyFirestoreMessage("Failed to save ${kind.label.lowercase()}.")
            }
        }
    }

    fun toggleRegistration(event: CampusEvent, member: InstitutionMembership, currentlyRegistered: Boolean) {
        viewModelScope.launch {
            val res = repository.toggleEventRegistration(institutionId, event, member, currentlyRegistered)
            res.onSuccess {
                _actionMessage.value = if (currentlyRegistered) "Registration cancelled." else "Registered for ${event.title}!"
            }.onFailure { err ->
                _actionMessage.value = err.toFriendlyFirestoreMessage("Registration update failed.")
            }
        }
    }

    fun submitFeedback(
        member: InstitutionMembership,
        confidential: Boolean,
        category: String,
        subject: String,
        message: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _actionBusy.value = true
            val res = repository.submitFeedback(institutionId, member, confidential, category, subject, message)
            _actionBusy.value = false
            res.onSuccess {
                _actionMessage.value = if (confidential) "Confidential feedback submitted securely." else "Feedback submitted."
                onSuccess()
            }.onFailure { err ->
                _actionMessage.value = err.toFriendlyFirestoreMessage("Feedback submission failed.")
            }
        }
    }

    fun respondToFeedback(
        item: FeedbackItem,
        newStatus: FeedbackStatus,
        reply: String,
        reviewer: InstitutionMembership
    ) {
        viewModelScope.launch {
            _actionBusy.value = true
            val res = repository.updateFeedbackResponse(institutionId, item, newStatus, reply, reviewer)
            _actionBusy.value = false
            res.onSuccess {
                _actionMessage.value = "Feedback updated to ${newStatus.label}."
            }.onFailure { err ->
                _actionMessage.value = err.toFriendlyFirestoreMessage("Could not update feedback.")
            }
        }
    }

    fun updateInstitutionSettings(
        name: String,
        tagline: String,
        contactEmail: String,
        contactPhone: String,
        accentColorKey: String,
        departments: List<String>,
        categories: List<String>,
        confidentialFeedbackEnabled: Boolean,
        actor: InstitutionMembership,
        auditSummary: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _actionBusy.value = true
            val res = repository.updateInstitutionSettings(
                instId = institutionId,
                name = name,
                tagline = tagline,
                contactEmail = contactEmail,
                contactPhone = contactPhone,
                accentColorKey = accentColorKey,
                departments = departments,
                categories = categories,
                confidentialFeedbackEnabled = confidentialFeedbackEnabled,
                actor = actor,
                auditSummary = auditSummary
            )
            _actionBusy.value = false
            res.onSuccess {
                _actionMessage.value = "Institution settings saved."
                onSuccess()
            }.onFailure { err ->
                _actionMessage.value = err.toFriendlyFirestoreMessage("Could not update settings.")
            }
        }
    }

    fun updateStudentPass(enabled: Boolean, newPass: String, actor: InstitutionMembership) {
        viewModelScope.launch {
            _actionBusy.value = true
            val res = repository.updateStudentPass(institutionId, enabled, newPass, actor)
            _actionBusy.value = false
            res.onSuccess {
                _actionMessage.value = if (enabled) "Student Entry Pass updated." else "Student Entry Pass disabled."
            }.onFailure { err ->
                _actionMessage.value = err.toFriendlyFirestoreMessage("Failed to update Student Entry Pass.")
            }
        }
    }

    fun inviteMember(
        email: String,
        displayName: String,
        designation: String,
        department: String,
        role: MemberRole,
        permissions: List<String>,
        scopedDepartments: List<String>,
        actor: InstitutionMembership,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _actionBusy.value = true
            val res = repository.inviteMember(
                instId = institutionId,
                email = email,
                displayName = displayName,
                designation = designation,
                department = department,
                role = role,
                permissions = permissions,
                scopedDepartments = scopedDepartments,
                actor = actor
            )
            _actionBusy.value = false
            res.onSuccess {
                _actionMessage.value = "Invitation created for $email (${role.label})."
                onSuccess()
            }.onFailure { err ->
                _actionMessage.value = err.toFriendlyFirestoreMessage("Failed to create invitation.")
            }
        }
    }

    fun updateMemberAccess(
        target: InstitutionMembership,
        newRole: MemberRole,
        newPermissions: List<String>,
        newStatus: MemberStatus,
        newDepartment: String,
        actor: InstitutionMembership
    ) {
        viewModelScope.launch {
            _actionBusy.value = true
            val res = repository.updateMemberAccess(institutionId, target, newRole, newPermissions, newStatus, newDepartment, actor)
            _actionBusy.value = false
            res.onSuccess {
                _actionMessage.value = "Updated access for ${target.displayName}."
            }.onFailure { err ->
                _actionMessage.value = err.toFriendlyFirestoreMessage("Failed to update member access.")
            }
        }
    }

    fun updateDriveConnection(
        connected: Boolean,
        driveAccountEmail: String,
        driveFolderName: String,
        driveFolderId: String,
        actor: InstitutionMembership
    ) {
        viewModelScope.launch {
            _actionBusy.value = true
            val res = repository.updateDriveConnection(
                instId = institutionId,
                connected = connected,
                driveAccountEmail = driveAccountEmail,
                driveFolderName = driveFolderName,
                driveFolderId = driveFolderId,
                actor = actor
            )
            _actionBusy.value = false
            res.onSuccess {
                _actionMessage.value = if (connected) "Google Drive connection configured." else "Google Drive disconnected."
            }.onFailure { err ->
                _actionMessage.value = err.toFriendlyFirestoreMessage("Failed to update Drive connection.")
            }
        }
    }
}
