package com.ghostmessages;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(10, 13, 22);
    private final int panel = Color.rgb(22, 28, 43);
    private final int accent = Color.rgb(111, 133, 255);
    private LinearLayout list;
    private TextView status;
    private String filter = "Все";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(0);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(24), dp(18), dp(12));
        root.setBackgroundColor(bg);

        TextView logo = new TextView(this);
        logo.setText("GHOST MESSAGES");
        logo.setTextColor(Color.WHITE);
        logo.setTextSize(23);
        logo.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(logo);

        TextView sub = new TextView(this);
        sub.setText("История уведомлений");
        sub.setTextColor(Color.LTGRAY);
        sub.setTextSize(14);
        sub.setPadding(0, dp(5), 0, dp(16));
        root.addView(sub);

        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setTextSize(13);
        status.setPadding(dp(14), dp(12), dp(14), dp(12));
        status.setBackground(round(panel, 14));
        root.addView(status);

        Button permission = button("Включить доступ к уведомлениям");
        permission.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        root.addView(permission, marginTop(10));

        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        for (String f : new String[]{"Все", "WhatsApp", "Telegram"}) {
            Button btt = button(f);
            btt.setOnClickListener(v -> { filter = f; refresh(); });
            filters.addView(btt, new LinearLayout.LayoutParams(0, dp(42), 1));
        }
        root.addView(filters, marginTop(12));

        TextView label = new TextView(this);
        label.setText("СОХРАНЁННЫЕ СООБЩЕНИЯ");
        label.setTextColor(Color.rgb(155, 165, 190));
        label.setTextSize(11);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setPadding(2, dp(18), 0, dp(8));
        root.addView(label);

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        Button clear = button("Очистить историю");
        clear.setOnClickListener(v -> {
            getSharedPreferences("ghost_store", MODE_PRIVATE).edit().putString("items", "[]").apply();
            refresh();
        });
        root.addView(clear, marginTop(8));
        setContentView(root);
        refresh();
    }

    @Override protected void onResume() { super.onResume(); refresh(); }

    private void refresh() {
        if (status == null || list == null) return;
        boolean enabled = false;
        String listeners = android.provider.Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (listeners != null && listeners.contains(getPackageName())) enabled = true;
        status.setText(enabled ? "● Доступ включён. Новые уведомления сохраняются." :
                "● Доступ выключен. Нажми кнопку ниже и разреши Ghost Messages.");
        list.removeAllViews();
        try {
            JSONArray arr = new JSONArray(getSharedPreferences("ghost_store", MODE_PRIVATE).getString("items", "[]"));
            int shown = 0;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject item = arr.optJSONObject(i);
                if (item == null) continue;
                String app = item.optString("app");
                if (!"Все".equals(filter) && !filter.equals(app)) continue;
                list.addView(makeCard(item));
                shown++;
            }
            if (shown == 0) {
                TextView empty = new TextView(this);
                empty.setText("Пока пусто\nЗдесь появятся уведомления WhatsApp и Telegram.");
                empty.setTextColor(Color.LTGRAY);
                empty.setGravity(Gravity.CENTER);
                empty.setPadding(12, dp(35), 12, dp(35));
                list.addView(empty);
            }
        } catch (Exception ignored) { }
    }

    private View makeCard(JSONObject item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(round(panel, 14));
        TextView head = new TextView(this);
        head.setText(item.optString("app") + "  ·  " + item.optString("title"));
        head.setTextColor(Color.WHITE);
        head.setTextSize(14);
        head.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(head);
        TextView body = new TextView(this);
        body.setText(item.optString("body"));
        body.setTextColor(Color.rgb(220, 225, 240));
        body.setTextSize(14);
        body.setPadding(0, dp(7), 0, dp(7));
        card.addView(body);
        String time = new SimpleDateFormat("dd.MM.yyyy  HH:mm", Locale.getDefault()).format(new Date(item.optLong("time")));
        TextView foot = new TextView(this);
        foot.setText(time + (item.optBoolean("removed") ? "  ·  уведомление исчезло" : ""));
        foot.setTextColor(item.optBoolean("removed") ? Color.rgb(255, 190, 100) : Color.GRAY);
        foot.setTextSize(11);
        card.addView(foot);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(9);
        card.setLayoutParams(p);
        return card;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(12);
        b.setBackground(round(accent, 12));
        return b;
    }
    private GradientDrawable round(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private LinearLayout.LayoutParams marginTop(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(46));
        p.topMargin = dp(top); return p;
    }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
}
