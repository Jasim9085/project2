package com.chaquo.python.console;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class NotebookActivity extends AppCompatActivity {

    private ViewTreeObserver.OnGlobalLayoutListener keyboardListener;
    private WebView webView;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_notebook);  // Direct reference

            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(getApplicationContext()));
            }

            webView = findViewById(R.id.webview);
            rootView = findViewById(android.R.id.content);

            if (webView == null) {
                throw new NullPointerException("WebView not found in layout.");
            }

            WebSettings webSettings = webView.getSettings();
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

            webView.addJavascriptInterface(new WebAppInterface(this, webView), "Android");

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    view.evaluateJavascript("javascript:onAndroidReady();", null);
                }
            });

            setupKeyboardListener();
            webView.loadUrl("file:///android_asset/jupyter_ui.html");

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupKeyboardListener() {
        keyboardListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            private final Rect r = new Rect();
            private boolean wasOpened = false;

            @Override
            public void onGlobalLayout() {
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getRootView().getHeight();
                int keyboardHeight = screenHeight - r.bottom;
                boolean isKeyboardVisible = keyboardHeight > screenHeight * 0.15;

                if (isKeyboardVisible != wasOpened) {
                    wasOpened = isKeyboardVisible;
                    int keyboardHeightInDp = (int) (keyboardHeight / getResources().getDisplayMetrics().density);
                    final String script = "javascript:onKeyboardVisibilityChanged(" + isKeyboardVisible + ", " + keyboardHeightInDp + ");";
                    if (webView != null) {
                        webView.post(() -> webView.evaluateJavascript(script, null));
                    }
                }
            }
        };
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(keyboardListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (keyboardListener != null && rootView != null) {
            rootView.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardListener);
        }
    }
}
