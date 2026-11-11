package com.ar.markdowneditor.util;

import android.content.Context;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class BackupScheduler {

    private static final String WORK_NAME = "auto_backup_work";

    public static void schedule(Context context) {
        int days = PrefsHelper.getAutobackupDays(context);

        if (days <= 0) {
            cancel(context);
            return;
        }

        Constraints constraints = new Constraints.Builder()
			.setRequiresBatteryNotLow(true)
			.build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
			BackupWorker.class,
			1, TimeUnit.DAYS
        )
			.setConstraints(constraints)
			.build();

        WorkManager.getInstance(context)
			.enqueueUniquePeriodicWork(
			WORK_NAME,
			ExistingPeriodicWorkPolicy.KEEP,
			request
		);
    }

    public static void cancel(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
    }
	
	public static java.util.UUID runNow(Context context) {
        androidx.work.Data data = new androidx.work.Data.Builder()
                .putBoolean("force", true)
                .build();

        androidx.work.OneTimeWorkRequest request =
                new androidx.work.OneTimeWorkRequest.Builder(BackupWorker.class)
                        .setInputData(data)
                        .build();

        WorkManager.getInstance(context).enqueue(request);
        return request.getId();
    }
}