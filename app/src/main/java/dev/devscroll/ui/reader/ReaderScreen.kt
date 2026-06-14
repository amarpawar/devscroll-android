package dev.devscroll.ui.reader

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
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

    LaunchedEffect(showAuthToast) {
        if (showAuthToast) {
            kotlinx.coroutines.delay(2500.milliseconds)
            showAuthToast = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 530, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cursorAlpha",
    )

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
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
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
                IconButton(
                    onClick = {
                        if (user == null) {
                            showAuthToast = true
                        } else {
                            article?.let { viewModel.toggleSave(it.id) }
                        }
                    },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = if (article?.isSaved == true) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (article?.isSaved == true) "Unsave" else "Save",
                        tint = if (article?.isSaved == true)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            IconButton(
                onClick = {
                    article?.url?.let { url ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.size(40.dp),
                enabled = article?.url != null
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open in browser",
                    tint = if (article?.url != null)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

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
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView,
                                    request: WebResourceRequest,
                                ) = false

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
                        }.also { webView = it }
                    },
                    update = { wv ->
                        val url = article?.url
                        if (url != null && wv.url == null) {
                            isWebViewLoading = true
                            wv.loadUrl(url)
                        }
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
            cursorAlpha = cursorAlpha,
            modifier = Modifier.clickable {
                showAuthToast = false
                onSignInClick()
            }
        )
    }
}
}
