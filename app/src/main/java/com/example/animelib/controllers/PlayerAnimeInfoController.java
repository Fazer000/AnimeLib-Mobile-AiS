package com.example.animelib.controllers;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.animelib.R;
import com.example.animelib.models.AnimeInfoResponse;
import com.example.animelib.util.ImageLoader;
import com.example.animelib.util.SkeletonHelper;

/**
 * Контроллер карточки информации об аниме и скелетонов загрузки.
 */
public class PlayerAnimeInfoController {

    private static final String TAG = "PlayerAnimeInfoCtrl";

    private View animeInfoPlaceholder;
    private ImageView animeInfoPoster;
    private TextView animeInfoTitle;
    private TextView animeInfoOriginalTitle;
    private TextView animeInfoYear;
    private TextView animeInfoType;
    private TextView animeInfoStatus;
    private TextView animeInfoRating;
    private TextView animeInfoEpisodes;
    private TextView animeInfoAge;
    private TextView animeInfoReleaseDate;
    private TextView animeInfoShikimori;

    private boolean hasShownInitialAnimeInfo = false;
    private String currentPosterUrl = "";
    private AnimeInfoResponse currentAnimeInfo;

    public void initViews(View rootView) {
        if (rootView == null) return;
        animeInfoPlaceholder = rootView.findViewById(R.id.animeInfoPlaceholder);
        animeInfoPoster = rootView.findViewById(R.id.animeInfoPoster);
        animeInfoTitle = rootView.findViewById(R.id.animeInfoTitle);
        animeInfoOriginalTitle = rootView.findViewById(R.id.animeInfoOriginalTitle);
        animeInfoYear = rootView.findViewById(R.id.animeInfoYear);
        animeInfoType = rootView.findViewById(R.id.animeInfoType);
        animeInfoStatus = rootView.findViewById(R.id.animeInfoStatus);
        animeInfoRating = rootView.findViewById(R.id.animeInfoRating);
        animeInfoEpisodes = rootView.findViewById(R.id.animeInfoEpisodes);
        animeInfoAge = rootView.findViewById(R.id.animeInfoAge);
        animeInfoReleaseDate = rootView.findViewById(R.id.animeInfoReleaseDate);
        animeInfoShikimori = rootView.findViewById(R.id.animeInfoShikimori);
    }

    public void showSkeletons() {
        if (animeInfoTitle != null) SkeletonHelper.showSkeleton(animeInfoTitle, 200);
        if (animeInfoOriginalTitle != null) SkeletonHelper.showSkeleton(animeInfoOriginalTitle, 140);
        if (animeInfoRating != null) SkeletonHelper.showSkeleton(animeInfoRating, 40);
        if (animeInfoEpisodes != null) SkeletonHelper.showSkeleton(animeInfoEpisodes, 80);
        if (animeInfoYear != null) SkeletonHelper.showSkeleton(animeInfoYear, 50);
        if (animeInfoAge != null) SkeletonHelper.showSkeleton(animeInfoAge, 50);
        if (animeInfoType != null) SkeletonHelper.showSkeleton(animeInfoType, 60);
        if (animeInfoStatus != null) SkeletonHelper.showSkeleton(animeInfoStatus, 70);
        if (animeInfoReleaseDate != null) SkeletonHelper.showSkeleton(animeInfoReleaseDate, 100);
        if (animeInfoShikimori != null) SkeletonHelper.showSkeleton(animeInfoShikimori, 40);
    }

    public void hideSkeletons(String defaultTitle) {
        if (animeInfoTitle != null) SkeletonHelper.hideSkeleton(animeInfoTitle, defaultTitle != null ? defaultTitle : "");
        if (animeInfoOriginalTitle != null) SkeletonHelper.hideSkeleton(animeInfoOriginalTitle, "");
        if (animeInfoRating != null) SkeletonHelper.hideSkeleton(animeInfoRating, "");
        if (animeInfoEpisodes != null) SkeletonHelper.hideSkeleton(animeInfoEpisodes, "");
        if (animeInfoYear != null) SkeletonHelper.hideSkeleton(animeInfoYear, "");
        if (animeInfoAge != null) SkeletonHelper.hideSkeleton(animeInfoAge, "");
        if (animeInfoType != null) SkeletonHelper.hideSkeleton(animeInfoType, "");
        if (animeInfoStatus != null) SkeletonHelper.hideSkeleton(animeInfoStatus, "");
        if (animeInfoReleaseDate != null) SkeletonHelper.hideSkeleton(animeInfoReleaseDate, "");
        if (animeInfoShikimori != null) SkeletonHelper.hideSkeleton(animeInfoShikimori, "");
    }

    public void displayAnimeInfo(Context context, AnimeInfoResponse animeInfo) {
        if (animeInfo == null || animeInfo.getData() == null) {
            Log.e(TAG, "displayAnimeInfo: animeInfo or data is null");
            return;
        }

        this.currentAnimeInfo = animeInfo;
        AnimeInfoResponse.Data data = animeInfo.getData();

        if (animeInfoTitle != null) {
            SkeletonHelper.hideSkeleton(animeInfoTitle, data.getRus_name() != null ? data.getRus_name() : "—");
        }

        if (animeInfoOriginalTitle != null) {
            if (!TextUtils.isEmpty(data.getEng_name())) {
                SkeletonHelper.hideSkeleton(animeInfoOriginalTitle, data.getEng_name());
                animeInfoOriginalTitle.setVisibility(View.VISIBLE);
            } else {
                SkeletonHelper.hideSkeleton(animeInfoOriginalTitle, "");
                animeInfoOriginalTitle.setVisibility(View.GONE);
            }
        }

        if (animeInfoYear != null) {
            String year = (data.getReleaseDate() != null && data.getReleaseDate().length() >= 4)
                    ? data.getReleaseDate().substring(0, 4) : "—";
            SkeletonHelper.hideSkeleton(animeInfoYear, year);
        }

        if (animeInfoType != null) {
            String type = (data.getType() != null && data.getType().getLabel() != null)
                    ? data.getType().getLabel().replace("TV ", "") : "—";
            SkeletonHelper.hideSkeleton(animeInfoType, type);
        }

        if (animeInfoStatus != null) {
            String statusLabel = (data.getStatus() != null && data.getStatus().getLabel() != null)
                    ? data.getStatus().getLabel() : "—";
            SkeletonHelper.hideSkeleton(animeInfoStatus, statusLabel);
        }

        if (animeInfoRating != null) {
            String ratingStr = (data.getRating() != null && data.getRating().getAverageFormated() != null)
                    ? data.getRating().getAverageFormated() : "—";
            SkeletonHelper.hideSkeleton(animeInfoRating, ratingStr);
        }

        if (animeInfoEpisodes != null) {
            String epStr = (data.getItems_count() != null && data.getItems_count().getUploaded() > 0)
                    ? (data.getItems_count().getUploaded() + " эпизодов") : "—";
            SkeletonHelper.hideSkeleton(animeInfoEpisodes, epStr);
        }

        if (animeInfoAge != null) {
            String ageStr = (data.getAgeRestriction() != null && data.getAgeRestriction().getLabel() != null)
                    ? data.getAgeRestriction().getLabel() : "—";
            SkeletonHelper.hideSkeleton(animeInfoAge, ageStr);
        }

        if (animeInfoReleaseDate != null) {
            String relStr = data.getReleaseDateString() != null ? data.getReleaseDateString() : "—";
            SkeletonHelper.hideSkeleton(animeInfoReleaseDate, relStr);
        }

        if (animeInfoShikimori != null) {
            String shikiStr = data.getShikimori_href() != null ? String.valueOf(data.getShiki_rate()) : "—";
            SkeletonHelper.hideSkeleton(animeInfoShikimori, shikiStr);
        }

        if (animeInfoPoster != null && data.getCover() != null) {
            String posterUrl = data.getCover().getDefaultUrl();
            if (posterUrl != null && !posterUrl.isEmpty()) {
                currentPosterUrl = posterUrl;
                ImageLoader.getInstance()
                        .loadInto(animeInfoPoster, posterUrl, R.drawable.ic_image_placeholder);
            } else {
                animeInfoPoster.setImageResource(R.drawable.ic_image_placeholder);
            }
        }
    }

    public void showPlaceholder() {
        if (animeInfoPlaceholder != null) {
            animeInfoPlaceholder.setVisibility(View.VISIBLE);
            animeInfoPlaceholder.setTranslationX(0);
        }
    }

    public void hidePlaceholderAnimated() {
        if (animeInfoPlaceholder != null && animeInfoPlaceholder.getVisibility() == View.VISIBLE) {
            animeInfoPlaceholder.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        if (animeInfoPlaceholder != null) {
                            animeInfoPlaceholder.setVisibility(View.GONE);
                            animeInfoPlaceholder.setAlpha(1f);
                        }
                    })
                    .start();
        }
    }

    public void hidePlaceholderImmediately() {
        if (animeInfoPlaceholder != null) {
            animeInfoPlaceholder.setVisibility(View.GONE);
        }
    }

    public boolean isHasShownInitialAnimeInfo() {
        return hasShownInitialAnimeInfo;
    }

    public void setHasShownInitialAnimeInfo(boolean hasShown) {
        this.hasShownInitialAnimeInfo = hasShown;
    }

    public String getCurrentPosterUrl() {
        return currentPosterUrl;
    }

    public AnimeInfoResponse getCurrentAnimeInfo() {
        return currentAnimeInfo;
    }
}
