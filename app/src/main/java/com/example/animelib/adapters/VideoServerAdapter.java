package com.example.animelib.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.R;

import java.util.List;

public class VideoServerAdapter extends RecyclerView.Adapter<VideoServerAdapter.ServerViewHolder> {

    public interface OnServerSelectedListener {
        void onServerSelected(String domain);
    }

    public static class ServerOption {
        public final String domain;
        public final String name;
        public final String url;

        public ServerOption(String domain, String name, String url) {
            this.domain = domain;
            this.name = name;
            this.url = url;
        }
    }

    private final List<ServerOption> servers;
    private String currentDomain;
    private final OnServerSelectedListener listener;

    public VideoServerAdapter(List<ServerOption> servers, String currentDomain, OnServerSelectedListener listener) {
        this.servers = servers;
        this.currentDomain = currentDomain;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ServerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video_server_option, parent, false);
        return new ServerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServerViewHolder holder, int position) {
        ServerOption option = servers.get(position);
        holder.serverNameText.setText(option.name);

        boolean isCurrent = option.domain.equalsIgnoreCase(currentDomain);
        holder.currentIndicator.setVisibility(isCurrent ? View.VISIBLE : View.GONE);
        holder.itemView.setSelected(isCurrent);

        TypedValue typedValue = new TypedValue();
        Context context = holder.itemView.getContext();

        if (isCurrent) {
            context.getTheme().resolveAttribute(R.attr.secondaryTextColor, typedValue, true);
            holder.serverNameText.setTextColor(typedValue.data);
        } else {
            context.getTheme().resolveAttribute(R.attr.primaryTextColor, typedValue, true);
            holder.serverNameText.setTextColor(typedValue.data);
        }

        holder.itemView.setScaleX(1.0f);
        holder.itemView.setScaleY(1.0f);
        com.example.animelib.util.ItemAnimationUtils.animateItemStateTransition(holder.itemView, isCurrent);

        holder.itemView.setOnClickListener(v -> {
            com.example.animelib.util.ItemAnimationUtils.animateItemClick(v, () -> {
                if (listener != null) {
                    listener.onServerSelected(option.domain);
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return servers.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateCurrentDomain(String newDomain) {
        this.currentDomain = newDomain;
        notifyDataSetChanged();
    }

    public static class ServerViewHolder extends RecyclerView.ViewHolder {
        TextView serverNameText;
        ImageView currentIndicator;

        ServerViewHolder(@NonNull View itemView) {
            super(itemView);
            serverNameText = itemView.findViewById(R.id.serverNameText);
            currentIndicator = itemView.findViewById(R.id.currentIndicator);
        }
    }
}
