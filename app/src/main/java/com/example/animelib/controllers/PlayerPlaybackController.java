package com.example.animelib.controllers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.TrackSelector;
import androidx.media3.ui.PlayerView;

import com.example.animelib.managers.AmbientLightManager;
import com.example.animelib.managers.GesturesManager;
import com.example.animelib.managers.TimecodeManager;
import com.example.animelib.util.MediaCacheManager;
import com.example.animelib.util.SurroundRenderersFactory;

import java.util.Collections;

/**
 * Контроллер для инициализации, управления жизненным циклом и воспроизведением ExoPlayer.
 */
public class PlayerPlaybackController {

    private static final String TAG = "PlayerPlaybackCtrl";

    private final Context context;
    private ExoPlayer player;
    private PlayerView playerView;
    private DataSource.Factory httpDataSourceFactory;

    private boolean isVideoLoading = false;
    private boolean hasRenderedFirstFrame = false;
    private boolean autoPlayOnPrepare = true;
    private int currentResizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT;

    private final Handler bufferingHandler = new Handler(Looper.getMainLooper());
    private Runnable bufferingRunnable;

    public interface PlaybackCallback {
        Context getPlayerContext();
        PlayerAudioController getPlayerAudioController();
        AmbientLightManager getAmbientLightManager();
        GesturesManager getGesturesManager();
        TimecodeManager getTimecodeManager();
        void onFirstFrameRendered();
        void onPlaybackStateChanged(int state, boolean playWhenReady);
        void onPlayerError(PlaybackException error);
        void safeRunOnUiThread(Runnable runnable);
    }

    private PlaybackCallback callback;

    public PlayerPlaybackController(Context context, PlayerView playerView, DataSource.Factory httpDataSourceFactory) {
        this.context = context;
        this.playerView = playerView;
        this.httpDataSourceFactory = httpDataSourceFactory;
    }

    public void setCallback(PlaybackCallback callback) {
        this.callback = callback;
    }

    public void setPlayerView(PlayerView playerView) {
        this.playerView = playerView;
        if (playerView != null && player != null) {
            playerView.setPlayer(player);
        }
    }

    @Nullable
    public ExoPlayer getPlayer() {
        return player;
    }

    public boolean isVideoLoading() {
        return isVideoLoading;
    }

    public void setVideoLoading(boolean loading) {
        this.isVideoLoading = loading;
    }

    public boolean hasRenderedFirstFrame() {
        return hasRenderedFirstFrame;
    }

    public void setHasRenderedFirstFrame(boolean rendered) {
        this.hasRenderedFirstFrame = rendered;
    }

    public boolean isAutoPlayOnPrepare() {
        return autoPlayOnPrepare;
    }

    public void setAutoPlayOnPrepare(boolean autoPlay) {
        this.autoPlayOnPrepare = autoPlay;
    }

    public int getCurrentResizeMode() {
        return currentResizeMode;
    }

    public void setVideoResizeMode(int resizeMode) {
        this.currentResizeMode = resizeMode;
        if (playerView != null) {
            playerView.setResizeMode(resizeMode);
        }
    }

    public ExoPlayer initializePlayer(String videoUrl, MediaItem mediaItem, int resizeMode, boolean autoPlay) {
        if (context == null) return null;

        if (player != null) {
            player.release();
            player = null;
        }

        this.isVideoLoading = true;
        this.hasRenderedFirstFrame = false;
        this.currentResizeMode = resizeMode;
        this.autoPlayOnPrepare = autoPlay;

        LoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(50000, 120000, 2500, 5000)
                .build();

        TrackSelector trackSelector = new DefaultTrackSelector(context);

        Context playerCtx = (callback != null) ? callback.getPlayerContext() : context;
        PlayerAudioController audioCtrl = (callback != null) ? callback.getPlayerAudioController() : null;

        SurroundRenderersFactory rf1 = new SurroundRenderersFactory(
                playerCtx, audioCtrl != null ? audioCtrl.getSurroundAudioProcessor() : null);

        DataSource.Factory cachedHttpFactory = MediaCacheManager.createCacheDataSourceFactory(context, httpDataSourceFactory);

        player = new ExoPlayer.Builder(playerCtx, rf1)
                .setSeekBackIncrementMs(10000)
                .setSeekForwardIncrementMs(10000)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(cachedHttpFactory))
                .setLoadControl(loadControl)
                .setTrackSelector(trackSelector)
                .build();

        if (playerView != null) {
            playerView.setPlayer(player);
            playerView.setResizeMode(resizeMode);
            playerView.setUseController(true);
        }

        if (callback != null) {
            AmbientLightManager ambientLightManager = callback.getAmbientLightManager();
            if (ambientLightManager != null) {
                ambientLightManager.setDataSourceFactory(httpDataSourceFactory);
                ambientLightManager.setPlayer(player);
            }

            GesturesManager gesturesManager = callback.getGesturesManager();
            if (gesturesManager != null) {
                gesturesManager.updatePlayer(player);
            }
        }

        if (audioCtrl != null) {
            audioCtrl.attachPlayer(player);
        }

        setupInternalListeners();

        if (mediaItem != null) {
            player.setMediaItem(mediaItem);
            if (callback != null && callback.getAmbientLightManager() != null) {
                callback.getAmbientLightManager().setPlayer(player, mediaItem, videoUrl);
            }
        }

        player.prepare();

        if (autoPlayOnPrepare) {
            player.play();
        }

        startBufferingMonitoring();
        return player;
    }

    public ExoPlayer initializeHlsPlayer(String hlsUrl, MediaItem mediaItem, int resizeMode, boolean autoPlay) {
        if (context == null) return null;

        if (player != null) {
            player.release();
            player = null;
        }

        this.isVideoLoading = true;
        this.hasRenderedFirstFrame = false;
        this.currentResizeMode = resizeMode;
        this.autoPlayOnPrepare = autoPlay;

        okhttp3.OkHttpClient okHttpClient = new okhttp3.OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    okhttp3.Request original = chain.request();
                    boolean kodikHost = original.url().host().contains("kodik");
                    String referer = kodikHost ? "https://kodik.info/" : "https://v3.animelib.org/";
                    String origin = kodikHost ? "https://kodik.info" : "https://v3.animelib.org";

                    okhttp3.Request.Builder requestBuilder = original.newBuilder()
                            .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1")
                            .header("Referer", referer)
                            .header("Accept", "video/mp4,video/*,*/*")
                            .header("Accept-Encoding", "identity;q=1, *;q=0")
                            .header("Accept-Language", "ru,en;q=0.9,de;q=0.8,zh;q=0.7")
                            .header("Origin", origin)
                            .header("Sec-Fetch-Dest", "video")
                            .header("Sec-Fetch-Mode", "cors")
                            .header("Sec-Fetch-Site", "cross-site")
                            .header("Priority", "i");

                    return chain.proceed(requestBuilder.build());
                })
                .build();

        androidx.media3.datasource.okhttp.OkHttpDataSource.Factory okHttpDataSourceFactory = 
                new androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(okHttpClient);

        LoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(50000, 120000, 2500, 5000)
                .build();

        TrackSelector trackSelector = new DefaultTrackSelector(context);

        Context playerCtx = (callback != null) ? callback.getPlayerContext() : context;
        PlayerAudioController audioCtrl = (callback != null) ? callback.getPlayerAudioController() : null;

        SurroundRenderersFactory rf2 = new SurroundRenderersFactory(
                playerCtx, audioCtrl != null ? audioCtrl.getSurroundAudioProcessor() : null);

        DataSource.Factory cachedOkHttpFactory = MediaCacheManager.createCacheDataSourceFactory(context, okHttpDataSourceFactory);

        player = new ExoPlayer.Builder(playerCtx, rf2)
                .setSeekBackIncrementMs(10000)
                .setSeekForwardIncrementMs(10000)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(cachedOkHttpFactory))
                .setLoadControl(loadControl)
                .setTrackSelector(trackSelector)
                .build();

        if (playerView != null) {
            playerView.setPlayer(player);
            playerView.setResizeMode(resizeMode);
            playerView.setUseController(true);
        }

        if (callback != null) {
            AmbientLightManager ambientLightManager = callback.getAmbientLightManager();
            if (ambientLightManager != null) {
                ambientLightManager.setDataSourceFactory(okHttpDataSourceFactory);
                ambientLightManager.setPlayer(player, mediaItem, hlsUrl);
            }

            GesturesManager gesturesManager = callback.getGesturesManager();
            if (gesturesManager != null) {
                gesturesManager.updatePlayer(player);
            }
        }

        if (audioCtrl != null) {
            audioCtrl.attachPlayer(player);
        }

        setupInternalListeners();

        if (mediaItem != null) {
            player.setMediaItem(mediaItem);
        }

        player.prepare();

        if (autoPlayOnPrepare) {
            player.play();
        }

        startBufferingMonitoring();
        return player;
    }

    public void initializePlayer(String videoUrl, @Nullable String subtitleUrl, boolean subtitlesEnabled) {
        if (context == null || playerView == null) return;

        isVideoLoading = true;
        hasRenderedFirstFrame = false;

        LoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(50000, 120000, 2500, 5000)
                .build();

        TrackSelector trackSelector = new DefaultTrackSelector(context);

        Context playerCtx = (callback != null) ? callback.getPlayerContext() : context;
        PlayerAudioController audioCtrl = (callback != null) ? callback.getPlayerAudioController() : null;

        SurroundRenderersFactory rf1 = new SurroundRenderersFactory(
                playerCtx, audioCtrl != null ? audioCtrl.getSurroundAudioProcessor() : null);

        DataSource.Factory cachedHttpFactory = MediaCacheManager.createCacheDataSourceFactory(context, httpDataSourceFactory);

        player = new ExoPlayer.Builder(playerCtx, rf1)
                .setSeekBackIncrementMs(10000)
                .setSeekForwardIncrementMs(10000)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(cachedHttpFactory))
                .setLoadControl(loadControl)
                .setTrackSelector(trackSelector)
                .build();

        playerView.setPlayer(player);
        playerView.setResizeMode(currentResizeMode);

        if (callback != null) {
            AmbientLightManager ambientLightManager = callback.getAmbientLightManager();
            if (ambientLightManager != null) {
                ambientLightManager.setDataSourceFactory(httpDataSourceFactory);
                ambientLightManager.setPlayer(player);
            }

            GesturesManager gesturesManager = callback.getGesturesManager();
            if (gesturesManager != null) {
                gesturesManager.updatePlayer(player);
            }
        }

        if (audioCtrl != null) {
            audioCtrl.attachPlayer(player);
        }

        playerView.setUseController(true);

        setupInternalListeners();

        MediaItem mediaItem = createMediaItemWithSubtitles(videoUrl, subtitleUrl, subtitlesEnabled);
        player.setMediaItem(mediaItem);

        if (callback != null && callback.getAmbientLightManager() != null) {
            callback.getAmbientLightManager().setPlayer(player, mediaItem, videoUrl);
        }

        player.prepare();

        if (autoPlayOnPrepare) {
            player.play();
        }
        autoPlayOnPrepare = true;

        startBufferingMonitoring();
    }

    private void setupInternalListeners() {
        if (player == null) return;

        player.addListener(new Player.Listener() {
            @Override
            public void onRenderedFirstFrame() {
                hasRenderedFirstFrame = true;
                isVideoLoading = false;
                if (callback != null) {
                    callback.onFirstFrameRendered();
                }
            }

            @Override
            public void onPlaybackStateChanged(int state) {
                boolean playWhenReady = player != null && player.getPlayWhenReady();
                if (callback != null) {
                    callback.onPlaybackStateChanged(state, playWhenReady);
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                if (callback != null) {
                    callback.onPlayerError(error);
                }
            }
        });
    }

    public MediaItem createMediaItemWithSubtitles(String videoUrl, @Nullable String subtitleUrl, boolean subtitlesEnabled) {
        MediaItem.Builder builder = new MediaItem.Builder().setUri(videoUrl);

        if (subtitlesEnabled && subtitleUrl != null && !subtitleUrl.isEmpty()) {
            MediaItem.SubtitleConfiguration subtitleConfig = new MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(subtitleUrl))
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage("ru")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .setRoleFlags(MediaMetadata.FOLDER_TYPE_MIXED)
                    .build();

            builder.setSubtitleConfigurations(Collections.singletonList(subtitleConfig));
        }

        return builder.build();
    }

    public void applyPlaybackSpeed(float speed) {
        if (player != null) {
            player.setPlaybackSpeed(speed);
        }
    }

    public void startBufferingMonitoring() {
        stopBufferingMonitoring();
        bufferingRunnable = new Runnable() {
            @Override
            public void run() {
                if (player != null && callback != null) {
                    int state = player.getPlaybackState();
                    boolean isBuffering = (state == Player.STATE_BUFFERING);
                    callback.onPlaybackStateChanged(state, player.getPlayWhenReady());
                    if (isBuffering) {
                        bufferingHandler.postDelayed(this, 1000);
                    }
                }
            }
        };
        bufferingHandler.post(bufferingRunnable);
    }

    public void stopBufferingMonitoring() {
        if (bufferingRunnable != null) {
            bufferingHandler.removeCallbacks(bufferingRunnable);
            bufferingRunnable = null;
        }
    }

    public void stopCurrentPlayback() {
        stopBufferingMonitoring();
        if (player != null) {
            try {
                player.stop();
                player.clearMediaItems();
            } catch (Exception e) {
                Log.w(TAG, "Error stopping player: " + e.getMessage());
            }
        }
    }

    public void releasePlayer() {
        stopBufferingMonitoring();
        if (player != null) {
            try {
                player.release();
            } catch (Exception e) {
                Log.w(TAG, "Error releasing player: " + e.getMessage());
            }
            player = null;
        }
    }
}
