package com.example.animelib.controllers;

import android.content.Context;
import android.util.Log;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.ui.PlayerView;

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

    public ExoPlayer initializePlayer(String videoUrl, MediaItem mediaItem, int resizeMode, boolean playWhenReady) {
        if (context == null || videoUrl == null) return player;

        Context playerContext = callback != null ? callback.getPlayerContext() : context;
        if (playerContext == null) playerContext = context;

        isFirstFrameRendered = false;

        if (player == null) {
            ExoPlayer.Builder builder = new ExoPlayer.Builder(playerContext);
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

        MediaSource mediaSource;
        if (videoUrl.contains(".m3u8") || videoUrl.contains("hls")) {
            mediaSource = new HlsMediaSource.Factory(httpDataSourceFactory)
                    .createMediaSource(mediaItem != null ? mediaItem : MediaItem.fromUri(videoUrl));
        } else {
            mediaSource = new ProgressiveMediaSource.Factory(httpDataSourceFactory)
                    .createMediaSource(mediaItem != null ? mediaItem : MediaItem.fromUri(videoUrl));
        }

        player.setMediaSource(mediaSource);

        AmbientLightManager ambientLightManager = callback != null ? callback.getAmbientLightManager() : null;
        if (ambientLightManager != null) {
            ambientLightManager.setPlayer(player);
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
