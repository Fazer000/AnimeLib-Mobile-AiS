package com.example.animelib.controllers;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.media3.common.Player;

import com.example.animelib.R;
import com.example.animelib.api.ApiService;
import com.example.animelib.data.entity.DownloadedEpisodeEntity;
import com.example.animelib.data.entity.OfflineBookmarkEntity;
import com.example.animelib.managers.BookmarkManager;
import com.example.animelib.managers.EpisodesManager;
import com.example.animelib.managers.OfflineSyncManager;
import com.example.animelib.managers.PlayersManager;
import com.example.animelib.models.AnimeBookmarkResponse;
import com.example.animelib.models.EpisodeResponse;
import com.example.animelib.models.EpisodesListResponse;

/**
 * Контроллер для отслеживания прогресса просмотра и автосохранения закладок.
 */
public class PlayerProgressController {

    private static final String TAG = "PlayerProgressCtrl";

    private final Context context;
    private final ApiService apiService;

    private boolean isCurrentEpisodeMarkedViewed = false;
    private boolean autoBookmarkSaved = false;

    private final Handler viewProgressHandler = new Handler(Looper.getMainLooper());
    private Runnable viewProgressRunnable;

    public interface ProgressCallback {
        String getAnimeId();
        String getAnimeUrl();
        boolean isOfflineMode();
        Player getPlayer();
        EpisodesManager getEpisodesManager();
        PlayersManager getPlayersManager();
        ImageView getBookmarkButton();
        ImageView getPortraitBookmarkButton();
        void saveLatestViewOnExit();
        void safeRunOnUiThread(Runnable runnable);
    }

    private ProgressCallback callback;

    public PlayerProgressController(Context context, ApiService apiService) {
        this.context = context;
        this.apiService = apiService;
    }

    public void setCallback(ProgressCallback callback) {
        this.callback = callback;
    }

    public boolean isCurrentEpisodeMarkedViewed() {
        return isCurrentEpisodeMarkedViewed;
    }

    public void setCurrentEpisodeMarkedViewed(boolean viewed) {
        this.isCurrentEpisodeMarkedViewed = viewed;
    }

    public boolean isAutoBookmarkSaved() {
        return autoBookmarkSaved;
    }

    public void setAutoBookmarkSaved(boolean saved) {
        this.autoBookmarkSaved = saved;
    }

    public void resetProgressForNewEpisode() {
        isCurrentEpisodeMarkedViewed = false;
        autoBookmarkSaved = false;
    }

    public void startViewProgressTracking() {
        stopViewProgressTracking();
        viewProgressRunnable = new Runnable() {
            @Override
            public void run() {
                checkPlaybackViewProgress();
                if (callback != null) {
                    Player p = callback.getPlayer();
                    if (p != null && p.isPlaying() && !isCurrentEpisodeMarkedViewed) {
                        viewProgressHandler.postDelayed(this, 1000);
                    }
                }
            }
        };
        viewProgressHandler.post(viewProgressRunnable);
    }

    public void stopViewProgressTracking() {
        if (viewProgressRunnable != null) {
            viewProgressHandler.removeCallbacks(viewProgressRunnable);
            viewProgressRunnable = null;
        }
    }

    private void checkPlaybackViewProgress() {
        if (isCurrentEpisodeMarkedViewed || callback == null) return;
        Player p = callback.getPlayer();
        if (p == null) return;

        long duration = p.getDuration();
        long currentPos = p.getCurrentPosition();

        if (duration > 0 && currentPos >= (long) (duration * 0.60)) {
            isCurrentEpisodeMarkedViewed = true;
            Log.d(TAG, "60% view threshold reached: " + currentPos + "/" + duration + "ms");

            String animeId = callback.getAnimeId();

            int playerId = 0;
            PlayersManager pm = callback.getPlayersManager();
            if (pm != null && pm.getCurrentPlayerData() != null) {
                playerId = pm.getCurrentPlayerData().getId();
            }

            if (animeId != null && !animeId.isEmpty() && playerId > 0) {
                Log.d(TAG, "Enqueuing VIEW task for animeId: " + animeId + ", playerId: " + playerId);
                OfflineSyncManager.getInstance(context).enqueueViewTask(animeId, playerId);
            }
        }
    }

    public void autoSaveBookmark() {
        if (callback == null) return;

        saveLatestViewOnExit();

        EpisodesManager em = callback.getEpisodesManager();
        EpisodesListResponse.EpisodeItem currentEpisode = em != null ? em.getCurrentEpisode() : null;
        if (currentEpisode == null) return;

        Player p = callback.getPlayer();
        long currentPosition = p != null ? p.getCurrentPosition() : 0;
        if (currentPosition < 1000) return;

        boolean isOffline = callback.isOfflineMode();
        if (isOffline) {
            String animeId = callback.getAnimeId();
            if (animeId != null && apiService != null && apiService.getDatabaseManager() != null) {
                String timecode = ApiService.formatTimecode(currentPosition);
                apiService.getDatabaseManager().saveOfflineBookmark(animeId, currentEpisode.getId(), currentEpisode.getNumber(), timecode, currentPosition);
                autoBookmarkSaved = true;
            }
            return;
        }

        if (apiService == null || !apiService.isAuthorized()) return;

        PlayersManager pm = callback.getPlayersManager();
        EpisodeResponse.PlayerData currentPlayer = pm != null ? pm.getCurrentPlayerData() : null;
        if (currentPlayer == null || em == null) return;

        String animeUrl = callback.getAnimeUrl();
        String mediaSlug = (animeUrl != null && !animeUrl.isEmpty()) ? ApiService.extractMediaSlugFromUrl(animeUrl) : null;
        if (mediaSlug == null) return;

        em.getBookmarkManager().addBookmark(
                mediaSlug,
                currentPlayer,
                currentEpisode,
                currentPosition,
                new BookmarkManager.BookmarkAddCallback() {
                    @Override
                    public void onBookmarkAdded(int episodeId) {
                        autoBookmarkSaved = true;
                    }

                    @Override
                    public void onBookmarkError(String error) {
                        Log.e(TAG, "Failed to auto-save bookmark: " + error);
                    }
                },
                false
        );
    }

    public void saveLatestViewOnExit() {
        if (callback != null) {
            callback.saveLatestViewOnExit();
        }
    }

    public void updateBookmarkButtonColor(boolean isBookmarked) {
        if (callback == null) return;
        callback.safeRunOnUiThread(() -> {
            ImageView bmBtn = callback.getBookmarkButton();
            if (bmBtn != null) {
                bmBtn.setEnabled(true);
                bmBtn.setClickable(true);
                bmBtn.setAlpha(1.0f);
                int targetColor = isBookmarked ? context.getResources().getColor(R.color.bookmark_color) : context.getResources().getColor(R.color.white_color);
                animateImageColorFilter(bmBtn, targetColor);
            }

            ImageView portraitBmBtn = callback.getPortraitBookmarkButton();
            if (portraitBmBtn != null) {
                portraitBmBtn.setEnabled(true);
                portraitBmBtn.setClickable(true);
                portraitBmBtn.setFocusable(true);
                portraitBmBtn.setAlpha(1.0f);
                int targetColor = isBookmarked ? context.getResources().getColor(R.color.bookmark_color) : context.getResources().getColor(R.color.primary_text_color);
                animateImageColorFilter(portraitBmBtn, targetColor);
            }
        });
    }

    public void updateEpisodesListAfterBookmark() {
        if (callback == null) return;
        callback.safeRunOnUiThread(() -> {
            EpisodesManager em = callback.getEpisodesManager();
            if (em == null) return;

            if (callback.isOfflineMode()) {
                String animeId = callback.getAnimeId();
                if (animeId != null && apiService != null && apiService.getDatabaseManager() != null) {
                    OfflineBookmarkEntity bm = apiService.getDatabaseManager().getOfflineBookmarkSync(animeId);
                    if (bm != null) {
                        AnimeBookmarkResponse.BookmarkData bookmarkData = new AnimeBookmarkResponse.BookmarkData();
                        bookmarkData.setItemId(bm.getEpisodeId());
                        bookmarkData.setProgress(bm.getTimecode());
                        em.updateBookmarkInAdapter(bookmarkData);
                    }
                }
                return;
            }

            String animeUrl = callback.getAnimeUrl();
            String mediaSlug = (animeUrl != null && !animeUrl.isEmpty()) ? ApiService.extractMediaSlugFromUrl(animeUrl) : null;

            if (mediaSlug != null) {
                em.getBookmarkManager().fetchAnimeBookmark(mediaSlug, new BookmarkManager.AnimeBookmarkCallback() {
                    @Override
                    public void onBookmarkReceived(AnimeBookmarkResponse response) {
                        callback.safeRunOnUiThread(() -> {
                            if (response != null && response.getData() != null) {
                                em.updateBookmarkInAdapter(response.getData());
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Failed to update bookmark in episodes list: " + error);
                    }
                });
            }
        });
    }

    private static class ColorAnimHolder {
        int color;
        ValueAnimator animator;
    }

    private void animateImageColorFilter(ImageView imageView, int targetColor) {
        if (imageView == null) return;
        ColorAnimHolder holder = (ColorAnimHolder) imageView.getTag();
        if (holder == null) {
            holder = new ColorAnimHolder();
            holder.color = targetColor;
            imageView.setTag(holder);
            imageView.setColorFilter(targetColor);
            return;
        }

        if (holder.animator != null) {
            holder.animator.cancel();
            holder.animator = null;
        }

        if (holder.color == targetColor) {
            imageView.setColorFilter(targetColor);
            return;
        }

        final ColorAnimHolder finalHolder = holder;
        ValueAnimator anim = ValueAnimator.ofObject(new ArgbEvaluator(), holder.color, targetColor);
        anim.setDuration(220);
        anim.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            finalHolder.color = val;
            imageView.setColorFilter(val);
        });
        holder.animator = anim;
        anim.start();
    }
}
