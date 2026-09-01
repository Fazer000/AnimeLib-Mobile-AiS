package com.example.animelib.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.datasource.DataSource;
import androidx.media3.ui.PlayerView;

import com.example.animelib.ui.AmbientLightView;
import com.example.animelib.ui.AmbientVignetteOverlayView;
import com.example.animelib.util.FastBlurUtils;

/**
 * Высокооптимизированный менеджер фоновой подсветки (Ambilight).
 * Сэмплирует кадры напрямую из TextureView основного плеера.
 * 0 дополнительной сетевой нагрузки, 0 второго ExoPlayer, 0 задержек и фризов!
 */
public class AmbientLightManager {
    private static final String TAG = "AmbientLightManager";
    private static final int SAMPLE_WIDTH = 64;
    private static final int SAMPLE_HEIGHT = 36;
    private static final long PLAYING_SAMPLE_INTERVAL_MS = 33; // ~30 FPS
    private static final long PAUSED_SAMPLE_INTERVAL_MS = 300;

    private final Context context;
    private final PlayerView mainPlayerView;
    private final View ambientContainer;
    private final AmbientLightView ambientLightView;
    private final AmbientVignetteOverlayView ambientVignetteOverlay;
    private final Handler mainHandler;

    private Player mainPlayer;
    private Player.Listener mainPlayerListener;

    private HandlerThread workerThread;
    private Handler workerHandler;

    private Bitmap sampleBitmap;
    private int[] samplePixels;
    private FastBlurUtils fastBlurUtils;

    private volatile boolean isEnabled = false;
    private volatile boolean isSuspended = false;
    private volatile boolean isFrozen = false;

    private TextureView cachedTextureView = null;
    private final Rect tempBoundsRect = new Rect();

    private final Runnable sampleRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isEnabled || isSuspended || isFrozen) {
                return;
            }

            boolean isPlaying = mainPlayer != null && mainPlayer.isPlaying();
            captureAndProcessFrame();

            long delay = isPlaying ? PLAYING_SAMPLE_INTERVAL_MS : PAUSED_SAMPLE_INTERVAL_MS;
            if (workerHandler != null && isEnabled && !isSuspended && !isFrozen) {
                workerHandler.postDelayed(this, delay);
            }
        }
    };

    public AmbientLightManager(@NonNull Context context,
                               @NonNull PlayerView mainPlayerView,
                               @Nullable View ambientPlayerView) {
        this(context, mainPlayerView, null, ambientPlayerView, null);
    }

    public AmbientLightManager(@NonNull Context context,
                               @NonNull PlayerView mainPlayerView,
                               @Nullable View ambientContainer,
                               @Nullable View ambientView,
                               @Nullable AmbientVignetteOverlayView ambientVignetteOverlay) {
        this.context = context;
        this.mainPlayerView = mainPlayerView;
        this.ambientContainer = ambientContainer;
        this.ambientVignetteOverlay = ambientVignetteOverlay;
        this.mainHandler = new Handler(Looper.getMainLooper());

        if (ambientView instanceof AmbientLightView) {
            this.ambientLightView = (AmbientLightView) ambientView;
        } else if (ambientContainer instanceof ViewGroup) {
            this.ambientLightView = findAmbientLightView((ViewGroup) ambientContainer);
        } else {
            this.ambientLightView = null;
        }

        initWorkerThread();
    }

    private AmbientLightView findAmbientLightView(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof AmbientLightView) {
                return (AmbientLightView) child;
            } else if (child instanceof ViewGroup) {
                AmbientLightView found = findAmbientLightView((ViewGroup) child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void initWorkerThread() {
        workerThread = new HandlerThread("AmbientLightWorker");
        workerThread.start();
        workerHandler = new Handler(workerThread.getLooper());

        sampleBitmap = Bitmap.createBitmap(SAMPLE_WIDTH, SAMPLE_HEIGHT, Bitmap.Config.ARGB_8888);
        samplePixels = new int[SAMPLE_WIDTH * SAMPLE_HEIGHT];
        fastBlurUtils = new FastBlurUtils(SAMPLE_WIDTH, SAMPLE_HEIGHT, 12);
    }

    private TextureView getTextureView() {
        if (cachedTextureView != null && cachedTextureView.isAttachedToWindow()) {
            return cachedTextureView;
        }
        if (mainPlayerView != null) {
            View surface = mainPlayerView.getVideoSurfaceView();
            if (surface instanceof TextureView) {
                cachedTextureView = (TextureView) surface;
                return cachedTextureView;
            }
            cachedTextureView = recursiveFindTextureView(mainPlayerView);
            return cachedTextureView;
        }
        return null;
    }

    private TextureView recursiveFindTextureView(View view) {
        if (view instanceof TextureView) {
            return (TextureView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextureView found = recursiveFindTextureView(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private void captureAndProcessFrame() {
        mainHandler.post(() -> {
            TextureView textureView = getTextureView();
            if (textureView == null || !textureView.isAvailable() || textureView.getWidth() <= 0 || textureView.getHeight() <= 0) {
                return;
            }

            try {
                // Извлекаем кадр с GPU напрямую в предустановленный битмап без аллокаций
                textureView.getBitmap(sampleBitmap);
                sampleBitmap.getPixels(samplePixels, 0, SAMPLE_WIDTH, 0, 0, SAMPLE_WIDTH, SAMPLE_HEIGHT);

                // Синхронизируем границы видеокарточки для vignette
                updateVideoBounds();

                // Обработка размытия и калибровка цветов на фоновом потоке
                if (workerHandler != null) {
                    workerHandler.post(() -> {
                        if (fastBlurUtils != null && samplePixels != null) {
                            fastBlurUtils.blurAndSmooth(samplePixels);
                            mainHandler.post(() -> {
                                if (ambientLightView != null && isEnabled && !isSuspended && !isFrozen) {
                                    ambientLightView.updateSamplePixels(samplePixels, SAMPLE_WIDTH, SAMPLE_HEIGHT);
                                }
                            });
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error capturing frame from TextureView", e);
            }
        });
    }

    private void updateVideoBounds() {
        if (mainPlayerView == null) return;
        mainPlayerView.getGlobalVisibleRect(tempBoundsRect);
        if (tempBoundsRect.width() > 0 && tempBoundsRect.height() > 0) {
            if (ambientVignetteOverlay != null) {
                ambientVignetteOverlay.setVideoBounds(
                        tempBoundsRect.left,
                        tempBoundsRect.top,
                        tempBoundsRect.right,
                        tempBoundsRect.bottom
                );
            }
            if (ambientLightView != null) {
                ambientLightView.setCustomVideoBounds(
                        tempBoundsRect.left,
                        tempBoundsRect.top,
                        tempBoundsRect.right,
                        tempBoundsRect.bottom
                );
            }
        }
    }

    public void setDataSourceFactory(DataSource.Factory dataSourceFactory) {
        // Прямое сэмплирование кадра не требует DataSource
    }

    public void setPlayer(@Nullable Player mainPlayer) {
        setPlayer(mainPlayer, null, null);
    }

    public void setPlayer(@Nullable Player mainPlayer, @Nullable MediaItem mediaItem, @Nullable String videoUrl) {
        if (this.mainPlayer != null && mainPlayerListener != null) {
            this.mainPlayer.removeListener(mainPlayerListener);
        }

        this.mainPlayer = mainPlayer;
        if (mainPlayer != null) {
            setupMainPlayerListener();
        }

        if (isEnabled && !isSuspended && !isFrozen) {
            triggerSampling();
        }
    }

    private void setupMainPlayerListener() {
        if (mainPlayer == null) return;

        mainPlayerListener = new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isEnabled && !isSuspended && !isFrozen) {
                    triggerSampling();
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (isEnabled && !isSuspended && !isFrozen) {
                    triggerSampling();
                }
            }

            @Override
            public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
                if (isEnabled && !isSuspended && !isFrozen) {
                    triggerSampling();
                }
            }
        };

        mainPlayer.addListener(mainPlayerListener);
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;

        mainHandler.post(() -> {
            boolean visible = enabled && !isSuspended;
            if (ambientContainer != null) {
                ambientContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
            if (ambientLightView != null) {
                ambientLightView.setVisibility(visible ? View.VISIBLE : View.GONE);
            }

            if (enabled && !isSuspended && !isFrozen) {
                triggerSampling();
            } else {
                stopSampling();
            }
        });

        Log.d(TAG, "Ambient light " + (enabled ? "enabled" : "disabled"));
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void suspend() {
        this.isSuspended = true;
        stopSampling();
        mainHandler.post(() -> {
            if (ambientContainer != null) {
                ambientContainer.setVisibility(View.GONE);
            }
            if (ambientLightView != null) {
                ambientLightView.setVisibility(View.GONE);
            }
        });
    }

    public void freeze() {
        this.isFrozen = true;
        stopSampling();
    }

    public void unfreeze() {
        this.isFrozen = false;
        mainHandler.post(() -> {
            if (isEnabled && !isSuspended) {
                if (ambientContainer != null) {
                    ambientContainer.setVisibility(View.VISIBLE);
                }
                if (ambientLightView != null) {
                    ambientLightView.setVisibility(View.VISIBLE);
                }
                triggerSampling();
            }
        });
    }

    public void resume() {
        this.isSuspended = false;
        this.isFrozen = false;
        mainHandler.post(() -> {
            if (isEnabled) {
                if (ambientContainer != null) {
                    ambientContainer.setVisibility(View.VISIBLE);
                }
                if (ambientLightView != null) {
                    ambientLightView.setVisibility(View.VISIBLE);
                }
                triggerSampling();
            }
        });
    }

    public void refreshAmbientFrame() {
        if (isEnabled && !isSuspended && !isFrozen) {
            triggerSampling();
        }
    }

    private synchronized void triggerSampling() {
        if (workerHandler == null || !isEnabled || isSuspended || isFrozen) return;

        workerHandler.removeCallbacks(sampleRunnable);
        workerHandler.post(sampleRunnable);
    }

    private synchronized void stopSampling() {
        if (workerHandler != null) {
            workerHandler.removeCallbacks(sampleRunnable);
        }
    }

    public void onConfigurationChanged() {
        mainHandler.postDelayed(() -> {
            cachedTextureView = null;
            updateVideoBounds();
            refreshAmbientFrame();
        }, 100);
    }

    public void cleanup() {
        stopSampling();

        if (mainPlayer != null && mainPlayerListener != null) {
            mainPlayer.removeListener(mainPlayerListener);
            mainPlayerListener = null;
        }

        if (workerThread != null) {
            workerThread.quitSafely();
            workerThread = null;
            workerHandler = null;
        }

        if (fastBlurUtils != null) {
            fastBlurUtils.resetHistory();
        }
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
