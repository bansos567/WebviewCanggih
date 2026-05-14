package com.tamoda.customtab;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.*;

@DesignerComponent(
    version = 1,
    description = "Tamoda Custom Tab Ekstensi Khusus: Super Garcep, Header 55px, No URL, Anti-White Flash & Anti-Hang.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = ""
)
@SimpleObject(external = true)
public class TamodaCustomTab extends AndroidNonvisibleComponent {

    private Context context;
    private Activity activity;
    private Handler uiHandler;
    
    // Setelan Desain Khusus Bos
    private int headerHeight = 55;
    private int headerColor = 0xFF1B2430; 
    private int webBackgroundColor = 0xFF19222E; // Anti-White Flash
    
    private String backIconText = "←"; // Bisa diisi font icon
    private int backIconColor = 0xFFFFFFFF;
    private float backIconSize = 24f;

    private String titleText = "Kebijakan Privasi";
    private int titleColor = 0xFFFFFFFF;
    private float titleSize = 18f;

    public TamodaCustomTab(ComponentContainer container) {
        super(container.$form());
        this.activity = (Activity) container.$context();
        this.context = container.$context();
        this.uiHandler = new Handler(Looper.getMainLooper());
    }

    // =================================================================
    // PROPERTI DESAIN KREATIF (Muncul di Designer Kodular)
    // =================================================================

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "55")
    @SimpleProperty(description = "Tinggi Header/Toolbar (dalam px/dp).")
    public void HeaderHeight(int height) { this.headerHeight = height; }
    @SimpleProperty public int HeaderHeight() { return this.headerHeight; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF1B2430")
    @SimpleProperty(description = "Warna background Header.")
    public void HeaderColor(int argb) { this.headerColor = argb; }
    @SimpleProperty public int HeaderColor() { return this.headerColor; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF19222E")
    @SimpleProperty(description = "Warna ruang kosong sebelum web dimuat (Mencegah layar berkedip putih).")
    public void WebBackgroundColor(int argb) { this.webBackgroundColor = argb; }
    @SimpleProperty public int WebBackgroundColor() { return this.webBackgroundColor; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "←")
    @SimpleProperty(description = "Teks/Icon untuk tombol kembali. Bisa paste Unicode Font Icon.")
    public void BackIconText(String text) { this.backIconText = text; }
    @SimpleProperty public String BackIconText() { return this.backIconText; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFFFFFFFF")
    @SimpleProperty public void BackIconColor(int argb) { this.backIconColor = argb; }
    @SimpleProperty public int BackIconColor() { return this.backIconColor; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "Kebijakan Privasi")
    @SimpleProperty(description = "Judul yang ditampilkan di Header (Tanpa URL).")
    public void TitleText(String text) { this.titleText = text; }
    @SimpleProperty public String TitleText() { return this.titleText; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFFFFFFFF")
    @SimpleProperty public void TitleColor(int argb) { this.titleColor = argb; }
    @SimpleProperty public int TitleColor() { return this.titleColor; }

    // =================================================================
    // FUNGSI EKSEKUSI (IN-APP DIALOG SUPER GARCEP)
    // =================================================================

    @SimpleFunction(description = "Buka Link menggunakan Custom Tab.")
    public void BukaLink(String url) {
        if (url == null || url.isEmpty()) return;

        final Dialog customTabDialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        
        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(webBackgroundColor); 

        float scale = context.getResources().getDisplayMetrics().density;
        int fixedHeightPx = (int) (headerHeight * scale + 0.5f);

        LinearLayout headerLayout = new LinearLayout(context);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setBackgroundColor(headerColor); 
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);
        headerLayout.setPadding((int)(15 * scale), 0, (int)(15 * scale), 0); 

        TextView closeButton = new TextView(context);
        closeButton.setText(backIconText); 
        closeButton.setTextColor(backIconColor); 
        closeButton.setTextSize(backIconSize); 
        closeButton.setTypeface(Typeface.DEFAULT_BOLD);
        closeButton.setPadding(0, 0, (int)(20 * scale), 0); 
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customTabDialog.dismiss(); 
            }
        });

        TextView titleView = new TextView(context);
        titleView.setText(titleText); 
        titleView.setTextColor(titleColor); 
        titleView.setTextSize(titleSize); 
        titleView.setTypeface(Typeface.DEFAULT_BOLD); 
        titleView.setSingleLine(true);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        headerLayout.addView(closeButton);
        headerLayout.addView(titleView, titleParams);

        final WebView childWebView = new WebView(context);
        
        // --- ANTI WHITE FLASH FIX ---
        childWebView.setBackgroundColor(webBackgroundColor);
        
        childWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null); 
        childWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        childWebView.getSettings().setJavaScriptEnabled(true);
        childWebView.getSettings().setDomStorageEnabled(true);

        childWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url); 
                return true;
            }
        }); 
        
        childWebView.loadUrl(url);

        // ====================================================================
        // ULTIMATE ANTI-FREEZE (DELAYED CLEANUP) - DIJAMIN LANCAR WALAUPUN CLOSE GARCEP
        // ====================================================================
        customTabDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (childWebView != null) {
                    try {
                        childWebView.setVisibility(View.GONE);
                        ViewGroup parent = (ViewGroup) childWebView.getParent();
                        if (parent != null) {
                            parent.removeView(childWebView);
                        }
                        
                        uiHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    childWebView.setWebChromeClient(null);
                                    childWebView.setWebViewClient(null);
                                    childWebView.stopLoading();
                                    childWebView.clearHistory();
                                    childWebView.removeAllViews();
                                    childWebView.destroy();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }, 3000); 

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
}
