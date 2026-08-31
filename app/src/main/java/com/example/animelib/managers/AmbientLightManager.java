package com.example.animelib.managers;

import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import com.example.animelib.ui.AmbientVignetteOverlayView;
import com.example.animelib.util.MediaCacheManager;

/**
 * Менеджер фоновой подсветки (Ambilight) на основе второго легковесного ExoPlayer,
 * расположенного непосредственно под основным видеоплеером.
 */
public class AmbientLightManager {
    private static final String TAG = "AmbientLightManager";
    private static final long HARD_SEEK_THRESHOLD_MS = 1200; // Жёсткая подгонка кадра через seekTo только при разрыве >1.2с
    private static final long SPEED_ADJUST_MIN_DELTA_MS = 15; // Минимальный порог подстройки скорости (sub-frame sync)
    private static final long SYNC_INTERVAL_MS = 200; // Оптимизированный плавный цикл синхронизации (200 мс)

    private final Context context;
    private final PlayerView mainPlayerView;
    private final View ambientContainer;
    private final PlayerView ambientPlayerView;
    private final AmbientVignetteOverlayView ambientVignetteOverlay;
    private final Handler mainHandler;

    private ExoPlayer mainPlayer;
    private ExoPlayer ambientPlayer;

    private DataSource.Factory cacheDataSourceFactory;
    private MediaItem currentMediaItem;
    private String currentVideoUrl;

    private volatile boolean isEnabled = false;
    private volatile boolean isPrepared = false;
    private boolean isErrorState = false;
    private boolean isSuspended = false;
    private boolean isFrozen = false;
    private long lastHardSeekTimeMs = 0;

    private Player.Listener mainPlayerListener;
    private Player.Listener ambientPlayerListener;

    private final Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {
            syncPositionAndSpeed();
            if (isEnabled && !isSuspended && !isFrozen && mainPlayer != null && mainPlayer.isPlaying() && !isErrorState) {
                mainHandler.postDelayed(this, SYNC_INTERVAL_MS);
            }
        }
    };

    public AmbientLightManager(@NonNull Context context,
                               @NonNull PlayerView mainPlayerView,
                               @Nullable PlayerView ambientPlayerView) {
        this(context, mainPlayerView, null, ambientPlayerView, null);
    }

    public AmbientLightManager(@NonNull Context context,
                               @NonNull PlayerView mainPlayerView,
                               @Nullable View ambientContainer,
                               @Nullable PlayerView ambientPlayerView,
                               @Nullable AmbientVignetteOverlayView ambientVignetteOverlay) {
        this.context = context;
        this.mainPlayerView = mainPlayerView;
        this.ambientContainer = ambientContainer;
        this.ambientPlayerView = ambientPlayerView;
        this.ambientVignetteOverlay = ambientVignetteOverlay;
        this.mainHandler = new Handler(Looper.getMainLooper());

        setupAmbientViewStyle();
    }

    /**
     * Первичная настройка второго плеера (сохранение пропорций FIT, масштаб и блюр)
     */
    private void setupAmbientViewStyle() {
        if (ambientContainer != null) {
            ambientContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        if (ambientPlayerView == null) return;

        ambientPlayerView.setUseController(false);
        ambientPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
        ambientPlayerView.setAlpha(1.0f);

        // Аппаратный размытый краевой блюр GPU (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                RenderEffect blurEffect = RenderEffect.createBlurEffect(75f, 75f, Shader.TileMode.CLAMP);
                ColorMatrix colorMatrix = new ColorMatrix();
                colorMatrix.setSaturation(1.25f); // Насыщенность +25%
                ColorMatrix scaleMatrix = new ColorMatrix();
                scaleMatrix.setScale(1.0f, 1.0f, 1.0f, 1.0f); // Естественная яркость без завышения темных фонов
                colorMatrix.postConcat(scaleMatrix);

                RenderEffect colorEffect = RenderEffect.createColorFilterEffect(new ColorMatrixColorFilter(colorMatrix));
                RenderEffect combinedEffect = RenderEffect.createChainEffect(blurEffect, colorEffect);
                ambientPlayerView.setRenderEffect(combinedEffect);
            } catch (Exception e) {
                Log.e(TAG, "Failed to apply RenderEffect blur", e);
            }
        } else {
            ambientPlayerView.setAlpha(0.95f);
        }

        ambientPlayerView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if ((left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) && isEnabled && !isSuspended && ambientPlayer != null && mainPlayer != null) {
                if (mainPlayer.isPlaying() && !ambientPlayer.isPlaying()) {
                    ambientPlayer.play();
                }
            }
        });
    }

    public void setDataSourceFactory(DataSource.Factory dataSourceFactory) {
        if (dataSourceFactory != null) {
            this.cacheDataSourceFactory = MediaCacheManager.createCacheDataSourceFactory(context, dataSourceFactory);
            if (ambientPlayer != null) {
                releaseAmbientPlayer();
                if (isEnabled && !isSuspended) {
                    ensureAmbientPlayerInitialized();
                }
            }
        }
    }

    public void setPlayer(@Nullable ExoPlayer mainPlayer) {
        setPlayer(mainPlayer, null, null);
    }

    public void setPlayer(@Nullable ExoPlayer mainPlayer, @Nullable MediaItem mediaItem, @Nullable String videoUrl) {
        if (this.mainPlayer != null && mainPlayerListener != null) {
            this.mainPlayer.removeListener(mainPlayerListener);
        }

        this.mainPlayer = mainPlayer;
        if (mediaItem != null) this.currentMediaItem = mediaItem;
        if (videoUrl != null) this.currentVideoUrl = videoUrl;

        this.isErrorState = false;
        this.isPrepared = false;

        if (mainPlayer == null) {
            releaseAmbientPlayer();
            return;
        }

        setupMainPlayerListener();

        if (isEnabled && !isSuspended) {
            ensureAmbientPlayerInitialized();
        }
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;

        mainHandler.post(() -> {
            boolean visible = enabled && !isErrorState && !isSuspended;
            if (ambientContainer != null) {
                ambientContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
            } else if (ambientPlayerView != null) {
                ambientPlayerView.setVisibility(visible ? View.VISIBLE : View.GONE);
            }

            if (enabled && !isSuspended) {
                ensureAmbientPlayerInitialized();
                syncWithMainPlayerState();
            } else {
                pauseAmbientPlayer();
            }
        });

        Log.d(TAG, "Ambient light " + (enabled ? "enabled" : "disabled"));
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void suspend() {
        this.isSuspended = true;
        mainHandler.post(() -> {
            if (ambientContainer != null) {
                ambientContainer.setVisibility(View.GONE);
            } else if (ambientPlayerView != null) {
                ambientPlayerView.setVisibility(View.GONE);
            }
            pauseAmbientPlayer();
        });
    }

    public void freeze() {
        this.isFrozen = true;
        mainHandler.post(() -> {
            if (isEnabled && !isErrorState && !isSuspended) {
                if (ambientContainer != null) {
                    ambientContainer.setVisibility(View.VISIBLE);
                } else if (ambientPlayerView != null) {
                    ambientPlayerView.setVisibility(View.VISIBLE);
                }
            }
            pauseAmbientPlayer();
        });
    }

    public void unfreeze() {
        this.isFrozen = false;
        mainHandler.post(() -> {
            if (isEnabled && !isErrorState && !isSuspended) {
                if (ambientContainer != null) {
                    ambientContainer.setVisibility(View.VISIBLE);
                } else if (ambientPlayerView != null) {
                    ambientPlayerView.setVisibility(View.VISIBLE);
                }
                ensureAmbientPlayerInitialized();
                syncWithMainPlayerState();
            }
        });
    }

    public void resume() {
        this.isSuspended = false;
        this.isFrozen = false;
        mainHandler.post(() -> {
            if (isEnabled && !isErrorState) {
                if (ambientContainer != null) {
                    ambientContainer.setVisibility(View.VISIBLE);
                } else if (ambientPlayerView != null) {
                    ambientPlayerView.setVisibility(View.VISIBLE);
                }
                ensureAmbientPlayerInitialized();
                syncWithMainPlayerState();
            }
        });
    }

    /**
     * Инициализация второго плеера с ультра-низким энергопотреблением
     */
    private void ensureAmbientPlayerInitialized() {
        if (!isEnabled || mainPlayer == null || isErrorState || isSuspended || ambientPlayerView == null) return;

        if (ambientPlayer == null) {
            try {
                DefaultLoadControl ambientLoadControl = new DefaultLoadControl.Builder()
                        .setBufferDurationsMs(
                                1_500, // minBufferMs
                                4_000, // maxBufferMs
                                300,   // bufferForPlaybackMs
                                500    // bufferForPlaybackAfterRebufferMs
                        )
                        .setPrioritizeTimeOverSizeThresholds(true)
                        .build();

                ExoPlayer.Builder builder = new ExoPlayer.Builder(context)
                        .setLoadControl(ambientLoadControl);

                if (cacheDataSourceFactory != null) {
                    builder.setMediaSourceFactory(new DefaultMediaSourceFactory(cacheDataSourceFactory));
                }

                ambientPlayer = builder.build();
                ambientPlayerView.setPlayer(ambientPlayer);

                // ОПТИМИЗАЦИЯ ДЛЯ МИНИМАЛЬНОГО ПОТРЕБЛЕНИЯ РЕСУРСОВ:
                // 1. Отключаем звук полностью
                ambientPlayer.setVolume(0f);

                // 2. Отключаем аудио и текстовые треки, ограничиваем максимальное качество до 180p/240p
                TrackSelectionParameters parameters = ambientPlayer.getTrackSelectionParameters()
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        .setMaxVideoSize(320, 180)
                        .setMaxVideoBitrate(200_000)
                        .build();
                ambientPlayer.setTrackSelectionParameters(parameters);

                setupAmbientPlayerListener();
            } catch (Exception e) {
                Log.e(TAG, "Failed to create ambient ExoPlayer", e);
                isErrorState = true;
                if (ambientPlayerView != null) ambientPlayerView.setVisibility(View.GONE);
                return;
            }
        }

        prepareAmbientMedia();
    }

    private void prepareAmbientMedia() {
        if (ambientPlayer == null || mainPlayer == null || isErrorState) return;

        try {
            MediaItem mediaItemToUse = currentMediaItem;
            if (mediaItemToUse == null && currentVideoUrl != null && !currentVideoUrl.isEmpty()) {
                mediaItemToUse = MediaItem.fromUri(currentVideoUrl);
            }

            if (mediaItemToUse != null) {
                ambientPlayer.setMediaItem(mediaItemToUse);
                ambientPlayer.prepare();
                ambientPlayer.seekTo(mainPlayer.getCurrentPosition());
                if (mainPlayer.isPlaying()) {
                    ambientPlayer.play();
                } else {
                    ambientPlayer.pause();
                }
                isPrepared = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error preparing ambient media", e);
        }
    }

    private void setupMainPlayerListener() {
        if (mainPlayer == null) return;

        mainPlayerListener = new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (!isEnabled || isSuspended || isFrozen || ambientPlayer == null) return;
                mainHandler.removeCallbacks(syncRunnable);
                if (isPlaying) {
                    syncPositionAndSpeed();
                    ambientPlayer.play();
                    mainHandler.post(syncRunnable);
                } else {
                    ambientPlayer.pause();
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (!isEnabled || isSuspended || isFrozen || ambientPlayer == null) return;
                if (playbackState == Player.STATE_READY) {
                    if (!isPrepared || isErrorState) {
                        isErrorState = false;
                        prepareAmbientMedia();
                    } else {
                        syncPositionAndSpeed();
                        if (mainPlayer.isPlaying()) {
                            ambientPlayer.play();
                            mainHandler.removeCallbacks(syncRunnable);
                            mainHandler.post(syncRunnable);
                        }
                    }
                } else if (playbackState == Player.STATE_BUFFERING) {
                    ambientPlayer.pause();
                    mainHandler.removeCallbacks(syncRunnable);
                }
            }

            @Override
            public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
                if (!isEnabled || isSuspended || isFrozen || ambientPlayer == null) return;
                if (isErrorState) {
                    isErrorState = false;
                    prepareAmbientMedia();
                } else {
                    ambientPlayer.seekTo(mainPlayer.getCurrentPosition());
                    syncPositionAndSpeed();
                }
                if (mainPlayer.isPlaying()) {
                    mainHandler.removeCallbacks(syncRunnable);
                    mainHandler.post(syncRunnable);
                }
            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
                if (!isEnabled || isSuspended || isFrozen || ambientPlayer == null) return;
                syncPositionAndSpeed();
            }
        };

        mainPlayer.addListener(mainPlayerListener);
    }

    private void setupAmbientPlayerListener() {
        if (ambientPlayer == null) return;

        ambientPlayerListener = new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                Log.w(TAG, "Ambient player encountered playback error: " + error.getMessage() + ". Retrying softly.");
                isErrorState = true;
                mainHandler.removeCallbacks(syncRunnable);
                pauseAmbientPlayer();
            }
        };

        ambientPlayer.addListener(ambientPlayerListener);
    }

    private void syncPositionAndSpeed() {
        if (!isEnabled || isSuspended || isFrozen || mainPlayer == null || ambientPlayer == null || isErrorState) return;
        try {
            if (!mainPlayer.isPlaying()) {
                if (ambientPlayer.isPlaying()) {
                    ambientPlayer.pause();
                }
                return;
            }

            int mainState = mainPlayer.getPlaybackState();
            if (mainState == Player.STATE_BUFFERING) {
                if (ambientPlayer.isPlaying()) {
                    ambientPlayer.pause();
                }
                return;
            } else if (mainState == Player.STATE_READY && !ambientPlayer.isPlaying()) {
                ambientPlayer.play();
            }

            long mainPos = mainPlayer.getCurrentPosition();
            long ambientPos = ambientPlayer.getCurrentPosition();
            long deltaMs = mainPos - ambientPos;

            float mainSpeed = mainPlayer.getPlaybackParameters().speed;
            if (mainSpeed <= 0.1f) mainSpeed = 1.0f;

            if (Math.abs(deltaMs) > HARD_SEEK_THRESHOLD_MS) {
                long now = android.os.SystemClock.elapsedRealtime();
                if (now - lastHardSeekTimeMs > 2000) {
                    lastHardSeekTimeMs = now;
                    ambientPlayer.seekTo(mainPos);
                    ambientPlayer.setPlaybackParameters(new PlaybackParameters(mainSpeed));
                }
            } else if (Math.abs(deltaMs) > SPEED_ADJUST_MIN_DELTA_MS) {
                // Бесшовное динамическое микро-выравнивание скорости воспроизведения (NTP-Style)
                // Без рестартов, скачков и фризов декодера!
                float adjustFactor;
                if (deltaMs > 0) {
                    // ambientPlayer отстает от главного -> плавно ускоряем на 1..15%
                    adjustFactor = 1.0f + Math.min(0.15f, (deltaMs / 400.0f) * 0.08f);
                } else {
                    // ambientPlayer ушел вперед главного -> плавно замедляем на 1..15%
                    adjustFactor = 1.0f - Math.min(0.15f, (Math.abs(deltaMs) / 400.0f) * 0.08f);
                }
                float targetSpeed = mainSpeed * adjustFactor;
                ambientPlayer.setPlaybackParameters(new PlaybackParameters(targetSpeed));
            } else {
                // Идеальная субкадровая синхронизация (< 15 мс)
                PlaybackParameters currentParams = ambientPlayer.getPlaybackParameters();
                if (Math.abs(currentParams.speed - mainSpeed) > 0.001f) {
                    ambientPlayer.setPlaybackParameters(new PlaybackParameters(mainSpeed));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in syncPositionAndSpeed", e);
        }
    }

    private void syncWithMainPlayerState() {
        if (!isEnabled || isSuspended || isFrozen || mainPlayer == null || ambientPlayer == null || isErrorState) return;
        try {
            ambientPlayer.setPlaybackParameters(mainPlayer.getPlaybackParameters());
            ambientPlayer.seekTo(mainPlayer.getCurrentPosition());
            if (mainPlayer.isPlaying()) {
                ambientPlayer.play();
                mainHandler.removeCallbacks(syncRunnable);
                mainHandler.post(syncRunnable);
            } else {
                ambientPlayer.pause();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing with main player state", e);
        }
    }

    private void pauseAmbientPlayer() {
        if (ambientPlayer != null) {
            try {
                ambientPlayer.pause();
            } catch (Exception ignored) {}
        }
        mainHandler.removeCallbacks(syncRunnable);
    }

    public void releaseAmbientPlayer() {
        mainHandler.removeCallbacks(syncRunnable);
        if (mainPlayer != null && mainPlayerListener != null) {
            mainPlayer.removeListener(mainPlayerListener);
            mainPlayerListener = null;
        }

        if (ambientPlayer != null) {
            try {
                if (ambientPlayerListener != null) {
                    ambientPlayer.removeListener(ambientPlayerListener);
                    ambientPlayerListener = null;
                }
                ambientPlayer.stop();
                ambientPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing ambientPlayer", e);
            }
            ambientPlayer = null;
        }
        isPrepared = false;
        isErrorState = false;
    }

    public void cleanup() {
        releaseAmbientPlayer();
    }

    public void onConfigurationChanged() {
        mainHandler.post(() -> {
            if (isEnabled && !isSuspended && !isFrozen && ambientPlayerView != null && ambientPlayer != null) {
                ambientPlayerView.setPlayer(null);
                ambientPlayerView.setPlayer(ambientPlayer);
                if (mainPlayer != null) {
                    ambientPlayer.seekTo(mainPlayer.getCurrentPosition());
                    if (mainPlayer.isPlaying()) {
                        ambientPlayer.play();
                        mainHandler.removeCallbacks(syncRunnable);
                        mainHandler.post(syncRunnable);
                    }
                }
            }
        });
    }

    public void onPause() {
        suspend();
    }

    public void onResume() {
        resume();
    }

    public void onDestroy() {
        cleanup();
    }
}
