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
        if (holder.serverSubtitleText != null) {
            holder.serverSubtitleText.setText(option.url != null ? option.url : "Сервер трансляции");
        }

        int totalCount = getItemCount();
        if (holder.itemContainer != null) {
            if (totalCount <= 1) {
                holder.itemContainer.setBackgroundResource(R.drawable.bg_m3_item_single);
            } else if (position == 0) {
                holder.itemContainer.setBackgroundResource(R.drawable.bg_m3_item_top);
            } else if (position == totalCount - 1) {
                holder.itemContainer.setBackgroundResource(R.drawable.bg_m3_item_bottom);
            } else {
                holder.itemContainer.setBackgroundResource(R.drawable.bg_m3_item_middle);
            }
        }

        boolean isCurrent = option.domain.equalsIgnoreCase(currentDomain);
        if (holder.selectedPill != null) {
            holder.selectedPill.setVisibility(isCurrent ? View.VISIBLE : View.GONE);
        }
        if (holder.unselectedIndicator != null) {
            holder.unselectedIndicator.setVisibility(isCurrent ? View.GONE : View.VISIBLE);
        }

        Context context = holder.itemView.getContext();
        TypedValue primaryColorVal = new TypedValue();
        TypedValue secondaryColorVal = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.primaryTextColor, primaryColorVal, true);
        context.getTheme().resolveAttribute(R.attr.secondaryColor, secondaryColorVal, true);

        holder.serverNameText.setTextColor(isCurrent ? secondaryColorVal.data : primaryColorVal.data);
        holder.itemView.setSelected(isCurrent);

        View.OnClickListener clickListener = v -> {
            com.example.animelib.util.ItemAnimationUtils.animateItemClick(v, () -> {
                if (listener != null) {
                    listener.onServerSelected(option.domain);
                }
            });
        };
        holder.itemView.setOnClickListener(clickListener);
        if (holder.itemContainer != null) {
            holder.itemContainer.setOnClickListener(clickListener);
        }
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
        View itemContainer;
        TextView serverNameText;
        TextView serverSubtitleText;
        View selectedPill;
        View unselectedIndicator;

        ServerViewHolder(@NonNull View itemView) {
            super(itemView);
            itemContainer = itemView.findViewById(R.id.itemContainer);
            serverNameText = itemView.findViewById(R.id.serverNameText);
            serverSubtitleText = itemView.findViewById(R.id.serverSubtitleText);
            selectedPill = itemView.findViewById(R.id.selectedPill);
            unselectedIndicator = itemView.findViewById(R.id.unselectedIndicator);
        }
    }
}
