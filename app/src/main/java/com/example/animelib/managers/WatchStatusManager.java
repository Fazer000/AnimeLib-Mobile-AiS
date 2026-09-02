package com.example.animelib.managers;

import com.example.animelib.models.WatchStatusItem;

import java.util.Arrays;
import java.util.List;

public class WatchStatusManager {

    public static final List<WatchStatusItem> STATUS_ITEMS = Arrays.asList(
            new WatchStatusItem("Смотрю", 21, "#ff9b40"),
            new WatchStatusItem("Запланировано", 22, "#2196f3"),
            new WatchStatusItem("Брошено", 23, "#f3382a"),
            new WatchStatusItem("Просмотрено", 24, "#3cce7b"),
            new WatchStatusItem("Любимые", 25, "#ff6666"),
            new WatchStatusItem("Пересматриваю", 26, "#ff6666"),
            new WatchStatusItem("Отложено", 27, "#ff6666"),
            new WatchStatusItem("Другое", "other", "#888888")
    );

    public static WatchStatusItem getStatusById(Object id) {
        if (id == null) return null;
        for (WatchStatusItem item : STATUS_ITEMS) {
            if (item.getId().equals(id)) {
                return item;
            }
            if (id instanceof Number && item.getId() instanceof Number) {
                if (((Number) id).intValue() == ((Number) item.getId()).intValue()) {
                    return item;
                }
            }
            if (id instanceof String && item.getId() instanceof String) {
                if (((String) id).equalsIgnoreCase((String) item.getId())) {
                    return item;
                }
            }
        }
        return null;
    }
}
