package com.chaquo.python.console;

import android.app.Activity;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import org.json.JSONObject;

public class WebAppInterface {
    private Activity activity;
    private WebView webView;

    WebAppInterface(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
    }

    /**
     * This method is exposed to JavaScript and can be called from the WebView.
     * It executes Python code and sends the result back to a JavaScript callback.
     * @param cellId The ID of the cell that is being executed.
     * @param code The Python code to execute.
     */
    @JavascriptInterface
    public void runPythonCode(String cellId, String code) {
        try {
            // Get the Python instance and the executor module
            Python py = Python.getInstance();
            PyObject executor = py.getModule("py_executor");

            // Call the run_code function in our Python module
            PyObject resultObj = executor.callAttr("run_code", code);

            // Get the result as a Java string
            String result = resultObj.toString();

            // We need to send the result back to the WebView on the UI thread.
            activity.runOnUiThread(() -> {
                // Escape the result for safe transport within a JavaScript string
                String escapedResult = JSONObject.quote(result);

                // Call the 'showOutput' JavaScript function with the cellId and result
                webView.evaluateJavascript("javascript:showOutput('" + cellId + "', " + escapedResult + ");", null);
            });

        } catch (Exception e) {
            // It's good practice to handle potential errors
            e.printStackTrace();
        }
    }
}
