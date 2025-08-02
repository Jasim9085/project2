package com.chaquo.python.console;

// --- (Existing imports are untouched) ---
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.chaquo.python.utils.Utils;

public class NotebookActivity extends AppCompatActivity {

    // --- ADDED: Member variables to hold the listener and WebView ---
    private ViewTreeObserver.OnGlobalLayoutListener keyboardListener;
    private WebView webView;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(Utils.resId(this, "layout", "activity_notebook"));

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(getApplicationContext()));
        }

        // --- MODIFIED: Assign to member variables ---
        webView = findViewById(Utils.resId(this, "id", "webview"));
        rootView = findViewById(android.R.id.content); // Get the root content view
        // --- End of modification ---

        WebSettings webSettings = webView.getSettings();

        // --- Standard WebView Configuration (This block is unchanged) ---
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
        
        // --- Add the JavaScript Bridge (This block is unchanged) ---
        webView.addJavascriptInterface(new WebAppInterface(this, webView), "Android");

        // --- Set a custom WebViewClient (This block is unchanged) ---
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.evaluateJavascript("javascript:onAndroidReady();", null);
            }
        });

        // --- ADDED: The keyboard visibility listener is now set up ---
        setupKeyboardListener();
        // --- End of addition ---

        // --- Load the HTML file (This line is unchanged) ---
        webView.loadUrl("file:///android_asset/jupyter_ui.html");
    }

    // --- ADDED: New method to set up the keyboard listener ---
    private void setupKeyboardListener() {
        keyboardListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            private final Rect r = new Rect();
            private boolean wasOpened = false;

            @Override
            public void onGlobalLayout() {
                // This gives us the visible display frame, which shrinks when the keyboard appears
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getRootView().getHeight();

                // Calculate the height of the keyboard by finding the difference
                int keyboardHeight = screenHeight - r.bottom;

                // A threshold helps determine if the keyboard is really open
                boolean isKeyboardVisible = keyboardHeight > screenHeight * 0.15;

                // Only call the JavaScript function if the keyboard's state has changed
                if (isKeyboardVisible != wasOpened) {
                    wasOpened = isKeyboardVisible;
                    
                    // CRITICAL: Convert raw pixels to density-independent pixels (dp)
                    // The WebView's CSS uses dp, so this ensures the bar is positioned correctly on all devices.
                    int keyboardHeightInDp = (int) (keyboardHeight / getResources().getDisplayMetrics().density);

                    // This is the JavaScript call that makes the bar appear or disappear
                    final String script = "javascript:onKeyboardVisibilityChanged(" + isKeyboardVisible + ", " + keyboardHeightInDp + ");";
                    webView.post(() -> webView.evaluateJavascript(script, null));
                }
            }
        };
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(keyboardListener);
    }

    // --- ADDED: New method to clean up the listener and prevent memory leaks ---
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // It's very important to remove the listener when the activity is destroyed
        if (keyboardListener != null) {
            rootView.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardListener);
        }
    }
}
