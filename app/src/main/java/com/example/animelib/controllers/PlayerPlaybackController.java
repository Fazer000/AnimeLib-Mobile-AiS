package com.example.animelib.controllers;

import android.content.Context;
import android.util.Log;
import android.view.View;
import com.google.android.material.button.MaterialButton;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.ui.PlayerView;

import com.example.animelib.R;
import com.example.animelib.managers.AmbientLightManager;
import com.example.animelib.managers.GesturesManager;
import com.example.animelib.managers.TimecodeManager;

public class PlayerPlaybackController {

    private static final String TAG = "PlayerPlaybackController";

    public interface PlaybackCallback {
        Context getPlayerContext();
        PlayerAudioController getPlayerAudioController();
        AmbientLightManager getAmbientLightManager();
        GesturesManager getGesturesManager();
        TimecodeManager getTimecodeManager();
        HttpDataSource.Factory getHttpDataSourceFactory();
        void onFirstFrameRendered();
        void onPlaybackStateChanged(int state, boolean playWhenReady);
        void onPlayerError(PlaybackException error);
        void safeRunOnUiThread(Runnable runnable);
    }

    private final Context context;
    private final PlayerView playerView;
    private final HttpDataSource.Factory httpDataSourceFactory;
    private ExoPlayer player;
    private PlaybackCallback callback;
    private boolean isFirstFrameRendered = false;

    public PlayerPlaybackController(Context context, PlayerView playerView, HttpDataSource.Factory httpDataSourceFactory) {
        this.context = context;
        this.playerView = playerView;
        this.httpDataSourceFactory = httpDataSourceFactory;
    }

    public void setCallback(PlaybackCallback callback) {
        this.callback = callback;
    }

    public ExoPlayer getPlayer() {
        return player;
    }

    private HttpDataSource.Factory getEffectiveHttpDataSourceFactory() {
        if (httpDataSourceFactory != null) {
            return httpDataSourceFactory;
        }
        if (callback != null && callback.getHttpDataSourceFactory() != null) {
            return callback.getHttpDataSourceFactory();
        }
        return new DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android 14; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36");
    }

    public ExoPlayer initializePlayer(String videoUrl, MediaItem mediaItem, int resizeMode, boolean playWhenReady) {
        if (context == null || videoUrl == null) return player;

        Context playerContext = callback != null ? callback.getPlayerContext() : context;
        if (playerContext == null) playerContext = context;

        isFirstFrameRendered = false;

        PlayerAudioController audioController = callback != null ? callback.getPlayerAudioController() : null;

        if (player == null) {
            com.example.animelib.util.SurroundRenderersFactory rf = new com.example.animelib.util.SurroundRenderersFactory(
                    playerContext,
                    audioController != null ? audioController.getSurroundAudioProcessor() : null);

            ExoPlayer.Builder builder = new ExoPlayer.Builder(playerContext, rf)
                    .setSeekBackIncrementMs(10000)
                    .setSeekForwardIncrementMs(10000);

            player = builder.build();
            if (playerView != null) {
                playerView.setPlayer(player);
                playerView.setResizeMode(resizeMode);
            }
            setupPlayerListener();
        } else {
            player.stop();
            player.clearMediaItems();
        }

        HttpDataSource.Factory dsFactory = getEffectiveHttpDataSourceFactory();

        MediaSource mediaSource;
        if (videoUrl.contains(".m3u8") || videoUrl.contains("hls")) {
            mediaSource = new HlsMediaSource.Factory(dsFactory)
                    .createMediaSource(mediaItem != null ? mediaItem : MediaItem.fromUri(videoUrl));
        } else {
            mediaSource = new ProgressiveMediaSource.Factory(dsFactory)
                    .createMediaSource(mediaItem != null ? mediaItem : MediaItem.fromUri(videoUrl));
        }

        player.setMediaSource(mediaSource);

        AmbientLightManager ambientLightManager = callback != null ? callback.getAmbientLightManager() : null;
        if (ambientLightManager != null) {
            ambientLightManager.setPlayer(player, mediaItem != null ? mediaItem : MediaItem.fromUri(videoUrl), videoUrl);
        }

        if (audioController != null) {
            audioController.attachPlayer(player);
        }

        GesturesManager gesturesManager = callback != null ? callback.getGesturesManager() : null;
        if (gesturesManager != null) {
            gesturesManager.updatePlayer(player);
        }

        TimecodeManager timecodeManager = callback != null ? callback.getTimecodeManager() : null;
        if (timecodeManager != null && playerView != null) {
            View controllerView = playerView.findViewById(R.id.exo_controller);
            MaterialButton skipButton = controllerView != null ? controllerView.findViewById(R.id.skipSegmentButton) : null;
            timecodeManager.initializeViews(player, playerView, skipButton);
        }

        player.setPlayWhenReady(playWhenReady);
        player.prepare();

        return player;
    }

    public ExoPlayer initializeHlsPlayer(String hlsUrl, MediaItem mediaItem, int resizeMode, boolean playWhenReady) {
        return initializePlayer(hlsUrl, mediaItem, resizeMode, playWhenReady);
    }

    private void setupPlayerListener() {
        if (player == null) return;
        player.addListener(new Player.Listener() {
            @Override
            public void onRenderedFirstFrame() {
                isFirstFrameRendered = true;
                if (callback != null) {
                    callback.onFirstFrameRendered();
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (callback != null) {
                    callback.onPlaybackStateChanged(playbackState, player != null && player.getPlayWhenReady());
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e(TAG, "ExoPlayer error", error);
                if (callback != null) {
                    callback.onPlayerError(error);
                }
            }
        });
    }

    public void release() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
