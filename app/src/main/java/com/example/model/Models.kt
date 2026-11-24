package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class MemberRole(val label: String) {
    DEAN("Dean"),
    ADMIN("Admin"),
    STAFF("Staff"),
    STUDENT_LEADER("Student Leader"),
    STUDENT("Student");

    companion object {
        fun fromRaw(raw: String?): MemberRole =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: STUDENT
    }
}

enum class AppPermission(val key: String, val label: String, val studentSideAllowed: Boolean) {
    READ_NOTICE("READ_NOTICE", "Read Notices", true),
    VIEW_EVENTS("VIEW_EVENTS", "View Events & Sports", true),
    SUBMIT_FEEDBACK("SUBMIT_FEEDBACK", "Submit Feedback", true),
    CREATE_NOTICE("CREATE_NOTICE", "Create Draft Notices", false),
    EDIT_NOTICE("EDIT_NOTICE", "Edit & Revise Notices", false),
    APPROVE_NOTICE("APPROVE_NOTICE", "Approve Notices", false),
    PUBLISH_MAJOR_BOARD("PUBLISH_MAJOR_BOARD", "Publish to Major Board", false),
    PUBLISH_DEPT_BOARD("PUBLISH_DEPT_BOARD", "Publish to Dept Boards", false),
    PIN_EMERGENCY("PIN_EMERGENCY", "Pin Emergency Alerts", false),
    WITHDRAW_ARCHIVE("WITHDRAW_ARCHIVE", "Withdraw & Archive", false),
    VIEW_ANALYTICS("VIEW_ANALYTICS", "View Readership Analytics", false),
    MANAGE_USERS("MANAGE_USERS", "Manage Staff & Accounts", false),
    MANAGE_EVENTS("MANAGE_EVENTS", "Manage Campus Activities", true),
    MANAGE_SPORTS("MANAGE_SPORTS", "Manage Sports & Fixtures", true),
    VIEW_FEEDBACK("VIEW_FEEDBACK", "Review Normal Feedback", false),
    VIEW_CONFIDENTIAL_FEEDBACK("VIEW_CONFIDENTIAL_FEEDBACK", "Review Confidential Feedback", false),
    MANAGE_DRIVE("MANAGE_DRIVE", "Manage Google Drive", false),
    VIEW_AUDIT("VIEW_AUDIT", "View Audit & Security Logs", false);

    companion object {
        val defaultDeanPermissions = entries.map { it.key }
        val defaultStudentPermissions = listOf(READ_NOTICE.key, VIEW_EVENTS.key, SUBMIT_FEEDBACK.key)
        val defaultStudentLeaderPermissions = listOf(
            READ_NOTICE.key, VIEW_EVENTS.key, SUBMIT_FEEDBACK.key, MANAGE_EVENTS.key, MANAGE_SPORTS.key
        )
        val defaultStaffPermissions = listOf(
            READ_NOTICE.key, VIEW_EVENTS.key, SUBMIT_FEEDBACK.key,
            CREATE_NOTICE.key, EDIT_NOTICE.key, PUBLISH_DEPT_BOARD.key,
            MANAGE_EVENTS.key, MANAGE_SPORTS.key, VIEW_FEEDBACK.key
        )
        val defaultAdminPermissions = listOf(
            READ_NOTICE.key, VIEW_EVENTS.key, SUBMIT_FEEDBACK.key,
            CREATE_NOTICE.key, EDIT_NOTICE.key, APPROVE_NOTICE.key,
            PUBLISH_MAJOR_BOARD.key, PUBLISH_DEPT_BOARD.key, PIN_EMERGENCY.key,
            WITHDRAW_ARCHIVE.key, VIEW_ANALYTICS.key, MANAGE_EVENTS.key,
            MANAGE_SPORTS.key, VIEW_FEEDBACK.key
        )
    }
}

enum class MemberStatus(val label: String) {
    ACTIVE("Active"),
    SUSPENDED("Suspended"),
    REVOKED("Revoked");

    companion object {
        fun fromRaw(raw: String?): MemberStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: ACTIVE
    }
}

enum class NoticePriority(val label: String) {
    EMERGENCY("Emergency"),
    URGENT("Urgent"),
    NORMAL("Normal");

    companion object {
        fun fromRaw(raw: String?): NoticePriority =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: NORMAL
    }
}

enum class NoticeLifecycleStatus(val label: String) {
    DRAFT("Draft"),
    SCHEDULED("Scheduled"),
    PUBLISHED("Published"),
    UPDATED("Updated"),
    WITHDRAWN("Withdrawn"),
    EXPIRED("Expired"),
    ARCHIVED("Archived");

    companion object {
        fun fromRaw(raw: String?): NoticeLifecycleStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PUBLISHED
    }
}

enum class EventKind(val label: String) {
    ACTIVITY("Activity"),
    SPORT("Sport");

    companion object {
        fun fromRaw(raw: String?): EventKind =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: ACTIVITY
    }
}

enum class EventStatus(val label: String) {
    UPCOMING("Upcoming"),
    ONGOING("Ongoing"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    companion object {
        fun fromRaw(raw: String?): EventStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: UPCOMING
    }
}

enum class FeedbackStatus(val label: String) {
    SUBMITTED("Submitted"),
    UNDER_REVIEW("Under Review"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved"),
    CLOSED("Closed");

    companion object {
        fun fromRaw(raw: String?): FeedbackStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: SUBMITTED
    }
}

@Keep
@IgnoreExtraProperties
data class UserInstitutionLink(
    @DocumentId val id: String = "",
    val uid: String = "",
    val institutionId: String = "",
    val institutionName: String = "",
    val role: String = MemberRole.STUDENT.name,
    val accentColorKey: String = "INDIGO",
    @ServerTimestamp val joinedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class InstitutionCode(
    @DocumentId val id: String = "",
    val institutionId: String = "",
    val name: String = "",
    val tagline: String = "",
    val accentColorKey: String = "INDIGO",
    val deanUid: String = "",
    val studentPassEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Institution(
    @DocumentId val id: String = "",
    val institutionId: String = "",
    val name: String = "",
    val tagline: String = "",
    val contactEmail: String = "",
    val contactPhone: String = "",
    val accentColorKey: String = "INDIGO",
    val deanUid: String = "",
    val deanName: String = "",
    val studentPassEnabled: Boolean = true,
    val studentPassHash: String = "",
    val confidentialFeedbackEnabled: Boolean = true,
    val driveConnected: Boolean = false,
    val driveAccountEmail: String = "",
    val driveFolderName: String = "",
    val driveFolderId: String = "",
    val departments: List<String> = DEFAULT_DEPARTMENTS,
    val categories: List<String> = DEFAULT_CATEGORIES,
    val priorities: List<String> = DEFAULT_PRIORITIES,
    val activityTypes: List<String> = DEFAULT_ACTIVITY_TYPES,
    val sportTypes: List<String> = DEFAULT_SPORT_TYPES,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        val DEFAULT_DEPARTMENTS = listOf("CSE", "ISE", "AIML", "ECE", "EEE", "ME", "CE", "CHE", "BT", "Other")
        val DEFAULT_CATEGORIES = listOf("Circular", "Exam & Results", "Timetable", "Placement", "Holiday", "Emergency", "Department", "General")
        val DEFAULT_PRIORITIES = listOf("NORMAL", "URGENT", "EMERGENCY")
        val DEFAULT_ACTIVITY_TYPES = listOf("Technical", "Cultural", "Social", "Academic", "Student Welfare")
        val DEFAULT_SPORT_TYPES = listOf("Cricket", "Football", "Volleyball", "Kabaddi", "Athletics", "Badminton")
    }
}

@Keep
@IgnoreExtraProperties
data class InstitutionMembership(
    @DocumentId val id: String = "",
    val uid: String = "",
    val institutionId: String = "",
    val displayName: String = "",
    val email: String = "",
    val designation: String = "",
    val department: String = "",
    val role: String = MemberRole.STUDENT.name,
    val permissions: List<String> = emptyList(),
    val scopedDepartments: List<String> = emptyList(),
    val status: String = MemberStatus.ACTIVE.name,
    val joinMethod: String = "STUDENT_PASS",
    val passHashProof: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun roleEnum(): MemberRole = MemberRole.fromRaw(role)
    fun statusEnum(): MemberStatus = MemberStatus.fromRaw(status)
    fun hasPermission(perm: AppPermission): Boolean {
        if (statusEnum() != MemberStatus.ACTIVE) return false
        if (roleEnum() == MemberRole.DEAN) return true
        return permissions.contains(perm.key)
    }
}

@Keep
@IgnoreExtraProperties
data class MemberInvite(
    @DocumentId val id: String = "",
    val email: String = "",
    val institutionId: String = "",
    val displayName: String = "",
    val designation: String = "",
    val department: String = "",
    val role: String = MemberRole.STAFF.name,
    val permissions: List<String> = emptyList(),
    val scopedDepartments: List<String> = emptyList(),
    val status: String = "PENDING",
    val invitedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class NoticeAttachment(
    val id: String = "",
    val name: String = "",
    val mimeType: String = "application/pdf",
    val sizeBytes: Long = 0L,
    val storageTarget: String = "GOOGLE_DRIVE",
    val driveFileId: String = "",
    val driveUrl: String = "",
    val localUri: String = "",
    val uploadStatus: String = "VERIFIED" // VERIFIED, PENDING, FAILED
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "mimeType" to mimeType,
        "sizeBytes" to sizeBytes,
        "storageTarget" to storageTarget,
        "driveFileId" to driveFileId,
        "driveUrl" to driveUrl,
        "localUri" to localUri,
        "uploadStatus" to uploadStatus
    )
}

@Keep
@IgnoreExtraProperties
data class Notice(
    @DocumentId val id: String = "",
    val institutionId: String = "",
    val title: String = "",
    val normalizedTitle: String = "",
    val description: String = "",
    val category: String = "Circular",
    val priority: String = NoticePriority.NORMAL.name,
    val originDepartment: String = "Administration",
    val visibilityTargets: List<String> = listOf("MAJOR_BOARD"),
    val targetAudience: String = "All Students",
    val lifecycleStatus: String = NoticeLifecycleStatus.PUBLISHED.name,
    val pinned: Boolean = false,
    val publisherUid: String = "",
    val publisherName: String = "",
    val publisherRole: String = MemberRole.DEAN.name,
    val revisionNumber: Long = 1L,
    val idempotencyKey: String = "",
    val attachments: List<Map<String, Any?>> = emptyList(),
    val scheduledPublishAt: Timestamp? = null,
    val publishedAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun priorityEnum(): NoticePriority = NoticePriority.fromRaw(priority)
    fun lifecycleEnum(): NoticeLifecycleStatus = NoticeLifecycleStatus.fromRaw(lifecycleStatus)
    fun parsedAttachments(): List<NoticeAttachment> = attachments.map { raw ->
        NoticeAttachment(
            id = raw["id"] as? String ?: "",
            name = raw["name"] as? String ?: "Attachment",
            mimeType = raw["mimeType"] as? String ?: "application/octet-stream",
            sizeBytes = (raw["sizeBytes"] as? Number)?.toLong() ?: 0L,
            storageTarget = raw["storageTarget"] as? String ?: "GOOGLE_DRIVE",
            driveFileId = raw["driveFileId"] as? String ?: "",
            driveUrl = raw["driveUrl"] as? String ?: "",
            localUri = raw["localUri"] as? String ?: "",
            uploadStatus = raw["uploadStatus"] as? String ?: "VERIFIED"
        )
    }
}

@Keep
@IgnoreExtraProperties
data class NoticeRevision(
    @DocumentId val id: String = "",
    val institutionId: String = "",
    val noticeId: String = "",
    val revisionNumber: Long = 1L,
    val title: String = "",
    val description: String = "",
    val lifecycleStatus: String = NoticeLifecycleStatus.UPDATED.name,
    val editedByUid: String = "",
    val editedByName: String = "",
    val changeSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class NoticeReadReceipt(
    @DocumentId val id: String = "",
    val institutionId: String = "",
    val noticeId: String = "",
    val uid: String = "",
    val displayName: String = "",
    val acknowledged: Boolean = true,
    @ServerTimestamp val readAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CampusEvent(
    @DocumentId val id: String = "",
    val institutionId: String = "",
    val eventKind: String = EventKind.ACTIVITY.name,
    val subType: String = "Technical",
    val title: String = "",
    val description: String = "",
    val organizer: String = "",
    val venue: String = "",
    val scheduleText: String = "",
    val eligibility: String = "Open to All",
    val registrationOpen: Boolean = true,
    val registeredCount: Long = 0L,
    val teamsInfo: String = "",
    val fixturesSummary: String = "",
    val resultsSummary: String = "",
    val status: String = EventStatus.UPCOMING.name,
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun kindEnum(): EventKind = EventKind.fromRaw(eventKind)
    fun statusEnum(): EventStatus = EventStatus.fromRaw(status)
}

@Keep
@IgnoreExtraProperties
data class EventRegistration(
    @DocumentId val id: String = "",
    val institutionId: String = "",
    val eventId: String = "",
    val uid: String = "",
    val displayName: String = "",
    val department: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FeedbackItem(
    @DocumentId val id: String = "",
    val institutionId: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val authorEmail: String = "",
    val confidential: Boolean = false,
    val category: String = "Suggestions",
    val subject: String = "",
    val message: String = "",
    val status: String = FeedbackStatus.SUBMITTED.name,
    val staffReply: String = "",
    val reviewedByName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): FeedbackStatus = FeedbackStatus.fromRaw(status)
}

@Keep
@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val institutionId: String = "",
    val actorUid: String = "",
    val actorName: String = "",
    val actorRole: String = "",
    val actionType: String = "",
    val targetSummary: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
