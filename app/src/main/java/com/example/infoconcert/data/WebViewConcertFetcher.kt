package com.example.infoconcert.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WebViewConcertFetcher {

    suspend fun fetch(context: Context, url: String): List<Concert> = withTimeout(45_000L) {
        val appContext = context.applicationContext
        val js = appContext.assets.open("extract.js").bufferedReader().use { it.readText() }
        val raw = withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                val finished = AtomicBoolean(false)
                val mainHandler = Handler(Looper.getMainLooper())
                val webView = WebView(appContext)
                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true
                webView.settings.loadsImagesAutomatically = false
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        if (!finished.compareAndSet(false, true)) return
                        view.evaluateJavascript(js) { result ->
                            cont.resume(result ?: "null")
                            mainHandler.post { runCatching { webView.destroy() } }
                        }
                    }

                    @SuppressLint("WebViewClient")
                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError
                    ) {
                        if (request.isForMainFrame && finished.compareAndSet(false, true)) {
                            cont.resumeWithException(
                                RuntimeException("Erreur de chargement : ${error.description}")
                            )
                            mainHandler.post { runCatching { webView.destroy() } }
                        }
                    }
                }
                webView.loadUrl(url)
                cont.invokeOnCancellation {
                    mainHandler.post { runCatching { webView.destroy() } }
                }
            }
        }
        parseConcerts(raw)
    }

    private fun parseConcerts(raw: String): List<Concert> {
        if (raw == "null") return emptyList()
        val json = runCatching { JSONObject("{\"v\":$raw}").optString("v", "[]") }
            .getOrDefault("[]")
        val array = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        val concerts = ArrayList<Concert>()
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val concert = Concert(
                name = o.optString("name"),
                city = o.optString("city"),
                date = o.optString("date"),
                time = o.optString("time").ifEmpty { null },
                venue = o.optString("venue"),
                price = o.optString("price").ifEmpty { null },
                bookingUrl = o.optString("bookingUrl").ifEmpty { null },
                infoUrl = o.optString("infoUrl")
            )
            if (concert.name.isNotBlank() && concert.date.isNotBlank()) {
                concerts.add(concert)
            }
        }
        return concerts
    }
}
