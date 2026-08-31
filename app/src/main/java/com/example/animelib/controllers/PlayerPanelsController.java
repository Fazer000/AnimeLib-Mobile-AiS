package com.example.animelib.controllers;

import android.app.Activity;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.core.view.ViewCompat;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.example.animelib.R;
import com.example.animelib.managers.CommentsManager;
import com.example.animelib.managers.PlayersManager;
import com.example.animelib.ui.AmbientVignetteOverlayView;
import com.example.animelib.ui.DraggableSidePanel;

public class PlayerPanelsController {

    private final Activity activity;
    private final PanelsCallback callback;

    private DraggableSidePanel menuPanelContainer;
    private DraggableSidePanel commentsPanelContainer;
    private View slidingMenuPanel;
    private View commentsPanel;
    private View menuOverlay;
    private ViewGroup playerContainer;

    public interface PanelsCallback {
        ExoPlayer getPlayer();
        PlayerView getPlayerView();
        PlayersManager getPlayersManager();
        CommentsManager getCommentsManager();
        boolean isOfflineMode();
        int getStatusBarHeight();
        void updateAmbientPlayerTransform(float scale, float translationX, float translationY, boolean isCropped);
        void onOutlineValuesChanged(float left, float top, float right, float bottom, float radiusPx);
    }

    public PlayerPanelsController(Activity activity, PanelsCallback callback) {
        this.activity = activity;
        this.callback = callback;
    }

    public void initViews(DraggableSidePanel menuPanelContainer,
                          DraggableSidePanel commentsPanelContainer,
                          View slidingMenuPanel,
                          View commentsPanel,
                          View menuOverlay,
                          ViewGroup playerContainer) {
        this.menuPanelContainer = menuPanelContainer;
        this.commentsPanelContainer = commentsPanelContainer;
        this.slidingMenuPanel = slidingMenuPanel;
        this.commentsPanel = commentsPanel;
        this.menuOverlay = menuOverlay;
        this.playerContainer = playerContainer;

        setupDraggablePanels();
    }

    public void openMenuPanel() {
        if (callback.isOfflineMode()) return;
        PlayerView pv = callback.getPlayerView();
        if (pv != null) {
            pv.hideController();
        }
        if (slidingMenuPanel != null) {
            slidingMenuPanel.setVisibility(View.VISIBLE);
        }
        if (menuPanelContainer != null) {
            menuPanelContainer.openPanel();
        }
    }

    public void closeMenuPanel() {
        if (menuPanelContainer != null) {
            menuPanelContainer.closePanel();
        }
    }

    public void openCommentsPanel() {
        if (callback.isOfflineMode()) return;
        PlayerView pv = callback.getPlayerView();
        if (pv != null) {
            pv.hideController();
        }
        if (commentsPanel != null) {
            commentsPanel.setVisibility(View.VISIBLE);
        }
        if (commentsPanelContainer != null) {
            commentsPanelContainer.openPanel();
        }
    }

    public void closeCommentsPanel() {
        if (commentsPanelContainer != null) {
            commentsPanelContainer.closePanel();
        }
    }

    public boolean isMenuPanelOpen() {
        return menuPanelContainer != null && menuPanelContainer.isOpen();
    }

    public boolean isCommentsPanelOpen() {
        return commentsPanelContainer != null && commentsPanelContainer.isOpen();
    }

    private void setupDraggablePanels() {
        if (menuPanelContainer != null) {
            menuPanelContainer.setOnPanelStateChangeListener(new DraggableSidePanel.OnPanelStateChangeListener() {
                @Override
                public void onPanelDragStart() {
                    PlayersManager pm = callback.getPlayersManager();
                    if (pm != null) {
                        pm.onPanelDragStart();
                    }
                }

                @Override
                public void onPanelOpened() {
                    applyPlayerSidePanelTransform(1f);
                    if (menuOverlay != null) {
                        menuOverlay.setVisibility(View.VISIBLE);
                    }
                    PlayersManager pm = callback.getPlayersManager();
                    if (pm != null) {
                        pm.onPanelOpened();
                    }
                }

                @Override
                public void onPanelClosed() {
                    applyPlayerSidePanelTransform(0f);
                    if (menuOverlay != null) {
                        menuOverlay.setVisibility(View.GONE);
                    }
                    PlayersManager pm = callback.getPlayersManager();
                    if (pm != null) {
                        pm.onPanelClosedByDrag();
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
                    if (callback.isOfflineMode()) {
                        return true;
                    }
                    PlayersManager pm = callback.getPlayersManager();
                    if (pm != null && pm.getCurrentPlayerData() == null) {
                        return false;
                    }
                    return true;
                }
            });
        }

        if (commentsPanelContainer != null) {
            commentsPanelContainer.setOnPanelStateChangeListener(new DraggableSidePanel.OnPanelStateChangeListener() {
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
                    CommentsManager cm = callback.getCommentsManager();
                    if (cm != null) {
                        cm.onPanelClosedByDrag();
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

    public void applyPlayerSidePanelTransform(float openProgress) {
        openProgress = Math.max(0f, Math.min(1f, openProgress));

        if (playerContainer == null) {
            return;
        }

        ExoPlayer player = callback.getPlayer();
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        View decorView = activity.getWindow().getDecorView();
        int screenW = (decorView != null && decorView.getWidth() > 0) ? decorView.getWidth() : dm.widthPixels;
        int screenH = (decorView != null && decorView.getHeight() > 0) ? decorView.getHeight() : dm.heightPixels;
        boolean isPortrait = (activity.getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) || (screenH > screenW);
        ViewCompat.setElevation(playerContainer, isPortrait ? 10f : 0f);

        if (isPortrait) {
            playerContainer.setPivotX(0f);
            playerContainer.setPivotY(0f);
            playerContainer.setScaleX(1f);
            playerContainer.setScaleY(1f);
            playerContainer.setTranslationX(0f);
            playerContainer.setTranslationY(0f);
            callback.onOutlineValuesChanged(0f, 0f, 0f, 0f, 0f);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                playerContainer.invalidateOutline();
            }
            View ambientContainer = activity.findViewById(R.id.ambientContainer);
            int sbHeight = callback.getStatusBarHeight();
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

            int sw = activity.getResources().getDisplayMetrics().widthPixels;
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

            AmbientVignetteOverlayView ambientVignetteOverlay = activity.findViewById(R.id.ambientVignetteOverlay);
            if (ambientVignetteOverlay != null) {
                ambientVignetteOverlay.setVideoBounds(portLeft, portTop, portLeft + portW, portTop + portH);
            }
            callback.updateAmbientPlayerTransform(1f, 0f, 0f, true);
            ViewGroup.LayoutParams lp = playerContainer.getLayoutParams();
            if (lp != null) {
                int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
                int targetH = screenWidth * 9 / 16;
                if (lp.height != targetH) {
                    lp.height = targetH;
                    if (lp instanceof LinearLayout.LayoutParams) {
                        ((LinearLayout.LayoutParams) lp).weight = 0;
                    }
                    playerContainer.setLayoutParams(lp);
                }
            }
            return;
        }

        // Landscape mode
        boolean isMenuVisible = menuPanelContainer != null && menuPanelContainer.getVisibility() == View.VISIBLE;
        boolean isCommentsVisible = commentsPanelContainer != null && commentsPanelContainer.getVisibility() == View.VISIBLE;
        if (!isMenuVisible && !isCommentsVisible) {
            openProgress = 0f;
        }

        if (openProgress <= 0f) {
            playerContainer.setPivotX(0f);
            playerContainer.setPivotY(0f);
            playerContainer.setScaleX(1f);
            playerContainer.setScaleY(1f);
            playerContainer.setTranslationX(0f);
            playerContainer.setTranslationY(0f);
            callback.onOutlineValuesChanged(0f, 0f, 0f, 0f, 0f);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                playerContainer.invalidateOutline();
            }
            int sw = 0;
            int sh = 0;
            if (decorView != null && decorView.getWidth() > 0 && decorView.getHeight() > 0) {
                sw = decorView.getWidth();
                sh = decorView.getHeight();
            } else {
                sw = dm.widthPixels;
                sh = dm.heightPixels;
            }
            AmbientVignetteOverlayView ambientVignetteOverlay = activity.findViewById(R.id.ambientVignetteOverlay);
            if (ambientVignetteOverlay != null) {
                ambientVignetteOverlay.clearCustomVideoBounds();
            }
            callback.updateAmbientPlayerTransform(1f, 0f, 0f, false);
            ViewGroup.LayoutParams rawLp = playerContainer.getLayoutParams();
            if (rawLp != null) {
                if (rawLp.width != ViewGroup.LayoutParams.MATCH_PARENT ||
                        rawLp.height != ViewGroup.LayoutParams.MATCH_PARENT) {
                    rawLp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    rawLp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    playerContainer.setLayoutParams(rawLp);
                }
            }
            return;
        }

        int screenWidth = 0;
        int screenHeight = 0;
        if (decorView != null && decorView.getWidth() > 0 && decorView.getHeight() > 0) {
            screenWidth = decorView.getWidth();
            screenHeight = decorView.getHeight();
        } else {
            screenWidth = dm.widthPixels;
            screenHeight = dm.heightPixels;
        }

        if (screenWidth <= 0 || screenHeight <= 0) return;

        ViewGroup.LayoutParams rawLp = playerContainer.getLayoutParams();
        if (rawLp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) rawLp;
            if (mlp.width != ViewGroup.LayoutParams.MATCH_PARENT ||
                    mlp.height != ViewGroup.LayoutParams.MATCH_PARENT ||
                    mlp.leftMargin != 0 || mlp.topMargin != 0) {
                mlp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                mlp.height = ViewGroup.LayoutParams.MATCH_PARENT;
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
            if (rawLp.width != ViewGroup.LayoutParams.MATCH_PARENT ||
                    rawLp.height != ViewGroup.LayoutParams.MATCH_PARENT) {
                rawLp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                rawLp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                playerContainer.setLayoutParams(rawLp);
            }
        }

        float videoAspect = 16f / 9f;
        if (player != null && player.getVideoSize() != null) {
            int vWidth = player.getVideoSize().width;
            int vHeight = player.getVideoSize().height;
            if (vWidth > 0 && vHeight > 0) {
                videoAspect = (float) vWidth / (float) vHeight;
            }
        }

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

        float density = activity.getResources().getDisplayMetrics().density;
        float panelWidthPx = 360f * density;

        DraggableSidePanel activePanel = null;
        if (commentsPanelContainer != null && commentsPanelContainer.getVisibility() == View.VISIBLE) {
            activePanel = commentsPanelContainer;
        } else if (menuPanelContainer != null && menuPanelContainer.getVisibility() == View.VISIBLE) {
            activePanel = menuPanelContainer;
        }

        if (activePanel != null && activePanel.getChildCount() > 0) {
            int w = activePanel.getChildAt(0).getWidth();
            if (w > 0) panelWidthPx = w;
        }

        float sideMarginPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, activity.getResources().getDisplayMetrics());

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

        float currentVideoW = videoW0 + (videoW1 - videoW0) * openProgress;
        float currentVideoH = videoH0 + (videoH1 - videoH0) * openProgress;
        float currentLeft = videoLeft0 + (videoLeft1 - videoLeft0) * openProgress;
        float currentTop = videoTop0 + (videoTop1 - videoTop0) * openProgress;

        float scale = (videoW0 > 0) ? (currentVideoW / videoW0) : 1f;

        float translationX = currentLeft - (videoLeft0 * scale);
        float translationY = currentTop - (videoTop0 * scale);

        playerContainer.setPivotX(0f);
        playerContainer.setPivotY(0f);
        playerContainer.setScaleX(scale);
        playerContainer.setScaleY(scale);
        playerContainer.setTranslationX(translationX);
        playerContainer.setTranslationY(translationY);

        float outlineLeft = videoLeft0 * openProgress;
        float outlineTop = videoTop0 * openProgress;
        float outlineRight = screenWidth + (videoLeft0 + videoW0 - screenWidth) * openProgress;
        float outlineBottom = screenHeight + (videoTop0 + videoH0 - screenHeight) * openProgress;

        float maxRadiusPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, activity.getResources().getDisplayMetrics());
        float cornerRadiusPx = (scale > 0) ? (maxRadiusPx * openProgress / scale) : 0f;
        callback.onOutlineValuesChanged(outlineLeft, outlineTop, outlineRight, outlineBottom, cornerRadiusPx);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            playerContainer.invalidateOutline();
        }

        AmbientVignetteOverlayView ambientVignetteOverlay = activity.findViewById(R.id.ambientVignetteOverlay);
        if (ambientVignetteOverlay != null) {
            ambientVignetteOverlay.clearCustomVideoBounds();
        }
        callback.updateAmbientPlayerTransform(1f, 0f, 0f, false);
    }
}
