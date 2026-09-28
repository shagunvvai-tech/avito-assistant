package com.example.avitoassistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.Locale;

public class AvitoNotificationListener extends NotificationListenerService {
    private static final String CHANNEL_ID = "matches";

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;
        if (getPackageName().equals(sbn.getPackageName())) return;

        Notification n = sbn.getNotification();
        Bundle x = n.extras;
        String title = x == null ? "" : safe(x.getCharSequence(Notification.EXTRA_TITLE));
        String text = x == null ? "" : safe(x.getCharSequence(Notification.EXTRA_TEXT));
        if (text.isEmpty() && x != null) text = safe(x.getCharSequence(Notification.EXTRA_BIG_TEXT));

        String pkg = sbn.getPackageName() == null ? "" : sbn.getPackageName();
        if (!looksLikeAvito(pkg, title, text)) return;

        long price = Store.extractPrice(title + " " + text);
        boolean message = Store.looksLikeMessage(title, text);
        boolean highlighted = Store.shouldHighlight(this, title, text, price, message);

        Store.Event e = new Store.Event();
        e.time = System.currentTimeMillis();
        e.title = title;
        e.text = text;
        e.pkg = pkg;
        e.price = price;
        e.message = message;
        e.highlighted = highlighted;
        Store.addEvent(this, e);

        if (highlighted) {
            showAssistantNotification(title, text, n.contentIntent, message, price);
        }
    }

    private boolean looksLikeAvito(String pkg, String title, String text) {
        String p = pkg.toLowerCase(Locale.ROOT);
        String s = (title + " " + text).toLowerCase(Locale.ROOT);
        return p.contains("avito") || s.contains("avito") || s.contains("авито");
    }

    private void showAssistantNotification(String title, String text, PendingIntent original,
                                           boolean message, long price) {
        String head = message ? "💬 Новое сообщение Avito" : "🔥 Подходящее предложение";
        String line = (title == null || title.isEmpty()) ? text : title;
        if (!message && price > 0) line += " · " + price + " ₽";

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        b.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(head)
                .setContentText(line)
                .setStyle(new Notification.BigTextStyle().bigText(
                        (title == null ? "" : title) + "\n" + (text == null ? "" : text)))
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH);

        if (original != null) b.setContentIntent(original);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify((int)(System.currentTimeMillis() & 0x7fffffff), b.build());
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                NotificationChannel ch = new NotificationChannel(
                        CHANNEL_ID, "Выгодные предложения Avito",
                        NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("Подходящие объявления и сообщения Avito");
                nm.createNotificationChannel(ch);
            }
        }
    }

    private static String safe(CharSequence s) {
        return s == null ? "" : s.toString();
    }
}
