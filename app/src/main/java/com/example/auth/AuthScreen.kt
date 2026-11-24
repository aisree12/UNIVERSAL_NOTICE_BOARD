package com.example.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.ui.components.AnimatedColorfulTitle
import com.example.ui.components.AnimatedSectionReveal
import com.example.ui.components.ColorfulIconBadge
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseCrimson
import com.example.ui.theme.TealAccent

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f)
                    )
                )
            )
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                AnimatedSectionReveal(delayIndex = 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ColorfulIconBadge(
                            icon = Icons.Rounded.Campaign,
                            tint = IndigoPrimary,
                            contentDescription = "Notice Board emblem",
                            size = 54.dp,
                            iconSize = 30.dp
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            AnimatedColorfulTitle(
                                text = "NOTICE BOARD",
                                style = MaterialTheme.typography.displayMedium
                            )
                            Text(
                                text = "Multi-Institution Campus Platform",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                AnimatedSectionReveal(delayIndex = 1) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "One app. Every campus, strictly isolated.",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Establish your own institution space with a permanent ID, or join an existing campus with your verified role or student pass.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(18.dp))
                            FeatureHighlightRow(
                                icon = Icons.Rounded.AdminPanelSettings,
                                tint = IndigoPrimary,
                                title = "Dean-First Self-Registration",
                                subtitle = "No preloaded college data or external approval wait."
                            )
                            Spacer(Modifier.height(12.dp))
                            FeatureHighlightRow(
                                icon = Icons.Rounded.AccountBalance,
                                tint = TealAccent,
                                title = "Major Board & Department Mirroring",
                                subtitle = "One canonical notice record synced across boards."
                            )
                            Spacer(Modifier.height(12.dp))
                            FeatureHighlightRow(
                                icon = Icons.Rounded.DocumentScanner,
                                tint = AmberWarm,
                                title = "CameraX Multi-Page PDF Scanning",
                                subtitle = "Scan notices directly into PDFs with preview & retry."
                            )
                            Spacer(Modifier.height(12.dp))
                            FeatureHighlightRow(
                                icon = Icons.Rounded.Lock,
                                tint = EmeraldSuccess,
                                title = "Zero-Trust Tenant Isolation",
                                subtitle = "Private campus notices stay protected until verified."
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            AnimatedSectionReveal(delayIndex = 2) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = RoseCrimson.copy(alpha = 0.12f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = RoseCrimson,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            errorMessage = null
                            isLoading = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                }
                            )
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("google_sign_in_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Signing in...", fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.VerifiedUser,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Sign in with Google",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Authentication secures your campus session. Google Drive authorization remains separate and optional for Deans.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureHighlightRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
