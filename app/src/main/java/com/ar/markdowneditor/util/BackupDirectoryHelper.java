package com.ar.markdowneditor.util;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class BackupDirectoryHelper {

    private static final String APP_FOLDER = "Markdown Editor";
    private static final String BACKUP_FOLDER = "Backups";
    private static final String NOMEDIA_FILE = ".nomedia";
    private static final int MAX_BACKUPS = 10;

    public static File getAutoBackupDir(Context context) {
        File mediaDir = new File(
                Environment.getExternalStorageDirectory(),
                "Android/media/" + context.getPackageName() + "/" + APP_FOLDER + "/" + BACKUP_FOLDER
        );
        if (!mediaDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            mediaDir.mkdirs();
        }
        ensureNoMedia(mediaDir);
        return mediaDir;
    }

    private static void ensureNoMedia(File dir) {
        File noMedia = new File(dir, NOMEDIA_FILE);
        if (!noMedia.exists()) {
            try (FileOutputStream fos = new FileOutputStream(noMedia)) {
                fos.write(new byte[0]);
            } catch (IOException ignored) {}
        }
    }

    public static File createBackupFile(Context context) {
        File dir = getAutoBackupDir(context);
        String fileName = buildFileName();
        return new File(dir, fileName);
    }

    private static String buildFileName() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
        return "MarkdownEditor_Backup_" + fmt.format(new Date()) + ".zip";
    }

    public static void cleanupOldBackups(Context context) {
        File dir = getAutoBackupDir(context);
        File[] files = dir.listFiles((d, name) -> name.startsWith("MarkdownEditor_Backup_")
                && name.endsWith(".zip"));

        if (files == null || files.length <= MAX_BACKUPS) return;

        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return Long.compare(b.lastModified(), a.lastModified());
            }
        });

        for (int i = MAX_BACKUPS; i < files.length; i++) {
            //noinspection ResultOfMethodCallIgnored
            files[i].delete();
        }
    }

    public static File[] listBackups(Context context) {
        File dir = getAutoBackupDir(context);
        File[] files = dir.listFiles((d, name) ->
                name.startsWith("MarkdownEditor_Backup_") && name.endsWith(".zip"));

        if (files == null) return new File[0];

        Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return files;
    }

    public static void deleteAllBackups(Context context) {
        File dir = getAutoBackupDir(context);
        File[] files = dir.listFiles((d, name) ->
                name.startsWith("MarkdownEditor_Backup_") && name.endsWith(".zip"));
        if (files == null) return;
        for (File f : files) {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }
}