package com.example.animelib;


import java.io.File;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Rational;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowManager;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Bitmap;
import com.example.animelib.util.CustomTypefaceSpan;
import com.example.animelib.util.FontResolver;
import com.example.animelib.util.CustomToast;
import com.example.animelib.util.SkeletonHelper;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import android.text.Spanned;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.text.style.ReplacementSpan;
import android.graphics.Typeface;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Canvas;
import android.graphics.Paint;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.text.Cue;
import androidx.media3.common.text.CueGroup;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.C;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.TrackSelector;
import androidx.media3.ui.CaptionStyleCompat;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.SubtitleView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.animelib.models.AnimeInfoResponse;

import com.example.animelib.api.ApiService;
import com.example.animelib.settings.SettingsBottomSheet;
import com.example.animelib.managers.BookmarkManager;
import com.example.animelib.managers.CommentsManager;
import com.example.animelib.managers.EpisodesManager;
import com.example.animelib.managers.PlayersManager;
import com.example.animelib.managers.GesturesManager;
import com.example.animelib.managers.VerticalGesturesManager;
import com.example.animelib.managers.TimecodeManager;
import com.example.animelib.managers.AmbientLightManager;
import com.example.animelib.adapters.HorizontalRelatedTitlesAdapter;
import com.example.animelib.managers.RelatedTitlesManager;
import com.example.animelib.util.ThemeUtils;
import com.example.animelib.models.EpisodeResponse;
import com.example.animelib.models.EpisodesListResponse;
import com.example.animelib.models.KodikResponse;
import com.example.animelib.models.RelatedTitlesResponse;
import com.example.animelib.services.DownloadService;
import com.example.animelib.settings.QualityBottomSheet;
import com.example.animelib.ui.TitleWebViewBottomSheet;
import com.example.animelib.ui.VideoUrlHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;


@UnstableApi
public class VideoPlayerActivity extends AppCompatActivity {
    public static final String EXTRA_VIDEO_URL = "video_url";
    public static final String EXTRA_ANIME_URL = "anime_url";

    private PlayerView playerView;
    private ExoPlayer player;
    private View loadingOverlay;
    private androidx.media3.ui.DefaultTimeBar timeBar;
    private View controllerView;

    private String currentVideoUrl;
    private String animeUrl;
    private Map<String, String> videoQualities;

    private ExecutorService executor;
    private DefaultHttpDataSource.Factory httpDataSourceFactory;
    private ApiService apiService;

    // Menu components
    private ImageButton ibClosePlayer;
    private ImageButton menuToggleButton;
    private ImageButton settingsButton;
    private TextView settingsQualityTag;
    private ImageButton menuToggleFullscreen;
    private LinearLayout slidingMenuPanel;
    private View menuLoadingIndicator;
    private View menuLoadingOverlay;
    
    // Draggable panels
    private FrameLayout playerContainer;
    private float currentCornerRadiusPx = 0f;
    private float currentOutlineLeft = 0f;
    private float currentOutlineTop = 0f;
    private float currentOutlineRight = 0f;
    private float currentOutlineBottom = 0f;
    private com.example.animelib.ui.DraggableSidePanel menuPanelContainer;
    private com.example.animelib.ui.DraggableSidePanel commentsPanelContainer;
    
    // Anime info placeholder
    private View animeInfoPlaceholder;
    private boolean hasShownInitialAnimeInfo = false;
    private String currentPosterUrl = "";
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
    
    // Next episode overlay
    private View nextEpisodeOverlay;
    private TextView nextEpisodeNumber;
    private TextView nextEpisodeCountdown;
    private com.google.android.material.button.MaterialButton cancelNextEpisodeButton;
    private com.google.android.material.button.MaterialButton playNextEpisodeButton;
    private Handler nextEpisodeHandler;
    private Runnable nextEpisodeRunnable;
    private int countdownSeconds = 7;
    
    // UI components for managers
    private View menuOverlay;
    private ImageButton closeMenuButton;
    private View commentsPanel;
    private ImageButton closeCommentsButton;
    private RecyclerView commentsRecyclerView;
    private View commentsLoadingOverlay;
    private View commentsOptionsButton;
    private TextView emptyCommentsText;
    private TextView seekPreviewText;
    private TextView holdSpeedToast;
    private ImageButton pipButton;
    private RecyclerView episodesHorizontalRecyclerView;
    private ImageButton commentsButton;
    private ImageButton bookmarkButton;
    private View skipIndicatorLeft;
    private View skipIndicatorRight;
    private View playerBufferingIndicator;
    private volatile boolean isVideoLoading = false;
    private volatile boolean hasRenderedFirstFrame = false;
    private volatile boolean isSeeking = false;
    private volatile boolean isScrubbingTimeBar = false;
    private int currentControlState = -1; // -1 = uninitialized, 0 = play, 1 = pause, 2 = loading
    private final Handler seekResetHandler = new Handler(Looper.getMainLooper());
    private Player.Listener playerEventListener;

    // Comments manager
    private CommentsManager commentsManager;
    private com.example.animelib.data.DatabaseManager databaseManager;
    private SettingsBottomSheet currentSettingsBottomSheet;
    private String currentVideoDomain = VideoUrlHelper.DOMAIN_MAIN;
    private androidx.appcompat.app.AlertDialog currentErrorDialog;

    // Orientation listener for autorotation
    private android.view.OrientationEventListener orientationEventListener;
    private int lastPhysicalOrientation = -1;
    private boolean manualOrientationOverride = false;

    // Episodes manager
    private EpisodesManager episodesManager;
    
    // Episodes UI components (for EpisodesManager)
    private ImageButton episodesMenuButton;
    
    // Related titles components
    private FrameLayout relatedTitlesOverlay;
    private View relatedTitlesDimOverlay;
    private RecyclerView relatedTitlesRecyclerView;
    private HorizontalRelatedTitlesAdapter relatedTitlesAdapter;
    private RelatedTitlesManager relatedTitlesManager;

    // Players manager
    private PlayersManager playersManager;
    
    // Gestures manager
    private GesturesManager gesturesManager;
    private VerticalGesturesManager verticalGesturesManager;
    
    // Timecode manager
    private TimecodeManager timecodeManager;

    // View progress tracking (60% threshold)
    private boolean isCurrentEpisodeMarkedViewed = false;
    private final Handler viewProgressHandler = new Handler(Looper.getMainLooper());
    private Runnable viewProgressRunnable;
    
    // Ambient light manager
    private AmbientLightManager ambientLightManager;
    private com.example.animelib.ui.AmbientVignetteOverlayView ambientVignetteOverlay;
    private float lastCornerRadiusPx = -1f;

    // Video filters manager
    private com.example.animelib.managers.VideoFiltersManager videoFiltersManager;
    private float filterBrightness = 0f;
    private float filterContrast = 100f;
    private float filterSaturation = 100f;
    private float filterGamma = 1.0f;
    private float filterHue = 0f;

    // Surround sound manager
    private com.example.animelib.managers.SurroundSoundManager surroundSoundManager;
    private boolean enableSurroundSound = true;
    private int surroundMode = 0;
    private float surroundSpatialWidth = 1.0f;
    private float surroundDialogueBoost = 1.0f;
    private float surroundBassBoost = 1.0f;
    private float surroundTrebleBoost = 1.0f;

    // Subtitle settings
    private boolean subtitlesEnabled = true;
    private String subtitleFormat = "ass";
    private float subtitleTextSize = 18f;
    private int subtitleTextColor = 0xFFFFFFFF;
    private int subtitleBackgroundColor = 0x00000000;
    private int subtitleEdgeType = androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_OUTLINE;
    private int subtitleEdgeColor = 0xFF000000;

    // Picture-in-Picture support
    private static final String ACTION_PIP_PLAY_PAUSE = "com.example.animelib.PIP_PLAY_PAUSE";
    private static final String ACTION_PIP_REWIND = "com.example.animelib.PIP_REWIND";
    private static final String ACTION_PIP_FAST_FORWARD = "com.example.animelib.PIP_FAST_FORWARD";

    private static final int PIP_REQ_PLAY_PAUSE = 101;
    private static final int PIP_REQ_REWIND = 102;
    private static final int PIP_REQ_FAST_FORWARD = 103;

    private BroadcastReceiver pipBroadcastReceiver;
    private boolean isInPictureInPictureMode = false;
    private boolean wasCommentsVisibleBeforePiP = false;
    private boolean wasPlayingBeforeBackground = false;

    // Buffering monitor for MP4 and HLS streams
    private final Handler bufferingMonitorHandler = new Handler(Looper.getMainLooper());
    private final Runnable bufferingMonitorRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null && !isFinishing()) {
                updatePlayPauseAndLoadingState(false);
                if (player.getPlayWhenReady() && player.getPlaybackState() != Player.STATE_ENDED) {
                    bufferingMonitorHandler.postDelayed(this, 300);
                }
            }
        }
    };

    private void startBufferingMonitoring() {
        bufferingMonitorHandler.removeCallbacks(bufferingMonitorRunnable);
        bufferingMonitorHandler.post(bufferingMonitorRunnable);
    }

    private void stopBufferingMonitoring() {
        bufferingMonitorHandler.removeCallbacks(bufferingMonitorRunnable);
    }

    // Episode navigation buttons (for EpisodesManager)
    private ImageButton prevEpisodeButton;
    private ImageButton nextEpisodeButton;
    
    // Other UI components
    private View playersControlBar;
    private TextView animeTitleView;
    private TextView currentEpisodeNumberView;
    private TextView currentTeamName;
    private TextView currentEpisodeName;

    // Controller visibility state
    private boolean isControllerVisible = false;
    
    // Fullscreen state
    private boolean isFullscreenMode = false;
    private int currentResizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT;

    // Player data is now managed by PlayersManager
    private boolean isOfflineMode = false;
    private boolean isFirstOfflineLaunch = true;
    private final java.util.Map<String, com.example.animelib.data.entity.DownloadedEpisodeEntity> offlineEpisodesMap = new java.util.HashMap<>();
    private KodikResponse currentKodikResponse;
    private String currentAnimeId;
    private AnimeInfoResponse currentAnimeInfo;
    private long bookmarkTimecode = 0; // Таймкод из закладки в миллисекундах
    private long savedPlayerPosition = 0; // Сохраненная позиция при смене плеера
    private boolean autoBookmarkSaved = false; // Флаг для предотвращения дублирования автосохранения

    // Portrait UI components
    private View portraitVoiceoverPlayerButton;
    private TextView tvPortraitVoiceover;
    private TextView tvPortraitPlayer;
    private ImageView ivPortraitVoiceoverChevron;
    private ImageButton portraitBookmarkButton;
    private ImageButton portraitDownloadButton;
    private RecyclerView portraitEpisodesRecyclerView;
    private TextView tvPortraitAnimeTitle;
    private TextView tvPortraitEpisodeTitle;
    private View btnPortraitSortComments;
    private TextView tvPortraitSortComments;
    private View btnPortraitCommentRules;
    private EditText portraitCommentInputField;
    private RecyclerView portraitCommentsRecyclerView;
    private TextView tvPortraitEmptyComments;
    private View portraitCommentsLoadingOverlay;
    private androidx.core.widget.NestedScrollView portraitScrollView;
    private FloatingActionButton btnPortraitScrollToTop;
    private View portraitDownloadProgressContainer;
    private View portraitDownloadProgressCard;
    private TextView tvPortraitDownloadPercent;
    private View portraitRelatedTitlesContainer;
    private RecyclerView portraitRelatedTitlesRecyclerView;
    private HorizontalRelatedTitlesAdapter portraitRelatedTitlesAdapter;

    // User preferences are now managed by PlayersManager
    private String preferredQuality;
    private ImageButton downloadButton;
    private ImageButton downloadButtonTop;
    private ImageButton btnDownloadFromMenu;
    private TextView downloadProgressText;
    private boolean isDownloading = false;
    private int currentDownloadPercent = 0;
    private boolean enable4K = false;
    private boolean enableAmbientLight = true;
    private boolean autoPlay = true;
    private int longSkipDuration = 85; // seconds
    private int currentTheme = ThemeUtils.THEME_SYSTEM;
    private boolean isNewEpisodeSelection = true;
    private boolean autoPlayOnPrepare = true;

    // Menu state
    // isMenuVisible is now managed by PlayersManager
    private int menuWidth = 300; // dp

    private final int controllerShowTimeoutMs = 4000;
    private boolean shouldAutoHideControls = true; // Контроль автоматического скрытия

    public static void startFromAnimePage(Activity context, String animeUrl) {
        Intent intent = new Intent(context, VideoPlayerActivity.class);
        intent.putExtra(EXTRA_ANIME_URL, animeUrl);
        if (context instanceof VideoPlayerActivity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        context.startActivity(intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        // Stop current player if active
        if (player != null) {
            try {
                player.stop();
                player.clearMediaItems();
            } catch (Exception e) {
                Log.w("VideoPlayer", "Error stopping player in onNewIntent", e);
            }
        }

        // Process new intent data
        String localFilePath = intent.getStringExtra("EXTRA_LOCAL_FILE_PATH");
        currentVideoUrl = intent.getStringExtra(EXTRA_VIDEO_URL);
        animeUrl = intent.getStringExtra(EXTRA_ANIME_URL);

        if (localFilePath != null) {
            isOfflineMode = true;
            String animeTitle = intent.getStringExtra("EXTRA_ANIME_TITLE");
            String episodeTitle = intent.getStringExtra("EXTRA_EPISODE_TITLE");
            playOfflineFile(localFilePath, animeTitle, episodeTitle);
        } else if (currentVideoUrl != null) {
            initializePlayer();
        } else if (animeUrl != null) {
            showLoading("Загрузка видео...");
            loadAnimeFromUrl(animeUrl);
        }
    }

    public static void startForOfflineEpisode(Context context, String localFilePath, String animeTitle, String episodeTitle, String animeId, String episodeNumber) {
        Intent intent = new Intent(context, VideoPlayerActivity.class);
        intent.putExtra("EXTRA_LOCAL_FILE_PATH", localFilePath);
        intent.putExtra("EXTRA_ANIME_TITLE", animeTitle);
        intent.putExtra("EXTRA_EPISODE_TITLE", episodeTitle);
        intent.putExtra("EXTRA_ANIME_ID", animeId);
        intent.putExtra("EXTRA_EPISODE_NUMBER", episodeNumber);
        context.startActivity(intent);
    }

    public static void startForOfflineEpisode(Context context, String localFilePath, String animeTitle, String episodeTitle) {
        startForOfflineEpisode(context, localFilePath, animeTitle, episodeTitle, null, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_AnimeLIB_VideoPlayer);
        setContentView(R.layout.activity_video_player);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        playerView = findViewById(R.id.playerView);
        playerContainer = findViewById(R.id.playerContainer);
        if (playerContainer != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                playerContainer.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        int l = Math.round(currentOutlineLeft);
                        int t = Math.round(currentOutlineTop);
                        int r = Math.round(currentOutlineRight);
                        int b = Math.round(currentOutlineBottom);
                        if (r > l && b > t && (l > 0 || t > 0 || r < view.getWidth() || b < view.getHeight() || currentCornerRadiusPx > 0)) {
                            if (currentCornerRadiusPx > 0) {
                                outline.setRoundRect(l, t, r, b, currentCornerRadiusPx);
                            } else {
                                outline.setRect(l, t, r, b);
                            }
                        } else {
                            outline.setRect(0, 0, view.getWidth(), view.getHeight());
                        }
                    }
                });
                playerContainer.setClipToOutline(true);
            }
            playerContainer.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                    boolean isPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
                    if (isPortrait) {
                        applyPlayerSidePanelTransform(0f);
                    }
                }
            });
        }
        loadingOverlay = findViewById(R.id.loadingOverlay);

        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.setFitsSystemWindows(false);
        }

        updateControllerAutoHide();

        playerView.setControllerAnimationEnabled(false);

        executor = Executors.newSingleThreadExecutor();
        apiService = new ApiService(this);

        loadAndApplyTheme();

        commentsManager = new CommentsManager(this, apiService);

        episodesManager = new EpisodesManager(this, apiService);

        episodesManager.setPlayerControlsCallback(shouldAutoHide -> {
            shouldAutoHideControls = shouldAutoHide;
            updateControllerAutoHide();
        });

        playersManager = new PlayersManager(this, apiService);

        gesturesManager = new GesturesManager(this);
        verticalGesturesManager = new VerticalGesturesManager(this);

        gesturesManager.setVerticalGesturesManager(verticalGesturesManager);
        verticalGesturesManager.setGesturesManager(gesturesManager);

        timecodeManager = new TimecodeManager(this);
        surroundSoundManager = new com.example.animelib.managers.SurroundSoundManager(this);

        initOrientationEventListener();

        try {
            android.webkit.WebView webView = new android.webkit.WebView(this);
            webView.clearCache(true);
            webView.clearHistory();
            webView.destroy();
        } catch (Exception e) {
            Log.w("VideoPlayer", "Failed to clear WebView cache", e);
        }

        // Load subtitle settings early before background initialization
        subtitlesEnabled = apiService.loadSubtitlesEnabledSetting();
        subtitleFormat = apiService.loadSubtitleFormatSetting();

        executor.execute(() -> {
            enable4K = apiService.load4KSetting();
            enableAmbientLight = apiService.loadAmbientLightSetting();
            enableSurroundSound = apiService.loadSurroundSoundSetting();
            surroundMode = apiService.loadSurround3DMode();
            surroundSpatialWidth = apiService.loadSurroundSpatialWidth();
            surroundDialogueBoost = apiService.loadSurroundDialogueBoost();
            surroundBassBoost = apiService.loadSurroundBassBoost();
            surroundTrebleBoost = apiService.loadSurroundTrebleBoost();
            autoPlay = apiService.loadAutoPlaySetting();
            isNewEpisodeSelection = true;
            autoPlayOnPrepare = autoPlay;
            longSkipDuration = apiService.loadLongSkipDurationSetting();
            currentTheme = apiService.loadThemeSetting();
            subtitlesEnabled = apiService.loadSubtitlesEnabledSetting();
            subtitleFormat = apiService.loadSubtitleFormatSetting();
            subtitleTextSize = apiService.loadSubtitleTextSizeSetting();
            subtitleTextColor = apiService.loadSubtitleTextColorSetting();
            subtitleBackgroundColor = apiService.loadSubtitleBackgroundColorSetting();
            subtitleEdgeType = apiService.loadSubtitleEdgeTypeSetting();
            subtitleEdgeColor = apiService.loadSubtitleEdgeColorSetting();
            float[] filters = apiService.loadVideoFilters();
            if (filters != null && filters.length >= 5) {
                filterBrightness = filters[0];
                filterContrast = filters[1];
                filterSaturation = filters[2];
                filterGamma = filters[3];
                filterHue = filters[4];
            }
            Log.d("VideoPlayer", "Loaded settings - 4K: " + enable4K + ", AmbientLight: " + enableAmbientLight + ", SurroundSound: " + enableSurroundSound + " (mode=" + surroundMode + "), AutoPlay: " + autoPlay + ", SkipDuration: " + longSkipDuration + ", Theme: " + currentTheme + ", Subtitles: " + subtitlesEnabled + " (" + subtitleFormat + ")");

            runOnUiThread(() -> {
                if (playersManager != null) {
                    playersManager.setEnable4K(enable4K);
                }
                if (ambientLightManager != null) {
                    ambientLightManager.setEnabled(enableAmbientLight);
                }
                if (videoFiltersManager != null) {
                    videoFiltersManager.setFilters(filterBrightness, filterContrast, filterSaturation, filterGamma, filterHue);
                }
                if (surroundSoundManager != null) {
                    surroundSoundManager.setEnabled(enableSurroundSound);
                    surroundSoundManager.setSpatialMode(surroundMode);
                    surroundSoundManager.setSpatialWidth(surroundSpatialWidth);
                    surroundSoundManager.setDialogueBoost(surroundDialogueBoost);
                    surroundSoundManager.setBassBoostLevel(surroundBassBoost);
                    surroundSoundManager.setTrebleBoostLevel(surroundTrebleBoost);
                }
            });
        });

        // Initialize HTTP data source with custom headers for video requests
        httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android 14; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")
                .setDefaultRequestProperties(Map.of(
                        "Referer", "https://v3.animelib.org/",
                        "Accept", "video/mp4,video/*,*/*",
                        "Accept-Encoding", "identity;q=1, *;q=0",
                        "Accept-Language", "ru,en;q=0.9,de;q=0.8,zh;q=0.7",
                        "Origin", "https://v3.animelib.org",
                        "Sec-Fetch-Dest", "video",
                        "Sec-Fetch-Mode", "cors",
                        "Sec-Fetch-Site", "cross-site",
                        "Priority", "i"
                ));

        Log.d("VideoPlayer", "Initialized HTTP data source with custom headers: User-Agent, Referer, Accept, Accept-Encoding, Accept-Language, Origin, Sec-Fetch-*");

        // Get data from intent
        String localFilePath = getIntent().getStringExtra("EXTRA_LOCAL_FILE_PATH");
        currentVideoUrl = getIntent().getStringExtra(EXTRA_VIDEO_URL);
        animeUrl = getIntent().getStringExtra(EXTRA_ANIME_URL);

        if (localFilePath != null) {
            isOfflineMode = true;
        }

        // Setup fullscreen
        setupFullscreen();

        // Setup menu
        setupMenu();

        setupPipReceiver();

        if (localFilePath != null) {
            String animeTitle = getIntent().getStringExtra("EXTRA_ANIME_TITLE");
            String episodeTitle = getIntent().getStringExtra("EXTRA_EPISODE_TITLE");
            playOfflineFile(localFilePath, animeTitle, episodeTitle);
        } else if (currentVideoUrl != null) {
            // Direct video URL provided - start player immediately
            initializePlayer();
        } else if (animeUrl != null) {
            // Anime page URL provided - fetch video links first
            showLoading("Загрузка видео...");
            loadAnimeFromUrl(animeUrl);
        } else {
            CustomToast.showWarning(this, "Ошибка: Не указан URL видео или страницы аниме");
            finish();
        }
    }

    /**
     * Безопасно вызывает код в главном потоке
     */
    private void safeRunOnUiThread(Runnable runnable) {
        try {
            runOnUiThread(runnable);
        } catch (Exception e) {
            Log.e("VideoPlayer", "Error calling UI thread", e);
            // Fallback - вызываем в текущем потоке
            try {
                runnable.run();
            } catch (Exception ex) {
                Log.e("VideoPlayer", "Error in fallback callback", ex);
            }
        }
    }

    private void loadAndApplyTheme() {
        executor.execute(() -> {
            try {
                int sharedPrefTheme = ThemeUtils.getSavedThemePreference(this);
                int themeMode = apiService.loadThemeSetting();
                Log.d("VideoPlayerTheme", "Loaded theme - SharedPref: " + sharedPrefTheme + ", DB: " + themeMode);
                
                int finalTheme = (sharedPrefTheme >= 0 && sharedPrefTheme <= 2) ? sharedPrefTheme : themeMode;
                
                // Применяем тему в главном потоке
                safeRunOnUiThread(() -> {
                    ThemeUtils.applyThemeToActivity(VideoPlayerActivity.this, finalTheme);
                    setupFullscreen();
                    checkAndUpdateOrientation();
                    Log.d("VideoPlayerTheme", "Theme applied on startup: " + finalTheme);
                });
                
            } catch (Exception e) {
                Log.e("VideoPlayerTheme", "Failed to load and apply theme", e);
                safeRunOnUiThread(() -> {
                    ThemeUtils.applyThemeToActivity(VideoPlayerActivity.this, ThemeUtils.getSavedThemePreference(VideoPlayerActivity.this));
                    setupFullscreen();
                    checkAndUpdateOrientation();
                });
            }
        });
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, android.content.res.Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        this.isInPictureInPictureMode = isInPictureInPictureMode;

        if (isInPictureInPictureMode) {
            // Entering PiP mode
            if (player != null && !player.isPlaying()) {
                player.play();
                Log.d("VideoPlayer", "Resumed playback in PiP mode");
            }
            wasCommentsVisibleBeforePiP = commentsManager.isCommentsVisible();
            hideAllUI();
            updatePictureInPictureParams();
            Log.d("VideoPlayer", "Entered Picture-in-Picture mode");
        } else {
            // Exiting PiP mode
            showAllUI();
            Log.d("VideoPlayer", "Exited Picture-in-Picture mode");
        }
    }

    private void setupPipReceiver() {
        pipBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || intent.getAction() == null || player == null) return;
                String action = intent.getAction();
                if (ACTION_PIP_PLAY_PAUSE.equals(action)) {
                    if (player.isPlaying()) {
                        player.pause();
                    } else {
                        player.play();
                    }
                    updatePictureInPictureParams();
                } else if (ACTION_PIP_REWIND.equals(action)) {
                    long currentPos = player.getCurrentPosition();
                    long newPos = Math.max(0, currentPos - 10000);
                    startSeekingState();
                    player.seekTo(newPos);
                    scheduleEndSeekingState(600);
                } else if (ACTION_PIP_FAST_FORWARD.equals(action)) {
                    long currentPos = player.getCurrentPosition();
                    long duration = player.getDuration();
                    long newPos = duration > 0 ? Math.min(duration, currentPos + 10000) : currentPos + 10000;
                    startSeekingState();
                    player.seekTo(newPos);
                    scheduleEndSeekingState(600);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PIP_PLAY_PAUSE);
        filter.addAction(ACTION_PIP_REWIND);
        filter.addAction(ACTION_PIP_FAST_FORWARD);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pipBroadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(pipBroadcastReceiver, filter);
        }
    }

    public List<RemoteAction> buildPipActions() {
        List<RemoteAction> actions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Rewind 10s
            Intent rewindIntent = new Intent(ACTION_PIP_REWIND).setPackage(getPackageName());
            PendingIntent rewindPendingIntent = PendingIntent.getBroadcast(
                    this, PIP_REQ_REWIND, rewindIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            Icon rewindIcon = Icon.createWithResource(this, R.drawable.ic_pip_rewind);
            actions.add(new RemoteAction(rewindIcon, "10 сек назад", "Перемотать на 10 секунд назад", rewindPendingIntent));

            // Play / Pause
            boolean isPlaying = player != null && player.isPlaying();
            Intent playPauseIntent = new Intent(ACTION_PIP_PLAY_PAUSE).setPackage(getPackageName());
            PendingIntent playPausePendingIntent = PendingIntent.getBroadcast(
                    this, PIP_REQ_PLAY_PAUSE, playPauseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            Icon playPauseIcon = Icon.createWithResource(
                    this,
                    isPlaying ? R.drawable.ic_pip_pause : R.drawable.ic_pip_play
            );
            String playPauseTitle = isPlaying ? "Пауза" : "Воспроизведение";
            actions.add(new RemoteAction(playPauseIcon, playPauseTitle, playPauseTitle, playPausePendingIntent));

            // Fast Forward 10s
            Intent ffIntent = new Intent(ACTION_PIP_FAST_FORWARD).setPackage(getPackageName());
            PendingIntent ffPendingIntent = PendingIntent.getBroadcast(
                    this, PIP_REQ_FAST_FORWARD, ffIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            Icon ffIcon = Icon.createWithResource(this, R.drawable.ic_pip_fast_forward);
            actions.add(new RemoteAction(ffIcon, "10 сек вперед", "Перемотать на 10 секунд вперед", ffPendingIntent));
        }
        return actions;
    }

    public void updatePictureInPictureParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode) {
            try {
                PictureInPictureParams params = new PictureInPictureParams.Builder()
                        .setAspectRatio(new Rational(16, 9))
                        .setActions(buildPipActions())
                        .build();
                setPictureInPictureParams(params);
            } catch (Exception e) {
                Log.e("VideoPlayer", "Failed to update Picture-in-Picture params", e);
            }
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        // Enter PiP mode when user presses home button
        if (player != null && player.isPlaying() && !isInPictureInPictureMode) {
            // Автоматически сохраняем закладку перед переходом в PiP
            autoSaveBookmark();
            enterPictureInPictureMode();
        }
    }

    public void enterPictureInPictureMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                PictureInPictureParams params = new PictureInPictureParams.Builder()
                        .setAspectRatio(new Rational(16, 9))
                        .setActions(buildPipActions())
                        .build();
                enterPictureInPictureMode(params);
            } catch (Exception e) {
                Log.e("VideoPlayer", "Failed to enter Picture-in-Picture mode", e);
            }
        }
    }

    /**
     * Переключает ориентацию экрана (портретная / альбомная)
     */
    private void toggleOrientation() {
        boolean isPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        manualOrientationOverride = true;
        if (isPortrait) {
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else {
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private void initOrientationEventListener() {
        orientationEventListener = new android.view.OrientationEventListener(this, android.hardware.SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) return;

                int currentPhysicalOrientation;
                if ((orientation >= 315 || orientation < 45) || (orientation >= 135 && orientation < 225)) {
                    currentPhysicalOrientation = android.content.res.Configuration.ORIENTATION_PORTRAIT;
                } else if ((orientation >= 45 && orientation < 135) || (orientation >= 225 && orientation < 315)) {
                    currentPhysicalOrientation = android.content.res.Configuration.ORIENTATION_LANDSCAPE;
                } else {
                    return;
                }

                if (lastPhysicalOrientation != -1 && currentPhysicalOrientation != lastPhysicalOrientation) {
                    if (manualOrientationOverride) {
                        manualOrientationOverride = false;
                        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                    }
                }
                lastPhysicalOrientation = currentPhysicalOrientation;
            }
        };
        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable();
        }
    }

    private void setVideoResizeMode(int resizeMode) {
        this.currentResizeMode = resizeMode;
        if (playerView != null) {
            playerView.setResizeMode(resizeMode);
        }
        if (player != null) {
            if (resizeMode == androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
            } else {
                player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            }
        }
    }

    /**
     * Обновление настроек автоматического скрытия контроллера
     */
    private void updateControllerAutoHide() {
        updateControllerAutoHide(shouldAutoHideControls);
    }
    
    /**
     * Обновление настроек автоматического скрытия контроллера с принудительным значением
     */
    private void updateControllerAutoHide(boolean enableAutoHide) {
        if (playerView != null) {
            if (enableAutoHide) {
                // Включаем автоматическое скрытие
                playerView.setControllerShowTimeoutMs(controllerShowTimeoutMs);
                playerView.setControllerAutoShow(true);
                playerView.setControllerHideOnTouch(true);
                Log.d("VideoPlayer", "Controller auto-hide enabled");
            } else {
                // Отключаем автоматическое скрытие
                playerView.setControllerShowTimeoutMs(0); // Никогда не скрывать
                playerView.setControllerAutoShow(false);
                playerView.setControllerHideOnTouch(false);
                Log.d("VideoPlayer", "Controller auto-hide disabled");
            }
        }
    }

    /**
     * Открыть панель меню плееров
     */
    public void openMenuPanel() {
        if (isOfflineMode) return;
        if (playerView != null) {
            playerView.hideController();
        }
        if (slidingMenuPanel != null) {
            slidingMenuPanel.setVisibility(View.VISIBLE);
        }
        if (menuPanelContainer != null) {
            menuPanelContainer.openPanel();
        }
    }
    
    /**
     * Закрыть панель меню плееров
     */
    public void closeMenuPanel() {
        if (menuPanelContainer != null) {
            menuPanelContainer.closePanel();
        }
    }
    
    /**
     * Открыть панель комментариев
     */
    public void openCommentsPanel() {
        if (isOfflineMode) return;
        if (playerView != null) {
            playerView.hideController();
        }
        if (commentsPanel != null) {
            commentsPanel.setVisibility(View.VISIBLE);
        }
        if (commentsPanelContainer != null) {
            commentsPanelContainer.openPanel();
        }
    }
    
    /**
     * Закрыть панель комментариев
     */
    public void closeCommentsPanel() {
        if (commentsPanelContainer != null) {
            commentsPanelContainer.closePanel();
        }
    }
    
    
    /**
     * Проверить открыта ли панель меню
     */
    public boolean isMenuPanelOpen() {
        return menuPanelContainer != null && menuPanelContainer.isOpen();
    }
    
    /**
     * Проверить открыта ли панель комментариев
     */
    public boolean isCommentsPanelOpen() {
        return commentsPanelContainer != null && commentsPanelContainer.isOpen();
    }
    
    
    private void hideAllUI() {
        // Hide all UI elements except the player
        if (playerView != null) {
            playerView.setUseController(false);
        }

        // Hide menu and other panels
        if (menuPanelContainer != null) {
            menuPanelContainer.closePanel();
        }
        // Hide comments panel if it's currently visible
        if (commentsManager.isCommentsVisible()) {
            commentsManager.hideCommentsPanel();
        }
        // Episodes are now managed by EpisodesManager
        if (episodesManager != null) {
            // НЕ скрываем playersControlBar в PiP режиме, только остальные элементы
            episodesManager.hideEpisodesUIForPiP();
        }
        
        // Players are now managed by PlayersManager
        if (playersManager != null) {
            playersManager.hideAllPlayersUI();
        }
        
        // Gestures are now managed by GesturesManager
        if (gesturesManager != null) {
            gesturesManager.hideAllGesturesUI();
        }

        // Hide PiP button in PiP mode
        ImageButton pipButton = findViewById(R.id.pipButton);
        if (pipButton != null) {
            pipButton.setVisibility(View.GONE);
        }

        // Hide settings dialog if open
        if (currentSettingsBottomSheet != null && currentSettingsBottomSheet.isShowing()) {
            currentSettingsBottomSheet.dismiss();
        }
    }

    private void showAllUI() {
        // Show UI elements back
        if (playerView != null) {
            playerView.setUseController(true);
        }

        // Menu panel is shown only when user requests it
        // (don't auto-open after PiP)

        // Show PiP button back
        ImageButton pipButton = findViewById(R.id.pipButton);
        if (pipButton != null) {
            pipButton.setVisibility(View.VISIBLE);
        }

        // Restore episodes UI state
        if (episodesManager != null) {
            episodesManager.showAllEpisodesUI();
        }
        
        // Restore players UI state
        if (playersManager != null) {
            playersManager.showAllPlayersUI();
        }
        
        // Restore gestures UI state
        if (gesturesManager != null) {
            gesturesManager.showAllGesturesUI();
        }

        // Restore comments panel state if it was visible before PiP
        if (wasCommentsVisibleBeforePiP && !commentsManager.isCommentsVisible()) {
            commentsManager.showCommentsPanel();
        }
    }

    private void setupFullscreen() {
        Window window = getWindow();
        if (window != null) {
            boolean isPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;

            WindowCompat.setDecorFitsSystemWindows(window, false);

            WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, window.getDecorView());
            controller.setAppearanceLightStatusBars(false);
            controller.setAppearanceLightNavigationBars(false);

            if (isPortrait) {
                // В вертикальном положении показывай системную шторку (status bar)
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
                window.setNavigationBarColor(android.graphics.Color.TRANSPARENT);
                controller.show(WindowInsetsCompat.Type.statusBars());
            } else {
                // В горизонтальном положении скрываем системные панели, с прозрачным фоном для шторки
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
                window.setNavigationBarColor(android.graphics.Color.TRANSPARENT);
                controller.hide(WindowInsetsCompat.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.getAttributes().layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }

            View decorView = window.getDecorView();
            ViewCompat.setOnApplyWindowInsetsListener(decorView, (v, insets) -> {
                v.setPadding(0, 0, 0, 0);
                View content = v.findViewById(android.R.id.content);
                if (content != null) {
                    content.setPadding(0, 0, 0, 0);
                }
                return WindowInsetsCompat.CONSUMED;
            });
        }

        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.setFitsSystemWindows(false);
            rootView.setPadding(0, 0, 0, 0);
        }
    }

    @SuppressLint({"ClickableViewAccessibility", "RtlHardcoded"})
    private void setupMenu() {
        // === 1. Initialize UI Components ===
        initializeUIComponents();
        
        // === 2. Initialize Managers ===
        initializeManagers();
        
        // === 3. Setup Event Listeners ===
        setupEventListeners();
        
        // === 4. Configure Initial State ===
        configureInitialState();
        checkAndUpdateOrientation();
        
        // === 5. Setup Manager Callbacks ===
        setupManagerCallbacks();
        
        // === 6. Start Initial Loading ===
        startInitialLoading();
    }
    
    /**
     * Инициализация всех UI компонентов
     */
    private void initializeUIComponents() {

        // Main menu components
        controllerView = playerView.findViewById(R.id.exo_controller);
        timeBar = controllerView.findViewById(R.id.exo_progress);
        
        // Set initial alpha for controller (will be animated by our custom logic)
        if (controllerView != null) {
            controllerView.setAlpha(1.0f);
        }

        // Initialize draggable panels
        menuPanelContainer = findViewById(R.id.menuPanelContainer);
        commentsPanelContainer = findViewById(R.id.commentsPanelContainer);
        
        slidingMenuPanel = findViewById(R.id.slidingMenuPanel);
        menuOverlay = findViewById(R.id.menuOverlay);
        menuLoadingIndicator = findViewById(R.id.menuLoadingIndicator);
        menuLoadingOverlay = findViewById(R.id.menuLoadingOverlay);
        
        // Setup menu overlay click to close panels
        if (menuOverlay != null) {
            menuOverlay.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            menuOverlay.setOnClickListener(v -> {
                // Закрываем открытые панели при клике на overlay
                if (menuPanelContainer != null && menuPanelContainer.isOpen()) {
                    closeMenuPanel();
                }
                if (commentsPanelContainer != null && commentsPanelContainer.isOpen()) {
                    closeCommentsPanel();
                }
            });
        }
        
        // Initialize anime info placeholder
        animeInfoPlaceholder = findViewById(R.id.animeInfoPlaceholder);
        animeInfoPoster = findViewById(R.id.animeInfoPoster);
        animeInfoTitle = findViewById(R.id.animeInfoTitle);
        animeInfoOriginalTitle = findViewById(R.id.animeInfoOriginalTitle);
        animeInfoYear = findViewById(R.id.animeInfoYear);
        animeInfoType = findViewById(R.id.animeInfoType);
        animeInfoStatus = findViewById(R.id.animeInfoStatus);
        animeInfoRating = findViewById(R.id.animeInfoRating);
        animeInfoEpisodes = findViewById(R.id.animeInfoEpisodes);
        animeInfoAge = findViewById(R.id.animeInfoAge);
        animeInfoReleaseDate = findViewById(R.id.animeInfoReleaseDate);
        animeInfoShikimori = findViewById(R.id.animeInfoShikimori);
        
        // Next episode overlay
        nextEpisodeOverlay = findViewById(R.id.nextEpisodeOverlay);
        nextEpisodeNumber = findViewById(R.id.nextEpisodeNumber);
        nextEpisodeCountdown = findViewById(R.id.nextEpisodeCountdown);
        cancelNextEpisodeButton = findViewById(R.id.cancelNextEpisodeButton);
        playNextEpisodeButton = findViewById(R.id.playNextEpisodeButton);
        nextEpisodeHandler = new Handler(Looper.getMainLooper());
        
        // Setup next episode overlay buttons
        if (cancelNextEpisodeButton != null) {
            cancelNextEpisodeButton.setOnClickListener(v -> cancelNextEpisode());
        }
        if (playNextEpisodeButton != null) {
            playNextEpisodeButton.setOnClickListener(v -> playNextEpisodeNow());
        }
        
        // Gesture components
        TextView seekPreviewText = findViewById(R.id.seekPreviewText);
        TextView holdSpeedToast = findViewById(R.id.holdSpeedToast);
        View skipIndicatorLeft = findViewById(R.id.skipIndicatorLeft);
        View skipIndicatorRight = findViewById(R.id.skipIndicatorRight);
        
        // Comments components
        View commentsPanel = findViewById(R.id.commentsPanel);
        ImageButton closeCommentsButton = findViewById(R.id.closeCommentsButton);
        RecyclerView commentsRecyclerView = findViewById(R.id.commentsRecyclerView);
        View commentsLoadingOverlay = findViewById(R.id.commentsLoadingOverlay);
        View commentsOptionsButton = findViewById(R.id.btnSortComments);
        TextView emptyCommentsText = findViewById(R.id.emptyCommentsText);
        
        // Related titles components
        relatedTitlesOverlay = findViewById(R.id.relatedTitlesOverlay);
        relatedTitlesDimOverlay = findViewById(R.id.relatedTitlesDimOverlay);
        relatedTitlesRecyclerView = findViewById(R.id.relatedTitlesRecyclerView);
        
        // Player control components
        if (controllerView != null) {
            initializeControllerComponents();
        }
        
        // Menu control components
        ImageButton closeMenuButton = findViewById(R.id.closeMenuButton);
        ImageButton pipButton = findViewById(R.id.pipButton);
        
        // Store components for manager initialization
        this.menuOverlay = menuOverlay;
        this.closeMenuButton = closeMenuButton;
        this.commentsPanel = commentsPanel;
        this.closeCommentsButton = closeCommentsButton;
        this.commentsRecyclerView = commentsRecyclerView;
        this.commentsLoadingOverlay = commentsLoadingOverlay;
        this.commentsOptionsButton = commentsOptionsButton;
        this.emptyCommentsText = emptyCommentsText;
        this.seekPreviewText = seekPreviewText;
        this.holdSpeedToast = holdSpeedToast;
        this.pipButton = pipButton;
        this.skipIndicatorLeft = skipIndicatorLeft;
        this.skipIndicatorRight = skipIndicatorRight;
        this.playerBufferingIndicator = findViewById(R.id.playerBufferingIndicator);
    }
    
    /**
     * Инициализация компонентов контроллера плеера
     */
    private void initializeControllerComponents() {
        // Player info components
        ibClosePlayer = controllerView.findViewById(R.id.ibClosePlayer);
        settingsButton = controllerView.findViewById(R.id.settingsButton);
        settingsQualityTag = controllerView.findViewById(R.id.settingsQualityTag);
        updateSettingsQualityTag();
        menuToggleFullscreen = controllerView.findViewById(R.id.menuToggleFullscreen);
        if (pipButton == null && controllerView != null) {
            pipButton = controllerView.findViewById(R.id.pipButton);
        }
        animeTitleView = controllerView.findViewById(R.id.animeTitle);
        currentTeamName = controllerView.findViewById(R.id.currentTeamName);
        currentEpisodeName = controllerView.findViewById(R.id.currentEpisodeName);
        currentEpisodeNumberView = controllerView.findViewById(R.id.currentEpisodeNumber);
        
        // Navigation components
        episodesMenuButton = controllerView.findViewById(R.id.episodesMenuButton);
        prevEpisodeButton = controllerView.findViewById(R.id.prevEpisodeButton);
        nextEpisodeButton = controllerView.findViewById(R.id.nextEpisodeButton);
        
        // Control components
        menuToggleButton = controllerView.findViewById(R.id.menuToggleButton);
        playersControlBar = controllerView.findViewById(R.id.playersControlBar);
        
        // Episode list component
        RecyclerView episodesHorizontalRecyclerView = controllerView.findViewById(R.id.episodesHorizontalRecyclerView);
        ImageButton commentsButton = controllerView.findViewById(R.id.commentsButton);
        bookmarkButton = controllerView.findViewById(R.id.bookmarkButton);
        downloadButton = null;
        downloadButtonTop = null;
        btnDownloadFromMenu = findViewById(R.id.btnDownloadFromMenu);
        downloadProgressText = findViewById(R.id.downloadProgressText);
        setupDownloadListener();

        // Store for manager initialization
        this.episodesHorizontalRecyclerView = episodesHorizontalRecyclerView;
        this.commentsButton = commentsButton;
    }
    
    /**
     * Настройка драггабельных панелей
     */
    private void setupDraggablePanels() {
        if (menuPanelContainer != null) {
            menuPanelContainer.setOnPanelStateChangeListener(new com.example.animelib.ui.DraggableSidePanel.OnPanelStateChangeListener() {
                @Override
                public void onPanelDragStart() {
                    if (playersManager != null) {
                        playersManager.onPanelDragStart();
                    }
                }

                @Override
                public void onPanelOpened() {
                    applyPlayerSidePanelTransform(1f);
                    if (menuOverlay != null) {
                        menuOverlay.setVisibility(View.VISIBLE);
                    }
                    if (playersManager != null) {
                        playersManager.onPanelOpened();
                    }
                }

                @Override
                public void onPanelClosed() {
                    applyPlayerSidePanelTransform(0f);
                    if (menuOverlay != null) {
                        menuOverlay.setVisibility(View.GONE);
                    }
                    if (playersManager != null) {
                        playersManager.onPanelClosedByDrag();
                    }
                }

                @Override
                public void onPanelSliding(float slideOffset) {
                    applyPlayerSidePanelTransform(1f - slideOffset);
                    if (menuOverlay != null) {
                        if (slideOffset < 1f && menuOverlay.getVisibility() != View.VISIBLE) {
                            menuOverlay.setVisibility(View.VISIBLE);
                        } else if (slideOffset >= 1f && menuOverlay.getVisibility() != View.GONE) {
                            menuOverlay.setVisibility(View.GONE);
                        }
                    }
                }
                
                @Override
                public boolean canClosePanel() {
                    if (isOfflineMode) {
                        return true;
                    }
                    if (playersManager != null && playersManager.getCurrentPlayerData() == null) {
                        return false;
                    }
                    return true;
                }
            });
        }
        
        if (commentsPanelContainer != null) {
            commentsPanelContainer.setOnPanelStateChangeListener(new com.example.animelib.ui.DraggableSidePanel.OnPanelStateChangeListener() {
                @Override
                public void onPanelDragStart() {
                }

                @Override
                public void onPanelOpened() {
                    applyPlayerSidePanelTransform(1f);
                    if (menuOverlay != null) {
                        menuOverlay.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onPanelClosed() {
                    applyPlayerSidePanelTransform(0f);
                    if (menuOverlay != null) {
                        menuOverlay.setVisibility(View.GONE);
                    }
                    if (commentsManager != null) {
                        commentsManager.onPanelClosedByDrag();
                    }
                }

                @Override
                public void onPanelSliding(float slideOffset) {
                    applyPlayerSidePanelTransform(1f - slideOffset);
                    if (menuOverlay != null) {
                        if (slideOffset < 1f && menuOverlay.getVisibility() != View.VISIBLE) {
                            menuOverlay.setVisibility(View.VISIBLE);
                        } else if (slideOffset >= 1f && menuOverlay.getVisibility() != View.GONE) {
                            menuOverlay.setVisibility(View.GONE);
                        }
                    }
                }
            });
        }
    }

    /**
     * Применяет трансформацию плеера (уменьшение, позиционирование, скругление углов)
     * и синхронизирует положение ambient-подсветки при открытии боковых панелей (озвучки/комментарии).
     * @param openProgress Прогресс открытия панели от 0.0 (закрыто, полный экран) до 1.0 (полностью открыто)
     */
    private void applyPlayerSidePanelTransform(float openProgress) {
        openProgress = Math.max(0f, Math.min(1f, openProgress));

        if (playerContainer == null) {
            return;
        }

        boolean isPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        ViewCompat.setElevation(playerContainer, isPortrait ? 10f : 0f);

        if (isPortrait) {
            playerContainer.setPivotX(0f);
            playerContainer.setPivotY(0f);
            playerContainer.setScaleX(1f);
            playerContainer.setScaleY(1f);
            playerContainer.setTranslationX(0f);
            playerContainer.setTranslationY(0f);
            currentOutlineLeft = 0f;
            currentOutlineTop = 0f;
            currentOutlineRight = 0f;
            currentOutlineBottom = 0f;
            currentCornerRadiusPx = 0f;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                playerContainer.invalidateOutline();
            }
            View ambientContainer = findViewById(R.id.ambientContainer);
            int sbHeight = getStatusBarHeight();
            float actualTop = sbHeight;
            float actualLeft = 0f;
            if (playerContainer != null && ambientContainer != null) {
                int[] pLoc = new int[2];
                int[] aLoc = new int[2];
                playerContainer.getLocationOnScreen(pLoc);
                ambientContainer.getLocationOnScreen(aLoc);
                if (pLoc[1] > 0 || aLoc[1] > 0) {
                    actualTop = pLoc[1] - aLoc[1];
                    actualLeft = pLoc[0] - aLoc[0];
                }
            }

            int sw = getResources().getDisplayMetrics().widthPixels;
            float containerW = (playerContainer != null && playerContainer.getWidth() > 0) ? playerContainer.getWidth() : sw;
            float containerH = (playerContainer != null && playerContainer.getHeight() > 0) ? playerContainer.getHeight() : (sw * 9f / 16f);

            float portW = containerW;
            float portH = containerH;
            float portLeft = actualLeft;
            float portTop = actualTop;

            if (player != null && player.getVideoSize() != null) {
                int vw = player.getVideoSize().width;
                int vh = player.getVideoSize().height;
                if (vw > 0 && vh > 0) {
                    float videoAspect = (float) vw / vh;
                    float containerAspect = containerW / containerH;
                    if (containerAspect > videoAspect) {
                        portH = containerH;
                        portW = containerH * videoAspect;
                        portLeft = actualLeft + (containerW - portW) / 2f;
                        portTop = actualTop;
                    } else {
                        portW = containerW;
                        portH = containerW / videoAspect;
                        portLeft = actualLeft;
                        portTop = actualTop + (containerH - portH) / 2f;
                    }
                }
            }

            com.example.animelib.ui.AmbientVignetteOverlayView ambientVignetteOverlay = findViewById(R.id.ambientVignetteOverlay);
            if (ambientVignetteOverlay != null) {
                ambientVignetteOverlay.setVideoBounds(portLeft, portTop, portLeft + portW, portTop + portH);
            }
            updateAmbientPlayerTransform(portLeft, portTop, portW, portH, true);
            android.view.ViewGroup.LayoutParams lp = playerContainer.getLayoutParams();
            if (lp != null) {
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                int targetH = screenWidth * 9 / 16;
                if (lp.height != targetH) {
                    lp.height = targetH;
                    if (lp instanceof android.widget.LinearLayout.LayoutParams) {
                        ((android.widget.LinearLayout.LayoutParams) lp).weight = 0;
                    }
                    playerContainer.setLayoutParams(lp);
                }
            }
            return;
        }

        // В ландшафтном режиме проверяем, действительно ли открыта хотя бы одна боковая панель
        boolean isMenuOpen = menuPanelContainer != null && menuPanelContainer.isOpen() && menuPanelContainer.getVisibility() == View.VISIBLE;
        boolean isCommentsOpen = commentsPanelContainer != null && commentsPanelContainer.isOpen() && commentsPanelContainer.getVisibility() == View.VISIBLE;
        if (!isMenuOpen && !isCommentsOpen) {
            openProgress = 0f;
        }

        if (openProgress <= 0f) {
            playerContainer.setPivotX(0f);
            playerContainer.setPivotY(0f);
            playerContainer.setScaleX(1f);
            playerContainer.setScaleY(1f);
            playerContainer.setTranslationX(0f);
            playerContainer.setTranslationY(0f);
            currentOutlineLeft = 0f;
            currentOutlineTop = 0f;
            currentOutlineRight = 0f;
            currentOutlineBottom = 0f;
            currentCornerRadiusPx = 0f;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                playerContainer.invalidateOutline();
            }
            int sw = 0;
            int sh = 0;
            View decorView = getWindow().getDecorView();
            if (decorView != null && decorView.getWidth() > 0 && decorView.getHeight() > 0) {
                sw = decorView.getWidth();
                sh = decorView.getHeight();
            } else {
                DisplayMetrics dm = getResources().getDisplayMetrics();
                sw = dm.widthPixels;
                sh = dm.heightPixels;
            }
            float aspect = 16f / 9f;
            if (player != null && player.getVideoSize() != null) {
                int vw = player.getVideoSize().width;
                int vh = player.getVideoSize().height;
                if (vw > 0 && vh > 0) {
                    aspect = (float) vw / vh;
                }
            }
            float screenAspect = (sw > 0 && sh > 0) ? ((float) sw / sh) : (16f / 9f);
            float lsW, lsH;
            if (screenAspect > aspect) {
                lsH = sh;
                lsW = sh * aspect;
            } else {
                lsW = sw;
                lsH = sw / aspect;
            }
            float lsLeft = (sw - lsW) / 2f;
            float lsTop = (sh - lsH) / 2f;

            com.example.animelib.ui.AmbientVignetteOverlayView ambientVignetteOverlay = findViewById(R.id.ambientVignetteOverlay);
            if (ambientVignetteOverlay != null) {
                ambientVignetteOverlay.clearCustomVideoBounds();
            }
            updateAmbientPlayerTransform(0f, 0f, sw, sh, false);
            android.view.ViewGroup.LayoutParams rawLp = playerContainer.getLayoutParams();
            if (rawLp != null) {
                if (rawLp.width != android.view.ViewGroup.LayoutParams.MATCH_PARENT ||
                    rawLp.height != android.view.ViewGroup.LayoutParams.MATCH_PARENT) {
                    rawLp.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                    rawLp.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                    playerContainer.setLayoutParams(rawLp);
                }
            }
            return;
        }

        // Получаем реальные размеры экрана
        int screenWidth = 0;
        int screenHeight = 0;
        View decorView = getWindow().getDecorView();
        if (decorView != null && decorView.getWidth() > 0 && decorView.getHeight() > 0) {
            screenWidth = decorView.getWidth();
            screenHeight = decorView.getHeight();
        } else {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            screenWidth = dm.widthPixels;
            screenHeight = dm.heightPixels;
        }

        if (screenWidth <= 0 || screenHeight <= 0) return;

        // Важно: playerContainer всегда должен быть MATCH_PARENT, чтобы элементы управления
        // растягивались на весь экран, и жесты вытягивания с края экрана улавливались на playerView
        android.view.ViewGroup.LayoutParams rawLp = playerContainer.getLayoutParams();
        if (rawLp instanceof android.view.ViewGroup.MarginLayoutParams) {
            android.view.ViewGroup.MarginLayoutParams mlp = (android.view.ViewGroup.MarginLayoutParams) rawLp;
            if (mlp.width != android.view.ViewGroup.LayoutParams.MATCH_PARENT ||
                mlp.height != android.view.ViewGroup.LayoutParams.MATCH_PARENT ||
                mlp.leftMargin != 0 || mlp.topMargin != 0) {
                mlp.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                mlp.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                mlp.leftMargin = 0;
                mlp.topMargin = 0;
                if (mlp instanceof FrameLayout.LayoutParams) {
                    ((FrameLayout.LayoutParams) mlp).gravity = Gravity.TOP | Gravity.START;
                } else if (mlp instanceof LinearLayout.LayoutParams) {
                    ((LinearLayout.LayoutParams) mlp).gravity = Gravity.TOP | Gravity.START;
                }
                playerContainer.setLayoutParams(mlp);
            }
        } else if (rawLp != null) {
            if (rawLp.width != android.view.ViewGroup.LayoutParams.MATCH_PARENT ||
                rawLp.height != android.view.ViewGroup.LayoutParams.MATCH_PARENT) {
                rawLp.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                rawLp.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                playerContainer.setLayoutParams(rawLp);
            }
        }

        // Определяем соотношение сторон видео (по умолчанию 16:9)
        float videoAspect = 16f / 9f;
        if (player != null && player.getVideoSize() != null) {
            int vWidth = player.getVideoSize().width;
            int vHeight = player.getVideoSize().height;
            if (vWidth > 0 && vHeight > 0) {
                videoAspect = (float) vWidth / (float) vHeight;
            }
        }

        // Размеры видео в полноэкранном режиме (progress = 0)
        float screenAspect = (float) screenWidth / (float) screenHeight;
        float videoW0, videoH0;
        if (screenAspect > videoAspect) {
            videoH0 = screenHeight;
            videoW0 = screenHeight * videoAspect;
        } else {
            videoW0 = screenWidth;
            videoH0 = screenWidth / videoAspect;
        }
        float videoLeft0 = (screenWidth - videoW0) / 2f;
        float videoTop0 = (screenHeight - videoH0) / 2f;

        // Определяем ширину открытой боковой панели в пикселях (по умолчанию 360dp)
        float density = getResources().getDisplayMetrics().density;
        float panelWidthPx = 360f * density;

        if (menuPanelContainer != null && menuPanelContainer.getChildCount() > 0) {
            int w = menuPanelContainer.getChildAt(0).getWidth();
            if (w > 0) panelWidthPx = w;
        } else if (commentsPanelContainer != null && commentsPanelContainer.getChildCount() > 0) {
            int w = commentsPanelContainer.getChildAt(0).getWidth();
            if (w > 0) panelWidthPx = w;
        }

        // Отступ слева от экрана и от панели для видео в открытом состоянии (16dp)
        float sideMarginPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, getResources().getDisplayMetrics());

        // Доступная область для видео при открытой панели (progress = 1)
        float availWidth1 = Math.max(0f, screenWidth - panelWidthPx - (2f * sideMarginPx));
        float availHeight1 = Math.max(0f, screenHeight - (2f * sideMarginPx));
        float availAspect1 = (availHeight1 > 0) ? (availWidth1 / availHeight1) : videoAspect;
        float videoW1, videoH1;
        if (availAspect1 > videoAspect) {
            videoH1 = availHeight1;
            videoW1 = availHeight1 * videoAspect;
        } else {
            videoW1 = availWidth1;
            videoH1 = availWidth1 / videoAspect;
        }
        float videoLeft1 = sideMarginPx + (availWidth1 - videoW1) / 2f;
        float videoTop1 = sideMarginPx + (availHeight1 - videoH1) / 2f;

        // Линейная интерполяция размеров и положения видео на экране
        float currentVideoW = videoW0 + (videoW1 - videoW0) * openProgress;
        float currentVideoH = videoH0 + (videoH1 - videoH0) * openProgress;
        float currentLeft = videoLeft0 + (videoLeft1 - videoLeft0) * openProgress;
        float currentTop = videoTop0 + (videoTop1 - videoTop0) * openProgress;

        // Масштабирование playerContainer от 1.0
        float scale = (videoW0 > 0) ? (currentVideoW / videoW0) : 1f;

        // Положение верхнего левого угла видео на экране
        float translationX = currentLeft - (videoLeft0 * scale);
        float translationY = currentTop - (videoTop0 * scale);

        playerContainer.setPivotX(0f);
        playerContainer.setPivotY(0f);
        playerContainer.setScaleX(scale);
        playerContainer.setScaleY(scale);
        playerContainer.setTranslationX(translationX);
        playerContainer.setTranslationY(translationY);

        // Границы отсечения внутри playerContainer в его локальных координатах
        currentOutlineLeft = videoLeft0 * openProgress;
        currentOutlineTop = videoTop0 * openProgress;
        currentOutlineRight = screenWidth + (videoLeft0 + videoW0 - screenWidth) * openProgress;
        currentOutlineBottom = screenHeight + (videoTop0 + videoH0 - screenHeight) * openProgress;

        // Скругление углов контейнера: визуально 20dp в открытом состоянии
        float maxRadiusPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, getResources().getDisplayMetrics());
        currentCornerRadiusPx = (scale > 0) ? (maxRadiusPx * openProgress / scale) : 0f;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            playerContainer.invalidateOutline();
        }

        // Обновляем подсветку (ambient light), пересчитывая органичную виньетку и трансформ плеера
        com.example.animelib.ui.AmbientVignetteOverlayView ambientVignetteOverlay = findViewById(R.id.ambientVignetteOverlay);
        if (openProgress > 0f) {
            if (ambientVignetteOverlay != null) {
                ambientVignetteOverlay.setVideoBounds(currentLeft, currentTop, currentLeft + currentVideoW, currentTop + currentVideoH);
            }
            updateAmbientPlayerTransform(currentLeft, currentTop, currentVideoW, currentVideoH, true);
        } else {
            if (isPortrait) {
                View ambientContainer = findViewById(R.id.ambientContainer);
                float actualTop = getStatusBarHeight();
                float actualLeft = 0f;
                if (playerContainer != null && ambientContainer != null) {
                    int[] pLoc = new int[2];
                    int[] aLoc = new int[2];
                    playerContainer.getLocationOnScreen(pLoc);
                    ambientContainer.getLocationOnScreen(aLoc);
                    if (pLoc[1] > 0 || aLoc[1] > 0) {
                        actualTop = pLoc[1] - aLoc[1];
                        actualLeft = pLoc[0] - aLoc[0];
                    }
                }
                float containerW = (playerContainer != null && playerContainer.getWidth() > 0) ? playerContainer.getWidth() : screenWidth;
                float containerH = (playerContainer != null && playerContainer.getHeight() > 0) ? playerContainer.getHeight() : (screenWidth * 9f / 16f);
                float portW = containerW;
                float portH = containerH;
                float portLeft = actualLeft;
                float portTop = actualTop;
                if (player != null && player.getVideoSize() != null) {
                    int vw = player.getVideoSize().width;
                    int vh = player.getVideoSize().height;
                    if (vw > 0 && vh > 0) {
                        float vAspect = (float) vw / vh;
                        float containerAspect = containerW / containerH;
                        if (containerAspect > vAspect) {
                            portH = containerH;
                            portW = containerH * vAspect;
                            portLeft = actualLeft + (containerW - portW) / 2f;
                            portTop = actualTop;
                        } else {
                            portW = containerW;
                            portH = containerW / vAspect;
                            portLeft = actualLeft;
                            portTop = actualTop + (containerH - portH) / 2f;
                        }
                    }
                }
                if (ambientVignetteOverlay != null) {
                    ambientVignetteOverlay.setVideoBounds(portLeft, portTop, portLeft + portW, portTop + portH);
                }
                updateAmbientPlayerTransform(portLeft, portTop, portW, portH, true);
            } else {
                if (ambientVignetteOverlay != null) {
                    ambientVignetteOverlay.clearCustomVideoBounds();
                }
                updateAmbientPlayerTransform(0f, 0f, screenWidth, screenHeight, false);
            }
        }
    }

    private void updateAmbientPlayerTransform(float vLeft, float vTop, float vWidth, float vHeight, boolean isCroppedToVideo) {
        androidx.media3.ui.PlayerView ambientPlayerView = findViewById(R.id.ambientPlayerView);
        if (ambientPlayerView != null) {
            android.view.ViewGroup.LayoutParams lp = ambientPlayerView.getLayoutParams();
            if (isCroppedToVideo && vWidth > 0 && vHeight > 0) {
                int targetW = (int) Math.ceil(vWidth);
                int targetH = (int) Math.ceil(vHeight);
                if (lp != null && (lp.width != targetW || lp.height != targetH)) {
                    lp.width = targetW;
                    lp.height = targetH;
                    if (lp instanceof FrameLayout.LayoutParams) {
                        ((FrameLayout.LayoutParams) lp).gravity = Gravity.TOP | Gravity.START;
                    }
                    ambientPlayerView.setLayoutParams(lp);
                }
                ambientPlayerView.setPivotX(vWidth / 2f);
                ambientPlayerView.setPivotY(vHeight / 2f);
                ambientPlayerView.setTranslationX(vLeft);
                ambientPlayerView.setTranslationY(vTop);

                ambientPlayerView.setScaleX(1.12f);
                ambientPlayerView.setScaleY(1.12f);
            } else {
                if (lp != null && (lp.width != android.view.ViewGroup.LayoutParams.MATCH_PARENT || lp.height != android.view.ViewGroup.LayoutParams.MATCH_PARENT)) {
                    lp.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                    lp.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                    if (lp instanceof FrameLayout.LayoutParams) {
                        ((FrameLayout.LayoutParams) lp).gravity = Gravity.TOP | Gravity.START;
                    }
                    ambientPlayerView.setLayoutParams(lp);
                }
                ambientPlayerView.setTranslationX(0f);
                ambientPlayerView.setTranslationY(0f);

                ambientPlayerView.setScaleX(1.0f);
                ambientPlayerView.setScaleY(1.0f);
            }

            if (ambientLightManager != null) {
                ambientLightManager.resume();
            }
        }
    }
    
    /**
     * Инициализация всех менеджеров
     */
    private void initializeManagers() {
        // Initialize gestures manager
        gesturesManager.initializeViews(playerView, null, holdSpeedToast, seekPreviewText,
                skipIndicatorLeft, skipIndicatorRight);
        
        // Setup draggable panels
        setupDraggablePanels();
        
        // Initialize comments manager
        commentsManager.initializeViews(commentsPanel, closeCommentsButton, commentsRecyclerView,
                commentsLoadingOverlay, commentsButton, commentsOptionsButton, menuOverlay, emptyCommentsText);
        
        // Initialize players manager
        playersManager.initializeViews(slidingMenuPanel, closeMenuButton, null, null,
                menuOverlay, menuLoadingOverlay, menuLoadingIndicator);
        
        // Initialize episodes manager
        episodesManager.initializeViews(null, episodesMenuButton, episodesHorizontalRecyclerView,
                null, prevEpisodeButton, nextEpisodeButton, menuOverlay, playersControlBar);
        
        // Configure menu width
        float density = getResources().getDisplayMetrics().density;
        menuWidth = (int) (menuWidth * density);
        playersManager.setMenuWidth(menuWidth);
        
        // Initialize ambient light manager with secondary ambient player view & vignette overlay
        View ambientContainer = findViewById(R.id.ambientContainer);
        androidx.media3.ui.PlayerView ambientPlayerView = findViewById(R.id.ambientPlayerView);
        com.example.animelib.ui.AmbientVignetteOverlayView ambientVignetteOverlay = findViewById(R.id.ambientVignetteOverlay);
        ambientLightManager = new AmbientLightManager(this, playerView, ambientContainer, ambientPlayerView, ambientVignetteOverlay);
        if (ambientLightManager != null) {
            ambientLightManager.setEnabled(enableAmbientLight);
        }
        
        // Initialize video filters manager
        videoFiltersManager = new com.example.animelib.managers.VideoFiltersManager(this, playerView);
        if (videoFiltersManager != null) {
            videoFiltersManager.setFilters(filterBrightness, filterContrast, filterSaturation, filterGamma, filterHue);
        }
        
        // Initialize related titles
        initializeRelatedTitles();

        // Initialize portrait views
        initializePortraitViews();
    }

    public static String cleanEpisodeName(String name, String epNumber) {
        if (name == null) return "";
        String clean = name.trim();
        if (clean.isEmpty()) return "";

        // Normalize spaces
        clean = clean.replaceAll("\\s+", " ");

        // Remove leading episode patterns e.g. "Серия 1 - ", "Серия 1: ", "1 серия - ", "Серия 1"
        if (epNumber != null && !epNumber.trim().isEmpty()) {
            String num = epNumber.trim();
            clean = clean.replaceAll("^(?i)(?:серия\\s*" + java.util.regex.Pattern.quote(num) + "|" + java.util.regex.Pattern.quote(num) + "\\s*серия)\\s*[-:–—.]*\\s*", "");
        } else {
            clean = clean.replaceAll("^(?i)(?:серия\\s*\\d+|\\d+\\s*серия)\\s*[-:–—.]*\\s*", "");
        }

        clean = clean.trim();
        if (clean.equalsIgnoreCase("серия") || clean.matches("^(?i)серия\\s*\\d*$") || clean.matches("^\\d+$")) {
            return "";
        }
        return clean;
    }

    private String getOfflineVoiceoverName() {
        com.example.animelib.data.entity.DownloadedEpisodeEntity ep = getCurrentOfflineEpisode();
        if (ep != null && ep.getTeamName() != null && !ep.getTeamName().trim().isEmpty()) {
            return ep.getTeamName().trim();
        }
        return "Офлайн";
    }

    private String getOfflinePlayerTypeName() {
        com.example.animelib.data.entity.DownloadedEpisodeEntity ep = getCurrentOfflineEpisode();
        if (ep != null && ep.getPlayerType() != null && !ep.getPlayerType().trim().isEmpty()) {
            String pt = ep.getPlayerType().trim();
            return pt.substring(0, 1).toUpperCase() + pt.substring(1);
        }
        return "Офлайн";
    }

    private com.example.animelib.data.entity.DownloadedEpisodeEntity getCurrentOfflineEpisode() {
        if (!isOfflineMode) return null;
        EpisodesListResponse.EpisodeItem cur = episodesManager != null ? episodesManager.getCurrentEpisode() : null;
        if (cur != null) {
            if (cur.getNumber() != null && offlineEpisodesMap.containsKey(cur.getNumber())) {
                return offlineEpisodesMap.get(cur.getNumber());
            }
            if (offlineEpisodesMap.containsKey(String.valueOf(cur.getId()))) {
                return offlineEpisodesMap.get(String.valueOf(cur.getId()));
            }
        }
        String epNum = getIntent() != null ? getIntent().getStringExtra("EXTRA_EPISODE_NUMBER") : null;
        if (epNum != null && offlineEpisodesMap.containsKey(epNum)) {
            return offlineEpisodesMap.get(epNum);
        }
        if (!offlineEpisodesMap.isEmpty()) {
            return offlineEpisodesMap.values().iterator().next();
        }
        return null;
    }

    private void hideAllSkeletonsForOffline() {
        safeRunOnUiThread(() -> {
            String animeTitle = getIntent() != null ? getIntent().getStringExtra("EXTRA_ANIME_TITLE") : null;
            if (animeTitle == null || animeTitle.isEmpty()) {
                com.example.animelib.data.entity.DownloadedEpisodeEntity cur = getCurrentOfflineEpisode();
                if (cur != null) animeTitle = cur.getAnimeTitle();
            }
            if (animeTitle == null) animeTitle = "";

            if (animeTitleView != null) {
                SkeletonHelper.hideSkeleton(animeTitleView, animeTitle);
            }
            if (tvPortraitAnimeTitle != null) {
                SkeletonHelper.hideSkeleton(tvPortraitAnimeTitle, animeTitle);
            }
            if (currentTeamName != null) {
                SkeletonHelper.hideSkeleton(currentTeamName, getOfflineVoiceoverName());
            }
            if (tvPortraitVoiceover != null) {
                SkeletonHelper.hideSkeleton(tvPortraitVoiceover, getOfflineVoiceoverName());
            }
            if (tvPortraitPlayer != null) {
                SkeletonHelper.hideSkeleton(tvPortraitPlayer, getOfflinePlayerTypeName());
            }

            EpisodesListResponse.EpisodeItem curEp = episodesManager != null ? episodesManager.getCurrentEpisode() : null;
            String epNum = curEp != null && curEp.getNumber() != null ? curEp.getNumber() : (getIntent() != null ? getIntent().getStringExtra("EXTRA_EPISODE_NUMBER") : null);
            String rawName = curEp != null && curEp.getName() != null ? curEp.getName() : (getIntent() != null ? getIntent().getStringExtra("EXTRA_EPISODE_TITLE") : null);
            String cleanName = cleanEpisodeName(rawName, epNum);

            if (currentEpisodeNumberView != null) {
                if (epNum != null && !epNum.isEmpty()) {
                    SkeletonHelper.hideSkeleton(currentEpisodeNumberView, epNum + " серия");
                } else {
                    SkeletonHelper.hideSkeleton(currentEpisodeNumberView, "");
                }
            }
            if (currentEpisodeName != null) {
                if (!cleanName.isEmpty()) {
                    SkeletonHelper.hideSkeleton(currentEpisodeName, ", " + cleanName);
                } else {
                    SkeletonHelper.hideSkeleton(currentEpisodeName, "");
                }
            }
            if (tvPortraitEpisodeTitle != null) {
                String epTitle = (epNum != null && !epNum.isEmpty() ? (epNum + " серия") : "") + (!cleanName.isEmpty() ? (", " + cleanName) : "");
                SkeletonHelper.hideSkeleton(tvPortraitEpisodeTitle, epTitle);
            }

            if (animeInfoTitle != null) SkeletonHelper.hideSkeleton(animeInfoTitle, animeTitle);
            if (animeInfoOriginalTitle != null) SkeletonHelper.hideSkeleton(animeInfoOriginalTitle, "");
            if (animeInfoRating != null) SkeletonHelper.hideSkeleton(animeInfoRating, "");
            if (animeInfoEpisodes != null) SkeletonHelper.hideSkeleton(animeInfoEpisodes, "");
            if (animeInfoYear != null) SkeletonHelper.hideSkeleton(animeInfoYear, "");
            if (animeInfoAge != null) SkeletonHelper.hideSkeleton(animeInfoAge, "");
            if (animeInfoType != null) SkeletonHelper.hideSkeleton(animeInfoType, "");
            if (animeInfoStatus != null) SkeletonHelper.hideSkeleton(animeInfoStatus, "");
            if (animeInfoReleaseDate != null) SkeletonHelper.hideSkeleton(animeInfoReleaseDate, "");
            if (animeInfoShikimori != null) SkeletonHelper.hideSkeleton(animeInfoShikimori, "");
        });
    }

    private void initializePortraitViews() {
        portraitScrollView = findViewById(R.id.portraitScrollView);
        btnPortraitScrollToTop = findViewById(R.id.btnPortraitScrollToTop);
        portraitVoiceoverPlayerButton = findViewById(R.id.portraitVoiceoverPlayerButton);
        tvPortraitVoiceover = findViewById(R.id.tvPortraitVoiceover);
        tvPortraitPlayer = findViewById(R.id.tvPortraitPlayer);
        ivPortraitVoiceoverChevron = findViewById(R.id.ivPortraitVoiceoverChevron);
        portraitBookmarkButton = findViewById(R.id.portraitBookmarkButton);
        portraitDownloadButton = findViewById(R.id.portraitDownloadButton);
        portraitEpisodesRecyclerView = findViewById(R.id.portraitEpisodesRecyclerView);
        tvPortraitAnimeTitle = findViewById(R.id.tvPortraitAnimeTitle);
        tvPortraitEpisodeTitle = findViewById(R.id.tvPortraitEpisodeTitle);
        btnPortraitSortComments = findViewById(R.id.btnPortraitSortComments);
        tvPortraitSortComments = findViewById(R.id.tvPortraitSortComments);
        btnPortraitCommentRules = findViewById(R.id.btnPortraitCommentRules);
        portraitCommentInputField = findViewById(R.id.portraitCommentInputField);
        portraitCommentsRecyclerView = findViewById(R.id.portraitCommentsRecyclerView);
        tvPortraitEmptyComments = findViewById(R.id.tvPortraitEmptyComments);
        portraitCommentsLoadingOverlay = findViewById(R.id.portraitCommentsLoadingOverlay);
        portraitDownloadProgressContainer = findViewById(R.id.portraitDownloadProgressContainer);
        portraitDownloadProgressCard = findViewById(R.id.portraitDownloadProgressCard);
        tvPortraitDownloadPercent = findViewById(R.id.tvPortraitDownloadPercent);
        portraitRelatedTitlesContainer = findViewById(R.id.portraitRelatedTitlesContainer);
        portraitRelatedTitlesRecyclerView = findViewById(R.id.portraitRelatedTitlesRecyclerView);

        if (portraitRelatedTitlesRecyclerView != null) {
            portraitRelatedTitlesRecyclerView.setLayoutManager(
                    new androidx.recyclerview.widget.LinearLayoutManager(this,
                            androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
            );
            portraitRelatedTitlesAdapter = new HorizontalRelatedTitlesAdapter(
                    new java.util.ArrayList<>(),
                    this::onRelatedTitleSelected
            );
            portraitRelatedTitlesRecyclerView.setAdapter(portraitRelatedTitlesAdapter);
        }

        if (isOfflineMode && portraitRelatedTitlesContainer != null) {
            portraitRelatedTitlesContainer.setVisibility(View.GONE);
        }

        if (portraitDownloadProgressCard != null) {
            portraitDownloadProgressCard.setOnClickListener(v -> showDownloadProgressBottomSheet());
        }

        if (!isOfflineMode) {
            showPlayerTextSkeletons();
        } else {
            hideAllSkeletonsForOffline();
        }

        if (portraitVoiceoverPlayerButton != null) {
            portraitVoiceoverPlayerButton.setOnClickListener(v -> {
                com.example.animelib.util.ItemAnimationUtils.animateItemClick(v, () -> {
                    if (isOfflineMode) {
                        CustomToast.showInfo(this, "Выбор озвучки недоступен в офлайн режиме");
                        return;
                    }
                    if (playersManager != null) {
                        playersManager.toggleMenu();
                    }
                });
            });
        }

        if (portraitBookmarkButton != null) {
            portraitBookmarkButton.setOnClickListener(v -> {
                addBookmark();
            });
        }

        if (portraitDownloadButton != null) {
            portraitDownloadButton.setOnClickListener(v -> {
                if (isOfflineMode) {
                    CustomToast.showInfo(this, "Воспроизводится скачанный файл");
                    return;
                }
                if (DownloadService.isRunning()) {
                    showDownloadProgressBottomSheet();
                } else {
                    showDownloadBottomSheet();
                }
            });
        }

        if (episodesManager != null && portraitEpisodesRecyclerView != null) {
            episodesManager.setPortraitEpisodesRecyclerView(portraitEpisodesRecyclerView);
        }

        if (commentsManager != null && portraitCommentsRecyclerView != null) {
            commentsManager.setPortraitViews(portraitCommentsRecyclerView, portraitCommentsLoadingOverlay,
                    tvPortraitEmptyComments, btnPortraitSortComments, tvPortraitSortComments, btnPortraitCommentRules);
            if (isOfflineMode) {
                commentsManager.setOfflineMode(true);
            }
        }

        if (btnPortraitScrollToTop != null) {
            btnPortraitScrollToTop.setOnClickListener(v -> {
                if (portraitScrollView != null) {
                    portraitScrollView.smoothScrollTo(0, 0);
                }
            });
        }

        if (portraitScrollView != null) {
            portraitScrollView.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (btnPortraitScrollToTop != null) {
                    if (scrollY > 300) {
                        if (btnPortraitScrollToTop.getVisibility() != View.VISIBLE) {
                            btnPortraitScrollToTop.show();
                        }
                    } else {
                        if (btnPortraitScrollToTop.getVisibility() == View.VISIBLE) {
                            btnPortraitScrollToTop.hide();
                        }
                    }
                }

                if (scrollY >= (v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight() - 200)) {
                    if (commentsManager != null && !isOfflineMode && getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
                        commentsManager.loadNextCommentsPageIfAvailable();
                    }
                }
            });
        }
    }

    /**
     * Показывает скелетоны загрузки для всех текстовых элементов плеера
     */
    private void showPlayerTextSkeletons() {
        if (isOfflineMode) return;
        safeRunOnUiThread(() -> {
            if (animeTitleView != null) SkeletonHelper.showSkeleton(animeTitleView, 160);
            if (currentTeamName != null) SkeletonHelper.showSkeleton(currentTeamName, 80);
            if (currentEpisodeNumberView != null) SkeletonHelper.showSkeleton(currentEpisodeNumberView, 65);
            if (currentEpisodeName != null) SkeletonHelper.showSkeleton(currentEpisodeName, 100);

            if (tvPortraitAnimeTitle != null) SkeletonHelper.showSkeleton(tvPortraitAnimeTitle, 180);
            if (tvPortraitEpisodeTitle != null) SkeletonHelper.showSkeleton(tvPortraitEpisodeTitle, 120);
            if (tvPortraitVoiceover != null) SkeletonHelper.showSkeleton(tvPortraitVoiceover, 90);
            if (tvPortraitPlayer != null) SkeletonHelper.showSkeleton(tvPortraitPlayer, 60);

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
        });
    }

    private void updatePortraitVoiceoverPlayerUI() {
        safeRunOnUiThread(() -> {
            if (tvPortraitVoiceover == null || tvPortraitPlayer == null) return;
            if (isOfflineMode) {
                SkeletonHelper.hideSkeleton(tvPortraitVoiceover, getOfflineVoiceoverName());
                SkeletonHelper.hideSkeleton(tvPortraitPlayer, getOfflinePlayerTypeName());
                com.example.animelib.util.ItemAnimationUtils.animateTextChange(tvPortraitVoiceover);
                return;
            }
            if (playersManager == null) return;
            EpisodeResponse.PlayerData cur = playersManager.getCurrentPlayerData();
            if (cur != null) {
                String teamName = (cur.getTeam() != null && cur.getTeam().getName() != null && !cur.getTeam().getName().isEmpty())
                        ? cur.getTeam().getName()
                        : ((cur.getTranslationType() != null && cur.getTranslationType().getLabel() != null)
                                ? cur.getTranslationType().getLabel() : "Озвучка");
                SkeletonHelper.hideSkeleton(tvPortraitVoiceover, teamName);

                String playerName = cur.getPlayer();
                if (playerName != null && !playerName.isEmpty()) {
                    playerName = playerName.substring(0, 1).toUpperCase() + playerName.substring(1);
                } else {
                    playerName = "Плеер";
                }
                SkeletonHelper.hideSkeleton(tvPortraitPlayer, playerName);
                com.example.animelib.util.ItemAnimationUtils.animateTextChange(tvPortraitVoiceover);
            } else {
                SkeletonHelper.hideSkeleton(tvPortraitVoiceover, "Выбрать озвучку");
                SkeletonHelper.hideSkeleton(tvPortraitPlayer, "");
            }
        });
    }

    private void updatePortraitHeaderTitlesUI() {
        safeRunOnUiThread(() -> {
            if (tvPortraitAnimeTitle != null) {
                String animeTitleStr = animeTitleView != null ? animeTitleView.getText().toString() : "";
                if (animeTitleStr.isEmpty() && getIntent() != null) {
                    animeTitleStr = getIntent().getStringExtra("EXTRA_ANIME_TITLE");
                }
                if (!isOfflineMode && animeTitleView != null && Boolean.TRUE.equals(animeTitleView.getTag(R.id.tag_skeleton_active))) {
                    SkeletonHelper.showSkeleton(tvPortraitAnimeTitle, 180);
                } else if (animeTitleStr != null && !animeTitleStr.isEmpty()) {
                    SkeletonHelper.hideSkeleton(tvPortraitAnimeTitle, animeTitleStr);
                } else {
                    SkeletonHelper.hideSkeleton(tvPortraitAnimeTitle, "");
                }
            }
            if (tvPortraitEpisodeTitle != null) {
                EpisodesListResponse.EpisodeItem curEp = episodesManager != null ? episodesManager.getCurrentEpisode() : null;
                if (curEp != null) {
                    String num = curEp.getNumber() != null ? curEp.getNumber().trim() : "";
                    String name = cleanEpisodeName(curEp.getName(), num);
                    String epTitle = (!num.isEmpty() ? (num + " серия") : "") + (!name.isEmpty() ? (", " + name) : "");
                    SkeletonHelper.hideSkeleton(tvPortraitEpisodeTitle, epTitle);
                } else if (isOfflineMode) {
                    String epNum = getIntent() != null ? getIntent().getStringExtra("EXTRA_EPISODE_NUMBER") : null;
                    String epName = getIntent() != null ? getIntent().getStringExtra("EXTRA_EPISODE_TITLE") : null;
                    String cleanName = cleanEpisodeName(epName, epNum);
                    String epTitle = (epNum != null && !epNum.isEmpty() ? (epNum + " серия") : "") + (!cleanName.isEmpty() ? (", " + cleanName) : "");
                    SkeletonHelper.hideSkeleton(tvPortraitEpisodeTitle, epTitle);
                } else {
                    SkeletonHelper.showSkeleton(tvPortraitEpisodeTitle, 120);
                }
            }
        });
    }
    
    /**
     * Инициализация связанных тайтлов
     */
    private void initializeRelatedTitles() {
        if (relatedTitlesRecyclerView == null) {
            Log.w("VideoPlayer", "Related titles RecyclerView is null");
            return;
        }
        
        // Setup RecyclerView
        relatedTitlesRecyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this, 
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
        
        // Initialize adapter
        relatedTitlesAdapter = new HorizontalRelatedTitlesAdapter(
                new java.util.ArrayList<>(), 
                this::onRelatedTitleSelected
        );
        
        // Initialize RelatedTitlesManager
        relatedTitlesManager = new RelatedTitlesManager();
        TextView relatedTitlesHeader = findViewById(R.id.relatedTitlesHeader);
        LinearLayout relatedAnimeInfoContainer = findViewById(R.id.relatedAnimeInfoContainer);
        ImageView relatedAnimeCover = findViewById(R.id.relatedAnimeCover);
        TextView relatedAnimeTitle = findViewById(R.id.relatedAnimeTitle);
        TextView relatedAnimeEngTitle = findViewById(R.id.relatedAnimeEngTitle);
        com.google.android.material.chip.Chip relatedAnimeTypeChip = findViewById(R.id.relatedAnimeTypeChip);
        com.google.android.material.chip.Chip relatedAnimeStatusChip = findViewById(R.id.relatedAnimeStatusChip);
        com.google.android.material.chip.Chip relatedAnimeYearChip = findViewById(R.id.relatedAnimeYearChip);
        com.google.android.material.chip.Chip relatedAnimeAgeChip = findViewById(R.id.relatedAnimeAgeChip);
        TextView relatedAnimeRating = findViewById(R.id.relatedAnimeRating);
        TextView relatedAnimeVotes = findViewById(R.id.relatedAnimeVotes);
        TextView relatedAnimeEpisodes = findViewById(R.id.relatedAnimeEpisodes);
        relatedTitlesManager.initialize(relatedTitlesOverlay, relatedTitlesDimOverlay, relatedTitlesRecyclerView,
                                        relatedTitlesHeader, relatedAnimeInfoContainer, relatedAnimeCover,
                                        relatedAnimeTitle, relatedAnimeEngTitle, relatedAnimeTypeChip, 
                                        relatedAnimeStatusChip, relatedAnimeYearChip, relatedAnimeAgeChip,
                                        relatedAnimeRating, relatedAnimeVotes, relatedAnimeEpisodes);
        relatedTitlesManager.setAdapter(relatedTitlesAdapter);
        
        // Устанавливаем listener для управления интерфейсом плеера
        relatedTitlesManager.setPlayerInterfaceControlListener(new RelatedTitlesManager.OnPlayerInterfaceControlListener() {
            @Override
            public void onHidePlayerInterface() {
                // НЕ скрываем интерфейс полностью, только меняем alpha через onPlayerInterfaceAlpha
            }
            
            @Override
            public void onShowPlayerInterface() {
                // Показываем интерфейс плеера
                if (playerView != null) {
                    playerView.showController();
                }
                if (ambientLightManager != null) {
                    ambientLightManager.resume();
                }
            }
            
            @Override
            public void onPlayerInterfaceAlpha(float alpha) {
                // Плавно меняем прозрачность интерфейса плеера
                View controller = findViewById(R.id.exo_controller);
                if (controller != null) {
                    controller.setAlpha(alpha);
                }
            }
        });
        
        // Настраиваем drag для закрытия панели связанных тайтлов
        setupRelatedTitlesDragToClose();
        
        // Load related titles if we have anime info
        if (currentAnimeId != null) {
            loadRelatedTitles();
        }
    }
    
    /**
     * Настраивает drag-to-close для панели связанных тайтлов (BottomSheet style)
     */
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private void setupRelatedTitlesDragToClose() {
        if (relatedTitlesOverlay == null) return;
        
        final float[] initialY = {0f};
        final float[] lastY = {0f};
        final boolean[] isDragging = {false};
        final int touchSlop = android.view.ViewConfiguration.get(this).getScaledTouchSlop();
        
        relatedTitlesOverlay.setOnTouchListener((v, event) -> {
            // Обрабатываем только если панель открыта
            if (relatedTitlesManager == null || !relatedTitlesManager.isRelatedTitlesVisible()) {
                return false;
            }
            
            float currentY = event.getRawY();
            
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    initialY[0] = currentY;
                    lastY[0] = currentY;
                    isDragging[0] = false;
                    // Останавливаем текущие анимации
                    relatedTitlesOverlay.animate().cancel();
                    return true;
                    
                case android.view.MotionEvent.ACTION_MOVE:
                    float deltaY = currentY - initialY[0];
                    float moveDelta = currentY - lastY[0];
                    
                    // Начинаем drag если прошли touchSlop
                    if (!isDragging[0] && Math.abs(deltaY) > touchSlop) {
                        isDragging[0] = true;
                        // Запрещаем родителю перехватывать события
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    
                    if (isDragging[0]) {
                        // ПАНЕЛЬ ОТКРЫВАЕТСЯ СВЕРХУ ВНИЗ (translationY: -height -> 0)
                        // Закрываем её тягой ВВЕРХ (deltaY < 0)
                        
                        if (deltaY < 0) {
                            // Тянем ВВЕРХ (закрытие) - translationY становится отрицательным
                            relatedTitlesOverlay.setTranslationY(deltaY);
                        } else {
                            // Тянем ВНИЗ - не даём тянуть дальше (панель уже открыта)
                            relatedTitlesOverlay.setTranslationY(0);
                        }
                        
                        // Вычисляем прогресс (1.0 = открыто на месте, 0.0 = закрыто наверху)
                        int screenHeight = getResources().getDisplayMetrics().heightPixels;
                        float currentTranslation = relatedTitlesOverlay.getTranslationY();
                        // currentTranslation: 0 (открыто) -> -screenHeight (закрыто)
                        float progress = 1.0f + (currentTranslation / screenHeight);
                        progress = Math.max(0f, Math.min(1f, progress));
                        
                        // Обновляем затемнение
                        if (relatedTitlesDimOverlay != null) {
                            relatedTitlesDimOverlay.setAlpha(progress);
                        }
                        
                        // Обновляем прозрачность интерфейса плеера
                        View controller = findViewById(R.id.exo_controller);
                        if (controller != null) {
                            controller.setAlpha(1f - progress);
                        }
                    }
                    
                    lastY[0] = currentY;
                    return true;
                    
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    if (isDragging[0]) {
                        float finalDeltaY = currentY - initialY[0];
                        float velocity = lastY[0] - currentY; // Скорость вверх = положительная
                        
                        int screenHeight = getResources().getDisplayMetrics().heightPixels;
                        float dismissThreshold = screenHeight * 0.15f; // Уменьшен порог с 0.3 до 0.15
                        
                        // ПАНЕЛЬ ЗАКРЫВАЕТСЯ ТЯГОЙ ВВЕРХ (finalDeltaY < 0)
                        // Решаем закрывать или нет на основе расстояния и скорости
                        boolean shouldDismiss = (finalDeltaY < -dismissThreshold) || (velocity > 50 && finalDeltaY < 0);
                        
                        if (shouldDismiss) {
                            // Закрываем панель
                            relatedTitlesManager.hideRelatedTitles();
                            if (ambientLightManager != null) {
                                ambientLightManager.resume();
                            }
                        } else {
                            // Возвращаем на место
                            relatedTitlesOverlay.animate()
                                    .translationY(0f)
                                    .setDuration(200)
                                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                    .setUpdateListener(animation -> {
                                        float currentTranslation = relatedTitlesOverlay.getTranslationY();
                                        float progress = 1.0f + (currentTranslation / screenHeight);
                                        
                                        if (relatedTitlesDimOverlay != null) {
                                            relatedTitlesDimOverlay.setAlpha(progress);
                                        }
                                        
                                        View controller = findViewById(R.id.exo_controller);
                                        if (controller != null) {
                                            controller.setAlpha(1f - progress);
                                        }
                                    })
                                    .start();
                        }
                        
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                    }
                    isDragging[0] = false;
                    return true;
            }
            
            return false;
        });
    }
    
    /**
     * Загрузка связанных тайтлов
     */
    private void loadRelatedTitles() {
        if (currentAnimeId == null) {
            Log.w("VideoPlayer", "No anime ID available for loading related titles");
            return;
        }
        
        // Extract anime slug from current anime ID or URL
        String animeSlug = currentAnimeId;
        if (animeUrl != null && !animeUrl.isEmpty()) {
            animeSlug = ApiService.extractMediaSlugFromUrl(animeUrl);
        }
        
        if (animeSlug == null || animeSlug.isEmpty()) {
            Log.w("VideoPlayer", "No anime slug available for loading related titles");
            return;
        }
        
        Log.d("VideoPlayer", "Loading related titles for anime: " + animeSlug);
        
        apiService.getRelatedTitles(animeSlug, new ApiService.RelatedTitlesCallback() {
            @Override
            public void onRelatedTitlesReceived(RelatedTitlesResponse response) {
                runOnUiThread(() -> {
                    if (response.getData() != null && !response.getData().isEmpty()) {
                        Log.d("VideoPlayer", "Related titles loaded: " + response.getData().size());
                        showRelatedTitles(response.getData());
                    } else {
                        Log.d("VideoPlayer", "No related titles found");
                        if (portraitRelatedTitlesContainer != null) {
                            portraitRelatedTitlesContainer.setVisibility(View.GONE);
                        }
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                Log.e("VideoPlayer", "Error loading related titles: " + error);
                runOnUiThread(() -> {
                    if (portraitRelatedTitlesContainer != null) {
                        portraitRelatedTitlesContainer.setVisibility(View.GONE);
                    }
                });
            }
        });
    }
    
    /**
     * Показать связанные тайтлы
     */
    private void showRelatedTitles(List<RelatedTitlesResponse.RelatedTitle> relatedTitles) {
        if (relatedTitlesManager != null) {
            relatedTitlesManager.updateRelatedTitles(relatedTitles);
        }
        if (portraitRelatedTitlesAdapter != null) {
            portraitRelatedTitlesAdapter.updateData(relatedTitles);
            if (portraitRelatedTitlesContainer != null) {
                if (portraitRelatedTitlesAdapter.getItemCount() > 0 && !isOfflineMode) {
                    portraitRelatedTitlesContainer.setVisibility(View.VISIBLE);
                } else {
                    portraitRelatedTitlesContainer.setVisibility(View.GONE);
                }
            }
        }
    }
    
    /**
     * Обработчик выбора связанного тайтла
     */
    /**
     * Устанавливает информацию об аниме в панель связанных тайтлов
     */
    private void setAnimeInfoToRelatedPanel(AnimeInfoResponse.Data animeData) {
        if (relatedTitlesManager == null) {
            Log.w("VideoPlayer", "relatedTitlesManager is null");
            return;
        }

        // Получаем обложку
        String coverUrl = null;
        if (animeData.getCover() != null && animeData.getCover().getDefaultUrl() != null) {
            coverUrl = animeData.getCover().getDefaultUrl();
            currentPosterUrl = coverUrl;
        }

        // Получаем название (приоритет: русское -> английское -> оригинальное)
        String title = animeData.getRus_name();
        if (title == null || title.trim().isEmpty()) {
            title = animeData.getEng_name();
        }
        if (title == null || title.trim().isEmpty()) {
            title = animeData.getName();
        }
        if (title == null) {
            title = "Без названия";
        }

        // Английское название (если русское название используется)
        String engTitle = null;
        if (animeData.getRus_name() != null && !animeData.getRus_name().trim().isEmpty()) {
            engTitle = animeData.getEng_name();
        }

        // Тип
        String type = null;
        if (animeData.getType() != null && animeData.getType().getLabel() != null) {
            type = animeData.getType().getLabel();
        }

        // Статус
        String status = null;
        if (animeData.getStatus() != null && animeData.getStatus().getLabel() != null) {
            status = animeData.getStatus().getLabel();
        }

        // Год выхода
        String year = null;
        if (animeData.getReleaseDateString() != null && !animeData.getReleaseDateString().isEmpty()) {
            // Извлекаем год из даты (например "2024" из "15.10.2024" или просто "2024")
            try {
                String dateStr = animeData.getReleaseDateString();
                if (dateStr.contains(".")) {
                    // Формат "DD.MM.YYYY"
                    String[] parts = dateStr.split("\\.");
                    if (parts.length >= 3) {
                        year = parts[2];
                    }
                } else if (dateStr.matches("\\d{4}")) {
                    // Формат "YYYY"
                    year = dateStr;
                }
            } catch (Exception e) {
                Log.w("VideoPlayer", "Failed to parse year from: " + animeData.getReleaseDateString());
            }
        }

        // Возрастной рейтинг
        String ageRating = null;
        if (animeData.getAgeRestriction() != null && animeData.getAgeRestriction().getLabel() != null) {
            ageRating = animeData.getAgeRestriction().getLabel();
        }

        // Рейтинг
        String rating = "";
        String votes = "";
        if (animeData.getRating() != null) {
            if (animeData.getRating().getAverageFormated() != null) {
                rating = animeData.getRating().getAverageFormated();
            }
            if (animeData.getRating().getVotesFormated() != null) {
                votes = "(" + animeData.getRating().getVotesFormated() + ")";
            }
        }

        // Количество эпизодов
        String episodes = null;
        if (animeData.getItems_count() != null) {
            episodes = "Эпизоды: " + animeData.getItems_count().getUploaded() +
                      " / " + animeData.getItems_count().getTotal();
        }

        Log.d("VideoPlayer", "Setting anime info to related panel: title=" + title + 
              ", type=" + type + ", status=" + status + ", year=" + year);
        
        relatedTitlesManager.setAnimeInfo(coverUrl, title, engTitle, type, status, year, 
                                         ageRating, rating, votes, episodes);
    }
    
    /**
     * Обработчик выбора связанного тайтла (открывает WebView в BottomSheet)
     * @param media Объект медиа выбранного тайтла
     */
    private void onRelatedTitleSelected(RelatedTitlesResponse.Media media) {
        if (isFinishing() || isDestroyed()) return;
        if (media == null) {
            Log.w("VideoPlayer", "Related title media is null");
            return;
        }

        String titleName = HorizontalRelatedTitlesAdapter.getDisplayTitle(media);
        String webUrl = HorizontalRelatedTitlesAdapter.buildWebUrl(media, this);

        Log.d("VideoPlayer", "Related title selected: " + titleName + " -> " + webUrl);

        if (webUrl == null || webUrl.isEmpty()) {
            CustomToast.showWarning(this, "Не удалось сформировать ссылку для тайтла");
            return;
        }

        try {
            // Если открыта верхняя шторка "Связанное", скроем её
            if (relatedTitlesManager != null && relatedTitlesManager.isRelatedTitlesVisible()) {
                relatedTitlesManager.hideRelatedTitles();
            }

            // Открываем новый BottomSheet с WebView для тайтла
            TitleWebViewBottomSheet bottomSheet = new TitleWebViewBottomSheet(this, titleName, webUrl);
            bottomSheet.show();
        } catch (Throwable t) {
            Log.e("VideoPlayer", "Error showing TitleWebViewBottomSheet: " + t.getMessage(), t);
            CustomToast.showWarning(this, "Не удалось открыть страницу тайтла");
        }
    }

    /**
     * Настройка всех event listeners
     */
    private void setupEventListeners() {
        // Setup controller visibility listener
        setupControllerVisibilityListener();
        
        // Setup button click listeners
        setupButtonClickListeners();
        
        // Setup overlay touch listener
        setupOverlayTouchListener();
        
        // Setup player listener
        setupPlayerListener();
        
        // Setup gestures callback
        setupGesturesCallback();
        
        // Setup timebar scrub listener
        setupTimeBarListener();
    }
    
    /**
     * Настройка listener'а перемотки таймбара
     */
    private void setupTimeBarListener() {
        if (timeBar != null) {
            timeBar.addListener(new androidx.media3.ui.TimeBar.OnScrubListener() {
                @Override
                public void onScrubStart(@NonNull androidx.media3.ui.TimeBar timeBar, long position) {
                    isScrubbingTimeBar = true;
                    startSeekingState();
                }

                @Override
                public void onScrubMove(@NonNull androidx.media3.ui.TimeBar timeBar, long position) {
                    isScrubbingTimeBar = true;
                    startSeekingState();
                }

                @Override
                public void onScrubStop(@NonNull androidx.media3.ui.TimeBar timeBar, long position, boolean canceled) {
                    isScrubbingTimeBar = false;
                    if (!canceled) {
                        startSeekingState();
                        if (player != null) {
                            player.seekTo(position);
                        }
                        scheduleEndSeekingState(600);
                    } else {
                        scheduleEndSeekingState(0);
                    }
                }
            });
        }
    }
    
    /**
     * Настройка listener'а видимости контроллера
     */
    private void setupControllerVisibilityListener() {
        playerView.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener) visibility -> {
            boolean shouldBeVisible = visibility == View.VISIBLE;
            
            // Animate controller visibility with alpha
            animateControllerVisibility(shouldBeVisible);
            
            isControllerVisible = shouldBeVisible;
            
            // Update navigation buttons visibility
            episodesManager.updateEpisodeNavigationButtonsVisibility();
            
            // Update button visibility
            updateControllerButtonsVisibility(isControllerVisible);

            updatePlayPauseAndLoadingState(false);
        });
    }
    
    /**
     * Анимация видимости контроллера через alpha
     */
    private void animateControllerVisibility(boolean visible) {
        if (controllerView == null) return;
        
        // Cancel any existing animation
        controllerView.animate().cancel();
        if (downloadProgressText != null) {
            downloadProgressText.animate().cancel();
        }
        
        if (visible) {
            // Show controller with fade in
            controllerView.setVisibility(View.VISIBLE);
            controllerView.animate()
                    .alpha(1.0f)
                    .setDuration(200)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .withStartAction(() -> {
                        // Уведомляем TimecodeManager что контроллер стал видимым
                        if (timecodeManager != null) {
                            timecodeManager.setControllerVisibility(true);
                        }
                    })
                    .start();

            boolean isPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
            if (isDownloading && downloadProgressText != null && !isPortrait) {
                downloadProgressText.setVisibility(View.VISIBLE);
                downloadProgressText.animate()
                        .alpha(1.0f)
                        .setDuration(200)
                        .start();
            } else if (downloadProgressText != null) {
                downloadProgressText.setVisibility(View.GONE);
            }
        } else {
            // Hide controller with fade out
            controllerView.animate()
                    .alpha(0.0f)
                    .setDuration(150)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .withStartAction(() -> {
                        // Уведомляем TimecodeManager что контроллер стал скрытым
                        if (timecodeManager != null) {
                            timecodeManager.setControllerVisibility(false);
                        }
                    })
                    .withEndAction(() -> {
                        // Set visibility to GONE after animation completes
                        if (controllerView.getAlpha() == 0.0f) {
                            controllerView.setVisibility(View.GONE);
                        }
                        updatePlayPauseAndLoadingState(true);
                    })
                    .start();

            if (downloadProgressText != null && downloadProgressText.getVisibility() == View.VISIBLE) {
                downloadProgressText.animate()
                        .alpha(0.0f)
                        .setDuration(150)
                        .withEndAction(() -> {
                            downloadProgressText.setVisibility(View.GONE);
                        })
                        .start();
            }
        }
        updatePlayPauseAndLoadingState(true);
    }
    
    
    /**
     * Обновление видимости кнопок контроллера
     */
    private void updateControllerButtonsVisibility(boolean visible) {
        boolean isPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        int visibility = visible ? View.VISIBLE : View.GONE;
        
        if (settingsButton != null) settingsButton.setVisibility(visibility);
        updateSettingsQualityTag();
        
        if (menuToggleFullscreen != null) {
            menuToggleFullscreen.setVisibility(visibility);
            menuToggleFullscreen.setImageResource(isPortrait ? R.drawable.ic_fullscreen : R.drawable.ic_fullscreen_exit);
        }

        if (!isPortrait) {
            if (episodesMenuButton != null) episodesMenuButton.setVisibility(visibility);
            if (menuToggleButton != null) menuToggleButton.setVisibility(visibility);
            commentsManager.updateCommentsButtonVisibility(visible);
        } else {
            if (episodesMenuButton != null) episodesMenuButton.setVisibility(View.GONE);
            if (menuToggleButton != null) menuToggleButton.setVisibility(View.GONE);
            commentsManager.updateCommentsButtonVisibility(false);
        }
    }

    private void updateSettingsQualityTag() {
        if (settingsQualityTag != null) {
            if (settingsButton != null && settingsButton.getVisibility() != View.VISIBLE) {
                settingsQualityTag.setVisibility(View.GONE);
                return;
            }
            String tag = com.example.animelib.util.FloatingBottomSheetUtils.getQualityTag(preferredQuality);
            if (tag != null && !tag.isEmpty()) {
                settingsQualityTag.setText(tag);
                settingsQualityTag.setVisibility(View.VISIBLE);
            } else {
                settingsQualityTag.setVisibility(View.GONE);
            }
        }
    }

    private void showSurroundSoundToast(boolean enabled) {
        // Toast notification disabled by user request
    }
    
    /**
     * Настройка click listeners для кнопок
     */
    private void setupButtonClickListeners() {
        // Navigation buttons
        if (prevEpisodeButton != null) {
            prevEpisodeButton.setOnClickListener(v -> {
                com.example.animelib.util.ItemAnimationUtils.animateItemClick(v, () -> {
                    if (prevEpisodeButton.isEnabled()) {
                        episodesManager.navigateToPreviousEpisode();
                    }
                });
            });
        }
        
        if (nextEpisodeButton != null) {
            nextEpisodeButton.setOnClickListener(v -> {
                com.example.animelib.util.ItemAnimationUtils.animateItemClick(v, () -> {
                    if (nextEpisodeButton.isEnabled()) {
                        episodesManager.navigateToNextEpisode();
                    }
                });
            });
        }
        
        // Menu buttons
        if (menuToggleButton != null) {
            menuToggleButton.setOnClickListener(v -> toggleMenu());
        }
        
        if (closeMenuButton != null) {
            closeMenuButton.setOnClickListener(v -> {
                if (playersManager.getCurrentPlayerData() != null) {
                    playersManager.hideMenu();
                } else {
                    finish();
                }
            });
        }
        
        // Control buttons
        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> showSettingsDialog());
        }
        
        if (menuToggleFullscreen != null) {
            menuToggleFullscreen.setOnClickListener(v -> toggleOrientation());
        }
        
        if (pipButton != null) {
            pipButton.setOnClickListener(v -> {
                // Автоматически сохраняем закладку перед переходом в PiP
                autoSaveBookmark();
                enterPictureInPictureMode();
            });
        }
        
        if (ibClosePlayer != null) {
            ibClosePlayer.setOnClickListener(v -> {
                // Автоматически сохраняем закладку перед закрытием
                autoSaveBookmark();
                finish();
            });
        }

        if (episodesMenuButton != null) {
            episodesMenuButton.setOnClickListener(v -> toggleEpisodesInController());
        }

        View.OnClickListener downloadClickListener = v -> {
            if (DownloadService.isRunning()) {
                showDownloadProgressBottomSheet();
            } else {
                showDownloadBottomSheet();
            }
        };

        if (downloadButton != null) {
            downloadButton.setOnClickListener(downloadClickListener);
        }
        if (downloadButtonTop != null) {
            downloadButtonTop.setOnClickListener(downloadClickListener);
        }
        if (btnDownloadFromMenu != null) {
            btnDownloadFromMenu.setOnClickListener(downloadClickListener);
        }
        if (portraitDownloadButton != null) {
            portraitDownloadButton.setOnClickListener(downloadClickListener);
        }
        
        if (bookmarkButton != null) {
            Log.d("VideoPlayer", "Bookmark button found, visibility: " + bookmarkButton.getVisibility() + 
                               ", enabled: " + bookmarkButton.isEnabled() + 
                               ", clickable: " + bookmarkButton.isClickable());
            
            // Изначально отключаем кнопку до готовности плеера
            bookmarkButton.setEnabled(false);
            bookmarkButton.setClickable(false);
            
            bookmarkButton.setOnClickListener(v -> {
                Log.d("VideoPlayer", "Bookmark button clicked!");
                addBookmark();
            });
        } else {
            Log.e("VideoPlayer", "Bookmark button is null!");
        }
        
        // Setup player control buttons
        setupPlayerControlButtons();
    }
    
    /**
     * Настройка touch listener для overlay
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupOverlayTouchListener() {
        if (menuOverlay == null) return;
        
        menuOverlay.setVisibility(View.GONE);
        menuOverlay.setAlpha(0f);
        menuOverlay.setClickable(false);
        
        menuOverlay.setOnTouchListener((v, event) -> {
            if (!playersManager.isMenuVisible() && !commentsManager.isCommentsVisible()) return false;
            if (event.getAction() != MotionEvent.ACTION_DOWN) return false;
            
            int x = (int) event.getRawX();
            int y = (int) event.getRawY();
            boolean insidePanel = false;
            
            if (slidingMenuPanel != null && playersManager.isMenuVisible()) {
                Rect panelRect = new Rect();
                slidingMenuPanel.getGlobalVisibleRect(panelRect);
                insidePanel = panelRect.contains(x, y);
            }
            
            if (!insidePanel) {
                if (playersManager.isMenuVisible()) playersManager.hideMenu();
                if (commentsManager.isCommentsVisible()) commentsManager.hideCommentsPanel();
                return true;
            }
            return false;
        });
    }
    
    /**
     * Настройка player listener
     */
    private void setupPlayerListener() {
        if (player == null) return;
        if (playerEventListener != null) {
            player.removeListener(playerEventListener);
        }
        playerEventListener = new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Log.d("PlayerControls", "Playback state changed: " + playbackState);
                if (playbackState == Player.STATE_READY) {
                    if (hasRenderedFirstFrame || (player != null && player.isPlaying())) {
                        isVideoLoading = false;
                    }
                } else if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                    isSeeking = false;
                    isVideoLoading = false;
                    if (seekResetHandler != null) {
                        seekResetHandler.removeCallbacksAndMessages(null);
                    }
                }
                updatePlayerControlsState();
                if (episodesManager != null) {
                    episodesManager.updateEpisodeNavigationButtonsVisibility();
                }
                startBufferingMonitoring();
                updatePlayPauseAndLoadingState(true);

                // Handle auto-play next episode
                if (playbackState == Player.STATE_ENDED && autoPlay) {
                    Log.d("PlayerControls", "Video ended, checking for next episode");
                    if (episodesManager != null && episodesManager.getNextEpisode() != null) {
                        showNextEpisodeOverlay();
                    } else {
                        Log.d("PlayerControls", "No next episode available");
                    }
                }
            }

            @Override
            public void onPositionDiscontinuity(@NonNull Player.PositionInfo oldPosition, @NonNull Player.PositionInfo newPosition, int reason) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK || reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                    startSeekingState();
                    scheduleEndSeekingState(600);
                } else {
                    startBufferingMonitoring();
                    updatePlayPauseAndLoadingState(true);
                }
            }

            @Override
            public void onRenderedFirstFrame() {
                Log.d("PlayerControls", "First frame rendered!");
                hasRenderedFirstFrame = true;
                isVideoLoading = false;
                isSeeking = false;
                startBufferingMonitoring();
                updatePlayPauseAndLoadingState(true);
            }

            @Override
            public void onIsLoadingChanged(boolean isLoading) {
                startBufferingMonitoring();
                updatePlayPauseAndLoadingState(true);
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Log.d("PlayerControls", "Is playing changed: " + isPlaying);
                if (isPlaying) {
                    hasRenderedFirstFrame = true;
                    isVideoLoading = false;
                    if (!isScrubbingTimeBar) {
                        isSeeking = false;
                        if (seekResetHandler != null) {
                            seekResetHandler.removeCallbacksAndMessages(null);
                        }
                    }
                    startViewProgressTracking();
                } else {
                    stopViewProgressTracking();
                }
                updatePlayerControlsState();
                startBufferingMonitoring();
                updatePlayPauseAndLoadingState(true);
                if (isInPictureInPictureMode) {
                    updatePictureInPictureParams();
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.e("PlayerControls", "Player error: " + error.getMessage());
                isSeeking = false;
                isVideoLoading = false;
                if (seekResetHandler != null) {
                    seekResetHandler.removeCallbacksAndMessages(null);
                }
                updatePlayerControlsState();
                updatePlayPauseAndLoadingState(true);
            }

            @Override
            public void onEvents(@NonNull Player player, @NonNull Player.Events events) {
                updatePlayPauseAndLoadingState(true);
            }
        };
        player.addListener(playerEventListener);
    }

    private void startViewProgressTracking() {
        stopViewProgressTracking();
        viewProgressRunnable = new Runnable() {
            @Override
            public void run() {
                checkPlaybackViewProgress();
                if (player != null && player.isPlaying() && !isCurrentEpisodeMarkedViewed) {
                    viewProgressHandler.postDelayed(this, 1000);
                }
            }
        };
        viewProgressHandler.post(viewProgressRunnable);
    }

    private void stopViewProgressTracking() {
        if (viewProgressRunnable != null) {
            viewProgressHandler.removeCallbacks(viewProgressRunnable);
            viewProgressRunnable = null;
        }
    }

    private void checkPlaybackViewProgress() {
        if (isCurrentEpisodeMarkedViewed || player == null) return;
        long duration = player.getDuration();
        long currentPos = player.getCurrentPosition();

        if (duration > 0 && currentPos >= (long) (duration * 0.60)) {
            isCurrentEpisodeMarkedViewed = true;
            Log.d("VideoPlayer", "60% view threshold reached: " + currentPos + "/" + duration + "ms");

            String animeId = currentAnimeId;
            if (animeId == null || animeId.isEmpty()) {
                animeId = (getIntent() != null) ? getIntent().getStringExtra("EXTRA_ANIME_ID") : null;
            }

            int playerId = 0;
            if (playersManager != null && playersManager.getCurrentPlayerData() != null) {
                playerId = playersManager.getCurrentPlayerData().getId();
            }

            if (animeId != null && !animeId.isEmpty() && playerId > 0) {
                Log.d("VideoPlayer", "Enqueuing VIEW task for animeId: " + animeId + ", playerId: " + playerId);
                com.example.animelib.managers.OfflineSyncManager.getInstance(this).enqueueViewTask(animeId, playerId);
            } else {
                Log.w("VideoPlayer", "Cannot enqueue VIEW task: animeId=" + animeId + ", playerId=" + playerId);
            }
        }
    }
    
    /**
     * Настройка gestures callback
     */
    private void setupGesturesCallback() {
        gesturesManager.setGestureCallback(new GesturesManager.GestureCallback() {
            @Override
            public void onSeekGesture(long seekPosition) {
                Log.d("VideoPlayer", "Seek gesture: " + seekPosition);
                startSeekingState();
                scheduleEndSeekingState(600);
            }
            
            @Override
            public void onSpeedChange(float speed) {
                Log.d("VideoPlayer", "Speed change: " + speed);
            }
            
            @Override
            public void updatePlayLoadingIndicator(int playbackState) {
                VideoPlayerActivity.this.updatePlayLoadingIndicator(playbackState);
            }
            
            @Override
            public void onEpisodesSwipeUp() {
                Log.d("VideoPlayer", "Episodes swipe up detected");
                // Открываем панель эпизодов
                if (episodesManager != null) {
                    episodesManager.showEpisodesMenu();
                }
            }
            
            @Override
            public void onEpisodesSwipeDown() {
                Log.d("VideoPlayer", "Episodes swipe down detected");
                // Закрываем панель эпизодов
                if (episodesManager != null) {
                    episodesManager.hideEpisodesMenu();
                }
            }
            
            @Override
            public void onCommentsSwipeFromRight() {
                Log.d("VideoPlayer", "Comments swipe from right detected");
                // Открываем панель комментариев
                if (commentsManager != null) {
                    commentsManager.showCommentsPanel();
                }
            }
            
            @Override
            public void onPlayersSwipeFromRight() {
                Log.d("VideoPlayer", "Players swipe from right detected");
                // Открываем панель озвучек
                if (playersManager != null) {
                    playersManager.showMenu();
                }
            }
            
            @Override
            public void onEpisodesDragProgress(float progress) {
                // ВЕРТИКАЛЬНЫЕ ЖЕСТЫ: Эпизоды теперь обрабатываются в VerticalGesturesManager
                // Этот метод больше не вызывается для вертикальных свайпов
            }
            
            @Override
            public void onRelatedTitlesDragProgress(float progress) {
                // ВЕРТИКАЛЬНЫЕ ЖЕСТЫ: Related titles теперь обрабатываются в VerticalGesturesManager
                // Этот метод больше не вызывается для вертикальных свайпов
            }
            
            @Override
            public void onCommentsDragProgress(float progress) {
                if (isOfflineMode) return;
                
                // Скрываем худ плеера при начале drag панели комментариев
                if (progress > 0 && playerView != null) {
                    playerView.hideController();
                }
                
                // Обновляем прогресс вытягивания панели комментариев
                if (commentsPanelContainer != null) {
                    commentsPanelContainer.setDragProgress(progress);
                }
            }
            
            @Override
            public void onPlayersDragProgress(float progress) {
                if (isOfflineMode) return;
                
                // Скрываем худ плеера при начале drag панели озвучек
                if (progress > 0 && playerView != null) {
                    playerView.hideController();
                }
                
                // Обновляем прогресс вытягивания панели озвучек
                if (menuPanelContainer != null) {
                    menuPanelContainer.setDragProgress(progress);
                }
            }
            
            @Override
            public void onPanelDragComplete(GesturesManager.EdgeSwipeType type, boolean shouldOpen) {
                if (isOfflineMode) return;
                Log.d("VideoPlayer", "Panel drag complete: " + type + ", shouldOpen=" + shouldOpen);
                
                // ВЕРТИКАЛЬНЫЕ ЖЕСТЫ (эпизоды и связанные тайтлы) ОБРАБАТЫВАЮТСЯ В VerticalGesturesManager
                // Здесь только горизонтальные (комментарии и озвучки)
                
                switch (type) {
                    case COMMENTS_RIGHT:
                        // Даем DraggableSidePanel завершить анимацию, затем обновляем состояние
                        if (commentsPanelContainer != null) {
                            commentsPanelContainer.completeDrag(shouldOpen);
                        }
                        // Обновляем состояние менеджера после завершения анимации
                        if (commentsManager != null) {
                            commentsManager.updateDragState(shouldOpen);
                        }
                        break;
                        
                    case PLAYERS_RIGHT:
                        // Даем DraggableSidePanel завершить анимацию, затем обновляем состояние
                        if (menuPanelContainer != null) {
                            menuPanelContainer.completeDrag(shouldOpen);
                        }
                        // Обновляем состояние менеджера после завершения анимации
                        if (playersManager != null) {
                            playersManager.updateDragState(shouldOpen);
                        }
                        break;
                }
            }
            
            @Override
            public boolean isEpisodesMenuVisible() {
                return episodesManager != null && episodesManager.isEpisodesMenuVisible();
            }
            
            @Override
            public boolean isRelatedTitlesMenuVisible() {
                return relatedTitlesManager != null && relatedTitlesManager.isRelatedTitlesVisible();
            }
            
            @Override
            public void onDoubleTapSkip(boolean isForward, int skipDurationSeconds) {
                if (player == null) {
                    Log.w("VideoPlayer", "Player is null, cannot perform skip");
                    return;
                }

                long currentPosition = player.getCurrentPosition();
                long skipDuration = skipDurationSeconds * 1000L;
                long newPosition = isForward ? currentPosition + skipDuration : currentPosition - skipDuration;
                long duration = player.getDuration();
                
                // Ограничиваем позицию границами видео
                newPosition = Math.max(0, newPosition);
                if (duration > 0) {
                    newPosition = Math.min(newPosition, duration);
                }
                
                startSeekingState();
                player.seekTo(newPosition);
                scheduleEndSeekingState(600);
                Log.d("VideoPlayer", "Double tap skip: " + (isForward ? "forward" : "backward") + 
                      " to " + (newPosition / 1000) + "s (skip=" + (skipDuration / 1000) + "s)");
            }
        });
        
        // Setup vertical gestures callback
        verticalGesturesManager.setCallback(new VerticalGesturesManager.VerticalGestureCallback() {
            @Override
            public void onEpisodesDragProgress(float progress) {
                // Показываем интерфейс плеера при начале drag (первый вызов с progress > 0)
                if (progress > 0 && playerView != null && !playerView.isControllerFullyVisible()) {
                    playerView.showController();
                    Log.d("VideoPlayer", "Showing controller on episodes drag start");
                }
                
                if (episodesManager != null) {
                    episodesManager.setDragProgress(progress);
                }
            }
            
            @Override
            public void onRelatedInfoDragProgress(float progress) {
                if (isOfflineMode) return;
                
                // Показываем интерфейс плеера при начале drag
                if (progress > 0 && playerView != null && !playerView.isControllerFullyVisible()) {
                    playerView.showController();
                    Log.d("VideoPlayer", "Showing controller on related titles drag start");
                }
                
                if (relatedTitlesManager != null) {
                    relatedTitlesManager.setDragProgress(progress);
                }
            }
            
            @Override
            public void onEpisodesDragComplete(boolean shouldOpen) {
                Log.d("VideoPlayer", "Episodes drag complete: shouldOpen=" + shouldOpen);
                if (episodesManager != null) {
                    // Используем completeDrag для корректного завершения анимации
                    episodesManager.completeDrag(shouldOpen);
                }
            }
            
            @Override
            public void onRelatedInfoDragComplete(boolean shouldOpen) {
                if (isOfflineMode) return;
                Log.d("VideoPlayer", "Related info drag complete: shouldOpen=" + shouldOpen);
                // Сначала сбрасываем эпизоды если они открыты
                if (episodesManager != null && episodesManager.isEpisodesMenuVisible()) {
                    episodesManager.resetControllerPosition();
                }
                if (relatedTitlesManager != null) {
                    relatedTitlesManager.completeDrag(shouldOpen);
                }
            }
            
            @Override
            public boolean isEpisodesOpen() {
                return episodesManager != null && episodesManager.isEpisodesMenuVisible();
            }
            
            @Override
            public boolean isRelatedInfoOpen() {
                return relatedTitlesManager != null && relatedTitlesManager.isRelatedTitlesVisible();
            }
        });
    }
    
    /**
     * Настройка начального состояния UI
     */
    private void configureInitialState() {
        // Hide controller buttons initially
        updateControllerButtonsVisibility(false);
        
        // Set initial episode navigation buttons visibility
        if (prevEpisodeButton != null) prevEpisodeButton.setVisibility(View.VISIBLE);
        if (nextEpisodeButton != null) nextEpisodeButton.setVisibility(View.VISIBLE);
        
        // Players control bar position is now managed by EpisodesManager
    }
    
    /**
     * Настройка callbacks для менеджеров
     */
    private void setupManagerCallbacks() {
        // Comments manager callbacks
        commentsManager.setVisibilityCallback(isVisible -> {
            if (isVisible && playerView != null) {
                playerView.hideController();
            }
            if (episodesManager != null) {
                episodesManager.updateEpisodeNavigationButtonsVisibility();
            }
        });
        
        // Episodes manager callbacks
        episodesManager.setEpisodeSelectionCallback(this::onEpisodeSelected);
        episodesManager.setVisibilityCallback(isVisible -> {
            if (ambientLightManager != null) {
                ambientLightManager.resume();
            }
        });
        
        // Related titles manager callbacks
        relatedTitlesManager.setVisibilityCallback(isVisible -> {
            if (ambientLightManager != null) {
                ambientLightManager.resume();
            }
        });
        episodesManager.setDataCallback(new EpisodesManager.EpisodesDataCallback() {
            @Override
            public void onEpisodesLoaded(List<EpisodesListResponse.EpisodeItem> episodes) {
                Log.d("VideoPlayer", "Episodes loaded: " + episodes.size());
                
                // Сначала пытаемся загрузить эпизод из закладки
                if (currentAnimeId != null) {
                    Log.d("VideoPlayer", "Trying to load episode from bookmark for anime: " + currentAnimeId);
                    
                    // Получаем media_slug для закладки
                    String animeUrl = getIntent().getStringExtra("anime_url");
                    String mediaSlug = null;
                    if (animeUrl != null && !animeUrl.isEmpty()) {
                        mediaSlug = ApiService.extractMediaSlugFromUrl(animeUrl);
                    }
                    
                    if (mediaSlug != null) {
                        episodesManager.getBookmarkManager().getLastEpisodeFromBookmark(mediaSlug, episodes, 
                            new BookmarkManager.LastEpisodeCallback() {
                                @Override
                                public void onLastEpisodeFound(EpisodesListResponse.EpisodeItem episode, String progress) {
                                    Log.d("VideoPlayer", "Found bookmarked episode: " + episode.getNumber() + 
                                                         ", progress: " + progress);
                                    
                                    // Сохраняем таймкод из закладки
                                    bookmarkTimecode = episodesManager.getBookmarkManager().parseTimecodeToMilliseconds(progress);
                                    Log.d("VideoPlayer", "Parsed bookmark timecode: " + progress + " -> " + bookmarkTimecode + "ms");
                                    
                                    // Устанавливаем красный цвет кнопки для эпизода с закладкой
                                    updateBookmarkButtonColor(true);
                                    
                                    isNewEpisodeSelection = true;
                                    autoPlayOnPrepare = VideoPlayerActivity.this.autoPlay;
                                    episodesManager.setCurrentEpisode(episode);
                                    commentsManager.setCurrentEpisode(episode);
                                    
                                    // СРАЗУ обновляем заголовок с номером эпизода
                                    updateEpisodeHeaderQuick();
                                    
                                    // ВАЖНО: Загружаем плееры для ПРАВИЛЬНОГО эпизода
                                    Log.d("VideoPlayer", "Loading players for bookmarked episode: " + episode.getId());
                                    playersManager.loadPlayersForEpisode(episode.getId());
                                }
                                
                                @Override
                                public void onNoBookmarkFound() {
                                    Log.d("VideoPlayer", "No bookmark found, loading first episode");
                                    loadFirstEpisode();
                                }
                            });
                    } else {
                        Log.d("VideoPlayer", "No media slug available, falling back to URL detection");
                        fallbackToUrlDetection();
                    }
                } else {
                    // Нет anime ID, используем URL detection
                    fallbackToUrlDetection();
                }
            }
            
            @Override
            public void onEpisodesError(String error) {
                Log.e("VideoPlayer", "Error loading episodes: " + error);
                showVideoErrorDialog("Ошибка загрузки озвучек", "Не удалось загрузить список эпизодов и озвучек:\n" + error, () -> {
                    if (currentAnimeId != null) {
                        loadEpisodes(currentAnimeId);
                    }
                }, true);
                initializeMenuWithoutAutoPlay();
            }
        });
        
        // Players manager callbacks
        playersManager.setPlayerSelectionCallback(this::onPlayerSelected);
        
        // Устанавливаем callback для загрузки плееров
        playersManager.setDataCallback(new PlayersManager.PlayersDataCallback() {
            @Override
            public void onPlayersLoaded(List<EpisodeResponse.PlayerData> players) {
                Log.d("VideoPlayer", "Players loaded: " + players.size() + " for episode: " + 
                      (episodesManager.getCurrentEpisode() != null ? episodesManager.getCurrentEpisode().getNumber() : "unknown"));
                
                // Проверяем нужно ли показывать меню
                // Показываем только если нет выбранной озвучки (первая загрузка)
                if (playersManager.getCurrentPlayerData() == null) {
                    Log.d("VideoPlayer", "No player selected yet, showing menu for first time");
                    showPlayerSelectionDialogWithAutoSelect(players);
                } else {
                    Log.d("VideoPlayer", "Player already selected, skipping menu (episode switch)");
                    // При переключении эпизода меню не показываем
                    // Автовыбор уже сработал в PlayersManager
                    if (menuLoadingOverlay != null) menuLoadingOverlay.setVisibility(View.GONE);
                }
            }
            
            @Override
            public void onPlayersError(String error) {
                Log.e("VideoPlayer", "Error loading players: " + error);
                showVideoErrorDialog("Ошибка загрузки плееров", "Не удалось загрузить список плееров:\n" + error, () -> {
                    EpisodesListResponse.EpisodeItem episode = episodesManager.getCurrentEpisode();
                    if (episode != null) {
                        playersManager.loadPlayersForEpisode(episode.getId());
                    }
                }, true);
            }
        });
        
        // Показываем placeholder когда нет выбранной озвучки
        playersManager.setVisibilityCallback(isVisible -> {
            if (isVisible && playerView != null) {
                playerView.hideController();
            }
            if (!isVisible && playersManager.getCurrentPlayerData() == null) {
                // Меню закрыто и нет выбранной озвучки - показываем placeholder
                showAnimeInfoPlaceholder();
            }
        });
    }
    
    /**
     * Запуск начальной загрузки
     */
    private void startInitialLoading() {
        if (isOfflineMode) {
            return;
        }
        boolean isPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        if (!isPortrait) {
            playersManager.showMenu();
            showAnimeInfoPlaceholder();
        }
        if (menuLoadingOverlay != null) {
            menuLoadingOverlay.setVisibility(View.VISIBLE);
        }
        
        // Показываем anime info placeholder и загружаем информацию
        // Placeholder будет скрыт автоматически при выборе озвучки
        loadAnimeInfoForPlaceholder();
    }
    
    /**
     * Показывает placeholder с информацией об аниме (только 1 раз при входе)
     */
    private void showAnimeInfoPlaceholder() {
        if (isOfflineMode) return;
        boolean isPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        if (isPortrait) return;
        if (hasShownInitialAnimeInfo) {
            Log.d("VideoPlayer", "Anime info placeholder already shown once, skipping");
            return;
        }
        hasShownInitialAnimeInfo = true;
        if (animeInfoPlaceholder != null) {
            animeInfoPlaceholder.setVisibility(View.VISIBLE);
            animeInfoPlaceholder.setTranslationX(0); // Показываем справа
        }
    }
    
    /**
     * Скрывает placeholder с информацией об аниме
     */
    private void hideAnimeInfoPlaceholder() {
        if (animeInfoPlaceholder != null) {
            animeInfoPlaceholder.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    animeInfoPlaceholder.setVisibility(View.GONE);
                    animeInfoPlaceholder.setAlpha(1f);
                })
                .start();
        }
    }
    
    /**
     * Загружает информацию об аниме для placeholder
     */
    private void loadAnimeInfoForPlaceholder() {
        if (animeUrl == null) return;
        
        // Извлекаем slug из URL для API
        String animeSlug = apiService.extractAnimeSlug(animeUrl);
        if (animeSlug == null) {
            Log.e("VideoPlayer", "Could not extract anime slug from URL: " + animeUrl);
            return;
        }
        
        Log.d("VideoPlayer", "Loading anime info for slug: " + animeSlug);
        apiService.fetchAnimeInfo(animeSlug, new ApiService.AnimeInfoCallback() {
            @Override
            public void onAnimeInfoReceived(AnimeInfoResponse response) {
                runOnUiThread(() -> displayAnimeInfo(response));
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("VideoPlayer", "Failed to load anime info: " + errorMessage);
            }
        });
    }
    
    /**
     * Отображает информацию об аниме в placeholder
     */
    @SuppressLint("SetTextI18n")
    private void displayAnimeInfo(AnimeInfoResponse animeInfo) {
        if (animeInfo == null || animeInfo.getData() == null) {
            Log.e("VideoPlayer", "displayAnimeInfo: animeInfo or data is null");
            return;
        }
        this.currentAnimeInfo = animeInfo;
        
        AnimeInfoResponse.Data data = animeInfo.getData();
        Log.d("VideoPlayer", "Displaying anime info: " + data.getRus_name());
        
        // Устанавливаем информацию об аниме в панель связанных тайтлов
        setAnimeInfoToRelatedPanel(data);
        
        // Название
        if (animeInfoTitle != null) {
            SkeletonHelper.hideSkeleton(animeInfoTitle, data.getRus_name() != null ? data.getRus_name() : "—");
        }
        
        // Оригинальное название
        if (animeInfoOriginalTitle != null) {
            if (data.getEng_name() != null && !data.getEng_name().isEmpty()) {
                SkeletonHelper.hideSkeleton(animeInfoOriginalTitle, data.getEng_name());
                animeInfoOriginalTitle.setVisibility(View.VISIBLE);
            } else {
                SkeletonHelper.hideSkeleton(animeInfoOriginalTitle, "");
                animeInfoOriginalTitle.setVisibility(View.GONE);
            }
        }
        
        // Год из releaseDate
        if (animeInfoYear != null) {
            String year = (data.getReleaseDate() != null && data.getReleaseDate().length() >= 4)
                    ? data.getReleaseDate().substring(0, 4) : "—";
            SkeletonHelper.hideSkeleton(animeInfoYear, year);
        }
        
        // Тип
        if (animeInfoType != null) {
            String type = (data.getType() != null && data.getType().getLabel() != null)
                    ? data.getType().getLabel().replace("TV ", "") : "—";
            SkeletonHelper.hideSkeleton(animeInfoType, type);
        }
        
        // Статус
        if (animeInfoStatus != null) {
            String statusLabel = (data.getStatus() != null && data.getStatus().getLabel() != null)
                    ? data.getStatus().getLabel() : "—";
            SkeletonHelper.hideSkeleton(animeInfoStatus, statusLabel);
        }
        
        // Рейтинг
        if (animeInfoRating != null) {
            String ratingStr = (data.getRating() != null && data.getRating().getAverageFormated() != null)
                    ? data.getRating().getAverageFormated() : "—";
            SkeletonHelper.hideSkeleton(animeInfoRating, ratingStr);
        }
        
        // Количество эпизодов
        if (animeInfoEpisodes != null) {
            String epStr = (data.getItems_count() != null && data.getItems_count().getUploaded() > 0)
                    ? (data.getItems_count().getUploaded() + " эпизодов") : "—";
            SkeletonHelper.hideSkeleton(animeInfoEpisodes, epStr);
        }
        
        // Возрастное ограничение
        if (animeInfoAge != null) {
            String ageStr = (data.getAgeRestriction() != null && data.getAgeRestriction().getLabel() != null)
                    ? data.getAgeRestriction().getLabel() : "—";
            SkeletonHelper.hideSkeleton(animeInfoAge, ageStr);
        }
        
        // Дата выхода
        if (animeInfoReleaseDate != null) {
            String relStr = data.getReleaseDateString() != null ? data.getReleaseDateString() : "—";
            SkeletonHelper.hideSkeleton(animeInfoReleaseDate, relStr);
        }
        
        // Shikimori рейтинг
        if (animeInfoShikimori != null) {
            String shikiStr = data.getShikimori_href() != null ? String.valueOf(data.getShiki_rate()) : "—";
            SkeletonHelper.hideSkeleton(animeInfoShikimori, shikiStr);
        }
        
        // Постер - загружаем через ImageLoader
        if (animeInfoPoster != null && data.getCover() != null) {
            String posterUrl = data.getCover().getDefaultUrl();
            if (posterUrl != null && !posterUrl.isEmpty()) {
                currentPosterUrl = posterUrl;
                com.example.animelib.util.ImageLoader.getInstance()
                    .loadInto(animeInfoPoster, posterUrl, R.drawable.ic_image_placeholder);
            } else {
                animeInfoPoster.setImageResource(R.drawable.ic_image_placeholder);
            }
        }
    }

    private void toggleMenu() {
        if (isOfflineMode) return;
        playersManager.toggleMenu();
    }
    
    /**
     * Показывает оверлей следующего эпизода с обратным отсчетом
     */
    private void showNextEpisodeOverlay() {
        if (nextEpisodeOverlay == null || episodesManager == null) return;
        
        // Проверяем есть ли следующий эпизод
        EpisodesListResponse.EpisodeItem nextEpisode = episodesManager.getNextEpisode();
        if (nextEpisode == null) {
            Log.d("VideoPlayer", "No next episode available");
            return;
        }
        
        Log.d("VideoPlayer", "Showing next episode overlay for episode: " + nextEpisode.getNumber());
        
        // Устанавливаем номер эпизода
        if (nextEpisodeNumber != null) {
            nextEpisodeNumber.setText("эпизод " + nextEpisode.getNumber());
        }
        
        // Сбрасываем счетчик
        countdownSeconds = 7;
        if (nextEpisodeCountdown != null) {
            nextEpisodeCountdown.setText(String.valueOf(countdownSeconds));
        }
        
        // Показываем оверлей
        nextEpisodeOverlay.setVisibility(View.VISIBLE);
        nextEpisodeOverlay.setAlpha(0f);
        nextEpisodeOverlay.animate()
            .alpha(1f)
            .setDuration(300)
            .start();
        
        // Запускаем обратный отсчет
        startCountdown();
    }
    
    /**
     * Запускает обратный отсчет до следующего эпизода
     */
    private void startCountdown() {
        if (nextEpisodeHandler == null) return;
        
        nextEpisodeRunnable = new Runnable() {
            @Override
            public void run() {
                countdownSeconds--;
                
                if (nextEpisodeCountdown != null) {
                    nextEpisodeCountdown.setText(String.valueOf(countdownSeconds));
                }
                
                if (countdownSeconds > 0) {
                    nextEpisodeHandler.postDelayed(this, 1000);
                } else {
                    // Время вышло - запускаем следующий эпизод
                    playNextEpisodeNow();
                }
            }
        };
        
        nextEpisodeHandler.postDelayed(nextEpisodeRunnable, 1000);
    }
    
    /**
     * Отменяет автовоспроизведение следующего эпизода
     */
    private void cancelNextEpisode() {
        Log.d("VideoPlayer", "Next episode cancelled by user");
        
        // Останавливаем обратный отсчет
        if (nextEpisodeHandler != null && nextEpisodeRunnable != null) {
            nextEpisodeHandler.removeCallbacks(nextEpisodeRunnable);
        }
        
        // Скрываем оверлей
        hideNextEpisodeOverlay();
    }
    
    /**
     * Немедленно запускает следующий эпизод
     */
    private void playNextEpisodeNow() {
        Log.d("VideoPlayer", "Playing next episode now");
        
        // Останавливаем обратный отсчет
        if (nextEpisodeHandler != null && nextEpisodeRunnable != null) {
            nextEpisodeHandler.removeCallbacks(nextEpisodeRunnable);
        }
        
        // Скрываем оверлей
        hideNextEpisodeOverlay();
        
        // Переключаемся на следующий эпизод
        if (episodesManager != null) {
            episodesManager.navigateToNextEpisode();
        }
    }
    
    /**
     * Скрывает оверлей следующего эпизода
     */
    private void hideNextEpisodeOverlay() {
        if (nextEpisodeOverlay == null) return;
        
        nextEpisodeOverlay.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction(() -> {
                if (nextEpisodeOverlay != null) {
                    nextEpisodeOverlay.setVisibility(View.GONE);
                }
            })
            .start();
    }

    private void toggleEpisodesInController() {
        // Episodes are now managed by EpisodesManager through the side menu
        if (episodesManager != null) {
            episodesManager.toggleEpisodesMenu();
        }
    }

    private void hideEpisodesInController() {
        // Episodes are now managed by EpisodesManager through the side menu
        if (episodesManager != null) {
            episodesManager.hideEpisodesMenu();
        }
    }

    private void updatePlayerControlsState() {
        if (player == null) return;

        runOnUiThread(() -> {
            View controllerView = playerView.findViewById(R.id.exo_controller);
            if (controllerView != null) {
                ImageButton skipForwardButton = controllerView.findViewById(R.id.skipForwardButton);

                // Включаем/выключаем кнопки в зависимости от состояния плеера
                boolean canSeek = player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM);

                if (skipForwardButton != null) {
                    skipForwardButton.setEnabled(canSeek);
                    skipForwardButton.setAlpha(canSeek ? 1.0f : 0.5f);
                }

                // Обновляем видимость play/pause кнопок
                updatePlayPauseButtonsVisibility();
            }
        });
    }

    private void updatePlayPauseButtonsVisibility() {
        updatePlayPauseAndLoadingState(true);
    }

    private void onPlayerSelected(EpisodeResponse.PlayerData playerData) {
        Log.d("VideoPlayer", "Player selected: " + playerData.getPlayer());

        if (playerData != null && playerData.getVideoDomain() != null && !playerData.getVideoDomain().isEmpty()) {
            currentVideoDomain = playerData.getVideoDomain();
            Log.d("VideoPlayer", "Set currentVideoDomain from API: " + currentVideoDomain);
        }

        if (isNewEpisodeSelection) {
            autoPlayOnPrepare = this.autoPlay;
            isNewEpisodeSelection = false;
        } else if (player != null) {
            autoPlayOnPrepare = player.getPlayWhenReady();
        } else {
            autoPlayOnPrepare = this.autoPlay;
        }

        hideAnimeInfoPlaceholder();

        updateEpisodeHeaderQuick();

        if (playerData.getPlayer() != null && playerData.getTeam() != null) {
            apiService.savePlayerPreferences(playerData.getPlayer(), playerData.getTeam().getId());
            Log.d("VideoPlayer", "Immediately saved player preferences: player=" + playerData.getPlayer() + 
                  ", teamId=" + playerData.getTeam().getId());
        }

        if (player != null) {
            savedPlayerPosition = player.getCurrentPosition();
            Log.d("VideoPlayer", "Saved player position: " + savedPlayerPosition + "ms before switching to: " + playerData.getPlayer());
        }

        stopCurrentPlayback();

        List<String> onlineQualities = playersManager.getAvailableQualities();
        List<String> newQualities = getQualitiesWithDownloadedOption(onlineQualities);

        enableBookmarkButton();

        playersManager.setCurrentPlayerData(playerData);

        Log.d("VideoPlayer", "Player selected, ready to start playback");
        
        if (!newQualities.isEmpty()) {
            executor.execute(() -> {
                com.example.animelib.data.entity.PlayerPreferences prefs = apiService.loadPlayerPreferences();
                String savedQuality = (prefs != null) ? prefs.getPreferredQuality() : null;
                
                safeRunOnUiThread(() -> {
                    String newPreferredQuality = null;
                    
                    com.example.animelib.data.entity.DownloadedEpisodeEntity downloadedEp = getDownloadedEpisodeForActive();
                    if (downloadedEp != null) {
                        String dq = downloadedEp.getQuality();
                        if (dq == null || dq.isEmpty()) dq = "1080p";
                        else if (!dq.endsWith("p") && !dq.equalsIgnoreCase("4k")) dq += "p";
                        newPreferredQuality = "Загруженное (" + dq + ")";
                        Log.d("VideoPlayer", "Downloaded episode found, preferring downloaded quality: " + newPreferredQuality);
                    } else if (savedQuality != null && newQualities.contains(savedQuality)) {
                        newPreferredQuality = savedQuality;
                        Log.d("VideoPlayer", "Using saved quality: " + savedQuality);
                    } else if (!newQualities.isEmpty()) {
                        newPreferredQuality = newQualities.get(0);
                        Log.d("VideoPlayer", "Saved quality not found, using top quality: " + newPreferredQuality);
                    }
                    
                    preferredQuality = newPreferredQuality;
                    updateSettingsQualityTag();
                    Log.d("VideoPlayer", "Updated preferred quality to: " + newPreferredQuality + " for player: " + playerData.getPlayer());
                    
                    if (playerData.getPlayer() != null && playerData.getTeam() != null) {
                        apiService.savePlayerPreferences(playerData.getPlayer(), playerData.getTeam().getId(), preferredQuality);
                        Log.d("VideoPlayer", "Updated player preferences with quality: player=" + playerData.getPlayer() + 
                              ", teamId=" + playerData.getTeam().getId() + ", quality=" + preferredQuality);
                    }
                    
                    if (currentSettingsBottomSheet != null) {
                        currentSettingsBottomSheet.updateQualities(newQualities, preferredQuality);
                    }
                    
                    long startPosition = bookmarkTimecode > 0 ? bookmarkTimecode : savedPlayerPosition;
                    Log.d("VideoPlayer", "Starting player with position: " + startPosition + "ms (bookmark: " + bookmarkTimecode + "ms, saved: " + savedPlayerPosition + "ms)");
                    
                    if (playerData.getPlayer() != null && "animelib".equalsIgnoreCase(playerData.getPlayer())) {
                        handleAnimelibPlayer(playerData, startPosition);
                    } else if (playerData.getPlayer() != null && "kodik".equalsIgnoreCase(playerData.getPlayer())) {
                        handleKodikPlayer(playerData, startPosition);
                    }
                    
                    updateAnimeInfoHeaderFull();
                });
            });
        } else {
            long startPosition = bookmarkTimecode > 0 ? bookmarkTimecode : savedPlayerPosition;
            Log.d("VideoPlayer", "Starting player with position: " + startPosition + "ms (no qualities available)");
            
            if (playerData.getPlayer() != null && "animelib".equalsIgnoreCase(playerData.getPlayer())) {
                handleAnimelibPlayer(playerData, startPosition);
            } else if (playerData.getPlayer() != null && "kodik".equalsIgnoreCase(playerData.getPlayer())) {
                handleKodikPlayer(playerData, startPosition);
            }
            
            updateAnimeInfoHeaderFull();
        }
    }

    private void onEpisodeSelected(EpisodesListResponse.EpisodeItem episode, boolean autoPlay) {
        Log.d("VideoPlayer", "Episode selected: " + episode.getNumber() + " (ID: " + episode.getId() + "), autoPlay: " + autoPlay);

        isNewEpisodeSelection = true;
        autoPlayOnPrepare = this.autoPlay;

        // Reset bookmark timecode for new episode selection
        bookmarkTimecode = 0;
        isCurrentEpisodeMarkedViewed = false;
        Log.d("VideoPlayer", "Reset bookmark timecode and viewed status for new episode");
        
        // Reset saved player position for new episode
        savedPlayerPosition = 0;
        Log.d("VideoPlayer", "Reset saved player position for new episode");
        
        // Auto-save bookmark for previous episode if applicable before switching
        autoSaveBookmark();

        // Reset auto-bookmark flag for new episode
        autoBookmarkSaved = false;
        Log.d("VideoPlayer", "Reset auto-bookmark flag for new episode");
        
        // Reset bookmark button color for new episode
        updateBookmarkButtonColor(false);

        // Stop current playback if playing
        stopCurrentPlayback();

        // Hide episodes in controller
        hideEpisodesInController();

        // Update current episode in both managers
        episodesManager.setCurrentEpisode(episode);
        commentsManager.setCurrentEpisode(episode);
        commentsManager.resetCommentsOnEpisodeChange(true);

        // Эпизод теперь сохраняется автоматически через закладки при добавлении
        Log.d("EpisodeMemory", "Episode " + episode.getNumber() + " is now current episode");

        // Update navigation buttons visibility
        episodesManager.updateEpisodeNavigationButtonsVisibility();

        // Update episodes RecyclerView to highlight current episode
        episodesManager.updateEpisodesRecyclerView();
        
        // СРАЗУ обновляем заголовок с номером эпизода (синхронно)
        updateEpisodeHeaderQuick();

        if (isOfflineMode) {
            com.example.animelib.data.entity.DownloadedEpisodeEntity offlineEp = offlineEpisodesMap.get(episode.getNumber());
            if (offlineEp == null) {
                offlineEp = offlineEpisodesMap.get(String.valueOf(episode.getId()));
            }
            if (offlineEp != null && offlineEp.getLocalFilePath() != null) {
                playOfflineFile(offlineEp.getLocalFilePath(), offlineEp.getAnimeTitle(), offlineEp.getEpisodeName());
            } else {
                CustomToast.showWarning(this, "Файл серии не найден");
            }
            return;
        }

        // Show loading and load players for this episode
        runOnUiThread(() -> {
            showLoading("Загрузка плееров для эпизода...");
            if (menuLoadingOverlay != null) menuLoadingOverlay.setVisibility(View.VISIBLE);
        });

        // Load players for the selected episode
        playersManager.loadPlayersForEpisode(episode.getId());
    }

    private void stopCurrentPlayback() {
        if (player != null) {
            Log.d("VideoPlayer", "Stopping current playback");
            player.stop();
            player.clearMediaItems();
        }
    }

    private com.example.animelib.data.entity.DownloadedEpisodeEntity getDownloadedEpisodeForActive() {
        if (databaseManager == null) {
            if (apiService != null) {
                databaseManager = apiService.getDatabaseManager();
            }
            if (databaseManager == null) {
                databaseManager = new com.example.animelib.data.DatabaseManager(this);
            }
        }

        String animeId = currentAnimeId != null ? currentAnimeId : (getIntent() != null ? getIntent().getStringExtra("EXTRA_ANIME_ID") : null);
        if (animeId == null && getIntent() != null) {
            String localPath = getIntent().getStringExtra("EXTRA_LOCAL_FILE_PATH");
            if (localPath != null) {
                com.example.animelib.data.entity.DownloadedEpisodeEntity dep = databaseManager.findEpisodeByPath(localPath);
                if (dep != null) animeId = dep.getAnimeId();
            }
        }
        if (animeId == null) return null;

        EpisodesListResponse.EpisodeItem currentEpisode = episodesManager != null ? episodesManager.getCurrentEpisode() : null;
        String epNum = currentEpisode != null ? currentEpisode.getNumber() : (getIntent() != null ? getIntent().getStringExtra("EXTRA_EPISODE_NUMBER") : null);
        int currentEpId = currentEpisode != null ? currentEpisode.getId() : 0;
        if (epNum == null && currentEpId == 0) return null;

        EpisodeResponse.PlayerData currentPlayerData = playersManager != null ? playersManager.getCurrentPlayerData() : null;
        String team = (currentPlayerData != null && currentPlayerData.getTeam() != null) ? currentPlayerData.getTeam().getName() : null;

        com.example.animelib.data.entity.DownloadedEpisodeEntity downloaded = null;

        // 1. Try exact match by episodeId and team
        if (currentEpId != 0) {
            downloaded = databaseManager.findDownloadedEpisode(animeId, currentEpId, epNum, team);
        }

        // 1b. Fallback to exact match by epNum and team
        if (downloaded == null && team != null && !team.isEmpty() && epNum != null) {
            downloaded = databaseManager.findDownloadedEpisode(animeId, epNum, team);
        }

        // 2. If not found, check all downloaded episodes for this anime
        if (downloaded == null) {
            List<com.example.animelib.data.entity.DownloadedEpisodeEntity> downloadedEps = databaseManager.getEpisodesForAnimeSync(animeId);
            if (downloadedEps != null) {
                for (com.example.animelib.data.entity.DownloadedEpisodeEntity ep : downloadedEps) {
                    if (currentEpId != 0 && ep.getEpisodeId() == currentEpId) {
                        downloaded = ep;
                        break;
                    } else if (epNum != null && epNum.equals(ep.getEpisodeNumber())) {
                        downloaded = ep;
                        break;
                    }
                }
            }
        }

        // 3. Confirm file exists on disk
        if (downloaded != null && downloaded.getLocalFilePath() != null) {
            java.io.File file = new java.io.File(downloaded.getLocalFilePath());
            if (file.exists() && file.length() > 0) {
                return downloaded;
            }
        }

        return null;
    }

    private List<String> getQualitiesWithDownloadedOption(List<String> onlineQualities) {
        List<String> result = new ArrayList<>();
        com.example.animelib.data.entity.DownloadedEpisodeEntity downloadedEp = getDownloadedEpisodeForActive();
        if (downloadedEp != null) {
            String q = downloadedEp.getQuality();
            if (q == null || q.isEmpty()) {
                q = "1080p";
            } else if (!q.endsWith("p") && !q.equalsIgnoreCase("4k")) {
                q = q + "p";
            }
            String label = "Загруженное (" + q + ")";
            result.add(label);
        }
        if (onlineQualities != null) {
            for (String oq : onlineQualities) {
                if (!result.contains(oq)) {
                    result.add(oq);
                }
            }
        }
        return result;
    }

    private boolean isDownloadedQuality(String quality) {
        if (quality == null) return false;
        String q = quality.toLowerCase();
        return q.contains("загруженное") || q.contains("скачанное") || q.contains("локально");
    }

    private void showSettingsDialog() {
        if (!isOfflineMode && playersManager.getCurrentPlayerData() == null) {
            return;
        }

        List<String> availableQualities;
        if (isOfflineMode) {
            availableQualities = getQualitiesWithDownloadedOption(new ArrayList<>());
        } else {
            availableQualities = getQualitiesWithDownloadedOption(playersManager.getAvailableQualities());
            if (availableQualities.isEmpty()) {
                CustomToast.showWarning(this, "Качества недоступны");
                return;
            }
        }

        SettingsBottomSheet dialog = new SettingsBottomSheet(this, availableQualities, preferredQuality,
                quality -> {
                    String oldQuality = preferredQuality;
                    preferredQuality = quality;
                    updateSettingsQualityTag();
                    Log.d("VideoPlayer", "Selected quality: " + quality);

                    EpisodeResponse.PlayerData currentPlayer = playersManager.getCurrentPlayerData();
                    if (currentPlayer != null && currentPlayer.getPlayer() != null && currentPlayer.getTeam() != null) {
                        apiService.savePlayerPreferences(currentPlayer.getPlayer(), 
                                                        currentPlayer.getTeam().getId(), 
                                                        quality);
                        Log.d("VideoPlayer", "Saved quality preference: " + quality);
                    }

                    if (!quality.equals(oldQuality) && player != null) {
                        Log.d("VideoPlayer", "Restarting player with new quality");
                        restartPlayerWithNewQuality();
                    }
                },
                player != null ? player.getPlaybackParameters().speed : 1.0f,
                speed -> {
                    if (player != null) {
                        player.setPlaybackSpeed(speed);
                        Log.d("VideoPlayer", "Speed changed via dialog: " + speed);
                    }
                },
                enable4K,
                enabled -> {
                    enable4K = enabled;
                    apiService.save4KSetting(enabled);
                    playersManager.setEnable4K(enabled);
                    Log.d("VideoPlayer", "4K setting changed to: " + enabled);
                    List<String> newQualities = playersManager.getAvailableQualities();
                    Log.d("VideoPlayer", "New qualities after 4K toggle: " + newQualities);
                    if (!newQualities.isEmpty()) {
                        if (!newQualities.contains(preferredQuality)) {
                            String oldQuality = preferredQuality;
                            preferredQuality = newQualities.get(0);
                            Log.d("VideoPlayer", "Preferred quality changed from " + oldQuality + " to " + preferredQuality);
                        }
                        if (currentSettingsBottomSheet != null) {
                            Log.d("VideoPlayer", "Updating SettingsBottomSheet with new qualities");
                            currentSettingsBottomSheet.updateQualities(newQualities, preferredQuality);
                        } else {
                            Log.w("VideoPlayer", "currentSettingsBottomSheet is null, cannot update");
                        }
                    } else {
                        Log.w("VideoPlayer", "New qualities list is empty!");
                    }
                },
                enableAmbientLight,
                enabled -> {
                    enableAmbientLight = enabled;
                    apiService.saveAmbientLightSetting(enabled);
                    if (ambientLightManager != null) {
                        ambientLightManager.setEnabled(enabled);
                    }
                    Log.d("VideoPlayer", "Ambient light setting changed to: " + enabled);
                },
                autoPlay,
                enabled -> {
                    autoPlay = enabled;
                    autoPlayOnPrepare = enabled;
                    apiService.saveAutoPlaySetting(enabled);
                    Log.d("VideoPlayer", "AutoPlay enabled: " + enabled);
                },
                longSkipDuration,
                duration -> {
                    longSkipDuration = duration;
                    apiService.saveLongSkipDurationSetting(duration);
                    Log.d("VideoPlayer", "LongSkipDuration changed: " + duration);
                },
                currentTheme,
                themeMode -> {
                    currentTheme = themeMode;
                    ThemeUtils.applyThemeToActivity(VideoPlayerActivity.this, themeMode);
                    setupFullscreen();
                    checkAndUpdateOrientation();
                    apiService.saveThemeSetting(themeMode);
                    Log.d("VideoPlayer", "Theme changed: " + themeMode);
                });

        dialog.setOfflineMode(isOfflineMode);
        dialog.setVideoFilters(filterBrightness, filterContrast, filterSaturation, filterGamma, filterHue,
                (b, c, s, g, h) -> {
                    filterBrightness = b;
                    filterContrast = c;
                    filterSaturation = s;
                    filterGamma = g;
                    filterHue = h;
                    apiService.saveVideoFilters(b, c, s, g, h);
                    if (videoFiltersManager != null) {
                        videoFiltersManager.setFilters(b, c, s, g, h);
                    }
                    Log.d("VideoPlayer", "Video filters changed: b=" + b + ", c=" + c + ", s=" + s + ", g=" + g + ", h=" + h);
                });
        dialog.setSurround3DSettings(
                enableSurroundSound,
                surroundMode,
                surroundSpatialWidth,
                surroundDialogueBoost,
                surroundBassBoost,
                surroundTrebleBoost,
                (enabled, mode, spatialWidth, dialogueBoost, bassBoost, trebleBoost) -> {
                    enableSurroundSound = enabled;
                    surroundMode = mode;
                    surroundSpatialWidth = spatialWidth;
                    surroundDialogueBoost = dialogueBoost;
                    surroundBassBoost = bassBoost;
                    surroundTrebleBoost = trebleBoost;

                    apiService.saveSurroundSoundSetting(enabled);
                    apiService.saveSurround3DSettings(mode, spatialWidth, dialogueBoost, bassBoost, trebleBoost);

                    if (surroundSoundManager != null) {
                        surroundSoundManager.setEnabled(enabled);
                        surroundSoundManager.setSpatialMode(mode);
                        surroundSoundManager.setSpatialWidth(spatialWidth);
                        surroundSoundManager.setDialogueBoost(dialogueBoost);
                        surroundSoundManager.setBassBoostLevel(bassBoost);
                        surroundSoundManager.setTrebleBoostLevel(trebleBoost);
                    }
                    showSurroundSoundToast(enabled);
                    Log.d("VideoPlayer", "3D Surround sound settings changed: enabled=" + enabled + ", mode=" + mode + ", width=" + spatialWidth + ", dialogue=" + dialogueBoost + ", bass=" + bassBoost + ", treble=" + trebleBoost);
                }
        );
        dialog.setResizeMode(currentResizeMode, newMode -> setVideoResizeMode(newMode));

        List<EpisodeResponse.SubtitleData> subs = null;
        if (playersManager != null && playersManager.getCurrentPlayerData() != null) {
            subs = playersManager.getCurrentPlayerData().getSubtitles();
        }
        dialog.setSubtitleSettings(subtitlesEnabled, subtitleFormat, subs, (enabled, format) -> {
            boolean formatChanged = !Objects.equals(subtitleFormat, format);
            subtitlesEnabled = enabled;
            subtitleFormat = format;
            apiService.saveSubtitlesEnabledSetting(enabled);
            apiService.saveSubtitleFormatSetting(format);
            Log.d("VideoPlayer", "Subtitle settings changed: enabled=" + enabled + ", format=" + format);
            applySubtitlesStateToPlayer();
            if (formatChanged && enabled && player != null) {
                reloadPlayerWithSubtitles();
            }
        });
        dialog.setSubtitleStyleSettings(subtitleTextSize, subtitleTextColor, subtitleBackgroundColor, subtitleEdgeType, subtitleEdgeColor,
                (textSize, textColor, bgColor, edgeType, edgeColor) -> {
                    subtitleTextSize = textSize;
                    subtitleTextColor = textColor;
                    subtitleBackgroundColor = bgColor;
                    subtitleEdgeType = edgeType;
                    subtitleEdgeColor = edgeColor;
                    apiService.saveSubtitleStyleSettings(textSize, textColor, bgColor, edgeType, edgeColor);
                    applySubtitlesStateToPlayer();
                });

        boolean checkKodik = false;
        if (playersManager != null && playersManager.getCurrentPlayerData() != null) {
            String pType = playersManager.getCurrentPlayerData().getPlayer();
            if (pType != null && "kodik".equalsIgnoreCase(pType)) {
                checkKodik = true;
            }
        }
        final boolean isKodik = checkKodik;
        dialog.setVideoServerSettings(currentVideoDomain, isKodik, domain -> {
            if (!Objects.equals(currentVideoDomain, domain)) {
                currentVideoDomain = domain;
                Log.d("VideoPlayer", "Selected video server domain: " + domain);
                if (!isKodik && player != null) {
                    restartPlayerWithNewQuality();
                }
            }
        });

        currentSettingsBottomSheet = dialog;

        dialog.setOnShowListener(dialogInterface -> {
            applySettingsFromDialog(dialog);
        });

        dialog.show();
    }

    private Context getPlayerContext() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                Context attributionContext = createAttributionContext("default");
                if (attributionContext != null) {
                    return attributionContext;
                }
            } catch (Exception e) {
                Log.e("VideoPlayer", "Error creating attribution context", e);
            }
        }
        return this;
    }

    private void playOfflineFile(String localPath, String animeTitle, String episodeTitle) {
        isOfflineMode = true;
        java.io.File file = new java.io.File(localPath);
        if (!file.exists()) {
            CustomToast.showWarning(this, "Офлайн файл не найден: " + localPath);
            finish();
            return;
        }

        if (commentsManager != null) {
            commentsManager.setOfflineMode(true);
        }

        if (animeTitleView != null) {
            SkeletonHelper.hideSkeleton(animeTitleView, animeTitle != null ? animeTitle : "");
        }
        if (tvPortraitAnimeTitle != null) {
            SkeletonHelper.hideSkeleton(tvPortraitAnimeTitle, animeTitle != null ? animeTitle : "");
        }
        if (tvPortraitEpisodeTitle != null) {
            EpisodesListResponse.EpisodeItem curEp = episodesManager != null ? episodesManager.getCurrentEpisode() : null;
            String epNum = curEp != null && curEp.getNumber() != null ? curEp.getNumber() : (getIntent() != null ? getIntent().getStringExtra("EXTRA_EPISODE_NUMBER") : null);
            String cleanName = cleanEpisodeName(episodeTitle, epNum);
            String epTitle = (epNum != null && !epNum.isEmpty() ? (epNum + " серия") : "") + (!cleanName.isEmpty() ? (", " + cleanName) : "");
            SkeletonHelper.hideSkeleton(tvPortraitEpisodeTitle, epTitle);
        }

        Uri videoUri = Uri.fromFile(file);
        if (player == null) {
            com.example.animelib.util.SurroundRenderersFactory rf = new com.example.animelib.util.SurroundRenderersFactory(
                    getPlayerContext(), surroundSoundManager != null ? surroundSoundManager.getSurroundAudioProcessor() : null);
            player = new androidx.media3.exoplayer.ExoPlayer.Builder(getPlayerContext(), rf)
                    .setSeekBackIncrementMs(10000)
                    .setSeekForwardIncrementMs(10000)
                    .build();
            playerView.setPlayer(player);
            setVideoResizeMode(currentResizeMode);
            setupPlayerListener();
        } else {
            player.stop();
            player.clearMediaItems();
            setupPlayerListener();
        }

        if (ambientLightManager != null) {
            ambientLightManager.setPlayer(player);
        }

        if (surroundSoundManager != null) {
            surroundSoundManager.attachPlayer(player);
        }

        playerView.setUseController(true);
        updateControllerAutoHide();

        if (gesturesManager != null) {
            gesturesManager.updatePlayer(player);
        }

        com.google.android.material.button.MaterialButton skipSegmentButton = findViewById(R.id.skipSegmentButton);
        if (timecodeManager != null) {
            timecodeManager.initializeViews(player, playerView, skipSegmentButton);
        }

        MediaItem mediaItem = createMediaItemWithSubtitles(videoUri.toString());
        player.setMediaItem(mediaItem);
        if (ambientLightManager != null) {
            ambientLightManager.setPlayer(player, mediaItem, videoUri.toString());
        }
        player.prepare();
        setupSubtitlePlayerListener(player);
        applySubtitlesStateToPlayer();
        player.setPlayWhenReady(autoPlayOnPrepare);
        autoPlayOnPrepare = true;

        setupPlayerControlButtons();

        hideLoading();
        setupOfflineEpisodes(localPath);
        applyOfflineUIState();
        hideAllSkeletonsForOffline();
    }

    private void setupOfflineEpisodes(String localPath) {
        if (!isOfflineMode) return;

        executor.execute(() -> {
            com.example.animelib.data.DatabaseManager databaseManager = apiService.getDatabaseManager();
            if (databaseManager == null) return;

            String animeId = getIntent().getStringExtra("EXTRA_ANIME_ID");
            String intentEpNumber = getIntent().getStringExtra("EXTRA_EPISODE_NUMBER");

            if (animeId == null && localPath != null) {
                com.example.animelib.data.entity.DownloadedEpisodeEntity currentEp = databaseManager.findEpisodeByPath(localPath);
                if (currentEp != null) {
                    animeId = currentEp.getAnimeId();
                }
            }

            if (animeId != null) {
                currentAnimeId = animeId;
                List<com.example.animelib.data.entity.DownloadedEpisodeEntity> downloadedEps = databaseManager.getEpisodesForAnimeSync(animeId);
                if (downloadedEps != null && !downloadedEps.isEmpty()) {
                    List<EpisodesListResponse.EpisodeItem> episodeItems = new ArrayList<>();
                    EpisodesListResponse.EpisodeItem currentItem = null;

                    for (com.example.animelib.data.entity.DownloadedEpisodeEntity dep : downloadedEps) {
                        EpisodesListResponse.EpisodeItem item = new EpisodesListResponse.EpisodeItem();
                        int id = dep.getEpisodeId();
                        if (id <= 0) {
                            try {
                                id = Integer.parseInt(dep.getEpisodeNumber());
                            } catch (Exception ignored) {
                                id = Math.abs(dep.getId().hashCode());
                            }
                        }
                        item.setId(id);
                        item.setNumber(dep.getEpisodeNumber());
                        String cleanName = cleanEpisodeName(dep.getEpisodeName(), dep.getEpisodeNumber());
                        item.setName(cleanName);

                        if (dep.getEpisodeNumber() != null) {
                            offlineEpisodesMap.put(dep.getEpisodeNumber(), dep);
                        }
                        offlineEpisodesMap.put(String.valueOf(id), dep);

                        episodeItems.add(item);

                        // Priority 1: Exact match by localPath
                        if (localPath != null && dep.getLocalFilePath() != null) {
                            String normLocal = localPath.startsWith("file://") ? localPath.substring(7) : localPath;
                            String normDep = dep.getLocalFilePath().startsWith("file://") ? dep.getLocalFilePath().substring(7) : dep.getLocalFilePath();
                            java.io.File f1 = new java.io.File(normLocal);
                            java.io.File f2 = new java.io.File(normDep);
                            if (normLocal.equalsIgnoreCase(normDep) ||
                                f1.getAbsolutePath().equalsIgnoreCase(f2.getAbsolutePath()) ||
                                (f1.getName() != null && f1.getName().equalsIgnoreCase(f2.getName()))) {
                                currentItem = item;
                            }
                        }
                    }

                    // Priority 2: Match by existing episodesManager current episode
                    if (currentItem == null && episodesManager != null) {
                        EpisodesListResponse.EpisodeItem existingCur = episodesManager.getCurrentEpisode();
                        if (existingCur != null) {
                            for (EpisodesListResponse.EpisodeItem item : episodeItems) {
                                if (item.getNumber() != null && item.getNumber().equals(existingCur.getNumber())) {
                                    currentItem = item;
                                    break;
                                }
                                if (item.getId() == existingCur.getId()) {
                                    currentItem = item;
                                    break;
                                }
                            }
                        }
                    }

                    // Priority 3: Match by intent episode number (only on first launch)
                    if (currentItem == null && isFirstOfflineLaunch && intentEpNumber != null && !intentEpNumber.isEmpty()) {
                        for (EpisodesListResponse.EpisodeItem item : episodeItems) {
                            if (item.getNumber() != null && item.getNumber().equals(intentEpNumber)) {
                                currentItem = item;
                                break;
                            }
                        }
                    }

                    // Priority 4: Offline bookmark (only on first launch)
                    com.example.animelib.data.entity.OfflineBookmarkEntity offlineBookmark = databaseManager.getOfflineBookmarkSync(animeId);
                    if (currentItem == null && isFirstOfflineLaunch && offlineBookmark != null) {
                        for (EpisodesListResponse.EpisodeItem item : episodeItems) {
                            if (item.getNumber() != null && item.getNumber().equals(offlineBookmark.getEpisodeNumber())) {
                                currentItem = item;
                                break;
                            }
                            if (item.getId() == offlineBookmark.getEpisodeId()) {
                                currentItem = item;
                                break;
                            }
                        }
                    }

                    if (currentItem == null && !episodeItems.isEmpty()) {
                        currentItem = episodeItems.get(0);
                    }

                    final List<EpisodesListResponse.EpisodeItem> finalItems = episodeItems;
                    final EpisodesListResponse.EpisodeItem finalCurrent = currentItem;

                    runOnUiThread(() -> {
                        if (episodesManager != null) {
                            episodesManager.setEpisodes(finalItems);
                            if (finalCurrent != null) {
                                episodesManager.setCurrentEpisode(finalCurrent);
                            }
                            if (offlineBookmark != null) {
                                com.example.animelib.models.AnimeBookmarkResponse.BookmarkData bmData = new com.example.animelib.models.AnimeBookmarkResponse.BookmarkData();
                                bmData.setItemId(offlineBookmark.getEpisodeId());
                                bmData.setProgress(offlineBookmark.getTimecode());
                                episodesManager.updateBookmarkInAdapter(bmData);

                                boolean isCurrentBookmarked = finalCurrent != null &&
                                        (finalCurrent.getId() == offlineBookmark.getEpisodeId() ||
                                         (finalCurrent.getNumber() != null && finalCurrent.getNumber().equals(offlineBookmark.getEpisodeNumber())));

                                if (isCurrentBookmarked) {
                                    updateBookmarkButtonColor(true);
                                    if (offlineBookmark.getPositionMs() > 0 && player != null && isFirstOfflineLaunch) {
                                        player.seekTo(offlineBookmark.getPositionMs());
                                        Log.d("VideoPlayer", "Seeked player to offline bookmark position: " + offlineBookmark.getPositionMs() + "ms");
                                    }
                                } else {
                                    updateBookmarkButtonColor(false);
                                }
                            } else {
                                updateBookmarkButtonColor(false);
                            }
                            episodesManager.updateEpisodeNavigationButtonsVisibility();
                        }
                        isFirstOfflineLaunch = false;
                        if (episodesMenuButton != null) {
                            boolean isPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
                            episodesMenuButton.setEnabled(true);
                            episodesMenuButton.setAlpha(1.0f);
                            episodesMenuButton.setVisibility(isPortrait ? View.GONE : View.VISIBLE);
                        }
                        updateEpisodeHeaderQuick();
                        hideAllSkeletonsForOffline();
                    });
                }
            }
        });
    }

    private void applyOfflineUIState() {
        isOfflineMode = true;

        if (commentsManager != null) {
            commentsManager.setOfflineMode(true);
        }

        if (menuPanelContainer != null) {
            menuPanelContainer.forceClose();
            menuPanelContainer.setVisibility(View.GONE);
        }
        if (commentsPanelContainer != null) {
            commentsPanelContainer.forceClose();
            commentsPanelContainer.setVisibility(View.GONE);
        }
        if (menuOverlay != null) {
            menuOverlay.setVisibility(View.GONE);
        }
        if (animeInfoPlaceholder != null) {
            animeInfoPlaceholder.setVisibility(View.GONE);
        }
        if (slidingMenuPanel != null) {
            slidingMenuPanel.setVisibility(View.GONE);
        }

        // Сбрасываем трансформацию плеера к полноэкранному состоянию
        applyPlayerSidePanelTransform(0f);
        if (playerContainer != null) {
            playerContainer.setPivotX(0f);
            playerContainer.setPivotY(0f);
            playerContainer.setScaleX(1f);
            playerContainer.setScaleY(1f);
            playerContainer.setTranslationX(0f);
            playerContainer.setTranslationY(0f);
        }

        if (settingsButton != null) {
            settingsButton.setEnabled(true);
            settingsButton.setAlpha(1.0f);
            settingsButton.setVisibility(View.VISIBLE);
        }

        if (menuToggleButton != null) {
            menuToggleButton.setEnabled(false);
            menuToggleButton.setAlpha(0.3f);
        }
        boolean hasEpisodes = episodesManager != null && !episodesManager.getEpisodes().isEmpty();
        if (episodesMenuButton != null) {
            boolean isPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
            episodesMenuButton.setEnabled(hasEpisodes);
            episodesMenuButton.setAlpha(hasEpisodes ? 1.0f : 0.3f);
            episodesMenuButton.setVisibility(isPortrait ? View.GONE : View.VISIBLE);
        }
        if (commentsButton != null) {
            commentsButton.setEnabled(false);
            commentsButton.setAlpha(0.3f);
        }
        if (bookmarkButton != null) {
            bookmarkButton.setEnabled(true);
            bookmarkButton.setClickable(true);
            bookmarkButton.setAlpha(1.0f);
        }
        if (episodesManager != null) {
            episodesManager.updateEpisodeNavigationButtonsVisibility();
        }
        if (downloadButton != null) {
            downloadButton.setEnabled(false);
            downloadButton.setAlpha(0.3f);
            downloadButton.setVisibility(View.GONE);
        }
        if (downloadButtonTop != null) {
            downloadButtonTop.setEnabled(false);
            downloadButtonTop.setAlpha(0.3f);
            downloadButtonTop.setVisibility(View.GONE);
        }
        if (btnDownloadFromMenu != null) {
            btnDownloadFromMenu.setEnabled(false);
            btnDownloadFromMenu.setAlpha(0.3f);
            btnDownloadFromMenu.setVisibility(View.GONE);
        }
        if (portraitDownloadButton != null) {
            portraitDownloadButton.setEnabled(false);
            portraitDownloadButton.setClickable(false);
            portraitDownloadButton.setFocusable(false);
            portraitDownloadButton.setAlpha(0.35f);
        }
        if (portraitBookmarkButton != null) {
            portraitBookmarkButton.setEnabled(true);
            portraitBookmarkButton.setClickable(true);
            portraitBookmarkButton.setFocusable(true);
            portraitBookmarkButton.setAlpha(1.0f);
        }
        if (portraitVoiceoverPlayerButton != null) {
            portraitVoiceoverPlayerButton.setEnabled(false);
            portraitVoiceoverPlayerButton.setClickable(false);
            portraitVoiceoverPlayerButton.setFocusable(false);
            portraitVoiceoverPlayerButton.setAlpha(0.6f);
        }
        if (ivPortraitVoiceoverChevron != null) {
            ivPortraitVoiceoverChevron.setVisibility(View.GONE);
        }

        hideLoading();
        hideAllSkeletonsForOffline();

        updatePlayPauseAndLoadingState(false);
    }

    /**
     * Показывает окно выбора серий для скачивания (BottomSheet)
     */
    public void showDownloadBottomSheet() {
        if (isOfflineMode) {
            CustomToast.showInfo(this, "Воспроизводится скачанный файл");
            return;
        }

        List<EpisodesListResponse.EpisodeItem> epList = episodesManager != null ? episodesManager.getEpisodes() : new ArrayList<>();
        List<EpisodeResponse.PlayerData> players = playersManager != null ? playersManager.getAllPlayers() : new ArrayList<>();

        boolean isTitleLoading = (animeTitleView != null && Boolean.TRUE.equals(animeTitleView.getTag(R.id.tag_skeleton_active)));
        String titleTxt = animeTitleView != null ? animeTitleView.getText().toString() : null;
        boolean hasIntentTitle = getIntent() != null && getIntent().getStringExtra("EXTRA_ANIME_TITLE") != null && !getIntent().getStringExtra("EXTRA_ANIME_TITLE").isEmpty();
        boolean isTitleValid = (titleTxt != null && !titleTxt.isEmpty() && !titleTxt.equalsIgnoreCase("Загрузка...") && !titleTxt.equalsIgnoreCase("Аниме") && !titleTxt.contains("Маг Целитель")) || hasIntentTitle;

        if (currentAnimeId == null || players == null || players.isEmpty() || epList == null || epList.isEmpty() || isTitleLoading || !isTitleValid) {
            CustomToast.showInfo(this, "Данные еще не загружены, подождите...");
            return;
        }

        requestNotificationPermission();

        String title = null;
        if (animeTitleView != null && !Boolean.TRUE.equals(animeTitleView.getTag(R.id.tag_skeleton_active))) {
            String viewTxt = animeTitleView.getText().toString();
            if (viewTxt != null && !viewTxt.isEmpty() && !viewTxt.equalsIgnoreCase("Загрузка...") && !viewTxt.contains("Маг Целитель")) {
                title = viewTxt;
            }
        }
        if ((title == null || title.isEmpty() || title.equalsIgnoreCase("Аниме") || title.contains("Маг Целитель")) && getIntent() != null) {
            title = getIntent().getStringExtra("EXTRA_ANIME_TITLE");
        }
        if (title == null || title.isEmpty() || title.equalsIgnoreCase("Загрузка...") || title.contains("Маг Целитель")) {
            title = "Аниме";
        }

        com.example.animelib.ui.DownloadBottomSheet bottomSheet = com.example.animelib.ui.DownloadBottomSheet.newInstance(
                currentAnimeId,
                title,
                currentPosterUrl,
                epList,
                players
        );
        bottomSheet.show(getSupportFragmentManager(), "DownloadBottomSheet");
    }

    /**
     * Показывает окно прогресса скачивания серий (BottomSheet)
     */
    public void showDownloadProgressBottomSheet() {
        com.example.animelib.ui.DownloadProgressBottomSheet progressSheet = com.example.animelib.ui.DownloadProgressBottomSheet.newInstance();
        progressSheet.show(getSupportFragmentManager(), "DownloadProgressBottomSheet");
    }

    /**
     * Подписывает плеер на события фонового скачивания
     */
    private void setupDownloadListener() {
        DownloadService.setListener(new DownloadService.ProgressListener() {
            @Override
            public void onProgress(int currentTaskIndex, int totalTasks, int taskPercent, String currentTaskTitle) {
                safeRunOnUiThread(() -> showDownloadProgress(taskPercent));
            }

            @Override
            public void onFinished(int totalDownloaded, int errorCount, boolean wasCancelled) {
                safeRunOnUiThread(() -> {
                    resetDownloadUi();
                    if (wasCancelled) {
                        CustomToast.showInfo(VideoPlayerActivity.this, "Скачивание остановлено");
                    } else if (errorCount > 0) {
                        if (totalDownloaded > 0) {
                            CustomToast.showWarning(VideoPlayerActivity.this,
                                    "Скачивание завершено: " + totalDownloaded + " успешно, " + errorCount + " с ошибкой");
                        } else {
                            CustomToast.showWarning(VideoPlayerActivity.this,
                                    "Ошибка скачивания серий");
                        }
                    } else {
                        CustomToast.showSuccess(VideoPlayerActivity.this,
                                "Скачивание завершено (" + totalDownloaded + " серий)");
                    }
                });
            }

            @Override
            public void onError(String message) {
                safeRunOnUiThread(() -> {
                    resetDownloadUi();
                    CustomToast.showWarning(VideoPlayerActivity.this,
                            "Ошибка скачивания: " + message);
                });
            }
        });
    }

    /**
     * Показывает плашку прогресса скачивания
     */
    private void showDownloadProgress(int percent) {
        boolean isPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;

        isDownloading = true;
        currentDownloadPercent = percent;

        if (downloadProgressText != null) {
            downloadProgressText.setText("Скачивание " + percent + "%");
            if (!isPortrait && isControllerVisible) {
                downloadProgressText.setVisibility(View.VISIBLE);
                downloadProgressText.setAlpha(1.0f);
            } else {
                downloadProgressText.setVisibility(View.GONE);
            }
        }

        if (portraitDownloadProgressContainer != null) {
            if (isPortrait) {
                portraitDownloadProgressContainer.setVisibility(View.VISIBLE);
                if (tvPortraitDownloadPercent != null) {
                    tvPortraitDownloadPercent.setText("Скачивание: " + percent + "%");
                }
            } else {
                portraitDownloadProgressContainer.setVisibility(View.GONE);
            }
        }

        if (downloadButton != null) {
            downloadButton.setAlpha(0.85f);
        }
        if (downloadButtonTop != null) {
            downloadButtonTop.setAlpha(0.85f);
        }
        if (btnDownloadFromMenu != null) {
            btnDownloadFromMenu.setAlpha(1.0f);
            com.example.animelib.util.DownloadAnimationUtils.startDownloadAnimation(btnDownloadFromMenu);
        }
        if (portraitDownloadButton != null) {
            portraitDownloadButton.setAlpha(1.0f);
            com.example.animelib.util.DownloadAnimationUtils.startDownloadAnimation(portraitDownloadButton);
        }
        if (playersManager != null) {
            playersManager.updateDownloadButtonState(true);
        }
    }

    /**
     * Запрашивает разрешение на уведомления о прогрессе
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 4201);
        }
    }

    /**
     * Возвращает ссылку на файл выбранного качества
     */
    private String resolveDownloadUrl(String quality) {
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
            Log.w("VideoPlayer", "Invalid quality format: " + quality);
        }
        return null;
    }

    /**
     * Формирует имя файла для скачиваемой серии
     */
    private String buildDownloadFileName(String quality) {
        String title = animeTitleView != null ? animeTitleView.getText().toString() : "anime";
        EpisodesListResponse.EpisodeItem episode = episodesManager.getCurrentEpisode();
        String number = episode != null ? episode.getNumber() : "0";
        String name = title + " - " + number + " серия (" + quality + ")";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim() + ".mp4";
    }

    /**
     * Возвращает элементы скачивания в исходное состояние
     */
    private void resetDownloadUi() {
        isDownloading = false;
        currentDownloadPercent = 0;
        if (downloadProgressText != null) {
            downloadProgressText.setVisibility(View.GONE);
        }
        if (portraitDownloadProgressContainer != null) {
            portraitDownloadProgressContainer.setVisibility(View.GONE);
        }
        if (downloadButton != null) {
            com.example.animelib.util.DownloadAnimationUtils.stopDownloadAnimation(downloadButton);
        }
        if (downloadButtonTop != null) {
            com.example.animelib.util.DownloadAnimationUtils.stopDownloadAnimation(downloadButtonTop);
        }
        if (btnDownloadFromMenu != null) {
            com.example.animelib.util.DownloadAnimationUtils.stopDownloadAnimation(btnDownloadFromMenu);
        }
        if (portraitDownloadButton != null) {
            com.example.animelib.util.DownloadAnimationUtils.stopDownloadAnimation(portraitDownloadButton);
        }
        if (isOfflineMode) {
            if (downloadButton != null) {
                downloadButton.setEnabled(false);
                downloadButton.setAlpha(0.3f);
                downloadButton.setVisibility(View.GONE);
            }
            if (downloadButtonTop != null) {
                downloadButtonTop.setEnabled(false);
                downloadButtonTop.setAlpha(0.3f);
                downloadButtonTop.setVisibility(View.GONE);
            }
            if (btnDownloadFromMenu != null) {
                btnDownloadFromMenu.setEnabled(false);
                btnDownloadFromMenu.setAlpha(0.3f);
                btnDownloadFromMenu.setVisibility(View.GONE);
            }
            if (portraitDownloadButton != null) {
                portraitDownloadButton.setEnabled(false);
                portraitDownloadButton.setClickable(false);
                portraitDownloadButton.setFocusable(false);
                portraitDownloadButton.setAlpha(0.35f);
            }
        }
        if (playersManager != null) {
            playersManager.updateDownloadButtonState(false);
        }
    }

    private void restartPlayerWithNewQuality() {
        EpisodeResponse.PlayerData currentPlayerData = playersManager.getCurrentPlayerData();
        if (currentPlayerData == null) {
            return;
        }

        long currentPosition = player != null ? player.getCurrentPosition() : 0;

        autoPlayOnPrepare = player != null && player.getPlayWhenReady();

        stopCurrentPlayback();

        if (isDownloadedQuality(preferredQuality)) {
            com.example.animelib.data.entity.DownloadedEpisodeEntity downloadedEp = getDownloadedEpisodeForActive();
            if (downloadedEp != null && downloadedEp.getLocalFilePath() != null) {
                java.io.File file = new java.io.File(downloadedEp.getLocalFilePath());
                if (file.exists() && file.length() > 0) {
                    currentVideoUrl = Uri.fromFile(file).toString();
                    timecodeManager.setTimecodes(currentPlayerData);
                    initializePlayer();
                    if (currentPosition > 0 && player != null) {
                        player.seekTo(currentPosition);
                    }
                    playersManager.setCurrentPlayerData(currentPlayerData);
                    Log.d("VideoPlayer", "Restarted player with local downloaded file: " + file.getAbsolutePath());
                    return;
                }
            }
        }

        if ("animelib".equalsIgnoreCase(currentPlayerData.getPlayer())) {
            handleAnimelibPlayer(currentPlayerData, currentPosition);
        } else if ("kodik".equalsIgnoreCase(currentPlayerData.getPlayer())) {
            if (currentKodikResponse != null && currentKodikResponse.getData() != null) {
                String qualityKey = preferredQuality != null ? preferredQuality.replace("p", "") : "1080";
                if (currentKodikResponse.getData().containsKey(qualityKey) &&
                        Objects.requireNonNull(currentKodikResponse.getData().get(qualityKey)).length > 0) {
                    String newHlsUrl = Objects.requireNonNull(currentKodikResponse.getData().get(qualityKey))[0].getSrc();
                    if (newHlsUrl != null && !newHlsUrl.isEmpty()) {
                        currentVideoUrl = newHlsUrl;
                        Log.d("VideoPlayer", "Updated HLS URL for quality " + preferredQuality + ": " + currentVideoUrl);
                    }
                }
            }
            handleKodikPlayer(currentPlayerData, currentPosition);
        }
        
        // Обновляем currentPlayerData в PlayersManager для правильной подсветки после смены качества
        playersManager.setCurrentPlayerData(currentPlayerData);
    }

    private void applySettingsFromDialog(SettingsBottomSheet dialog) {
        if (player != null) {
            // Apply playback speed
            float speed = dialog.getCurrentPlaybackSpeed();
            player.setPlaybackSpeed(speed);
            Log.d("VideoPlayer", "Applied playback speed: " + speed);
        }
    }

    private void showLoading(String message) {
        safeRunOnUiThread(() -> {
            isVideoLoading = true;
            updatePlayPauseAndLoadingState(true);
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(View.VISIBLE);
                TextView textView = loadingOverlay.findViewById(R.id.loadingText);
                if (textView != null) {
                    textView.setText(message);
                }
            }
        });
    }

    private void hideLoading() {
        safeRunOnUiThread(() -> {
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(View.GONE);
            }
            isVideoLoading = false;
            updatePlayPauseAndLoadingState(true);
        });
    }

    private void showVideoErrorDialog(String title, String message, Runnable retryAction) {
        showVideoErrorDialog(title, message, retryAction, false);
    }

    private void showVideoErrorDialog(String title, String message, Runnable retryAction, boolean isVoiceoverError) {
        safeRunOnUiThread(() -> {
            if (currentErrorDialog != null && currentErrorDialog.isShowing()) {
                try {
                    currentErrorDialog.dismiss();
                } catch (Exception ignored) {}
            }

            hideLoading();

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_video_error, null);
            TextView titleTv = dialogView.findViewById(R.id.errorTitleText);
            TextView messageTv = dialogView.findViewById(R.id.errorMessageText);
            TextView detailsTv = dialogView.findViewById(R.id.errorPlayerDetailsText);
            MaterialButton retryBtn = dialogView.findViewById(R.id.retryButton);
            MaterialButton goToDownloadsBtn = dialogView.findViewById(R.id.goToDownloadsButton);
            MaterialButton exitBtn = dialogView.findViewById(R.id.exitButton);
            ImageButton closeCrossBtn = dialogView.findViewById(R.id.closeErrorCrossButton);

            if (goToDownloadsBtn != null) {
                goToDownloadsBtn.setOnClickListener(v -> {
                    if (currentErrorDialog != null) {
                        currentErrorDialog.dismiss();
                    }
                    com.example.animelib.ui.DownloadsActivity.start(VideoPlayerActivity.this);
                });
            }

            if (title != null && !title.isEmpty()) {
                titleTv.setText(title);
            } else {
                titleTv.setText("Ошибка загрузки видео");
            }

            messageTv.setText(message != null ? message : "Произошла ошибка при загрузке видео.");

            EpisodeResponse.PlayerData playerData = playersManager != null ? playersManager.getCurrentPlayerData() : null;
            if (playerData != null && !isVoiceoverError) {
                String pName = playerData.getPlayer() != null ? playerData.getPlayer() : "Неизвестный";
                String tName = playerData.getTeam() != null ? playerData.getTeam().getName() : "";
                String qName = preferredQuality != null ? preferredQuality : "";
                StringBuilder details = new StringBuilder("Плеер: ").append(pName);
                if (tName != null && !tName.isEmpty()) {
                    details.append(" (").append(tName).append(")");
                }
                if (qName != null && !qName.isEmpty()) {
                    details.append(" • ").append(qName);
                }
                detailsTv.setText(details.toString());
                detailsTv.setVisibility(View.VISIBLE);
            } else {
                detailsTv.setVisibility(View.GONE);
            }

            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
            builder.setView(dialogView);
            currentErrorDialog = builder.create();

            if (currentErrorDialog.getWindow() != null) {
                currentErrorDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            }

            retryBtn.setOnClickListener(v -> {
                if (currentErrorDialog != null) {
                    currentErrorDialog.dismiss();
                }
                if (retryAction != null) {
                    showLoading("Повторная попытка...");
                    retryAction.run();
                }
            });

            if (exitBtn != null) {
                exitBtn.setOnClickListener(v -> {
                    if (currentErrorDialog != null) {
                        currentErrorDialog.dismiss();
                    }
                    finish();
                });
            }

            if (closeCrossBtn != null) {
                closeCrossBtn.setOnClickListener(v -> {
                    if (currentErrorDialog != null) {
                        currentErrorDialog.dismiss();
                    }
                });
            }

            currentErrorDialog.show();
        });
    }

    private void initializePlayer() {
        isVideoLoading = true;
        hasRenderedFirstFrame = false;
        updatePlayPauseAndLoadingState(true);
        // Create LoadControl with larger buffer for 4K support
        LoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        50000,  // min buffer (50s для 4K)
                        120000, // max buffer (120s для 4K)
                        2500,   // buffer for playback
                        5000    // buffer for playback after rebuffer
                )
                .build();
        
        // Create TrackSelector with 4K support
        TrackSelector trackSelector = new DefaultTrackSelector(this);
        
        // Create ExoPlayer with cached data source, 5.1 Surround Sound processor, and 4K support
        com.example.animelib.util.SurroundRenderersFactory rf1 = new com.example.animelib.util.SurroundRenderersFactory(
                getPlayerContext(), surroundSoundManager != null ? surroundSoundManager.getSurroundAudioProcessor() : null);

        androidx.media3.datasource.DataSource.Factory cachedHttpFactory = com.example.animelib.util.MediaCacheManager.createCacheDataSourceFactory(this, httpDataSourceFactory);

        player = new ExoPlayer.Builder(getPlayerContext(), rf1)
                .setSeekBackIncrementMs(10000)
                .setSeekForwardIncrementMs(10000)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(cachedHttpFactory))
                .setLoadControl(loadControl)
                .setTrackSelector(trackSelector)
                .build();

        playerView.setPlayer(player);
        setVideoResizeMode(currentResizeMode);
        setupPlayerListener();
        
        // Set player for ambient light manager
        if (ambientLightManager != null) {
            ambientLightManager.setDataSourceFactory(httpDataSourceFactory);
            ambientLightManager.setPlayer(player);
        }

        if (surroundSoundManager != null) {
            surroundSoundManager.attachPlayer(player);
        }

        // Ensure controller is properly configured for play/pause buttons
        playerView.setUseController(true);
        updateControllerAutoHide();

        Log.d("PlayerInit", "ExoPlayer bound to PlayerView with controller enabled");
        
        // Update gestures manager with new player
        gesturesManager.updatePlayer(player);
        
        // Initialize timecode manager with UI components
        MaterialButton skipSegmentButton = findViewById(R.id.skipSegmentButton);
        timecodeManager.initializeViews(player, playerView, skipSegmentButton);

        // Setup all player control buttons
        setupPlayerControlButtons();

        // Create media item
        MediaItem mediaItem = createMediaItemWithSubtitles(currentVideoUrl);
        player.setMediaItem(mediaItem);
        if (ambientLightManager != null) {
            ambientLightManager.setPlayer(player, mediaItem, currentVideoUrl);
        }
        player.prepare();
        setupSubtitlePlayerListener(player);
        applySubtitlesStateToPlayer();

        // Start playback
        if (autoPlayOnPrepare) {
            player.play();
        }
        autoPlayOnPrepare = true;

        // Add listener for errors
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.e("VideoPlayer", "Playback error: " + error.getMessage(), error);
                Log.e("VideoPlayer", "Error type: " + error.errorCode + ", current quality: " + preferredQuality);
                String errorMsg = "Ошибка воспроизведения";

                // Check if this is a 4K playback error (Source error, decoder error, etc.)
                boolean is4KError = (preferredQuality != null && (preferredQuality.equals("2160p") || preferredQuality.equals("4Kp"))) &&
                        (error.getMessage().contains("Source error") || 
                         error.getMessage().contains("Decoder") ||
                         error.getMessage().contains("Video decoder error") ||
                         error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
                         error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED);
                
                if (is4KError) {
                    Log.w("VideoPlayer", "4K playback failed, attempting fallback to 1080p");
                    errorMsg = "4K не поддерживается на этом устройстве. Переключаемся на 1080p...";
                    CustomToast.showWarning(VideoPlayerActivity.this, errorMsg);
                    
                    // Try to fallback to 1080p
                    List<String> availableQualities = playersManager.getAvailableQualities();
                    if (availableQualities.contains("1080p")) {
                        preferredQuality = "1080p";
                        // Restart player with lower quality
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            long savedPosition = player.getCurrentPosition();
                            restartPlayerWithNewQuality();
                        }, 500);
                        return; // Don't show error toast
                    } else if (!availableQualities.isEmpty()) {
                        // Use any available quality
                        preferredQuality = availableQualities.get(0);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            restartPlayerWithNewQuality();
                        }, 500);
                        return;
                    }
                } else if (error.getMessage().contains("403")) {
                    errorMsg += ": доступ запрещен (403). Пробуем без токена...";
                    Log.d("VideoPlayer", "403 Forbidden - trying URL without auth token");

                    // Try loading video without auth token
                    if (currentVideoUrl.contains("video1.cdnlibs.org/.%D0%B0s")) {
                        String urlWithoutToken = currentVideoUrl.replace("https://video1.cdnlibs.org/.%D0%B0s", "https://video1.cdnlibs.org");
                        Log.d("VideoPlayer", "Retrying with URL: " + urlWithoutToken);
                        retryWithUrl(urlWithoutToken);
                        return; // Don't show error toast yet
                    }
                } else if (error.getMessage().contains("404")) {
                    errorMsg += ": видео не найдено (404).";
                } else {
                    errorMsg += ": " + error.getMessage();
                }

                showVideoErrorDialog("Ошибка воспроизведения", errorMsg, () -> {
                    EpisodeResponse.PlayerData cur = playersManager.getCurrentPlayerData();
                    if (cur != null) {
                        onPlayerSelected(cur);
                    } else if (currentVideoUrl != null) {
                        retryWithUrl(currentVideoUrl);
                    }
                });
            }
        });
    }

    private void retryWithUrl(String newUrl) {
        safeRunOnUiThread(() -> {
            Log.d("VideoPlayer", "Retrying playback with new URL: " + newUrl);
            currentVideoUrl = newUrl;

            // Check if this is an HLS URL (contains .m3u8 or hls in path)
            boolean isHls = newUrl.contains(".m3u8") || newUrl.contains(":hls:");

            if (isHls) {
                // For HLS, reinitialize the player with HLS support
                Log.d("VideoPlayer", "Retrying with HLS player");
                initializeHlsPlayer(newUrl);
            } else {
                // For regular video, just change the media item
                Log.d("VideoPlayer", "Retrying with regular player");
                MediaItem mediaItem = createMediaItemWithSubtitles(newUrl);
                if (player != null) {
                    player.setMediaItem(mediaItem);
                    player.prepare();
                    applySubtitlesStateToPlayer();
                    player.play();
                } else {
                    initializePlayer();
                }
            }

            CustomToast.showInfo(this, "Повторная попытка загрузки видео...");
        });
    }

    private void loadAnimeFromUrl(String url) {
        // Извлекаем anime ID из URL
        currentAnimeId = apiService.extractAnimeId(url);
        Log.d("VideoPlayer", "Extracted anime ID from URL: " + currentAnimeId);
        
        if (currentAnimeId == null) {
            CustomToast.showWarning(this, "Не удалось извлечь ID аниме из URL");
            finish();
            return;
        }
        
        // Load related titles
        loadRelatedTitles();
        
        // ВАЖНО: Сначала загружаем эпизоды, потом плееры!
        // Это позволит правильно определить эпизод из закладки
        Log.d("VideoPlayer", "Loading episodes first, then players will be loaded for correct episode");
        loadEpisodes(currentAnimeId);
    }

    private void showPlayerSelectionDialogWithAutoSelect(List<EpisodeResponse.PlayerData> players) {
        hideLoading();

        Log.d("VideoPlayer", "showPlayerSelectionDialogWithAutoSelect called with " + players.size() + " players");
        Log.d("VideoPlayer", "NOTE: setPlayersData already called by PlayersManager, only checking for auto-select");

        // Попытка автоматического выбора плеера на основе сохраненных предпочтений
        // ВАЖНО: setPlayersData уже вызван в PlayersManager.loadPlayersForEpisode()!
        executor.execute(() -> {
            com.example.animelib.data.entity.PlayerPreferences prefs = apiService.loadPlayerPreferences();
            
            Log.d("VideoPlayer", "Loaded preferences from DB: " + (prefs != null ? 
                  ("player=" + prefs.getPlayer() + ", teamId=" + prefs.getTeamId() + ", quality=" + prefs.getPreferredQuality()) : 
                  "null"));
            
            EpisodeResponse.PlayerData matchingPlayer = null;
            
            if (prefs != null && prefs.getPlayer() != null && prefs.getTeamId() != null) {
                Log.d("VideoPlayer", "Found saved preferences: player=" + prefs.getPlayer() + ", teamId=" + prefs.getTeamId());
                
                // Сначала ищем точное совпадение (сохраненный плеер + озвучка)
                for (EpisodeResponse.PlayerData player : players) {
                    if (player.getPlayer() != null && 
                        player.getPlayer().equals(prefs.getPlayer()) && 
                        player.getTeam() != null && 
                        player.getTeam().getId() == prefs.getTeamId()) {
                        matchingPlayer = player;
                        Log.d("VideoPlayer", "Found exact match in saved player: " + player.getPlayer() + 
                              ", team: " + player.getTeam().getName());
                        break;
                    }
                }
                
                // Если не найдено в сохраненном плеере, ищем озвучку в других плеерах
                if (matchingPlayer == null) {
                    Log.d("VideoPlayer", "Team not found in saved player, searching in other players");
                    for (EpisodeResponse.PlayerData player : players) {
                        if (player.getTeam() != null && 
                            player.getTeam().getId() == prefs.getTeamId()) {
                            matchingPlayer = player;
                            Log.d("VideoPlayer", "Found team in different player: " + player.getPlayer() + 
                                  ", team: " + player.getTeam().getName());
                            break;
                        }
                    }
                }
            } else {
                Log.d("VideoPlayer", "No saved player preferences found");
            }
            
            // Финальный результат в UI потоке
            EpisodeResponse.PlayerData finalMatchingPlayer = matchingPlayer;
            safeRunOnUiThread(() -> {
                if (finalMatchingPlayer != null) {
                    // Автоматически выбираем найденный плеер
                    Log.d("VideoPlayer", "Auto-selecting player based on preferences");
                    onPlayerSelected(finalMatchingPlayer);
                } else if (players != null && !players.isEmpty()) {
                    // Ставим первую доступную озвучку, если не сохранена последняя
                    Log.d("VideoPlayer", "No matching saved preference, auto-selecting first available player");
                    onPlayerSelected(players.get(0));
                } else {
                    Log.d("VideoPlayer", "No players available");
                }
                
                // Скрываем loading overlay в любом случае
                if (menuLoadingOverlay != null) menuLoadingOverlay.setVisibility(View.GONE);
            });
        });
    }

    private void handleAnimelibPlayer(EpisodeResponse.PlayerData playerData, long seekToPosition) {
        Log.d("AnimelibPlayer", "Handling Animelib player");
        isVideoLoading = true;
        hasRenderedFirstFrame = false;
        updatePlayPauseAndLoadingState(true);

        if (isDownloadedQuality(preferredQuality)) {
            com.example.animelib.data.entity.DownloadedEpisodeEntity downloadedEp = getDownloadedEpisodeForActive();
            if (downloadedEp != null && downloadedEp.getLocalFilePath() != null) {
                java.io.File file = new java.io.File(downloadedEp.getLocalFilePath());
                if (file.exists() && file.length() > 0) {
                    currentVideoUrl = Uri.fromFile(file).toString();
                    timecodeManager.setTimecodes(playerData);
                    initializePlayer();
                    if (seekToPosition > 0 && player != null) {
                        player.seekTo(seekToPosition);
                    }
                    Log.d("AnimelibPlayer", "Playing downloaded local file: " + file.getAbsolutePath());
                    return;
                }
            }
        }

        if (playerData.getVideo() != null && playerData.getVideo().getQuality() != null && !playerData.getVideo().getQuality().isEmpty()) {
            // Select quality based on preference or best available
            EpisodeResponse.QualityData selectedQuality = null;
            String preferredQualityValue = preferredQuality != null ? preferredQuality.replace("p", "") : null;

            if (preferredQualityValue != null) {
                try {
                    int preferredQualityInt = Integer.parseInt(preferredQualityValue);
                    // Find exact quality match (include 4K if enabled)
                    for (EpisodeResponse.QualityData quality : playerData.getVideo().getQuality()) {
                        if (quality.getQuality() == preferredQualityInt) {
                            // Include 4K only if enabled
                            if (preferredQualityInt == 2160 && !enable4K) {
                                continue;
                            }
                            selectedQuality = quality;
                            Log.d("AnimelibPlayer", "Using preferred quality: " + preferredQualityInt + "p");
                            break;
                        }
                    }
                } catch (NumberFormatException e) {
                    Log.w("AnimelibPlayer", "Invalid preferred quality format: " + preferredQuality);
                }
            }

            // If no preferred quality found or no preference set, select best quality
            if (selectedQuality == null) {
                selectedQuality = null;
                Log.d("AnimelibPlayer", "Available qualities:");
                for (EpisodeResponse.QualityData quality : playerData.getVideo().getQuality()) {
                    String q = String.valueOf(quality.getQuality());
                    Log.d("AnimelibPlayer", "  - " + quality.getQuality() + "p: " + quality.getHref());
                    // Include 4K only if enabled
                    if (enable4K || !"2160".equals(q)) {
                        if (selectedQuality == null || quality.getQuality() > selectedQuality.getQuality()) {
                            selectedQuality = quality;
                        }
                    }
                }
                // Set preferred quality to the best available
                if (preferredQuality == null && selectedQuality != null) {
                    String quality = String.valueOf(selectedQuality.getQuality());
                    preferredQuality = quality + "p";
                }
            }

            if (selectedQuality == null) {
                Log.e("AnimelibPlayer", "No suitable quality found");
                showVideoErrorDialog("Ошибка плеера AnimeLib", "Нет подходящего качества видео для этой озвучки.", () -> {
                    onPlayerSelected(playerData);
                });
                return;
            }

            String videoUrl = selectedQuality.getHref();
            Log.d("AnimelibPlayer", "Selected quality: " + selectedQuality.getQuality() + "p, URL: " + videoUrl);

            videoUrl = VideoUrlHelper.toAbsoluteVideoUrl(videoUrl, currentVideoDomain);

            Log.d("AnimelibPlayer", "Final video URL: " + videoUrl);
            currentVideoUrl = videoUrl;
            
            // Set timecodes from player data
            timecodeManager.setTimecodes(playerData);
            
            initializePlayer();
            if (seekToPosition > 0) {
                player.seekTo(seekToPosition);
            }
        } else {
            showVideoErrorDialog("Видео недоступно", "У выбранной озвучки AnimeLib отсутствуют ссылки на видео.", () -> {
                onPlayerSelected(playerData);
            });
        }
    }

    private void handleKodikPlayer(EpisodeResponse.PlayerData playerData, long seekToPosition) {
        Log.d("KodikPlayer", "Handling Kodik player");

        if (isDownloadedQuality(preferredQuality)) {
            com.example.animelib.data.entity.DownloadedEpisodeEntity downloadedEp = getDownloadedEpisodeForActive();
            if (downloadedEp != null && downloadedEp.getLocalFilePath() != null) {
                java.io.File file = new java.io.File(downloadedEp.getLocalFilePath());
                if (file.exists() && file.length() > 0) {
                    currentVideoUrl = Uri.fromFile(file).toString();
                    timecodeManager.setTimecodes(playerData);
                    initializePlayer();
                    if (seekToPosition > 0 && player != null) {
                        player.seekTo(seekToPosition);
                    }
                    Log.d("KodikPlayer", "Playing downloaded local file: " + file.getAbsolutePath());
                    return;
                }
            }
        }

        // Set timecodes from player data
        timecodeManager.setTimecodes(playerData);
        
        if (playerData.getSrc() != null && !playerData.getSrc().isEmpty()) {
            String kodikSrc = playerData.getSrc();
            if (!kodikSrc.startsWith("http")) {
                kodikSrc = "https:" + kodikSrc;
            }
            Log.d("KodikPlayer", "Kodik src: " + kodikSrc);
            fetchKodikVideoLinks(kodikSrc, seekToPosition);
        } else {
            Log.w("KodikPlayer", "No src found in Kodik player data");
            showVideoErrorDialog("Ошибка Kodik", "Ссылка на плеер Kodik отсутствует. Попробуйте выбрать другую озвучку.", () -> {
                handleKodikPlayer(playerData, seekToPosition);
            });
            if (player != null) {
                player.stop();
                player.clearMediaItems();
            }
            if (menuLoadingOverlay != null) menuLoadingOverlay.setVisibility(View.GONE);
        }
    }

    private void fetchKodikVideoLinks(String kodikSrc, long seekToPosition) {
        Log.d("KodikAPI", "Fetching HLS links for Kodik src: " + kodikSrc);
        safeRunOnUiThread(() -> showLoading("Получение HLS ссылок..."));

        apiService.fetchKodikVideoLinks(kodikSrc, new ApiService.KodikVideoCallback() {
            @Override
            public void onKodikVideoReceived(KodikResponse response) {
                safeRunOnUiThread(() -> {
                    hideLoading();
                    startHlsPlayer(response, seekToPosition);
                });
            }

            @Override
            public void onError(String error) {
                safeRunOnUiThread(() -> {
                    hideLoading();
                    showVideoErrorDialog("Ошибка загрузки Kodik", "Не удалось загрузить HLS видеоссылки Kodik:\n" + error, () -> {
                        fetchKodikVideoLinks(kodikSrc, seekToPosition);
                    });
                    // Clean player state
                    if (player != null) {
                        player.stop();
                        player.clearMediaItems();
                    }
                    // Очистить возможные UI меню/загрузка
                    if (menuLoadingOverlay != null) menuLoadingOverlay.setVisibility(View.GONE);
                });
            }
        });
    }

    private void loadEpisodes(String animeId) {
        Log.d("EpisodesAPI", "Loading episodes for anime_id: " + animeId);
        
        // Получаем media_slug из URL аниме для загрузки закладки
        String animeUrl = getIntent().getStringExtra("anime_url");
        String mediaSlug = null;
        if (animeUrl != null && !animeUrl.isEmpty()) {
            mediaSlug = ApiService.extractMediaSlugFromUrl(animeUrl);
        }
        
        // Use EpisodesManager to load episodes with bookmark
        if (mediaSlug != null) {
            episodesManager.loadEpisodesWithBookmark(animeId, mediaSlug);
        } else {
            episodesManager.loadEpisodes(animeId);
        }
    }

    /**
     * Загружает первый эпизод когда нет закладки
     */
    private void loadFirstEpisode() {
        isNewEpisodeSelection = true;
        autoPlayOnPrepare = this.autoPlay;
        // Reset bookmark timecode when loading first episode
        bookmarkTimecode = 0;
        Log.d("VideoPlayer", "Reset bookmark timecode for first episode");
        
        // Reset saved player position when loading first episode
        savedPlayerPosition = 0;
        Log.d("VideoPlayer", "Reset saved player position for first episode");
        
        // Reset auto-bookmark flag when loading first episode
        autoBookmarkSaved = false;
        Log.d("VideoPlayer", "Reset auto-bookmark flag for first episode");
        
        // Reset bookmark button color for first episode
        updateBookmarkButtonColor(false);
        
        // Получаем первый эпизод из списка
        List<EpisodesListResponse.EpisodeItem> episodes = episodesManager.getEpisodes();
        if (episodes != null && !episodes.isEmpty()) {
            EpisodesListResponse.EpisodeItem firstEpisode = episodes.get(0);
            Log.d("VideoPlayer", "Loading first episode: " + firstEpisode.getNumber());
            
            episodesManager.setCurrentEpisode(firstEpisode);
            commentsManager.setCurrentEpisode(firstEpisode);
            
            // СРАЗУ обновляем заголовок с номером эпизода
            updateEpisodeHeaderQuick();
            
            playersManager.loadPlayersForEpisode(firstEpisode.getId());
        } else {
            Log.d("VideoPlayer", "No episodes available, initializing menu without auto play");
            initializeMenuWithoutAutoPlay();
        }
    }
    
    private void fallbackToUrlDetection() {
        // Reset bookmark timecode when falling back to URL detection
        bookmarkTimecode = 0;
        Log.d("VideoPlayer", "Reset bookmark timecode for URL detection fallback");
        
        // Reset saved player position when falling back to URL detection
        savedPlayerPosition = 0;
        Log.d("VideoPlayer", "Reset saved player position for URL detection fallback");
        
        // Reset auto-bookmark flag when falling back to URL detection
        autoBookmarkSaved = false;
        Log.d("VideoPlayer", "Reset auto-bookmark flag for URL detection fallback");
        
        // Reset bookmark button color for URL detection fallback
        updateBookmarkButtonColor(false);
        
        String animeUrl = getIntent().getStringExtra("anime_url");
        episodesManager.findAndSetCurrentEpisodeFromUrl(animeUrl);
        EpisodesListResponse.EpisodeItem currentEpisode = episodesManager.getCurrentEpisode();
        if (currentEpisode != null) {
            commentsManager.setCurrentEpisode(currentEpisode);
            
            // СРАЗУ обновляем заголовок с номером эпизода
            updateEpisodeHeaderQuick();
            
            playersManager.loadPlayersForEpisode(currentEpisode.getId());
        } else {
            // Если не найден эпизод по URL, загружаем первый
            loadFirstEpisode();
        }
    }

    /**
     * Включает кнопку закладки когда плеер готов
     */
    private void enableBookmarkButton() {
        if (bookmarkButton != null) {
            bookmarkButton.setEnabled(true);
            bookmarkButton.setClickable(true);
            bookmarkButton.setAlpha(1.0f);
            Log.d("VideoPlayer", "Bookmark button enabled");
        }
        if (portraitBookmarkButton != null) {
            portraitBookmarkButton.setEnabled(true);
            portraitBookmarkButton.setClickable(true);
            portraitBookmarkButton.setFocusable(true);
            portraitBookmarkButton.setAlpha(1.0f);
        }
    }
    
    private void saveLatestViewOnExit() {
        try {
            EpisodesListResponse.EpisodeItem currentEpisode = episodesManager != null ? episodesManager.getCurrentEpisode() : null;
            if (currentEpisode == null) return;

            long currentPosition = player != null ? player.getCurrentPosition() : 0;
            long duration = player != null && player.getDuration() > 0 ? player.getDuration() : 0;
            if (currentPosition < 1000) return;

            EpisodeResponse.PlayerData currentPlayer = playersManager != null ? playersManager.getCurrentPlayerData() : null;

            com.google.gson.JsonObject viewObj = new com.google.gson.JsonObject();

            // 1. media
            com.google.gson.JsonObject mediaObj = new com.google.gson.JsonObject();
            if (currentAnimeInfo != null && currentAnimeInfo.getData() != null) {
                AnimeInfoResponse.Data data = currentAnimeInfo.getData();
                mediaObj.addProperty("id", data.getId());
                mediaObj.addProperty("name", data.getName() != null ? data.getName() : (data.getRus_name() != null ? data.getRus_name() : "Anime"));
                mediaObj.addProperty("rus_name", data.getRus_name() != null ? data.getRus_name() : "");
                mediaObj.addProperty("eng_name", data.getEng_name() != null ? data.getEng_name() : "");

                String slugUrl = data.getSlug_url() != null ? data.getSlug_url() : "";
                mediaObj.addProperty("slug_url", slugUrl);

                String slug = slugUrl;
                if (slugUrl.contains("--")) {
                    String[] parts = slugUrl.split("--");
                    if (parts.length > 1) slug = parts[1];
                }
                mediaObj.addProperty("slug", slug);

                com.google.gson.JsonObject coverObj = new com.google.gson.JsonObject();
                if (data.getCover() != null) {
                    coverObj.addProperty("filename", data.getCover().getFilename() != null ? data.getCover().getFilename() : "");
                    coverObj.addProperty("thumbnail", data.getCover().getThumbnail() != null ? data.getCover().getThumbnail() : "");
                    coverObj.addProperty("default", data.getCover().getDefaultUrl() != null ? data.getCover().getDefaultUrl() : "");
                    coverObj.addProperty("md", data.getCover().getMd() != null ? data.getCover().getMd() : "");
                }
                mediaObj.add("cover", coverObj);
                mediaObj.addProperty("site", 5);
                mediaObj.addProperty("model", "anime");
            } else {
                int animeIdInt = 0;
                if (currentAnimeId != null) {
                    try { animeIdInt = Integer.parseInt(currentAnimeId); } catch (Exception ignored) {}
                }
                String titleStr = getIntent() != null && getIntent().getStringExtra("EXTRA_ANIME_TITLE") != null ?
                        getIntent().getStringExtra("EXTRA_ANIME_TITLE") : "Anime";
                String animeUrlStr = getIntent() != null ? getIntent().getStringExtra("anime_url") : null;
                String slugStr = animeUrlStr != null ? ApiService.extractMediaSlugFromUrl(animeUrlStr) : "anime";

                mediaObj.addProperty("id", animeIdInt);
                mediaObj.addProperty("name", titleStr);
                mediaObj.addProperty("rus_name", titleStr);
                mediaObj.addProperty("eng_name", titleStr);
                mediaObj.addProperty("slug", slugStr != null ? slugStr : "anime");
                mediaObj.addProperty("slug_url", slugStr != null ? slugStr : "anime");
                com.google.gson.JsonObject coverObj = new com.google.gson.JsonObject();
                mediaObj.add("cover", coverObj);
                mediaObj.addProperty("site", 5);
                mediaObj.addProperty("model", "anime");
            }
            viewObj.add("media", mediaObj);

            // 2. item
            com.google.gson.JsonObject itemObj = new com.google.gson.JsonObject();
            itemObj.addProperty("id", currentEpisode.getId());
            itemObj.addProperty("number", currentEpisode.getNumber() != null ? currentEpisode.getNumber() : "1");
            viewObj.add("item", itemObj);

            // 3. progress
            com.google.gson.JsonObject progressObj = new com.google.gson.JsonObject();
            progressObj.addProperty("current", ApiService.formatTimecode(currentPosition));
            progressObj.addProperty("total", ApiService.formatTimecode(duration));
            double percent = duration > 0 ? (double) Math.round((currentPosition * 100.0 / duration) * 100.0) / 100.0 : 0.0;
            progressObj.addProperty("percent", percent);
            viewObj.add("progress", progressObj);

            // 4. meta
            com.google.gson.JsonObject metaObj = new com.google.gson.JsonObject();
            int teamId = (currentPlayer != null && currentPlayer.getTeam() != null) ? currentPlayer.getTeam().getId() : 0;
            int transType = (currentPlayer != null && currentPlayer.getTranslationType() != null) ? currentPlayer.getTranslationType().getId() : 1;
            String playerStr = (currentPlayer != null && currentPlayer.getPlayer() != null) ? currentPlayer.getPlayer() : "Animelib";

            metaObj.addProperty("team", teamId);
            metaObj.addProperty("translation_type", transType);
            metaObj.addProperty("player", playerStr);
            metaObj.addProperty("episode", currentEpisode.getId());
            viewObj.add("meta", metaObj);

            com.example.animelib.util.LatestViewsManager.saveLatestView(getApplicationContext(), viewObj);
            Log.d("VideoPlayer", "Saved latest-view on exit for media: " + mediaObj.get("name").getAsString());

        } catch (Exception e) {
            Log.e("VideoPlayer", "Error saving latest view on exit", e);
        }
    }

    /**
     * Автоматически сохраняет закладку с текущим таймкодом
     */
    private void autoSaveBookmark() {
        Log.d("VideoPlayer", "Auto-saving bookmark on exit");
        saveLatestViewOnExit();
        
        EpisodesListResponse.EpisodeItem currentEpisode = episodesManager != null ? episodesManager.getCurrentEpisode() : null;
        if (currentEpisode == null) {
            Log.d("VideoPlayer", "Cannot auto-save bookmark - episode not ready");
            return;
        }
        
        long currentPosition = player != null ? player.getCurrentPosition() : 0;
        if (currentPosition < 1000) {
            Log.d("VideoPlayer", "Position too small for auto-save: " + currentPosition + "ms");
            return;
        }

        if (isOfflineMode) {
            String animeId = currentAnimeId != null ? currentAnimeId : (getIntent() != null ? getIntent().getStringExtra("EXTRA_ANIME_ID") : null);
            if (animeId == null && getIntent() != null && getIntent().getStringExtra("EXTRA_LOCAL_FILE_PATH") != null && apiService != null && apiService.getDatabaseManager() != null) {
                com.example.animelib.data.entity.DownloadedEpisodeEntity dep =
                        apiService.getDatabaseManager().findEpisodeByPath(getIntent().getStringExtra("EXTRA_LOCAL_FILE_PATH"));
                if (dep != null) animeId = dep.getAnimeId();
            }

            if (animeId != null && apiService != null && apiService.getDatabaseManager() != null) {
                String timecode = ApiService.formatTimecode(currentPosition);
                apiService.getDatabaseManager().saveOfflineBookmark(animeId, currentEpisode.getId(), currentEpisode.getNumber(), timecode, currentPosition);
                autoBookmarkSaved = true;
                Log.d("VideoPlayer", "Offline bookmark saved successfully for anime " + animeId + ", episode: " + currentEpisode.getNumber() + " at " + timecode);
            }
            return;
        }

        if (apiService == null || !apiService.isAuthorized()) {
            Log.d("VideoPlayer", "Cannot auto-save bookmark - user not authorized");
            return;
        }

        EpisodeResponse.PlayerData currentPlayer = playersManager != null ? playersManager.getCurrentPlayerData() : null;
        if (currentPlayer == null) {
            Log.d("VideoPlayer", "Cannot auto-save bookmark - player not ready");
            return;
        }

        String animeUrl = getIntent().getStringExtra("anime_url");
        String mediaSlug = null;
        if (animeUrl != null && !animeUrl.isEmpty()) {
            mediaSlug = ApiService.extractMediaSlugFromUrl(animeUrl);
        }
        
        if (mediaSlug == null) {
            Log.d("VideoPlayer", "Cannot auto-save bookmark - media slug not available");
            return;
        }

        // Используем BookmarkManager для добавления закладки (без UI обновлений)
        episodesManager.getBookmarkManager().addBookmark(
            mediaSlug,
            currentPlayer,
            currentEpisode,
            currentPosition,
            new BookmarkManager.BookmarkAddCallback() {
                @Override
                public void onBookmarkAdded(int episodeId) {
                    Log.d("VideoPlayer", "Auto-bookmark saved successfully for episode: " + episodeId);
                    autoBookmarkSaved = true;
                }
                
                @Override
                public void onBookmarkError(String error) {
                    Log.e("VideoPlayer", "Failed to auto-save bookmark: " + error);
                }
            },
            false
        );
    }
    
    private static class ColorAnimHolder {
        int color;
        android.animation.ValueAnimator animator;
    }

    private void animateImageColorFilter(android.widget.ImageView imageView, int targetColor) {
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
        android.animation.ValueAnimator anim = android.animation.ValueAnimator.ofObject(new android.animation.ArgbEvaluator(), holder.color, targetColor);
        anim.setDuration(220);
        anim.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            finalHolder.color = val;
            imageView.setColorFilter(val);
        });
        holder.animator = anim;
        anim.start();
    }

    /**
     * Обновляет цвет кнопки закладки с плавной анимацией перехода
     * @param isBookmarked true если закладка добавлена, false если нет
     */
    private void updateBookmarkButtonColor(boolean isBookmarked) {
        runOnUiThread(() -> {
            if (bookmarkButton != null) {
                bookmarkButton.setEnabled(true);
                bookmarkButton.setClickable(true);
                bookmarkButton.setAlpha(1.0f);
                int targetColor = isBookmarked ? getResources().getColor(R.color.bookmark_color) : getResources().getColor(R.color.white_color);
                animateImageColorFilter(bookmarkButton, targetColor);
            }
            if (portraitBookmarkButton != null) {
                portraitBookmarkButton.setEnabled(true);
                portraitBookmarkButton.setClickable(true);
                portraitBookmarkButton.setFocusable(true);
                portraitBookmarkButton.setAlpha(1.0f);
                int targetColor = isBookmarked ? getResources().getColor(R.color.bookmark_color) : getResources().getColor(R.color.primary_text_color);
                animateImageColorFilter(portraitBookmarkButton, targetColor);
            }
        });
    }
    
    /**
     * Обновляет список эпизодов после добавления закладки
     */
    private void updateEpisodesListAfterBookmark() {
        safeRunOnUiThread(() -> {
            if (isOfflineMode) {
                String animeId = currentAnimeId != null ? currentAnimeId : (getIntent() != null ? getIntent().getStringExtra("EXTRA_ANIME_ID") : null);
                if (animeId != null && apiService != null && apiService.getDatabaseManager() != null) {
                    com.example.animelib.data.entity.OfflineBookmarkEntity bm = apiService.getDatabaseManager().getOfflineBookmarkSync(animeId);
                    if (bm != null) {
                        com.example.animelib.models.AnimeBookmarkResponse.BookmarkData bookmarkData = new com.example.animelib.models.AnimeBookmarkResponse.BookmarkData();
                        bookmarkData.setItemId(bm.getEpisodeId());
                        bookmarkData.setProgress(bm.getTimecode());
                        episodesManager.updateBookmarkInAdapter(bookmarkData);
                    }
                }
                return;
            }

            // Получаем media_slug для обновления закладки
            String animeUrl = getIntent().getStringExtra("anime_url");
            String mediaSlug = null;
            if (animeUrl != null && !animeUrl.isEmpty()) {
                mediaSlug = ApiService.extractMediaSlugFromUrl(animeUrl);
            }
            
            if (mediaSlug != null) {
                // Обновляем закладку в EpisodesManager
                episodesManager.getBookmarkManager().fetchAnimeBookmark(mediaSlug, 
                    new BookmarkManager.AnimeBookmarkCallback() {
                        @Override
                        public void onBookmarkReceived(com.example.animelib.models.AnimeBookmarkResponse response) {
                            safeRunOnUiThread(() -> {
                                if (response != null && response.getData() != null) {
                                    // Обновляем закладку в адаптере
                                    episodesManager.updateBookmarkInAdapter(response.getData());
                                    Log.d("VideoPlayer", "Episodes list updated with new bookmark");
                                }
                            });
                        }
                        
                        @Override
                        public void onError(String error) {
                            Log.e("VideoPlayer", "Failed to update bookmark in episodes list: " + error);
                        }
                    });
            }
        });
    }

    private void initializeMenuWithoutAutoPlay() {
        // Players are now managed by PlayersManager
        episodesManager.updateEpisodeNavigationButtonsVisibility();
        episodesManager.updateEpisodesRecyclerView(); // Call this last to ensure currentEpisode is set

        // Загружаем плееры для текущего эпизода
        EpisodesListResponse.EpisodeItem currentEpisode = episodesManager.getCurrentEpisode();
        if (currentEpisode != null) {
            Log.d("VideoPlayer", "Loading players for episode in initializeMenuWithoutAutoPlay: " + currentEpisode.getNumber());
            playersManager.loadPlayersForEpisode(currentEpisode.getId());
        } else {
            Log.d("VideoPlayer", "No current episode available for loading players");
        }

        // PlayersManager handles auto-selection and menu display
        // Ensure loading overlay is hidden once players are available
        if (menuLoadingOverlay != null) menuLoadingOverlay.setVisibility(View.GONE);
        if (menuLoadingIndicator != null) menuLoadingIndicator.setVisibility(View.GONE);
    }

    /**
     * Быстрое обновление заголовка с номером эпизода (синхронно, без API)
     */
    private void updateEpisodeHeaderQuick() {
        safeRunOnUiThread(() -> {
            if (isOfflineMode) {
                hideAllSkeletonsForOffline();
                return;
            }

            EpisodeResponse.PlayerData currentPlayerData = playersManager != null ? playersManager.getCurrentPlayerData() : null;
            EpisodesListResponse.EpisodeItem currentEpisode = episodesManager != null ? episodesManager.getCurrentEpisode() : null;
            
            String tm = (currentPlayerData != null && currentPlayerData.getTeam() != null)
                    ? currentPlayerData.getTeam().getName() : null;
            String ep = (currentEpisode != null) ? currentEpisode.getNumber() : null;
            String rawEm = (currentEpisode != null && currentEpisode.getName() != null && !Objects.equals(currentEpisode.getName(), ""))
                    ? currentEpisode.getName() : null;
            String em = cleanEpisodeName(rawEm, ep);

            if (currentTeamName != null) {
                if (tm != null && !tm.isEmpty()) {
                    SkeletonHelper.hideSkeleton(currentTeamName, tm);
                } else if (currentPlayerData == null) {
                    SkeletonHelper.showSkeleton(currentTeamName, 80);
                } else {
                    SkeletonHelper.hideSkeleton(currentTeamName, "");
                }
            }

            if (currentEpisodeNumberView != null) {
                if (ep != null && !ep.isEmpty()) {
                    SkeletonHelper.hideSkeleton(currentEpisodeNumberView, ep + " серия");
                    Log.d("VideoPlayer", "Quick header update: episode " + ep);
                } else if (currentEpisode == null) {
                    SkeletonHelper.showSkeleton(currentEpisodeNumberView, 65);
                } else {
                    SkeletonHelper.hideSkeleton(currentEpisodeNumberView, "");
                }
            }

            if (currentEpisodeName != null) {
                if (em != null && !em.isEmpty()) {
                    SkeletonHelper.hideSkeleton(currentEpisodeName, ", " + em);
                } else {
                    SkeletonHelper.hideSkeleton(currentEpisodeName, "");
                }
            }

            updatePortraitVoiceoverPlayerUI();
            updatePortraitHeaderTitlesUI();
        });
    }
    
    /**
     * Полное обновление заголовка с названием аниме (асинхронно с API)
     */
    private void updateAnimeInfoHeaderFull() {
        // Сначала быстро обновляем эпизод
        updateEpisodeHeaderQuick();
        
        // Затем асинхронно загружаем название аниме
        if (animeTitleView == null) return;
        String slugOrId = apiService.extractAnimeSlug(animeUrl);
        if (slugOrId == null) return;

        apiService.fetchAnimeInfo(slugOrId, new ApiService.AnimeInfoCallback() {
            @Override
            public void onAnimeInfoReceived(AnimeInfoResponse response) {
                safeRunOnUiThread(() -> {
                    if (response != null && response.getData() != null) {
                        String rus = response.getData().getRus_name();
                        SkeletonHelper.hideSkeleton(animeTitleView, rus != null ? rus : "");
                        updatePortraitHeaderTitlesUI();
                        Log.d("VideoPlayer", "Full header update: anime title set");
                    }
                });
            }

            @Override
            public void onError(String error) {
                safeRunOnUiThread(() -> {
                    SkeletonHelper.hideSkeleton(animeTitleView, "Аниме");
                });
                Log.w("VideoPlayer", "Failed to load anime title: " + error);
            }
        });
    }
    
    /**
     * Старый метод для обратной совместимости
     */
    private void updateAnimeInfoHeader() {
        updateAnimeInfoHeaderFull();
    }

    private void startHlsPlayer(KodikResponse kodikResponse, long seekToPosition) {
        // Save Kodik response for quality selection
        currentKodikResponse = kodikResponse;

        // Select quality based on preference or best available
        String hlsUrl = null;
        String preferredQualityKey = preferredQuality != null ? preferredQuality.replace("p", "") : null;

        if (preferredQualityKey != null && kodikResponse.getData().containsKey(preferredQualityKey) &&
                kodikResponse.getData().get(preferredQualityKey).length > 0) {
            hlsUrl = kodikResponse.getData().get(preferredQualityKey)[0].getSrc();
            Log.d("KodikPlayer", "Using preferred quality: " + preferredQualityKey + "p");
        } else {
            // Fallback to best quality available (prefer 720p, then 480p, then 360p)
            if (kodikResponse.getData().containsKey("720") && kodikResponse.getData().get("720").length > 0) {
                hlsUrl = kodikResponse.getData().get("720")[0].getSrc();
                if (preferredQuality == null) preferredQuality = "720p";
            } else if (kodikResponse.getData().containsKey("480") && kodikResponse.getData().get("480").length > 0) {
                hlsUrl = kodikResponse.getData().get("480")[0].getSrc();
                if (preferredQuality == null) preferredQuality = "480p";
            } else if (kodikResponse.getData().containsKey("360") && kodikResponse.getData().get("360").length > 0) {
                hlsUrl = kodikResponse.getData().get("360")[0].getSrc();
                if (preferredQuality == null) preferredQuality = "360p";
            }
        }

        if (hlsUrl != null) {
            // Ensure URL is absolute
            if (!hlsUrl.startsWith("http")) {
                hlsUrl = "https:" + hlsUrl;
            }

            Log.d("HlsPlayer", "Starting HLS playback with URL: " + hlsUrl);
            currentVideoUrl = hlsUrl;
            initializeHlsPlayer(hlsUrl);
            if (seekToPosition > 0) {
                player.seekTo(seekToPosition);
            }
        } else {
            showVideoErrorDialog("HLS видео недоступно", "HLS ссылка для выбранного качества Kodik не найдена.", () -> {
                handleKodikPlayer(playersManager.getCurrentPlayerData(), seekToPosition);
            });
        }
    }

    private void initializeHlsPlayer(String hlsUrl) {
        isVideoLoading = true;
        hasRenderedFirstFrame = false;
        updatePlayPauseAndLoadingState(true);
        // Create HLS media source with OkHttp data source
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    boolean kodikHost = original.url().host().contains("kodik");
                    String referer = kodikHost ? "https://kodik.info/" : "https://v3.animelib.org/";
                    String origin = kodikHost ? "https://kodik.info" : "https://v3.animelib.org";

                    Request.Builder requestBuilder = original.newBuilder()
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

                    Request request = requestBuilder.build();
                    return chain.proceed(request);
                })
                .build();

        OkHttpDataSource.Factory okHttpDataSourceFactory = new OkHttpDataSource.Factory(okHttpClient);

        MediaItem mediaItem = createMediaItemWithSubtitles(hlsUrl);

        // Create LoadControl with larger buffer for 4K support
        LoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        50000,  // min buffer (50s для 4K)
                        120000, // max buffer (120s для 4K)
                        2500,   // buffer for playback
                        5000    // buffer for playback after rebuffer
                )
                .build();
        
        // Create TrackSelector with 4K support
        TrackSelector trackSelector = new DefaultTrackSelector(this);
        
        // Create ExoPlayer with 4K support and 5.1 Surround Sound
        com.example.animelib.util.SurroundRenderersFactory rf2 = new com.example.animelib.util.SurroundRenderersFactory(
                getPlayerContext(), surroundSoundManager != null ? surroundSoundManager.getSurroundAudioProcessor() : null);

        androidx.media3.datasource.DataSource.Factory cachedOkHttpFactory = com.example.animelib.util.MediaCacheManager.createCacheDataSourceFactory(this, okHttpDataSourceFactory);

        player = new ExoPlayer.Builder(getPlayerContext(), rf2)
                .setSeekBackIncrementMs(10000)
                .setSeekForwardIncrementMs(10000)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(cachedOkHttpFactory))
                .setLoadControl(loadControl)
                .setTrackSelector(trackSelector)
                .build();

        playerView.setPlayer(player);
        setVideoResizeMode(currentResizeMode);
        setupPlayerListener();
        
        // Set player for ambient light manager
        if (ambientLightManager != null) {
            ambientLightManager.setDataSourceFactory(okHttpDataSourceFactory);
            ambientLightManager.setPlayer(player, mediaItem, hlsUrl);
        }

        if (surroundSoundManager != null) {
            surroundSoundManager.attachPlayer(player);
        }

        // Ensure controller is properly configured for play/pause buttons
        playerView.setUseController(true);
        updateControllerAutoHide();

        Log.d("HlsPlayerInit", "HLS ExoPlayer bound to PlayerView with controller enabled");
        
        // Update gestures manager with new player
        gesturesManager.updatePlayer(player);
        
        // Initialize timecode manager with UI components
        MaterialButton skipSegmentButton = findViewById(R.id.skipSegmentButton);
        timecodeManager.initializeViews(player, playerView, skipSegmentButton);

        player.setMediaItem(mediaItem);
        player.prepare();
        setupSubtitlePlayerListener(player);
        applySubtitlesStateToPlayer();
        if (autoPlayOnPrepare) {
            player.play();
        }
        autoPlayOnPrepare = true;

        // Re-setup all player control buttons for the new player
        setupPlayerControlButtons();

        // Add listener for errors
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.e("HlsPlayer", "HLS playback error: " + error.getMessage(), error);
                Log.e("HlsPlayer", "Error type: " + error.errorCode + ", current quality: " + preferredQuality);
                String errorMsg = "Ошибка HLS воспроизведения";

                // Check if this is a 4K playback error
                boolean is4KError = (preferredQuality != null && (preferredQuality.equals("2160p") || preferredQuality.equals("4Kp"))) &&
                        (error.getMessage().contains("Source error") || 
                         error.getMessage().contains("Decoder") ||
                         error.getMessage().contains("Video decoder error") ||
                         error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
                         error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED);
                
                if (is4KError) {
                    Log.w("HlsPlayer", "4K HLS playback failed, attempting fallback to 720p");
                    errorMsg = "4K не поддерживается на этом устройстве. Переключаемся на 720p...";
                    CustomToast.showWarning(VideoPlayerActivity.this, errorMsg);
                    
                    // For Kodik HLS, fallback to 720p (standard Kodik quality)
                    preferredQuality = "720p";
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        restartPlayerWithNewQuality();
                    }, 500);
                    return; // Don't show error toast
                } else if (error.getMessage().contains("403")) {
                    errorMsg += ": доступ запрещен (403)";
                } else if (error.getMessage().contains("404")) {
                    errorMsg += ": HLS плейлист не найден (404)";
                } else {
                    errorMsg += ": " + error.getMessage();
                }

                showVideoErrorDialog("Ошибка HLS воспроизведения", errorMsg, () -> {
                    EpisodeResponse.PlayerData cur = playersManager.getCurrentPlayerData();
                    if (cur != null) {
                        onPlayerSelected(cur);
                    } else if (currentVideoUrl != null) {
                        initializeHlsPlayer(currentVideoUrl);
                    }
                });
            }
        });

        Log.d("HlsPlayer", "HLS player initialized and started");
    }

    // ================= Subtitle Helpers =================

    private List<MediaItem.SubtitleConfiguration> buildSubtitleConfigurations() {
        List<MediaItem.SubtitleConfiguration> configs = new ArrayList<>();
        if (!subtitlesEnabled) {
            return configs;
        }

        if (isOfflineMode || (currentVideoUrl != null && currentVideoUrl.startsWith("/"))) {
            com.example.animelib.data.entity.DownloadedEpisodeEntity offlineEp = getCurrentOfflineEpisode();
            String localPath = (offlineEp != null && offlineEp.getLocalFilePath() != null) ? offlineEp.getLocalFilePath() : currentVideoUrl;
            if (localPath != null && localPath.startsWith("/")) {
                File videoFile = new File(localPath);
                File dir = videoFile.getParentFile();
                if (dir != null && dir.exists()) {
                    String baseName = videoFile.getName();
                    int dotIdx = baseName.lastIndexOf('.');
                    if (dotIdx > 0) baseName = baseName.substring(0, dotIdx);

                    File[] files = dir.listFiles();
                    if (files != null) {
                        int trackIdx = 1;
                        for (File f : files) {
                            if (f.getName().startsWith(baseName + "_sub_") && f.length() > 0) {
                                String name = f.getName();
                                String format = "";
                                int lastDot = name.lastIndexOf('.');
                                if (lastDot > 0) format = name.substring(lastDot + 1).toLowerCase();

                                String mimeType = getMimeTypeForSubtitle(format, f.getAbsolutePath());
                                MediaItem.SubtitleConfiguration config = new MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(f))
                                        .setMimeType(mimeType)
                                        .setLanguage("ru")
                                        .setLabel("Субтитры (офлайн " + format.toUpperCase() + ")")
                                        .setSelectionFlags(trackIdx == 1 ? C.SELECTION_FLAG_DEFAULT : 0)
                                        .setRoleFlags(C.ROLE_FLAG_SUBTITLE)
                                        .build();
                                configs.add(config);
                                trackIdx++;
                            }
                        }
                    }
                }
            }
            if (!configs.isEmpty()) {
                return configs;
            }
        }

        if (playersManager == null) {
            return configs;
        }

        EpisodeResponse.PlayerData playerData = playersManager.getCurrentPlayerData();
        if (playerData == null || playerData.getSubtitles() == null || playerData.getSubtitles().isEmpty()) {
            return configs;
        }

        List<EpisodeResponse.SubtitleData> subtitlesList = playerData.getSubtitles();
        if (subtitlesList.isEmpty()) {
            return configs;
        }

        // Determine which subtitle index is preferred
        int preferredIndex = -1;
        for (int i = 0; i < subtitlesList.size(); i++) {
            EpisodeResponse.SubtitleData sub = subtitlesList.get(i);
            if (sub == null || sub.getSrc() == null || sub.getSrc().trim().isEmpty()) {
                continue;
            }
            String format = sub.getFormat() != null ? sub.getFormat().trim().toLowerCase() : "";
            if ("auto".equalsIgnoreCase(subtitleFormat)) {
                if ("ass".equals(format) || "ssa".equals(format)) {
                    preferredIndex = i;
                    break;
                }
            } else if (format.equalsIgnoreCase(subtitleFormat)) {
                preferredIndex = i;
                break;
            }
        }

        // Fallback to first valid subtitle if preferred format not found
        if (preferredIndex == -1) {
            for (int i = 0; i < subtitlesList.size(); i++) {
                EpisodeResponse.SubtitleData sub = subtitlesList.get(i);
                if (sub != null && sub.getSrc() != null && !sub.getSrc().trim().isEmpty()) {
                    preferredIndex = i;
                    break;
                }
            }
        }

        if (preferredIndex == -1) {
            return configs;
        }

        // Add preferred track FIRST so ExoPlayer initializes it as track 0
        List<Integer> order = new ArrayList<>();
        order.add(preferredIndex);
        for (int i = 0; i < subtitlesList.size(); i++) {
            if (i != preferredIndex) {
                order.add(i);
            }
        }

        for (int idx : order) {
            EpisodeResponse.SubtitleData sub = subtitlesList.get(idx);
            if (sub == null || sub.getSrc() == null || sub.getSrc().trim().isEmpty()) {
                continue;
            }

            String format = sub.getFormat() != null ? sub.getFormat().trim().toLowerCase() : "";
            String mimeType = getMimeTypeForSubtitle(format, sub.getSrc());
            String absUrl = VideoUrlHelper.toAbsoluteVideoUrl(sub.getSrc(), currentVideoDomain);

            String label = sub.getName();
            if (label == null || label.isEmpty()) {
                label = sub.getFilename();
            }
            if (label == null || label.isEmpty()) {
                label = "Субтитры (" + (format.isEmpty() ? " track " + (idx + 1) : format.toUpperCase()) + ")";
            }

            boolean isPreferred = (idx == preferredIndex);

            MediaItem.SubtitleConfiguration config = new MediaItem.SubtitleConfiguration.Builder(Uri.parse(absUrl))
                    .setMimeType(mimeType)
                    .setLanguage("ru")
                    .setLabel(label)
                    .setSelectionFlags(isPreferred ? C.SELECTION_FLAG_DEFAULT : 0)
                    .setRoleFlags(C.ROLE_FLAG_SUBTITLE)
                    .build();

            configs.add(config);
        }

        return configs;
    }

    private String getMimeTypeForSubtitle(String format, String url) {
        if (format != null) {
            String fmt = format.trim().toLowerCase();
            if ("ass".equals(fmt) || "ssa".equals(fmt)) {
                return androidx.media3.common.MimeTypes.TEXT_SSA;
            } else if ("vtt".equals(fmt) || "webvtt".equals(fmt)) {
                return androidx.media3.common.MimeTypes.TEXT_VTT;
            } else if ("srt".equals(fmt) || "subrip".equals(fmt)) {
                return androidx.media3.common.MimeTypes.APPLICATION_SUBRIP;
            }
        }
        if (url != null) {
            String lowerUrl = url.toLowerCase();
            if (lowerUrl.endsWith(".ass") || lowerUrl.endsWith(".ssa")) {
                return androidx.media3.common.MimeTypes.TEXT_SSA;
            } else if (lowerUrl.endsWith(".vtt")) {
                return androidx.media3.common.MimeTypes.TEXT_VTT;
            } else if (lowerUrl.endsWith(".srt")) {
                return androidx.media3.common.MimeTypes.APPLICATION_SUBRIP;
            }
        }
        return androidx.media3.common.MimeTypes.TEXT_UNKNOWN;
    }

    private MediaItem createMediaItemWithSubtitles(String videoUrl) {
        MediaItem.Builder builder = new MediaItem.Builder().setUri(videoUrl);
        List<MediaItem.SubtitleConfiguration> subtitleConfigs = buildSubtitleConfigurations();
        if (!subtitleConfigs.isEmpty()) {
            builder.setSubtitleConfigurations(subtitleConfigs);
            Log.d("VideoPlayer", "Attached " + subtitleConfigs.size() + " subtitle tracks to media item.");
        }
        return builder.build();
    }

    private boolean isCurrentSubtitleVttOrSrt() {
        if ("vtt".equalsIgnoreCase(subtitleFormat) || "webvtt".equalsIgnoreCase(subtitleFormat) || "srt".equalsIgnoreCase(subtitleFormat)) {
            return true;
        }
        if ("ass".equalsIgnoreCase(subtitleFormat) || "ssa".equalsIgnoreCase(subtitleFormat)) {
            return false;
        }
        if (playersManager != null) {
            EpisodeResponse.PlayerData playerData = playersManager.getCurrentPlayerData();
            if (playerData != null && playerData.getSubtitles() != null) {
                for (EpisodeResponse.SubtitleData sub : playerData.getSubtitles()) {
                    if (sub != null && sub.getFormat() != null) {
                        String fmt = sub.getFormat().trim().toLowerCase();
                        if ("ass".equals(fmt) || "ssa".equals(fmt)) {
                            return false;
                        }
                        if ("vtt".equals(fmt) || "webvtt".equals(fmt) || "srt".equals(fmt)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static class AssPathSpan extends ReplacementSpan {
        private final Path rawPath;
        private final int fillColor;
        private final int strokeColor;
        private final float strokeWidth;
        private final float posX;
        private final float posY;
        private final float playResX;
        private final float playResY;

        public AssPathSpan(Path rawPath, int fillColor, int strokeColor, float strokeWidth, float posX, float posY, float playResX, float playResY) {
            this.rawPath = rawPath;
            this.fillColor = fillColor;
            this.strokeColor = strokeColor;
            this.strokeWidth = strokeWidth;
            this.posX = posX;
            this.posY = posY;
            this.playResX = playResX > 0 ? playResX : 1280.0f;
            this.playResY = playResY > 0 ? playResY : 720.0f;
        }

        @Override
        public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
            if (fm != null) {
                fm.ascent = 0;
                fm.top = 0;
                fm.descent = 0;
                fm.bottom = 0;
            }
            return 1;
        }

        @Override
        public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
            if (rawPath == null) return;
            canvas.save();

            float canvasW = canvas.getWidth();
            float canvasH = canvas.getHeight();
            if (canvasW <= 0) canvasW = 1280.0f;
            if (canvasH <= 0) canvasH = 720.0f;

            float scaleX = canvasW / playResX;
            float scaleY = canvasH / playResY;

            Matrix matrix = new Matrix();
            if (posX >= 0 && posY >= 0) {
                matrix.postTranslate(posX, posY);
            }
            matrix.postScale(scaleX, scaleY);

            Path transformedPath = new Path();
            rawPath.transform(matrix, transformedPath);

            if (fillColor != Color.TRANSPARENT) {
                Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                fillPaint.setStyle(Paint.Style.FILL);
                fillPaint.setColor(fillColor);
                canvas.drawPath(transformedPath, fillPaint);
            }

            if (strokeColor != Color.TRANSPARENT && strokeWidth > 0) {
                Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                strokePaint.setStyle(Paint.Style.STROKE);
                strokePaint.setColor(strokeColor);
                strokePaint.setStrokeWidth(strokeWidth * ((scaleX + scaleY) / 2.0f));
                canvas.drawPath(transformedPath, strokePaint);
            }

            canvas.restore();
        }
    }

    private static Path parseAssPath(String drawingCommands, float scale) {
        if (drawingCommands == null || drawingCommands.trim().isEmpty()) return null;
        try {
            Path path = new Path();
            String[] tokens = drawingCommands.trim().split("[\\s,]+");
            char currentCmd = 'm';
            int i = 0;
            boolean hasPoints = false;
            while (i < tokens.length) {
                String token = tokens[i].trim();
                if (token.isEmpty()) { i++; continue; }
                char c = Character.toLowerCase(token.charAt(0));
                if (c == 'm' || c == 'n' || c == 'l' || c == 'b' || c == 's' || c == 'p' || c == 'c') {
                    currentCmd = c;
                    i++;
                    if (c == 'c') {
                        path.close();
                        continue;
                    }
                    if (i >= tokens.length) break;
                }
                if (currentCmd == 'm' || currentCmd == 'n') {
                    if (i + 1 < tokens.length) {
                        float x = Float.parseFloat(tokens[i]) * scale;
                        float y = Float.parseFloat(tokens[i + 1]) * scale;
                        path.moveTo(x, y);
                        hasPoints = true;
                        i += 2;
                        currentCmd = 'l';
                    } else { i++; }
                } else if (currentCmd == 'l' || currentCmd == 's' || currentCmd == 'p') {
                    if (i + 1 < tokens.length) {
                        float x = Float.parseFloat(tokens[i]) * scale;
                        float y = Float.parseFloat(tokens[i + 1]) * scale;
                        path.lineTo(x, y);
                        hasPoints = true;
                        i += 2;
                    } else { i++; }
                } else if (currentCmd == 'b') {
                    if (i + 5 < tokens.length) {
                        float x1 = Float.parseFloat(tokens[i]) * scale;
                        float y1 = Float.parseFloat(tokens[i + 1]) * scale;
                        float x2 = Float.parseFloat(tokens[i + 2]) * scale;
                        float y2 = Float.parseFloat(tokens[i + 3]) * scale;
                        float x3 = Float.parseFloat(tokens[i + 4]) * scale;
                        float y3 = Float.parseFloat(tokens[i + 5]) * scale;
                        path.cubicTo(x1, y1, x2, y2, x3, y3);
                        hasPoints = true;
                        i += 6;
                    } else { i++; }
                } else {
                    i++;
                }
            }
            return hasPoints ? path : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap renderAssVectorPath(Path rawPath, int fillColor, int strokeColor, float strokeWidth, float pScale) {
        if (rawPath == null) return null;
        try {
            Path scaledPath = new Path();
            Matrix scaleMatrix = new Matrix();
            scaleMatrix.setScale(pScale, pScale);
            rawPath.transform(scaleMatrix, scaledPath);

            RectF bounds = new RectF();
            scaledPath.computeBounds(bounds, true);

            int padding = Math.max(4, Math.round(strokeWidth * 2));
            int bmpWidth = Math.max(1, Math.round(bounds.width()) + padding * 2);
            int bmpHeight = Math.max(1, Math.round(bounds.height()) + padding * 2);

            if (bmpWidth > 2048) bmpWidth = 2048;
            if (bmpHeight > 2048) bmpHeight = 2048;

            Bitmap bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            Matrix translate = new Matrix();
            translate.postTranslate(-bounds.left + padding, -bounds.top + padding);
            Path drawPath = new Path();
            scaledPath.transform(translate, drawPath);

            if (fillColor != Color.TRANSPARENT) {
                Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                fillPaint.setStyle(Paint.Style.FILL);
                fillPaint.setColor(fillColor);
                canvas.drawPath(drawPath, fillPaint);
            }

            if (strokeColor != Color.TRANSPARENT && strokeWidth > 0) {
                Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                strokePaint.setStyle(Paint.Style.STROKE);
                strokePaint.setColor(strokeColor);
                strokePaint.setStrokeWidth(strokeWidth);
                strokePaint.setStrokeJoin(Paint.Join.ROUND);
                strokePaint.setStrokeCap(Paint.Cap.ROUND);
                canvas.drawPath(drawPath, strokePaint);
            }

            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    private static void applyAnAlignment(Cue.Builder builder, int an) {
        switch (an) {
            case 7:
                builder.setPosition(0.05f).setPositionAnchor(Cue.ANCHOR_TYPE_START)
                       .setLine(0.05f, Cue.LINE_TYPE_FRACTION).setLineAnchor(Cue.ANCHOR_TYPE_START)
                       .setTextAlignment(android.text.Layout.Alignment.ALIGN_NORMAL);
                break;
            case 8:
                builder.setPosition(0.5f).setPositionAnchor(Cue.ANCHOR_TYPE_MIDDLE)
                       .setLine(0.05f, Cue.LINE_TYPE_FRACTION).setLineAnchor(Cue.ANCHOR_TYPE_START)
                       .setTextAlignment(android.text.Layout.Alignment.ALIGN_CENTER);
                break;
            case 9:
                builder.setPosition(0.95f).setPositionAnchor(Cue.ANCHOR_TYPE_END)
                       .setLine(0.05f, Cue.LINE_TYPE_FRACTION).setLineAnchor(Cue.ANCHOR_TYPE_START)
                       .setTextAlignment(android.text.Layout.Alignment.ALIGN_OPPOSITE);
                break;
            case 4:
                builder.setPosition(0.05f).setPositionAnchor(Cue.ANCHOR_TYPE_START)
                       .setLine(0.5f, Cue.LINE_TYPE_FRACTION).setLineAnchor(Cue.ANCHOR_TYPE_MIDDLE)
                       .setTextAlignment(android.text.Layout.Alignment.ALIGN_NORMAL);
                break;
            case 5:
                builder.setPosition(0.5f).setPositionAnchor(Cue.ANCHOR_TYPE_MIDDLE)
                       .setLine(0.5f, Cue.LINE_TYPE_FRACTION).setLineAnchor(Cue.ANCHOR_TYPE_MIDDLE)
                       .setTextAlignment(android.text.Layout.Alignment.ALIGN_CENTER);
                break;
            case 6:
                builder.setPosition(0.95f).setPositionAnchor(Cue.ANCHOR_TYPE_END)
                       .setLine(0.5f, Cue.LINE_TYPE_FRACTION).setLineAnchor(Cue.ANCHOR_TYPE_MIDDLE)
                       .setTextAlignment(android.text.Layout.Alignment.ALIGN_OPPOSITE);
                break;
            case 1:
                builder.setPosition(0.05f).setPositionAnchor(Cue.ANCHOR_TYPE_START)
                       .setLine(Cue.DIMEN_UNSET, Cue.TYPE_UNSET).setLineAnchor(Cue.ANCHOR_TYPE_END)
                       .setTextAlignment(android.text.Layout.Alignment.ALIGN_NORMAL);
                break;
            case 2:
                builder.setPosition(0.5f).setPositionAnchor(Cue.ANCHOR_TYPE_MIDDLE)
                       .setLine(Cue.DIMEN_UNSET, Cue.TYPE_UNSET).setLineAnchor(Cue.ANCHOR_TYPE_END)
                       .setTextAlignment(android.text.Layout.Alignment.ALIGN_CENTER);
                break;
            case 3:
                builder.setPosition(0.95f).setPositionAnchor(Cue.ANCHOR_TYPE_END)
                       .setLine(Cue.DIMEN_UNSET, Cue.TYPE_UNSET).setLineAnchor(Cue.ANCHOR_TYPE_END)
                       .setTextAlignment(android.text.Layout.Alignment.ALIGN_OPPOSITE);
                break;
        }
    }

    private static Integer parseAssColor(String rawHex) {
        if (rawHex == null) return null;
        String clean = rawHex.replaceAll("(?i)[&H#]", "").trim();
        if (clean.isEmpty()) return null;

        while (clean.length() < 6) {
            clean = "0" + clean;
        }

        try {
            if (clean.length() == 6) {
                int b = Integer.parseInt(clean.substring(0, 2), 16);
                int g = Integer.parseInt(clean.substring(2, 4), 16);
                int r = Integer.parseInt(clean.substring(4, 6), 16);
                return Color.argb(255, r, g, b);
            } else if (clean.length() >= 8) {
                String hex8 = clean.substring(clean.length() - 8);
                int assAlpha = Integer.parseInt(hex8.substring(0, 2), 16);
                int alpha = Math.max(0, Math.min(255, 255 - assAlpha));
                int b = Integer.parseInt(hex8.substring(2, 4), 16);
                int g = Integer.parseInt(hex8.substring(4, 6), 16);
                int r = Integer.parseInt(hex8.substring(6, 8), 16);
                return Color.argb(alpha, r, g, b);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static boolean isAssDrawingPath(String str) {
        if (str == null || str.trim().isEmpty()) return false;
        String trimmed = str.trim();

        if (trimmed.matches("(?i).*\\{\\\\p[1-9]\\}.*")) {
            return true;
        }

        String cleanText = trimmed.replaceAll("\\{([^\\}]+)\\}", "").trim();
        if (cleanText.matches("(?i)^(?:[mlbspcn]\\s+-?\\d+(?:\\.\\d+)?(?:\\s+|$))+.*")) {
            return true;
        }

        String[] tokens = cleanText.split("\\s+");
        if (tokens.length < 3) return false;
        int drawingTokens = 0;
        boolean hasDrawingCmd = false;
        for (String t : tokens) {
            if (t.matches("-?\\d+(?:\\.\\d+)?")) {
                drawingTokens++;
            } else if (t.matches("(?i)^[mlbspcn]$")) {
                drawingTokens++;
                hasDrawingCmd = true;
            }
        }
        return hasDrawingCmd && ((double) drawingTokens / tokens.length) >= 0.5;
    }

    private Cue processAssCue(Cue cue) {
        if (cue == null || cue.text == null) return null;
        CharSequence text = cue.text;
        if (text.length() == 0) return null;

        String raw = text.toString();
        if (raw.trim().isEmpty()) return null;

        Cue.Builder builder = cue.buildUpon();

        if (isAssDrawingPath(raw)) {
            float posX = -1.0f;
            float posY = -1.0f;
            java.util.regex.Matcher posMatcher = java.util.regex.Pattern.compile("(?i)\\\\pos\\(\\s*(-?\\d+(?:\\.\\d+)?)\\s*,\\s*(-?\\d+(?:\\.\\d+)?)\\s*\\)").matcher(raw);
            if (posMatcher.find()) {
                try {
                    posX = Float.parseFloat(posMatcher.group(1));
                    posY = Float.parseFloat(posMatcher.group(2));
                } catch (Exception ignored) {}
            }

            int fillColor = Color.WHITE;
            java.util.regex.Matcher colorMatcher = java.util.regex.Pattern.compile("(?i)\\\\(?:1c|c)[&H#]*([0-9a-fA-F]{1,8})&?").matcher(raw);
            if (colorMatcher.find()) {
                Integer c = parseAssColor(colorMatcher.group(1));
                if (c != null) fillColor = c;
            }

            int strokeColor = Color.BLACK;
            java.util.regex.Matcher outlineColorMatcher = java.util.regex.Pattern.compile("(?i)\\\\3c[&H#]*([0-9a-fA-F]{1,8})&?").matcher(raw);
            if (outlineColorMatcher.find()) {
                Integer c = parseAssColor(outlineColorMatcher.group(1));
                if (c != null) strokeColor = c;
            }

            float strokeWidth = 2.0f;
            java.util.regex.Matcher bordMatcher = java.util.regex.Pattern.compile("(?i)\\\\bord(\\d+(?:\\.\\d+)?)").matcher(raw);
            if (bordMatcher.find()) {
                try {
                    strokeWidth = Float.parseFloat(bordMatcher.group(1));
                } catch (Exception ignored) {}
            }

            float pScale = 1.0f;
            java.util.regex.Matcher pMatcher = java.util.regex.Pattern.compile("(?i)\\\\p([1-9])").matcher(raw);
            if (pMatcher.find()) {
                int pLevel = Integer.parseInt(pMatcher.group(1));
                pScale = 1.0f / (float) (1 << (pLevel - 1));
            }

            String drawingCommands = null;
            java.util.regex.Matcher pBlockMatcher = java.util.regex.Pattern.compile("(?i)\\{\\\\p[1-9]\\}(.*?)(?:\\{\\\\p0\\}|$)", java.util.regex.Pattern.DOTALL).matcher(raw);
            if (pBlockMatcher.find()) {
                drawingCommands = pBlockMatcher.group(1).replaceAll("\\{([^\\}]+)\\}", "").trim();
            } else {
                drawingCommands = raw.replaceAll("\\{([^\\}]+)\\}", "").trim();
            }

            float playResX = (posX > 1280 || posY > 720 || raw.contains("1920") || raw.contains("1080")) ? 1920.0f : 1280.0f;
            float playResY = (posX > 1280 || posY > 720 || raw.contains("1920") || raw.contains("1080")) ? 1080.0f : 720.0f;

            Path path = parseAssPath(drawingCommands, pScale);
            if (path != null) {
                Bitmap vectorBmp = renderAssVectorPath(path, fillColor, strokeColor, strokeWidth, pScale);
                if (vectorBmp != null) {
                    builder.setBitmap(vectorBmp);
                    if (posX >= 0 && posY >= 0) {
                        float xRatio = Math.max(0.0f, Math.min(1.0f, posX / playResX));
                        float yRatio = Math.max(0.0f, Math.min(1.0f, posY / playResY));
                        builder.setPosition(xRatio)
                               .setPositionAnchor(Cue.ANCHOR_TYPE_START)
                               .setLine(yRatio, Cue.LINE_TYPE_FRACTION)
                               .setLineAnchor(Cue.ANCHOR_TYPE_START)
                               .setSize((float) vectorBmp.getWidth() / playResX);
                    } else {
                        builder.setPosition(0.5f)
                               .setPositionAnchor(Cue.ANCHOR_TYPE_MIDDLE)
                               .setLine(0.5f, Cue.LINE_TYPE_FRACTION)
                               .setLineAnchor(Cue.ANCHOR_TYPE_MIDDLE);
                    }
                    return builder.build();
                }
            }
        }

        builder = cue.buildUpon();
        SpannableStringBuilder ssb = new SpannableStringBuilder(text);

        // Strip vector drawing blocks {\p1}...{\p0} from ssb if present
        String str = ssb.toString();
        java.util.regex.Matcher pBlockMatcher = java.util.regex.Pattern.compile("(?i)\\{\\\\p[1-9]\\}[^\\{]*(\\{\\\\p0\\})?").matcher(str);
        while (pBlockMatcher.find()) {
            ssb.delete(pBlockMatcher.start(), pBlockMatcher.end());
            str = ssb.toString();
            pBlockMatcher = java.util.regex.Pattern.compile("(?i)\\{\\\\p[1-9]\\}[^\\{]*(\\{\\\\p0\\})?").matcher(str);
        }

        // Strip standalone vector path sequences
        str = ssb.toString();
        java.util.regex.Matcher drawingPathMatcher = java.util.regex.Pattern.compile("(?i)(?:^|\\s)(?:m|n|l|b|s|p|c)(?:\\s+-?\\d+(?:\\.\\d+)?\\s*)+").matcher(str);
        while (drawingPathMatcher.find()) {
            ssb.delete(drawingPathMatcher.start(), drawingPathMatcher.end());
            str = ssb.toString();
            drawingPathMatcher = java.util.regex.Pattern.compile("(?i)(?:^|\\s)(?:m|n|l|b|s|p|c)(?:\\s+-?\\d+(?:\\.\\d+)?\\s*)+").matcher(str);
        }

        if (ssb.toString().replaceAll("\\{([^\\}]+)\\}", "").trim().isEmpty()) {
            return null;
        }

        // Convert ASS raw newline tags \N and \n to real newlines, \h to space
        str = ssb.toString();
        int idx;
        while ((idx = str.indexOf("\\N")) != -1) {
            ssb.replace(idx, idx + 2, "\n");
            str = ssb.toString();
        }
        while ((idx = str.indexOf("\\n")) != -1) {
            ssb.replace(idx, idx + 2, "\n");
            str = ssb.toString();
        }
        while ((idx = str.indexOf("\\h")) != -1) {
            ssb.replace(idx, idx + 2, " ");
            str = ssb.toString();
        }

        // Parse inline ASS tags {...}
        str = ssb.toString();
        if (str.contains("{")) {
            java.util.regex.Pattern tagPattern = java.util.regex.Pattern.compile("\\{([^\\}]+)\\}");

            Integer currentColor = null;
            Integer currentOutlineColor = null;
            String currentFont = null;
            Integer currentSize = null;
            Boolean isBold = null;
            Boolean isItalic = null;
            Boolean isUnderline = null;

            int safetyCounter = 0;
            while (safetyCounter++ < 50) {
                java.util.regex.Matcher matcher = tagPattern.matcher(str);
                if (!matcher.find()) break;

                int tagStart = matcher.start();
                int tagEnd = matcher.end();
                String tagBlock = matcher.group(1);

                int anVal = 2; // Default ASS alignment is bottom-center \an2
                java.util.regex.Matcher anMatcher = java.util.regex.Pattern.compile("(?i)\\\\an([1-9])").matcher(tagBlock);
                if (anMatcher.find()) {
                    anVal = Integer.parseInt(anMatcher.group(1));
                    applyAnAlignment(builder, anVal);
                }

                java.util.regex.Matcher posMatcher = java.util.regex.Pattern.compile("(?i)\\\\pos\\(\\s*(-?\\d+(?:\\.\\d+)?)\\s*,\\s*(-?\\d+(?:\\.\\d+)?)\\s*\\)").matcher(tagBlock);
                if (posMatcher.find()) {
                    try {
                        float px = Float.parseFloat(posMatcher.group(1));
                        float py = Float.parseFloat(posMatcher.group(2));
                        float scriptResX = (px > 1280 || py > 720 || raw.contains("1920") || raw.contains("1080")) ? 1920.0f : 1280.0f;
                        float scriptResY = (px > 1280 || py > 720 || raw.contains("1920") || raw.contains("1080")) ? 1080.0f : 720.0f;

                        float normX = Math.max(0.0f, Math.min(1.0f, px / scriptResX));
                        float normY = Math.max(0.0f, Math.min(1.0f, py / scriptResY));

                        int xAnchor = Cue.ANCHOR_TYPE_MIDDLE;
                        int yAnchor = Cue.ANCHOR_TYPE_END;
                        if (anVal == 1 || anVal == 4 || anVal == 7) xAnchor = Cue.ANCHOR_TYPE_START;
                        else if (anVal == 3 || anVal == 6 || anVal == 9) xAnchor = Cue.ANCHOR_TYPE_END;

                        if (anVal >= 7) yAnchor = Cue.ANCHOR_TYPE_START;
                        else if (anVal >= 4) yAnchor = Cue.ANCHOR_TYPE_MIDDLE;

                        builder.setPosition(normX).setPositionAnchor(xAnchor)
                               .setLine(normY, Cue.LINE_TYPE_FRACTION).setLineAnchor(yAnchor);
                    } catch (Exception ignored) {}
                }

                java.util.regex.Matcher colorMatcher = java.util.regex.Pattern.compile("(?i)\\\\(?:1c|c)[&H#]*([0-9a-fA-F]{1,8})&?").matcher(tagBlock);
                if (colorMatcher.find()) {
                    currentColor = parseAssColor(colorMatcher.group(1));
                }

                java.util.regex.Matcher outlineColorMatcher = java.util.regex.Pattern.compile("(?i)\\\\3c[&H#]*([0-9a-fA-F]{1,8})&?").matcher(tagBlock);
                if (outlineColorMatcher.find()) {
                    currentOutlineColor = parseAssColor(outlineColorMatcher.group(1));
                }

                java.util.regex.Matcher fontMatcher = java.util.regex.Pattern.compile("(?i)\\\\fn([^\\\\}]+)").matcher(tagBlock);
                if (fontMatcher.find()) {
                    currentFont = fontMatcher.group(1).trim();
                    if (currentFont.isEmpty()) currentFont = null;
                }

                java.util.regex.Matcher sizeMatcher = java.util.regex.Pattern.compile("(?i)\\\\fs(\\d+)").matcher(tagBlock);
                if (sizeMatcher.find()) {
                    try {
                        currentSize = Integer.parseInt(sizeMatcher.group(1));
                    } catch (Exception ignored) {}
                }

                java.util.regex.Matcher boldMatcher = java.util.regex.Pattern.compile("(?i)\\\\b([01]|\\d{3})").matcher(tagBlock);
                if (boldMatcher.find()) {
                    String val = boldMatcher.group(1);
                    isBold = "1".equals(val) || (val.length() == 3 && !val.equals("000"));
                }

                java.util.regex.Matcher italicMatcher = java.util.regex.Pattern.compile("(?i)\\\\i([01])").matcher(tagBlock);
                if (italicMatcher.find()) {
                    isItalic = "1".equals(italicMatcher.group(1));
                }

                java.util.regex.Matcher underlineMatcher = java.util.regex.Pattern.compile("(?i)\\\\u([01])").matcher(tagBlock);
                if (underlineMatcher.find()) {
                    isUnderline = "1".equals(underlineMatcher.group(1));
                }

                if (tagBlock.matches("(?i).*\\\\r.*")) {
                    currentColor = null;
                    currentOutlineColor = null;
                    currentFont = null;
                    currentSize = null;
                    isBold = null;
                    isItalic = null;
                    isUnderline = null;
                }

                ssb.delete(tagStart, tagEnd);

                str = ssb.toString();
                int nextTagIndex = str.indexOf('{', tagStart);
                int textSegmentEnd = (nextTagIndex != -1) ? nextTagIndex : str.length();

                if (textSegmentEnd > tagStart) {
                    if (currentColor != null) {
                        ssb.setSpan(new ForegroundColorSpan(currentColor), tagStart, textSegmentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    if (currentFont != null) {
                        Typeface tf = FontResolver.resolveTypeface(VideoPlayerActivity.this, currentFont, isBold != null && isBold, isItalic != null && isItalic);
                        ssb.setSpan(new CustomTypefaceSpan(tf), tagStart, textSegmentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    if (currentSize != null && currentSize > 0) {
                        ssb.setSpan(new AbsoluteSizeSpan(currentSize, true), tagStart, textSegmentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    if (isBold != null || isItalic != null) {
                        boolean b = (isBold != null && isBold);
                        boolean it = (isItalic != null && isItalic);
                        if (b && it) {
                            ssb.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), tagStart, textSegmentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        } else if (b) {
                            ssb.setSpan(new StyleSpan(Typeface.BOLD), tagStart, textSegmentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        } else if (it) {
                            ssb.setSpan(new StyleSpan(Typeface.ITALIC), tagStart, textSegmentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                    if (isUnderline != null && isUnderline) {
                        ssb.setSpan(new UnderlineSpan(), tagStart, textSegmentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
        }

        if (ssb.toString().trim().isEmpty()) {
            return null;
        }

        return builder.setText(ssb).build();
    }

    private List<Cue> resolveCueCollisions(List<Cue> cues) {
        if (cues == null || cues.size() <= 1) return cues;

        List<Cue> resolved = new ArrayList<>();
        int unpositionedBottomCount = 0;

        for (Cue cue : cues) {
            if (cue == null) continue;
            boolean isBottomUnpositioned = (cue.line == Cue.DIMEN_UNSET || cue.lineType == Cue.TYPE_UNSET);
            if (isBottomUnpositioned && cue.bitmap == null) {
                Cue.Builder b = cue.buildUpon();
                float linePos = 0.92f - (unpositionedBottomCount * 0.08f);
                b.setLine(Math.max(0.1f, linePos), Cue.LINE_TYPE_FRACTION)
                 .setLineAnchor(Cue.ANCHOR_TYPE_END);
                unpositionedBottomCount++;
                resolved.add(b.build());
            } else {
                resolved.add(cue);
            }
        }
        return resolved;
    }

    private void setupSubtitlePlayerListener(ExoPlayer p) {
        if (p == null) return;
        p.addListener(new Player.Listener() {
            @Override
            public void onCues(@NonNull CueGroup cueGroup) {
                if (!subtitlesEnabled || playerView == null || playerView.getSubtitleView() == null) {
                    if (playerView != null && playerView.getSubtitleView() != null) {
                        playerView.getSubtitleView().setCues(java.util.Collections.emptyList());
                    }
                    return;
                }
                List<Cue> processedCues = new ArrayList<>();
                for (Cue cue : cueGroup.cues) {
                    if (cue.text != null) {
                        Cue processed = processAssCue(cue);
                        if (processed != null && ((processed.text != null && processed.text.length() > 0) || processed.bitmap != null)) {
                            processedCues.add(processed);
                        }
                    } else if (cue.bitmap != null) {
                        processedCues.add(cue);
                    }
                }
                List<Cue> stackedCues = resolveCueCollisions(processedCues);
                playerView.getSubtitleView().setCues(stackedCues);
            }

            @Override
            public void onTracksChanged(@NonNull Tracks tracks) {
                if (!subtitlesEnabled || player == null) return;

                boolean hasSelectedTextTrack = false;
                Tracks.Group firstSupportedTextGroup = null;

                for (Tracks.Group group : tracks.getGroups()) {
                    if (group.getType() == C.TRACK_TYPE_TEXT) {
                        if (group.isSelected()) {
                            hasSelectedTextTrack = true;
                            break;
                        } else if (firstSupportedTextGroup == null && group.isSupported()) {
                            firstSupportedTextGroup = group;
                        }
                    }
                }

                if (!hasSelectedTextTrack && firstSupportedTextGroup != null) {
                    Log.d("VideoPlayer", "No text track auto-selected by Media3. Forcing selection of text track");
                    try {
                        TrackSelectionParameters newParams = player.getTrackSelectionParameters()
                                .buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                .setOverrideForType(new TrackSelectionOverride(firstSupportedTextGroup.getMediaTrackGroup(), 0))
                                .build();
                        player.setTrackSelectionParameters(newParams);
                    } catch (Exception e) {
                        Log.e("VideoPlayer", "Failed to force text track selection", e);
                    }
                }
            }
        });
    }

    private void applySubtitlesStateToPlayer() {
        if (player != null) {
            try {
                TrackSelectionParameters.Builder builder = player.getTrackSelectionParameters()
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !subtitlesEnabled);
                if (subtitlesEnabled) {
                    builder.setPreferredTextLanguage("ru")
                           .setPreferredTextRoleFlags(C.ROLE_FLAG_SUBTITLE)
                           .setSelectUndeterminedTextLanguage(true);
                }
                player.setTrackSelectionParameters(builder.build());
                Log.d("VideoPlayer", "Updated subtitle track selection state: enabled=" + subtitlesEnabled);
            } catch (Exception e) {
                Log.e("VideoPlayer", "Failed to setTrackSelectionParameters for subtitles", e);
            }
        }
        if (playerView != null && playerView.getSubtitleView() != null) {
            try {
                SubtitleView subtitleView = playerView.getSubtitleView();
                subtitleView.setVisibility(subtitlesEnabled ? View.VISIBLE : View.GONE);
                subtitleView.setApplyEmbeddedStyles(true);
                subtitleView.setApplyEmbeddedFontSizes(true);
                subtitleView.setViewType(SubtitleView.VIEW_TYPE_CANVAS);
                CaptionStyleCompat style = new CaptionStyleCompat(
                        subtitleTextColor,
                        subtitleBackgroundColor,
                        Color.TRANSPARENT,
                        subtitleEdgeType,
                        subtitleEdgeColor,
                        null
                );
                subtitleView.setStyle(style);
                subtitleView.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subtitleTextSize);
            } catch (Exception e) {
                Log.w("VideoPlayer", "Failed to style SubtitleView", e);
            }
        }
    }

    private void reloadPlayerWithSubtitles() {
        if (player == null) return;
        long currentPos = player.getCurrentPosition();
        boolean wasPlaying = player.isPlaying();

        if (currentVideoUrl != null && !currentVideoUrl.isEmpty()) {
            MediaItem mediaItem = createMediaItemWithSubtitles(currentVideoUrl);
            player.setMediaItem(mediaItem);
            player.seekTo(currentPos);
            player.prepare();
            if (wasPlaying) {
                player.play();
            }
            applySubtitlesStateToPlayer();
            Log.d("VideoPlayer", "Reloaded player with updated subtitle configurations at position " + currentPos);
        }
    }

    private void startSeekingState() {
        isSeeking = true;
        if (seekResetHandler != null) {
            seekResetHandler.removeCallbacksAndMessages(null);
        }
        startBufferingMonitoring();
        updatePlayPauseAndLoadingState(true);
    }

    private void scheduleEndSeekingState(long delayMs) {
        if (seekResetHandler != null) {
            seekResetHandler.removeCallbacksAndMessages(null);
            Runnable r = () -> {
                if (!isScrubbingTimeBar) {
                    long currentPos = (player != null) ? player.getCurrentPosition() : 0;
                    long bufferedPos = (player != null) ? player.getBufferedPosition() : 0;
                    long duration = (player != null) ? player.getDuration() : 0;
                    long bufferedAhead = bufferedPos - currentPos;
                    boolean isNearEnd = duration > 0 && currentPos >= (duration - 2000);

                    boolean isReadyAndBuffered = (player != null)
                            && (player.getPlaybackState() == Player.STATE_READY)
                            && (hasRenderedFirstFrame || player.isPlaying())
                            && (isNearEnd || bufferedAhead >= 1500);

                    if (player == null || !player.getPlayWhenReady() || isReadyAndBuffered) {
                        isSeeking = false;
                        updatePlayPauseAndLoadingState(true);
                    } else {
                        if (seekResetHandler != null) {
                            seekResetHandler.postDelayed(() -> scheduleEndSeekingState(0), 250);
                        }
                    }
                }
            };
            if (delayMs <= 0) {
                r.run();
            } else {
                seekResetHandler.postDelayed(r, delayMs);
            }
        } else {
            if (!isScrubbingTimeBar) {
                isSeeking = false;
                updatePlayPauseAndLoadingState(true);
            }
        }
    }

    private void setupPlayerControlButtons() {
        View controllerView = playerView.findViewById(R.id.exo_controller);
        if (controllerView == null) {
            Log.w("PlayerControls", "Controller view not found");
            return;
        }

        // НЕ трогаем стандартные ExoPlayer кнопки (exo_play, exo_pause)
        // ExoPlayer сам их обрабатывает автоматически

        // Настраиваем только наши кастомные кнопки
        ImageButton skipForwardButton = controllerView.findViewById(R.id.skipForwardButton);

        // Setup skip forward button (1 минута 25 секунд)
        if (skipForwardButton != null) {
            skipForwardButton.setOnClickListener(v -> {
                Log.d("PlayerControls", "Skip forward clicked (1:25)");
                if (player != null && player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) {
                    long currentPosition = player.getCurrentPosition();
                    long skipDuration = longSkipDuration * 1000L; // Convert seconds to milliseconds
                    long newPosition = currentPosition + skipDuration;
                    long duration = player.getDuration();

                    // Не даем перемотать дальше конца видео
                    if (newPosition > duration && duration > 0) {
                        newPosition = duration;
                    }

                    startSeekingState();
                    player.seekTo(newPosition);
                    scheduleEndSeekingState(600);
                    Log.d("PlayerControls", "Skipped forward to: " + (newPosition / 1000) + "s (+" + (skipDuration / 1000) + "s)");
                } else {
                    Log.w("PlayerControls", "Skip forward not available");
                }
            });
        }

        // Подключаем listener для синхронизации состояния плеера и кнопок
        setupPlayerListener();

        // Инициализируем состояние кнопок
        updatePlayerControlsState();

        // Устанавливаем начальную видимость play/pause кнопок и индикатора
        updatePlayPauseAndLoadingState(false);

        // Проверяем, что найдены кнопки управления
        ImageButton btnPlayerPlay = controllerView.findViewById(R.id.btnPlayerPlay);
        ImageButton btnPlayerPause = controllerView.findViewById(R.id.btnPlayerPause);

        Log.d("PlayerControls", "Player buttons found - Play: " + (btnPlayerPlay != null) +
                ", Pause: " + (btnPlayerPause != null));

        if (btnPlayerPlay != null) {
            btnPlayerPlay.setOnClickListener(v -> {
                Log.d("PlayerControls", "Play button clicked!");
                if (player != null) {
                    player.play();
                    updatePlayPauseAndLoadingState(true);
                }
            });
        }

        if (btnPlayerPause != null) {
            btnPlayerPause.setOnClickListener(v -> {
                Log.d("PlayerControls", "Pause button clicked!");
                if (player != null) {
                    player.pause();
                    updatePlayPauseAndLoadingState(true);
                }
            });
        }

        Log.d("PlayerControls", "Player control buttons setup completed");
    }

    private void updatePlayLoadingIndicator(int playbackState) {
        updatePlayPauseAndLoadingState(true);
    }

    private void updatePlayPauseAndLoadingState(boolean animate) {
        safeRunOnUiThread(() -> {
            boolean isEnded = (player != null) && (player.getPlaybackState() == Player.STATE_ENDED);
            boolean isPlayWhenReady = (player != null) && player.getPlayWhenReady();
            int playbackState = (player != null) ? player.getPlaybackState() : Player.STATE_IDLE;
            boolean realIsPlaying = (player != null) && player.isPlaying();
            boolean isLoading = (player != null) && player.isLoading();

            long currentPos = (player != null) ? player.getCurrentPosition() : 0;
            long bufferedPos = (player != null) ? player.getBufferedPosition() : 0;
            long duration = (player != null) ? player.getDuration() : 0;
            long bufferedAheadMs = bufferedPos - currentPos;

            boolean isNearEnd = duration > 0 && currentPos >= (duration - 2000);

            // Детектируем реальную подгрузку/буферизацию сети (особенно для MP4 AnimeLib и HLS):
            boolean isNetworkBuffering = isPlayWhenReady && !isNearEnd && (
                    playbackState == Player.STATE_BUFFERING
                    || bufferedAheadMs < 1500
                    || (isLoading && bufferedAheadMs < 3500)
                    || !realIsPlaying
            );

            // Буферизация / загрузка / перемотка:
            boolean isBuffering = !isEnded && (
                    isVideoLoading
                    || !hasRenderedFirstFrame
                    || isSeeking
                    || isScrubbingTimeBar
                    || isNetworkBuffering
            );

            // Показываем индикатор буферизации поверх плеера/контролов, если не активен полноэкранный loadingOverlay
            boolean showBuffering = isBuffering && (loadingOverlay == null || loadingOverlay.getVisibility() != View.VISIBLE);

            // Оверлейный индикатор на самом плеере (отображается, когда контролы скрыты)
            if (playerBufferingIndicator != null) {
                if (showBuffering && !isControllerVisible) {
                    if (playerBufferingIndicator.getVisibility() != View.VISIBLE) {
                        playerBufferingIndicator.animate().cancel();
                        playerBufferingIndicator.setVisibility(View.VISIBLE);
                        playerBufferingIndicator.setAlpha(0f);
                        playerBufferingIndicator.setScaleX(0.7f);
                        playerBufferingIndicator.setScaleY(0.7f);
                        playerBufferingIndicator.animate()
                                .alpha(1.0f)
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(200)
                                .start();
                    }
                    playerBufferingIndicator.bringToFront();
                } else if (playerBufferingIndicator.getVisibility() == View.VISIBLE) {
                    playerBufferingIndicator.animate().cancel();
                    playerBufferingIndicator.animate()
                            .alpha(0f)
                            .scaleX(0.7f)
                            .scaleY(0.7f)
                            .setDuration(150)
                            .withEndAction(() -> playerBufferingIndicator.setVisibility(View.GONE))
                            .start();
                }
            }

            if (playerView == null) return;
            View controllerView = playerView.findViewById(R.id.exo_controller);
            if (controllerView == null) return;

            View play = controllerView.findViewById(R.id.btnPlayerPlay);
            View pause = controllerView.findViewById(R.id.btnPlayerPause);
            View spinner = controllerView.findViewById(R.id.playLoadingIndicator);

            if (play == null || pause == null || spinner == null) return;

            int targetState;
            if (showBuffering) {
                targetState = 2; // LOADING
            } else if (realIsPlaying || (isPlayWhenReady && playbackState == Player.STATE_READY)) {
                targetState = 1; // PAUSE
            } else {
                targetState = 0; // PLAY
            }

            if (currentControlState == targetState && currentControlState != -1) {
                // Убедимся, что нужная view точно видима при совпадении состояния
                View activeView = (targetState == 2) ? spinner : ((targetState == 1) ? pause : play);
                if (activeView.getVisibility() != View.VISIBLE) {
                    activeView.setVisibility(View.VISIBLE);
                    activeView.setAlpha(1.0f);
                }
                return;
            }

            boolean shouldAnimate = animate && (currentControlState != -1);
            currentControlState = targetState;

            View showView = (targetState == 2) ? spinner : ((targetState == 1) ? pause : play);
            View hideView1 = (targetState == 2) ? play : ((targetState == 1) ? play : pause);
            View hideView2 = (targetState == 2) ? pause : ((targetState == 1) ? spinner : spinner);

            animateControlViewSwitch(showView, hideView1, hideView2, shouldAnimate);
        });
    }

    private void animateControlViewSwitch(View showView, View hideView1, View hideView2, boolean animate) {
        if (showView == null) return;

        for (View v : new View[]{hideView1, hideView2}) {
            if (v != null) {
                v.animate().cancel();
                if (animate && v.getVisibility() == View.VISIBLE) {
                    v.animate()
                            .alpha(0f)
                            .scaleX(0.6f)
                            .scaleY(0.6f)
                            .setDuration(150)
                            .withEndAction(() -> {
                                v.setVisibility(View.GONE);
                                v.setAlpha(1.0f);
                                v.setScaleX(1.0f);
                                v.setScaleY(1.0f);
                            })
                            .start();
                } else {
                    v.setVisibility(View.GONE);
                    v.setAlpha(1.0f);
                    v.setScaleX(1.0f);
                    v.setScaleY(1.0f);
                }
            }
        }

        showView.animate().cancel();
        showView.setVisibility(View.VISIBLE);

        if (showView instanceof com.google.android.material.progressindicator.CircularProgressIndicator) {
            ((com.google.android.material.progressindicator.CircularProgressIndicator) showView).show();
        }

        if (animate) {
            showView.setAlpha(0f);
            showView.setScaleX(0.6f);
            showView.setScaleY(0.6f);
            showView.animate()
                    .alpha(1.0f)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .start();
        } else {
            showView.setAlpha(1.0f);
            showView.setScaleX(1.0f);
            showView.setScaleY(1.0f);
        }
    }
    
    /**
     * Добавляет текущую серию в закладки
     */
    private void addBookmark() {
        Log.d("VideoPlayer", "Add bookmark method called");

        EpisodesListResponse.EpisodeItem currentEpisode = episodesManager.getCurrentEpisode();
        if (currentEpisode == null) {
            Log.e("VideoPlayer", "Current episode is null, cannot add bookmark");
            CustomToast.showWarning(this, "Эпизод не выбран");
            return;
        }

        long currentPosition = player != null ? player.getCurrentPosition() : 0;
        String timecode = ApiService.formatTimecode(currentPosition);

        if (isOfflineMode) {
            String animeId = currentAnimeId != null ? currentAnimeId : (getIntent() != null ? getIntent().getStringExtra("EXTRA_ANIME_ID") : null);
            if (animeId == null && getIntent() != null && getIntent().getStringExtra("EXTRA_LOCAL_FILE_PATH") != null && apiService != null && apiService.getDatabaseManager() != null) {
                com.example.animelib.data.entity.DownloadedEpisodeEntity dep =
                        apiService.getDatabaseManager().findEpisodeByPath(getIntent().getStringExtra("EXTRA_LOCAL_FILE_PATH"));
                if (dep != null) animeId = dep.getAnimeId();
            }

            if (animeId != null && apiService != null && apiService.getDatabaseManager() != null) {
                apiService.getDatabaseManager().saveOfflineBookmark(animeId, currentEpisode.getId(), currentEpisode.getNumber(), timecode, currentPosition);
                autoBookmarkSaved = true;
                updateBookmarkButtonColor(true);

                com.example.animelib.models.AnimeBookmarkResponse.BookmarkData offlineBookmark =
                        new com.example.animelib.models.AnimeBookmarkResponse.BookmarkData();
                offlineBookmark.setItemId(currentEpisode.getId());
                offlineBookmark.setProgress(timecode);
                episodesManager.updateBookmarkInAdapter(offlineBookmark);

                CustomToast.showSuccess(this, "Закладка сохранена (" + timecode + ")");
            } else {
                CustomToast.showWarning(this, "Не удалось сохранить закладку");
            }
            return;
        }

        if (apiService == null || !apiService.isAuthorized()) {
            CustomToast.showWarning(this, "Без авторизации поставить закладку нельзя");
            return;
        }

        // Получаем текущие данные эпизода
        EpisodeResponse.PlayerData currentPlayer = playersManager.getCurrentPlayerData();
        if (currentPlayer == null) {
            Log.e("VideoPlayer", "Current player is null, cannot add bookmark");
            return;
        }

        String animeUrl = getIntent().getStringExtra("anime_url");
        String mediaSlug = null;
        if (animeUrl != null && !animeUrl.isEmpty()) {
            mediaSlug = ApiService.extractMediaSlugFromUrl(animeUrl);
        }
        
        if (mediaSlug == null) {
            Log.e("VideoPlayer", "Media slug is null, cannot add bookmark");
            CustomToast.showWarning(this, "Не удалось определить аниме");
            return;
        }
        
        Log.d("VideoPlayer", "Adding bookmark with data:");
        Log.d("VideoPlayer", "  - mediaSlug: " + mediaSlug);
        Log.d("VideoPlayer", "  - episodeId: " + currentEpisode.getId());
        Log.d("VideoPlayer", "  - episodeNumber: " + currentEpisode.getNumber());
        Log.d("VideoPlayer", "  - teamId: " + (currentPlayer.getTeam() != null ? currentPlayer.getTeam().getId() : "null"));
        Log.d("VideoPlayer", "  - currentPosition: " + currentPosition + "ms");

        // Используем BookmarkManager для добавления закладки
        episodesManager.getBookmarkManager().addBookmark(
            mediaSlug,
            currentPlayer,
            currentEpisode,
            currentPosition,
            new BookmarkManager.BookmarkAddCallback() {
                @Override
                public void onBookmarkAdded(int episodeId) {
                    // Меняем цвет кнопки на красный
                    updateBookmarkButtonColor(true);
                    
                    // Обновляем список эпизодов
                    updateEpisodesListAfterBookmark();
                }
                
                @Override
                public void onBookmarkError(String error) {
                    // Кнопка остается белой при ошибке
                    updateBookmarkButtonColor(false);
                }
            },
            true // Показываем Toast при успехе для ручного добавления
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (player != null && wasPlayingBeforeBackground
                && player.getPlaybackState() != Player.STATE_ENDED) {
            player.play();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        autoSaveBookmark();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopBufferingMonitoring();

        if (orientationEventListener != null) {
            orientationEventListener.disable();
        }

        autoSaveBookmark();

        if (player != null) {
            wasPlayingBeforeBackground = player.getPlayWhenReady();
            player.pause();
        }
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            setupFullscreen();
        }
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        if (manualOrientationOverride && lastPhysicalOrientation == newConfig.orientation) {
            manualOrientationOverride = false;
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }

        setupFullscreen();
        checkAndUpdateOrientation();
    }

    private int getStatusBarHeight() {
        int sbHeight = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.view.WindowInsets insets = getWindow().getDecorView().getRootWindowInsets();
            if (insets != null) {
                sbHeight = insets.getInsets(android.view.WindowInsets.Type.statusBars()).top;
            }
        }
        if (sbHeight <= 0) {
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                sbHeight = getResources().getDimensionPixelSize(resourceId);
            }
        }
        return sbHeight;
    }

    private void applyRoundedCornersToTimeBar(View timeBarView) {
        if (timeBarView == null) return;
        try {
            float density = getResources().getDisplayMetrics().density;
            float radiusPx = 2f * density;
            android.graphics.CornerPathEffect pathEffect = new android.graphics.CornerPathEffect(radiusPx);

            Class<?> clazz = timeBarView.getClass();
            while (clazz != null && clazz != View.class) {
                java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
                for (java.lang.reflect.Field field : fields) {
                    if (android.graphics.Paint.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        android.graphics.Paint paint = (android.graphics.Paint) field.get(timeBarView);
                        if (paint != null) {
                            paint.setPathEffect(pathEffect);
                            paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
                            paint.setStrokeJoin(android.graphics.Paint.Join.ROUND);
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                timeBarView.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, android.graphics.Outline outline) {
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radiusPx);
                    }
                });
                timeBarView.setClipToOutline(true);
            }
            timeBarView.invalidate();
        } catch (Exception e) {
            Log.e("VideoPlayer", "Failed to round timebar corners", e);
        }
    }

    private void checkAndUpdateOrientation() {
        try {
            setupFullscreen();
            boolean isPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
            
            // Always force close landscape side panels and overlays on orientation change
            if (menuPanelContainer != null) {
                menuPanelContainer.forceClose();
                menuPanelContainer.setVisibility(View.GONE);
            }
            if (commentsPanelContainer != null) {
                commentsPanelContainer.forceClose();
                commentsPanelContainer.setVisibility(View.GONE);
            }
            if (menuOverlay != null) {
                menuOverlay.setVisibility(View.GONE);
                menuOverlay.setAlpha(0f);
            }
            if (animeInfoPlaceholder != null) {
                animeInfoPlaceholder.setVisibility(View.GONE);
            }
            if (slidingMenuPanel != null) {
                slidingMenuPanel.setVisibility(View.GONE);
            }
            if (commentsPanel != null) {
                commentsPanel.setVisibility(View.GONE);
            }
            if (commentsManager != null) {
                commentsManager.forceHideCommentsPanel();
            }
            if (playersManager != null) {
                playersManager.forceHideMenu();
            }
            if (episodesManager != null) {
                episodesManager.hideEpisodesMenu();
            }
            if (relatedTitlesManager != null) {
                relatedTitlesManager.hideRelatedTitles();
            }
            if (gesturesManager != null) {
                gesturesManager.hideAllGesturesUI();
            }
            if (currentSettingsBottomSheet != null && currentSettingsBottomSheet.isShowing()) {
                try {
                    currentSettingsBottomSheet.dismiss();
                } catch (Exception ignored) {}
            }

            applyPlayerSidePanelTransform(0f);
            if (playerContainer != null) {
                playerContainer.setPivotX(0f);
                playerContainer.setPivotY(0f);
                playerContainer.setScaleX(1f);
                playerContainer.setScaleY(1f);
                playerContainer.setTranslationX(0f);
                playerContainer.setTranslationY(0f);
            }

            int sbHeight = isPortrait ? getStatusBarHeight() : 0;

            View mainVideoLayout = findViewById(R.id.mainVideoLayout);
            if (mainVideoLayout != null) {
                mainVideoLayout.setPadding(0, sbHeight, 0, 0);
                if (mainVideoLayout instanceof android.view.ViewGroup) {
                    ((android.view.ViewGroup) mainVideoLayout).setClipChildren(false);
                    ((android.view.ViewGroup) mainVideoLayout).setClipToPadding(false);
                }
            }

            // 1. Adjust player container height and show/hide bottom empty area
            View portraitBottomContainer = findViewById(R.id.portraitBottomContainer);
            if (playerContainer != null) {
                playerContainer.setPadding(0, 0, 0, 0);
                playerContainer.setClipChildren(false);
                playerContainer.setClipToPadding(false);
                ViewCompat.setElevation(playerContainer, isPortrait ? 10f : 0f);

                android.view.ViewGroup.LayoutParams rawParams = playerContainer.getLayoutParams();
                if (rawParams != null) {
                    if (rawParams instanceof android.view.ViewGroup.MarginLayoutParams) {
                        android.view.ViewGroup.MarginLayoutParams mlp = (android.view.ViewGroup.MarginLayoutParams) rawParams;
                        mlp.topMargin = 0;
                        mlp.leftMargin = 0;
                        mlp.rightMargin = 0;
                        mlp.bottomMargin = 0;
                    }
                    if (isPortrait) {
                        DisplayMetrics dm = getResources().getDisplayMetrics();
                        int portraitWidth = Math.min(dm.widthPixels, dm.heightPixels);
                        rawParams.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                        rawParams.height = portraitWidth * 9 / 16;
                        if (rawParams instanceof android.widget.LinearLayout.LayoutParams) {
                            ((android.widget.LinearLayout.LayoutParams) rawParams).weight = 0;
                        }
                        if (portraitBottomContainer != null) {
                            portraitBottomContainer.setVisibility(View.VISIBLE);
                            ViewCompat.setElevation(portraitBottomContainer, 0f);
                            updatePortraitVoiceoverPlayerUI();
                            updatePortraitHeaderTitlesUI();
                            if (commentsManager != null) {
                                commentsManager.loadCommentsForPortraitIfNeeded();
                            }
                        }
                    } else {
                        rawParams.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                        rawParams.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                        if (rawParams instanceof android.widget.LinearLayout.LayoutParams) {
                            ((android.widget.LinearLayout.LayoutParams) rawParams).weight = 0;
                        }
                        if (portraitBottomContainer != null) {
                            portraitBottomContainer.setVisibility(View.GONE);
                        }
                    }
                    playerContainer.setLayoutParams(rawParams);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        playerContainer.invalidateOutline();
                    }
                }
            }

            if (playerView != null) {
                playerView.setClipChildren(false);
                playerView.setClipToPadding(false);
            }

            if (controllerView == null) {
                return;
            }

            if (controllerView instanceof android.view.ViewGroup) {
                ((android.view.ViewGroup) controllerView).setClipChildren(false);
                ((android.view.ViewGroup) controllerView).setClipToPadding(false);
            }

            // 2. Hide top anime info in portrait, keep close, settings, pip buttons visible
            View ibClosePlayer = controllerView.findViewById(R.id.ibClosePlayer);
            if (ibClosePlayer != null) ibClosePlayer.setVisibility(View.VISIBLE);

            View settingsButtonContainer = controllerView.findViewById(R.id.settingsButtonContainer);
            if (settingsButtonContainer != null) settingsButtonContainer.setVisibility(View.VISIBLE);
            if (settingsButton != null) settingsButton.setVisibility(View.VISIBLE);

            if (pipButton == null) pipButton = controllerView.findViewById(R.id.pipButton);
            if (pipButton != null) pipButton.setVisibility(View.VISIBLE);

            View animeInfoContainer = controllerView.findViewById(R.id.animeInfoContainer);
            if (animeInfoContainer != null) {
                animeInfoContainer.setVisibility(isPortrait ? View.GONE : View.VISIBLE);
            }

            View topBarContainer = controllerView.findViewById(R.id.topBarContainer);
            if (topBarContainer != null) {
                android.view.ViewGroup.LayoutParams lp = topBarContainer.getLayoutParams();
                if (lp instanceof android.widget.FrameLayout.LayoutParams) {
                    android.widget.FrameLayout.LayoutParams topParams = (android.widget.FrameLayout.LayoutParams) lp;
                    float density = getResources().getDisplayMetrics().density;
                    if (isPortrait) {
                        topParams.topMargin = (int) (10 * density);
                        topParams.leftMargin = (int) (10 * density);
                        topParams.rightMargin = (int) (10 * density);
                    } else {
                        topParams.topMargin = (int) (16 * density);
                        topParams.leftMargin = (int) (24 * density);
                        topParams.rightMargin = (int) (24 * density);
                    }
                    topBarContainer.setLayoutParams(topParams);
                }
            }

            // 3. Show center episode & play controls in both portrait and landscape
            View centerControlsContainer = controllerView.findViewById(R.id.centerControlsContainer);
            if (centerControlsContainer != null) {
                centerControlsContainer.setVisibility(View.VISIBLE);
            }

            // 4. Hide / show bottom buttons in portrait
            View skipForwardButton = controllerView.findViewById(R.id.skipForwardButton);
            View bookmarkBtn = controllerView.findViewById(R.id.bookmarkButton);
            View commentsBtn = controllerView.findViewById(R.id.commentsButton);
            View episodesMenuBtn = controllerView.findViewById(R.id.episodesMenuButton);
            View menuToggleBtn = controllerView.findViewById(R.id.menuToggleButton);
            View menuToggleFullscreenBtn = controllerView.findViewById(R.id.menuToggleFullscreen);

            if (skipForwardButton != null) skipForwardButton.setVisibility(isPortrait ? View.GONE : View.VISIBLE);
            if (bookmarkBtn != null) {
                bookmarkBtn.setVisibility(isPortrait ? View.GONE : View.VISIBLE);
                if (isOfflineMode) {
                    bookmarkBtn.setEnabled(true);
                    bookmarkBtn.setAlpha(1.0f);
                }
            }
            if (commentsBtn != null) {
                commentsBtn.setVisibility(isPortrait ? View.GONE : View.VISIBLE);
                if (isOfflineMode) {
                    commentsBtn.setEnabled(false);
                    commentsBtn.setAlpha(0.3f);
                }
            }
            if (episodesMenuBtn != null) episodesMenuBtn.setVisibility(isPortrait ? View.GONE : View.VISIBLE);
            if (menuToggleBtn != null) {
                menuToggleBtn.setVisibility(isPortrait ? View.GONE : View.VISIBLE);
                if (isOfflineMode) {
                    menuToggleBtn.setEnabled(false);
                    menuToggleBtn.setAlpha(0.3f);
                }
            }
            if (isOfflineMode) {
                if (portraitVoiceoverPlayerButton != null) {
                    portraitVoiceoverPlayerButton.setEnabled(false);
                    portraitVoiceoverPlayerButton.setClickable(false);
                    portraitVoiceoverPlayerButton.setFocusable(false);
                    portraitVoiceoverPlayerButton.setAlpha(0.6f);
                }
                if (ivPortraitVoiceoverChevron != null) {
                    ivPortraitVoiceoverChevron.setVisibility(View.GONE);
                }
                if (portraitBookmarkButton != null) {
                    portraitBookmarkButton.setEnabled(true);
                    portraitBookmarkButton.setClickable(true);
                    portraitBookmarkButton.setFocusable(true);
                    portraitBookmarkButton.setAlpha(1.0f);
                }
                if (portraitDownloadButton != null) {
                    portraitDownloadButton.setEnabled(false);
                    portraitDownloadButton.setClickable(false);
                    portraitDownloadButton.setFocusable(false);
                    portraitDownloadButton.setAlpha(0.35f);
                }
            }
            if (menuToggleFullscreenBtn != null) {
                menuToggleFullscreenBtn.setVisibility(View.VISIBLE);
                if (menuToggleFullscreenBtn instanceof ImageButton) {
                    ((ImageButton) menuToggleFullscreenBtn).setImageResource(isPortrait ? R.drawable.ic_fullscreen : R.drawable.ic_fullscreen_exit);
                }
            }
            if (episodesHorizontalRecyclerView != null) {
                if (isPortrait) {
                    episodesHorizontalRecyclerView.setVisibility(View.GONE);
                } else {
                    if (episodesManager != null && episodesManager.isEpisodesMenuVisible()) {
                        episodesHorizontalRecyclerView.setVisibility(View.VISIBLE);
                    } else {
                        episodesHorizontalRecyclerView.setVisibility(View.INVISIBLE);
                    }
                }
            }

            // 5. Adjust timebar position & margin
            android.widget.LinearLayout playersControlBarView = controllerView.findViewById(R.id.playersControlBar);
            View exoProgress = controllerView.findViewById(R.id.exo_progress);
            View timeAndControlsContainer = controllerView.findViewById(R.id.timeAndControlsContainer);
            View overlayBadgesContainer = controllerView.findViewById(R.id.overlayBadgesContainer);

            if (playersControlBarView != null && exoProgress != null && timeAndControlsContainer != null) {
                playersControlBarView.setClipChildren(false);
                playersControlBarView.setClipToPadding(false);

                if (overlayBadgesContainer != null && overlayBadgesContainer.getParent() instanceof android.view.ViewGroup) {
                    ((android.view.ViewGroup) overlayBadgesContainer.getParent()).removeView(overlayBadgesContainer);
                }
                if (exoProgress.getParent() instanceof android.view.ViewGroup) {
                    ((android.view.ViewGroup) exoProgress.getParent()).removeView(exoProgress);
                }
                if (timeAndControlsContainer.getParent() instanceof android.view.ViewGroup) {
                    ((android.view.ViewGroup) timeAndControlsContainer.getParent()).removeView(timeAndControlsContainer);
                }
                if (episodesHorizontalRecyclerView != null && episodesHorizontalRecyclerView.getParent() instanceof android.view.ViewGroup) {
                    ((android.view.ViewGroup) episodesHorizontalRecyclerView.getParent()).removeView(episodesHorizontalRecyclerView);
                }

                if (overlayBadgesContainer != null) {
                    android.view.ViewGroup.LayoutParams obLp = overlayBadgesContainer.getLayoutParams();
                    android.widget.LinearLayout.LayoutParams obParams = (obLp instanceof android.widget.LinearLayout.LayoutParams) ?
                            (android.widget.LinearLayout.LayoutParams) obLp :
                            new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                    float d = getResources().getDisplayMetrics().density;
                    obParams.leftMargin = (int) ((isPortrait ? 16 : 24) * d);
                    obParams.rightMargin = (int) ((isPortrait ? 16 : 24) * d);
                    obParams.bottomMargin = (int) (6 * d);
                    overlayBadgesContainer.setLayoutParams(obParams);
                }

                android.view.ViewGroup.LayoutParams tcLp = timeAndControlsContainer.getLayoutParams();
                android.widget.LinearLayout.LayoutParams timeAndControlsParams = (tcLp instanceof android.widget.LinearLayout.LayoutParams) ?
                        (android.widget.LinearLayout.LayoutParams) tcLp :
                        new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);

                android.view.ViewGroup.LayoutParams exLp = exoProgress.getLayoutParams();
                android.widget.LinearLayout.LayoutParams progressParams = (exLp instanceof android.widget.LinearLayout.LayoutParams) ?
                        (android.widget.LinearLayout.LayoutParams) exLp :
                        new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);

                float density = getResources().getDisplayMetrics().density;

                if (isPortrait) {
                    // Reset translationY to 0 so timebar and timecode are fully visible
                    playersControlBarView.setTranslationY(0f);

                    // Align timeAndControls (16dp) with exoProgress (8dp + 8dp internal padding = 16dp)
                    timeAndControlsParams.topMargin = (int) (2 * density);
                    timeAndControlsParams.bottomMargin = (int) (2 * density);
                    timeAndControlsParams.leftMargin = (int) (16 * density);
                    timeAndControlsParams.rightMargin = (int) (16 * density);

                    progressParams.topMargin = (int) (2 * density);
                    progressParams.bottomMargin = (int) (12 * density);
                    progressParams.leftMargin = (int) (8 * density);
                    progressParams.rightMargin = (int) (8 * density);

                    timeAndControlsContainer.setLayoutParams(timeAndControlsParams);
                    exoProgress.setLayoutParams(progressParams);

                    exoProgress.setTranslationY(0f);
                    applyRoundedCornersToTimeBar(exoProgress);

                    if (overlayBadgesContainer != null) {
                        playersControlBarView.addView(overlayBadgesContainer);
                    }
                    playersControlBarView.addView(timeAndControlsContainer);
                    playersControlBarView.addView(exoProgress);
                    if (episodesHorizontalRecyclerView != null) {
                        playersControlBarView.addView(episodesHorizontalRecyclerView);
                    }
                } else {
                    // Landscape: timeAndControlsContainer on top, exoProgress at bottom (same order as portrait)
                    if (episodesManager != null && episodesManager.isEpisodesMenuVisible()) {
                        playersControlBarView.setTranslationY(0f);
                    } else {
                        playersControlBarView.setTranslationY(60f * density);
                    }

                    // Align timeAndControls (24dp) with exoProgress (16dp + 8dp internal padding = 24dp)
                    timeAndControlsParams.topMargin = (int) (2 * density);
                    timeAndControlsParams.bottomMargin = (int) (2 * density);
                    timeAndControlsParams.leftMargin = (int) (24 * density);
                    timeAndControlsParams.rightMargin = (int) (24 * density);

                    progressParams.topMargin = (int) (2 * density);
                    progressParams.bottomMargin = (int) (14 * density);
                    progressParams.leftMargin = (int) (16 * density);
                    progressParams.rightMargin = (int) (16 * density);

                    timeAndControlsContainer.setLayoutParams(timeAndControlsParams);
                    exoProgress.setLayoutParams(progressParams);

                    exoProgress.setTranslationY(0f);
                    applyRoundedCornersToTimeBar(exoProgress);

                    if (overlayBadgesContainer != null) {
                        playersControlBarView.addView(overlayBadgesContainer);
                    }
                    playersControlBarView.addView(timeAndControlsContainer);
                    playersControlBarView.addView(exoProgress);
                    if (episodesHorizontalRecyclerView != null) {
                        playersControlBarView.addView(episodesHorizontalRecyclerView);
                    }
                }

                playersControlBarView.setVisibility(View.VISIBLE);
                exoProgress.setVisibility(View.VISIBLE);
                timeAndControlsContainer.setVisibility(View.VISIBLE);

                View timeCodeContainer = controllerView.findViewById(R.id.timeCodeContainer);
                if (timeCodeContainer != null) timeCodeContainer.setVisibility(View.VISIBLE);
            }

            // 6. Block drag events on side panels, top curtain, episode carousel (Task 3)
            if (menuPanelContainer != null) menuPanelContainer.setDragEnabled(!isPortrait);
            if (commentsPanelContainer != null) commentsPanelContainer.setDragEnabled(!isPortrait);
            if (gesturesManager != null) gesturesManager.setPortraitMode(isPortrait);
            if (verticalGesturesManager != null) verticalGesturesManager.setPortraitMode(isPortrait);

            if (DownloadService.isRunning()) {
                showDownloadProgress(DownloadService.getCurrentTaskPercent());
            } else {
                resetDownloadUi();
            }
        } catch (Exception e) {
            Log.e("VideoPlayer", "Error in checkAndUpdateOrientation", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (orientationEventListener != null && orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable();
        }

        setupFullscreen();
        startBufferingMonitoring();

        // Восстанавливаем плашку прогресса при возврате в плеер
        if (DownloadService.isRunning()) {
            showDownloadProgress(DownloadService.getCurrentTaskPercent());
        } else {
            resetDownloadUi();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopViewProgressTracking();
        stopBufferingMonitoring();
        
        if (orientationEventListener != null) {
            orientationEventListener.disable();
            orientationEventListener = null;
        }

        // Автоматически сохраняем закладку при закрытии плеера
        autoSaveBookmark();
        
        // Clear screen keep flag
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (currentErrorDialog != null && currentErrorDialog.isShowing()) {
            try {
                currentErrorDialog.dismiss();
            } catch (Exception ignored) {}
            currentErrorDialog = null;
        }

        if (player != null) {
            player.release();
            player = null;
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        if (apiService != null) {
            apiService.shutdown();
        }
        if (commentsManager != null) {
            commentsManager.cleanup();
        }
        if (playersManager != null) {
            playersManager.cleanup();
        }
        if (gesturesManager != null) {
            gesturesManager.cleanup();
        }
        if (timecodeManager != null) {
            timecodeManager.cleanup();
        }
        if (ambientLightManager != null) {
            ambientLightManager.cleanup();
        }
        if (surroundSoundManager != null) {
            surroundSoundManager.release();
            surroundSoundManager = null;
        }
        if (pipBroadcastReceiver != null) {
            try {
                unregisterReceiver(pipBroadcastReceiver);
            } catch (Exception ignored) {}
            pipBroadcastReceiver = null;
        }

        DownloadService.setListener(null);
        if (btnDownloadFromMenu != null) com.example.animelib.util.DownloadAnimationUtils.stopDownloadAnimation(btnDownloadFromMenu);

        if (nextEpisodeHandler != null && nextEpisodeRunnable != null) {
            nextEpisodeHandler.removeCallbacks(nextEpisodeRunnable);
        }
    }

    @Override
    public void onBackPressed() {
        if (player != null && player.isPlaying() && !isInPictureInPictureMode) {
            autoSaveBookmark();
            enterPictureInPictureMode();
            return;
        }
        autoSaveBookmark();

        super.onBackPressed();
    }
}


