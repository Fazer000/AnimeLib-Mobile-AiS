package com.example.animelib.util;

import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Менеджер синхронизации и дублирования куки и localStorage объекта auth
 * между всеми доменам сети Lib (AnimeLib, MangaLib, RanobeLib, HentaiLib, SlashLib, Lib.Social).
 * Выполняется асинхронно, чтобы исключить подлагивания UI при переходах между страницами.
 */
public class CookieSyncManager {
    private static final String TAG = "CookieSyncManager";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static volatile String savedAuthJson = null;
    private static volatile String lastSyncedCookieHash = "";
    private static volatile long lastSyncTimestamp = 0;

    // Все ключевые URL сети Lib
    public static final List<String> ALL_LIB_URLS = Arrays.asList(
        "https://animelib.me",
        "https://animelib.org",
        "https://v5.animelib.org",
        "https://v2.animelib.me",
        "https://mangalib.me",
        "https://mangalib.org",
        "https://v5.mangalib.org",
        "https://v2.mangalib.me",
        "https://ranobelib.me",
        "https://ranobelib.org",
        "https://v5.ranobelib.org",
        "https://v2.ranobelib.me",
        "https://hentailib.me",
        "https://hentailib.org",
        "https://v5.hentailib.org",
        "https://slashlib.me",
        "https://slashlib.org",
        "https://v5.slashlib.org",
        "https://lib.social"
    );

    // Корневые домены для установки Domain куков
    public static final List<String> ALL_LIB_DOMAINS = Arrays.asList(
        ".animelib.org",
        ".animelib.me",
        ".mangalib.org",
        ".mangalib.me",
        ".ranobelib.org",
        ".ranobelib.me",
        ".hentailib.org",
        ".hentailib.me",
        ".slashlib.org",
        ".slashlib.me",
        ".lib.social"
    );

    /**
     * Сохраняет JSON объект auth из localStorage
     */
    public static void saveAuthJson(String authJson) {
        if (authJson != null && !authJson.trim().isEmpty() && !"null".equals(authJson) && !"undefined".equals(authJson)) {
            savedAuthJson = authJson.trim();
            Log.d(TAG, "Saved auth object from localStorage for cross-domain sync");
        }
    }

    /**
     * Очищает сохранённый auth объект (при выходе)
     */
    public static void clearAuthJson() {
        savedAuthJson = null;
        Log.d(TAG, "Cleared saved auth object");
    }

    public static String getSavedAuthJson() {
        return savedAuthJson;
    }

    /**
     * Внедряет объект auth из localStorage на загруженную страницу WebView
     */
    public static void injectAuthLocalStorage(WebView webView) {
        if (webView == null) return;
        final String authStr = savedAuthJson;
        if (authStr == null || authStr.isEmpty() || "null".equals(authStr)) return;

        webView.post(() -> {
            try {
                // Подготавливаем безопасный скрипт для записи localStorage.auth
                String cleanAuth = authStr;
                // Если строка уже в кавычках или объекте, форматируем правильно
                String jsScript =
                    "(function() {" +
                    "  try {" +
                    "    var authVal = " + cleanAuth + ";" +
                    "    var authStr = (typeof authVal === 'string') ? authVal : JSON.stringify(authVal);" +
                    "    var current = localStorage.getItem('auth');" +
                    "    if (authStr && current !== authStr) {" +
                    "      localStorage.setItem('auth', authStr);" +
                    "      console.log('[CookieSyncManager] LocalStorage auth synced to ' + window.location.hostname);" +
                    "    }" +
                    "  } catch(e) {" +
                    "    console.error('[CookieSyncManager] Auth injection error:', e);" +
                    "  }" +
                    "})();";

                webView.evaluateJavascript(jsScript, null);
            } catch (Exception e) {
                Log.e(TAG, "Failed to inject auth into localStorage", e);
            }
        });
    }

    /**
     * Синхронизирует все куки с основных доменов сети Lib на все остальные (асинхронно)
     */
    public static void syncAllCookies() {
        syncFromUrl(null);
    }

    /**
     * Считывает куки с sourceUrl и асинхронно копирует на остальные сайты Lib
     */
    public static void syncFromUrl(final String sourceUrl) {
        long now = System.currentTimeMillis();
        // Троттлинг: если прошло менее 1.5 сек с последней синхронизации и нет специфичного URL, прогоняем асинхронно
        if (sourceUrl == null && (now - lastSyncTimestamp < 1500)) {
            return;
        }

        executor.execute(() -> {
            try {
                CookieManager cookieManager = CookieManager.getInstance();
                if (cookieManager == null) return;

                cookieManager.setAcceptCookie(true);

                Set<String> sourceUrls = new HashSet<>();
                if (sourceUrl != null && !sourceUrl.trim().isEmpty() && sourceUrl.startsWith("http")) {
                    sourceUrls.add(sourceUrl);
                }
                sourceUrls.add("https://v5.animelib.org");
                sourceUrls.add("https://animelib.org");
                sourceUrls.add("https://animelib.me");
                sourceUrls.add("https://mangalib.me");
                sourceUrls.add("https://ranobelib.me");
                sourceUrls.add("https://lib.social");

                StringBuilder combinedCookies = new StringBuilder();
                for (String src : sourceUrls) {
                    String cookies = cookieManager.getCookie(src);
                    if (cookies != null && !cookies.trim().isEmpty()) {
                        combinedCookies.append(cookies).append("; ");
                    }
                }

                String fullCookieStr = combinedCookies.toString().trim();
                if (!fullCookieStr.isEmpty()) {
                    copyCookieStringToAllDomainsInternal(cookieManager, fullCookieStr);
                }

                lastSyncTimestamp = System.currentTimeMillis();
            } catch (Exception e) {
                Log.e(TAG, "Error in async syncFromUrl", e);
            }
        });
    }

    /**
     * Асинхронно копирует строку куки на все домены сети Lib
     */
    public static void copyCookieStringToAllDomains(final String cookieHeader) {
        if (cookieHeader == null || cookieHeader.trim().isEmpty()) return;

        executor.execute(() -> {
            try {
                CookieManager cookieManager = CookieManager.getInstance();
                if (cookieManager == null) return;
                cookieManager.setAcceptCookie(true);

                copyCookieStringToAllDomainsInternal(cookieManager, cookieHeader);
            } catch (Exception e) {
                Log.e(TAG, "Error copying cookies to all domains", e);
            }
        });
    }

    private static void copyCookieStringToAllDomainsInternal(CookieManager cookieManager, String cookieHeader) {
        String hash = String.valueOf(cookieHeader.hashCode());
        if (hash.equals(lastSyncedCookieHash)) {
            // Уменьшаем избыточную перезапись, если куки не изменялись
            return;
        }

        String[] pairs = cookieHeader.split(";");
        boolean updated = false;

        for (String pair : pairs) {
            String trimmed = pair.trim();
            if (trimmed.isEmpty()) continue;

            int eqIdx = trimmed.indexOf('=');
            if (eqIdx <= 0) continue;

            String key = trimmed.substring(0, eqIdx).trim();
            String val = trimmed.substring(eqIdx + 1).trim();

            if (key.isEmpty()) continue;

            // 1. Копируем куки на основные URL
            String cookieValue = key + "=" + val + "; Path=/; SameSite=Lax";
            for (String targetUrl : ALL_LIB_URLS) {
                cookieManager.setCookie(targetUrl, cookieValue);
            }

            // 2. Копируем с директивой Domain на корневые домены
            for (String domain : ALL_LIB_DOMAINS) {
                String domainCookieValue = key + "=" + val + "; Path=/; Domain=" + domain + "; SameSite=Lax";
                String baseUrl = "https://" + (domain.startsWith(".") ? domain.substring(1) : domain);
                cookieManager.setCookie(baseUrl, domainCookieValue);
            }
            updated = true;
        }

        if (updated) {
            cookieManager.flush();
            lastSyncedCookieHash = hash;
            Log.d(TAG, "Successfully synced cookies across all Lib domains off UI thread.");
        }
    }
}
