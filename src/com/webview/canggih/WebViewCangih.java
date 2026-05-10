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

// SEMUA ANOTASI DAN KATEGORI DIAMBIL DARI SINI
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.*;

import java.io.File;

@DesignerComponent(
    version = 11,
    description = "WebView Canggih V11: Full Control, Anti-Bentrok Sinyal, dan Custom Tab Tamoda Dinamis.",
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

    // =================================================================
    // PROPERTI DINAMIS CUSTOM TAB (BARU)
    // =================================================================
    private String tabTitle = "( Tamoda web )";
    private String tabIconText = "←"; // Bos bisa ganti jadi karakter Google Material Icon atau Unicode dari Kodular
    private int tabBackgroundColor = 0xFF1B2430; // Biru Dongker Gelap
    private int tabTextColor = 0xFFFFFFFF;       // Putih
    private float tabTitleFontSize = 20f;
    private float tabIconFontSize = 32f;

    public WebViewCangih(ComponentContainer container) {
        super(container.$form());
        this.container = container;
        this.activity = (Activity) container.$context();
        this.context = container.$context();
        this.uiHandler = new Handler(Looper.getMainLooper());
    }

    // =================================================================
    // PROPERTI DESIGNER (ASLI MILIK BOS)
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
    // PROPERTI DESIGNER CUSTOM TAB
    // =================================================================

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "←")
    @SimpleProperty(description = "Ikon tombol kembali (Bisa paste Unicode atau teks Material Icon)")
    public void TabIconText(String icon) { this.tabIconText = icon; }
    @SimpleProperty(category = PropertyCategory.APPEARANCE) public String TabIconText() { return tabIconText; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "( Tamoda web )")
    @SimpleProperty(description = "Judul yang ditampilkan di Custom Tab")
    public void TabTitle(String title) { this.tabTitle = title; }
    @SimpleProperty(category = PropertyCategory.APPEARANCE) public String TabTitle() { return tabTitle; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF1B2430")
    @SimpleProperty(description = "Warna background header Custom Tab")
    public void TabBackgroundColor(int argb) { this.tabBackgroundColor = argb; }
    @SimpleProperty(category = PropertyCategory.APPEARANCE) public int TabBackgroundColor() { return tabBackgroundColor; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFFFFFFFF")
    @SimpleProperty(description = "Warna teks judul dan ikon back")
    public void TabTextColor(int argb) { this.tabTextColor = argb; }
    @SimpleProperty(category = PropertyCategory.APPEARANCE) public int TabTextColor() { return tabTextColor; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_FLOAT, defaultValue = "20")
    @SimpleProperty(description = "Ukuran font judul")
    public void TabTitleFontSize(float size) { this.tabTitleFontSize = size; }
    @SimpleProperty(category = PropertyCategory.APPEARANCE) public float TabTitleFontSize() { return tabTitleFontSize; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_FLOAT, defaultValue = "32")
    @SimpleProperty(description = "Ukuran font ikon panah")
    public void TabIconFontSize(float size) { this.tabIconFontSize = size; }
    @SimpleProperty(category = PropertyCategory.APPEARANCE) public float TabIconFontSize() { return tabIconFontSize; }

    // =================================================================
    // INITIALIZE (ASLI MILIK BOS)
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
        
        // Anti Melar (Sesuai Permintaan)
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
            
            // TANGKAP LINK DAN BUKA DI CUSTOM TAB SECARA CERDAS
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url == null) return false;

                // 1. JIKA ITU DEEP LINK (Intent ke DANA, Gojek, PlayStore, dll)
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        if (intent != null) activity.startActivity(intent);
                    } catch (Exception e) { e.printStackTrace(); }
                    return true;
                } 
                
                // 2. CEK SUMBER TRIGGER (DARI SISTEM ATAU SENTUHAN JARI)
                WebView.HitTestResult result = view.getHitTestResult();
                if (result != null && result.getType() == 0) {
                    // Tipe 0 berarti bukan hasil sentuhan user, tapi perintah Blok Kodular (LoadUrl)
                    return false; 
                } else {
                    // Link hasil klik/tap dari User, Buka di Custom Tab Dialog
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
            
            // TANGKAP WINDOW.OPEN
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
    // FUNGSI CUSTOM TAB DIALOG (DINAMIS 60DP)
    // =================================================================
    private void BukaDiCustomTab(String url) {
        final Dialog customTabDialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        
        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(tabBackgroundColor); 

        // Hitung Tinggi Fixed 60dp
        float scale = context.getResources().getDisplayMetrics().density;
        int fixedHeightPx = (int) (60 * scale + 0.5f);

        LinearLayout headerLayout = new LinearLayout(context);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setBackgroundColor(tabBackgroundColor); 
        headerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        headerLayout.setPadding((int)(16 * scale), 0, (int)(16 * scale), 0);

        // 1. TOMBOL BACK DINAMIS
        android.widget.TextView closeButton = new android.widget.TextView(context);
        closeButton.setText(tabIconText);
        closeButton.setTextColor(tabTextColor); 
        closeButton.setTextSize(tabIconFontSize);
        closeButton.setTypeface(null, android.graphics.Typeface.BOLD);
        closeButton.setPadding(0, 0, (int)(16 * scale), 0);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customTabDialog.dismiss(); 
            }
        });

        // 2. TEKS JUDUL DINAMIS
        android.widget.TextView titleView = new android.widget.TextView(context);
        titleView.setText(tabTitle); 
        titleView.setTextColor(tabTextColor); 
        titleView.setTextSize(tabTitleFontSize);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setSingleLine(true);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        headerLayout.addView(closeButton);
        headerLayout.addView(titleView, titleParams);

        // 3. SETUP WEBVIEW ANAK
        WebView childWebView = new WebView(context);
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
    // FUNGSI BLOK KODULAR (100% ASLI MILIK BOS, TIDAK ADA YANG HILANG)
    // =================================================================

    @SimpleFunction(description = "Muat URL.") public void LoadUrl(String url) { if (mainWebView != null) mainWebView.loadUrl(url); }
    @SimpleFunction(description = "Muat Ulang Halaman.") public void Reload() { if (mainWebView != null) mainWebView.reload(); }
    @SimpleFunction(description = "Kembali ke Halaman Sebelumnya.") public void GoBack() { if (mainWebView != null && mainWebView.canGoBack()) mainWebView.goBack(); }
    @SimpleFunction(description = "Jalankan skrip JavaScript.") public void RunJavaScript(String script) { if (mainWebView != null) mainWebView.evaluateJavascript(script, null); }
    
    @SimpleFunction(description = "Atur nilai WebViewString.")
    public void SetWebViewString(String value) {
        this.currentWebViewString = value;
        if (mainWebView != null) {
            String jsCode = "window.WebViewString = '" + value.replace("'", "\\'") + "';";
            mainWebView.evaluateJavascript(jsCode, null);
        }
    }
    
    @SimpleFunction(description = "Ambil nilai WebViewString saat ini.") public String GetWebViewString() { return this.currentWebViewString; }

    // =================================================================
    // EVENTS & INTERFACE JS (100% ASLI MILIK BOS, JAVA 7 SAFE)
    // =================================================================

    @SimpleEvent(description = "Dipicu saat WebViewString berubah.")
    public void WebViewStringChange(String value) {
        this.currentWebViewString = value;
        EventDispatcher.dispatchEvent(this, "WebViewStringChange", value);
    }

    @SimpleEvent(description = "Terpicu saat Web mengirim sinyal melalui Android.KirimSinyal.")
    public void SinyalDiterima(String data) {
        EventDispatcher.dispatchEvent(this, "SinyalDiterima", data);
    }

    public void TriggerWebViewStringChange(final String value) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() { WebViewStringChange(value); }
        });
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
        
        // JALUR BARU (ANTI-BENTROK)
        @JavascriptInterface public void KirimSinyal(String data) { if (parent != null) parent.TriggerSinyalDiterima(data); }
        
        // JALUR LAMA
        @JavascriptInterface public void WebViewString(String value) { if (parent != null) parent.TriggerWebViewStringChange(value); }
        @JavascriptInterface public void setWebViewString(String value) { if (parent != null) parent.TriggerWebViewStringChange(value); }
        @JavascriptInterface public String getWebViewString() { return (parent != null) ? parent.GetWebViewString() : ""; }
    }

    @SimpleEvent(description = "Dipicu saat progres pemuatan berubah.") public void OnProgressChanged(int progress) { EventDispatcher.dispatchEvent(this, "OnProgressChanged", progress); }
    @SimpleEvent(description = "Dipicu saat halaman mulai dimuat.") public void PageStarted(String url) { EventDispatcher.dispatchEvent(this, "PageStarted", url); }
    @SimpleEvent(description = "Dipicu saat halaman selesai dimuat.") public void PageFinished(String url) { EventDispatcher.dispatchEvent(this, "PageFinished", url); }
    @SimpleEvent(description = "Pesan konsol dari website.") public void OnConsoleMessage(String message, int lineNumber) { EventDispatcher.dispatchEvent(this, "OnConsoleMessage", message, lineNumber); }
}
