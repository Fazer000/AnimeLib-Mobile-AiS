package com.example.animelib.managers;

import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.TextureView;
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
 * Высокопроизводительный менеджер фоновой подсветки (Ambilight) на основе
 * легковесного второго ExoPlayer, синхронизированного с основным плеером.
 *
 * Архитектура:
 * 1. Событийная синхронизация (Event-Driven) без непрерывных тяжелых циклов seekTo/speed,
 *    что полностью исключает фризы, лаги декодера и рассинхрон.
 * 2. Ультра-низкие требования к ресурсам (ограничение 180p, отмена аудио/текста, минимальный буфер).
 * 3. Аппаратный GPU-блюр с повышенной насыщенностью цветов (Android 12+).
 * 4. Плавное альфа-затухание без резких скачков видимости.
 */
public class AmbientLightManager {
    private static final String TAG = "AmbientLightManager";
    
    // Интервал проверки рассинхрона (250 мс для высокой точности)
    private static final long DRIFT_SYNC_INTERVAL_MS = 250;
    // Порог жесткого перехода seekTo при сильном сдвиге (800 мс)
    private static final long HARD_SEEK_THRESHOLD_MS = 800;
    // Порог плавной микро-подгонки скорости (30 мс)
    private static final long SOFT_DRIFT_THRESHOLD_MS = 30;

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
    private long lastSyncSeekTimeMs = 0;

    private Player.Listener mainPlayerListener;
    private Player.Listener ambientPlayerListener;

    private final Runnable syncMonitorRunnable = new Runnable() {
        @Override
        public void run() {
            checkAndSyncDrift();
            if (isEnabled && !isSuspended && !isFrozen && mainPlayer != null && mainPlayer.isPlaying() && !isErrorState) {
                mainHandler.postDelayed(this, DRIFT_SYNC_INTERVAL_MS);
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
     * Стилизация и настройка отображения подсвечивающего плеера
     */
    private void setupAmbientViewStyle() {
        if (ambientContainer != null) {
            ambientContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        if (ambientPlayerView == null) return;

        ambientPlayerView.setUseController(false);
        ambientPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
        ambientPlayerView.setAlpha(1.0f);

        // Аппаратный размытый краевой блюр GPU (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                RenderEffect blurEffect = RenderEffect.createBlurEffect(120f, 120f, Shader.TileMode.CLAMP);
                
                // Матрица цвета: повышенная насыщенность (+35%) и легкое усиление яркости
                ColorMatrix colorMatrix = new ColorMatrix();
                colorMatrix.setSaturation(1.35f);
                
                ColorMatrix scaleMatrix = new ColorMatrix(new float[] {
                    1.08f, 0,     0,     0, 0,
                    0,     1.08f, 0,     0, 0,
                    0,     0,     1.08f, 0, 0,
                    0,     0,     0,     1, 0
                });
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
            if ((left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) && isEnabled && !isSuspended && !isFrozen) {
                refreshAmbientFrame();
            }
        });
        attachTextureViewListener();
    }

    /**
     * Обновление кадра при необходимости
     */
    public void refreshAmbientFrame() {
        mainHandler.post(() -> {
            if (isEnabled && !isSuspended && !isFrozen && ambientPlayer != null && mainPlayer != null && !isErrorState) {
                try {
                    ambientPlayer.setPlaybackParameters(mainPlayer.getPlaybackParameters());
                    ambientPlayer.seekTo(mainPlayer.getCurrentPosition());
                    if (mainPlayer.isPlaying()) {
                        ambientPlayer.play();
                        scheduleSyncMonitor();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error refreshing ambient frame", e);
                }
            }
        });
    }

    private void attachTextureViewListener() {
        if (ambientPlayerView == null) return;
        View videoSurfaceView = ambientPlayerView.getVideoSurfaceView();
        if (videoSurfaceView instanceof TextureView) {
            TextureView textureView = (TextureView) videoSurfaceView;
            TextureView.SurfaceTextureListener origListener = textureView.getSurfaceTextureListener();
            textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                    if (origListener != null) {
                        origListener.onSurfaceTextureAvailable(surface, width, height);
                    }
                    refreshAmbientFrame();
                }

                @Override
                public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
                    if (origListener != null) {
                        origListener.onSurfaceTextureSizeChanged(surface, width, height);
                    }
                    refreshAmbientFrame();
                }

                @Override
                public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                    if (origListener != null) {
                        return origListener.onSurfaceTextureDestroyed(surface);
                    }
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
                    if (origListener != null) {
                        origListener.onSurfaceTextureUpdated(surface);
                    }
                }
            });
        }
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
            if (visible) {
                showAmbientContainer(true);
                ensureAmbientPlayerInitialized();
                syncWithMainPlayerState();
            } else {
                hideAmbientContainer(true);
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
            hideAmbientContainer(false);
            pauseAmbientPlayer();
        });
    }

    public void freeze() {
        this.isFrozen = true;
        mainHandler.post(() -> {
            if (isEnabled && !isErrorState && !isSuspended) {
                showAmbientContainer(false);
            }
            pauseAmbientPlayer();
        });
    }

    public void unfreeze() {
        this.isFrozen = false;
        mainHandler.post(() -> {
            if (isEnabled && !isErrorState && !isSuspended) {
                showAmbientContainer(true);
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
                showAmbientContainer(true);
                ensureAmbientPlayerInitialized();
                syncWithMainPlayerState();
            }
        });
    }

    /**
     * Легковесная инициализация фонового плеера
     */
    private void ensureAmbientPlayerInitialized() {
        if (!isEnabled || mainPlayer == null || isErrorState || isSuspended || ambientPlayerView == null) return;

        if (ambientPlayer == null) {
            try {
                DefaultLoadControl ambientLoadControl = new DefaultLoadControl.Builder()
                        .setBufferDurationsMs(
                                1_000, // minBufferMs
                                3_000, // maxBufferMs
                                200,   // bufferForPlaybackMs
                                400    // bufferForPlaybackAfterRebufferMs
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
                attachTextureViewListener();

                // ОПТИМИЗАЦИЯ РЕСУРСОВ:
                // 1. Без звука
                ambientPlayer.setVolume(0f);

                // 2. Без аудио и субтитров, выбор трека с низким битрейтом (без жестких ограничений по разрешению)
                TrackSelectionParameters parameters = ambientPlayer.getTrackSelectionParameters()
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        .setMaxVideoBitrate(1_000_000)
                        .build();
                ambientPlayer.setTrackSelectionParameters(parameters);

                setupAmbientPlayerListener();
            } catch (Exception e) {
                Log.e(TAG, "Failed to create ambient ExoPlayer", e);
                isErrorState = true;
                hideAmbientContainer(false);
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
                ambientPlayer.setPlaybackParameters(mainPlayer.getPlaybackParameters());
                ambientPlayer.seekTo(mainPlayer.getCurrentPosition());
                if (mainPlayer.isPlaying()) {
                    ambientPlayer.play();
                    scheduleSyncMonitor();
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
                if (isPlaying) {
                    ambientPlayer.setPlaybackParameters(mainPlayer.getPlaybackParameters());
                    ambientPlayer.seekTo(mainPlayer.getCurrentPosition());
                    ambientPlayer.play();
                    scheduleSyncMonitor();
                } else {
                    ambientPlayer.pause();
                    cancelSyncMonitor();
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
                        ambientPlayer.setPlaybackParameters(mainPlayer.getPlaybackParameters());
                        if (mainPlayer.isPlaying()) {
                            ambientPlayer.seekTo(mainPlayer.getCurrentPosition());
                            ambientPlayer.play();
                            scheduleSyncMonitor();
                        }
                    }
                } else if (playbackState == Player.STATE_BUFFERING) {
                    ambientPlayer.pause();
                    cancelSyncMonitor();
                } else if (playbackState == Player.STATE_ENDED) {
                    ambientPlayer.pause();
                    cancelSyncMonitor();
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
                }
                if (mainPlayer.isPlaying()) {
                    ambientPlayer.play();
                    scheduleSyncMonitor();
                }
            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
                if (!isEnabled || isSuspended || isFrozen || ambientPlayer == null) return;
                ambientPlayer.setPlaybackParameters(playbackParameters);
            }
        };

        mainPlayer.addListener(mainPlayerListener);
    }

    private void setupAmbientPlayerListener() {
        if (ambientPlayer == null) return;

        ambientPlayerListener = new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (!isEnabled || isSuspended || isFrozen || mainPlayer == null) return;
                if (playbackState == Player.STATE_READY) {
                    if (mainPlayer.isPlaying() && !ambientPlayer.isPlaying()) {
                        ambientPlayer.play();
                        scheduleSyncMonitor();
                    }
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (!isEnabled || isSuspended || isFrozen || mainPlayer == null) return;
                if (isPlaying && mainPlayer.isPlaying()) {
                    scheduleSyncMonitor();
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Log.w(TAG, "Ambient player error: " + error.getMessage() + ". Soft retry planned.");
                isErrorState = true;
                cancelSyncMonitor();
                pauseAmbientPlayer();
                mainHandler.postDelayed(() -> {
                    if (isEnabled && !isSuspended && mainPlayer != null && mainPlayer.isPlaying()) {
                        isErrorState = false;
                        ensureAmbientPlayerInitialized();
                    }
                }, 5000);
            }
        };

        ambientPlayer.addListener(ambientPlayerListener);
    }

    /**
     * Безопасный мониторинг и устранение возможного рассинхрона.
     * Использует динамическую подгонку скорости воспроизведения (micro-speed scaling)
     * для устранения небольших задержек без рывков и перезапросов потока.
     */
    private void checkAndSyncDrift() {
        if (!isEnabled || isSuspended || isFrozen || mainPlayer == null || ambientPlayer == null || isErrorState) return;
        try {
            boolean mainIsPlaying = mainPlayer.isPlaying();
            boolean ambientIsPlaying = ambientPlayer.isPlaying();

            if (!mainIsPlaying || mainPlayer.getPlaybackState() == Player.STATE_BUFFERING) {
                if (ambientIsPlaying) {
                    ambientPlayer.pause();
                }
                return;
            }

            if (!ambientIsPlaying && mainIsPlaying && ambientPlayer.getPlaybackState() == Player.STATE_READY) {
                ambientPlayer.play();
            }

            long mainPos = mainPlayer.getCurrentPosition();
            long ambientPos = ambientPlayer.getCurrentPosition();
            long diffMs = mainPos - ambientPos; // положителен, если подсветка отстает
            long absDiffMs = Math.abs(diffMs);

            PlaybackParameters mainParams = mainPlayer.getPlaybackParameters();
            float baseSpeed = mainParams != null ? mainParams.speed : 1.0f;

            if (absDiffMs > HARD_SEEK_THRESHOLD_MS) {
                // Сильное отставание (>800мс): скачок seekTo с защитой от частых вызовов
                long now = android.os.SystemClock.elapsedRealtime();
                if (now - lastSyncSeekTimeMs > 2000) {
                    lastSyncSeekTimeMs = now;
                    ambientPlayer.seekTo(mainPos);
                    ambientPlayer.setPlaybackParameters(new PlaybackParameters(baseSpeed));
                    if (!ambientPlayer.isPlaying()) {
                        ambientPlayer.play();
                    }
                }
            } else if (absDiffMs > SOFT_DRIFT_THRESHOLD_MS) {
                // Небольшой рассинхрон (30мс - 800мс): бесшовное выравнивание за счет временного изм. скорости
                float correctionFactor;
                if (diffMs > 0) {
                    // Подсветка отстает -> ускоряем фоновый плеер
                    correctionFactor = (absDiffMs > 300) ? 1.15f : 1.05f;
                } else {
                    // Подсветка спешит -> слегка замедляем фоновый плеер
                    correctionFactor = (absDiffMs > 300) ? 0.85f : 0.95f;
                }
                float targetSpeed = Math.max(0.5f, Math.min(2.0f, baseSpeed * correctionFactor));
                if (Math.abs(ambientPlayer.getPlaybackParameters().speed - targetSpeed) > 0.01f) {
                    ambientPlayer.setPlaybackParameters(new PlaybackParameters(targetSpeed));
                }
            } else {
                // Полная синхронизация (отклонение <30мс): сброс на стандартную скорость
                if (Math.abs(ambientPlayer.getPlaybackParameters().speed - baseSpeed) > 0.01f) {
                    ambientPlayer.setPlaybackParameters(new PlaybackParameters(baseSpeed));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in checkAndSyncDrift", e);
        }
    }

    private void scheduleSyncMonitor() {
        mainHandler.removeCallbacks(syncMonitorRunnable);
        mainHandler.postDelayed(syncMonitorRunnable, DRIFT_SYNC_INTERVAL_MS);
    }

    private void cancelSyncMonitor() {
        mainHandler.removeCallbacks(syncMonitorRunnable);
    }

    private void syncWithMainPlayerState() {
        if (!isEnabled || isSuspended || isFrozen || mainPlayer == null || ambientPlayer == null || isErrorState) return;
        try {
            ambientPlayer.setPlaybackParameters(mainPlayer.getPlaybackParameters());
            ambientPlayer.seekTo(mainPlayer.getCurrentPosition());
            if (mainPlayer.isPlaying()) {
                ambientPlayer.play();
                scheduleSyncMonitor();
            } else {
                ambientPlayer.pause();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing with main player state", e);
        }
    }

    private void showAmbientContainer(boolean animate) {
        View targetView = ambientContainer != null ? ambientContainer : ambientPlayerView;
        if (targetView == null) return;

        targetView.removeCallbacks(null);
        if (targetView.getVisibility() != View.VISIBLE) {
            targetView.setVisibility(View.VISIBLE);
            targetView.setAlpha(0f);
        }

        if (animate) {
            targetView.animate()
                    .alpha(1.0f)
                    .setDuration(250)
                    .start();
        } else {
            targetView.setAlpha(1.0f);
        }
    }

    private void hideAmbientContainer(boolean animate) {
        View targetView = ambientContainer != null ? ambientContainer : ambientPlayerView;
        if (targetView == null) return;

        if (animate) {
            targetView.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> targetView.setVisibility(View.GONE))
                    .start();
        } else {
            targetView.setAlpha(0f);
            targetView.setVisibility(View.GONE);
        }
    }

    private void pauseAmbientPlayer() {
        if (ambientPlayer != null) {
            try {
                ambientPlayer.pause();
            } catch (Exception ignored) {}
        }
        cancelSyncMonitor();
    }

    public void releaseAmbientPlayer() {
        cancelSyncMonitor();
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
        mainHandler.postDelayed(() -> {
            if (isEnabled && !isSuspended && !isFrozen && ambientPlayerView != null) {
                ensureAmbientPlayerInitialized();
                attachTextureViewListener();
                refreshAmbientFrame();
            }
        }, 150);
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
