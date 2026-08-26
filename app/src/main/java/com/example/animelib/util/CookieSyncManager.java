package com.example.animelib.util;

import android.util.Log;
import android.webkit.CookieManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Менеджер синхронизации и дублирования куки авторизации с AnimeLib на остальные сайты сети Lib (MangaLib, RanobeLib, HentaiLib, SlashLib, Lib.Social)
 */
public class CookieSyncManager {
    private static final String TAG = "CookieSyncManager";

    // Все URL доменов сети Lib для дублирования куки
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

    // Все корневые домены для директивы Domain
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
     * Синхронизирует все куки с анимелиба (и имеющихся доменов) на MangaLib, RanobeLib и остальные сайты
     */
    public static void syncAllCookies() {
        syncFromUrl(null);
    }

    /**
     * Считывает куки с исходного URL (или с доменов AnimeLib) и копирует их на все остальные домены
     * @param sourceUrl исходный URL страницы (например, текущий URL в WebView)
     */
    public static void syncFromUrl(String sourceUrl) {
        try {
            CookieManager cookieManager = CookieManager.getInstance();
            if (cookieManager == null) return;

            cookieManager.setAcceptCookie(true);

            Set<String> sourceUrls = new HashSet<>();
            if (sourceUrl != null && !sourceUrl.trim().isEmpty() && sourceUrl.startsWith("http")) {
                sourceUrls.add(sourceUrl);
            }
            // Всегда проверяем основные домены AnimeLib и Lib.Social
            sourceUrls.add("https://v5.animelib.org");
            sourceUrls.add("https://animelib.org");
            sourceUrls.add("https://animelib.me");
            sourceUrls.add("https://lib.social");

            for (String src : sourceUrls) {
                String cookies = cookieManager.getCookie(src);
                if (cookies != null && !cookies.trim().isEmpty()) {
                    copyCookieStringToAllDomains(cookies);
                }
            }

            cookieManager.flush();
        } catch (Exception e) {
            Log.e(TAG, "Error in syncFromUrl", e);
        }
    }

    /**
     * Копирует строку куки (например, "token=xyz; remember_web=123") на все домены сети Lib
     * @param cookieHeader строка куков в формате "key1=val1; key2=val2"
     */
    public static void copyCookieStringToAllDomains(String cookieHeader) {
        if (cookieHeader == null || cookieHeader.trim().isEmpty()) return;

        try {
            CookieManager cookieManager = CookieManager.getInstance();
            if (cookieManager == null) return;

            cookieManager.setAcceptCookie(true);

            String[] pairs = cookieHeader.split(";");
            for (String pair : pairs) {
                String trimmed = pair.trim();
                if (trimmed.isEmpty()) continue;

                int eqIdx = trimmed.indexOf('=');
                if (eqIdx <= 0) continue;

                String key = trimmed.substring(0, eqIdx).trim();
                String val = trimmed.substring(eqIdx + 1).trim();

                if (key.isEmpty()) continue;

                // 1. Устанавливаем куки напрямую для каждого URL
                String cookieValue = key + "=" + val + "; Path=/; SameSite=Lax";
                for (String targetUrl : ALL_LIB_URLS) {
                    cookieManager.setCookie(targetUrl, cookieValue);
                }

                // 2. Устанавливаем куки с явным указанием корневого домена
                for (String domain : ALL_LIB_DOMAINS) {
                    String domainCookieValue = key + "=" + val + "; Path=/; Domain=" + domain + "; SameSite=Lax";
                    String baseUrl = "https://" + (domain.startsWith(".") ? domain.substring(1) : domain);
                    cookieManager.setCookie(baseUrl, domainCookieValue);
                }
            }

            cookieManager.flush();
            Log.d(TAG, "Duplicated cookies across all Lib domains (mangalib, ranobelib, etc.)");
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy cookie string to all domains", e);
        }
    }
}
