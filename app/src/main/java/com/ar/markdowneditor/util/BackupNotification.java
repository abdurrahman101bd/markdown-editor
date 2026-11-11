package com.ar.markdowneditor.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.ar.markdowneditor.R;

public class BackupNotification {

    private static final String CHANNEL_ID = "backup_channel";
    private static final String CHANNEL_NAME = "Auto Backup";
    private static final int NOTIF_ID = 1001;

    private static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Auto backup status");
                nm.createNotificationChannel(channel);
            }
        }
    }

    public static void showSuccess(Context context, String fileName) {
        ensureChannel(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle("Backup completed")
                .setContentText(fileName)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, builder.build());
        } catch (SecurityException ignored) {
            
        }
    }

    public static void showFailure(Context context, String error) {
        ensureChannel(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_close)
                .setContentTitle("Backup failed")
                .setContentText(error)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID + 1, builder.build());
        } catch (SecurityException ignored) {}
    }
}