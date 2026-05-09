package com.webview.canggih;

import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.Message;
import android.os.Handler;
import android.os.Looper;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ConsoleMessage;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.*;

@DesignerComponent(
    version = 7,
    description = "WebView Canggih V7: Full Control (JS, DomStorage, Location) via Designer + Fix Interface.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = ""
)
@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.INTERNET, android.permission.READ_EXTERNAL_STORAGE, android.permission.WRITE_EXTERNAL_STORAGE, android.permission.ACCESS_FINE_LOCATION")
public class WebViewCangih extends AndroidNonvisibleComponent implements ActivityResultListener {

    private Context context;
    private Activity activity;
    private ComponentContainer container;
    private WebView mainWebView;
    private Handler uiHandler;

    private ValueCallback<Uri[]> mFilePathCallback;
    private final static int FILECHOOSER_RESULTCODE = 1;
    private String currentWebViewString = "";

    private boolean jsEnabled = true;
    private boolean domStorageEnabled = true;
    private boolean locationEnabled = true;
    private boolean fileAccessEnabled = true;

    public WebViewCangih(ComponentContainer container) {
        super(container.$form());
        this.container = container;
        this.activity = (Activity) container.$context();
        this.context = container.$context();
        this.uiHandler = new Handler(Looper.getMainLooper());
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(description = "Aktifkan JavaScript.")
    public void JavascriptEnabled(boolean enabled) {
        this.jsEnabled = enabled;
        if (mainWebView != null) mainWebView.getSettings().setJavaScriptEnabled(enabled);
    }

    @SimpleProperty
    public boolean JavascriptEnabled() { return jsEnabled; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(description = "Aktifkan DomStorage.")
    public void DomStorageEnabled(boolean enabled) {
        this.domStorageEnabled = enabled;
        if (mainWebView != null) mainWebView.getSettings().setDomStorageEnabled(enabled);
    }

    @SimpleProperty
    public boolean DomStorageEnabled() { return domStorageEnabled; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(description = "Izinkan Website akses Lokasi.")
    public void PromptForPermission(boolean enabled) {
        this.locationEnabled = enabled;
        if (mainWebView != null) mainWebView.getSettings().setGeolocationEnabled(enabled);
    }

    @SimpleProperty
    public boolean PromptForPermission() { return locationEnabled; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(description = "Izinkan akses file.")
    public void AllowFileAccess(boolean enabled) {
        this.fileAccessEnabled = enabled;
        if (mainWebView != null) {
            mainWebView.getSettings().setAllowFileAccess(enabled);
            mainWebView.getSettings().setAllowContentAccess(enabled);
        }
    }

    @SimpleProperty
    public boolean AllowFileAccess() { return fileAccessEnabled; }

    @SimpleFunction(description = "Inisialisasi WebView di dalam Layout.")
    public void Initialize(AndroidViewComponent containerView) {
        View view = containerView.getView();
        if (!(view instanceof ViewGroup)) return;
        ViewGroup layout = (ViewGroup) view;
        layout.removeAllViews();

        mainWebView = new WebView(context);
        WebSettings s = mainWebView.getSettings();

        s.setJavaScriptEnabled(jsEnabled);
        s.setDomStorageEnabled(domStorageEnabled);
        s.setGeolocationEnabled(locationEnabled);
        s.setAllowFileAccess(fileAccessEnabled);
        s.setAllowContentAccess(fileAccessEnabled);
        s.setDatabaseEnabled(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportMultipleWindows(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);

        CookieManager.getInstance().setAcceptThirdPartyCookies(mainWebView, true);

        WebAppInterface jsBridge = new WebAppInterface(this);
        mainWebView.addJavascriptInterface(jsBridge, "Android");
        mainWebView.addJavascriptInterface(jsBridge, "AppInventor");

        mainWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.setMimeType(mimetype);
                    String cookies = CookieManager.getInstance().getCookie(url);
                    request.addRequestHeader("cookie", cookies);
                    request.addRequestHeader("User-Agent", userAgent);
                    request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype));
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype));
                    DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                    dm.enqueue(request);
                    Toast.makeText(context, "Mulai Download...", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(context, "Gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        mainWebView.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view, String url, Bitmap favicon) { PageStarted(url); }
            @Override public void onPageFinished(WebView view, String url) { PageFinished(url); }
        });

        mainWebView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int newProgress) { OnProgressChanged(newProgress); }
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                if (mFilePathCallback != null) mFilePathCallback.onReceiveValue(null);
                mFilePathCallback = filePathCallback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                container.$form().startActivityForResult(Intent.createChooser(intent, "Pilih File"), FILECHOOSER_RESULTCODE);
                return true;
            }
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                final Dialog authDialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                WebView childView = new WebView(context);
                childView.getSettings().setJavaScriptEnabled(jsEnabled);
                childView.setWebViewClient(new WebViewClient());
                authDialog.setContentView(childView);
                authDialog.show();
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(childView);
                resultMsg.sendToTarget();
                return true;
            }
            @Override public boolean onConsoleMessage(ConsoleMessage cm) { OnConsoleMessage(cm.message(), cm.lineNumber()); return true; }
        });

        container.$form().registerForActivityResult(this);
        layout.addView(mainWebView, new LinearLayout.LayoutParams(-1, -1));
    }

    @Override
    public void resultReturned(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (mFilePathCallback == null) return;
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                results = new Uri[]{data.getData()};
            }
            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;
        }
    }

    @SimpleFunction public void LoadUrl(String url) { if (mainWebView != null) mainWebView.loadUrl(url); }
    @SimpleFunction public void Reload() { if (mainWebView != null) mainWebView.reload(); }
    @SimpleFunction public void GoBack() { if (mainWebView != null && mainWebView.canGoBack()) mainWebView.goBack(); }
    @SimpleFunction public void RunJavaScript(String script) { if (mainWebView != null) mainWebView.evaluateJavascript(script, null); }
    
    @SimpleFunction public void SetWebViewString(String value) {
        this.currentWebViewString = value;
        if (mainWebView != null) {
            String jsCode = "window.WebViewString = '" + value.replace("'", "\\'") + "';";
            mainWebView.evaluateJavascript(jsCode, null);
        }
    }
    
    @SimpleFunction public String GetWebViewString() { return this.currentWebViewString; }

    @SimpleEvent public void WebViewStringChange(String value) {
        this.currentWebViewString = value;
        EventDispatcher.dispatchEvent(this, "WebViewStringChange", value);
    }

    public static class WebAppInterface {
        WebViewCangih parent;
        WebAppInterface(WebViewCangih parent) { this.parent = parent; }
        @JavascriptInterface public void WebViewString(String value) { if (parent != null) parent.WebViewStringChange(value); }
        @JavascriptInterface public void setWebViewString(String value) { if (parent != null) parent.WebViewStringChange(value); }
        @JavascriptInterface public String getWebViewString() { return (parent != null) ? parent.GetWebViewString() : ""; }
    }

    @SimpleEvent public void OnProgressChanged(int progress) { EventDispatcher.dispatchEvent(this, "OnProgressChanged", progress); }
    @SimpleEvent public void PageStarted(String url) { EventDispatcher.dispatchEvent(this, "PageStarted", url); }
    @SimpleEvent public void PageFinished(String url) { EventDispatcher.dispatchEvent(this, "PageFinished", url); }
    @SimpleEvent public void OnConsoleMessage(String message, int lineNumber) { EventDispatcher.dispatchEvent(this, "OnConsoleMessage", message, lineNumber); }
}
