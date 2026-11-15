package dev.devscroll.ui.reader

import android.content.Intent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import dev.devscroll.navigation.reportArticle
import dev.devscroll.navigation.shareArticle
import dev.devscroll.ui.components.TerminalDivider
import dev.devscroll.ui.components.TerminalIconButton
import dev.devscroll.ui.components.TerminalLoader
import dev.devscroll.ui.components.TerminalToast
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ReaderScreen(
    articleId: Long,
    onBack: () -> Unit,
    onSignInClick: () -> Unit = {},
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val article by viewModel.article.collectAsState()
    val user by viewModel.userState.collectAsState()
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isWebViewLoading by remember(article?.url) { mutableStateOf(true) }

    var showAuthToast by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    val saveError by viewModel.error.collectAsState()

    LaunchedEffect(saveError) {
        if (saveError != null) {
            kotlinx.coroutines.delay(2500.milliseconds)
            viewModel.clearError()
        }
    }

    LaunchedEffect(showAuthToast) {
        if (showAuthToast) {
            kotlinx.coroutines.delay(2500.milliseconds)
            showAuthToast = false
        }
    }

    LaunchedEffect(articleId) { viewModel.loadArticle(articleId) }

    BackHandler {
        if (webView?.canGoBack() == true) webView?.goBack() else onBack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TerminalIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onClick = onBack)
                Text(
                    text = article?.title ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                )
                // All actions live behind one overflow so the title keeps its width.
                Box {
                    TerminalIconButton(
                        icon = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        enabled = article != null,
                    ) { menuExpanded = true }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (article?.isSaved == true) "unsave" else "save",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (article?.isSaved == true) Icons.Filled.Bookmark
                                    else Icons.Outlined.BookmarkBorder,
                                    contentDescription = null,
                                    tint = if (article?.isSaved == true)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                if (user == null) {
                                    showAuthToast = true
                                } else {
                                    article?.let { viewModel.toggleSave(it.id) }
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("share", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                article?.let { context.shareArticle(it.title, it.url) }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("open in browser", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                article?.url?.let { url ->
                                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                    context.startActivity(intent)
                                }
                            },
                        )
                        TerminalDivider()
                        // Separated: destructive-ish and rarely wanted by accident.
                        DropdownMenuItem(
                            text = { Text("report", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Flag,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                article?.let {
                                    context.reportArticle(
                                        articleId = it.id,
                                        title = it.title,
                                        url = it.url,
                                        source = it.blogName,
                                    )
                                }
                            },
                        )
                    }
                }
        }
        TerminalDivider()

        if (saveError != null) {
            Text(
                text = "error: $saveError",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }

        if (article == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TerminalLoader(
                    labels = listOf("FETCHING", "GET /article"),
                    modifier = Modifier.fillMaxWidth(0.6f),
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            // Opaque white, like a browser's default canvas. Pages that
                            // don't set their own background (e.g. Mashable) would
                            // otherwise show the dark app theme behind their dark text.
                            // The loading overlay covers this until the page renders.
                            setBackgroundColor(android.graphics.Color.WHITE)
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView,
                                    request: WebResourceRequest,
                                ): Boolean {
                                    // Only web content stays in the WebView. Anything else
                                    // (file://, intent://, custom schemes) is handed to the
                                    // system so a hostile page cannot pivot to local files.
                                    val scheme = request.url.scheme?.lowercase()
                                    if (scheme == "http" || scheme == "https") return false
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                                    }
                                    return true
                                }

                                override fun onPageStarted(
                                    view: WebView?,
                                    url: String?,
                                    favicon: android.graphics.Bitmap?,
                                ) {
                                    isWebViewLoading = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isWebViewLoading = false
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: android.webkit.WebResourceError?,
                                ) {
                                    isWebViewLoading = false
                                }
                            }
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            // JS is on, so local access must be off: below API 30 these
                            // default to true, letting a page read app-private files.
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            @Suppress("DEPRECATION")
                            settings.allowFileAccessFromFileURLs = false
                            @Suppress("DEPRECATION")
                            settings.allowUniversalAccessFromFileURLs = false
                        }.also { webView = it }
                    },
                    update = { wv ->
                        val url = article?.url
                        if (url != null && wv.url == null) {
                            isWebViewLoading = true
                            wv.loadUrl(url)
                        }
                    },
                    onRelease = { wv ->
                        // Without this each reader visit leaks a WebView and its
                        // renderer process until GC.
                        webView = null
                        wv.stopLoading()
                        wv.loadUrl("about:blank")
                        wv.webViewClient = WebViewClient()
                        wv.destroy()
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                if (isWebViewLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center,
                    ) {
                        TerminalLoader(
                            labels = listOf("CONNECT", "GET /page", "RENDER"),
                            modifier = Modifier.fillMaxWidth(0.6f),
                        )
                    }
                }
        }
    }
}

    // Dimming Scrim behind the toast
    if (showAuthToast) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    showAuthToast = false
                }
        )
    }

    // Terminal Toast overlay matching screenshot design
    AnimatedVisibility(
        visible = showAuthToast,
        enter = fadeIn(animationSpec = tween(250)) + slideInVertically(initialOffsetY = { it }, animationSpec = tween(250)),
        exit = fadeOut(animationSpec = tween(250)) + slideOutVertically(targetOffsetY = { it }, animationSpec = tween(250)),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        TerminalToast(
            actionLabel = "save articles",
            commandLabel = "save",
            modifier = Modifier.clickable {
                showAuthToast = false
                onSignInClick()
            }
        )
    }
}
}
