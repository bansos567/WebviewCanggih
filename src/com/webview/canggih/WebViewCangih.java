package com.webview.canggih;

import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
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
    version = 10,
    description = "WebView Canggih V10 Final: Custom Tab Tamoda Full Dinamis (Fixed 60dp), Anti-White Flash, & Load HTML.",
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

    // Setelan dasar WebView
    private boolean jsEnabled = true;
    private boolean domStorageEnabled = true;
    private boolean locationEnabled = true;
    private boolean fileAccessEnabled = true;

    // Setelan dinamis Custom Tab (FINAL)
    private String tabTitle = "( Tamoda web )";
    private String tabIconText = "←"; 
    private int tabBackgroundColor = 0xFF1B2430; 
    private int tabTextColor = 0xFFFFFFFF;       
    private float tabTitleFontSize = 20f;
    private float tabIconFontSize = 32f;
    
    // Fitur Baru: Warna background utama (Anti White Flash)
    private int webViewBackColor = 0xFF19222E; 

    public WebViewCangih(ComponentContainer container) {
        super(container.$form());
        this.container = container;
        this.activity = (Activity) container.$context();
        this.context = container.$context();
        this.uiHandler = new Handler(Looper.getMainLooper());
    }

    // =================================================================
    // PROPERTI DESIGNER: WEBVIEW SETTINGS
    // =================================================================

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty public void JavascriptEnabled(boolean enabled) {
        this.jsEnabled = enabled;
        if (mainWebView != null) mainWebView.getSettings().setJavaScriptEnabled(enabled);
    }
    @SimpleProperty(category = PropertyCategory.BEHAVIOR) public boolean JavascriptEnabled() { return jsEnabled; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty public void DomStorageEnabled(boolean enabled) {
        this.domStorageEnabled = enabled;
        if (mainWebView != null) mainWebView.getSettings().setDomStorageEnabled(enabled);
    }
    @SimpleProperty(category = PropertyCategory.BEHAVIOR) public boolean DomStorageEnabled() { return domStorageEnabled; }

    // =================================================================
    // PROPERTI DESIGNER: CUSTOM TAB DYNAMIC UI & BACKGROUND
    // =================================================================

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF19222E")
    @SimpleProperty(description = "Warna background WebView untuk mencegah layar putih saat loading")
    public void BackgroundColor(int argb) { this.webViewBackColor = argb; }
    @SimpleProperty(category = PropertyCategory.APPEARANCE) public int BackgroundColor() { return webViewBackColor; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "←")
    @SimpleProperty(description = "Ikon tombol kembali (Gunakan simbol/teks seperti ← atau ❮)")
    public void TabIconText(String icon) { this.tabIconText = icon; }
    @SimpleProperty(category = PropertyCategory.APPEARANCE) public String TabIconText() { return tabIconText; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "( Tamoda web )")
    @SimpleProperty(description = "Judul yang ditampilkan di Custom Tab")
    public void TabTitle(String title) { this.tabTitle = title; }
    @SimpleProperty(category = PropertyCategory.APPEARANCE) public String TabTitle() { return tabTitle; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF1B2430")
    @SimpleProperty(description = "Warna background untuk header Custom Tab")
    public void TabBackgroundColor(int argb) { this.tabBackgroundColor = argb; }
    @SimpleProperty(category = PropertyCategory.APPEARANCE) public int TabBackgroundColor() { return tabBackgroundColor; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFFFFFFFF")
    @SimpleProperty(description = "Warna teks judul dan ikon back")
    public void TabTextColor(int argb) { this.tabTextColor = argb; }
    @SimpleProperty(category = PropertyCategory.APPEARANCE) public int TabTextColor() { return tabTextColor; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_FLOAT, defaultValue = "20")
    @SimpleProperty(description = "Ukuran font untuk judul")
    public void TabTitleFontSize(float size) { this.tabTitleFontSize = size; }
    @SimpleProperty(category = PropertyCategory.APPEARANCE) public float TabTitleFontSize() { return tabTitleFontSize; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_FLOAT, defaultValue = "32")
    @SimpleProperty(description = "Ukuran font untuk ikon panah")
    public void TabIconFontSize(float size) { this.tabIconFontSize = size; }
    @SimpleProperty(category = PropertyCategory.APPEARANCE) public float TabIconFontSize() { return tabIconFontSize; }

    // =================================================================
    // INITIALIZE & WEBVIEW SETUP
    // =================================================================

    @SimpleFunction(description = "Inisialisasi WebView di dalam Layout.")
    public void Initialize(AndroidViewComponent containerView) {
        View view = containerView.getView();
        if (!(view instanceof ViewGroup)) return;
        ViewGroup layout = (ViewGroup) view;
        layout.removeAllViews();

        layout.setBackgroundColor(webViewBackColor);

        mainWebView = new WebView(context);
        mainWebView.setBackgroundColor(webViewBackColor);

        WebSettings s = mainWebView.getSettings();

        s.setJavaScriptEnabled(jsEnabled);
        s.setDomStorageEnabled(domStorageEnabled);
        s.setGeolocationEnabled(locationEnabled);
        s.setAllowFileAccess(fileAccessEnabled);
        s.setAllowContentAccess(fileAccessEnabled);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setDatabaseEnabled(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportMultipleWindows(true);
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
            
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url == null) return false;
                
                if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("file://")) {
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
            @Override 
            public void onProgressChanged(WebView view, int newProgress) { 
                WebViewCangih.this.OnProgressChanged(newProgress); 
            }
            
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
        });

        container.$form().registerForActivityResult(this);
        layout.addView(mainWebView, new LinearLayout.LayoutParams(-1, -1));
    }

    // =================================================================
    // FUNGSI CUSTOM TAB DIALOG (FIXED 60DP HEADER)
    // =================================================================
    private void BukaDiCustomTab(String url) {
        final Dialog customTabDialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        
        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(tabBackgroundColor); 

        float scale = context.getResources().getDisplayMetrics().density;
        int fixedHeightPx = (int) (60 * scale + 0.5f);

        LinearLayout headerLayout = new LinearLayout(context);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setBackgroundColor(tabBackgroundColor); 
        headerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        headerLayout.setPadding((int)(20 * scale), 0, (int)(20 * scale), 0); 

        android.widget.TextView closeButton = new android.widget.TextView(context);
        closeButton.setText(tabIconText); 
        closeButton.setTextColor(tabTextColor); 
        closeButton.setTextSize(tabIconFontSize); 
        closeButton.setTypeface(null, android.graphics.Typeface.BOLD);
        closeButton.setPadding(0, 0, (int)(20 * scale), 0); 
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customTabDialog.dismiss(); 
            }
        });

        android.widget.TextView titleView = new android.widget.TextView(context);
        titleView.setText(tabTitle); 
        titleView.setTextColor(tabTextColor); 
        titleView.setTextSize(tabTitleFontSize); 
        titleView.setTypeface(null, android.graphics.Typeface.BOLD); 
        titleView.setSingleLine(true);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        headerLayout.addView(closeButton);
        headerLayout.addView(titleView, titleParams);

        final WebView childWebView = new WebView(context);
        childWebView.getSettings().setJavaScriptEnabled(true);
        childWebView.getSettings().setDomStorageEnabled(true);
        childWebView.getSettings().setSupportZoom(true);
        childWebView.getSettings().setBuiltInZoomControls(true);
        childWebView.getSettings().setDisplayZoomControls(false); 

        childWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url); 
                return true;
            }
        }); 
        
        childWebView.loadUrl(url);

        customTabDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (childWebView != null) {
                    childWebView.stopLoading();
                    childWebView.destroy();
                }
            }
        });

        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, fixedHeightPx);
        mainLayout.addView(headerLayout, headerParams);
        mainLayout.addView(childWebView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        customTabDialog.setContentView(mainLayout);
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
    // BLOK KODULAR & JEMBATAN JS (ANTI-BENTROK)
    // =================================================================

    @SimpleFunction(description = "Muat URL.") public void LoadUrl(String url) { if (mainWebView != null) mainWebView.loadUrl(url); }
    
    @SimpleFunction(description = "Tempel blok teks (balon) atau variabel global berisi kode HTML ke sini")
    public void LoadHtml(String htmlContent) {
        if (mainWebView != null) {
            mainWebView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html; charset=utf-8", "UTF-8", null);
        }
    }

    @SimpleFunction(description = "Atur nilai WebViewString.")
    public void SetWebViewString(String value) {
        this.currentWebViewString = value;
        if (mainWebView != null) {
            String jsCode = "window.WebViewString = '" + value.replace("'", "\\'") + "';";
            mainWebView.evaluateJavascript(jsCode, null);
        }
    }
    @SimpleFunction(description = "Ambil nilai WebViewString.") public String GetWebViewString() { return this.currentWebViewString; }

    @SimpleEvent(description = "Terpicu saat Web mengirim sinyal melalui Android.KirimSinyal.")
    public void SinyalDiterima(String data) {
        EventDispatcher.dispatchEvent(this, "SinyalDiterima", data);
    }

    @SimpleEvent(description = "Dipicu saat WebViewString berubah.")
    public void WebViewStringChange(String value) {
        this.currentWebViewString = value;
        EventDispatcher.dispatchEvent(this, "WebViewStringChange", value);
    }

    public static class WebAppInterface {
        WebViewCangih parent;
        WebAppInterface(WebViewCangih parent) { this.parent = parent; }
        
        @JavascriptInterface 
        public void KirimSinyal(final String data) { 
            if (parent != null) {
                parent.uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        parent.SinyalDiterima(data);
                    }
                });
            } 
        }
        
        @JavascriptInterface 
        public void WebViewString(final String value) { 
            if (parent != null) {
                parent.uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        parent.WebViewStringChange(value);
                    }
                });
            } 
        }
        
        @JavascriptInterface 
        public void setWebViewString(final String value) { 
            if (parent != null) {
                parent.uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        parent.WebViewStringChange(value);
                    }
                });
            } 
        }
        
        @JavascriptInterface 
        public String getWebViewString() { 
            return (parent != null) ? parent.GetWebViewString() : ""; 
        }
    }

    @SimpleEvent(description = "Progres berubah.") public void OnProgressChanged(int progress) { EventDispatcher.dispatchEvent(this, "OnProgressChanged", progress); }
    @SimpleEvent(description = "Pemuatan dimulai.") public void PageStarted(String url) { EventDispatcher.dispatchEvent(this, "PageStarted", url); }
    @SimpleEvent(description = "Pemuatan selesai.") public void PageFinished(String url) { EventDispatcher.dispatchEvent(this, "PageFinished", url); }
}
