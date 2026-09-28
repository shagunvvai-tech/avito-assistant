package com.example.avitoassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity {
    private LinearLayout feed;
    private TextView accessStatus;
    private EditText keywords;
    private EditText maxPrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
        refreshFeed();
    }

    private void buildUi() {
        int pad = dp(18);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, dp(30));
        root.setBackgroundColor(Color.rgb(247, 248, 252));
        scroll.addView(root);

        TextView title = text("Avito Assistant", 30, true);
        root.addView(title);

        TextView subtitle = text(
                "Локальный помощник: отслеживает уведомления Avito, выделяет подходящие предложения и новые сообщения.",
                15, false);
        subtitle.setTextColor(Color.DKGRAY);
        subtitle.setPadding(0, dp(6), 0, dp(18));
        root.addView(subtitle);

        accessStatus = text("", 16, true);
        root.addView(cardWrap(accessStatus));

        Button access = button("Включить доступ к уведомлениям");
        access.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(this, "Открой Настройки → Уведомления → Доступ к уведомлениям", Toast.LENGTH_LONG).show();
            }
        });
        root.addView(access);

        TextView h = text("Фильтр объявлений", 21, true);
        h.setPadding(0, dp(24), 0, dp(8));
        root.addView(h);

        keywords = input("Ключевые слова через запятую, например: S25 Ultra, Samsung");
        keywords.setText(Store.getKeywords(this));
        root.addView(keywords);

        maxPrice = input("Максимальная цена, ₽ (0 = без ограничения)");
        maxPrice.setInputType(InputType.TYPE_CLASS_NUMBER);
        long mp = Store.getMaxPrice(this);
        if (mp > 0) maxPrice.setText(String.valueOf(mp));
        root.addView(maxPrice);

        Button save = button("Сохранить фильтр");
        save.setOnClickListener(v -> {
            Store.saveRules(this, keywords.getText().toString(), maxPrice.getText().toString());
            Toast.makeText(this, "Фильтр сохранён", Toast.LENGTH_SHORT).show();
            refreshFeed();
        });
        root.addView(save);

        TextView note = text(
                "Совет: в самом Avito сохрани нужные поиски и включи для них уведомления. Avito Assistant будет быстро отбирать поступившие варианты.",
                14, false);
        note.setTextColor(Color.rgb(80, 80, 90));
        note.setPadding(0, dp(12), 0, dp(18));
        root.addView(note);

        LinearLayout feedHeader = new LinearLayout(this);
        feedHeader.setOrientation(LinearLayout.HORIZONTAL);
        feedHeader.setGravity(Gravity.CENTER_VERTICAL);

        TextView fh = text("Лента", 21, true);
        feedHeader.addView(fh, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Button clear = new Button(this);
        clear.setText("Очистить");
        clear.setAllCaps(false);
        clear.setOnClickListener(v -> {
            Store.clear(this);
            refreshFeed();
        });
        feedHeader.addView(clear);
        root.addView(feedHeader);

        feed = new LinearLayout(this);
        feed.setOrientation(LinearLayout.VERTICAL);
        root.addView(feed);

        setContentView(scroll);
    }

    private void refreshStatus() {
        boolean enabled = notificationAccessEnabled();
        if (accessStatus != null) {
            accessStatus.setText(enabled
                    ? "✅ Доступ к уведомлениям включён"
                    : "⚠️ Доступ к уведомлениям выключен");
            accessStatus.setTextColor(enabled ? Color.rgb(20, 120, 65) : Color.rgb(190, 90, 20));
        }
    }

    private boolean notificationAccessEnabled() {
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(getPackageName());
    }

    private void refreshFeed() {
        if (feed == null) return;
        feed.removeAllViews();
        List<Store.Event> events = Store.events(this);

        if (events.isEmpty()) {
            TextView empty = text(
                    "Пока пусто. После включения доступа здесь появятся уведомления Avito.",
                    15, false);
            empty.setTextColor(Color.GRAY);
            empty.setPadding(0, dp(12), 0, 0);
            feed.addView(empty);
            return;
        }

        for (Store.Event e : events) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14), dp(12), dp(14), dp(12));

            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setCornerRadius(dp(16));
            bg.setColor(e.highlighted ? Color.rgb(232, 250, 239) : Color.WHITE);
            bg.setStroke(dp(1), e.highlighted ? Color.rgb(70, 170, 100) : Color.rgb(225, 225, 230));
            card.setBackground(bg);

            TextView tag = text(e.message ? "💬 СООБЩЕНИЕ" :
                    (e.highlighted ? "🔥 ПОДХОДИТ" : "Обычное уведомление"), 12, true);
            card.addView(tag);

            TextView t = text(e.title == null || e.title.isEmpty() ? "Avito" : e.title, 17, true);
            t.setPadding(0, dp(5), 0, 0);
            card.addView(t);

            if (e.text != null && !e.text.isEmpty()) {
                TextView body = text(e.text, 15, false);
                body.setTextColor(Color.DKGRAY);
                body.setPadding(0, dp(4), 0, 0);
                card.addView(body);
            }

            String meta = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(new Date(e.time));
            if (e.price > 0) meta += " · " + e.price + " ₽";
            TextView m = text(meta, 12, false);
            m.setTextColor(Color.GRAY);
            m.setPadding(0, dp(7), 0, 0);
            card.addView(m);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dp(8), 0, dp(8));
            feed.addView(card, lp);
        }
    }

    private View cardWrap(View child) {
        LinearLayout card = new LinearLayout(this);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(Color.WHITE);
        bg.setStroke(dp(1), Color.rgb(225, 225, 230));
        card.setBackground(bg);
        card.addView(child);
        return card;
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(16);
        e.setSingleLine(true);
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        lp.setMargins(0, dp(6), 0, dp(6));
        e.setLayoutParams(lp);
        return e;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(16);
        b.setAllCaps(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        lp.setMargins(0, dp(8), 0, dp(4));
        b.setLayoutParams(lp);
        return b;
    }

    private TextView text(String s, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(Color.rgb(28, 29, 34));
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
