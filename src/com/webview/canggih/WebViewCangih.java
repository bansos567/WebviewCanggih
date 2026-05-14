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
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.InputStream;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.*;

@DesignerComponent(
    version = 15,
    description = "WebView Canggih V15 Final: Asset Domain, Async Destroy (Fix Lag 100%), Custom Tab, Navigasi Komplit.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = ""
)
@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.INTERNET, android.permission.READ_EXTERNAL_STORAGE, android.permission.WRITE_EXTERNAL_STORAGE, android.permission.ACCESS_FINE_LOCATION")
public class WebViewCangih extends AndroidNonvisibleComponent implements ActivityResultListener, OnDestroyListener, OnPauseListener, OnResumeListener {

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

    // Setelan Dinamis Asset Domain
    private String assetDomain = "https://aset.tamoda/";

    // Setelan dinamis Custom Tab
    private String tabTitle = "( Tamoda web )";
    private String tabIconText = "←"; 
    private int tabBackgroundColor = 0xFF1B2430; 
    private int tabTextColor = 0xFFFFFFFF;       
    private float tabTitleFontSize = 20f;
    private float tabIconFontSize = 32f;
    
    // Warna background utama
    private int webViewBackColor = 0xFF19222E; 

    public WebViewCangih(ComponentContainer container) {
        super(container.$form());
        this.container = container;
        this.activity = (Activity) container.$context();
        this.context = container.$context();
        this.uiHandler = new Handler(Looper.getMainLooper());
        
        // --- TAMBAHAN: Daftarkan pemantau penutupan aplikasi ---
        container.$form().registerForOnDestroy(this);
        container.$form().registerForOnPause(this);
        container.$form().registerForOnResume(this);
        // --------------------------------------------------------
    }

    // =================================================================
    // PROPERTI DESIGNER: WEBVIEW SETTINGS & ASSET DOMAIN
    // =================================================================

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "https://aset.tamoda/")
    @SimpleProperty(description = "Domain virtual untuk memanggil gambar/file dari Asset Kodular.")
    public void AssetDomain(String domain) {
        if (!domain.endsWith("/")) domain += "/";
        this.assetDomain = domain;
    }
    @SimpleProperty(category = PropertyCategory.BEHAVIOR) public String AssetDomain() { return assetDomain; }

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
    @SimpleProperty public void BackgroundColor(int argb) { this.webViewBackColor = argb; }
    @SimpleProperty(category = PropertyCategory.APPEARANCE) public int BackgroundColor() { return webViewBackColor; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "←")
    @SimpleProperty public void TabIconText(String icon) { this.tabIconText = icon; }
    @SimpleProperty(category = PropertyCategory.APPEARANCE) public String TabIconText() { return tabIconText; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "( Tamoda web )")
    @SimpleProperty public void TabTitle(String title) { this.tabTitle = title; }
    @SimpleProperty(category = PropertyCategory.APPEARANCE) public String TabTitle() { return tabTitle; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF1B2430")
    @SimpleProperty public void TabBackgroundColor(int argb) { this.tabBackgroundColor = argb; }
    @SimpleProperty(category = PropertyCategory.APPEARANCE) public int TabBackgroundColor() { return tabBackgroundColor; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFFFFFFFF")
    @SimpleProperty public void TabTextColor(int argb) { this.tabTextColor = argb; }
    @SimpleProperty(category = PropertyCategory.APPEARANCE) public int TabTextColor() { return tabTextColor; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_FLOAT, defaultValue = "20")
    @SimpleProperty public void TabTitleFontSize(float size) { this.tabTitleFontSize = size; }
    @SimpleProperty(category = PropertyCategory.APPEARANCE) public float TabTitleFontSize() { return tabTitleFontSize; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_FLOAT, defaultValue = "32")
    @SimpleProperty public void TabIconFontSize(float size) { this.tabIconFontSize = size; }
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
        s.setSupportMultipleWindows(false); 
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(false);
        s.setSupportZoom(false);
        s.setDisplayZoomControls(false);

        // --- TAMBAHAN FIX LAG / nativePollOnce ---
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        mainWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        // -----------------------------------------

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
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (url != null && url.startsWith(assetDomain)) {
                    try {
                        String fileName = url.substring(assetDomain.length());
                        if (fileName.contains("?")) fileName = fileName.substring(0, fileName.indexOf("?"));
                        if (fileName.contains("#")) fileName = fileName.substring(0, fileName.indexOf("#"));
                        
                        InputStream is = context.getAssets().open(fileName);
                        String mimeType = "application/octet-stream";
                        if (fileName.endsWith(".png")) mimeType = "image/png";
                        else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) mimeType = "image/jpeg";
                        else if (fileName.endsWith(".gif")) mimeType = "image/gif";
                        else if (fileName.endsWith(".css")) mimeType = "text/css";
                        else if (fileName.endsWith(".js")) mimeType = "application/javascript";
                        else if (fileName.endsWith(".svg")) mimeType = "image/svg+xml";
                        
                        return new WebResourceResponse(mimeType, "UTF-8", is);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null; 
                    }
                }
                return super.shouldInterceptRequest(view, url);
            }

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
    // FUNGSI CUSTOM TAB DIALOG (FIXED 60DP HEADER & ASYNC DISMISS)
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
        
        // --- TAMBAHAN FIX LAG / nativePollOnce DI CUSTOM TAB ---
        childWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null); 
        childWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        // -------------------------------------------------------

        childWebView.getSettings().setJavaScriptEnabled(true);
        childWebView.getSettings().setDomStorageEnabled(true);
        childWebView.getSettings().setSupportZoom(true);
        childWebView.getSettings().setBuiltInZoomControls(true);
        childWebView.getSettings().setDisplayZoomControls(false); 

        childWebView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (url != null && url.startsWith(assetDomain)) {
                    try {
                        String fileName = url.substring(assetDomain.length());
                        if (fileName.contains("?")) fileName = fileName.substring(0, fileName.indexOf("?"));
                        if (fileName.contains("#")) fileName = fileName.substring(0, fileName.indexOf("#"));
                        
                        InputStream is = context.getAssets().open(fileName);
                        String mimeType = "application/octet-stream";
                        if (fileName.endsWith(".png")) mimeType = "image/png";
                        else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) mimeType = "image/jpeg";
                        else if (fileName.endsWith(".gif")) mimeType = "image/gif";
                        
                        return new WebResourceResponse(mimeType, "UTF-8", is);
                    } catch (Exception e) {
                        return null; 
                    }
                }
                return super.shouldInterceptRequest(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url); 
                return true;
            }
        }); 
        
        childWebView.loadUrl(url);

        // ====================================================================
        // UPDATE V15.1: ASYNC DESTROY (FIX LAG 7 DETIK SAAT TUTUP CEPAT)
        // ====================================================================
        customTabDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (childWebView != null) {
                    try {
                        // 1. Lepas dari layout utama
                        ViewGroup parent = (ViewGroup) childWebView.getParent();
                        if (parent != null) {
                            parent.removeView(childWebView);
                        }
                        
                        // 2. Matikan Hardware Acceleration agar memori GPU langsung terlepas (Mencegah Lag)
                        childWebView.setLayerType(View.LAYER_TYPE_NONE, null);
                        
                        // 3. Hentikan semua proses secara paksa (HAPUS about:blank agar tidak bentrok dengan destroy)
                        childWebView.stopLoading();
                        childWebView.onPause(); 
                        childWebView.clearHistory();

                        // 4. Jeda 1.5 detik (1500ms). Biarkan UI Thread bernapas dan animasi tutup selesai, 
                        // baru hancurkan mesin Chromium secara aman di belakang layar.
                        uiHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    childWebView.removeAllViews();
                                    childWebView.destroy();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }, 1500); 

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
    // BLOK KODULAR & NAVIGASI KOMPLIT
    // =================================================================

    @SimpleFunction(description = "Muat URL.") 
    public void LoadUrl(String url) { 
        if (mainWebView != null) mainWebView.loadUrl(url); 
    }
    
    @SimpleFunction(description = "Tempel blok teks HTML ke sini")
    public void LoadHtml(String htmlContent) {
        if (mainWebView != null) {
            mainWebView.loadDataWithBaseURL(assetDomain, htmlContent, "text/html; charset=utf-8", "UTF-8", null);
        }
    }

    @SimpleFunction(description = "Mengeksekusi kode JavaScript langsung ke dalam WebView.")
    public void EvaluateJavaScript(String jsCode) {
        if (mainWebView != null) {
            mainWebView.evaluateJavascript(jsCode, null);
        }
    }

    @SimpleFunction(description = "Kembali ke halaman sebelumnya.")
    public void GoBack() {
        if (mainWebView != null && mainWebView.canGoBack()) {
            mainWebView.goBack();
        }
    }

    @SimpleFunction(description = "Cek apakah ada halaman sebelumnya.")
    public boolean CanGoBack() {
        return mainWebView != null && mainWebView.canGoBack();
    }

    @SimpleFunction(description = "Maju ke halaman berikutnya.")
    public void GoForward() {
        if (mainWebView != null && mainWebView.canGoForward()) {
            mainWebView.goForward();
        }
    }

    @SimpleFunction(description = "Cek apakah bisa maju ke halaman berikutnya.")
    public boolean CanGoForward() {
        return mainWebView != null && mainWebView.canGoForward();
    }

    @SimpleFunction(description = "Muat ulang (Refresh) halaman web saat ini.")
    public void Reload() {
        if (mainWebView != null) {
            mainWebView.reload();
        }
    }

    @SimpleFunction(description = "Hentikan pemuatan halaman web.")
    public void StopLoading() {
        if (mainWebView != null) {
            mainWebView.stopLoading();
        }
    }

    @SimpleFunction(description = "Bersihkan semua cache WebView.")
    public void ClearCaches() {
        if (mainWebView != null) {
            mainWebView.clearCache(true);
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
    @SimpleFunction(description = "Ambil nilai WebViewString.") 
    public String GetWebViewString() { 
        return this.currentWebViewString; 
    }

    // =================================================================
    // JEMBATAN JS & EVENT LISTENER
    // =================================================================

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
    @SimpleEvent(description = "Progres berubah.") public void OnProgressChanged(int progress) { EventDispatcher.dispatchEvent(this, "OnProgressChanged", progress); }
    @SimpleEvent(description = "Pemuatan dimulai.") public void PageStarted(String url) { EventDispatcher.dispatchEvent(this, "PageStarted", url); }
    @SimpleEvent(description = "Pemuatan selesai.") public void PageFinished(String url) { EventDispatcher.dispatchEvent(this, "PageFinished", url); }

    // =================================================================
    // LIFECYCLE MANAGER (ANTI-ANR BACKGROUND & MEMORY LEAK FIX)
    // =================================================================

    @Override
    public void onPause() {
        if (mainWebView != null) {
            mainWebView.onPause(); // Tidurkan mesin render saat app diminimize
            mainWebView.pauseTimers(); // Hentikan timer JS agar tidak makan CPU
        }
    }

    @Override
    public void onResume() {
        if (mainWebView != null) {
            mainWebView.onResume(); // Bangunkan mesin render saat app dibuka lagi
            mainWebView.resumeTimers();
        }
    }

    @Override
    public void onDestroy() {
        if (mainWebView != null) {
            try {
                // Lepaskan WebView utama dari layar dengan aman saat aplikasi ditutup
                ViewGroup parent = (ViewGroup) mainWebView.getParent();
                if (parent != null) {
                    parent.removeView(mainWebView);
                }
                mainWebView.setLayerType(View.LAYER_TYPE_NONE, null);
                mainWebView.stopLoading();
                mainWebView.onPause();
                mainWebView.clearHistory();
                mainWebView.removeAllViews();
                mainWebView.destroy();
                mainWebView = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
} // <--- PERHATIKAN: Kurung kurawal penutup class HARUS berada di paling bawah sini!

