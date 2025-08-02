package com.chaquo.python.console;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class NotebookActivity extends AppCompatActivity {

    private ViewTreeObserver.OnGlobalLayoutListener keyboardListener;
    private WebView webView;
    private View rootView;
    private EditText keyboardInput; // ✅ Hidden native EditText

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_notebook);  // MUST be inside try-catch

            rootView = findViewById(android.R.id.content);
            webView = findViewById(R.id.webview);
            keyboardInput = findViewById(R.id.keyboardInput); // ✅ Bind the EditText

            if (webView == null || keyboardInput == null) {
                throw new NullPointerException("WebView or keyboardInput not found in layout.");
            }

            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(getApplicationContext()));
            }
            Python py = Python.getInstance();

            String modulePath = Environment.getExternalStorageDirectory().getAbsolutePath() + ".JupyMini/modules";
            String addPathScript = String.format(
                "import sys\n" +
                "if r'%s' not in sys.path:\n" +
                "    sys.path.append(r'%s')\n",
                modulePath, modulePath
            );
            py.getModule("__main__").eval(addPathScript);

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

            // ✅ KeyboardBridge interface for JS
            webView.addJavascriptInterface(new Object() {
                @JavascriptInterface
                public void focus() {
                    runOnUiThread(() -> {
                        keyboardInput.requestFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(keyboardInput, InputMethodManager.SHOW_IMPLICIT);
                    });
                }

                @JavascriptInterface
                public void blur() {
                    runOnUiThread(() -> {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(keyboardInput.getWindowToken(), 0);
                        keyboardInput.clearFocus();
                    });
                }
            }, "KeyboardBridge");

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

    @Override
    public void onBackPressed() {
        if (keyboardInput != null && keyboardInput.hasFocus()) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(keyboardInput.getWindowToken(), 0);
            keyboardInput.clearFocus();
        } else {
            super.onBackPressed();
        }
    }

    private void handleError(String source, Exception e) {
        e.printStackTrace();
        Toast.makeText(this, source + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
    }
}
