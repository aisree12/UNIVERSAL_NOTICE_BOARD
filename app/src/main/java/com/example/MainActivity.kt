package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.AuthScreen
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.CampusRepository
import com.example.data.OnboardingRepository
import com.example.data.UserPreferencesDataStore
import com.example.ui.screens.DashboardScaffold
import com.example.ui.screens.InstitutionOnboardingScreen
import com.example.ui.theme.AccentPreset
import com.example.ui.theme.NoticeBoardTheme
import com.example.ui.theme.ThemeMode
import com.example.viewmodel.CampusViewModel
import com.example.viewmodel.OnboardingViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NoticeBoardApp()
        }
    }
}

@Composable
fun NoticeBoardApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { UserPreferencesDataStore(context) }
    val credentialManager = remember { CredentialManager.create(context) }

    val themeMode by prefs.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val savedInstitutionId by prefs.activeInstitutionId.collectAsStateWithLifecycle(initialValue = null)
    var activeAccentKey by remember { mutableStateOf("INDIGO") }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    NoticeBoardTheme(
        themeMode = themeMode,
        accentPreset = AccentPreset.fromKey(activeAccentKey)
    ) {
        val user = currentUser
        if (user == null) {
            AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
        } else {
            val uid = user.uid
            key(uid) {
                var activeInstitutionId by remember(savedInstitutionId) {
                    mutableStateOf(savedInstitutionId)
                }

                val handleSignOut: () -> Unit = {
                    scope.launch {
                        prefs.setActiveInstitutionId(null)
                    }
                    activeInstitutionId = null
                    signOutUser(credentialManager, scope)
                }

                if (activeInstitutionId.isNullOrBlank()) {
                    val onboardingVm: OnboardingViewModel = viewModel(
                        key = "onboarding_$uid",
                        factory = viewModelFactory {
                            initializer {
                                val app = checkNotNull(this[APPLICATION_KEY])
                                val dbId = app.getString(R.string.firestore_database_id)
                                val db = FirebaseFirestore.getInstance(dbId)
                                OnboardingViewModel(
                                    repository = OnboardingRepository(db),
                                    prefs = prefs,
                                    currentUserId = uid
                                )
                            }
                        }
                    )
                    InstitutionOnboardingScreen(
                        viewModel = onboardingVm,
                        userEmail = user.email.orEmpty(),
                        userDisplayName = user.displayName ?: "Campus Member",
                        onInstitutionReady = { instId ->
                            activeInstitutionId = instId
                        },
                        onSignOut = handleSignOut
                    )
                } else {
                    val instId = activeInstitutionId!!
                    key(instId) {
                        val campusVm: CampusViewModel = viewModel(
                            key = "campus_${uid}_$instId",
                            factory = viewModelFactory {
                                initializer {
                                    val app = checkNotNull(this[APPLICATION_KEY])
                                    val dbId = app.getString(R.string.firestore_database_id)
                                    val db = FirebaseFirestore.getInstance(dbId)
                                    CampusViewModel(
                                        repository = CampusRepository(db),
                                        prefs = prefs,
                                        currentUserId = uid,
                                        institutionId = instId
                                    )
                                }
                            }
                        )
                        DashboardScaffold(
                            viewModel = campusVm,
                            themeMode = themeMode,
                            onChangeTheme = { mode ->
                                scope.launch { prefs.setThemeMode(mode) }
                            },
                            onAccentResolved = { key -> activeAccentKey = key },
                            onSwitchInstitution = {
                                scope.launch { prefs.setActiveInstitutionId(null) }
                                activeInstitutionId = null
                            },
                            onSignOut = handleSignOut
                        )
                    }
                }
            }
        }
    }
}
