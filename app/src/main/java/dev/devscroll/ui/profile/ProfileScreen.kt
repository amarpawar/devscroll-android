package dev.devscroll.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import dev.devscroll.R
import coil.compose.SubcomposeAsyncImage
import dev.devscroll.data.remote.dto.DeleteAccountRequest
import dev.devscroll.navigation.ExternalLinks
import dev.devscroll.navigation.openUrl
import dev.devscroll.ui.components.TerminalConfirmDialog
import dev.devscroll.ui.components.TopBar
import dev.devscroll.ui.components.TerminalLoader
import dev.devscroll.ui.theme.ThemeViewModel
import dev.devscroll.ui.theme.AccentColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.credentials.exceptions.GetCredentialCancellationException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    themeViewModel: ThemeViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
) {
    val themeState by themeViewModel.themeState.collectAsState()
    val authState by profileViewModel.uiState.collectAsState()

    when (val state = authState) {
        is AuthUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                TerminalLoader(
                    labels = listOf("CONNECT /auth", "HANDSHAKE", "GET /profile"),
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }
        }
        is AuthUiState.LoggedIn -> {
            LaunchedEffect(Unit) {
                profileViewModel.refreshProfile()
            }
            LoggedInDashboard(
                user = state.user,
                isRefreshing = state.isRefreshing,
                onRefresh = { profileViewModel.refreshProfile() },
                onLogout = { profileViewModel.logout() },
                themeViewModel = themeViewModel,
                profileViewModel = profileViewModel,
            )
        }
        is AuthUiState.LoggedOut -> {
            LoggedOutView(
                themeState = themeState,
                profileViewModel = profileViewModel,
                onThemeToggle = { themeViewModel.toggleDark() }
            )
        }
        is AuthUiState.Authenticating -> {
            LoggedOutView(
                themeState = themeState,
                profileViewModel = profileViewModel,
                onThemeToggle = { themeViewModel.toggleDark() },
                isLoading = true
            )
        }
        is AuthUiState.Error -> {
            LoggedOutView(
                themeState = themeState,
                profileViewModel = profileViewModel,
                onThemeToggle = { themeViewModel.toggleDark() },
                errorMessage = state.message,
                onClearError = { profileViewModel.clearError() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoggedInDashboard(
    user: dev.devscroll.data.model.User,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    themeViewModel: ThemeViewModel,
    profileViewModel: ProfileViewModel,
) {
    val themeState by themeViewModel.themeState.collectAsState()
    val isDark = themeState.isDark
    val currentAccent = themeState.accentColor

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }
    var isDeleting by remember { mutableStateOf(false) }

    val isExporting by profileViewModel.isExporting.collectAsState()
    val exportMessage by profileViewModel.exportMessage.collectAsState()

    // The system picker owns the destination, so no storage permission is needed.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) profileViewModel.exportUserData(uri) }

    LaunchedEffect(exportMessage) {
        if (exportMessage != null) {
            kotlinx.coroutines.delay(2500)
            profileViewModel.clearExportMessage()
        }
    }

    if (showDeleteDialog) {
        val isPasswordAccount = user.provider == "username/password"
        TerminalConfirmDialog(
            title = "delete account — irreversible",
            message = "this removes your saves, subscriptions and streak. it cannot be undone.",
            reauthHint = when (user.provider) {
                "google" -> "confirm with google to continue"
                else -> "re-enter your password to continue"
            },
            confirmLabel = "delete",
            isBusy = isDeleting,
            passwordValue = if (isPasswordAccount) deletePassword else null,
            onPasswordChange = { deletePassword = it },
            onDismiss = {
                showDeleteDialog = false
                deletePassword = ""
            },
            onConfirm = {
                coroutineScope.launch {
                    isDeleting = true
                    try {
                        // A freshly minted credential, not the stored session — the
                        // server verifies it before deleting anything.
                        val request = when (user.provider) {
                            "google" -> DeleteAccountRequest(idToken = requestGoogleIdToken(context))
                            else -> DeleteAccountRequest(password = deletePassword)
                        }
                        showDeleteDialog = false
                        deletePassword = ""
                        profileViewModel.deleteAccount(request)
                    } catch (e: GetCredentialCancellationException) {
                        // Backed out of the sheet; leave the dialog open.
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        showDeleteDialog = false
                        deletePassword = ""
                        profileViewModel.onAuthError(e.message ?: "Re-authentication failed")
                    } finally {
                        isDeleting = false
                    }
                }
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            isDark = isDark,
            onThemeToggle = { themeViewModel.toggleDark() },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f),
            indicator = {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (isRefreshing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TerminalLoader(
                            labels = listOf("CONNECT /auth", "HANDSHAKE", "GET /profile"),
                            modifier = Modifier.fillMaxWidth(0.85f)
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                }
            // Header Row: < $ whoami
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$ whoami",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }

            // User Info Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large styled profile pic or fallback first-letter box
                val firstChar = user.name.firstOrNull()?.toString()?.uppercase() ?: "D"
                if (!user.pictureUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = user.pictureUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(64.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = firstChar,
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = firstChar,
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = firstChar,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "@${user.name.lowercase().replace(" ", "")}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "// signed in via ${user.provider}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Statistics Row: saved | subs | streak
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .height(72.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = user.savedArticlesCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "// saved",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                VerticalDivider(color = MaterialTheme.colorScheme.outline)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = user.subscriptionsCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "// subs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                VerticalDivider(color = MaterialTheme.colorScheme.outline)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${user.streakCount}d",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "// streak",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Appearance Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "// appearance",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Theme selection row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "theme", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                    
                    Row(
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                            .height(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .fillMaxHeight()
                                .background(if (isDark) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { if (!isDark) themeViewModel.toggleDark() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "dark",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        VerticalDivider(color = MaterialTheme.colorScheme.outline)
                        
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .fillMaxHeight()
                                .background(if (!isDark) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { if (isDark) themeViewModel.toggleDark() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "light",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (!isDark) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Accent selection row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "accent", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                        Text(
                            text = "// current: ${currentAccent.name.lowercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccentColor.entries.forEach { accent ->
                            val isActive = currentAccent == accent
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(if (isDark) accent.darkColor else accent.lightColor)
                                    .border(
                                        width = if (isActive) 2.dp else 1.dp,
                                        color = if (isActive) MaterialTheme.colorScheme.onBackground else Color.Transparent
                                    )
                                    .clickable { themeViewModel.setAccent(accent) }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // Account Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "// account",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ProfileActionRow(
                    title = "connected accounts",
                    subtitle = "${user.provider.providerLabel()} · linked",
                    onClick = null,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                ProfileActionRow(
                    title = "export data",
                    subtitle = if (isExporting) {
                        "exporting..."
                    } else {
                        exportMessage ?: "download your saves + subs as .json"
                    },
                    onClick = {
                        if (!isExporting) exportLauncher.launch("devscroll-export.json")
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                ProfileActionRow(
                    title = "logout",
                    subtitle = "disconnect active session",
                    onClick = onLogout
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                ProfileActionRow(
                    title = "delete account",
                    subtitle = "permanent — cannot be undone",
                    onClick = { showDeleteDialog = true },
                    titleColor = MaterialTheme.colorScheme.error
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // About Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "// about",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ProfileActionRow(
                    title = "terms of service",
                    subtitle = "read our privacy policies and terms of usage",
                    onClick = { context.openUrl(ExternalLinks.TERMS) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                ProfileActionRow(
                    title = "privacy policy",
                    subtitle = "how your data is handled",
                    onClick = { context.openUrl(ExternalLinks.PRIVACY) }
                )
            }
        }
        }
    }
}

@Composable
private fun ProfileActionRow(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    titleColor: Color = MaterialTheme.colorScheme.onBackground
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = titleColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // No chevron on informational rows — it would imply a tap target that isn't there.
        if (onClick != null) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Maps the stored provider id onto what the user actually sees. */
private fun String.providerLabel(): String = when (this) {
    "google" -> "google"
    else -> "email"
}

@Composable
private fun LoggedOutView(
    themeState: dev.devscroll.ui.theme.ThemeState,
    profileViewModel: ProfileViewModel,
    onThemeToggle: () -> Unit,
    errorMessage: String? = null,
    onClearError: (() -> Unit)? = null,
    isLoading: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var confirmPassword by remember { mutableStateOf("") }
    var showConfirmPassword by remember { mutableStateOf(false) }
    val isSignUp = selectedTab == 1

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            isDark = themeState.isDark,
            onThemeToggle = onThemeToggle,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "// auth required to access your account",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.error)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .clickable { if (!isLoading) onClearError?.invoke() }
                        .padding(12.dp)
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)) {
                                append("[!] error: ")
                            }
                            append(errorMessage)
                            append("\n// click to dismiss")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Info box with left accent border
            val borderColor = MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = borderColor,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                    .padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                                append("[!] ")
                            }
                            append("you need an account to ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("access your account.")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "// reading the feed stays free + anonymous.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Social buttons
            SocialButton(
                label = "continue with google",
                icon = {
                    androidx.compose.material3.Icon(
                        painterResource(R.drawable.ic_google),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.Unspecified
                    )
                },
                onClick = {
                    if (!isLoading) {
                        coroutineScope.launch {
                            try {
                                profileViewModel.loginWithGoogle(requestGoogleIdToken(context))
                            } catch (e: GetCredentialCancellationException) {
                                // User dismissed the sheet; not an error worth showing.
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                profileViewModel.onAuthError(e.message ?: "Google Sign-In failed")
                            }
                        }
                    }
                }
            )

            // Or divider
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
                Text(
                    text = "  // or  ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
            }

            // Sign in / Sign up tab row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                listOf("> sign in", "+ sign up").forEachIndexed { index, label ->
                    if (index > 0) VerticalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.height(40.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .background(
                                if (selectedTab == index) MaterialTheme.colorScheme.surfaceVariant
                                else Color.Transparent
                            )
                            .clickable { if (!isLoading) selectedTab = index },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selectedTab == index) MaterialTheme.colorScheme.onBackground
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Email field
            TerminalInputField(
                label = "// email",
                value = email,
                onValueChange = { email = it },
                placeholder = "you@dev.local",
                keyboardType = KeyboardType.Email,
                enabled = !isLoading
            )

            // Password field
            TerminalInputField(
                label = "// password",
                value = password,
                onValueChange = { password = it },
                placeholder = "••••••••",
                keyboardType = KeyboardType.Password,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                enabled = !isLoading,
                trailing = {
                    Text(
                        text = if (showPassword) "hide" else "show",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { if (!isLoading) showPassword = !showPassword },
                    )
                },
            )

            if (isSignUp) {
                TerminalInputField(
                    label = "// confirm password",
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = "••••••••",
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    enabled = !isLoading,
                    trailing = {
                        Text(
                            text = if (showConfirmPassword) "hide" else "show",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { if (!isLoading) showConfirmPassword = !showConfirmPassword },
                        )
                    },
                )
            } else {
                Text(
                    text = "forgot password?",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { if (!isLoading) { } },
                )
            }

            // Submit button / Loader
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TerminalLoader(
                        labels = listOf("CONNECT /auth", "HANDSHAKE", "GET /profile"),
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            if (isSignUp) {
                                profileViewModel.signUpWithEmail(email, password)
                            } else {
                                profileViewModel.loginWithEmail(email, password)
                            }
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isSignUp) "$ create account" else "$ login",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Footer
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    // These were styled as links but weren't clickable — the user is being
                    // asked to agree to documents they had no way to open.
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append("// by continuing you agree to ")
                        }
                        withLink(LinkAnnotation.Url(ExternalLinks.TERMS)) {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                append("terms")
                            }
                        }
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append(" and ")
                        }
                        withLink(LinkAnnotation.Url(ExternalLinks.PRIVACY)) {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                append("privacy")
                            }
                        }
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append(".")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = "// no email marketing. ever.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SocialButton(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}


@Composable
private fun TerminalInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "> ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = if (enabled) MaterialTheme.colorScheme.onBackground
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation = visualTransformation,
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                },
            )
            if (trailing != null) trailing()
        }
    }
}
