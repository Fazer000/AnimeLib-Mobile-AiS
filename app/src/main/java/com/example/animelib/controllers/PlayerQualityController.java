package com.example.animelib.controllers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.animelib.api.ApiService;
import com.example.animelib.managers.PlayersManager;
import com.example.animelib.models.EpisodeResponse;
import com.example.animelib.models.KodikResponse;
import com.example.animelib.ui.VideoUrlHelper;

import java.util.List;

/**
 * Контроллер выборов и смены качества видеопотока (включая HLS Kodik).
 */
public class PlayerQualityController {

    private static final String TAG = "PlayerQualityCtrl";

    private String preferredQuality = "1080p";
    private KodikResponse currentKodikResponse;

    public interface QualityCallback {
        void onQualityChanged(String newQuality, String newVideoUrl, boolean isHls);
        void onError(String title, String message, Runnable retryAction);
    }

    private QualityCallback callback;

    public PlayerQualityController() {}

    public void setCallback(QualityCallback callback) {
        this.callback = callback;
    }

    public String getPreferredQuality() {
        return preferredQuality;
    }

    public void setPreferredQuality(String quality) {
        this.preferredQuality = quality;
    }

    public KodikResponse getCurrentKodikResponse() {
        return currentKodikResponse;
    }

    public void setCurrentKodikResponse(KodikResponse response) {
        this.currentKodikResponse = response;
    }

    @Nullable
    public String resolveDownloadUrl(PlayersManager playersManager, String currentVideoDomain, String quality) {
        if (playersManager == null) return null;
        EpisodeResponse.PlayerData playerData = playersManager.getCurrentPlayerData();
        if (playerData == null || playerData.getVideo() == null || playerData.getVideo().getQuality() == null) {
            return null;
        }

        try {
            int target = Integer.parseInt(quality.replace("p", ""));
            for (EpisodeResponse.QualityData data : playerData.getVideo().getQuality()) {
                if (data.getQuality() == target) {
                    String domain = (playerData.getVideoDomain() != null && !playerData.getVideoDomain().isEmpty())
                            ? playerData.getVideoDomain() : currentVideoDomain;
                    return VideoUrlHelper.toAbsoluteVideoUrl(data.getHref(), domain);
                }
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid quality format: " + quality);
        }
        return null;
    }

    @Nullable
    public String resolveKodikHlsUrl(KodikResponse response, String targetQuality) {
        if (response == null || response.getData() == null) return null;

        String qualityKey = targetQuality != null ? targetQuality.replace("p", "") : null;
        if (qualityKey != null && response.getData().containsKey(qualityKey) &&
                response.getData().get(qualityKey).length > 0) {
            String url = response.getData().get(qualityKey)[0].getSrc();
            if (url != null && !url.startsWith("http")) {
                url = "https:" + url;
            }
            return url;
        }

        // Fallbacks
        for (String key : new String[]{"720", "480", "360"}) {
            if (response.getData().containsKey(key) && response.getData().get(key).length > 0) {
                String url = response.getData().get(key)[0].getSrc();
                if (url != null && !url.startsWith("http")) {
                    url = "https:" + url;
                }
                return url;
            }
        }
        return null;
    }

    public void switchQuality(PlayersManager playersManager, String currentVideoDomain, String newQuality) {
        this.preferredQuality = newQuality;
        if (playersManager == null) return;

        EpisodeResponse.PlayerData playerData = playersManager.getCurrentPlayerData();
        if (playerData == null) return;

        if ("kodik".equalsIgnoreCase(playerData.getPlayer())) {
            if (currentKodikResponse != null) {
                String hlsUrl = resolveKodikHlsUrl(currentKodikResponse, newQuality);
                if (hlsUrl != null && callback != null) {
                    callback.onQualityChanged(newQuality, hlsUrl, true);
                } else if (callback != null) {
                    callback.onError("Ошибка смены качества", "Не удалось сформировать HLS ссылку для качества " + newQuality, null);
                }
            }
        } else {
            String videoUrl = resolveDownloadUrl(playersManager, currentVideoDomain, newQuality);
            if (videoUrl != null && callback != null) {
                callback.onQualityChanged(newQuality, videoUrl, false);
            } else if (callback != null) {
                callback.onError("Ошибка смены качества", "Ссылка для качества " + newQuality + " не найдена", null);
            }
        }
    }
}
