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
            setContentView(R.layout.activity_notebook);  // MUST be inside try-catch

            rootView = findViewById(android.R.id.content);
            webView = findViewById(R.id.webview);

            if (webView == null) {
                throw new NullPointerException("WebView not found in layout.");
            }

            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(getApplicationContext()));
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
                    try {
                        super.onPageFinished(view, url);
                        view.evaluateJavascript("javascript:onAndroidReady();", null);
                    } catch (Exception e) {
                        handleError("WebViewClient Error", e);
                    }
                }
            });

            setupKeyboardListener();
            webView.loadUrl("file:///android_asset/jupyter_ui.html");

        } catch (Exception e) {
            handleError("Error during onCreate", e);
        }
    }

    private void setupKeyboardListener() {
        try {
            keyboardListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                private final Rect r = new Rect();
                private boolean wasOpened = false;

                @Override
                public void onGlobalLayout() {
                    try {
                        rootView.getWindowVisibleDisplayFrame(r);
                        int screenHeight = rootView.getRootView().getHeight();
                        int keyboardHeight = screenHeight - r.bottom;
                        boolean isKeyboardVisible = keyboardHeight > screenHeight * 0.15;

                        if (isKeyboardVisible != wasOpened) {
                            wasOpened = isKeyboardVisible;
                            int keyboardHeightInDp = (int) (keyboardHeight / getResources().getDisplayMetrics().density);
                            final String script = "javascript:onKeyboardVisibilityChanged(" + isKeyboardVisible + ", " + keyboardHeightInDp + ");";
                            if (webView != null) {
                                webView.post(() -> {
                                    try {
                                        webView.evaluateJavascript(script, null);
                                    } catch (Exception e) {
                                        handleError("evaluateJavascript Error", e);
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        handleError("Keyboard listener error", e);
                    }
                }
            };
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(keyboardListener);
        } catch (Exception e) {
            handleError("setupKeyboardListener Error", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (keyboardListener != null && rootView != null) {
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardListener);
            }
        } catch (Exception e) {
            handleError("onDestroy Error", e);
        }
    }

    private void handleError(String source, Exception e) {
        e.printStackTrace();
        Toast.makeText(this, source + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
    }
}
