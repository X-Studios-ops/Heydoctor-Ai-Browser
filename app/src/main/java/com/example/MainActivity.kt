package com.example

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Path
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SophisticatedDarkBg
import com.example.ui.theme.SophisticatedDarkBorder
import com.example.ui.theme.SophisticatedDarkContainer
import com.example.ui.theme.SophisticatedDarkPrimary
import com.example.ui.theme.SophisticatedDarkSurface
import com.example.ui.theme.SophisticatedDarkText
import com.example.ui.theme.SophisticatedDarkTextSecondary
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = SophisticatedDarkBg
                ) { innerPadding ->
                    HeydoctorBrowserApp(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

data class BrowserTab(
    val id: String,
    val title: String,
    val url: String,
    val isIncognito: Boolean = false
)

enum class SidebarTab {
    WORKSPACE,
    HISTORY
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HeydoctorBrowserApp(modifier: Modifier = Modifier) {
    // History State
    val historyViewModel: HistoryViewModel = viewModel()
    val historyItems by historyViewModel.historyItems.collectAsStateWithLifecycle()
    var activeSidebarTab by remember { mutableStateOf(SidebarTab.WORKSPACE) }
    var historySearchQuery by remember { mutableStateOf("") }

    // Tab Management State
    var tabs by remember {
        mutableStateOf(
            listOf(
                BrowserTab(id = "1", title = "Heydoctor AI", url = "https://heydoctor.ai")
            )
        )
    }
    var activeTabId by remember { mutableStateOf("1") }
    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull() ?: BrowserTab("1", "Heydoctor AI", "https://heydoctor.ai")
    val isActiveTabIncognito = activeTab.isIncognito

    // Browser State
    var currentUrl by remember { mutableStateOf("https://heydoctor.ai") }
    var inputUrlByHand by remember { mutableStateOf("https://heydoctor.ai") }
    var pageTitle by remember { mutableStateOf("Heydoctor AI") }
    var isPageLoading by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableIntStateOf(0) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Helper to open a new tab
    val onNewTab: (Boolean) -> Unit = { isPrivate ->
        val newId = System.currentTimeMillis().toString()
        val defaultUrl = if (isPrivate) "https://www.bing.com" else "https://heydoctor.ai"
        val defaultTitle = if (isPrivate) "Incognito Tab" else "Heydoctor AI"
        val newTab = BrowserTab(id = newId, title = defaultTitle, url = defaultUrl, isIncognito = isPrivate)
        tabs = tabs + newTab
        activeTabId = newId
        currentUrl = defaultUrl
        inputUrlByHand = defaultUrl
        pageTitle = defaultTitle
        
        webViewInstance?.apply {
            if (isPrivate) {
                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                clearCache(true)
                clearHistory()
            } else {
                settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            }
            loadUrl(defaultUrl)
        }
    }

    // Helper to close a tab
    val onCloseTab: (String) -> Unit = { tabId ->
        if (tabs.size > 1) {
            val indexToClose = tabs.indexOfFirst { it.id == tabId }
            val wasActive = activeTabId == tabId
            tabs = tabs.filter { it.id != tabId }
            
            if (wasActive) {
                val newActiveIndex = if (indexToClose >= tabs.size) tabs.size - 1 else indexToClose
                val newActiveTab = tabs[newActiveIndex]
                activeTabId = newActiveTab.id
                currentUrl = newActiveTab.url
                inputUrlByHand = newActiveTab.url
                pageTitle = newActiveTab.title
                
                webViewInstance?.apply {
                    if (newActiveTab.isIncognito) {
                        settings.cacheMode = WebSettings.LOAD_NO_CACHE
                        clearCache(true)
                        clearHistory()
                    } else {
                        settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                    }
                    loadUrl(newActiveTab.url)
                }
            }
        } else {
            // Re-initialize to a default tab if all tabs are closed
            val newId = System.currentTimeMillis().toString()
            tabs = listOf(BrowserTab(id = newId, title = "Heydoctor AI", url = "https://heydoctor.ai"))
            activeTabId = newId
            currentUrl = "https://heydoctor.ai"
            inputUrlByHand = "https://heydoctor.ai"
            pageTitle = "Heydoctor AI"
            webViewInstance?.apply {
                settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                loadUrl("https://heydoctor.ai")
            }
        }
    }

    // Helper to switch tabs
    val onSwitchTab: (String) -> Unit = { tabId ->
        val tab = tabs.find { it.id == tabId }
        if (tab != null) {
            activeTabId = tabId
            currentUrl = tab.url
            inputUrlByHand = tab.url
            pageTitle = tab.title
            
            webViewInstance?.apply {
                if (tab.isIncognito) {
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    clearCache(true)
                    clearHistory()
                } else {
                    settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                }
                loadUrl(tab.url)
            }
        }
    }
    
    // Sidebar State
    var isSidebarExpanded by remember { mutableStateOf(true) }
    
    // Laptop target optimization metric states (Simulated live performance stats for 4GB system)
    var simulatedRamUsed by remember { mutableStateOf(1.2f) }
    var isSystemOptimal by remember { mutableStateOf(true) }
    
    // Read the actual Android process JVM RAM usage to feed into the simulation
    LaunchedEffect(isPageLoading) {
        while (true) {
            val runtime = Runtime.getRuntime()
            val usedMemMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024).toDouble()
            // Map actual JVM usage (typically 20MB to 180MB) to a realistic 4GB laptop footprint (e.g. 1.0GB to 1.8GB)
            val mappedFootprint = 1.0f + (usedMemMB / 250.0f).coerceIn(0.0, 1.0).toFloat() * 0.8f
            simulatedRamUsed = String.format("%.1f", mappedFootprint).toFloat()
            isSystemOptimal = simulatedRamUsed < 3.2f
            delay(2000)
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .background(SophisticatedDarkBg)
    ) {
        // Top Simulated Status and Mock Title bar (Elegant iOS/Android notch style spacing)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "9:41 AM",
                style = TextStyle(
                    color = SophisticatedDarkText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.testTag("time_display")
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Network signal indicator
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .border(1.5.dp, SophisticatedDarkText.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(SophisticatedDarkText, CircleShape)
                    )
                }
                // Custom mobile status connection indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.height(16.dp)
                ) {
                    Box(modifier = Modifier.size(3.dp, 8.dp).background(SophisticatedDarkText).clip(RoundedCornerShape(1.dp)))
                    Box(modifier = Modifier.size(3.dp, 11.dp).background(SophisticatedDarkText).clip(RoundedCornerShape(1.dp)))
                    Box(modifier = Modifier.size(3.dp, 14.dp).background(SophisticatedDarkBorder).clip(RoundedCornerShape(1.dp)))
                }
            }
        }

        // --- Core Browser Navigation Bar ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(
                width = if (isActiveTabIncognito) 1.5.dp else 1.dp,
                color = if (isActiveTabIncognito) Color(0xFF6A40B5) else SophisticatedDarkBorder
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Address Field Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActiveTabIncognito) Color(0xFF131118) else SophisticatedDarkContainer
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isActiveTabIncognito) Color(0xFF4E2C75) else SophisticatedDarkBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isActiveTabIncognito) Icons.Default.VisibilityOff else Icons.Default.Lock,
                            contentDescription = if (isActiveTabIncognito) "Private Connection" else "Secure Connection",
                            tint = if (isActiveTabIncognito) Color(0xFFCF9BFF) else SophisticatedDarkPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        
                        // Custom Address Text Input Field
                        BasicTextField(
                            value = inputUrlByHand,
                            onValueChange = { inputUrlByHand = it },
                            textStyle = TextStyle(
                                color = if (isActiveTabIncognito) Color.White else SophisticatedDarkText,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.SansSerif
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    val query = inputUrlByHand.trim()
                                    var targetUrl = query
                                    val isUrl = (query.startsWith("http://") || query.startsWith("https://")) ||
                                            (query.contains(".") && !query.contains(" ") && query.length > 3)
                                    if (!isUrl) {
                                        try {
                                            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                                            targetUrl = "https://www.bing.com/search?q=$encodedQuery"
                                        } catch (e: Exception) {
                                            targetUrl = "https://www.bing.com/search?q=$query"
                                        }
                                    } else {
                                        if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
                                            targetUrl = "https://$targetUrl"
                                        }
                                    }
                                    currentUrl = targetUrl
                                    inputUrlByHand = targetUrl
                                    tabs = tabs.map { tab ->
                                        if (tab.id == activeTabId) tab.copy(url = targetUrl) else tab
                                    }
                                    webViewInstance?.loadUrl(targetUrl)
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("url_input")
                        )

                        if (isActiveTabIncognito) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF4E2C75), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "PRIVATE",
                                    color = Color(0xFFCF9BFF),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Loading Status Indicator / Refresh action
                        if (isPageLoading) {
                            CircularProgressIndicator(
                                progress = { loadingProgress / 100f },
                                modifier = Modifier
                                    .size(20.dp)
                                    .testTag("webview_progress"),
                                color = if (isActiveTabIncognito) Color(0xFFCF9BFF) else SophisticatedDarkPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            IconButton(
                                onClick = {
                                    webViewInstance?.reload()
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("refresh_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh page",
                                    tint = if (isActiveTabIncognito) Color(0xFFCF9BFF) else SophisticatedDarkTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Navigation Controls & Sidebar toggle Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // BACK Button
                        IconButton(
                            onClick = {
                                if (webViewInstance?.canGoBack() == true) {
                                    webViewInstance?.goBack()
                                }
                            },
                            enabled = webViewInstance?.canGoBack() == true,
                            modifier = Modifier.testTag("back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Go back",
                                tint = if (webViewInstance?.canGoBack() == true) SophisticatedDarkText else SophisticatedDarkBorder,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // FORWARD Button
                        IconButton(
                            onClick = {
                                if (webViewInstance?.canGoForward() == true) {
                                    webViewInstance?.goForward()
                                }
                            },
                            enabled = webViewInstance?.canGoForward() == true,
                            modifier = Modifier.testTag("forward_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Go forward",
                                tint = if (webViewInstance?.canGoForward() == true) SophisticatedDarkText else SophisticatedDarkBorder,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // HOME Button
                        IconButton(
                            onClick = {
                                currentUrl = "https://heydoctor.ai"
                                inputUrlByHand = "https://heydoctor.ai"
                                webViewInstance?.loadUrl("https://heydoctor.ai")
                            },
                            modifier = Modifier.testTag("home_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home Page",
                                tint = SophisticatedDarkText,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // HISTORY Button
                        IconButton(
                            onClick = {
                                activeSidebarTab = SidebarTab.HISTORY
                                isSidebarExpanded = true
                            },
                            modifier = Modifier.testTag("history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Open History",
                                tint = if (activeSidebarTab == SidebarTab.HISTORY && isSidebarExpanded) SophisticatedDarkPrimary else SophisticatedDarkText,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Collapsible Sidebar Drawer Toggle Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clickable { isSidebarExpanded = !isSidebarExpanded }
                            .padding(8.dp)
                            .testTag("sidebar_toggle")
                    ) {
                        Icon(
                            imageVector = if (isSidebarExpanded) Icons.Default.MenuOpen else Icons.Default.Menu,
                            contentDescription = "Toggle projects sidebar",
                            tint = SophisticatedDarkPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (isSidebarExpanded) "Hide Projects" else "Show Projects",
                            style = TextStyle(
                                color = SophisticatedDarkPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }

        // --- Layout of Collapsible Sidebar + Webview Frame ---
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // SLEEK COLLAPSIBLE SIDEBAR
            this@Row.AnimatedVisibility(
                visible = isSidebarExpanded,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .width(160.dp)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedDarkBg),
                    border = BorderStroke(1.dp, SophisticatedDarkBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Sidebar Tab Selector Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Workspace Tab Button
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                                    .clickable { activeSidebarTab = SidebarTab.WORKSPACE }
                                    .testTag("tab_workspace_btn"),
                                shape = RoundedCornerShape(6.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (activeSidebarTab == SidebarTab.WORKSPACE) SophisticatedDarkContainer else Color.Transparent
                                ),
                                border = if (activeSidebarTab == SidebarTab.WORKSPACE) BorderStroke(1.dp, SophisticatedDarkBorder) else null
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Dashboard,
                                        contentDescription = "Workspace",
                                        tint = if (activeSidebarTab == SidebarTab.WORKSPACE) SophisticatedDarkPrimary else SophisticatedDarkTextSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Portal",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeSidebarTab == SidebarTab.WORKSPACE) SophisticatedDarkText else SophisticatedDarkTextSecondary
                                    )
                                }
                            }

                            // History Tab Button
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                                    .clickable { activeSidebarTab = SidebarTab.HISTORY }
                                    .testTag("tab_history_btn"),
                                shape = RoundedCornerShape(6.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (activeSidebarTab == SidebarTab.HISTORY) SophisticatedDarkContainer else Color.Transparent
                                ),
                                border = if (activeSidebarTab == SidebarTab.HISTORY) BorderStroke(1.dp, SophisticatedDarkBorder) else null
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "History",
                                        tint = if (activeSidebarTab == SidebarTab.HISTORY) SophisticatedDarkPrimary else SophisticatedDarkTextSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "History",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeSidebarTab == SidebarTab.HISTORY) SophisticatedDarkText else SophisticatedDarkTextSecondary
                                    )
                                }
                            }
                        }

                        if (activeSidebarTab == SidebarTab.WORKSPACE) {
                            // Sidebar Custom Logo / Banner Header
                            Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SophisticatedDarkPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "HD",
                                style = TextStyle(
                                    color = Color(0xFF381E72),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text(
                            text = "Heydoctor",
                            style = TextStyle(
                                color = SophisticatedDarkText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "AI HEALTH BROWSER",
                            style = TextStyle(
                                color = SophisticatedDarkPrimary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // SPECIAL HIGH-LIGHTED HEY DOCTOR AI PORTAL CARD
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentUrl = "https://my-hey-doctor-ai-website.vercel.app"
                                    inputUrlByHand = "https://my-hey-doctor-ai-website.vercel.app"
                                    webViewInstance?.loadUrl("https://my-hey-doctor-ai-website.vercel.app")
                                }
                                .testTag("shortcut_hey_doctor_ai_special"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, Brush.horizontalGradient(
                                listOf(Color(0xFFD0BCFF), Color(0xFF00E5FF))
                            )),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF211F26)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0xFF311062),
                                                Color(0xFF00223E)
                                            )
                                        )
                                    )
                                    .padding(10.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Live status dot
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color(0xFF00FF87), CircleShape)
                                        )
                                        Text(
                                            text = "FEATURED PORTAL",
                                            style = TextStyle(
                                                color = Color(0xFF00FF87),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Text(
                                        text = "Hey Doctor AI",
                                        style = TextStyle(
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                    
                                    Text(
                                        text = "https://my-hey-doctor-ai-website.vercel.app",
                                        style = TextStyle(
                                            color = Color(0xFFD0BCFF),
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // High Performance Optimized Target Footprint widget (matches design)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
                            border = BorderStroke(1.dp, SophisticatedDarkBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "RAM LIMIT: 4.0GB",
                                    style = TextStyle(
                                        color = SophisticatedDarkTextSecondary,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "USED: ${simulatedRamUsed}GB",
                                    style = TextStyle(
                                        color = SophisticatedDarkPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { simulatedRamUsed / 4.0f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = if (isSystemOptimal) SophisticatedDarkPrimary else Color.Red,
                                    trackColor = SophisticatedDarkBorder
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "STATUS: ${if (isSystemOptimal) "OPTIMAL" else "HEAVY"}",
                                    style = TextStyle(
                                        color = if (isSystemOptimal) Color.Green else Color.Yellow,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // QUICK SEARCH PORTAL
                        Text(
                            text = "SEARCH HUB",
                            style = TextStyle(
                                color = SophisticatedDarkTextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            modifier = Modifier.align(Alignment.Start)
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = {
                                currentUrl = "https://www.bing.com"
                                inputUrlByHand = "https://www.bing.com"
                                webViewInstance?.loadUrl("https://www.bing.com")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("shortcut_bing_search"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00809D),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Bing Search",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                currentUrl = "https://www.google.com"
                                inputUrlByHand = "https://www.google.com"
                                webViewInstance?.loadUrl("https://www.google.com")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("shortcut_google_search"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4285F4),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Google Search",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // OPEN TABS OVERVIEW
                        Text(
                            text = "OPEN TABS",
                            style = TextStyle(
                                color = SophisticatedDarkTextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            modifier = Modifier.align(Alignment.Start)
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))

                        // Column with open tabs in sidebar
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            tabs.forEach { tab ->
                                val isSelected = tab.id == activeTabId
                                val sidebarTabBg = when {
                                    tab.isIncognito && isSelected -> Color(0xFF31124D)
                                    tab.isIncognito -> Color(0xFF1B0E2A)
                                    isSelected -> SophisticatedDarkContainer
                                    else -> SophisticatedDarkSurface
                                }
                                val sidebarTabBorder = when {
                                    tab.isIncognito && isSelected -> BorderStroke(1.dp, Color(0xFFCF9BFF))
                                    tab.isIncognito -> BorderStroke(1.dp, Color(0xFF4E2C75))
                                    isSelected -> BorderStroke(1.dp, SophisticatedDarkBorder)
                                    else -> BorderStroke(1.dp, SophisticatedDarkBorder.copy(alpha = 0.5f))
                                }
                                val sidebarTabTextColor = if (isSelected) SophisticatedDarkText else SophisticatedDarkTextSecondary

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .clickable { onSwitchTab(tab.id) }
                                        .testTag("sidebar_tab_${tab.id}"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = sidebarTabBg),
                                    border = sidebarTabBorder
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (tab.isIncognito) Icons.Default.VisibilityOff else Icons.Default.Language,
                                            contentDescription = if (tab.isIncognito) "Incognito" else "Web",
                                            tint = if (tab.isIncognito) Color(0xFFCF9BFF) else SophisticatedDarkPrimary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = tab.title.ifEmpty { tab.url.removePrefix("https://").removePrefix("www.") },
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = sidebarTabTextColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { onCloseTab(tab.id) },
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close tab",
                                                tint = sidebarTabTextColor.copy(alpha = 0.6f),
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        } else {
                            // History Panel UI
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Search & Clear Header Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // History Search field
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(28.dp)
                                            .border(1.dp, SophisticatedDarkBorder, RoundedCornerShape(6.dp))
                                            .background(SophisticatedDarkSurface)
                                            .padding(horizontal = 4.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Search",
                                                tint = SophisticatedDarkTextSecondary,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            BasicTextField(
                                                value = historySearchQuery,
                                                onValueChange = { historySearchQuery = it },
                                                textStyle = TextStyle(
                                                    color = SophisticatedDarkText,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth().testTag("history_search_input")
                                            )
                                        }
                                    }

                                    // Clear All button
                                    IconButton(
                                        onClick = { historyViewModel.clearHistory() },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                            .background(SophisticatedDarkSurface)
                                            .testTag("clear_history_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteForever,
                                            contentDescription = "Clear History",
                                            tint = Color.Red,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                // Chronological History List
                                val filteredItems = historyItems.filter {
                                    it.title.contains(historySearchQuery, ignoreCase = true) ||
                                    it.url.contains(historySearchQuery, ignoreCase = true)
                                }

                                if (filteredItems.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (historySearchQuery.isEmpty()) "No history logged yet." else "No matches found.",
                                            color = SophisticatedDarkTextSecondary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        filteredItems.forEach { item ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(44.dp)
                                                    .clickable {
                                                        currentUrl = item.url
                                                        inputUrlByHand = item.url
                                                        webViewInstance?.loadUrl(item.url)
                                                    }
                                                    .testTag("history_item_${item.id}"),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
                                                border = BorderStroke(1.dp, SophisticatedDarkBorder.copy(alpha = 0.5f))
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Language,
                                                        contentDescription = "Visited Page",
                                                        tint = SophisticatedDarkPrimary,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Column(
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text(
                                                            text = item.title,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = SophisticatedDarkText,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = item.url.removePrefix("https://").removePrefix("www."),
                                                            fontSize = 7.sp,
                                                            fontWeight = FontWeight.Normal,
                                                            color = SophisticatedDarkTextSecondary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { historyViewModel.deleteHistoryEntry(item.id) },
                                                        modifier = Modifier.size(16.dp).testTag("delete_history_${item.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Delete entry",
                                                            tint = SophisticatedDarkTextSecondary.copy(alpha = 0.6f),
                                                            modifier = Modifier.size(10.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // MAIN WEBVIEW CARD CONTAINER
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
                border = BorderStroke(1.dp, SophisticatedDarkBorder),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // --- Scrollable Tab Bar Strip ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SophisticatedDarkSurface)
                            .padding(top = 8.dp, bottom = 4.dp, start = 8.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Scrollable Row of Tabs
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tabs.forEach { tab ->
                                val isSelected = tab.id == activeTabId
                                val tabBg = when {
                                    tab.isIncognito && isSelected -> Color(0xFF31124D)
                                    tab.isIncognito -> Color(0xFF1B0E2A)
                                    isSelected -> SophisticatedDarkContainer
                                    else -> Color.Transparent
                                }
                                val tabBorderColor = when {
                                    tab.isIncognito && isSelected -> Color(0xFFCF9BFF)
                                    tab.isIncognito -> Color(0xFF4E2C75)
                                    isSelected -> SophisticatedDarkBorder
                                    else -> Color.Transparent
                                }
                                val tabTextColor = if (isSelected) SophisticatedDarkText else SophisticatedDarkTextSecondary
                                
                                Card(
                                    modifier = Modifier
                                        .widthIn(max = 130.dp)
                                        .height(34.dp)
                                        .clickable { onSwitchTab(tab.id) }
                                        .testTag("tab_item_${tab.id}"),
                                    shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                                    colors = CardDefaults.cardColors(containerColor = tabBg),
                                    border = if (tabBorderColor != Color.Transparent) BorderStroke(1.dp, tabBorderColor) else null
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (tab.isIncognito) Icons.Default.VisibilityOff else Icons.Default.Language,
                                            contentDescription = if (tab.isIncognito) "Incognito Tab" else "Standard Tab",
                                            tint = if (tab.isIncognito) Color(0xFFCF9BFF) else SophisticatedDarkPrimary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        
                                        Text(
                                            text = tab.title.ifEmpty { tab.url.removePrefix("https://").removePrefix("www.") },
                                            style = TextStyle(
                                                color = tabTextColor,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        
                                        // Close tab button
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .clickable { onCloseTab(tab.id) }
                                                .background(if (isSelected) SophisticatedDarkBorder.copy(alpha = 0.3f) else Color.Transparent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close tab",
                                                tint = tabTextColor.copy(alpha = 0.7f),
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Action buttons (+ Tab, + Incognito)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            // Standard Tab Button
                            IconButton(
                                onClick = { onNewTab(false) },
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(SophisticatedDarkContainer)
                                    .testTag("add_tab_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Standard Tab",
                                    tint = SophisticatedDarkPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            // Incognito Tab Button
                            IconButton(
                                onClick = { onNewTab(true) },
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF31124D))
                                    .testTag("add_incognito_tab_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = "Add Incognito Tab",
                                    tint = Color(0xFFCF9BFF),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    
                    HorizontalDivider(color = SophisticatedDarkBorder, thickness = 1.dp)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Optimized WebView implementation
                        AndroidView(
                            factory = { context ->
                                WebView(context).apply {
                                    layoutParams = android.view.ViewGroup.LayoutParams(
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    
                                    // target optimization settings
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        databaseEnabled = true
                                        cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                        loadWithOverviewMode = true
                                        useWideViewPort = true
                                        setSupportZoom(true)
                                        builtInZoomControls = true
                                        displayZoomControls = false
                                    }

                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                            super.onPageStarted(view, url, favicon)
                                            isPageLoading = true
                                            url?.let {
                                                currentUrl = it
                                                inputUrlByHand = it
                                                tabs = tabs.map { tab ->
                                                    if (tab.id == activeTabId) tab.copy(url = it) else tab
                                                }
                                            }
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            isPageLoading = false
                                            url?.let {
                                                currentUrl = it
                                                inputUrlByHand = it
                                                tabs = tabs.map { tab ->
                                                    if (tab.id == activeTabId) tab.copy(url = it) else tab
                                                }
                                            }
                                            view?.title?.let { title ->
                                                pageTitle = title
                                                tabs = tabs.map { tab ->
                                                    if (tab.id == activeTabId) tab.copy(title = title) else tab
                                                }
                                            }
                                            if (!isActiveTabIncognito) {
                                                historyViewModel.addHistoryEntry(url ?: "", view?.title ?: "")
                                            }
                                        }

                                        override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                            return false // Load inside this WebView itself
                                        }
                                    }

                                    webChromeClient = object : WebChromeClient() {
                                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                            super.onProgressChanged(view, newProgress)
                                            loadingProgress = newProgress
                                        }

                                        override fun onReceivedTitle(view: WebView?, title: String?) {
                                            super.onReceivedTitle(view, title)
                                            title?.let { pageTitle = it }
                                        }
                                    }

                                    // Open default land page
                                    loadUrl(currentUrl)
                                    webViewInstance = this
                                }
                            },
                        update = { webView ->
                            // Ensure webViewInstance stays updated
                            webViewInstance = webView
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("browser_webview")
                    )

                    // Overlay indicator when page loads to give visually high-fidelity transitions
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isPageLoading && loadingProgress < 20,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(SophisticatedDarkSurface.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(
                                            Brush.sweepGradient(
                                                listOf(SophisticatedDarkPrimary, SophisticatedDarkBg)
                                            ),
                                            CircleShape
                                        )
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(SophisticatedDarkSurface, CircleShape)
                                    )
                                }
                                Text(
                                    text = "Connecting to Secure medical nodes...",
                                    style = TextStyle(
                                        color = SophisticatedDarkTextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        }

        // --- Bottom Tablet/Mobile Navigation Control Bar Matching Design Specs ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(SophisticatedDarkBg),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Simulated pill layout
            Row(
                modifier = Modifier.width(320.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Arrow icon
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clickable {
                            if (webViewInstance?.canGoBack() == true) {
                                webViewInstance?.goBack()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .border(1.5.dp, SophisticatedDarkText.copy(alpha = 0.4f), RoundedCornerShape(1.dp))
                    )
                }

                // Center Circle / Square System controls
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .border(1.5.dp, SophisticatedDarkText.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                )

                // Right Back triangle
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clickable {
                            if (webViewInstance?.canGoBack() == true) {
                                webViewInstance?.goBack()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        val composePath = Path().apply {
                            moveTo(size.width * 0.8f, size.height * 0.1f)
                            lineTo(size.width * 0.2f, size.height * 0.5f)
                            lineTo(size.width * 0.8f, size.height * 0.9f)
                            close()
                        }
                        drawPath(
                            path = composePath,
                            color = SophisticatedDarkText.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

