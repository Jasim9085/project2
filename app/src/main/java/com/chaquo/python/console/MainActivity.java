package com.chaquo.python.console;

import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.chaquo.python.utils.Utils;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(Utils.resId(this, "layout", "activity_console"));

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(getApplicationContext()));
        }

        WebView webView = findViewById(Utils.resId(this, "id", "webview"));
        WebSettings webSettings = webView.getSettings();

        // --- Standard WebView Configuration ---
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(false);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.setWebChromeClient(new WebChromeClient());
        WebView.setWebContentsDebuggingEnabled(true);
        
        // --- Add the JavaScript Bridge ---
        webView.addJavascriptInterface(new WebAppInterface(this, webView), "Android");

        // --- Set a custom WebViewClient to solve the race condition ---
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // The HTML is loaded, the bridge is injected. NOW it's safe to run the script.
                view.evaluateJavascript("javascript:onAndroidReady();", null);
            }
        });

        // --- Load the HTML file ---
        webView.loadUrl("file:///android_asset/jupyter_ui.html");
    }
}
