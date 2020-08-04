package com.zetzaus.quickentry.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
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

class WebFragment : Fragment() {

    private lateinit var viewModel: WebFragmentViewModel
    private var fromCode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_web, container, false)

        val url = requireArguments().getString(URL_KEY)
        fromCode = requireArguments().getBoolean(FROM_CODE_KEY)

        Log.d(TAG, "Received URL: $url")
        Log.d(
            TAG,
            if (fromCode)
                "The URL is from code, further data will be saved"
            else
                "The URL is from list, no data will be written"
        )

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

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    url?.let {
                        Log.d(TAG, "Page finished loading for url $url")
                        processCurrentURL(view, it)
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    progressIndicator.setProgressCompat(newProgress, true)
                    viewModel.updateProgressIndicator(newProgress)

                    if (newProgress == 100) {
                        // Web app navigation is not recognized in onPageFinished()
                        view?.let {
                            if (view.url.isSafeEntryCompletionURL()) {
                                processCurrentURL(view, view.url)
                            }
                        }
                    }
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    setSubtitle(title)
                }
            }

            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            loadUrl(url)
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

    class JsHandler {
        @JavascriptInterface
        fun processHTML(location: String) {
            Log.d(TAG, "The URL is for location $location")
            // TODO: save to db
        }

        @JavascriptInterface
        fun checkInOrOut(content: String) {
            if (content.contains("checkout-success-page-icon.svg")) {
                Log.d(TAG, "User has checked out from location")
                // TODO: update db checkedIn = false
            } else if (content.contains("checkin-success-page-icon.svg")) {
                Log.d(TAG, "User has checked in to the location")
                // TODO: update db checkedIn = true
            }
        }
    }

    private fun processCurrentURL(view: WebView?, url: String) {
        if (url.isSafeEntryURL()) {
            Log.d(TAG, "Encountered safe entry url!")
            view?.loadUrl("javascript:window.HTMLOUT.processHTML(document.getElementById('location-text').innerHTML);")
        } else if (url.isSafeEntryCompletionURL()) {
            Log.d(TAG, "Encountered safe entry completion url: $url")
            lifecycleScope.launch {
                delay(500) // Wait until form completion
                view?.loadUrl("javascript:window.HTMLOUT.checkInOrOut('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');")
            }

        }
    }

    companion object {
        val TAG = WebFragment::class.simpleName
        val URL_KEY = WebFragment::class.qualifiedName + "_URL_KEY"
        val FROM_CODE_KEY = WebFragment::class.qualifiedName + "_FROM_CODE_KEY"

        fun create(url: String, fromCode: Boolean) = WebFragment().apply {
            arguments = Bundle().apply {
                putString(URL_KEY, url)
                putBoolean(FROM_CODE_KEY, fromCode)
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
        if (webView.url.isSafeEntryURL()) {
            false
        } else {
            webView.goBack()
            true
        }
}