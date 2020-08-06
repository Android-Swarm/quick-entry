package com.zetzaus.quickentry.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.zetzaus.quickentry.R
import com.zetzaus.quickentry.extensions.isSafeEntryCompletionURL
import com.zetzaus.quickentry.extensions.isSafeEntryURL
import kotlinx.android.synthetic.main.fragment_web.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jsoup.Jsoup

class WebFragment : Fragment() {

    private lateinit var viewModel: WebFragmentViewModel
    private lateinit var url: String
    private var location: Location? = null

    private var fromCode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_web, container, false)

        requireArguments().apply {
            url = getString(URL_KEY)!!
            fromCode = getBoolean(FROM_CODE_KEY)
            location = getParcelable(LOCATION_KEY)
        }

        Log.d(TAG, "Received URL: $url")

        root.findViewById<WebView>(R.id.webView).apply {
            settings.apply {
                @SuppressLint("SetJavaScriptEnabled")
                javaScriptEnabled = true
                setAppCacheEnabled(true)
                setAppCachePath(requireContext().cacheDir.path)
                databaseEnabled = true
                domStorageEnabled = true
            }

            addJavascriptInterface(JsHandler(), "HTMLOUT")

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    request?.url?.let {
                        if (it.scheme !in listOf("http", "https")) {
                            return handleIntentScheme(view!!, it.toString())
                        }
                    }
                    return false
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    progressIndicator.setProgressCompat(newProgress, true)
                    viewModel.updateProgressIndicator(newProgress)

                    if (newProgress == 100) {
                        view?.let { processCurrentURL(view) }
                    }
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    setSubtitle(title)
                }
            }

            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            loadUrl(this@WebFragment.url)
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(WebFragmentViewModel::class.java)

        viewModel.progressIndicatorVisibility.observe(viewLifecycleOwner,
            Observer<Boolean> { t ->
                t?.let { visible ->
                    progressIndicator.visibility = if (visible) View.VISIBLE else View.GONE
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        setSubtitle(null)
    }

    /**
     * Sets the action bar's subtitle for the [AppCompatActivity] that this [WebFragment] is attached to.
     *
     * @param sub The subtitle to set.
     */
    private fun setSubtitle(sub: String?) {
        (activity as AppCompatActivity).supportActionBar?.subtitle = sub
    }

    private inner class JsHandler {
        @JavascriptInterface
        fun processHTML(content: String, url: String) {
            when {
                url.isSafeEntryURL() -> Jsoup.parse(content)
                    .getElementById("location-text")
                    .text()
                    .run {
                        processSafeEntry(url, this)
                    }

                url.isSafeEntryCompletionURL() -> Jsoup.parse(content)
                    .getElementsByClass("building-name ng-star-inserted")[0]
                    .text()
                    .run {
                        Log.d(TAG, "Completed transaction for the location $this")
                        checkInOrOut(content, url, this)
                    }
            }

        }

        private fun processSafeEntry(url: String, locationName: String) {

            val locationIdentifier = url + locationName
            if (!viewModel.detectedUrls.add(locationIdentifier))
                return

            Log.d(TAG, "The safe entry URL $url is for location $locationName")

            // Save to db
            if (fromCode) {
                viewModel.saveSpot(url, locationName, location!!)
                Log.d(TAG, "Persisted spot in the database")
            }

        }

        private fun checkInOrOut(content: String, url: String, locationName: String) {
            Jsoup.parse(content)
            viewModel.updateCheckIn(
                url = url,
                newCheckedIn = if (content.contains("checkin-success-page-icon.svg")) {
                    Log.d(TAG, "User has checked in from location")
                    true
                } else {
                    //checkout-success-page-icon.svg
                    Log.d(TAG, "User has checked out to the location")
                    false
                },
                locationName = locationName
            )
            Log.d(TAG, "Updated check-in status")
        }
    }

    private fun processCurrentURL(view: WebView?) {
        lifecycleScope.launch {
            delay(500) // Wait until form completion
            view?.loadUrl(
                "javascript:window.HTMLOUT.processHTML(" +
                        "'<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>', " +
                        "'${view.url}'" +
                        ");"
            )
        }
    }

    companion object {
        val TAG = WebFragment::class.simpleName
        val URL_KEY = WebFragment::class.qualifiedName + "_URL_KEY"
        val FROM_CODE_KEY = WebFragment::class.qualifiedName + "_FROM_CODE_KEY"
        val LOCATION_KEY = WebFragment::class.qualifiedName + "_LOCATION_KEY"

        fun create(url: String, fromCode: Boolean, location: Location?) = WebFragment().apply {
            arguments = Bundle().apply {
                putString(URL_KEY, url)
                putBoolean(FROM_CODE_KEY, fromCode)
                putParcelable(LOCATION_KEY, location)
            }
        }
    }

    private fun handleIntentScheme(web: WebView, url: String): Boolean {
        if (url.startsWith("intent://")) {
            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)

            if (intent != null) {
                web.stopLoading()

                web.context.packageManager.resolveActivity(
                    intent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )?.let {
                    startActivity(intent)
                } ?: intent.getStringExtra("browser_fallback_url").run {
                    web.loadUrl(this)
                }

                return true
            }
        }

        return false
    }

    fun onBackPressed() =
        webView.url.run {
            if (this.isSafeEntryURL() || this.isSafeEntryCompletionURL()) {
                false
            } else {
                webView.goBack()
                true
            }
        }
}