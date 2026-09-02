package com.example.animelib.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.R;
import com.example.animelib.models.WatchStatusItem;

import java.util.List;

public class WatchStatusAdapter extends RecyclerView.Adapter<WatchStatusAdapter.ViewHolder> {

    public interface OnStatusClickListener {
        void onStatusClick(WatchStatusItem item);
    }

    private final List<WatchStatusItem> items;
    private final Object currentStatusId;
    private final OnStatusClickListener listener;

    public WatchStatusAdapter(List<WatchStatusItem> items, Object currentStatusId, OnStatusClickListener listener) {
        this.items = items;
        this.currentStatusId = currentStatusId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_watch_status, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WatchStatusItem item = items.get(position);
        holder.tvStatusLabel.setText(item.getLabel());

        boolean isSelected = isSameStatus(currentStatusId, item.getId());
        holder.ivStatusSelectedCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStatusClick(item);
            }
        });
    }

    private boolean isSameStatus(Object id1, Object id2) {
        if (id1 == null || id2 == null) return false;
        if (id1.equals(id2)) return true;
        if (id1 instanceof Number && id2 instanceof Number) {
            return ((Number) id1).intValue() == ((Number) id2).intValue();
        }
        if (id1 instanceof String && id2 instanceof String) {
            return ((String) id1).equalsIgnoreCase((String) id2);
        }
        return false;
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStatusLabel;
        ImageView ivStatusSelectedCheck;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStatusLabel = itemView.findViewById(R.id.tvStatusLabel);
            ivStatusSelectedCheck = itemView.findViewById(R.id.ivStatusSelectedCheck);
        }
    }
}
