package com.ar.markdowneditor;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import com.ar.markdowneditor.util.PrefsHelper;
import com.ar.markdowneditor.util.ThemeUtils;
import com.ar.markdowneditor.util.ViewModePrefs;
import android.widget.CompoundButton;
import com.ar.markdowneditor.util.BiometricHelper;
import com.ar.markdowneditor.util.BiometricPromptHelper;
import android.widget.ProgressBar;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.ar.markdowneditor.util.BackupManager;
import com.ar.markdowneditor.util.BackupPrefs;
import com.ar.markdowneditor.util.BackupScheduler;
import com.ar.markdowneditor.util.AppLinks;
import android.widget.LinearLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends BaseActivity {

    private static final int[] AUTOSAVE_STEPS = {1, 3, 5, 10, 15, 30, 60};
    private static final String[] AUTOREMOVE_STEPS = {"Off", "3 Days", "7 Days", "30 Days", "60 Days", "90 Days"};
    private static final int[] AUTOREMOVE_DAYS  = {0, 3, 7, 30, 60, 90};
    private static final String[] AUTOBACKUP_STEPS_LABELS = {"Off", "1 Day", "3 Days", "7 Days", "15 Days", "30 Days"};
    private static final int[] AUTOBACKUP_STEPS = {0, 1, 3, 7, 15, 30};

    private int autosaveIndex = 0;
    private int autoremoveIndex = 0;
    private int autobackupIndex = 0;

    private ActivityResultLauncher<Intent> backupFolderPicker;
    private ActivityResultLauncher<Intent> backupFilePicker;

    private TextView themeValue;
    private TextView viewModeValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setupDrawer(R.id.nav_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        themeValue = findViewById(R.id.text_theme_value);
        viewModeValue = findViewById(R.id.text_view_mode_value);
        
        if (getIntent().getBooleanExtra("open_feedback", false)) {
            showFeedbackDialog();
        }

        updateThemeSubtitle();
        updateDefaultViewSubtitle();
        setupAutosaveStepper();
        setupAutoremoveStepper();
        setupAutobackupStepper();
        setupScreenshotToggle();
        setupBiometricToggle();
        setupAppVersion();
        setupBackupLaunchers();

        findViewById(R.id.row_theme).setOnClickListener(v -> showThemeDialog());
        findViewById(R.id.row_view_mode).setOnClickListener(v -> showDefaultViewDialog());

        findViewById(R.id.row_import_backup).setOnClickListener(v -> onImportBackup());
        findViewById(R.id.row_export_backup).setOnClickListener(v -> onExportBackup());
        findViewById(R.id.row_backup_now).setOnClickListener(v -> onBackupNow());
        
        findViewById(R.id.row_backup_history).setOnClickListener(v -> {
            startActivity(new Intent(this, BackupHistoryActivity.class));
        });

        findViewById(R.id.row_feedback).setOnClickListener(v -> showFeedbackDialog());
        findViewById(R.id.row_documentation).setOnClickListener(v -> openUrl(AppLinks.DOCS_URL));
        findViewById(R.id.row_source_code).setOnClickListener(v -> openUrl(AppLinks.REPO_URL));
        findViewById(R.id.row_libraries).setOnClickListener(v -> showLibrariesDialog());
    }
	
    private void updateThemeSubtitle() {
        int mode = ThemeUtils.getSavedMode(this);
        int resId;
        switch (mode) {
            case ThemeUtils.THEME_LIGHT:
                resId = R.string.theme_light;
                break;
            case ThemeUtils.THEME_DARK:
                resId = R.string.theme_dark;
                break;
            case ThemeUtils.THEME_SYSTEM:
            default:
                resId = R.string.theme_system;
                break;
        }
        themeValue.setText(resId);
    }

    private void showThemeDialog() {
        int current = ThemeUtils.getSavedMode(this);

        ContextThemeWrapper wrapper = new ContextThemeWrapper(
			this,
			com.google.android.material.R.style.Theme_MaterialComponents_DayNight);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(wrapper);
        View dialogView = LayoutInflater.from(wrapper).inflate(R.layout.dialog_theme, null);
        View optionSystem = dialogView.findViewById(R.id.option_system);
        View optionLight = dialogView.findViewById(R.id.option_light);
        View optionDark = dialogView.findViewById(R.id.option_dark);
        ImageView checkSystem = dialogView.findViewById(R.id.check_system);
        ImageView checkLight = dialogView.findViewById(R.id.check_light);
        ImageView checkDark = dialogView.findViewById(R.id.check_dark);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        applyThemeSelection(current, optionSystem, optionLight, optionDark,
							checkSystem, checkLight, checkDark);

        optionSystem.setOnClickListener(v -> applyTheme(dialog, ThemeUtils.THEME_SYSTEM));
        optionLight.setOnClickListener(v -> applyTheme(dialog, ThemeUtils.THEME_LIGHT));
        optionDark.setOnClickListener(v -> applyTheme(dialog, ThemeUtils.THEME_DARK));
        dialog.show();
    }

    private void applyThemeSelection(int mode,
                                     View sys, View light, View dark,
                                     ImageView checkSys, ImageView checkLight, ImageView checkDark) {
        boolean isSystem = mode == ThemeUtils.THEME_SYSTEM;
        boolean isLight = mode == ThemeUtils.THEME_LIGHT;
        boolean isDark = mode == ThemeUtils.THEME_DARK;

        sys.setSelected(isSystem);
        light.setSelected(isLight);
        dark.setSelected(isDark);

        checkSys.setVisibility(isSystem ? View.VISIBLE : View.GONE);
        checkLight.setVisibility(isLight ? View.VISIBLE : View.GONE);
        checkDark.setVisibility(isDark ? View.VISIBLE : View.GONE);
    }

    private void applyTheme(AlertDialog dialog, int mode) {
        if (ThemeUtils.getSavedMode(this) == mode) {
            dialog.dismiss();
            return;
        }
        ThemeUtils.setMode(this, mode);
        updateThemeSubtitle();
        dialog.dismiss();
        recreate();
    }

    private void updateDefaultViewSubtitle() {
        int mode = ViewModePrefs.getMode(this);
        if (mode == ViewModePrefs.MODE_GRID) {
            viewModeValue.setText(R.string.label_grid);
        } else {
            viewModeValue.setText(R.string.label_list);
        }
    }

    private void showDefaultViewDialog() {
        int currentMode = ViewModePrefs.getMode(this);

        ContextThemeWrapper wrapper = new ContextThemeWrapper(
			this,
			com.google.android.material.R.style.Theme_MaterialComponents_DayNight);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(wrapper);
        View dialogView = LayoutInflater.from(wrapper).inflate(R.layout.dialog_default_view, null);
        View optionGrid = dialogView.findViewById(R.id.option_grid);
        View optionList = dialogView.findViewById(R.id.option_list);
        ImageView checkGrid = dialogView.findViewById(R.id.check_grid_view);
        ImageView checkList = dialogView.findViewById(R.id.check_list_view);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        applyViewSelection(currentMode, optionGrid, optionList, checkGrid, checkList);

        optionGrid.setOnClickListener(v -> {
            ViewModePrefs.setMode(this, ViewModePrefs.MODE_GRID);
            updateDefaultViewSubtitle();
            dialog.dismiss();
        });

        optionList.setOnClickListener(v -> {
            ViewModePrefs.setMode(this, ViewModePrefs.MODE_LIST);
            updateDefaultViewSubtitle();
            dialog.dismiss();
        });
        dialog.show();
    }

    private void applyViewSelection(int mode,
                                    View grid, View list,
                                    ImageView checkGrid, ImageView checkList) {
        boolean isGrid = mode == ViewModePrefs.MODE_GRID;
        boolean isList = mode == ViewModePrefs.MODE_LIST;

        grid.setSelected(isGrid);
        list.setSelected(isList);

        checkGrid.setVisibility(isGrid ? View.VISIBLE : View.GONE);
        checkList.setVisibility(isList ? View.VISIBLE : View.GONE);
    }

    private void setupAutosaveStepper() {
        ImageButton btnMinus = findViewById(R.id.btn_autosave_minus);
        ImageButton btnPlus = findViewById(R.id.btn_autosave_plus);
        TextView valueText = findViewById(R.id.text_autosave_value);

        int current = PrefsHelper.getAutosaveSeconds(this);
        autosaveIndex = findClosestIndex(AUTOSAVE_STEPS, current);
        updateAutosaveText(valueText);

        btnMinus.setOnClickListener(v -> {
            if (autosaveIndex > 0) {
                autosaveIndex--;
                PrefsHelper.setAutosaveSeconds(this, AUTOSAVE_STEPS[autosaveIndex]);
                updateAutosaveText(valueText);
            }
        });
        btnPlus.setOnClickListener(v -> {
            if (autosaveIndex < AUTOSAVE_STEPS.length - 1) {
                autosaveIndex++;
                PrefsHelper.setAutosaveSeconds(this, AUTOSAVE_STEPS[autosaveIndex]);
                updateAutosaveText(valueText);
            }
        });
    }

    private void updateAutosaveText(TextView tv) {
        tv.setText(AUTOSAVE_STEPS[autosaveIndex] + "s");
    }

    private void setupAutoremoveStepper() {
		ImageButton btnMinus = findViewById(R.id.btn_autoremove_minus);
		ImageButton btnPlus = findViewById(R.id.btn_autoremove_plus);
		TextView valueText = findViewById(R.id.text_autoremove_value);

		int current = PrefsHelper.getAutoremoveDays(this);
		autoremoveIndex = findClosestIndex(AUTOREMOVE_DAYS, current);
		updateAutoremoveText(valueText);

		btnMinus.setOnClickListener(v -> {
			if (autoremoveIndex > 0) {
				autoremoveIndex--;
				PrefsHelper.setAutoremoveDays(this, AUTOREMOVE_DAYS[autoremoveIndex]);
				updateAutoremoveText(valueText);
				onAutoremoveChanged();
			}
		});
		btnPlus.setOnClickListener(v -> {
			if (autoremoveIndex < AUTOREMOVE_DAYS.length - 1) {
				autoremoveIndex++;
				PrefsHelper.setAutoremoveDays(this, AUTOREMOVE_DAYS[autoremoveIndex]);
				updateAutoremoveText(valueText);
				onAutoremoveChanged();
			}
		});
	}

	private void onAutoremoveChanged() {
		int days = AUTOREMOVE_DAYS[autoremoveIndex];
		if (days > 0) {
			new com.ar.markdowneditor.data.ReadmeRepository(this).purgeOldTrash(days);
			Toast.makeText(this,
						   "Notes older than " + days + " days will be removed",
						   Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this, "Auto-remove disabled", Toast.LENGTH_SHORT).show();
		}
	}

    private void updateAutoremoveText(TextView tv) {
        tv.setText(AUTOREMOVE_STEPS[autoremoveIndex]);
    }

    private void setupAutobackupStepper() {
		ImageButton btnMinus = findViewById(R.id.btn_autobackup_minus);
		ImageButton btnPlus = findViewById(R.id.btn_autobackup_plus);
		TextView valueText = findViewById(R.id.text_autobackup_value);

		int current = PrefsHelper.getAutobackupDays(this);
		autobackupIndex = findClosestIndex(AUTOBACKUP_STEPS, current);
		updateAutobackupText(valueText);

		btnMinus.setOnClickListener(v -> {
			if (autobackupIndex > 0) {
				autobackupIndex--;
				PrefsHelper.setAutobackupDays(this, AUTOBACKUP_STEPS[autobackupIndex]);
				updateAutobackupText(valueText);
				onAutobackupChanged();
			}
		});
		btnPlus.setOnClickListener(v -> {
			if (autobackupIndex < AUTOBACKUP_STEPS.length - 1) {
				autobackupIndex++;
				PrefsHelper.setAutobackupDays(this, AUTOBACKUP_STEPS[autobackupIndex]);
				updateAutobackupText(valueText);
				onAutobackupChanged();
			}
		});
	}

	private void updateAutobackupText(TextView tv) {
		tv.setText(AUTOBACKUP_STEPS_LABELS[autobackupIndex]);
	}

	private void onAutobackupChanged() {
    	int days = AUTOBACKUP_STEPS[autobackupIndex];
	    if (days > 0) {
    	    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        	    if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            	        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                	Toast.makeText(this, "Storage permission required for auto backup", Toast.LENGTH_SHORT).show();
                
	                androidx.activity.result.ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(
    	                    new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), granted -> {
        	                    if (granted) {
            	                    BackupScheduler.schedule(SettingsActivity.this);
                	                Toast.makeText(SettingsActivity.this, "Auto backup enabled", Toast.LENGTH_SHORT).show();
                    	        } else {
                        	        Toast.makeText(SettingsActivity.this, "Permission denied", Toast.LENGTH_SHORT).show();
                            	}
	                        });
    	            permissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        	        return;
            	}
	        }
    	    
	        BackupScheduler.schedule(this);
    	    Toast.makeText(this, "Auto backup every " + days + " day(s)", Toast.LENGTH_SHORT).show();
	    } else {
    	    BackupScheduler.cancel(this);
        	Toast.makeText(this, "Auto backup disabled", Toast.LENGTH_SHORT).show();
	    }
	}

    private void setupScreenshotToggle() {
        SwitchMaterial sw = findViewById(R.id.switch_screenshot);
        TextView subtitle = findViewById(R.id.text_screenshot_subtitle);

        boolean allowed = PrefsHelper.isScreenshotAllowed(this);
        sw.setChecked(!allowed);
        subtitle.setText(allowed ? "Allowed" : "Disallowed");

        findViewById(R.id.row_screenshot).setOnClickListener(v -> sw.toggle());

        sw.setOnCheckedChangeListener((b, isChecked) -> {
            boolean allow = !isChecked;
            PrefsHelper.setScreenshotAllowed(this, allow);
            subtitle.setText(allow ? "Allowed" : "Disallowed");
        });
    }

    private void setupBiometricToggle() {
		SwitchMaterial sw = findViewById(R.id.switch_biometric);
		TextView subtitle = findViewById(R.id.text_biometric_subtitle);

		boolean available = BiometricHelper.isAvailable(this);
		boolean enabled = PrefsHelper.isBiometricEnabled(this) && available;

		sw.setChecked(enabled);
		subtitle.setText(enabled ? "Enabled" : (available ? "Disabled" : "Not available"));

		findViewById(R.id.row_biometric).setOnClickListener(v -> {
			if (!available) {
				Toast.makeText(this,
							   "Biometric or device lock not available",
							   Toast.LENGTH_SHORT).show();
				return;
			}
			sw.toggle();
		});

		sw.setOnCheckedChangeListener((b, isChecked) -> {
			if (isChecked) {
				BiometricPromptHelper.show(
                    this,
                    "Confirm biometric lock",
                    "Authenticate to enable app lock",
                    new BiometricPromptHelper.Callback() {
                        @Override
                        public void onSuccess() {
                            PrefsHelper.setBiometricEnabled(SettingsActivity.this, true);
                            subtitle.setText("Enabled");
                            Toast.makeText(SettingsActivity.this,
										   "App lock enabled", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String message) {
                            sw.setOnCheckedChangeListener(null);
                            sw.setChecked(false);
                            subtitle.setText("Disabled");
                            sw.setOnCheckedChangeListener(getBiometricListener(sw, subtitle));
                            Toast.makeText(SettingsActivity.this,
										   "Authentication failed", Toast.LENGTH_SHORT).show();
                        }
                    });
			} else {
				PrefsHelper.setBiometricEnabled(this, false);
				subtitle.setText("Disabled");
			}
		});
	}

	private CompoundButton.OnCheckedChangeListener getBiometricListener(
        SwitchMaterial sw, TextView subtitle) {
		return (b, isChecked) -> {
			if (!isChecked) {
				PrefsHelper.setBiometricEnabled(SettingsActivity.this, false);
				subtitle.setText("Disabled");
			}
		};
	}

    private void onImportBackup() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("application/zip");
		intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
							"application/zip",
							"application/x-zip-compressed",
							"*/*"
						});
		backupFilePicker.launch(intent);
	}

    private void onExportBackup() {
		String savedUri = BackupPrefs.getManualTreeUri(this);

		if (savedUri != null) {
			Uri treeUri = Uri.parse(savedUri);
			try {
				androidx.documentfile.provider.DocumentFile tree =
                    androidx.documentfile.provider.DocumentFile.fromTreeUri(this, treeUri);
				if (tree != null && tree.canWrite()) {
					performExport(treeUri);
					return;
				}
			} catch (Exception ignored) {}
		}

		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
						| Intent.FLAG_GRANT_WRITE_URI_PERMISSION
						| Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
						| Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
		backupFolderPicker.launch(intent);
	}

	private void performExport(Uri treeUri) {
		Toast.makeText(this, R.string.backup_in_progress, Toast.LENGTH_SHORT).show();

		BackupManager.exportToTree(this, treeUri, new BackupManager.ExportCallback() {
				@Override
				public void onSuccess(String fileName) {
					runOnUiThread(() -> Toast.makeText(SettingsActivity.this,
													   getString(R.string.backup_export_success, fileName),
													   Toast.LENGTH_LONG).show());
				}

				@Override
				public void onError(String message) {
					runOnUiThread(() -> Toast.makeText(SettingsActivity.this,
													   getString(R.string.backup_export_failed, message),
													   Toast.LENGTH_LONG).show());
				}
			});
	}

	private void setupBackupLaunchers() {
		// Export — folder picker
		backupFolderPicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
		result -> {
			if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
			Uri treeUri = result.getData().getData();
			if (treeUri == null) return;

			final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
				| Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
			try {
				getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
			} catch (Exception ignored) {}

			BackupPrefs.setManualTreeUri(this, treeUri.toString());
			performExport(treeUri);
		});

		// Import — file picker
		backupFilePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
		result -> {
			if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
			Uri fileUri = result.getData().getData();
			if (fileUri == null) return;

			showImportOptionsDialog(fileUri);
		});
	}
	
	private void onBackupNow() {
        if (PrefsHelper.getAutobackupDays(this) <= 0) {
            Toast.makeText(this,
                    "Enable auto backup first to set a schedule",
                    Toast.LENGTH_SHORT).show();
        }

        java.util.UUID workId = BackupScheduler.runNow(this);
        Toast.makeText(this, "Backup started…", Toast.LENGTH_SHORT).show();

        androidx.work.WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(workId)
                .observe(this, workInfo -> {
                    if (workInfo == null) return;

                    androidx.work.WorkInfo.State state = workInfo.getState();
                    if (state == androidx.work.WorkInfo.State.SUCCEEDED
                            || state == androidx.work.WorkInfo.State.FAILED) {
                        updateLastBackupInfo();
                        updateHistoryCount();
                    }
                });
    }
	
	private void updateLastBackupInfo() {
        TextView lastBackup = findViewById(R.id.text_last_backup);
        TextView lastFile   = findViewById(R.id.text_last_backup_file);
        if (lastBackup == null) return;

        long lastAt = BackupPrefs.getLastAutoBackupAt(this);
        if (lastAt <= 0) {
            lastBackup.setText("Never");
            if (lastFile != null) lastFile.setText("");
            return;
        }

        CharSequence relative = android.text.format.DateUtils
                .getRelativeTimeSpanString(lastAt);
        lastBackup.setText(relative);

        String name = BackupPrefs.getLastAutoBackupName(this);
        if (lastFile != null) {
            lastFile.setText(name != null ? name : "");
        }
    }

	private void showImportOptionsDialog(Uri fileUri) {
		ContextThemeWrapper wrapper = new ContextThemeWrapper(
            this,
            com.google.android.material.R.style.Theme_Material3_DayNight);

		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(wrapper);
		View dialogView = LayoutInflater.from(wrapper).inflate(R.layout.dialog_import_backup, null);

		View optionSettingsOnly = dialogView.findViewById(R.id.option_settings_only);
		View optionNotesOnly = dialogView.findViewById(R.id.option_notes_only);
		View optionFullReplace = dialogView.findViewById(R.id.option_full_replace);
		View optionMerge = dialogView.findViewById(R.id.option_merge);
		View btnCancel = dialogView.findViewById(R.id.btn_cancel);

		builder.setView(dialogView);
		AlertDialog dialog = builder.create();

		if (dialog.getWindow() != null) {
			dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
		}

		optionSettingsOnly.setOnClickListener(v -> {
			dialog.dismiss();
			performImport(fileUri, ImportMode.SETTINGS_ONLY);
		});
		optionNotesOnly.setOnClickListener(v -> {
			dialog.dismiss();
			performImport(fileUri, ImportMode.NOTES_ONLY);
		});
		optionFullReplace.setOnClickListener(v -> {
			dialog.dismiss();
			performImport(fileUri, ImportMode.FULL_REPLACE);
		});
		optionMerge.setOnClickListener(v -> {
			dialog.dismiss();
			performImport(fileUri, ImportMode.MERGE);
		});
		btnCancel.setOnClickListener(v -> dialog.dismiss());

		dialog.show();
	}

	private enum ImportMode { SETTINGS_ONLY, NOTES_ONLY, FULL_REPLACE, MERGE }

	private void performImport(Uri fileUri, ImportMode mode) {
		if (mode == ImportMode.NOTES_ONLY || mode == ImportMode.FULL_REPLACE) {
			confirmImport(fileUri, mode);
		} else {
			runImport(fileUri, mode);
		}
	}

	private void confirmImport(Uri fileUri, ImportMode mode) {
		boolean isFullRestore = (mode == ImportMode.FULL_REPLACE);

		ContextThemeWrapper wrapper = new ContextThemeWrapper(
            this,
            com.google.android.material.R.style.Theme_Material3_DayNight);

		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(wrapper);
		View dialogView = LayoutInflater.from(wrapper).inflate(R.layout.dialog_import_confirm, null);

		TextView titleView = dialogView.findViewById(R.id.text_confirm_title);
		TextView messageView = dialogView.findViewById(R.id.text_confirm_message);
		View btnCancel = dialogView.findViewById(R.id.btn_cancel);
		View btnContinue = dialogView.findViewById(R.id.btn_continue);

		titleView.setText(isFullRestore
						  ? R.string.confirm_full_restore
						  : R.string.confirm_replace_notes);
		messageView.setText(isFullRestore
							? R.string.action_full_restore
							: R.string.action_replace_notes);

		builder.setView(dialogView);
		AlertDialog dialog = builder.create();

		if (dialog.getWindow() != null) {
			dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
		}

		btnCancel.setOnClickListener(v -> dialog.dismiss());
		btnContinue.setOnClickListener(v -> {
			dialog.dismiss();
			runImport(fileUri, mode);
		});

		dialog.show();
	}

	private void runImport(Uri fileUri, ImportMode mode) {
		Toast.makeText(this, "Importing…", Toast.LENGTH_SHORT).show();

		BackupManager.ImportMode bmMode = convertMode(mode);

		BackupManager.importFromUri(this, fileUri, bmMode,
            new BackupManager.ImportCallback() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> {
                        Toast.makeText(SettingsActivity.this,
									   message, Toast.LENGTH_LONG).show();

                        if (mode == ImportMode.SETTINGS_ONLY
							|| mode == ImportMode.FULL_REPLACE
							|| mode == ImportMode.MERGE) {
                            updateThemeSubtitle();
                            updateDefaultViewSubtitle();
                            recreate();
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> Toast.makeText(SettingsActivity.this,
													   "Import failed: " + message, Toast.LENGTH_LONG).show());
                }
            });
	}

	private BackupManager.ImportMode convertMode(ImportMode mode) {
		switch (mode) {
			case SETTINGS_ONLY: return BackupManager.ImportMode.SETTINGS_ONLY;
			case NOTES_ONLY: return BackupManager.ImportMode.NOTES_ONLY;
			case FULL_REPLACE: return BackupManager.ImportMode.FULL_REPLACE;
			case MERGE: return BackupManager.ImportMode.MERGE;
		}
		return BackupManager.ImportMode.SETTINGS_ONLY;
	}

    private void setupAppVersion() {
        TextView tv = findViewById(R.id.text_app_version);
        try {
            String versionName = getPackageManager()
				.getPackageInfo(getPackageName(), 0).versionName;
            tv.setText("Version " + versionName);
        } catch (Exception e) {}
        
    }
    
	private void openUrl(String url) {
    	try {
	        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    	    startActivity(intent);
	    } catch (Exception e) {
    	    Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show();
	    }
	}

	private void showFeedbackDialog() {
    	ContextThemeWrapper wrapper = new ContextThemeWrapper(this, com.google.android.material.R.style.Theme_Material3_DayNight);
	    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(wrapper);
    	View view = LayoutInflater.from(wrapper).inflate(R.layout.dialog_send_feedback, null);

	    builder.setView(view);
    	AlertDialog dialog = builder.create();
	    if (dialog.getWindow() != null) {
    	    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
	    }

	    view.findViewById(R.id.option_bug).setOnClickListener(v -> {
    	    dialog.dismiss();
	        openUrl(AppLinks.BUG_REPORT_URL);
    	});
	    view.findViewById(R.id.option_feature).setOnClickListener(v -> {
    	    dialog.dismiss();
        	openUrl(AppLinks.FEATURE_REQUEST_URL);
	    });
	    view.findViewById(R.id.option_general).setOnClickListener(v -> {
    	    dialog.dismiss();
        	openUrl(AppLinks.GENERAL_FEEDBACK_URL);
	    });
    	view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());

 	   dialog.show();
	}

	private void showLibrariesDialog() {
    	ContextThemeWrapper wrapper = new ContextThemeWrapper(this, com.google.android.material.R.style.Theme_Material3_DayNight);
	    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(wrapper);
    	View view = LayoutInflater.from(wrapper).inflate(R.layout.dialog_libraries, null);

	    builder.setView(view);
    	AlertDialog dialog = builder.create();
	    if (dialog.getWindow() != null) {
    	    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
	    }

	    LinearLayout container = view.findViewById(R.id.container_libraries);
    	AppLinks.Library[] libraries = AppLinks.getLibraries();

	    for (AppLinks.Library lib : libraries) {
            TextView tv = new TextView(this);
            tv.setText(lib.name);
            tv.setTextSize(16f);
            tv.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.md_text_primary));
    
            int padding = (int) (8 * getResources().getDisplayMetrics().density);
            tv.setPadding(padding, padding, padding, padding);
    
            tv.setBackgroundResource(R.drawable.option_bg);
            tv.setClickable(true);
            tv.setFocusable(true);
    
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, (int) (4 * getResources().getDisplayMetrics().density)); 
            tv.setLayoutParams(lp);

            tv.setOnClickListener(v -> openUrl(lib.url));
            container.addView(tv);
       }

	    view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
    	dialog.show();
	}

    private int findClosestIndex(int[] arr, int value) {
        int best = 0;
        for (int i = 0; i < arr.length; i++) {
            if (Math.abs(arr[i] - value) < Math.abs(arr[best] - value)) best = i;
        }
        return best;
    }
    
    private void updateHistoryCount() {
        TextView tv = findViewById(R.id.text_backup_history_count);
        if (tv == null) return;
            new Thread(() -> {
            int count = com.ar.markdowneditor.util.BackupDirectoryHelper.listBackups(this).length;
            runOnUiThread(() -> tv.setText(getString(R.string.backup_count, count)));
        }).start();
    }
	
	@Override
    protected void onResume() {
        super.onResume();
        if (navView != null) {
            navView.setCheckedItem(R.id.nav_settings);
        }
		updateLastBackupInfo();
		updateHistoryCount();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}