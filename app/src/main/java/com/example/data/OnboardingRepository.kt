package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AppPermission
import com.example.model.Institution
import com.example.model.InstitutionCode
import com.example.model.InstitutionMembership
import com.example.model.MemberInvite
import com.example.model.MemberRole
import com.example.model.MemberStatus
import com.example.model.UserInstitutionLink
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class OnboardingRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUser() = auth.currentUser
        ?: throw IllegalStateException("Please sign in with Google to continue.")

    fun observeJoinedInstitutions(userId: String): Flow<List<UserInstitutionLink>> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank() || uid != userId) {
            emit(emptyList())
            return@flow
        }
        val path = "user_institutions/$userId/joined"
        emitAll(
            db.collection("user_institutions").document(userId).collection("joined")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        UserInstitutionLink::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createInstitution(
        institutionName: String,
        tagline: String,
        contactEmail: String,
        contactPhone: String,
        deanName: String,
        initialStudentPass: String,
        departments: List<String>,
        accentColorKey: String
    ): Result<InstitutionCode> {
        val user = try {
            requireUser()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanName = institutionName.trim().take(120)
        val cleanTagline = tagline.trim().ifBlank { "Official Campus Notice Board" }.take(160)
        val cleanEmail = contactEmail.trim().ifBlank { user.email.orEmpty() }.take(160)
        val cleanDeanName = deanName.trim().ifBlank { user.displayName ?: "Institution Dean" }.take(100)
        val cleanDepts = departments.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(30)
            .ifEmpty { Institution.DEFAULT_DEPARTMENTS }

        repeat(5) { attempt ->
            val candidateId = InstitutionIdGenerator.generateCandidateId(cleanName)
            val codePath = "institution_codes/$candidateId"
            try {
                val existingCode = db.collection("institution_codes").document(candidateId).get().await()
                if (existingCode.exists()) {
                    return@repeat
                }

                val passEnabled = initialStudentPass.trim().isNotBlank()
                val passHash = if (passEnabled) {
                    InstitutionIdGenerator.hashPasscode(candidateId, initialStudentPass.trim())
                } else ""

                val batch = db.batch()
                val codeRef = db.collection("institution_codes").document(candidateId)
                val instRef = db.collection("institutions").document(candidateId)
                val memberRef = instRef.collection("memberships").document(user.uid)
                val linkRef = db.collection("user_institutions").document(user.uid)
                    .collection("joined").document(candidateId)
                val auditRef = instRef.collection("audit_logs").document(UUID.randomUUID().toString())

                val codePayload = mapOf(
                    "institutionId" to candidateId,
                    "name" to cleanName,
                    "tagline" to cleanTagline,
                    "accentColorKey" to accentColorKey,
                    "deanUid" to user.uid,
                    "studentPassEnabled" to passEnabled,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                val instPayload = mapOf(
                    "institutionId" to candidateId,
                    "name" to cleanName,
                    "tagline" to cleanTagline,
                    "contactEmail" to cleanEmail,
                    "contactPhone" to contactPhone.trim(),
                    "accentColorKey" to accentColorKey,
                    "deanUid" to user.uid,
                    "deanName" to cleanDeanName,
                    "studentPassEnabled" to passEnabled,
                    "studentPassHash" to passHash,
                    "confidentialFeedbackEnabled" to true,
                    "driveConnected" to false,
                    "driveAccountEmail" to "",
                    "driveFolderName" to "",
                    "driveFolderId" to "",
                    "departments" to cleanDepts,
                    "categories" to Institution.DEFAULT_CATEGORIES,
                    "priorities" to Institution.DEFAULT_PRIORITIES,
                    "activityTypes" to Institution.DEFAULT_ACTIVITY_TYPES,
                    "sportTypes" to Institution.DEFAULT_SPORT_TYPES,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                val memberPayload = mapOf(
                    "uid" to user.uid,
                    "institutionId" to candidateId,
                    "displayName" to cleanDeanName,
                    "email" to (user.email ?: cleanEmail),
                    "designation" to "Dean & Principal Authority",
                    "department" to "Administration",
                    "role" to MemberRole.DEAN.name,
                    "permissions" to AppPermission.defaultDeanPermissions,
                    "scopedDepartments" to emptyList<String>(),
                    "status" to MemberStatus.ACTIVE.name,
                    "joinMethod" to "DEAN_FOUNDER",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                val linkPayload = mapOf(
                    "uid" to user.uid,
                    "institutionId" to candidateId,
                    "institutionName" to cleanName,
                    "role" to MemberRole.DEAN.name,
                    "accentColorKey" to accentColorKey,
                    "joinedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                val auditPayload = mapOf(
                    "institutionId" to candidateId,
                    "actorUid" to user.uid,
                    "actorName" to cleanDeanName,
                    "actorRole" to MemberRole.DEAN.name,
                    "actionType" to "INSTITUTION_CREATED",
                    "targetSummary" to candidateId,
                    "details" to "Dean-first institution space registered for $cleanName.",
                    "createdAt" to FieldValue.serverTimestamp()
                )

                batch.set(codeRef, codePayload)
                batch.set(instRef, instPayload)
                batch.set(memberRef, memberPayload)
                batch.set(linkRef, linkPayload)
                batch.set(auditRef, auditPayload)
                batch.commit().await()

                return Result.success(
                    InstitutionCode(
                        id = candidateId,
                        institutionId = candidateId,
                        name = cleanName,
                        tagline = cleanTagline,
                        accentColorKey = accentColorKey,
                        deanUid = user.uid,
                        studentPassEnabled = passEnabled
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, codePath)
                if (attempt == 4) return Result.failure(e)
            }
        }
        return Result.failure(IllegalStateException("Could not allocate a unique Institution ID. Please retry."))
    }

    suspend fun lookupInstitutionCode(rawInstitutionId: String): Result<InstitutionCode> {
        val normalizedId = rawInstitutionId.trim().uppercase()
        val path = "institution_codes/$normalizedId"
        return try {
            requireUser()
            require(normalizedId.matches(Regex("^NB-[A-Z0-9]+-[A-Z0-9]{4,8}$"))) {
                "Enter a valid Institution ID (e.g. NB-BLDE-7K4P2)."
            }
            val snapshot = db.collection("institution_codes").document(normalizedId).get().await()
            if (!snapshot.exists()) {
                return Result.failure(IllegalStateException("No institution found for ID $normalizedId."))
            }
            val code = snapshot.toObject(
                InstitutionCode::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return Result.failure(IllegalStateException("Invalid institution profile."))
            Result.success(code)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun joinWithStudentPass(
        institutionCode: InstitutionCode,
        passcode: String,
        displayName: String,
        department: String
    ): Result<Unit> {
        val instId = institutionCode.institutionId
        var path = "institutions/$instId/memberships"
        return try {
            val user = requireUser()
            val cleanPass = passcode.trim()
            require(cleanPass.isNotBlank()) { "Please enter the Student Entry Pass provided by your Dean." }
            require(institutionCode.studentPassEnabled) {
                "Student Entry Pass is currently disabled by the Dean of this institution."
            }

            val cleanName = displayName.trim().ifBlank { user.displayName ?: "Student" }.take(100)
            val passHashProof = InstitutionIdGenerator.hashPasscode(instId, cleanPass)
            val memberRef = db.collection("institutions").document(instId)
                .collection("memberships").document(user.uid)
            path = memberRef.path

            val memberPayload = mapOf(
                "uid" to user.uid,
                "institutionId" to instId,
                "displayName" to cleanName,
                "email" to (user.email ?: ""),
                "designation" to "Student",
                "department" to department.trim().ifBlank { "General" },
                "role" to MemberRole.STUDENT.name,
                "permissions" to AppPermission.defaultStudentPermissions,
                "scopedDepartments" to emptyList<String>(),
                "status" to MemberStatus.ACTIVE.name,
                "joinMethod" to "STUDENT_PASS",
                "passHashProof" to passHashProof,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            memberRef.set(memberPayload).await()

            val linkPayload = mapOf(
                "uid" to user.uid,
                "institutionId" to instId,
                "institutionName" to institutionCode.name,
                "role" to MemberRole.STUDENT.name,
                "accentColorKey" to institutionCode.accentColorKey,
                "joinedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("user_institutions").document(user.uid)
                .collection("joined").document(instId).set(linkPayload).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinWithInviteOrExisting(
        institutionCode: InstitutionCode,
        expectedEntranceRole: MemberRole? = null
    ): Result<InstitutionMembership> {
        val instId = institutionCode.institutionId
        var path = "institutions/$instId/memberships"
        return try {
            val user = requireUser()
            val memberRef = db.collection("institutions").document(instId)
                .collection("memberships").document(user.uid)
            path = memberRef.path

            // 1. Check if already a member
            val existingSnap = try {
                memberRef.get().await()
            } catch (_: Exception) {
                null
            }
            if (existingSnap != null && existingSnap.exists()) {
                val existing = existingSnap.toObject(
                    InstitutionMembership::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )!!
                if (existing.statusEnum() != MemberStatus.ACTIVE) {
                    return Result.failure(
                        IllegalStateException("Your account status is ${existing.status}. Contact your institution Dean.")
                    )
                }
                if (expectedEntranceRole != null && existing.roleEnum() != expectedEntranceRole) {
                    return Result.failure(
                        IllegalStateException("Your account is registered as ${existing.roleEnum().label}, not ${expectedEntranceRole.label}.")
                    )
                }
                val linkPayload = mapOf(
                    "uid" to user.uid,
                    "institutionId" to instId,
                    "institutionName" to institutionCode.name,
                    "role" to existing.role,
                    "accentColorKey" to institutionCode.accentColorKey,
                    "joinedAt" to (existing.createdAt ?: FieldValue.serverTimestamp()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("user_institutions").document(user.uid)
                    .collection("joined").document(instId).set(linkPayload).await()
                return Result.success(existing)
            }

            // 2. Otherwise check pending Dean invite matching verified Google email
            val userEmail = user.email?.trim()?.lowercase()
                ?: return Result.failure(IllegalStateException("Your Google account does not expose an email for invite matching."))

            val inviteRef = db.collection("institutions").document(instId)
                .collection("invites").document(userEmail)
            path = inviteRef.path
            val inviteSnap = inviteRef.get().await()
            if (!inviteSnap.exists()) {
                return Result.failure(
                    IllegalStateException("No pending Dean invitation found for $userEmail in ${institutionCode.name}.")
                )
            }
            val invite = inviteSnap.toObject(
                MemberInvite::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return Result.failure(IllegalStateException("Invalid invite record."))

            if (invite.status != "PENDING") {
                return Result.failure(IllegalStateException("This invitation is ${invite.status.lowercase()}."))
            }

            if (expectedEntranceRole != null && MemberRole.fromRaw(invite.role) != expectedEntranceRole) {
                return Result.failure(
                    IllegalStateException("Your invitation is for ${MemberRole.fromRaw(invite.role).label}, not ${expectedEntranceRole.label}.")
                )
            }

            val displayName = invite.displayName.ifBlank { user.displayName ?: userEmail }.take(100)
            val memberPayload = mapOf(
                "uid" to user.uid,
                "institutionId" to instId,
                "displayName" to displayName,
                "email" to userEmail,
                "designation" to invite.designation,
                "department" to invite.department,
                "role" to invite.role,
                "permissions" to invite.permissions,
                "scopedDepartments" to invite.scopedDepartments,
                "status" to MemberStatus.ACTIVE.name,
                "joinMethod" to "INVITE",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            memberRef.set(memberPayload).await()

            inviteRef.update(
                mapOf(
                    "status" to "CLAIMED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val linkPayload = mapOf(
                "uid" to user.uid,
                "institutionId" to instId,
                "institutionName" to institutionCode.name,
                "role" to invite.role,
                "accentColorKey" to institutionCode.accentColorKey,
                "joinedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("user_institutions").document(user.uid)
                .collection("joined").document(instId).set(linkPayload).await()

            val claimedMember = InstitutionMembership(
                id = user.uid,
                uid = user.uid,
                institutionId = instId,
                displayName = displayName,
                email = userEmail,
                designation = invite.designation,
                department = invite.department,
                role = invite.role,
                permissions = invite.permissions,
                scopedDepartments = invite.scopedDepartments,
                status = MemberStatus.ACTIVE.name,
                joinMethod = "INVITE"
            )
            Result.success(claimedMember)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
