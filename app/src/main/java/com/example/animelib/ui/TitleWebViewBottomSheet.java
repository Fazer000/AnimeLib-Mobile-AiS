package com.example.animelib.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.animelib.R;
import com.example.animelib.util.FlexibleBottomSheetDialog;
import com.example.animelib.util.FloatingBottomSheetUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class TitleWebViewBottomSheet extends FlexibleBottomSheetDialog {

    private static final String TAG = "TitleWebViewBS";

    private final String titleName;
    private final String targetUrl;

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

        View view = LayoutInflater.from(getContext()).inflate(R.layout.bs_title_webview, null);
        setContentView(view);
        setCanceledOnTouchOutside(true);
        setCancelable(true);

        FloatingBottomSheetUtils.setupFloatingStyle(this);

        FrameLayout webViewContainer = view.findViewById(R.id.webViewContainer);
        if (webViewContainer != null) {
            int screenHeight = getContext().getResources().getDisplayMetrics().heightPixels;
            int targetHeight = (int) (screenHeight * 0.70f);
            ViewGroup.LayoutParams lp = webViewContainer.getLayoutParams();
            if (lp != null) {
                lp.height = targetHeight;
                webViewContainer.setLayoutParams(lp);
            }
        }

        initViews(view);
        setupWebView();
        loadUrl();
        setupBehavior();
    }

    private void setupBehavior() {
        BottomSheetBehavior<FrameLayout> behavior = getBehavior();
        if (behavior != null) {
            behavior.setFitToContents(true);
            behavior.setSkipCollapsed(true);
            behavior.setHideable(true);
            int screenHeight = getContext().getResources().getDisplayMetrics().heightPixels;
            behavior.setPeekHeight(screenHeight * 2);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private void initViews(View root) {
        btnRefreshWeb = root.findViewById(R.id.btnRefreshWeb);
        btnCloseWeb = root.findViewById(R.id.btnCloseWeb);
        progressBarWeb = root.findViewById(R.id.progressBarWeb);
        webViewTitle = root.findViewById(R.id.webViewTitle);
        layoutWebError = root.findViewById(R.id.layoutWebError);
        btnRetryWeb = root.findViewById(R.id.btnRetryWeb);

        if (btnCloseWeb != null) {
            btnCloseWeb.setOnClickListener(v -> dismiss());
        }

        if (btnRefreshWeb != null) {
            btnRefreshWeb.setOnClickListener(v -> {
                if (webViewTitle != null) {
                    try {
                        webViewTitle.reload();
                    } catch (Exception e) {
                        Log.e(TAG, "Error reloading webView: " + e.getMessage());
                    }
                }
            });
        }

        if (btnRetryWeb != null) {
            btnRetryWeb.setOnClickListener(v -> {
                if (layoutWebError != null) {
                    layoutWebError.setVisibility(View.GONE);
                }
                if (webViewTitle != null) {
                    try {
                        webViewTitle.reload();
                    } catch (Exception e) {
                        Log.e(TAG, "Error retrying webView: " + e.getMessage());
                    }
                }
            });
        }

        setOnKeyListener((dialog, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                if (webViewTitle != null) {
                    try {
                        if (webViewTitle.canGoBack()) {
                            webViewTitle.goBack();
                            return true;
                        }
                    } catch (Exception ignored) {}
                }
            }
            return false;
        });
    }

    private void setupWebView() {
        if (webViewTitle == null) return;

        try {
            WebSettings webSettings = webViewTitle.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
            webSettings.setDomStorageEnabled(true);
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

            try {
                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.setAcceptCookie(true);
                cookieManager.setAcceptThirdPartyCookies(webViewTitle, true);
            } catch (Exception e) {
                Log.w(TAG, "CookieManager setup failed: " + e.getMessage());
            }

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
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    if (progressBarWeb != null) {
                        progressBarWeb.setVisibility(View.GONE);
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
        } catch (Exception e) {
            Log.e(TAG, "Failed to setup WebView: " + e.getMessage(), e);
        }
    }

    private void loadUrl() {
        if (webViewTitle != null && targetUrl != null && !targetUrl.isEmpty()) {
            Log.d(TAG, "Loading URL in BottomSheet WebView: " + targetUrl);
            try {
                webViewTitle.loadUrl(targetUrl);
            } catch (Exception e) {
                Log.e(TAG, "Failed to load URL: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void dismiss() {
        if (webViewTitle != null) {
            try {
                webViewTitle.stopLoading();
                if (webViewTitle.getParent() instanceof ViewGroup) {
                    ((ViewGroup) webViewTitle.getParent()).removeView(webViewTitle);
                }
                webViewTitle.destroy();
            } catch (Exception e) {
                Log.w(TAG, "Error destroying webView: " + e.getMessage());
            }
            webViewTitle = null;
        }
        try {
            super.dismiss();
        } catch (Exception t) {
            Log.w(TAG, "Error dismissing dialog: " + t.getMessage());
        }
    }
}
