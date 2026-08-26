package com.example.animelib.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.animelib.R;
import com.example.animelib.util.FlexibleBottomSheetDialog;

public class TitleWebViewBottomSheet extends FlexibleBottomSheetDialog {

    private static final String TAG = "TitleWebViewBS";

    private final String titleName;
    private final String targetUrl;

    private TextView tvTitleWebHeader;
    private TextView tvSubtitleWebUrl;
    private ImageView btnRefreshWeb;
    private ImageView btnCloseWeb;
    private ProgressBar progressBarWeb;
    private WebView webViewTitle;
    private View layoutWebError;
    private Button btnRetryWeb;

    public TitleWebViewBottomSheet(@NonNull Context context, String titleName, String targetUrl) {
        super(context);
        this.titleName = titleName;
        this.targetUrl = targetUrl;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bs_title_webview);

        initViews();
        setupWebView();
        loadUrl();
    }

    private void initViews() {
        tvTitleWebHeader = findViewById(R.id.tvTitleWebHeader);
        tvSubtitleWebUrl = findViewById(R.id.tvSubtitleWebUrl);
        btnRefreshWeb = findViewById(R.id.btnRefreshWeb);
        btnCloseWeb = findViewById(R.id.btnCloseWeb);
        progressBarWeb = findViewById(R.id.progressBarWeb);
        webViewTitle = findViewById(R.id.webViewTitle);
        layoutWebError = findViewById(R.id.layoutWebError);
        btnRetryWeb = findViewById(R.id.btnRetryWeb);

        if (tvTitleWebHeader != null && titleName != null) {
            tvTitleWebHeader.setText(titleName);
        }

        if (tvSubtitleWebUrl != null && targetUrl != null) {
            tvSubtitleWebUrl.setText(targetUrl);
        }

        if (btnCloseWeb != null) {
            btnCloseWeb.setOnClickListener(v -> dismiss());
        }

        if (btnRefreshWeb != null) {
            btnRefreshWeb.setOnClickListener(v -> {
                if (webViewTitle != null) {
                    webViewTitle.reload();
                }
            });
        }

        if (btnRetryWeb != null) {
            btnRetryWeb.setOnClickListener(v -> {
                if (layoutWebError != null) {
                    layoutWebError.setVisibility(View.GONE);
                }
                if (webViewTitle != null) {
                    webViewTitle.reload();
                }
            });
        }

        setOnKeyListener((dialog, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                if (webViewTitle != null && webViewTitle.canGoBack()) {
                    webViewTitle.goBack();
                    return true;
                }
            }
            return false;
        });
    }

    private void setupWebView() {
        if (webViewTitle == null) return;

        WebSettings webSettings = webViewTitle.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webViewTitle, true);

        webViewTitle.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (progressBarWeb != null) {
                    progressBarWeb.setVisibility(View.VISIBLE);
                }
                if (layoutWebError != null) {
                    layoutWebError.setVisibility(View.GONE);
                }
                if (tvSubtitleWebUrl != null && url != null) {
                    tvSubtitleWebUrl.setText(url);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (progressBarWeb != null) {
                    progressBarWeb.setVisibility(View.GONE);
                }
                if (view.getTitle() != null && !view.getTitle().isEmpty() && tvTitleWebHeader != null) {
                    if (titleName == null || titleName.isEmpty() || titleName.equals("Тайтл")) {
                        tvTitleWebHeader.setText(view.getTitle());
                    }
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request != null && request.isForMainFrame()) {
                    if (progressBarWeb != null) {
                        progressBarWeb.setVisibility(View.GONE);
                    }
                    if (layoutWebError != null) {
                        layoutWebError.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        webViewTitle.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (progressBarWeb != null) {
                    if (newProgress < 100) {
                        progressBarWeb.setVisibility(View.VISIBLE);
                        progressBarWeb.setProgress(newProgress);
                    } else {
                        progressBarWeb.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    private void loadUrl() {
        if (webViewTitle != null && targetUrl != null && !targetUrl.isEmpty()) {
            Log.d(TAG, "Loading URL in BottomSheet WebView: " + targetUrl);
            webViewTitle.loadUrl(targetUrl);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        if (webViewTitle != null) {
            webViewTitle.stopLoading();
            webViewTitle.destroy();
        }
        super.onDetachedFromWindow();
    }
}
