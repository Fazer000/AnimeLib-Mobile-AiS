package com.example.animelib.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.webkit.WebView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class LatestViewsManager {
    private static final String TAG = "LatestViewsManager";
    private static final String PREF_NAME = "latest_views_pref";
    private static final String KEY_LATEST_VIEWS = "latest_views_json";

    private static String cachedJson = null;

    /**
     * Сохраняет или обновляет объект аниме в latest-views.
     * Если аниме с таким media.id или media.slug_url уже есть, оно заменяется, иначе добавляется в начало.
     */
    public static synchronized void saveLatestView(Context context, JsonObject newEntry) {
        if (context == null || newEntry == null) return;
        try {
            SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String existingJson = prefs.getString(KEY_LATEST_VIEWS, "[]");

            JsonArray array;
            try {
                JsonElement elem = JsonParser.parseString(existingJson);
                if (elem != null && elem.isJsonArray()) {
                    array = elem.getAsJsonArray();
                } else {
                    array = new JsonArray();
                }
            } catch (Exception e) {
                array = new JsonArray();
            }

            int newMediaId = getMediaId(newEntry);
            String newSlugUrl = getMediaSlugUrl(newEntry);

            int matchIndex = -1;
            for (int i = 0; i < array.size(); i++) {
                JsonElement el = array.get(i);
                if (el != null && el.isJsonObject()) {
                    JsonObject item = el.getAsJsonObject();
                    int existingId = getMediaId(item);
                    String existingSlug = getMediaSlugUrl(item);

                    if ((newMediaId > 0 && existingId == newMediaId) ||
                        (!newSlugUrl.isEmpty() && newSlugUrl.equalsIgnoreCase(existingSlug))) {
                        matchIndex = i;
                        break;
                    }
                }
            }

            if (matchIndex != -1) {
                array.remove(matchIndex);
            }

            // Создаём новый массив, помещая новую запись на первое место (unshift)
            JsonArray updatedArray = new JsonArray();
            updatedArray.add(newEntry);
            for (int i = 0; i < array.size(); i++) {
                if (updatedArray.size() < 50) { // Ограничиваем список 50 элементами
                    updatedArray.add(array.get(i));
                }
            }

            String updatedJson = updatedArray.toString();
            prefs.edit().putString(KEY_LATEST_VIEWS, updatedJson).apply();
            cachedJson = updatedJson;

            Log.d(TAG, "Saved latest-view entry successfully. Total items in list: " + updatedArray.size());
        } catch (Exception e) {
            Log.e(TAG, "Failed to save latest view", e);
        }
    }

    /**
     * Обновляет локальное хранилище из JavaScript (если сайт сам изменил latest-views)
     */
    public static synchronized void updateFromJS(Context context, String jsonFromJS) {
        if (context == null || jsonFromJS == null || jsonFromJS.trim().isEmpty() || "null".equals(jsonFromJS)) return;
        try {
            SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_LATEST_VIEWS, jsonFromJS.trim()).apply();
            cachedJson = jsonFromJS.trim();
            Log.d(TAG, "Updated latest-views from JS");
        } catch (Exception e) {
            Log.e(TAG, "Failed to update latest-views from JS", e);
        }
    }

    /**
     * Возвращает сохранённую строку JSON массива latest-views
     */
    public static synchronized String getLatestViewsJson(Context context) {
        if (cachedJson != null) return cachedJson;
        if (context == null) return "[]";
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        cachedJson = prefs.getString(KEY_LATEST_VIEWS, "[]");
        return cachedJson;
    }

    /**
     * Внедряет объект latest-views в localStorage сайта в WebView
     */
    public static void injectLatestViewsToWebView(WebView webView) {
        if (webView == null) return;
        Context context = webView.getContext();
        final String latestViewsJson = getLatestViewsJson(context);

        if (latestViewsJson == null || latestViewsJson.isEmpty() || "[]".equals(latestViewsJson) || "null".equals(latestViewsJson)) {
            return;
        }

        webView.post(() -> {
            try {
                String jsScript =
                    "(function() {" +
                    "  try {" +
                    "    var newViews = " + latestViewsJson + ";" +
                    "    if (!Array.isArray(newViews) || newViews.length === 0) return;" +
                    "    var existingStr = localStorage.getItem('latest-views');" +
                    "    var list = [];" +
                    "    if (existingStr) {" +
                    "      try { list = JSON.parse(existingStr); } catch(e) {}" +
                    "    }" +
                    "    if (!Array.isArray(list)) list = [];" +
                    "    var updated = false;" +
                    "    newViews.forEach(function(newItem) {" +
                    "      if (!newItem || !newItem.media) return;" +
                    "      var idx = -1;" +
                    "      for (var i = 0; i < list.length; i++) {" +
                    "        if (list[i] && list[i].media) {" +
                    "          if ((newItem.media.id && list[i].media.id === newItem.media.id) ||" +
                    "              (newItem.media.slug_url && list[i].media.slug_url === newItem.media.slug_url)) {" +
                    "            idx = i;" +
                    "            break;" +
                    "          }" +
                    "        }" +
                    "      }" +
                    "      if (idx !== -1) {" +
                    "        list[idx] = newItem;" +
                    "        updated = true;" +
                    "      } else {" +
                    "        list.unshift(newItem);" +
                    "        updated = true;" +
                    "      }" +
                    "    });" +
                    "    if (updated) {" +
                    "      localStorage.setItem('latest-views', JSON.stringify(list));" +
                    "      console.log('[LatestViewsManager] Synced latest-views into localStorage');" +
                    "    }" +
                    "  } catch(e) {" +
                    "    console.error('[LatestViewsManager] Error injecting latest-views:', e);" +
                    "  }" +
                    "})();";

                webView.evaluateJavascript(jsScript, null);
            } catch (Exception e) {
                Log.e(TAG, "Error injecting latest-views into WebView", e);
            }
        });
    }

    private static int getMediaId(JsonObject obj) {
        try {
            if (obj != null && obj.has("media") && obj.get("media").isJsonObject()) {
                JsonObject media = obj.getAsJsonObject("media");
                if (media.has("id") && !media.get("id").isJsonNull()) return media.get("id").getAsInt();
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static String getMediaSlugUrl(JsonObject obj) {
        try {
            if (obj != null && obj.has("media") && obj.get("media").isJsonObject()) {
                JsonObject media = obj.getAsJsonObject("media");
                if (media.has("slug_url") && !media.get("slug_url").isJsonNull()) return media.get("slug_url").getAsString();
                if (media.has("slug") && !media.get("slug").isJsonNull()) return media.get("slug").getAsString();
            }
        } catch (Exception ignored) {}
        return "";
    }
}
