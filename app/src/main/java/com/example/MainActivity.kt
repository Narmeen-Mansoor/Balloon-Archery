package com.example

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          val context = LocalContext.current
          val webView = remember {
            WebView(context).apply {
              layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
              )
              webViewClient = WebViewClient()
              webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                  consoleMessage?.let {
                    Log.d("WebViewConsole", "${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}")
                  }
                  return true
                }
              }
              settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                displayZoomControls = false
                builtInZoomControls = false
                allowFileAccess = true
                allowContentAccess = true
              }
              loadUrl("file:///android_asset/index.html")
            }
          }

          val lifecycleOwner = LocalLifecycleOwner.current
          DisposableEffect(lifecycleOwner, webView) {
            val observer = LifecycleEventObserver { _, event ->
              when (event) {
                Lifecycle.Event.ON_RESUME -> {
                  webView.onResume()
                }
                Lifecycle.Event.ON_PAUSE -> {
                  // Safely pause audio context and WebView execution in background
                  webView.evaluateJavascript(
                    "try { if (typeof audioCtx !== 'undefined' && audioCtx) { audioCtx.suspend(); } } catch(e) {}",
                    null
                  )
                  webView.onPause()
                }
                else -> {}
              }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
              lifecycleOwner.lifecycle.removeObserver(observer)
              (webView.parent as? ViewGroup)?.removeView(webView)
              webView.stopLoading()
            }
          }

          AndroidView(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding),
            factory = { webView }
          )
        }
      }
    }
  }
}

