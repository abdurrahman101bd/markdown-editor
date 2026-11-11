package com.ar.markdowneditor.util;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class BackupWorker extends Worker {

    public BackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

	@NonNull
	@Override
	public Result doWork() {
		Context ctx = getApplicationContext();
		boolean force = getInputData().getBoolean("force", false);

		try {
			int days = PrefsHelper.getAutobackupDays(ctx);
			if (days <= 0) {
				return Result.success();
			}

			long lastAt = BackupPrefs.getLastAutoBackupAt(ctx);
			long intervalMs = days * 24L * 60 * 60 * 1000;

			if (!force && System.currentTimeMillis() - lastAt < intervalMs) {
				return Result.success();
			}

			File file = BackupDirectoryHelper.createBackupFile(ctx);
			try (OutputStream os = new FileOutputStream(file)) {
				BackupManager.writeZip(ctx, os);
			}

			BackupPrefs.setLastAutoBackupAt(ctx, System.currentTimeMillis());
			BackupPrefs.setLastAutoBackupName(ctx, file.getName());

			BackupDirectoryHelper.cleanupOldBackups(ctx);

			BackupNotification.showSuccess(ctx, file.getName());

			return Result.success();

		} catch (Exception e) {
			BackupNotification.showFailure(ctx,
										   e.getMessage() == null ? "Unknown error" : e.getMessage());
			return Result.retry();
		}
	}

}