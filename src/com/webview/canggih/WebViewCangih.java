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
    version = 9,
    description = "WebView Canggih V9: Full Control, Anti-Bentrok Sinyal, & Tamoda Premium TabView.",
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

    // =================================================================
    // PROPERTI DESIGNER
    // =================================================================

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    public void JavascriptEnabled(boolean enabled) {
        this.jsEnabled = enabled;
        if (mainWebView != null) mainWebView.getSettings().setJavaScriptEnabled(enabled);
    }
    
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Aktifkan JavaScript.")
    public boolean JavascriptEnabled() { return jsEnabled; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    public void DomStorageEnabled(boolean enabled) {
        this.domStorageEnabled = enabled;
        if (mainWebView != null) mainWebView.getSettings().setDomStorageEnabled(enabled);
    }
    
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Aktifkan DomStorage.")
    public boolean DomStorageEnabled() { return domStorageEnabled; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    public void PromptForPermission(boolean enabled) {
        this.locationEnabled = enabled;
        if (mainWebView != null) mainWebView.getSettings().setGeolocationEnabled(enabled);
    }
    
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Izinkan Website akses Lokasi.")
    public boolean PromptForPermission() { return locationEnabled; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    public void AllowFileAccess(boolean enabled) {
        this.fileAccessEnabled = enabled;
        if (mainWebView != null) {
            mainWebView.getSettings().setAllowFileAccess(enabled);
            mainWebView.getSettings().setAllowContentAccess(enabled);
        }
    }
    
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Izinkan akses file.")
    public boolean AllowFileAccess() { return fileAccessEnabled; }

    // =================================================================
    // INITIALIZE
    // =================================================================

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
        
        s.setBuiltInZoomControls(false);
        s.setSupportZoom(false);
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
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype));
                    DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                    dm.enqueue(request);
                    Toast.makeText(context, "Mengunduh file...", Toast.LENGTH_SHORT).show();
                } catch (Exception e) { e.printStackTrace(); }
            }
        });

        mainWebView.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view, String url, Bitmap favicon) { PageStarted(url); }
            @Override public void onPageFinished(WebView view, String url) { PageFinished(url); }
            
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url == null) return false;
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        if (intent != null) activity.startActivity(intent);
                    } catch (Exception e) { e.printStackTrace(); }
                    return true;
                } 
                
                WebView.HitTestResult result = view.getHitTestResult();
                if (result != null && result.getType() == 0) {
                    return false; 
                } else {
                    BukaDiCustomTab(url);
                    return true; 
                }
            }
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
                WebView dummyWebView = new WebView(context);
                dummyWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        BukaDiCustomTab(url);
                        return true;
                    }
                });
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(dummyWebView);
                resultMsg.sendToTarget();
                return true;
            }
        });

        container.$form().registerForActivityResult(this);
        layout.addView(mainWebView, new LinearLayout.LayoutParams(-1, -1));
    }

    // =================================================================
    // FUNGSI CUSTOM TAB DIALOG (TEMA DARK TAMODA V2)
    // =================================================================
    private void BukaDiCustomTab(String url) {
        final Dialog customTabDialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        
        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(android.graphics.Color.parseColor("#111822")); 

        // --- HEADER BAR ---
        LinearLayout headerLayout = new LinearLayout(context);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setBackgroundColor(android.graphics.Color.parseColor("#19222e")); 
        headerLayout.setPadding(30, 40, 30, 40);
        headerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // 1. TOMBOL TUTUP (PANAH KEMBALI) DI KIRI
        android.widget.TextView backBtn = new android.widget.TextView(context);
        backBtn.setText("←"); // Ikon Panah Kiri
        backBtn.setTextColor(android.graphics.Color.WHITE);
        backBtn.setTextSize(22f);
        backBtn.setPadding(10, 0, 30, 0);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customTabDialog.dismiss(); 
            }
        });

        // 2. BRANDING TEXT DI TENGAH
        android.widget.TextView titleView = new android.widget.TextView(context);
        titleView.setText("TAMODA TABVIEW");
        titleView.setTextColor(android.graphics.Color.parseColor("#FFD700")); // Warna Emas
        titleView.setTextSize(14f);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setLetterSpacing(0.1f);
        
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        
        // 3. SUSUN HEADER
        headerLayout.addView(backBtn);
        headerLayout.addView(titleView, titleParams);

        // --- WEBVIEW ---
        WebView childWebView = new WebView(context);
        childWebView.getSettings().setJavaScriptEnabled(true);
        childWebView.getSettings().setDomStorageEnabled(true);
        childWebView.getSettings().setSupportZoom(true);
        childWebView.getSettings().setBuiltInZoomControls(true);
        childWebView.getSettings().setDisplayZoomControls(false); 
        
        childWebView.setWebViewClient(new WebViewClient()); 
        childWebView.loadUrl(url);

        mainLayout.addView(headerLayout);
        mainLayout.addView(childWebView, new LinearLayout.LayoutParams(-1, -1));

        customTabDialog.setContentView(mainLayout);
        
        // Animasi Slide Up
        Window window = customTabDialog.getWindow();
        if (window != null) {
            window.setWindowAnimations(android.R.style.Animation_InputMethod);
        }
        
        customTabDialog.show();
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

    // =================================================================
    // FUNGSI BLOK KODULAR
    // =================================================================

    @SimpleFunction public void LoadUrl(String url) { if (mainWebView != null) mainWebView.loadUrl(url); }
    @SimpleFunction public void Reload() { if (mainWebView != null) mainWebView.reload(); }
    @SimpleFunction public void GoBack() { if (mainWebView != null && mainWebView.canGoBack()) mainWebView.goBack(); }
    @SimpleFunction public void RunJavaScript(String script) { if (mainWebView != null) mainWebView.evaluateJavascript(script, null); }
    
    @SimpleFunction
    public void SetWebViewString(String value) {
        this.currentWebViewString = value;
        if (mainWebView != null) {
            String safeValue = value.replace("'", "\\'");
            mainWebView.evaluateJavascript("window.WebViewString = '" + safeValue + "';", null);
        }
    }
    
    @SimpleFunction public String GetWebViewString() { return this.currentWebViewString; }

    @SimpleEvent public void WebViewStringChange(String value) {
        this.currentWebViewString = value;
        EventDispatcher.dispatchEvent(this, "WebViewStringChange", value);
    }

    @SimpleEvent public void SinyalDiterima(String data) {
        EventDispatcher.dispatchEvent(this, "SinyalDiterima", data);
    }

    public void TriggerSinyalDiterima(final String data) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() { SinyalDiterima(data); }
        });
    }

    public static class WebAppInterface {
        WebViewCangih parent;
        WebAppInterface(WebViewCangih parent) { this.parent = parent; }
        
        @JavascriptInterface public void KirimSinyal(String data) { if (parent != null) parent.TriggerSinyalDiterima(data); }
        @JavascriptInterface public void WebViewString(String value) { if (parent != null) parent.TriggerSinyalDiterima(value); }
        @JavascriptInterface public void setWebViewString(String value) { if (parent != null) parent.TriggerSinyalDiterima(value); }
        @JavascriptInterface public String getWebViewString() { return (parent != null) ? parent.GetWebViewString() : ""; }
    }

    @SimpleEvent public void OnProgressChanged(int progress) { EventDispatcher.dispatchEvent(this, "OnProgressChanged", progress); }
    @SimpleEvent public void PageStarted(String url) { EventDispatcher.dispatchEvent(this, "PageStarted", url); }
    @SimpleEvent public void PageFinished(String url) { EventDispatcher.dispatchEvent(this, "PageFinished", url); }
    @SimpleEvent public void OnConsoleMessage(String message, int lineNumber) { EventDispatcher.dispatchEvent(this, "OnConsoleMessage", message, lineNumber); }
}
