package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AppPermission
import com.example.model.AuditLog
import com.example.model.CampusEvent
import com.example.model.EventKind
import com.example.model.EventRegistration
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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CampusRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUid(): String = auth.currentUser?.uid
        ?: throw IllegalStateException("Please sign in with Google before accessing campus data.")

    private fun <T> Flow<T>.logFirestoreFailure(op: OperationType, path: String, expectedUid: String): Flow<T> =
        catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val toLog = firestoreError ?: (error as? Exception)
            if (toLog != null && auth.currentUser?.uid == expectedUid) {
                handleFirestoreError(toLog, op, path)
            }
            throw error
        }

    fun observeInstitution(instId: String): Flow<Institution?> = flow {
        val uid = requireUid()
        val path = "institutions/$instId"
        emitAll(
            db.collection("institutions").document(instId)
                .snapshots()
                .map { it.toObject(Institution::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreFailure(OperationType.GET, path, uid)
        )
    }

    fun observeMembership(instId: String, memberUid: String): Flow<InstitutionMembership?> = flow {
        val uid = requireUid()
        val path = "institutions/$instId/memberships/$memberUid"
        emitAll(
            db.collection("institutions").document(instId)
                .collection("memberships").document(memberUid)
                .snapshots()
                .map { it.toObject(InstitutionMembership::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreFailure(OperationType.GET, path, uid)
        )
    }

    fun observeMembers(instId: String): Flow<List<InstitutionMembership>> = flow {
        val uid = requireUid()
        val path = "institutions/$instId/memberships"
        emitAll(
            db.collection("institutions").document(instId).collection("memberships")
                .snapshots()
                .map { snap ->
                    snap.toObjects(InstitutionMembership::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.displayName.lowercase() }
                }
                .logFirestoreFailure(OperationType.LIST, path, uid)
        )
    }

    fun observeInvites(instId: String): Flow<List<MemberInvite>> = flow {
        val uid = requireUid()
        val path = "institutions/$instId/invites"
        emitAll(
            db.collection("institutions").document(instId).collection("invites")
                .snapshots()
                .map { snap ->
                    snap.toObjects(MemberInvite::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreFailure(OperationType.LIST, path, uid)
        )
    }

    fun observeNotices(instId: String): Flow<List<Notice>> = flow {
        val uid = requireUid()
        val path = "institutions/$instId/notices"
        emitAll(
            db.collection("institutions").document(instId).collection("notices")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Notice::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareByDescending<Notice> { it.pinned && it.priority == NoticePriority.EMERGENCY.name }
                                .thenByDescending { it.pinned }
                                .thenByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                        )
                }
                .logFirestoreFailure(OperationType.LIST, path, uid)
        )
    }

    fun observeNoticeRevisions(instId: String, noticeId: String): Flow<List<NoticeRevision>> = flow {
        val uid = requireUid()
        val path = "institutions/$instId/notices/$noticeId/revisions"
        emitAll(
            db.collection("institutions").document(instId)
                .collection("notices").document(noticeId)
                .collection("revisions")
                .snapshots()
                .map { snap ->
                    snap.toObjects(NoticeRevision::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.revisionNumber }
                }
                .logFirestoreFailure(OperationType.LIST, path, uid)
        )
    }

    fun observeNoticeReadReceipts(instId: String, noticeId: String): Flow<List<NoticeReadReceipt>> = flow {
        val uid = requireUid()
        val path = "institutions/$instId/notices/$noticeId/reads"
        emitAll(
            db.collection("institutions").document(instId)
                .collection("notices").document(noticeId)
                .collection("reads")
                .snapshots()
                .map { snap ->
                    snap.toObjects(NoticeReadReceipt::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .logFirestoreFailure(OperationType.LIST, path, uid)
        )
    }

    fun observeEvents(instId: String): Flow<List<CampusEvent>> = flow {
        val uid = requireUid()
        val path = "institutions/$instId/events"
        emitAll(
            db.collection("institutions").document(instId).collection("events")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CampusEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreFailure(OperationType.LIST, path, uid)
        )
    }

    fun observeMyEventRegistrations(instId: String, memberUid: String): Flow<Set<String>> = flow {
        val uid = requireUid()
        val path = "institutions/$instId/events"
        emitAll(
            db.collection("institutions").document(instId).collection("events")
                .snapshots()
                .map { eventsSnap ->
                    val registered = mutableSetOf<String>()
                    for (doc in eventsSnap.documents) {
                        val regSnap = doc.reference.collection("registrations").document(memberUid).get().await()
                        if (regSnap.exists()) registered.add(doc.id)
                    }
                    registered.toSet()
                }
                .logFirestoreFailure(OperationType.LIST, path, uid)
        )
    }

    fun observeMyFeedback(instId: String, authorUid: String): Flow<List<FeedbackItem>> = flow {
        val uid = requireUid()
        val path = "institutions/$instId/feedback"
        emitAll(
            db.collection("institutions").document(instId).collection("feedback")
                .whereEqualTo("authorUid", authorUid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FeedbackItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreFailure(OperationType.LIST, path, uid)
        )
    }

    fun observeNormalFeedback(instId: String): Flow<List<FeedbackItem>> = flow {
        val uid = requireUid()
        val path = "institutions/$instId/feedback"
        emitAll(
            db.collection("institutions").document(instId).collection("feedback")
                .whereEqualTo("confidential", false)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FeedbackItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreFailure(OperationType.LIST, path, uid)
        )
    }

    fun observeConfidentialFeedback(instId: String): Flow<List<FeedbackItem>> = flow {
        val uid = requireUid()
        val path = "institutions/$instId/feedback"
        emitAll(
            db.collection("institutions").document(instId).collection("feedback")
                .whereEqualTo("confidential", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FeedbackItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreFailure(OperationType.LIST, path, uid)
        )
    }

    fun observeAuditLogs(instId: String): Flow<List<AuditLog>> = flow {
        val uid = requireUid()
        val path = "institutions/$instId/audit_logs"
        emitAll(
            db.collection("institutions").document(instId).collection("audit_logs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .snapshots()
                .map { snap ->
                    snap.toObjects(AuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .logFirestoreFailure(OperationType.LIST, path, uid)
        )
    }

    /**
     * Publishes or saves a canonical notice.
     * Enforces Section 12 Critical Rule: when a department-originated notice targets MAJOR_BOARD,
     * its visibilityTargets automatically mirror to its originating department board on one canonical doc.
     */
    suspend fun publishOrUpdateNotice(
        instId: String,
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
        changeSummary: String
    ): Result<String> {
        val noticeId = existingNotice?.id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val path = "institutions/$instId/notices/$noticeId"
        return try {
            val uid = requireUid()
            require(title.trim().length in 3..200) { "Notice title must be between 3 and 200 characters." }
            require(description.trim().length in 3..8000) { "Notice body must be between 3 and 8000 characters." }
            require(attachments.none { it.uploadStatus == "FAILED" || it.uploadStatus == "PENDING" } || lifecycleStatus == NoticeLifecycleStatus.DRAFT) {
                "Verify or retry pending attachments before publishing."
            }

            val deptTarget = "DEPT_${originDepartment.trim()}"
            val canonicalTargets = requestedTargets.toMutableSet()
            if (canonicalTargets.contains("MAJOR_BOARD") && originDepartment != "Administration" && originDepartment.isNotBlank()) {
                // Automatic canonical mirroring to origin department
                canonicalTargets.add(deptTarget)
            }
            if (canonicalTargets.isEmpty()) canonicalTargets.add("MAJOR_BOARD")

            val noticeRef = db.collection("institutions").document(instId)
                .collection("notices").document(noticeId)
            val batch = db.batch()
            val serializedAttachments = attachments.take(10).map { it.toMap() }
            val normalized = InstitutionIdGenerator.normalizeNoticeTitle(title)

            if (existingNotice == null) {
                val idempotencyKey = UUID.randomUUID().toString()
                val payload = mapOf(
                    "institutionId" to instId,
                    "title" to title.trim(),
                    "normalizedTitle" to normalized,
                    "description" to description.trim(),
                    "category" to category.trim(),
                    "priority" to priority.name,
                    "originDepartment" to originDepartment.trim(),
                    "visibilityTargets" to canonicalTargets.toList(),
                    "targetAudience" to targetAudience.trim().ifBlank { "All Students" },
                    "lifecycleStatus" to lifecycleStatus.name,
                    "pinned" to (pinned || priority == NoticePriority.EMERGENCY),
                    "publisherUid" to uid,
                    "publisherName" to publisher.displayName,
                    "publisherRole" to publisher.role,
                    "revisionNumber" to 1L,
                    "idempotencyKey" to idempotencyKey,
                    "attachments" to serializedAttachments,
                    "publishedAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(noticeRef, payload)

                val revRef = noticeRef.collection("revisions").document("rev_1")
                batch.set(
                    revRef,
                    mapOf(
                        "institutionId" to instId,
                        "noticeId" to noticeId,
                        "revisionNumber" to 1L,
                        "title" to title.trim(),
                        "description" to description.trim(),
                        "lifecycleStatus" to lifecycleStatus.name,
                        "editedByUid" to uid,
                        "editedByName" to publisher.displayName,
                        "changeSummary" to "Initial ${lifecycleStatus.label.lowercase()} notice",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                val nextRev = existingNotice.revisionNumber + 1L
                val nextStatus = if (
                    lifecycleStatus == NoticeLifecycleStatus.PUBLISHED &&
                    existingNotice.lifecycleEnum() != NoticeLifecycleStatus.DRAFT &&
                    existingNotice.revisionNumber >= 1L
                ) {
                    NoticeLifecycleStatus.UPDATED
                } else lifecycleStatus

                batch.update(
                    noticeRef,
                    mapOf(
                        "title" to title.trim(),
                        "normalizedTitle" to normalized,
                        "description" to description.trim(),
                        "category" to category.trim().ifBlank { "General" }.take(60),
                        "priority" to priority.name,
                        "originDepartment" to originDepartment.trim().ifBlank { "Administration" }.take(80),
                        "visibilityTargets" to canonicalTargets.toList(),
                        "targetAudience" to targetAudience.trim().ifBlank { "All Students" }.take(120),
                        "lifecycleStatus" to nextStatus.name,
                        "pinned" to pinned,
                        "revisionNumber" to nextRev,
                        "attachments" to serializedAttachments,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val revRef = noticeRef.collection("revisions").document("rev_$nextRev")
                batch.set(
                    revRef,
                    mapOf(
                        "institutionId" to instId,
                        "noticeId" to noticeId,
                        "revisionNumber" to nextRev,
                        "title" to title.trim(),
                        "description" to description.trim(),
                        "lifecycleStatus" to nextStatus.name,
                        "editedByUid" to uid,
                        "editedByName" to publisher.displayName.ifBlank { "Member" }.take(100),
                        "changeSummary" to changeSummary.trim().ifBlank { "Revision #$nextRev correction" }.take(300),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            val auditRef = db.collection("institutions").document(instId)
                .collection("audit_logs").document(UUID.randomUUID().toString())
            batch.set(
                auditRef,
                mapOf(
                    "institutionId" to instId,
                    "actorUid" to uid,
                    "actorName" to publisher.displayName.ifBlank { "Member" }.take(100),
                    "actorRole" to publisher.role,
                    "actionType" to if (existingNotice == null) "NOTICE_CREATED" else "NOTICE_REVISED",
                    "targetSummary" to title.trim().take(180),
                    "details" to "Targets: ${canonicalTargets.joinToString(", ")} | Status: ${lifecycleStatus.name}".take(1000),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(noticeId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateNoticeStatus(
        instId: String,
        notice: Notice,
        newStatus: NoticeLifecycleStatus,
        pinned: Boolean,
        actor: InstitutionMembership,
        reason: String
    ): Result<Unit> {
        val path = "institutions/$instId/notices/${notice.id}"
        return try {
            val uid = requireUid()
            val noticeRef = db.collection("institutions").document(instId)
                .collection("notices").document(notice.id)
            val nextRev = notice.revisionNumber + 1L
            val batch = db.batch()
            batch.update(
                noticeRef,
                mapOf(
                    "lifecycleStatus" to newStatus.name,
                    "pinned" to pinned,
                    "revisionNumber" to nextRev,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                noticeRef.collection("revisions").document("rev_$nextRev"),
                mapOf(
                    "institutionId" to instId,
                    "noticeId" to notice.id,
                    "revisionNumber" to nextRev,
                    "title" to notice.title,
                    "description" to notice.description,
                    "lifecycleStatus" to newStatus.name,
                    "editedByUid" to uid,
                    "editedByName" to actor.displayName,
                    "changeSummary" to reason.ifBlank { "Status changed to ${newStatus.label}" },
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("institutions").document(instId)
                    .collection("audit_logs").document(UUID.randomUUID().toString()),
                mapOf(
                    "institutionId" to instId,
                    "actorUid" to uid,
                    "actorName" to actor.displayName,
                    "actorRole" to actor.role,
                    "actionType" to "NOTICE_${newStatus.name}",
                    "targetSummary" to notice.title.take(180),
                    "details" to reason.ifBlank { "Lifecycle updated to ${newStatus.label}, pinned=$pinned" },
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markNoticeAcknowledged(
        instId: String,
        noticeId: String,
        member: InstitutionMembership
    ): Result<Unit> {
        val path = "institutions/$instId/notices/$noticeId/reads/${member.uid}"
        return try {
            val uid = requireUid()
            db.document(path).set(
                mapOf(
                    "institutionId" to instId,
                    "noticeId" to noticeId,
                    "uid" to uid,
                    "displayName" to member.displayName,
                    "acknowledged" to true,
                    "readAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveCampusEvent(
        instId: String,
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
        actor: InstitutionMembership
    ): Result<Unit> {
        val eventId = existing?.id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val path = "institutions/$instId/events/$eventId"
        return try {
            val uid = requireUid()
            require(title.trim().length in 3..160) { "Event title must be 3–160 characters." }
            require(description.trim().length in 3..4000) { "Description must be 3–4000 characters." }
            val ref = db.collection("institutions").document(instId).collection("events").document(eventId)
            if (existing == null) {
                ref.set(
                    mapOf(
                        "institutionId" to instId,
                        "eventKind" to kind.name,
                        "subType" to subType.trim(),
                        "title" to title.trim(),
                        "description" to description.trim(),
                        "organizer" to organizer.trim().ifBlank { actor.displayName },
                        "venue" to venue.trim(),
                        "scheduleText" to scheduleText.trim(),
                        "eligibility" to eligibility.trim().ifBlank { "Open to All" },
                        "registrationOpen" to registrationOpen,
                        "registeredCount" to 0L,
                        "teamsInfo" to teamsInfo.trim(),
                        "fixturesSummary" to fixturesSummary.trim(),
                        "resultsSummary" to resultsSummary.trim(),
                        "status" to status.name,
                        "createdByUid" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.update(
                    mapOf(
                        "eventKind" to kind.name,
                        "subType" to subType.trim(),
                        "title" to title.trim(),
                        "description" to description.trim(),
                        "organizer" to organizer.trim(),
                        "venue" to venue.trim(),
                        "scheduleText" to scheduleText.trim(),
                        "eligibility" to eligibility.trim(),
                        "registrationOpen" to registrationOpen,
                        "teamsInfo" to teamsInfo.trim(),
                        "fixturesSummary" to fixturesSummary.trim(),
                        "resultsSummary" to resultsSummary.trim(),
                        "status" to status.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleEventRegistration(
        instId: String,
        event: CampusEvent,
        member: InstitutionMembership,
        currentlyRegistered: Boolean
    ): Result<Unit> {
        val path = "institutions/$instId/events/${event.id}/registrations/${member.uid}"
        return try {
            val uid = requireUid()
            val eventRef = db.collection("institutions").document(instId).collection("events").document(event.id)
            val regRef = eventRef.collection("registrations").document(uid)
            val batch = db.batch()
            val safeMemberName = member.displayName.ifBlank { "Member" }.take(100)
            if (currentlyRegistered) {
                batch.delete(regRef)
                if (event.registeredCount > 0L) {
                    batch.update(
                        eventRef,
                        mapOf(
                            "registeredCount" to FieldValue.increment(-1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            } else {
                batch.set(
                    regRef,
                    mapOf(
                        "institutionId" to instId,
                        "eventId" to event.id,
                        "uid" to uid,
                        "displayName" to safeMemberName,
                        "department" to member.department.take(80),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    eventRef,
                    mapOf(
                        "registeredCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun submitFeedback(
        instId: String,
        member: InstitutionMembership,
        confidential: Boolean,
        category: String,
        subject: String,
        message: String
    ): Result<Unit> {
        val fbId = UUID.randomUUID().toString()
        val path = "institutions/$instId/feedback/$fbId"
        return try {
            val uid = requireUid()
            require(subject.trim().length in 3..180) { "Subject must be 3–180 characters." }
            require(message.trim().length in 5..4000) { "Message must be 5–4000 characters." }
            db.document(path).set(
                mapOf(
                    "institutionId" to instId,
                    "authorUid" to uid,
                    "authorName" to member.displayName,
                    "authorEmail" to member.email,
                    "confidential" to confidential,
                    "category" to category.trim(),
                    "subject" to subject.trim(),
                    "message" to message.trim(),
                    "status" to FeedbackStatus.SUBMITTED.name,
                    "staffReply" to "",
                    "reviewedByName" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateFeedbackResponse(
        instId: String,
        feedback: FeedbackItem,
        newStatus: FeedbackStatus,
        staffReply: String,
        reviewer: InstitutionMembership
    ): Result<Unit> {
        val path = "institutions/$instId/feedback/${feedback.id}"
        return try {
            val uid = requireUid()
            val batch = db.batch()
            batch.update(
                db.document(path),
                mapOf(
                    "status" to newStatus.name,
                    "staffReply" to staffReply.trim(),
                    "reviewedByName" to reviewer.displayName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (feedback.confidential) {
                batch.set(
                    db.collection("institutions").document(instId)
                        .collection("audit_logs").document(UUID.randomUUID().toString()),
                    mapOf(
                        "institutionId" to instId,
                        "actorUid" to uid,
                        "actorName" to reviewer.displayName,
                        "actorRole" to reviewer.role,
                        "actionType" to "CONFIDENTIAL_FEEDBACK_REVIEWED",
                        "targetSummary" to feedback.subject.take(180),
                        "details" to "Updated status to ${newStatus.label}",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- DEAN CONTROL CENTRE MUTATIONS ---

    suspend fun updateInstitutionSettings(
        instId: String,
        name: String,
        tagline: String,
        contactEmail: String,
        contactPhone: String,
        accentColorKey: String,
        departments: List<String>,
        categories: List<String>,
        confidentialFeedbackEnabled: Boolean,
        actor: InstitutionMembership,
        auditSummary: String
    ): Result<Unit> {
        val path = "institutions/$instId"
        return try {
            val uid = requireUid()
            val batch = db.batch()
            batch.update(
                db.document(path),
                mapOf(
                    "name" to name.trim(),
                    "tagline" to tagline.trim(),
                    "contactEmail" to contactEmail.trim(),
                    "contactPhone" to contactPhone.trim(),
                    "accentColorKey" to accentColorKey,
                    "departments" to departments.filter { it.isNotBlank() }.distinct(),
                    "categories" to categories.filter { it.isNotBlank() }.distinct(),
                    "confidentialFeedbackEnabled" to confidentialFeedbackEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("institution_codes").document(instId),
                mapOf(
                    "name" to name.trim(),
                    "tagline" to tagline.trim(),
                    "accentColorKey" to accentColorKey,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("institutions").document(instId)
                    .collection("audit_logs").document(UUID.randomUUID().toString()),
                mapOf(
                    "institutionId" to instId,
                    "actorUid" to uid,
                    "actorName" to actor.displayName,
                    "actorRole" to actor.role,
                    "actionType" to "INSTITUTION_SETTINGS_UPDATED",
                    "targetSummary" to name.trim().take(180),
                    "details" to auditSummary.take(1000),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateStudentPass(
        instId: String,
        enabled: Boolean,
        newPasscode: String,
        actor: InstitutionMembership
    ): Result<Unit> {
        val path = "institutions/$instId"
        return try {
            val uid = requireUid()
            val cleanPass = newPasscode.trim()
            if (enabled) {
                require(cleanPass.length >= 4) { "Student pass must be at least 4 characters." }
            }
            val passHash = if (cleanPass.isNotBlank()) {
                InstitutionIdGenerator.hashPasscode(instId, cleanPass)
            } else ""

            val batch = db.batch()
            val instUpdate = mutableMapOf<String, Any>(
                "studentPassEnabled" to enabled,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (passHash.isNotEmpty()) {
                instUpdate["studentPassHash"] = passHash
            }
            batch.update(db.document(path), instUpdate)
            batch.update(
                db.collection("institution_codes").document(instId),
                mapOf(
                    "studentPassEnabled" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("institutions").document(instId)
                    .collection("audit_logs").document(UUID.randomUUID().toString()),
                mapOf(
                    "institutionId" to instId,
                    "actorUid" to uid,
                    "actorName" to actor.displayName,
                    "actorRole" to actor.role,
                    "actionType" to "STUDENT_PASS_UPDATED",
                    "targetSummary" to instId,
                    "details" to "Student entry pass enabled=$enabled, rotated=${passHash.isNotEmpty()}",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun inviteMember(
        instId: String,
        email: String,
        displayName: String,
        designation: String,
        department: String,
        role: MemberRole,
        permissions: List<String>,
        scopedDepartments: List<String>,
        actor: InstitutionMembership
    ): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        val path = "institutions/$instId/invites/$cleanEmail"
        return try {
            val uid = requireUid()
            require(cleanEmail.contains("@")) { "Enter a valid Google account email." }
            require(role in listOf(MemberRole.ADMIN, MemberRole.STAFF, MemberRole.STUDENT_LEADER)) {
                "Invites support Admin, Staff, or Student Leader roles."
            }
            val filteredPerms = if (role == MemberRole.STUDENT_LEADER) {
                permissions.filter { p -> AppPermission.entries.firstOrNull { it.key == p }?.studentSideAllowed == true }
                    .ifEmpty { AppPermission.defaultStudentLeaderPermissions }
            } else {
                permissions
            }
            val batch = db.batch()
            batch.set(
                db.document(path),
                mapOf(
                    "email" to cleanEmail,
                    "institutionId" to instId,
                    "displayName" to displayName.trim().ifBlank { cleanEmail.substringBefore("@") },
                    "designation" to designation.trim(),
                    "department" to department.trim(),
                    "role" to role.name,
                    "permissions" to filteredPerms,
                    "scopedDepartments" to scopedDepartments,
                    "status" to "PENDING",
                    "invitedByUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("institutions").document(instId)
                    .collection("audit_logs").document(UUID.randomUUID().toString()),
                mapOf(
                    "institutionId" to instId,
                    "actorUid" to uid,
                    "actorName" to actor.displayName,
                    "actorRole" to actor.role,
                    "actionType" to "MEMBER_INVITED",
                    "targetSummary" to cleanEmail,
                    "details" to "Invited as ${role.label} (${department.ifBlank { "All" }})",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateMemberAccess(
        instId: String,
        target: InstitutionMembership,
        newRole: MemberRole,
        newPermissions: List<String>,
        newStatus: MemberStatus,
        newDepartment: String,
        actor: InstitutionMembership
    ): Result<Unit> {
        val path = "institutions/$instId/memberships/${target.uid}"
        return try {
            val uid = requireUid()
            require(target.roleEnum() != MemberRole.DEAN) { "Dean account cannot be suspended or demoted." }
            val safePerms = if (newRole == MemberRole.STUDENT_LEADER || newRole == MemberRole.STUDENT) {
                newPermissions.filter { key ->
                    AppPermission.entries.firstOrNull { it.key == key }?.studentSideAllowed == true
                }
            } else newPermissions
            val batch = db.batch()
            batch.update(
                db.document(path),
                mapOf(
                    "role" to newRole.name,
                    "permissions" to safePerms,
                    "status" to newStatus.name,
                    "department" to newDepartment.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("institutions").document(instId)
                    .collection("audit_logs").document(UUID.randomUUID().toString()),
                mapOf(
                    "institutionId" to instId,
                    "actorUid" to uid,
                    "actorName" to actor.displayName,
                    "actorRole" to actor.role,
                    "actionType" to "MEMBER_ACCESS_CHANGED",
                    "targetSummary" to target.displayName,
                    "details" to "Role=${newRole.name}, Status=${newStatus.name}, Dept=$newDepartment",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateDriveConnection(
        instId: String,
        connected: Boolean,
        driveAccountEmail: String,
        driveFolderName: String,
        driveFolderId: String,
        actor: InstitutionMembership
    ): Result<Unit> {
        val path = "institutions/$instId"
        return try {
            val uid = requireUid()
            val batch = db.batch()
            batch.update(
                db.document(path),
                mapOf(
                    "driveConnected" to connected,
                    "driveAccountEmail" to driveAccountEmail.trim(),
                    "driveFolderName" to driveFolderName.trim(),
                    "driveFolderId" to driveFolderId.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("institutions").document(instId)
                    .collection("audit_logs").document(UUID.randomUUID().toString()),
                mapOf(
                    "institutionId" to instId,
                    "actorUid" to uid,
                    "actorName" to actor.displayName,
                    "actorRole" to actor.role,
                    "actionType" to if (connected) "DRIVE_CONNECTED" else "DRIVE_DISCONNECTED",
                    "targetSummary" to driveAccountEmail.ifBlank { "Shared Drive" }.take(200),
                    "details" to "Folder: $driveFolderName ($driveFolderId). Existing notice references preserved.".take(1000),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
