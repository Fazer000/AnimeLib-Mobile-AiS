package com.example.animelib;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.animelib.util.CustomToast;
import com.example.animelib.util.CookieSyncManager;
import com.example.animelib.util.UpdateManager;
import com.example.animelib.models.UpdateInfo;

import com.example.animelib.data.DatabaseManager;
import com.example.animelib.data.entity.TokenEntity;
import com.example.animelib.dialogs.ThemeSelectionDialog;
import com.example.animelib.models.TokenResponse;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.animelib.ui.JSInjectionsHandler;
import com.example.animelib.util.ThemeUtils;
import com.example.animelib.viewmodel.AppSettingsViewModel;
import com.example.animelib.api.ApiService;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.viewpager2.widget.ViewPager2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import okhttp3.OkHttpClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private CircularProgressIndicator spinner;
    private FrameLayout spinnerBackground;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FrameLayout fullscreenContainer;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private boolean isFirstLoad = true;
    private String currentDomain = null;
    private long lastBackPressTime = 0;
    private static final int BACK_PRESS_INTERVAL = 2000; // 2 секунды
    private static final int REQUEST_URL_INPUT = 1001;
    private OkHttpClient httpClient;
    private Executor executor;
    private Gson gson;
    private AppSettingsViewModel viewModel;
    private ApiService apiService;
    private JSInjectionsHandler JSInjectionsHandler;
    private ThemeSelectionDialog themeDialog;
    private DatabaseManager databaseManager;

    private View webErrorLayout;
    private TextView tvErrorTitle;
    private TextView tvErrorMessage;
    private com.google.android.material.button.MaterialButton btnRetryLoad;
    private View btnOpenDownloads;
    private String lastFailedUrl = null;
    private boolean isUrlInputShowing = false;

    private View urlTopBanner;
    private TextView tvCurrentUrlBanner;
    private View btnChangeUrlBanner;
    private View btnCloseUrlBanner;
    private View btnBannerSelectCis;
    private View btnBannerSelectOther;
    private final android.os.Handler urlBannerHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable urlBannerRunnable;
    private boolean isUrlBannerShownThisSession = false;

    private final android.os.Handler spinnerTimeoutHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable hideSpinnerRunnable = this::hideDomainChangeSpinner;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Применяем тему синхронно из SharedPreferences до инфлейта разметки
        int initialTheme = ThemeUtils.getSavedThemePreference(this);
        ThemeUtils.applyThemeToActivity(this, initialTheme);

        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            isUrlInputShowing = savedInstanceState.getBoolean("isUrlInputShowing", false);
        }

        webView = findViewById(R.id.webView);
        spinner = findViewById(R.id.spinner);
        spinnerBackground = findViewById(R.id.spinnerBackground);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        fullscreenContainer = findViewById(R.id.fullscreenContainer);

        webErrorLayout = findViewById(R.id.webErrorLayout);
        tvErrorTitle = findViewById(R.id.tvErrorTitle);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        btnRetryLoad = findViewById(R.id.btnRetryLoad);
        btnOpenDownloads = findViewById(R.id.btnOpenDownloads);

        if (btnRetryLoad != null) {
            btnRetryLoad.setOnClickListener(v -> reloadWebView());
        }

        if (btnOpenDownloads != null) {
            btnOpenDownloads.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, com.example.animelib.ui.DownloadsActivity.class);
                startActivity(intent);
            });
        }

        // Initialize HTTP client and Gson
        httpClient = new OkHttpClient();
        executor = Executors.newSingleThreadExecutor();
        gson = new Gson();

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(AppSettingsViewModel.class);

        // Initialize API service
        apiService = new ApiService(this);

        databaseManager = new DatabaseManager(this);

        // Initialize player button handler
        JSInjectionsHandler = new JSInjectionsHandler(this);

        // Initialize theme manager
        themeDialog = new ThemeSelectionDialog(this, apiService);

        // Clear WebView cache to avoid Chromium errors
        try {
            android.webkit.WebView tempWebView = new android.webkit.WebView(this);
            tempWebView.clearCache(true);
            tempWebView.clearHistory();
            tempWebView.destroy();
        } catch (Exception e) {
            Log.w("MainActivity", "Failed to clear WebView cache", e);
        }

        // Включаем аппаратное ускорение для всего приложения
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        );

        // Load and apply theme
        loadAndApplyTheme();

        // Обновляем токены при запуске
        updateTokensOnStartup();

        setupWebView();
        setupRefreshLayout();
        setupBackPressHandler();
        setupUrlTopBanner();

        checkAndLoadUrl();

        checkForUpdatesOnStartup();
    }

    private void checkForUpdatesOnStartup() {
        UpdateManager.checkForUpdates(this, httpClient, new UpdateManager.CheckUpdateCallback() {
            @Override
            public void onUpdateCheckResult(boolean hasUpdate, UpdateInfo updateInfo, String currentVersion) {
                if (hasUpdate && updateInfo != null) {
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed() && !isUrlInputShowing) {
                            CustomToast.showUpdateAlert(MainActivity.this, updateInfo);
                        }
                    });
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.d("MainActivity", "Startup update check error: " + errorMessage);
            }
        });
    }

    private void loadAndApplyTheme() {
        executor.execute(() -> {
            try {
                // Приоритет отдаем SharedPreferences (синхронный источник актуальной темы)
                int sharedPrefTheme = ThemeUtils.getSavedThemePreference(this);
                int dbThemeMode = databaseManager.loadThemeSetting();
                
                int finalTheme = (sharedPrefTheme >= 0 && sharedPrefTheme <= 2) ? sharedPrefTheme : dbThemeMode;
                Log.d("Theme", "Loaded theme - SharedPref: " + sharedPrefTheme + ", DB: " + dbThemeMode + " -> final: " + finalTheme);
                
                // Применяем тему в главном потоке
                runOnUiThread(() -> {
                    ThemeUtils.applyThemeToActivity(MainActivity.this, finalTheme);
                    updateSystemBarColors();
                    Log.d("Theme", "Theme applied: " + finalTheme);
                });
                
            } catch (Exception e) {
                Log.e("Theme", "Failed to load and apply theme", e);
                runOnUiThread(() -> {
                    ThemeUtils.applyThemeToActivity(MainActivity.this, ThemeUtils.getSavedThemePreference(MainActivity.this));
                    updateSystemBarColors();
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAndApplyTheme();
    }

    private void updateSystemBarColors() {
        int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        boolean isDark = (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES);
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            int targetStatusBarColor = isDark ? 
                androidx.core.content.ContextCompat.getColor(this, R.color.dt_header_color) :
                androidx.core.content.ContextCompat.getColor(this, R.color.lt_header_color);
            
            getWindow().setStatusBarColor(targetStatusBarColor);
            
            androidx.core.view.WindowInsetsControllerCompat controller = 
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            if (controller != null) {
                controller.setAppearanceLightStatusBars(!isDark);
            }
        }
        
        if (swipeRefreshLayout != null) {
            int targetBgColor = isDark ? 
                androidx.core.content.ContextCompat.getColor(this, R.color.dt_header_color) :
                androidx.core.content.ContextCompat.getColor(this, R.color.lt_header_color);
            swipeRefreshLayout.setBackgroundColor(targetBgColor);
        }
    }
    
    /**
     * Обновляет токены при запуске приложения
     */
    private void updateTokensOnStartup() {
        executor.execute(() -> {
            try {
                // Проверяем есть ли токен в БД
                boolean hasToken = databaseManager.hasToken();
                Log.d("MainActivity", "Has token in DB: " + hasToken);
                
                if (hasToken) {
                    TokenEntity token = databaseManager.getToken();
                    if (token != null) {
                        Log.d("MainActivity", "Token found in DB: " + token.getAccessToken().substring(0, 20) + "...");
                        
                        // Проверяем не истек ли токен
                        long currentTime = System.currentTimeMillis();
                        long tokenExpiry = token.getTimestamp() + (token.getExpiresIn() * 1000);
                        
                        if (currentTime < tokenExpiry) {
                            Log.d("MainActivity", "Token is still valid, expires at: " + new java.util.Date(tokenExpiry));
                        } else {
                            Log.d("MainActivity", "Token expired, will update from localStorage");
                        }
                    }
                } else {
                    Log.d("MainActivity", "No token in DB, will load from localStorage when available");
                }
                
            } catch (Exception e) {
                Log.e("MainActivity", "Error checking tokens on startup", e);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isUrlInputShowing", isUrlInputShowing);
    }

    private void setupUrlTopBanner() {
        urlTopBanner = findViewById(R.id.urlTopBanner);
        tvCurrentUrlBanner = findViewById(R.id.tvCurrentUrlBanner);
        btnChangeUrlBanner = findViewById(R.id.btnChangeUrlBanner);
        btnCloseUrlBanner = findViewById(R.id.btnCloseUrlBanner);
        btnBannerSelectCis = findViewById(R.id.btnBannerSelectCis);
        btnBannerSelectOther = findViewById(R.id.btnBannerSelectOther);

        if (btnChangeUrlBanner != null) {
            btnChangeUrlBanner.setOnClickListener(v -> {
                hideUrlTopBannerAnimated();
                showUrlInputActivity();
            });
        }

        if (btnCloseUrlBanner != null) {
            btnCloseUrlBanner.setOnClickListener(v -> hideUrlTopBannerAnimated());
        }

        if (btnBannerSelectCis != null) {
            btnBannerSelectCis.setOnClickListener(v -> selectDomainFromBanner("https://v5.animelib.org"));
        }

        if (btnBannerSelectOther != null) {
            btnBannerSelectOther.setOnClickListener(v -> selectDomainFromBanner("https://animelib.org"));
        }
    }

    private void selectDomainFromBanner(String targetUrl) {
        try {
            if (viewModel != null) viewModel.saveSettings(targetUrl);
            if (databaseManager != null) databaseManager.saveSiteUrl(targetUrl);
        } catch (Exception e) {
            Log.e("MainActivity", "Failed to save domain from banner", e);
        }

        updateBannerSelectionUI(targetUrl);
        if (tvCurrentUrlBanner != null) {
            tvCurrentUrlBanner.setText("Текущий адрес: " + targetUrl);
        }
        loadUrl(targetUrl);

        urlBannerHandler.postDelayed(this::hideUrlTopBannerAnimated, 1200);
    }

    private void updateBannerSelectionUI(String currentUrl) {
        if (urlTopBanner == null) return;
        boolean isCis = currentUrl != null && currentUrl.contains("v5.animelib.org");

        View bannerPillCis = urlTopBanner.findViewById(R.id.bannerPillCis);
        View bannerIndicatorOther = urlTopBanner.findViewById(R.id.bannerIndicatorOther);

        if (btnBannerSelectCis != null) {
            btnBannerSelectCis.setBackgroundResource(isCis ? R.drawable.card_resize_selected : R.drawable.card_resize_unselected);
            if (bannerPillCis != null) bannerPillCis.setVisibility(isCis ? View.VISIBLE : View.GONE);
        }

        if (btnBannerSelectOther != null) {
            btnBannerSelectOther.setBackgroundResource(!isCis ? R.drawable.card_resize_selected : R.drawable.card_resize_unselected);
            if (bannerIndicatorOther != null) bannerIndicatorOther.setVisibility(!isCis ? View.GONE : View.VISIBLE);
        }
    }

    private void showUrlTopBannerAnimated(String url) {
        if (urlTopBanner == null) return;

        if (tvCurrentUrlBanner != null && url != null && !url.trim().isEmpty()) {
            tvCurrentUrlBanner.setText("Текущий адрес: " + url);
        }
        updateBannerSelectionUI(url);

        if (urlBannerRunnable != null) {
            urlBannerHandler.removeCallbacks(urlBannerRunnable);
        }

        urlTopBanner.setVisibility(View.VISIBLE);
        urlTopBanner.setAlpha(0f);

        urlTopBanner.post(() -> {
            int height = urlTopBanner.getHeight();
            if (height <= 0) {
                height = (int) (120 * getResources().getDisplayMetrics().density);
            }
            urlTopBanner.setTranslationY(-height - 100f);
            urlTopBanner.animate()
                    .translationY(0f)
                    .alpha(1.0f)
                    .setDuration(450)
                    .setInterpolator(new DecelerateInterpolator(1.5f))
                    .start();
        });

        urlBannerRunnable = this::hideUrlTopBannerAnimated;
        urlBannerHandler.postDelayed(urlBannerRunnable, 7000);
    }

    private void hideUrlTopBannerAnimated() {
        if (urlBannerRunnable != null) {
            urlBannerHandler.removeCallbacks(urlBannerRunnable);
        }

        if (urlTopBanner != null && urlTopBanner.getVisibility() == View.VISIBLE) {
            int height = urlTopBanner.getHeight();
            if (height <= 0) {
                height = (int) (70 * getResources().getDisplayMetrics().density);
            }
            urlTopBanner.animate()
                    .translationY(-height - 100f)
                    .alpha(0f)
                    .setDuration(350)
                    .setInterpolator(new AccelerateInterpolator(1.5f))
                    .withEndAction(() -> urlTopBanner.setVisibility(View.GONE))
                    .start();
        }
    }

    private void checkAndLoadUrl() {
        if (isUrlInputShowing) {
            return;
        }
        viewModel.getSettings().observe(this, settings -> {
            if (settings != null && settings.getSiteUrl() != null && !settings.getSiteUrl().trim().isEmpty()) {
                String siteUrl = settings.getSiteUrl();
                String currentWebUrl = webView != null ? webView.getUrl() : null;
                if (currentWebUrl == null || "about:blank".equals(currentWebUrl) || !extractDomain(currentWebUrl).equalsIgnoreCase(extractDomain(siteUrl))) {
                    loadUrl(siteUrl);
                }
                if (!isUrlBannerShownThisSession) {
                    isUrlBannerShownThisSession = true;
                    showUrlTopBannerAnimated(siteUrl);
                }
            } else if (!isUrlInputShowing) {
                // URL не найден, показываем активность для ввода
                showUrlInputActivity();
            }
        });
    }

    private void loadUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        // Обновляем текущий домен для нового URL
        currentDomain = extractDomain(url);

        if (!isNetworkAvailable()) {
            showCustomErrorPage(url, "Нет подключения к интернету", "Отсутствует подключение к интернету. Проверьте соединение и повторите попытку.");
            return;
        }

        Map<String, String> headers = getStringStringMap();
        webView.loadUrl(url, headers);

        // Check API for toast messages
        checkApiForToast();
    }

    private void showUrlInputActivity() {
        if (isUrlInputShowing) {
            return;
        }
        isUrlInputShowing = true;
        Intent intent = new Intent(this, UrlInputActivity.class);
        startActivityForResult(intent, REQUEST_URL_INPUT);
    }

    private void checkApiForToast() {
        apiService.checkApiForToast(new ApiService.ToastCheckCallback() {
            @Override
            public void onToastReceived(String message, String newUrl) {
                runOnUiThread(() -> {
                    if (newUrl != null && message.contains("Перейти на зеркало")) {
                        // Обновляем URL в базе данных
                        viewModel.updateSettings(newUrl);
                        CustomToast.showSuccess(MainActivity.this, "Зеркало обновлено: " + newUrl);
                        Log.d("ApiCheck", "URL updated to: " + newUrl);
                    } else {
                        Log.d("checkApiForToast", message);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.d("checkApiForToast", error);
            }
        });
    }


    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();

        // Основные настройки JavaScript и DOM
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);

        // Улучшенные настройки кэширования
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Оптимизация viewport и масштабирования
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(false);

        // Улучшение качества текста и изображений
        webSettings.setTextZoom(100);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);

        // Современные настройки безопасности и контента
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(false);
        webSettings.setAllowUniversalAccessFromFileURLs(false);

        // Медиа и геолокация
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setGeolocationEnabled(true);

        // Улучшенный User Agent
        webSettings.setUserAgentString(getRandomUserAgent());

        // Дополнительные настройки производительности
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Настройки для лучшего отображения
        webSettings.setNeedInitialFocus(false);

        // Оптимизация загрузки ресурсов
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setBlockNetworkLoads(false);

        // Дополнительные оптимизации для современных устройств
        webSettings.setSafeBrowsingEnabled(true);

        // Принудительная темная тема для WebView (если поддерживается)
        webSettings.setForceDark(WebSettings.FORCE_DARK_AUTO);

        // Современная оптимизация рендера и производительности
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // Включаем рендеринг вне основного потока для лучшей производительности
        webSettings.setOffscreenPreRaster(true);

        // Отключаем отладку в production для лучшей производительности
        WebView.setWebContentsDebuggingEnabled(false);

        // Дополнительные оптимизации WebView
        webView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(true);
        webView.setVerticalScrollBarEnabled(true);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

        // Оптимизация памяти и производительности
        webView.setInitialScale(0);
        webView.getSettings().setMinimumFontSize(8);
        webView.getSettings().setMinimumLogicalFontSize(8);
        webView.getSettings().setDefaultFontSize(16);
        webView.getSettings().setDefaultFixedFontSize(13);

        // Настройки cookies для лучшей совместимости
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        // Дополнительные настройки для работы с cookies
        cookieManager.flush();

        // Включаем cookies для всех доменов
        CookieManager.setAcceptFileSchemeCookies(true);

        // Синхронизируем и дублируем куки авторизации на все домены сети Lib (MangaLib, RanobeLib, etc.)
        CookieSyncManager.syncAllCookies();

        // Add JavaScript interface for video detection
        JSInjectionsHandler.addJavaScriptInterface(webView);

        // Добавляем JavaScript для работы с cookies
        webView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void setCookie(String name, String value, String domain) {
                CookieManager.getInstance().setCookie(domain, name + "=" + value);
                CookieSyncManager.copyCookieStringToAllDomains(name + "=" + value);
            }

            @android.webkit.JavascriptInterface
            public String getCookie(String name, String domain) {
                String cookies = CookieManager.getInstance().getCookie(domain);
                if (cookies != null) {
                    String[] cookieArray = cookies.split(";");
                    for (String cookie : cookieArray) {
                        String[] parts = cookie.trim().split("=");
                        if (parts.length == 2 && parts[0].equals(name)) {
                            return parts[1];
                        }
                    }
                }
                return null;
            }
        }, "CookieManager");


        Map<String, String> headers = getStringStringMap();
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // Оптимизация скроллинга
        webView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(true);
        webView.setVerticalScrollBarEnabled(true);
        webView.setHorizontalScrollBarEnabled(false);

        webView.setWebViewClient(new WebViewClient() {
            @OptIn(markerClass = UnstableApi.class)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.d("WebView", "=== shouldOverrideUrlLoading ===");
                Log.d("WebView", "URL: " + url);
                Log.d("WebView", "Current Domain: " + currentDomain);

                // Обработка intent:// схем для открытия сторонних приложений
                if (url.startsWith("intent://")) {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        Log.d("WebView", "Parsed intent: " + intent.toString());
                        
                        // Пытаемся запустить приложение напрямую
                        try {
                            startActivity(intent);
                            Log.d("WebView", "Started activity from intent");
                            return true;
                        } catch (android.content.ActivityNotFoundException e) {
                            Log.w("WebView", "App not installed, trying fallback options");
                            
                            // Если приложение не установлено, пытаемся открыть fallback URL
                            String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                            if (fallbackUrl != null) {
                                Log.d("WebView", "Opening fallback URL: " + fallbackUrl);
                                try {
                                    Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                                    marketIntent.setData(android.net.Uri.parse(fallbackUrl));
                                    startActivity(marketIntent);
                                    return true;
                                } catch (Exception ex) {
                                    Log.e("WebView", "Failed to open fallback URL", ex);
                                }
                            }
                            
                            // Если нет fallback URL, пытаемся открыть в Play Market по package name
                            String packageName = intent.getPackage();
                            if (packageName != null) {
                                Log.d("WebView", "Opening Play Market for package: " + packageName);
                                try {
                                    Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                                    marketIntent.setData(android.net.Uri.parse("market://details?id=" + packageName));
                                    startActivity(marketIntent);
                                    return true;
                                } catch (Exception ex) {
                                    Log.e("WebView", "Failed to open Play Market", ex);
                                }
                            }
                            
                            CustomToast.showWarning(MainActivity.this, "Не удалось открыть приложение");
                        }
                        
                    } catch (Exception e) {
                        Log.e("WebView", "Error parsing intent URL: " + url, e);
                        CustomToast.showWarning(MainActivity.this, "Ошибка обработки ссылки");
                    }
                    return true;
                }
                
                // Обработка других специальных схем (tel:, mailto:, etc.)
                if (url.startsWith("tel:") || url.startsWith("mailto:") || 
                    url.startsWith("sms:") || url.startsWith("market:")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(android.net.Uri.parse(url));
                        startActivity(intent);
                        Log.d("WebView", "Opened special scheme: " + url);
                        return true;
                    } catch (Exception e) {
                        Log.e("WebView", "Error handling special scheme: " + url, e);
                        CustomToast.showWarning(MainActivity.this, "Не удалось открыть ссылку");
                        return true;
                    }
                }

                // Проверяем переход на другой домен
                String newDomain = extractDomain(url);
                Log.d("WebView", "New Domain: " + newDomain);

                if (!isNetworkAvailable()) {
                    showCustomErrorPage(url, "Нет подключения к интернету", "Отсутствует подключение к интернету. Проверьте соединение и повторите попытку.");
                    return true;
                }
                
                // Показываем спиннер при смене домена
                if (newDomain != null && currentDomain != null && !currentDomain.equals(newDomain)) {
                    Log.d("WebView", "DOMAIN CHANGE: " + currentDomain + " -> " + newDomain);
                    showDomainChangeSpinner();
                }
                
                // Перехват ссылок на плеер/просмотр
                if (url != null && (url.contains("/watch") || url.contains("episode"))) {
                    String currentUrl = view.getUrl();
                    String resolvedUrl = com.example.animelib.ui.VideoUrlHelper.resolveAnimeUrl(url, currentUrl);
                    Log.d("WebView", "Intercepted watch link in shouldOverrideUrlLoading: " + url + " -> " + resolvedUrl);
                    getAuthAndStartVideoPlayer(resolvedUrl);
                    return true;
                }

                // ВСЕГДА загружаем с headers для корректной работы
                // (авторизация, куки, и т.д. могут зависеть от headers)
                Log.d("WebView", "Loading URL with headers");
                view.loadUrl(url, headers);
                return true;
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, android.webkit.WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);

                if (request != null && request.isForMainFrame()) {
                    String url = request.getUrl() != null ? request.getUrl().toString() : "";
                    int statusCode = errorResponse != null ? errorResponse.getStatusCode() : 0;
                    Log.w("WebView", "HTTP Error " + statusCode + " for URL: " + url);

                    if (statusCode == 404) {
                        runOnUiThread(() -> {
                            if (webView.canGoBack()) {
                                webView.goBack();
                                Log.d("WebView", "Auto-back from 404 error on main frame");
                            } else {
                                showCustomErrorPage(url, "HTTP 404: Страница не найдена");
                            }
                        });
                    } else if (statusCode >= 500) {
                        showCustomErrorPage(url, "Ошибка сервера HTTP " + statusCode);
                    }
                }
            }

            @Override
            public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error) {
                handler.cancel();
                String failingUrl = error != null ? error.getUrl() : null;
                String errorMsg = "Ошибка SSL-соединения: " + (error != null ? error.toString() : "Небезопасный сертификат");
                showCustomErrorPage(failingUrl, errorMsg);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.d("WebView", "=== onPageStarted ===");
                Log.d("WebView", "URL: " + url);

                if (url != null && !"about:blank".equals(url)) {
                    if (!isNetworkAvailable()) {
                        showCustomErrorPage(url, "Нет подключения к интернету", "Отсутствует подключение к интернету. Проверьте соединение и повторите попытку.");
                        return;
                    }
                    if (webErrorLayout != null) {
                        webErrorLayout.setVisibility(View.GONE);
                    }
                    if (webView != null) {
                        webView.setVisibility(View.VISIBLE);
                    }
                }
                
                String newDomain = extractDomain(url);
                Log.d("WebView", "Current domain before update: " + currentDomain);
                Log.d("WebView", "New domain: " + newDomain);

                // Игнорируем встраиваемые плееры и фреймы, чтобы они не перекрывали экран
                boolean isEmbedOrPlayer = url != null && (
                        url.contains("kodik") || url.contains("v2.kodik") ||
                        url.contains("rutube") || url.contains("youtube") ||
                        url.contains("alloha") || url.contains("cache") ||
                        url.contains("/player") || url.contains("iframe")
                );

                if (!isEmbedOrPlayer && url != null && !"about:blank".equals(url)) {
                    showDomainChangeSpinner();
                }

                // Обновляем текущий домен
                currentDomain = newDomain;
                Log.d("WebView", "Current domain updated to: " + currentDomain);
                swipeRefreshLayout.setRefreshing(false);

                // Синхронизируем куки с текущей страницы на остальные домены Lib
                CookieSyncManager.syncFromUrl(url);
                CookieSyncManager.injectAuthLocalStorage(view);

                // Always setup listeners - let JavaScript determine if it's needed
                JSInjectionsHandler.setupPlayerButtonListeners(view);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (url != null && "about:blank".equals(url)) {
                    return;
                }

                hideDomainChangeSpinner();

                JSInjectionsHandler.reinjectDomListeners(view);
                CookieSyncManager.syncFromUrl(url);
                CookieSyncManager.injectAuthLocalStorage(view);

                if (isFirstLoad) {
                    isFirstLoad = false;
                }
                swipeRefreshLayout.setRefreshing(false);
                Log.d("WebView", "Finished loading: " + url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
                // DO NOT call super.onReceivedError to prevent Chromium built-in error HTML page!

                if (request != null && request.isForMainFrame()) {
                    String errorDescription = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && error != null) {
                        CharSequence desc = error.getDescription();
                        if (desc != null && desc.length() > 0) {
                            errorDescription = desc.toString();
                        } else {
                            errorDescription = "Код ошибки: " + error.getErrorCode();
                        }
                    }
                    String failingUrl = request.getUrl() != null ? request.getUrl().toString() : null;
                    showCustomErrorPage(failingUrl, errorDescription);
                }
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                // DO NOT call super.onReceivedError to prevent Chromium built-in error HTML page!
                showCustomErrorPage(failingUrl, description != null ? description : ("Код ошибки: " + errorCode));
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (newProgress >= 100) {
                    hideDomainChangeSpinner();
                }
            }
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }

                customView = view;
                customViewCallback = callback;

                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );
                fullscreenContainer.addView(customView, params);
                fullscreenContainer.setVisibility(View.VISIBLE);

                webView.setVisibility(View.GONE);
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) {
                    return;
                }

                fullscreenContainer.removeView(customView);
                customView = null;

                fullscreenContainer.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                customViewCallback.onCustomViewHidden();
            }
        });
    }


    private boolean isNetworkAvailable() {
        try {
            android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    android.net.Network network = connectivityManager.getActiveNetwork();
                    if (network == null) return false;
                    android.net.NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                    return capabilities != null && (
                            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) ||
                            capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET));
                } else {
                    android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
                }
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error checking network availability", e);
        }
        return false;
    }

    private void reloadWebView() {
        String targetUrl = (lastFailedUrl != null && !lastFailedUrl.isEmpty() && !"about:blank".equals(lastFailedUrl))
                ? lastFailedUrl
                : (webView != null ? webView.getUrl() : null);

        if (!isNetworkAvailable()) {
            showCustomErrorPage(targetUrl, "Нет подключения к интернету", "Отсутствует подключение к интернету. Проверьте соединение и повторите попытку.");
            return;
        }

        if (webErrorLayout != null) webErrorLayout.setVisibility(View.GONE);
        if (webView != null) {
            webView.setVisibility(View.VISIBLE);
            if (spinner != null) spinner.setVisibility(View.VISIBLE);
            if (spinnerBackground != null) spinnerBackground.setVisibility(View.VISIBLE);
            if (lastFailedUrl != null && !lastFailedUrl.isEmpty() && !"about:blank".equals(lastFailedUrl)) {
                webView.loadUrl(lastFailedUrl, getStringStringMap());
            } else if (webView.getUrl() != null && !"about:blank".equals(webView.getUrl())) {
                webView.reload();
            } else if (currentDomain != null) {
                loadUrl("https://" + currentDomain);
            } else {
                checkAndLoadUrl();
            }
        }
    }

    private void showCustomErrorPage(String failingUrl, String title, String errorMsg) {
        runOnUiThread(() -> {
            if (failingUrl != null && !failingUrl.isEmpty() && !"about:blank".equals(failingUrl)) {
                lastFailedUrl = failingUrl;
            } else if (webView != null && webView.getUrl() != null && !"about:blank".equals(webView.getUrl())) {
                lastFailedUrl = webView.getUrl();
            }

            if (spinner != null) spinner.setVisibility(View.GONE);
            if (spinnerBackground != null) spinnerBackground.setVisibility(View.GONE);
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);

            if (webView != null) {
                webView.stopLoading();
                webView.loadUrl("about:blank");
                webView.setVisibility(View.GONE);
            }

            if (tvErrorTitle != null) {
                if (title != null && !title.isEmpty()) {
                    tvErrorTitle.setText(title);
                } else {
                    tvErrorTitle.setText("Что-то пошло не так");
                }
            }

            if (tvErrorMessage != null) {
                StringBuilder sb = new StringBuilder();
                if (errorMsg != null && !errorMsg.trim().isEmpty()) {
                    sb.append(errorMsg);
                } else {
                    sb.append("Произошла ошибка при загрузке.");
                }
                if (lastFailedUrl != null && !lastFailedUrl.isEmpty() && !"about:blank".equals(lastFailedUrl)) {
                    sb.append("\n\n").append(lastFailedUrl);
                }
                tvErrorMessage.setText(sb.toString());
            }

            if (webErrorLayout != null) {
                webErrorLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showCustomErrorPage(String failingUrl, String errorMsg) {
        if (!isNetworkAvailable()) {
            showCustomErrorPage(failingUrl, "Нет подключения к интернету", "Отсутствует подключение к интернету. Проверьте соединение и повторите попытку.");
        } else {
            showCustomErrorPage(failingUrl, "Что-то пошло не так", errorMsg);
        }
    }

    public void disallowInterceptTouch() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.requestDisallowInterceptTouchEvent(true);
        }
    }

    private float startTouchX = 0f;
    private float startTouchY = 0f;

    @SuppressLint("ClickableViewAccessibility")
    private void setupRefreshLayout() {
        swipeRefreshLayout.setOnRefreshListener(() -> reloadWebView());
        swipeRefreshLayout.setOnChildScrollUpCallback((parent, child) -> webView.getScrollY() > 0);
        swipeRefreshLayout.setOverScrollMode(View.OVER_SCROLL_NEVER);

        if (webView != null) {
            webView.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startTouchX = event.getX();
                        startTouchY = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float dx = Math.abs(event.getX() - startTouchX);
                        float dy = Math.abs(event.getY() - startTouchY);
                        if (dx > dy && dx > 10) {
                            swipeRefreshLayout.requestDisallowInterceptTouchEvent(true);
                        }
                        break;
                }
                return false;
            });
        }
    }

    private String getRandomUserAgent() {
        String[] userAgents = {
                // Современные Chrome на Android
                "Mozilla/5.0 (Linux; Android 14; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 13; SM-A546B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36",
                // Samsung Internet
                "Mozilla/5.0 (Linux; Android 14; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/23.0 Chrome/115.0.0.0 Mobile Safari/537.36",
                // Edge Mobile
                "Mozilla/5.0 (Linux; Android 14; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36 EdgA/131.0.0.0"
        };
        return userAgents[new Random().nextInt(userAgents.length)];
    }

    public String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH);
        dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
        return dateFormat.format(new Date());
    }

    @NonNull
    private Map<String, String> getStringStringMap() {
        Map<String, String> headers = new HashMap<>();

        // Основные заголовки
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
        headers.put("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Cache-Control", "max-age=0");
        headers.put("Connection", "keep-alive");

        // Современные заголовки безопасности
        headers.put("Sec-Fetch-Dest", "document");
        headers.put("Sec-Fetch-Mode", "navigate");
        headers.put("Sec-Fetch-Site", "none");
        headers.put("Sec-Fetch-User", "?1");
        headers.put("Upgrade-Insecure-Requests", "1");

        // Client Hints для лучшей оптимизации
        headers.put("Sec-CH-UA", "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"");
        headers.put("Sec-CH-UA-Mobile", "?1");
        headers.put("Sec-CH-UA-Platform", "\"Android\"");
        headers.put("Sec-CH-UA-Platform-Version", "\"14.0.0\"");
        headers.put("Sec-CH-UA-Arch", "\"arm\"");
        headers.put("Sec-CH-UA-Bitness", "\"64\"");
        headers.put("Sec-CH-UA-Model", "\"SM-G998B\"");

        // Динамический User-Agent
        headers.put("User-Agent", getRandomUserAgent());

        // Дата для кэширования
        headers.put("Date", getCurrentDate());

        return headers;
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (customView != null) {
                    // Выход из полноэкранного режима
                    Objects.requireNonNull(webView.getWebChromeClient()).onHideCustomView();
                } else if (webView.canGoBack()) {
                    // Возврат в WebView
                    webView.goBack();
                } else {
                    // Двойное нажатие для выхода из приложения
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastBackPressTime < BACK_PRESS_INTERVAL) {
                        // Второе нажатие - выходим из приложения
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                    } else {
                        // Первое нажатие - показываем сообщение
                        lastBackPressTime = currentTime;
                        CustomToast.showInfo(MainActivity.this, "Нажмите еще раз для выхода");
                    }
                }
            }
        });
    }

    /**
     * Показывает диалог выбора темы
     */
    public void showThemeDialog() {
        themeDialog.show();
    }
    
    /**
     * Показывает фрагмент быстрого поиска с анимацией из центра
     */
    public void showSearchFragment() {
        SearchFragment searchFragment = new SearchFragment();
        
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(
                        R.anim.fragment_scale_fade_in,  // enter
                        R.anim.fragment_scale_fade_out, // exit
                        R.anim.fragment_scale_fade_in,  // popEnter
                        R.anim.fragment_scale_fade_out  // popExit
                )
                .add(R.id.fragment_container, searchFragment)
                .addToBackStack(null)
                .commit();
        
        Log.d("MainActivity", "Search fragment shown with scale-fade animation");
    }

    /**
     * Загружает URL в WebView
     * @param url Относительный или абсолютный URL для загрузки
     */
    public void loadUrlInWebView(String url) {
        if (webView == null) {
            Log.e("MainActivity", "WebView is null, cannot load URL");
            return;
        }

        String fullUrl = url.startsWith("/") ? resolveBaseUrl() + url : url;

        if (!isNetworkAvailable()) {
            showCustomErrorPage(fullUrl, "Нет подключения к интернету", "Отсутствует подключение к интернету. Проверьте соединение и повторите попытку.");
            return;
        }

        if (webErrorLayout != null) webErrorLayout.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);

        Log.d("MainActivity", "Loading URL in WebView: " + url);
        Log.d("MainActivity", "Full URL: " + fullUrl);
        webView.loadUrl(fullUrl, getStringStringMap());
    }

    /**
     * Определяет базовый адрес по текущей странице WebView
     */
    private String resolveBaseUrl() {
        String currentUrl = webView.getUrl();
        if (currentUrl != null && (currentUrl.startsWith("http://") || currentUrl.startsWith("https://"))) {
            try {
                java.net.URL parsed = new java.net.URL(currentUrl);
                String base = parsed.getProtocol() + "://" + parsed.getHost();
                Log.d("MainActivity", "Base URL from WebView: " + base);
                return base;
            } catch (Exception e) {
                Log.w("MainActivity", "Failed to parse WebView URL: " + currentUrl, e);
            }
        }

        String fallback = "https://" + getString(R.string.site_url);
        Log.w("MainActivity", "WebView URL unavailable, using fallback: " + fallback);
        return fallback;
    }
    
    /**
     * Получает значение auth из localStorage WebView и показывает в Toast
     */
    public void getAuthFromLocalStorage() {
        getAuthFromLocalStorage(null);
    }
    
    /**
     * Получает auth токен из localStorage и вызывает callback после сохранения
     * @param callback callback для выполнения после сохранения токена
     */
    public void getAuthFromLocalStorage(Runnable callback) {
        if (webView != null) {
            webView.evaluateJavascript(
                "localStorage.getItem('auth')",
                value -> {
                    runOnUiThread(() -> {
                        // Убираем внешние кавычки и экранируем внутренние кавычки
                        String authValue = value != null ? value.replaceAll("^\"|\"$", "") : "null";
                        
                        // Декодируем экранированные кавычки
                        if (authValue != null && !"null".equals(authValue)) {
                            authValue = authValue.replace("\\\"", "\"");
                        }
                        
                        if ("null".equals(authValue)) {
                            Log.d("MainActivity", "Auth не найден в localStorage");
                            // Вызываем callback даже если токен не найден
                            if (callback != null) {
                                callback.run();
                            }
                        } else {
                            Log.d("MainActivity", "Auth получен из localStorage");
                            
                            // Парсим и сохраняем токены
                            try {
                                // Проверяем что это валидный JSON
                                if (authValue.startsWith("{") && authValue.endsWith("}")) {
                                    CookieSyncManager.saveAuthJson(authValue);
                                    Gson gson = new Gson();
                                    TokenResponse tokenResponse = gson.fromJson(authValue, TokenResponse.class);
                                    if (tokenResponse != null && tokenResponse.getToken() != null) {
                                        saveTokensToDatabase(tokenResponse, authValue, callback);
                                        Log.d("MainActivity", "Successfully parsed and saved tokens");
                                    } else {
                                        Log.w("MainActivity", "Token data not found in auth response");
                                        if (callback != null) {
                                            callback.run();
                                        }
                                    }
                                } else {
                                    Log.w("MainActivity", "Auth value is not valid JSON: " + authValue.substring(0, Math.min(50, authValue.length())));
                                    if (callback != null) {
                                        callback.run();
                                    }
                                }
                            } catch (Exception e) {
                                Log.e("MainActivity", "Error parsing auth JSON: " + e.getMessage());
                                Log.e("MainActivity", "Auth value: " + authValue.substring(0, Math.min(100, authValue.length())));
                                if (callback != null) {
                                    callback.run();
                                }
                            }
                        }
                        Log.d("MainActivity", "Auth from localStorage processed");
                    });
                }
            );
        } else {
            Log.d("MainActivity", "WebView не готов");
            if (callback != null) {
                callback.run();
            }
        }
    }
    
    /**
     * Получает токен из localStorage и затем запускает VideoPlayerActivity
     * @param animeUrl URL страницы аниме для воспроизведения
     */
    @OptIn(markerClass = UnstableApi.class)
    public void getAuthAndStartVideoPlayer(String animeUrl) {
        Log.d("MainActivity", "Getting auth token before starting VideoPlayerActivity");
        String currentUrl = webView != null ? webView.getUrl() : null;
        String resolvedUrl = com.example.animelib.ui.VideoUrlHelper.resolveAnimeUrl(animeUrl, currentUrl);
        getAuthFromLocalStorage(() -> {
            Log.d("MainActivity", "Starting VideoPlayerActivity with resolved URL: " + resolvedUrl);
            VideoPlayerActivity.startFromAnimePage(this, resolvedUrl);
        });
    }
    
    /**
     * Сохраняет токены в базу данных из TokenResponse
     */
    private void saveTokensToDatabase(TokenResponse tokenResponse, Runnable callback) {
        saveTokensToDatabase(tokenResponse, null, callback);
    }

    private void saveTokensToDatabase(TokenResponse tokenResponse, String rawAuthJson, Runnable callback) {
        if (tokenResponse == null || tokenResponse.getToken() == null) {
            if (callback != null) runOnUiThread(callback);
            return;
        }

        TokenResponse.TokenData tokenData = tokenResponse.getToken();
        String userId = null;
        String username = null;

        if (tokenResponse.getAuth() != null) {
            if (tokenResponse.getAuth().getId() != 0) {
                userId = String.valueOf(tokenResponse.getAuth().getId());
            }
            username = tokenResponse.getAuth().getUsername();
        }

        if (userId == null || userId.isEmpty()) {
            userId = ApiService.extractUserIdFromToken(tokenData.getAccessToken());
        }

        final String finalUserId = userId;
        final String finalUsername = username;

        executor.execute(() -> {
            try {
                TokenEntity tokenEntity = new TokenEntity(
                    tokenData.getTokenType(),
                    tokenData.getExpiresIn(),
                    tokenData.getAccessToken(),
                    tokenData.getRefreshToken(),
                    tokenData.getTimestamp(),
                    finalUserId,
                    finalUsername
                );
                if (rawAuthJson != null && !rawAuthJson.trim().isEmpty()) {
                    tokenEntity.setAuthJson(rawAuthJson.trim());
                } else {
                    String saved = CookieSyncManager.getAuthJson(this);
                    if (saved != null) {
                        tokenEntity.setAuthJson(saved);
                    }
                }
                
                databaseManager.saveToken(tokenEntity);
                Log.d("MainActivity", "Tokens saved to database successfully with userId: " + finalUserId + ", username: " + finalUsername);
                
                if (callback != null) {
                    runOnUiThread(callback);
                }
                
            } catch (Exception e) {
                Log.e("MainActivity", "Error saving tokens to database", e);
                if (callback != null) {
                    runOnUiThread(callback);
                }
            }
        });
    }

    /**
     * Очищает токен и ID пользователя в базе данных при нажатии кнопки выхода в WebView
     */
    public void clearAuthToken() {
        Log.d("MainActivity", "Clearing auth token and user ID from database");
        executor.execute(() -> {
            try {
                databaseManager.deleteToken();
                Log.d("MainActivity", "Token and user ID successfully deleted from database");
            } catch (Exception e) {
                Log.e("MainActivity", "Error deleting token from database", e);
            }
        });
    }

    /**
     * Сохраняет OAuth токен, перехваченный из запроса POST https://hapi.hentaicdn.org/api/auth/oauth/token
     */
    public void saveOAuthTokenFromJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            Log.w("MainActivity", "Empty OAuth token JSON received");
            return;
        }

        Log.d("MainActivity", "Processing intercepted OAuth token response");
        executor.execute(() -> {
            try {
                Gson gson = new Gson();
                JsonObject json = gson.fromJson(jsonString, JsonObject.class);
                if (json == null || !json.has("access_token") || json.get("access_token").isJsonNull()) {
                    Log.w("MainActivity", "Invalid OAuth response JSON or missing access_token");
                    return;
                }

                String accessToken = json.get("access_token").getAsString();
                String refreshToken = json.has("refresh_token") && !json.get("refresh_token").isJsonNull()
                        ? json.get("refresh_token").getAsString() : null;
                String tokenType = json.has("token_type") && !json.get("token_type").isJsonNull()
                        ? json.get("token_type").getAsString() : "Bearer";
                long expiresIn = json.has("expires_in") && !json.get("expires_in").isJsonNull()
                        ? json.get("expires_in").getAsLong() : 2678399L;
                long timestamp = System.currentTimeMillis();

                TokenEntity existing = databaseManager.getToken();
                if (existing != null) {
                    existing.setTokenType(tokenType);
                    existing.setExpiresIn(expiresIn);
                    existing.setAccessToken(accessToken);
                    if (refreshToken != null) {
                        existing.setRefreshToken(refreshToken);
                    }
                    existing.setTimestamp(timestamp);
                    if (existing.getUserId() == null || existing.getUserId().isEmpty()) {
                        String extractedId = ApiService.extractUserIdFromToken(accessToken);
                        if (extractedId != null) {
                            existing.setUserId(extractedId);
                        }
                    }
                    databaseManager.saveToken(existing);
                    Log.d("MainActivity", "Updated existing token with new OAuth token. userId: " + existing.getUserId());
                } else {
                    String extractedId = ApiService.extractUserIdFromToken(accessToken);
                    TokenEntity newToken = new TokenEntity(
                            tokenType,
                            expiresIn,
                            accessToken,
                            refreshToken,
                            timestamp,
                            extractedId,
                            null
                    );
                    databaseManager.saveToken(newToken);
                    Log.d("MainActivity", "Saved new OAuth token. userId: " + extractedId);
                }
                CookieSyncManager.syncAllCookies();

            } catch (Exception e) {
                Log.e("MainActivity", "Error saving OAuth token from JSON", e);
            }
        });
    }

    /**
     * Сохраняет данные пользователя (ID и username), перехваченные из запроса GET https://hapi.hentaicdn.org/api/auth/me
     */
    public void saveAuthMeDataFromJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            Log.w("MainActivity", "Empty Auth me JSON received");
            return;
        }

        Log.d("MainActivity", "Processing intercepted Auth me response");
        executor.execute(() -> {
            try {
                Gson gson = new Gson();
                JsonObject json = gson.fromJson(jsonString, JsonObject.class);
                if (json == null || !json.has("data") || json.get("data").isJsonNull()) {
                    Log.w("MainActivity", "Invalid Auth me JSON or missing data object");
                    return;
                }

                JsonObject dataObj = json.getAsJsonObject("data");
                if (!dataObj.has("id") || dataObj.get("id").isJsonNull()) {
                    Log.w("MainActivity", "Auth me data missing id");
                    return;
                }

                long id = dataObj.get("id").getAsLong();
                String userIdStr = String.valueOf(id);
                String username = dataObj.has("username") && !dataObj.get("username").isJsonNull()
                        ? dataObj.get("username").getAsString() : null;

                TokenEntity existing = databaseManager.getToken();
                if (existing != null) {
                    existing.setUserId(userIdStr);
                    if (username != null) {
                        existing.setUsername(username);
                    }
                    databaseManager.saveToken(existing);
                    Log.d("MainActivity", "Updated token entity with user ID from auth/me: " + userIdStr + ", username: " + username);
                } else {
                    TokenEntity newToken = new TokenEntity(
                            "Bearer",
                            2678399L,
                            "",
                            "",
                            System.currentTimeMillis(),
                            userIdStr,
                            username
                    );
                    databaseManager.saveToken(newToken);
                    Log.d("MainActivity", "Saved new token entity with user ID from auth/me: " + userIdStr + ", username: " + username);
                }
                CookieSyncManager.syncAllCookies();

            } catch (Exception e) {
                Log.e("MainActivity", "Error saving Auth me data from JSON", e);
            }
        });
    }

    /**
     * Показывает кастомный диалог выбора для HTML select элементов
     */
    public void showCustomSelectDialog(String dialogDataJson) {
        try {
            // Парсим JSON данные
            Gson gson = new Gson();
            SelectDialogData dialogData = gson.fromJson(dialogDataJson, SelectDialogData.class);

            if (dialogData == null || dialogData.options == null || dialogData.values == null) {
                Log.e("MainActivity", "Invalid dialog data received");
                return;
            }

            // Создаем и показываем диалог
            com.example.animelib.dialogs.CustomSelectDialog dialog =
                new com.example.animelib.dialogs.CustomSelectDialog(this);

            dialog.show(
                dialogData.title,
                dialogData.options,
                dialogData.values,
                dialogData.currentValue,
                (value, text) -> {
                    // Обновляем кнопку в WebView
                    updateSelectButton(dialogData.selectId, value, text);
                }
            );

            Log.d("MainActivity", "Custom select dialog shown for: " + dialogData.selectId);

        } catch (Exception e) {
            Log.e("MainActivity", "Error showing custom select dialog", e);
        }
    }

    /**
     * Обновляет кнопку select в WebView после выбора опции
     */
    public void updateSelectButton(String selectId, String selectedValue, String selectedText) {
        if (webView != null) {
            String jsCode = String.format(
                "if (window.customSelectHandler) { " +
                "  window.customSelectHandler.updateButtonAfterSelection('%s', '%s', '%s'); " +
                "}",
                selectId, selectedValue, selectedText
            );

            webView.evaluateJavascript(jsCode, result -> {
                Log.d("MainActivity", "Select button updated: " + selectId + " = " + selectedText);
            });
        }
    }

    /**
     * Класс для парсинга данных диалога select
     */
    private static class SelectDialogData {
        public String title;
        public java.util.List<String> options;
        public java.util.List<String> values;
        public String currentValue;
        public String selectId; // ID кнопки
    }

    /**
     * Извлекает домен из URL
     */
    private String extractDomain(String url) {
        try {
            if (url == null || url.isEmpty()) {
                return null;
            }

            // Добавляем протокол если его нет
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            java.net.URL urlObj = new java.net.URL(url);
            return urlObj.getHost();
        } catch (Exception e) {
            Log.w("MainActivity", "Failed to extract domain from URL: " + url, e);
            return null;
        }
    }

    /**
     * Показывает спиннер при смене домена или загрузке страницы
     */
    public void showDomainChangeSpinner() {
        runOnUiThread(() -> {
            Log.d("MainActivity", "Showing loading spinner");
            if (spinnerTimeoutHandler != null) {
                spinnerTimeoutHandler.removeCallbacks(hideSpinnerRunnable);
            }
            if (spinnerBackground != null) {
                spinnerBackground.animate().cancel();
                spinnerBackground.setVisibility(View.VISIBLE);
                spinnerBackground.setAlpha(1f);
            }
            if (spinner != null) {
                spinner.animate().cancel();
                spinner.setVisibility(View.VISIBLE);
                spinner.setAlpha(1f);
                spinner.setScaleX(1f);
                spinner.setScaleY(1f);
            }
            // Timeout fallback to auto-hide after 10 seconds if page gets stuck
            if (spinnerTimeoutHandler != null) {
                spinnerTimeoutHandler.postDelayed(hideSpinnerRunnable, 10000);
            }
        });
    }

    /**
     * Скрывает спиннер при смене домена или завершении загрузки
     */
    public void hideDomainChangeSpinner() {
        runOnUiThread(() -> {
            Log.d("MainActivity", "Hiding loading spinner");
            if (spinnerTimeoutHandler != null) {
                spinnerTimeoutHandler.removeCallbacks(hideSpinnerRunnable);
            }
            if (spinner != null) {
                spinner.animate().cancel();
                spinner.animate()
                        .alpha(0f)
                        .scaleX(0.8f)
                        .scaleY(0.8f)
                        .setDuration(200)
                        .setInterpolator(new android.view.animation.AccelerateInterpolator())
                        .withEndAction(() -> {
                            if (spinner != null) spinner.setVisibility(View.GONE);
                        })
                        .start();
            }
            if (spinnerBackground != null) {
                spinnerBackground.animate().cancel();
                spinnerBackground.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .setInterpolator(new android.view.animation.AccelerateInterpolator())
                        .withEndAction(() -> {
                            if (spinnerBackground != null) spinnerBackground.setVisibility(View.GONE);
                        })
                        .start();
            }
        });
    }

//    /**
//     * Показывает popup с закладками при запуске приложения (один раз за сессию)
//     */
//    private void showBookmarksPopupIfNeeded() {
//            apiService.fetchBookmarksList(new ApiService.BookmarksListCallback() {
//                @Override
//                public void onBookmarksReceived(BookmarksListResponse response) {
//                    runOnUiThread(() -> {
//                        currentBookmarksDialog = new BookmarksPopupDialog(MainActivity.this, response, 10000);
//                        currentBookmarksDialog.show();
//                        currentBookmarksDialog.setOnDismissListener(dialog -> currentBookmarksDialog = null);
//                    });
//                }
//
//                @Override
//                public void onError(String error) {
//                    Log.e("MainActivity", "Failed to load bookmarks: " + error);
//                }
//            });
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_URL_INPUT) {
            isUrlInputShowing = false;
            if (resultCode == RESULT_OK && data != null) {
                String siteUrl = data.getStringExtra("site_url");
                if (siteUrl != null && !siteUrl.isEmpty()) {
                    // Сохраняем URL в ViewModel
                    viewModel.saveSettings(siteUrl);
                    // Загружаем URL
                    loadUrl(siteUrl);
                    showUrlTopBannerAnimated(siteUrl);
                    Log.d("MainActivity", "URL received from UrlInputActivity: " + siteUrl);
                }
            } else {
                if (viewModel.getSettings().getValue() == null || viewModel.getSettings().getValue().getSiteUrl() == null) {
                    // Пользователь отменил ввод, показываем сообщение
                    CustomToast.showWarning(this, "Для работы приложения необходимо указать URL сайта");
                    // Показываем активность снова
                    showUrlInputActivity();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (urlBannerHandler != null && urlBannerRunnable != null) {
            urlBannerHandler.removeCallbacks(urlBannerRunnable);
        }
        if (webView != null) {
            webView.destroy();
        }
        if (apiService != null) {
            apiService.shutdown();
        }

        super.onDestroy();
    }
}
