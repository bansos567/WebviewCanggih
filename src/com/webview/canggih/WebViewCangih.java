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
import com.google.appinventor.components.common.PropertyCategory; // IMPORT BARU YANG DIBUTUHKAN
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.*;

import java.io.File;

@DesignerComponent(
    version = 8,
    description = "WebView Canggih V8: Full Control (JS, DomStorage), Anti-Bentrok Sinyal, dan Custom Tab Tamoda.",
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

    // File Upload Variables
    private ValueCallback<Uri[]> mFilePathCallback;
    private final static int FILECHOOSER_RESULTCODE = 1;

    // WebView String Logic
    private String currentWebViewString = "";

    // VARIABLE DESIGNER PROPERTIES (Default True)
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
    // PROPERTI DESIGNER (SUDAH DIPERBAIKI KATEGORINYA)
    // =================================================================

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    public void JavascriptEnabled(boolean enabled) {
        this.jsEnabled = enabled;
        if (mainWebView != null) mainWebView.getSettings().setJavaScriptEnabled(enabled);
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Aktifkan JavaScript (Wajib True untuk web modern).")
    public boolean JavascriptEnabled() {
        return jsEnabled;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    public void DomStorageEnabled(boolean enabled) {
        this.domStorageEnabled = enabled;
        if (mainWebView != null) mainWebView.getSettings().setDomStorageEnabled(enabled);
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Aktifkan DomStorage (Wajib untuk Login & Simpan Data).")
    public boolean DomStorageEnabled() {
        return domStorageEnabled;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    public void PromptForPermission(boolean enabled) {
        this.locationEnabled = enabled;
        if (mainWebView != null) mainWebView.getSettings().setGeolocationEnabled(enabled);
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Izinkan Website akses Lokasi.")
    public boolean PromptForPermission() {
        return locationEnabled;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    public void AllowFileAccess(boolean enabled) {
        this.fileAccessEnabled = enabled;
        if (mainWebView != null) {
            mainWebView.getSettings().setAllowFileAccess(enabled);
            mainWebView.getSettings().setAllowContentAccess(enabled);
        }
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Izinkan akses file (Untuk Upload Gambar).")
    public boolean AllowFileAccess() {
        return fileAccessEnabled;
    }

    // =================================================================
    // 1. INITIALIZE & SETUP
    // =================================================================

    @SimpleFunction(description = "Inisialisasi WebView di dalam Layout.")
    public void Initialize(AndroidViewComponent containerView) {
        View view = containerView.getView();
        if (!(view instanceof ViewGroup)) return;
        ViewGroup layout = (ViewGroup) view;
        layout.removeAllViews();

        mainWebView = new WebView(context);
        WebSettings s = mainWebView.getSettings();

        // --- KONFIGURASI DARI DESIGNER PROPERTIES ---
        s.setJavaScriptEnabled(jsEnabled);
        s.setDomStorageEnabled(domStorageEnabled);
        s.setGeolocationEnabled(locationEnabled);
        s.setAllowFileAccess(fileAccessEnabled);
        s.setAllowContentAccess(fileAccessEnabled);

        // --- KONFIGURASI TETAP (FIXED) ---
        s.setDatabaseEnabled(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportMultipleWindows(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);

        // Config Tampilan Default (Kaku, Anti-Melar sesuai request Tamoda)
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(false); // Dimatikan agar tidak bisa di zoom
        s.setDisplayZoomControls(false);

        CookieManager.getInstance().setAcceptThirdPartyCookies(mainWebView, true);

        // --- DUAL INTERFACE (ANTI-BENTROK SINYAL) ---
        WebAppInterface jsBridge = new WebAppInterface(this);
        mainWebView.addJavascriptInterface(jsBridge, "Android");      // Jalur Tol: window.Android.KirimSinyal(...)
        mainWebView.addJavascriptInterface(jsBridge, "AppInventor");  // Jalur Lama: window.AppInventor.setWebViewString(...)

        // Download Listener
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

        // WebView Client
        mainWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                PageStarted(url);
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                PageFinished(url);
            }

            // =================================================================
            // TANGKAP KLIK LINK (CEGAH PINDAH HALAMAN & BUKA DI CUSTOM TAB)
            // =================================================================
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url == null) return false;

                // 1. JIKA ITU DEEP LINK (Mau buka Aplikasi DANA, Gojek, PlayStore, dll)
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        if (intent != null) {
                            activity.startActivity(intent);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                }
                // 2. JIKA ITU LINK WEB BIASA (Iklan Banner, Sponsor, Bantuan, dll)
                else {
                    BukaDiCustomTab(url); // Panggil UI Kaca Gelap Tamoda
                    return true; // KUNCI: Mencegah halaman utama Tamoda pindah!
                }
            }
        });

        // Web Chrome Client
        mainWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                OnProgressChanged(newProgress);
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                if (mFilePathCallback != null) mFilePathCallback.onReceiveValue(null);
                mFilePathCallback = filePathCallback;

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                String[] mimeTypes = {"image/*", "application/pdf"};
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                container.$form().startActivityForResult(Intent.createChooser(intent, "Pilih File"), FILECHOOSER_RESULTCODE);
                return true;
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                // Biarkan WebView yang mencoba membuat Pop-up window ditangani oleh Custom Tab juga
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                WebView dummyWebView = new WebView(context);
                dummyWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        BukaDiCustomTab(url);
                        return true;
                    }
                });
                transport.setWebView(dummyWebView);
                resultMsg.sendToTarget();
                return true;
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage cm) {
                OnConsoleMessage(cm.message(), cm.lineNumber());
                return true;
            }
        });

        container.$form().registerForActivityResult(this);
        layout.addView(mainWebView, new LinearLayout.LayoutParams(-1, -1));
    }

    // =================================================================
    // FUNGSI CUSTOM TAB DIALOG (TEMA DARK TAMODA)
    // =================================================================
    private void BukaDiCustomTab(String url) {
        // 1. Buat Dialog Fullscreen
        final Dialog customTabDialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        
        // 2. Buat Layout Utama
        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(android.graphics.Color.parseColor("#19222e")); // Background Gelap Tamoda

        // 3. --- HEADER TEMA TAMODA ---
        LinearLayout headerLayout = new LinearLayout(context);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setBackgroundColor(android.graphics.Color.parseColor("#19222e")); 
        headerLayout.setPadding(20, 30, 20, 30);
        headerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            headerLayout.setElevation(10f);
        }

        // 4. Tombol Tutup (X) - Warna Merah Elegan
        android.widget.TextView closeButton = new android.widget.TextView(context);
        closeButton.setText("✕ TUTUP");
        closeButton.setTextColor(android.graphics.Color.parseColor("#ef5350")); // Merah khas Tamoda
        closeButton.setTextSize(14f);
        closeButton.setTypeface(null, android.graphics.Typeface.BOLD);
        closeButton.setPadding(10, 10, 20, 10);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customTabDialog.dismiss(); // Hancurkan dialog, balik ke halaman utama
            }
        });

        // 5. Teks Judul URL (Nampilin domain biar rapi)
        android.widget.TextView titleView = new android.widget.TextView(context);
        try {
            java.net.URL parsedUrl = new java.net.URL(url);
            titleView.setText(parsedUrl.getHost()); 
        } catch (Exception e) {
            titleView.setText("Tamoda Browser");
        }
        titleView.setTextColor(android.graphics.Color.parseColor("#b0bec5")); // Teks abu-abu terang
        titleView.setTextSize(13f);
        titleView.setSingleLine(true);
        titleView.setPadding(20, 0, 0, 0);

        // Susun elemen Header
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        headerLayout.addView(closeButton);
        headerLayout.addView(titleView, titleParams);

        // 6. --- WEBVIEW UNTUK IKLAN/SPONSOR ---
        WebView childWebView = new WebView(context);
        childWebView.getSettings().setJavaScriptEnabled(true);
        childWebView.getSettings().setDomStorageEnabled(true);
        
        // Beda dengan web utama, web iklan ini diizinkan untuk di-zoom!
        childWebView.getSettings().setSupportZoom(true);
        childWebView.getSettings().setBuiltInZoomControls(true);
        childWebView.getSettings().setDisplayZoomControls(false); 

        childWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url); // Pastikan klik di dalam iklan tetap di tab ini
                return true;
            }
        }); 
        
        childWebView.loadUrl(url);

        // 7. --- SUSUNAN AKHIR ---
        mainLayout.addView(headerLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
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
    // 2. FITUR UTAMA
    // =================================================================

    @SimpleFunction(description = "Set Mode Desktop (True) atau Mobile (False).")
    public void SetDesktopMode(boolean enabled) {
        if (mainWebView != null) {
            WebSettings settings = mainWebView.getSettings();
            if (enabled) {
                String desktopUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36";
                settings.setUserAgentString(desktopUA);
                settings.setUseWideViewPort(true);
                settings.setLoadWithOverviewMode(true);
            } else {
                settings.setUserAgentString(null);
                settings.setUseWideViewPort(true);
                settings.setLoadWithOverviewMode(true);
            }
            mainWebView.reload();
        }
    }

    @SimpleFunction(description = "Atur Ukuran Font (Text Zoom). Default 100.")
    public void SetFontSize(int percent) {
        if (mainWebView != null) {
            mainWebView.getSettings().setTextZoom(percent);
        }
    }

    @SimpleFunction(description = "Atur Zoom Halaman. Default 0 (Auto).")
    public void SetPageZoom(int percent) {
        if (mainWebView != null) {
            mainWebView.setInitialScale(percent);
        }
    }

    // =================================================================
    // 3. FITUR LAINNYA
    // =================================================================

    @SimpleFunction(description = "Atur Scrollbar.")
    public void SetScrollbar(boolean visible) {
        if (mainWebView != null) {
            mainWebView.setVerticalScrollBarEnabled(visible);
            mainWebView.setHorizontalScrollBarEnabled(visible);
        }
    }

    @SimpleFunction(description = "Developer Mode.")
    public void SetDeveloperMode(boolean enable) {
        if (mainWebView != null) {
            WebView.setWebContentsDebuggingEnabled(enable);
        }
    }

    @SimpleFunction(description = "Tampilkan Console DevTools (Eruda).")
    public void ShowOnScreenConsole() {
        if (mainWebView != null) {
            String js = "(function () { var script = document.createElement('script'); script.src='https://cdn.jsdelivr.net/npm/eruda'; document.body.appendChild(script); script.onload = function () { eruda.init(); } })();";
            mainWebView.evaluateJavascript(js, null);
        }
    }

    @SimpleFunction(description = "Ganti User Agent Manual.")
    public void SetUserAgent(String userAgent) {
        if (mainWebView != null) {
            if (userAgent == null || userAgent.isEmpty()) {
                mainWebView.getSettings().setUserAgentString(null);
            } else {
                mainWebView.getSettings().setUserAgentString(userAgent);
            }
        }
    }

    // =================================================================
    // 4. WEBVIEW STRING & BASIC
    // =================================================================

    @SimpleFunction(description = "Jalankan JS.")
    public void RunJavaScript(String script) {
        if (mainWebView != null) mainWebView.evaluateJavascript(script, null);
    }

    @SimpleFunction(description = "Set WebViewString.")
    public void SetWebViewString(String value) {
        this.currentWebViewString = value;
        if (mainWebView != null) {
            String safeValue = value.replace("'", "\\'").replace("\n", "\\n");
            String jsCode = "window.WebViewString = '" + safeValue + "';" +
                            "var event = new CustomEvent('WebViewStringChange', { detail: '" + safeValue + "' });" +
                            "window.dispatchEvent(event);";
            mainWebView.evaluateJavascript(jsCode, null);
        }
    }

    @SimpleFunction(description = "Get WebViewString.")
    public String GetWebViewString() {
        return this.currentWebViewString;
    }

    @SimpleFunction(description = "Muat URL.")
    public void LoadUrl(String url) {
        if (mainWebView != null) mainWebView.loadUrl(url);
    }

    @SimpleFunction(description = "Muat HTML String.")
    public void LoadHtml(String html) {
        if (mainWebView != null) mainWebView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null);
    }

    @SimpleFunction(description = "Reload.")
    public void Reload() {
        if (mainWebView != null) mainWebView.reload();
    }

    @SimpleFunction(description = "Back.")
    public void GoBack() {
        if (mainWebView != null && mainWebView.canGoBack()) mainWebView.goBack();
    }

    @SimpleFunction(description = "Forward.")
    public void GoForward() {
        if (mainWebView != null && mainWebView.canGoForward()) mainWebView.goForward();
    }

    @SimpleFunction(description = "Print to PDF.")
    public void PrintWebPage(String docName) {
        if (mainWebView != null) {
            PrintManager pm = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);
            PrintDocumentAdapter pa = mainWebView.createPrintDocumentAdapter(docName);
            pm.print(docName, pa, new PrintAttributes.Builder().build());
        }
    }

    @SimpleFunction(description = "Hapus Cache.")
    public void ClearData() {
        if (mainWebView != null) {
            mainWebView.clearCache(true);
            mainWebView.clearHistory();
        }
    }

    @SimpleFunction(description = "Cari Teks.")
    public void SearchText(String text) {
        if (mainWebView != null) mainWebView.findAllAsync(text);
    }

    @SimpleFunction(description = "Cari Selanjutnya.")
    public void FindNext() {
        if (mainWebView != null) mainWebView.findNext(true);
    }

    @SimpleFunction(description = "Hapus Highlight.")
    public void ClearMatches() {
        if (mainWebView != null) mainWebView.clearMatches();
    }

    // =================================================================
    // 5. EVENTS & INTERFACE (JEMBATAN ANTI BENTROK)
    // =================================================================

    @SimpleEvent(description = "Event WebViewString Change (Jalur Lama).")
    public void WebViewStringChange(String value) {
        this.currentWebViewString = value;
        EventDispatcher.dispatchEvent(this, "WebViewStringChange", value);
    }

    @SimpleEvent(description = "Terpicu saat Web mengirim sinyal melalui Android.KirimSinyal (Jalur Anti-Bentrok).")
    public void SinyalDiterima(String data) {
        EventDispatcher.dispatchEvent(this, "SinyalDiterima", data);
    }

    public void TriggerWebViewStringChange(final String value) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                WebViewStringChange(value);
            }
        });
    }

    // INTERNAL CLASS UNTUK JEMBATAN JS
    public static class WebAppInterface {
        WebViewCangih parent;

        WebAppInterface(WebViewCangih parent) {
            this.parent = parent;
        }

        // --- INI JALUR BARU YANG ANTI-BENTROK (SinyalDiterima) ---
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

        // --- INI JALUR LAMA (WebViewStringChange) ---
        @JavascriptInterface
        public void WebViewString(String value) {
            if (parent != null) parent.TriggerWebViewStringChange(value);
        }

        @JavascriptInterface
        public void setWebViewString(String value) {
            if (parent != null) parent.TriggerWebViewStringChange(value);
        }

        @JavascriptInterface
        public String getWebViewString() {
            return (parent != null) ? parent.GetWebViewString() : "";
        }
    }

    @SimpleEvent(description = "Progress Loading.")
    public void OnProgressChanged(int progress) {
        EventDispatcher.dispatchEvent(this, "OnProgressChanged", progress);
    }

    @SimpleEvent(description = "Page Started.")
    public void PageStarted(String url) {
        EventDispatcher.dispatchEvent(this, "PageStarted", url);
    }

    @SimpleEvent(description = "Page Finished.")
    public void PageFinished(String url) {
        EventDispatcher.dispatchEvent(this, "PageFinished", url);
    }

    @SimpleEvent(description = "Console Message.")
    public void OnConsoleMessage(String message, int lineNumber) {
        EventDispatcher.dispatchEvent(this, "OnConsoleMessage", message, lineNumber);
    }
}
