package com.ghostmessages;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MessageListener extends NotificationListenerService {
    private static final String PREFS = "ghost_store";
    private static final String KEY = "items";
    private static final int MAX_ITEMS = 500;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String pkg = sbn.getPackageName();
        if (!"com.whatsapp".equals(pkg) &&
            !"org.telegram.messenger".equals(pkg) &&
            !"org.telegram.messenger.web".equals(pkg)) return;

        Notification n = sbn.getNotification();
        if (n == null || n.extras == null) return;
        Bundle e = n.extras;

        CharSequence titleCs = e.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence textCs = e.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (textCs == null) textCs = e.getCharSequence(Notification.EXTRA_TEXT);
        if (textCs == null) textCs = e.getCharSequence(Notification.EXTRA_SUMMARY_TEXT);

        String title = titleCs == null ? "Сообщение" : titleCs.toString().trim();
        String body = textCs == null ? "" : textCs.toString().trim();
        if (body.isEmpty() || body.equals("This message was deleted") ||
            body.equals("Сообщение удалено")) return;

        String app = "com.whatsapp".equals(pkg) ? "WhatsApp" : "Telegram";
        long now = System.currentTimeMillis();
        try {
            JSONObject item = new JSONObject();
            item.put("id", sbn.getKey());
            item.put("app", app);
            item.put("title", title);
            item.put("body", body);
            item.put("time", now);
            item.put("removed", false);
            synchronized (MessageListener.class) {
                JSONArray old = new JSONArray(getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY, "[]"));
                JSONArray updated = new JSONArray();
                updated.put(item);
                for (int i = 0; i < old.length() && updated.length() < MAX_ITEMS; i++) {
                    JSONObject prev = old.optJSONObject(i);
                    if (prev != null && !sbn.getKey().equals(prev.optString("id"))) updated.put(prev);
                }
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY, updated.toString()).apply();
            }
        } catch (Exception ignored) { }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        synchronized (MessageListener.class) {
            try {
                JSONArray old = new JSONArray(getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY, "[]"));
                JSONArray updated = new JSONArray();
                for (int i = 0; i < old.length(); i++) {
                    JSONObject item = old.optJSONObject(i);
                    if (item != null) {
                        if (sbn.getKey().equals(item.optString("id"))) item.put("removed", true);
                        updated.put(item);
                    }
                }
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY, updated.toString()).apply();
            } catch (Exception ignored) { }
        }
    }
}
