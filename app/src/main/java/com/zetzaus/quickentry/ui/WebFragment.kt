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
import com.zetzaus.quickentry.R
import com.zetzaus.quickentry.extensions.isSafeEntryURL
import kotlinx.android.synthetic.main.fragment_web.*

class WebFragment : Fragment() {

    private lateinit var viewModel: WebFragmentViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_web, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(WebFragmentViewModel::class.java)
        viewModel.url = WebFragmentArgs.fromBundle(requireArguments()).url
        Log.d(TAG, "Received URL: ${viewModel.url}")

        viewModel.progressIndicatorVisibility.observe(viewLifecycleOwner,
            Observer<Boolean> { t ->
                t?.let { visible ->
                    progressIndicator.visibility = if (visible) View.VISIBLE else View.GONE
                }
            })

        webView.apply {
            @SuppressLint("SetJavaScriptEnabled")
            settings.javaScriptEnabled = true

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
                    return super.shouldOverrideUrlLoading(view, request)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "Page finished loading for url $url")

                    if (url!!.isSafeEntryURL()) {
                        Log.d(TAG, "Encountered safe entry url!")
                        view?.loadUrl("javascript:window.HTMLOUT.processHTML(document.getElementById('location-text').innerHTML);")
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    progressIndicator.setProgressCompat(newProgress, true)
                    viewModel.updateProgressIndicator(newProgress)
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    setSubtitle(title)
                }
            }

            loadUrl(viewModel.url)
        }
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
            Log.d(TAG, location)
        }
    }

    companion object {
        val TAG = WebFragment::class.simpleName
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
        if (webView.canGoBack()) {
            webView.goBack()
            true
        } else {
            false
        }
}