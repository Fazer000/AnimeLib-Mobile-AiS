package com.example.animelib.util;

public class SiteUtils {

    public static boolean isOtherRegion(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        String lower = url.toLowerCase();
        if (lower.contains("v5.")) {
            return false;
        }
        if (lower.contains(".me")) {
            return false;
        }
        return lower.contains(".org");
    }

    public static String getSiteKey(String url) {
        if (url == null) return "animelib";
        String lower = url.toLowerCase();
        if (lower.contains("mangalib")) return "mangalib";
        if (lower.contains("ranobelib")) return "ranobelib";
        if (lower.contains("hentailib")) return "hentailib";
        if (lower.contains("slashlib")) return "slashlib";
        return "animelib";
    }

    public static String getSiteName(String siteKey) {
        if (siteKey == null) return "AnimeLib";
        switch (siteKey.toLowerCase()) {
            case "mangalib": return "MangaLib";
            case "ranobelib": return "RanobeLib";
            case "hentailib": return "HentaiLib";
            case "slashlib": return "SlashLib";
            case "animelib":
            default: return "AnimeLib";
        }
    }

    public static String getUrlForSiteAndRegion(String siteKey, boolean isOtherRegion) {
        if (siteKey == null) siteKey = "animelib";
        switch (siteKey.toLowerCase()) {
            case "mangalib":
                return isOtherRegion ? "https://mangalib.org" : "https://mangalib.me";
            case "ranobelib":
                return "https://ranobelib.me";
            case "hentailib":
                return isOtherRegion ? "https://hentailib.org" : "https://hentailib.me";
            case "slashlib":
                return isOtherRegion ? "https://slashlib.org" : "https://slashlib.me";
            case "animelib":
            default:
                return isOtherRegion ? "https://animelib.org" : "https://v5.animelib.org";
        }
    }

    public static String getDomainHost(String url) {
        if (url == null) return "";
        String clean = url.replace("https://", "").replace("http://", "");
        int slashIdx = clean.indexOf('/');
        if (slashIdx != -1) {
            clean = clean.substring(0, slashIdx);
        }
        return clean;
    }
}
