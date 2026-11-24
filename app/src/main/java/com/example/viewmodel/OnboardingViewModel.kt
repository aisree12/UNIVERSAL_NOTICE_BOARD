package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.OnboardingRepository
import com.example.data.UserPreferencesDataStore
import com.example.data.toFriendlyFirestoreMessage
import com.example.model.InstitutionCode
import com.example.model.MemberRole
import com.example.model.UiState
import com.example.model.UserInstitutionLink
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val repository: OnboardingRepository,
    private val prefs: UserPreferencesDataStore,
    private val currentUserId: String
) : ViewModel() {

    val joinedInstitutions: StateFlow<UiState<List<UserInstitutionLink>>> =
        repository.observeJoinedInstitutions(currentUserId)
            .map<List<UserInstitutionLink>, UiState<List<UserInstitutionLink>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w("OnboardingVM", "Error observing joined institutions", root)
                emit(UiState.Error(root.toFriendlyFirestoreMessage("Failed to load your institutions")))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _createdInstitution = MutableStateFlow<InstitutionCode?>(null)
    val createdInstitution: StateFlow<InstitutionCode?> = _createdInstitution.asStateFlow()

    private val _resolvedInstitution = MutableStateFlow<InstitutionCode?>(null)
    val resolvedInstitution: StateFlow<InstitutionCode?> = _resolvedInstitution.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearResolvedInstitution() {
        _resolvedInstitution.value = null
        _errorMessage.value = null
    }

    fun dismissCreatedCelebration(onContinue: (String) -> Unit) {
        val created = _createdInstitution.value
        _createdInstitution.value = null
        if (created != null) {
            viewModelScope.launch {
                prefs.setActiveInstitutionId(created.institutionId)
                onContinue(created.institutionId)
            }
        }
    }

    fun createInstitution(
        name: String,
        tagline: String,
        contactEmail: String,
        contactPhone: String,
        deanName: String,
        studentPass: String,
        departments: List<String>,
        accentColorKey: String
    ) {
        if (name.trim().length < 2) {
            _errorMessage.value = "Please enter your official institution name."
            return
        }
        viewModelScope.launch {
            _busy.value = true
            _errorMessage.value = null
            val result = repository.createInstitution(
                institutionName = name,
                tagline = tagline,
                contactEmail = contactEmail,
                contactPhone = contactPhone,
                deanName = deanName,
                initialStudentPass = studentPass,
                departments = departments,
                accentColorKey = accentColorKey
            )
            _busy.value = false
            result.onSuccess { code ->
                _createdInstitution.value = code
            }.onFailure { err ->
                _errorMessage.value = err.toFriendlyFirestoreMessage("Could not create institution.")
            }
        }
    }

    fun lookupInstitution(institutionId: String) {
        viewModelScope.launch {
            _busy.value = true
            _errorMessage.value = null
            val result = repository.lookupInstitutionCode(institutionId)
            _busy.value = false
            result.onSuccess { code ->
                _resolvedInstitution.value = code
            }.onFailure { err ->
                _resolvedInstitution.value = null
                _errorMessage.value = err.toFriendlyFirestoreMessage("Institution ID lookup failed.")
            }
        }
    }

    fun joinAsStudent(
        code: InstitutionCode,
        passcode: String,
        displayName: String,
        department: String,
        onJoined: (String) -> Unit
    ) {
        viewModelScope.launch {
            _busy.value = true
            _errorMessage.value = null
            val result = repository.joinWithStudentPass(code, passcode, displayName, department)
            _busy.value = false
            result.onSuccess {
                prefs.setActiveInstitutionId(code.institutionId)
                _resolvedInstitution.value = null
                onJoined(code.institutionId)
            }.onFailure { err ->
                _errorMessage.value = err.toFriendlyFirestoreMessage("Could not verify Student Entry Pass.")
            }
        }
    }

    fun joinWithEntrance(
        code: InstitutionCode,
        entranceRole: MemberRole,
        onJoined: (String) -> Unit
    ) {
        viewModelScope.launch {
            _busy.value = true
            _errorMessage.value = null
            val result = repository.joinWithInviteOrExisting(code, entranceRole)
            _busy.value = false
            result.onSuccess {
                prefs.setActiveInstitutionId(code.institutionId)
                _resolvedInstitution.value = null
                onJoined(code.institutionId)
            }.onFailure { err ->
                _errorMessage.value = err.toFriendlyFirestoreMessage("Could not verify your ${entranceRole.label} access.")
            }
        }
    }

    fun selectInstitution(institutionId: String, onSelected: (String) -> Unit) {
        viewModelScope.launch {
            prefs.setActiveInstitutionId(institutionId)
            onSelected(institutionId)
        }
    }
}
