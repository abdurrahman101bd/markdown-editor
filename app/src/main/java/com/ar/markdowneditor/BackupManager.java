package com.ar.markdowneditor.util;

import android.content.Context;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;

import com.ar.markdowneditor.data.ReadmeDao;
import com.ar.markdowneditor.data.ReadmeEntity;
import com.ar.markdowneditor.data.ReadmeRepository;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupManager {

    public static final int FORMAT_VERSION = 1;

    public interface ExportCallback {
        void onSuccess(String fileName);
        void onError(String message);
    }

    public static void exportToTree(Context context, Uri treeUri, ExportCallback cb) {
        new Thread(() -> {
            try {
                DocumentFile tree = DocumentFile.fromTreeUri(context, treeUri);
                if (tree == null || !tree.canWrite()) {
                    cb.onError("Cannot write to selected folder");
                    return;
                }

                String fileName = buildFileName();
                DocumentFile existing = tree.findFile(fileName);
                if (existing != null) existing.delete();

                DocumentFile file = tree.createFile("application/zip", fileName);
                if (file == null) {
                    cb.onError("Failed to create backup file");
                    return;
                }

                try (OutputStream os = context.getContentResolver().openOutputStream(file.getUri())) {
                    if (os == null) {
                        cb.onError("Cannot open output stream");
                        return;
                    }
                    writeZip(context, os);
                }

                BackupPrefs.setLastBackupAt(context, System.currentTimeMillis());
                BackupPrefs.setLastBackupName(context, fileName);

                cb.onSuccess(fileName);

            } catch (Exception e) {
                cb.onError(e.getMessage() == null ? "Unknown error" : e.getMessage());
            }
        }).start();
    }

    public static void writeZip(Context context, OutputStream os) throws Exception {
        try (ZipOutputStream zos = new ZipOutputStream(os)) {

            // 1. readmes.json
            String readmesJson = buildReadmesJson(context);
            zos.putNextEntry(new ZipEntry("readmes.json"));
            zos.write(readmesJson.getBytes("UTF-8"));
            zos.closeEntry();

            // 2. settings.json
            String settingsJson = buildSettingsJson(context);
            zos.putNextEntry(new ZipEntry("settings.json"));
            zos.write(settingsJson.getBytes("UTF-8"));
            zos.closeEntry();
        }
    }

    // JSON builders
    private static String buildReadmesJson(Context context) throws Exception {
        ReadmeDao dao = com.ar.markdowneditor.data.AppDatabase
			.getInstance(context).readmeDao();

        List<ReadmeEntity> all = dao.getAllSync();

        JSONArray arr = new JSONArray();
        for (ReadmeEntity e : all) {
            JSONObject o = new JSONObject();
            o.put("id", e.id);
            o.put("title", e.title);
            o.put("content", e.content);
            o.put("updatedAt", e.updatedAt);
            o.put("isDeleted", e.isDeleted);
            o.put("isArchived", e.isArchived);
            o.put("deletedAt", e.deletedAt);
            arr.put(o);
        }

        JSONObject root = new JSONObject();
        root.put("version", FORMAT_VERSION);
        root.put("exportedAt", System.currentTimeMillis());
        root.put("notes", arr);
        return root.toString();
    }

    private static String buildSettingsJson(Context context) throws Exception {
        JSONObject o = new JSONObject();
        o.put("version", FORMAT_VERSION);
        o.put("theme", ThemeUtils.getSavedMode(context));
        o.put("viewMode", ViewModePrefs.getMode(context));
        o.put("autosaveSec", PrefsHelper.getAutosaveSeconds(context));
        o.put("autoremoveDays", PrefsHelper.getAutoremoveDays(context));
        o.put("autobackupDays", PrefsHelper.getAutobackupDays(context));
        o.put("screenshotAllowed", PrefsHelper.isScreenshotAllowed(context));
        o.put("biometricEnabled", PrefsHelper.isBiometricEnabled(context));
        return o.toString();
    }
    
    // File name
    private static String buildFileName() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
        return "MarkdownEditor_Backup_" + fmt.format(new Date()) + ".zip";
    }

	public interface ImportCallback {
		void onSuccess(String message);
		void onError(String message);
	}

	public enum ImportMode {
		SETTINGS_ONLY,
		NOTES_ONLY,
		FULL_REPLACE,
		MERGE
		}

	public static void importFromUri(Context context, Uri fileUri,
									 ImportMode mode, ImportCallback cb) {
		new Thread(() -> {
			try {
				String readmesJson = null;
				String settingsJson = null;

				try (java.io.InputStream is = context.getContentResolver().openInputStream(fileUri)) {
					if (is == null) {
						cb.onError("Cannot open file");
						return;
					}
					try (java.util.zip.ZipInputStream zis =
						new java.util.zip.ZipInputStream(is)) {
						java.util.zip.ZipEntry entry;
						byte[] buf = new byte[8192];
						while ((entry = zis.getNextEntry()) != null) {
							java.io.ByteArrayOutputStream baos =
                                new java.io.ByteArrayOutputStream();
							int len;
							while ((len = zis.read(buf)) > 0) {
								baos.write(buf, 0, len);
							}
							String content = baos.toString("UTF-8");

							if ("readmes.json".equals(entry.getName())) {
								readmesJson = content;
							} else if ("settings.json".equals(entry.getName())) {
								settingsJson = content;
							}
							zis.closeEntry();
						}
					}
				}

				if (readmesJson == null && settingsJson == null) {
					cb.onError("Invalid backup file");
					return;
				}

				ReadmeRepository repo = new ReadmeRepository(context);

				switch (mode) {
					case SETTINGS_ONLY:
						if (settingsJson != null) applySettingsOnMain(context, settingsJson);  // replace
						cb.onSuccess("Settings restored");
						break;

					case NOTES_ONLY:
						if (readmesJson != null) {
							replaceNotes(repo, readmesJson, cb);
						} else {
							cb.onError("No notes in backup");
						}
						break;

					case FULL_REPLACE:
						if (settingsJson != null) applySettingsOnMain(context, settingsJson);  // replace
						if (readmesJson != null) {
							replaceNotes(repo, readmesJson, cb);
						} else {
							cb.onSuccess("Settings restored");
						}
						break;

					case MERGE:
						if (settingsJson != null) applySettingsOnMain(context, settingsJson);  // replace
						if (readmesJson != null) {
							mergeNotes(repo, readmesJson, cb);
						} else {
							cb.onSuccess("Settings restored");
						}
						break;
				}

			} catch (Exception e) {
				cb.onError(e.getMessage() == null ? "Import failed" : e.getMessage());
			}
		}).start();
	}

	private static void replaceNotes(ReadmeRepository repo, String json, ImportCallback cb) {
		try {
			JSONObject root = new JSONObject(json);
			JSONArray arr = root.optJSONArray("notes");
			if (arr == null) {
				cb.onError("No notes in backup");
				return;
			}

			List<ReadmeEntity> list = new ArrayList<>();
			for (int i = 0; i < arr.length(); i++) {
				JSONObject o = arr.getJSONObject(i);
				ReadmeEntity e = new ReadmeEntity();
				e.title = o.optString("title", "");
				e.content = o.optString("content", "");
				e.updatedAt = o.optLong("updatedAt", System.currentTimeMillis());
				e.isDeleted = o.optBoolean("isDeleted", false);
				e.isArchived = o.optBoolean("isArchived", false);
				e.deletedAt = o.optLong("deletedAt", 0);
				list.add(e);
			}

			repo.deleteAllNotes();

			repo.insertAll(list, () ->
			cb.onSuccess(list.size() + " notes restored"));

		} catch (Exception e) {
			cb.onError("Parse error: " + e.getMessage());
		}
	}

	private static void mergeNotes(ReadmeRepository repo, String json, ImportCallback cb) {
		try {
			JSONObject root = new JSONObject(json);
			JSONArray arr = root.optJSONArray("notes");
			if (arr == null) {
				cb.onError("No notes in backup");
				return;
			}

			repo.getAllSync(existing -> {
				java.util.Set<String> existingKeys = new java.util.HashSet<>();
				for (ReadmeEntity e : existing) {
					existingKeys.add(e.title + "|||" + e.content);
				}

				List<ReadmeEntity> toInsert = new ArrayList<>();
				int skipped = 0;

				try {
					for (int i = 0; i < arr.length(); i++) {
						JSONObject o = arr.getJSONObject(i);
						String title = o.optString("title", "");
						String content = o.optString("content", "");
						String key = title + "|||" + content;

						if (existingKeys.contains(key)) {
							skipped++;
							continue;
						}

						ReadmeEntity e = new ReadmeEntity();
						e.title = title;
						e.content = content;
						e.updatedAt = o.optLong("updatedAt", System.currentTimeMillis());
						e.isDeleted = o.optBoolean("isDeleted", false);
						e.isArchived = o.optBoolean("isArchived", false);
						e.deletedAt = o.optLong("deletedAt", 0);
						toInsert.add(e);
					}

					final int finalSkipped = skipped;
					if (toInsert.isEmpty()) {
						cb.onSuccess("No new notes to add");
					} else {
						repo.insertAll(toInsert, () ->
						cb.onSuccess(toInsert.size() + " notes added, " +
									 finalSkipped + " duplicates skipped"));
					}

				} catch (Exception e) {
					cb.onError("Parse error: " + e.getMessage());
				}
			});

		} catch (Exception e) {
			cb.onError("Parse error: " + e.getMessage());
		}
	}

	private static void applySettings(Context context, String json) throws Exception {
		JSONObject o = new JSONObject(json);
		if (o.has("theme")) ThemeUtils.setMode(context, o.getInt("theme"));
		if (o.has("viewMode")) ViewModePrefs.setMode(context, o.getInt("viewMode"));
		if (o.has("autosaveSec")) PrefsHelper.setAutosaveSeconds(context, o.getInt("autosaveSec"));
		if (o.has("autoremoveDays")) PrefsHelper.setAutoremoveDays(context, o.getInt("autoremoveDays"));
        if (o.has("autobackupDays")) PrefsHelper.setAutobackupDays(context, o.getInt("autobackupDays"));
		if (o.has("screenshotAllowed")) PrefsHelper.setScreenshotAllowed(context, o.getBoolean("screenshotAllowed"));
		if (o.has("biometricEnabled")) PrefsHelper.setBiometricEnabled(context, o.getBoolean("biometricEnabled"));
	}
	
	private static void applySettingsOnMain(Context context, String json) throws Exception {
		final Exception[] err = {null};

		android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
		java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

		mainHandler.post(() -> {
			try {
				applySettings(context, json);
			} catch (Exception e) {
				err[0] = e;
			} finally {
				latch.countDown();
			}
		});

		try {
			latch.await();
		} catch (InterruptedException ignored) {}

		if (err[0] != null) throw err[0];
	}
	
	// Backup Info (for history)
	public static BackupInfo readBackupInfo(File zipFile) {
		BackupInfo info = new BackupInfo(zipFile);

		try (java.io.InputStream is = new java.io.FileInputStream(zipFile);
		java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(is)) {

			java.util.zip.ZipEntry entry;
			byte[] buf = new byte[8192];

			while ((entry = zis.getNextEntry()) != null) {
				java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
				int len;
				while ((len = zis.read(buf)) > 0) {
					baos.write(buf, 0, len);
				}
				String content = baos.toString("UTF-8");

				if ("readmes.json".equals(entry.getName())) {
					JSONObject root = new JSONObject(content);
					JSONArray arr = root.optJSONArray("notes");
					if (arr != null) {
						info.noteCount = arr.length();
						for (int i = 0; i < arr.length(); i++) {
							JSONObject o = arr.getJSONObject(i);
							boolean isDeleted = o.optBoolean("isDeleted", false);
							boolean isArchived = o.optBoolean("isArchived", false);
							if (isDeleted) info.trashCount++;
							else if (isArchived) info.archivedCount++;
							else info.activeCount++;
						}
					}
				} else if ("settings.json".equals(entry.getName())) {
					JSONObject o = new JSONObject(content);
					info.theme = o.optInt("theme", 0);
					info.viewMode = o.optInt("viewMode", 1);
					info.autosaveSec = o.optInt("autosaveSec", 5);
					info.autoremoveDays = o.optInt("autoremoveDays", 0);
                    info.autobackupDays = o.optInt("autobackupDays", 0);
					info.screenshotAllowed = o.optBoolean("screenshotAllowed", true);
					info.biometricEnabled = o.optBoolean("biometricEnabled", false);
				}
				zis.closeEntry();
			}

			info.detailsLoaded = true;

		} catch (Exception ignored) {

		}

		return info;
	}

	public static boolean deleteBackup(File zipFile) {
		if (zipFile == null || !zipFile.exists()) return false;
		return zipFile.delete();
	}
	
}