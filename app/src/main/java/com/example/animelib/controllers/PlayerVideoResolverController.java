package com.example.animelib.controllers;

import android.net.Uri;
import android.util.Log;
import android.view.View;

import androidx.media3.exoplayer.ExoPlayer;

import com.example.animelib.api.ApiService;
import com.example.animelib.data.entity.DownloadedEpisodeEntity;
import com.example.animelib.managers.PlayersManager;
import com.example.animelib.managers.TimecodeManager;
import com.example.animelib.models.EpisodeResponse;
import com.example.animelib.models.KodikResponse;
import com.example.animelib.ui.VideoUrlHelper;

import java.io.File;

/**
 * Контроллер разрешения видеоисточников (AnimeLib, Kodik HLS, локальные скачанные файлы).
 */
public class PlayerVideoResolverController {

    private static final String TAG = "PlayerVideoResolver";

    public interface ResolverProvider {
        boolean isDownloadedQuality(String quality);
        DownloadedEpisodeEntity getDownloadedEpisodeForActive();
        void setCurrentVideoUrl(String url);
        String getPreferredQuality();
        void setPreferredQuality(String quality);
        boolean isEnable4K();
        String getCurrentVideoDomain();
        void showLoading(String message);
        void hideLoading();
        void showVideoErrorDialog(String title, String message, Runnable retryAction);
        void initializePlayer();
        ExoPlayer getPlayer();
        View getMenuLoadingOverlay();
        void setVideoLoading(boolean loading);
        void setHasRenderedFirstFrame(boolean rendered);
        void updatePlayPauseAndLoadingState(boolean animate);
        void setCurrentKodikResponse(KodikResponse response);
        void initializeHlsPlayer(String hlsUrl);
        void safeRunOnUiThread(Runnable runnable);
        void onPlayerSelected(EpisodeResponse.PlayerData playerData);
    }

    private final ApiService apiService;
    private final TimecodeManager timecodeManager;
    private final PlayersManager playersManager;
    private final ResolverProvider provider;

    public PlayerVideoResolverController(ApiService apiService, TimecodeManager timecodeManager, PlayersManager playersManager, ResolverProvider provider) {
        this.apiService = apiService;
        this.timecodeManager = timecodeManager;
        this.playersManager = playersManager;
        this.provider = provider;
    }

    public void handleAnimelibPlayer(EpisodeResponse.PlayerData playerData, long seekToPosition) {
        if (provider == null) return;
        Log.d(TAG, "Handling Animelib player");
        provider.setVideoLoading(true);
        provider.setHasRenderedFirstFrame(false);
        provider.updatePlayPauseAndLoadingState(true);

        String preferredQuality = provider.getPreferredQuality();

        if (provider.isDownloadedQuality(preferredQuality)) {
            DownloadedEpisodeEntity downloadedEp = provider.getDownloadedEpisodeForActive();
            if (downloadedEp != null && downloadedEp.getLocalFilePath() != null) {
                File file = new File(downloadedEp.getLocalFilePath());
                if (file.exists() && file.length() > 0) {
                    provider.setCurrentVideoUrl(Uri.fromFile(file).toString());
                    if (timecodeManager != null) timecodeManager.setTimecodes(playerData);
                    provider.initializePlayer();
                    ExoPlayer player = provider.getPlayer();
                    if (seekToPosition > 0 && player != null) {
                        player.seekTo(seekToPosition);
                    }
                    Log.d(TAG, "Playing downloaded local file: " + file.getAbsolutePath());
                    return;
                }
            }
        }

        if (playerData.getVideo() != null && playerData.getVideo().getQuality() != null && !playerData.getVideo().getQuality().isEmpty()) {
            EpisodeResponse.QualityData selectedQuality = null;
            String preferredQualityValue = preferredQuality != null ? preferredQuality.replace("p", "") : null;

            if (preferredQualityValue != null) {
                try {
                    int preferredQualityInt = Integer.parseInt(preferredQualityValue);
                    for (EpisodeResponse.QualityData quality : playerData.getVideo().getQuality()) {
                        if (quality.getQuality() == preferredQualityInt) {
                            if (preferredQualityInt == 2160 && !provider.isEnable4K()) {
                                continue;
                            }
                            selectedQuality = quality;
                            Log.d(TAG, "Using preferred quality: " + preferredQualityInt + "p");
                            break;
                        }
                    }
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid preferred quality format: " + preferredQuality);
                }
            }

            if (selectedQuality == null) {
                Log.d(TAG, "Available qualities:");
                for (EpisodeResponse.QualityData quality : playerData.getVideo().getQuality()) {
                    String q = String.valueOf(quality.getQuality());
                    Log.d(TAG, "  - " + quality.getQuality() + "p: " + quality.getHref());
                    if (provider.isEnable4K() || !"2160".equals(q)) {
                        if (selectedQuality == null || quality.getQuality() > selectedQuality.getQuality()) {
                            selectedQuality = quality;
                        }
                    }
                }
                if (preferredQuality == null && selectedQuality != null) {
                    String quality = String.valueOf(selectedQuality.getQuality());
                    provider.setPreferredQuality(quality + "p");
                }
            }

            if (selectedQuality == null) {
                Log.e(TAG, "No suitable quality found");
                provider.showVideoErrorDialog("Ошибка плеера AnimeLib", "Нет подходящего качества видео для этой озвучки.", () -> {
                    provider.onPlayerSelected(playerData);
                });
                return;
            }

            String videoUrl = selectedQuality.getHref();
            Log.d(TAG, "Selected quality: " + selectedQuality.getQuality() + "p, URL: " + videoUrl);

            videoUrl = VideoUrlHelper.toAbsoluteVideoUrl(videoUrl, provider.getCurrentVideoDomain());

            Log.d(TAG, "Final video URL: " + videoUrl);
            provider.setCurrentVideoUrl(videoUrl);
            
            if (timecodeManager != null) timecodeManager.setTimecodes(playerData);
            
            provider.initializePlayer();
            ExoPlayer player = provider.getPlayer();
            if (seekToPosition > 0 && player != null) {
                player.seekTo(seekToPosition);
            }
        } else {
            provider.showVideoErrorDialog("Видео недоступно", "У выбранной озвучки AnimeLib отсутствуют ссылки на видео.", () -> {
                provider.onPlayerSelected(playerData);
            });
        }
    }

    public void handleKodikPlayer(EpisodeResponse.PlayerData playerData, long seekToPosition) {
        if (provider == null) return;
        Log.d(TAG, "Handling Kodik player");

        String preferredQuality = provider.getPreferredQuality();
        if (provider.isDownloadedQuality(preferredQuality)) {
            DownloadedEpisodeEntity downloadedEp = provider.getDownloadedEpisodeForActive();
            if (downloadedEp != null && downloadedEp.getLocalFilePath() != null) {
                File file = new File(downloadedEp.getLocalFilePath());
                if (file.exists() && file.length() > 0) {
                    provider.setCurrentVideoUrl(Uri.fromFile(file).toString());
                    if (timecodeManager != null) timecodeManager.setTimecodes(playerData);
                    provider.initializePlayer();
                    ExoPlayer player = provider.getPlayer();
                    if (seekToPosition > 0 && player != null) {
                        player.seekTo(seekToPosition);
                    }
                    Log.d(TAG, "Playing downloaded local file: " + file.getAbsolutePath());
                    return;
                }
            }
        }

        if (timecodeManager != null) timecodeManager.setTimecodes(playerData);

        if (playerData.getSrc() != null && !playerData.getSrc().isEmpty()) {
            String kodikSrc = playerData.getSrc();
            if (!kodikSrc.startsWith("http")) {
                kodikSrc = "https:" + kodikSrc;
            }
            Log.d(TAG, "Kodik src: " + kodikSrc);
            fetchKodikVideoLinks(kodikSrc, seekToPosition);
        } else {
            Log.w(TAG, "No src found in Kodik player data");
            provider.showVideoErrorDialog("Ошибка Kodik", "Ссылка на плеер Kodik отсутствует. Попробуйте выбрать другую озвучку.", () -> {
                handleKodikPlayer(playerData, seekToPosition);
            });
            ExoPlayer player = provider.getPlayer();
            if (player != null) {
                player.stop();
                player.clearMediaItems();
            }
            View overlay = provider.getMenuLoadingOverlay();
            if (overlay != null) overlay.setVisibility(View.GONE);
        }
    }

    public void fetchKodikVideoLinks(String kodikSrc, long seekToPosition) {
        if (provider == null || apiService == null) return;
        Log.d(TAG, "Fetching HLS links for Kodik src: " + kodikSrc);
        provider.safeRunOnUiThread(() -> provider.showLoading("Получение HLS ссылок..."));

        apiService.fetchKodikVideoLinks(kodikSrc, new ApiService.KodikVideoCallback() {
            @Override
            public void onKodikVideoReceived(KodikResponse response) {
                provider.safeRunOnUiThread(() -> {
                    provider.hideLoading();
                    startHlsPlayer(response, seekToPosition);
                });
            }

            @Override
            public void onError(String error) {
                provider.safeRunOnUiThread(() -> {
                    provider.hideLoading();
                    provider.showVideoErrorDialog("Ошибка загрузки Kodik", "Не удалось загрузить HLS видеоссылки Kodik:\n" + error, () -> {
                        fetchKodikVideoLinks(kodikSrc, seekToPosition);
                    });
                    ExoPlayer player = provider.getPlayer();
                    if (player != null) {
                        player.stop();
                        player.clearMediaItems();
                    }
                    View overlay = provider.getMenuLoadingOverlay();
                    if (overlay != null) overlay.setVisibility(View.GONE);
                });
            }
        });
    }

    public void startHlsPlayer(KodikResponse kodikResponse, long seekToPosition) {
        if (provider == null) return;
        provider.setCurrentKodikResponse(kodikResponse);

        String hlsUrl = null;
        String preferredQuality = provider.getPreferredQuality();
        String preferredQualityKey = preferredQuality != null ? preferredQuality.replace("p", "") : null;

        if (preferredQualityKey != null && kodikResponse.getData().containsKey(preferredQualityKey) &&
                kodikResponse.getData().get(preferredQualityKey).length > 0) {
            hlsUrl = kodikResponse.getData().get(preferredQualityKey)[0].getSrc();
            Log.d(TAG, "Using preferred quality: " + preferredQualityKey + "p");
        } else {
            if (kodikResponse.getData().containsKey("720") && kodikResponse.getData().get("720").length > 0) {
                hlsUrl = kodikResponse.getData().get("720")[0].getSrc();
                if (preferredQuality == null) provider.setPreferredQuality("720p");
            } else if (kodikResponse.getData().containsKey("480") && kodikResponse.getData().get("480").length > 0) {
                hlsUrl = kodikResponse.getData().get("480")[0].getSrc();
                if (preferredQuality == null) provider.setPreferredQuality("480p");
            } else if (kodikResponse.getData().containsKey("360") && kodikResponse.getData().get("360").length > 0) {
                hlsUrl = kodikResponse.getData().get("360")[0].getSrc();
                if (preferredQuality == null) provider.setPreferredQuality("360p");
            }
        }

        if (hlsUrl != null) {
            if (!hlsUrl.startsWith("http")) {
                hlsUrl = "https:" + hlsUrl;
            }

            Log.d(TAG, "Starting HLS playback with URL: " + hlsUrl);
            provider.setCurrentVideoUrl(hlsUrl);
            provider.initializeHlsPlayer(hlsUrl);
            ExoPlayer player = provider.getPlayer();
            if (seekToPosition > 0 && player != null) {
                player.seekTo(seekToPosition);
            }
        } else {
            provider.showVideoErrorDialog("HLS видео недоступно", "HLS ссылка для выбранного качества Kodik не найдена.", () -> {
                if (playersManager != null) {
                    handleKodikPlayer(playersManager.getCurrentPlayerData(), seekToPosition);
                }
            });
        }
    }
}
