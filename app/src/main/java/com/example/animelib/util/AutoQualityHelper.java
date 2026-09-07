package com.example.animelib.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AutoQualityHelper {
    private static final String TAG = "AutoQualityHelper";

    /**
     * Проверяет, выбрано ли авто-качество.
     */
    public static boolean isAutoQuality(String quality) {
        if (quality == null || quality.trim().isEmpty()) return true; // Default to auto
        String q = quality.toLowerCase().trim();
        return q.equals("авто") || q.equals("auto") || q.startsWith("авто ") || q.startsWith("auto ");
    }

    /**
     * Опредяляет оптимальное разрешение в p (2160, 1080, 720, 480, 360) на основе сети.
     */
    public static int getOptimalQualityResolution(Context context) {
        if (context == null) return 720;

        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return 720;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.net.Network activeNetwork = cm.getActiveNetwork();
                if (activeNetwork != null) {
                    NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
                    if (caps != null) {
                        // Unmetered Wi-Fi or Ethernet -> Maximum quality (1080p+)
                        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                            Log.d(TAG, "Network: Wi-Fi / Ethernet -> 1080p+");
                            return 1080;
                        }

                        // Mobile network
                        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                            int downstreamKbps = caps.getLinkDownstreamBandwidthKbps();
                            Log.d(TAG, "Network: Mobile cellular. Downstream: " + downstreamKbps + " Kbps");

                            if (downstreamKbps >= 15000) { // High speed LTE / 5G
                                return 1080;
                            } else if (downstreamKbps >= 4000) { // Standard 4G / LTE
                                return 720;
                            } else if (downstreamKbps >= 1200) { // 3G
                                return 480;
                            } else { // 2G / Slow connection
                                return 360;
                            }
                        }
                    }
                }
            } else {
                android.net.NetworkInfo info = cm.getActiveNetworkInfo();
                if (info != null && info.isConnected()) {
                    if (info.getType() == ConnectivityManager.TYPE_WIFI || info.getType() == ConnectivityManager.TYPE_ETHERNET) {
                        return 1080;
                    } else if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                        return 720;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resolving optimal quality resolution: " + e.getMessage());
        }

        return 720;
    }

    /**
     * Подбирает наиболее подходящее доступное качество из списка.
     */
    public static String resolveBestQuality(Context context, List<String> availableQualities, String preferredQuality) {
        if (availableQualities == null || availableQualities.isEmpty()) {
            return "720p";
        }

        // Если выбрано конкретное качество (не Авто) и оно доступно, используем его
        if (!isAutoQuality(preferredQuality)) {
            for (String q : availableQualities) {
                if (q != null && q.equalsIgnoreCase(preferredQuality)) {
                    return q;
                }
            }
        }

        int targetRes = getOptimalQualityResolution(context);
        Log.d(TAG, "Auto Quality target resolution: " + targetRes + "p");

        List<Integer> parsedResolutions = new ArrayList<>();
        String bestMatch = null;
        int minDiff = Integer.MAX_VALUE;

        for (String qStr : availableQualities) {
            if (qStr == null || isAutoQuality(qStr) || qStr.toLowerCase().contains("скачанный")) continue;

            String digits = qStr.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) continue;

            try {
                int res = Integer.parseInt(digits);
                // Находим наиближайшее качество
                if (res <= targetRes) {
                    if (bestMatch == null || res > extractResolution(bestMatch)) {
                        bestMatch = qStr;
                    }
                } else if (bestMatch == null) {
                    bestMatch = qStr; // Запасной вариант если все качества выше targetRes
                }
            } catch (Exception ignored) {}
        }

        if (bestMatch != null) {
            Log.d(TAG, "Auto Quality resolved: " + bestMatch + " (Target: " + targetRes + "p)");
            return bestMatch;
        }

        // Запасной выбор первого небезопасного
        for (String q : availableQualities) {
            if (!isAutoQuality(q) && !q.toLowerCase().contains("скачанный")) return q;
        }

        return availableQualities.get(0);
    }

    private static int extractResolution(String qualityStr) {
        if (qualityStr == null) return 0;
        String digits = qualityStr.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0;
        try {
            return Integer.parseInt(digits);
        } catch (Exception e) {
            return 0;
        }
    }
}
