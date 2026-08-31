package com.example.animelib.controllers;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.animelib.R;
import com.example.animelib.managers.PlayersManager;
import com.example.animelib.models.EpisodeResponse;
import com.example.animelib.ui.DownloadsActivity;
import com.google.android.material.button.MaterialButton;

/**
 * Контроллер для отображения и управления диалоговыми окнами и BottomSheet в плеере.
 */
public class PlayerDialogsController {

    private final Activity activity;
    private AlertDialog currentErrorDialog;

    public interface DialogCallback {
        PlayersManager getPlayersManager();
        String getPreferredQuality();
        void hideLoading();
        void showLoading(String message);
        void safeRunOnUiThread(Runnable runnable);
    }

    private DialogCallback callback;

    public PlayerDialogsController(Activity activity) {
        this.activity = activity;
    }

    public void setCallback(DialogCallback callback) {
        this.callback = callback;
    }

    public void dismissErrorDialog() {
        if (currentErrorDialog != null && currentErrorDialog.isShowing()) {
            try {
                currentErrorDialog.dismiss();
            } catch (Exception ignored) {}
            currentErrorDialog = null;
        }
    }

    public void showVideoErrorDialog(String title, String message, Runnable retryAction) {
        showVideoErrorDialog(title, message, retryAction, false);
    }

    public void showVideoErrorDialog(String title, String message, Runnable retryAction, boolean isVoiceoverError) {
        if (activity == null || activity.isFinishing()) return;

        Runnable action = () -> {
            dismissErrorDialog();

            if (callback != null) {
                callback.hideLoading();
            }

            LayoutInflater inflater = activity.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_video_error, null);
            TextView titleTv = dialogView.findViewById(R.id.errorTitleText);
            TextView messageTv = dialogView.findViewById(R.id.errorMessageText);
            TextView detailsTv = dialogView.findViewById(R.id.errorPlayerDetailsText);
            MaterialButton retryBtn = dialogView.findViewById(R.id.retryButton);
            MaterialButton goToDownloadsBtn = dialogView.findViewById(R.id.goToDownloadsButton);
            MaterialButton exitBtn = dialogView.findViewById(R.id.exitButton);
            ImageButton closeCrossBtn = dialogView.findViewById(R.id.closeErrorCrossButton);

            if (goToDownloadsBtn != null) {
                goToDownloadsBtn.setOnClickListener(v -> {
                    dismissErrorDialog();
                    DownloadsActivity.start(activity);
                });
            }

            if (titleTv != null) {
                titleTv.setText((title != null && !title.isEmpty()) ? title : "Ошибка загрузки видео");
            }

            if (messageTv != null) {
                messageTv.setText(message != null ? message : "Произошла ошибка при загрузке видео.");
            }

            PlayersManager pm = callback != null ? callback.getPlayersManager() : null;
            EpisodeResponse.PlayerData playerData = pm != null ? pm.getCurrentPlayerData() : null;
            if (playerData != null && !isVoiceoverError && detailsTv != null) {
                String pName = playerData.getPlayer() != null ? playerData.getPlayer() : "Неизвестный";
                String tName = playerData.getTeam() != null ? playerData.getTeam().getName() : "";
                String qName = callback != null ? callback.getPreferredQuality() : "";
                StringBuilder details = new StringBuilder("Плеер: ").append(pName);
                if (tName != null && !tName.isEmpty()) {
                    details.append(" (").append(tName).append(")");
                }
                if (qName != null && !qName.isEmpty()) {
                    details.append(" • ").append(qName);
                }
                detailsTv.setText(details.toString());
                detailsTv.setVisibility(View.VISIBLE);
            } else if (detailsTv != null) {
                detailsTv.setVisibility(View.GONE);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setView(dialogView);
            currentErrorDialog = builder.create();

            if (currentErrorDialog.getWindow() != null) {
                currentErrorDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            if (retryBtn != null) {
                retryBtn.setOnClickListener(v -> {
                    dismissErrorDialog();
                    if (retryAction != null) {
                        if (callback != null) callback.showLoading("Повторная попытка...");
                        retryAction.run();
                    }
                });
            }

            if (exitBtn != null) {
                exitBtn.setOnClickListener(v -> {
                    dismissErrorDialog();
                    activity.finish();
                });
            }

            if (closeCrossBtn != null) {
                closeCrossBtn.setOnClickListener(v -> dismissErrorDialog());
            }

            currentErrorDialog.show();
        };

        if (callback != null) {
            callback.safeRunOnUiThread(action);
        } else {
            activity.runOnUiThread(action);
        }
    }
}
