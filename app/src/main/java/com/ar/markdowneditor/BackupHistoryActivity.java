package com.ar.markdowneditor;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ar.markdowneditor.adapter.BackupHistoryAdapter;
import com.ar.markdowneditor.util.BackupDirectoryHelper;
import com.ar.markdowneditor.util.BackupInfo;
import com.ar.markdowneditor.util.BackupManager;
import com.ar.markdowneditor.util.BackupPrefs;
import com.ar.markdowneditor.util.ThemeUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BackupHistoryActivity extends BaseActivity implements BackupHistoryAdapter.Listener {

    private RecyclerView recyclerView;
    private BackupHistoryAdapter adapter;
    private ProgressBar progress;
    private View emptyState;
    private Toolbar toolbar;
    private List<BackupInfo> allBackups = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_history);
        
        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        recyclerView = findViewById(R.id.recycler_backups);
        progress = findViewById(R.id.progress_loading);
        emptyState = findViewById(R.id.empty_state);

        adapter = new BackupHistoryAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        setupMainToolbar();
        loadBackups();
    }

    private void setupMainToolbar() {
		toolbar.getMenu().clear();
		toolbar.inflateMenu(R.menu.menu_backup_history_main);
		toolbar.setTitle("Backup History");
		toolbar.setNavigationIcon(R.drawable.ic_arrow_back);  

		int iconColor = ContextCompat.getColor(this, R.color.md_toolbar_icon);
		if (toolbar.getNavigationIcon() != null) {
			toolbar.getNavigationIcon().setTint(iconColor);
		}
		Menu menu = toolbar.getMenu();
		for (int i = 0; i < menu.size(); i++) {
			MenuItem item = menu.getItem(i);
			if (item.getIcon() != null) item.getIcon().setTint(iconColor);
		}

		toolbar.setNavigationOnClickListener(v -> finish());

		toolbar.setOnMenuItemClickListener(item -> {
			if (item.getItemId() == R.id.action_delete_all) {
				confirmDeleteAll();
				return true;
			}
			return false;
		});
	}

    private void setupSelectionToolbar() {
        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.menu_backup_history);
        toolbar.setNavigationIcon(R.drawable.ic_close);
        toolbar.setTitle("0 selected");

        int iconColor = ContextCompat.getColor(this, R.color.md_text_primary);
        Menu menu = toolbar.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getIcon() != null) item.getIcon().setTint(iconColor);
        }
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(iconColor);
        }

        toolbar.setNavigationOnClickListener(v -> {
            adapter.setSelectionMode(false);
            setupMainToolbar();
        });
        toolbar.setOnMenuItemClickListener(this::onSelectionMenuItem);
    }

    private boolean onSelectionMenuItem(MenuItem item) {
        int id = item.getItemId();
        List<BackupInfo> selected = adapter.getSelectedItems();

        if (id == R.id.action_select_all) {
            adapter.selectAll();
            return true;
        } else if (id == R.id.action_delete) {
            if (!selected.isEmpty()) confirmDeleteSelected(selected);
            return true;
        }
        return false;
    }

    private void loadBackups() {
        progress.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);

        new Thread(() -> {
            File[] files = BackupDirectoryHelper.listBackups(this);
            List<BackupInfo> infos = new ArrayList<>();
            for (File f : files) {
                infos.add(BackupManager.readBackupInfo(f));
            }

            runOnUiThread(() -> {
                allBackups = infos;
                adapter.submitList(infos);
                progress.setVisibility(View.GONE);

                if (infos.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }

    @Override
    public void onClick(BackupInfo info) {
        if (adapter.isSelectionMode()) return;
        showDetailsDialog(info);
    }

    @Override
    public void onLongClick(BackupInfo info) {
        adapter.setSelectionMode(true);
        setupSelectionToolbar();
    }

    @Override
    public void onSelectionChanged(int count) {
        if (count == 0) {
            adapter.setSelectionMode(false);
            setupMainToolbar();
        } else {
            toolbar.setTitle(count + " selected");
        }
    }

    private void showDetailsDialog(BackupInfo info) {
        ContextThemeWrapper wrapper = new ContextThemeWrapper(
			this,
			com.google.android.material.R.style.Theme_Material3_DayNight);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(wrapper);
        View view = LayoutInflater.from(wrapper).inflate(R.layout.dialog_backup_details, null);

        TextView fileView = view.findViewById(R.id.text_detail_file);
        TextView activeView = view.findViewById(R.id.text_detail_active);
        TextView archivedView = view.findViewById(R.id.text_detail_archived);
        TextView trashView = view.findViewById(R.id.text_detail_trash);
        TextView settingsView = view.findViewById(R.id.text_detail_settings);
        View btnClose = view.findViewById(R.id.btn_close);
        View btnImport = view.findViewById(R.id.btn_import);

        fileView.setText(info.fileName + "\n" +
						 android.text.format.DateUtils.getRelativeTimeSpanString(info.modifiedAt) +
						 " • " + humanSize(info.sizeBytes));

        activeView.setText(getString(R.string.active_count, info.activeCount));
        archivedView.setText(getString(R.string.archived_count, info.archivedCount));
        trashView.setText(getString(R.string.trash_count, info.trashCount));

        String themeName = info.theme == 1 ? "Light" : info.theme == 2 ? "Dark" : "System";
        String viewName = info.viewMode == 0 ? "List" : "Grid";
        String settingsText =
			"Theme: " + themeName + "\n" +
			"View: " + viewName + "\n" +
			"Autosave: " + info.autosaveSec + "s\n" +
			"Auto-remove: " + (info.autoremoveDays == 0 ? "Off" : info.autoremoveDays + " days") + "\n" +
            "Auto backup: " + (info.autobackupDays == 0 ? "Off" : info.autobackupDays + " days") + "\n" +
			"Screenshot: " + (info.screenshotAllowed ? "Allowed" : "Disallowed") + "\n" +
			"Biometric: " + (info.biometricEnabled ? "Enabled" : "Disabled");
        settingsView.setText(settingsText);
       
        builder.setView(view);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnImport.setOnClickListener(v -> {
            dialog.dismiss();
            startImportForBackup(info);
        });

        dialog.show();
    }

    private void startImportForBackup(BackupInfo info) {
		ContextThemeWrapper wrapper = new ContextThemeWrapper(
            this,
            com.google.android.material.R.style.Theme_Material3_DayNight);

		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(wrapper);
		View dialogView = LayoutInflater.from(wrapper).inflate(R.layout.dialog_import_backup, null);

		View optionSettingsOnly = dialogView.findViewById(R.id.option_settings_only);
		View optionNotesOnly    = dialogView.findViewById(R.id.option_notes_only);
		View optionFullReplace  = dialogView.findViewById(R.id.option_full_replace);
		View optionMerge        = dialogView.findViewById(R.id.option_merge);
		View btnCancel          = dialogView.findViewById(R.id.btn_cancel);

		builder.setView(dialogView);
		AlertDialog dialog = builder.create();

		if (dialog.getWindow() != null) {
			dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
		}

		optionSettingsOnly.setOnClickListener(v -> {
			dialog.dismiss();
			performImportWithConfirm(info, BackupManager.ImportMode.SETTINGS_ONLY);
		});
		optionNotesOnly.setOnClickListener(v -> {
			dialog.dismiss();
			performImportWithConfirm(info, BackupManager.ImportMode.NOTES_ONLY);
		});
		optionFullReplace.setOnClickListener(v -> {
			dialog.dismiss();
			performImportWithConfirm(info, BackupManager.ImportMode.FULL_REPLACE);
		});
		optionMerge.setOnClickListener(v -> {
			dialog.dismiss();
			performImportWithConfirm(info, BackupManager.ImportMode.MERGE);
		});
		btnCancel.setOnClickListener(v -> dialog.dismiss());

		dialog.show();
	}
	
	private void performImportWithConfirm(BackupInfo info, BackupManager.ImportMode mode) {
		if (mode != BackupManager.ImportMode.NOTES_ONLY
            && mode != BackupManager.ImportMode.FULL_REPLACE) {
			performImport(info, mode);
			return;
		}

		boolean isFullRestore = (mode == BackupManager.ImportMode.FULL_REPLACE);

		ContextThemeWrapper wrapper = new ContextThemeWrapper(
            this,
            com.google.android.material.R.style.Theme_Material3_DayNight);

		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(wrapper);
		View dialogView = LayoutInflater.from(wrapper).inflate(R.layout.dialog_import_confirm, null);

		TextView titleView   = dialogView.findViewById(R.id.text_confirm_title);
		TextView messageView = dialogView.findViewById(R.id.text_confirm_message);
		View btnCancel       = dialogView.findViewById(R.id.btn_cancel);
		View btnContinue     = dialogView.findViewById(R.id.btn_continue);

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
			performImport(info, mode);
		});

		dialog.show();
	}

    private void performImport(BackupInfo info, BackupManager.ImportMode mode) {
		Uri fileUri = Uri.fromFile(info.file);
		Toast.makeText(this, "Importing…", Toast.LENGTH_SHORT).show();

		BackupManager.importFromUri(this, fileUri, mode,
            new BackupManager.ImportCallback() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> {
                        Toast.makeText(BackupHistoryActivity.this,
									   message, Toast.LENGTH_LONG).show();

                        if (mode == BackupManager.ImportMode.SETTINGS_ONLY
							|| mode == BackupManager.ImportMode.FULL_REPLACE
							|| mode == BackupManager.ImportMode.MERGE) {                                                      
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> Toast.makeText(BackupHistoryActivity.this,
													   "Import failed: " + message, Toast.LENGTH_LONG).show());
                }
            });
	}

    private void confirmDeleteSelected(List<BackupInfo> selected) {
		ContextThemeWrapper wrapper = new ContextThemeWrapper(
            this,
            com.google.android.material.R.style.Theme_Material3_DayNight);

		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(wrapper);
		View dialogView = LayoutInflater.from(wrapper).inflate(R.layout.dialog_delete_backup, null);

		TextView titleView   = dialogView.findViewById(R.id.text_delete_title);
		TextView messageView = dialogView.findViewById(R.id.text_delete_message);
		View btnCancel       = dialogView.findViewById(R.id.btn_cancel);
		View btnDelete       = dialogView.findViewById(R.id.btn_delete);

		titleView.setText(R.string.confirm_delete_backup);
		messageView.setText(selected.size() + " backup(s) will be permanently deleted. This cannot be undone.");

		builder.setView(dialogView);
		AlertDialog dialog = builder.create();

		if (dialog.getWindow() != null) {
			dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
		}

		btnCancel.setOnClickListener(v -> dialog.dismiss());
		btnDelete.setOnClickListener(v -> {
            for (BackupInfo b : selected) BackupManager.deleteBackup(b.file);

            if (BackupDirectoryHelper.listBackups(this).length == 0) {
                BackupPrefs.clearLastAutoBackup(this);
            }

            adapter.setSelectionMode(false);
            setupMainToolbar();
            loadBackups();
            Toast.makeText(this, selected.size() + " deleted", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

		dialog.show();
	}

    private void confirmDeleteAll() {
		if (allBackups.isEmpty()) {
			Toast.makeText(this, "No backups to delete", Toast.LENGTH_SHORT).show();
			return;
		}

		ContextThemeWrapper wrapper = new ContextThemeWrapper(
            this,
            com.google.android.material.R.style.Theme_Material3_DayNight);

		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(wrapper);
		View dialogView = LayoutInflater.from(wrapper).inflate(R.layout.dialog_delete_backup, null);

		TextView titleView   = dialogView.findViewById(R.id.text_delete_title);
		TextView messageView = dialogView.findViewById(R.id.text_delete_message);
		View btnCancel       = dialogView.findViewById(R.id.btn_cancel);
		View btnDelete       = dialogView.findViewById(R.id.btn_delete);

		titleView.setText(R.string.confirm_delete_all_backups);
		messageView.setText("All " + allBackups.size() + " backup(s) will be permanently deleted. This cannot be undone.");

		builder.setView(dialogView);
		AlertDialog dialog = builder.create();

		if (dialog.getWindow() != null) {
			dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
		}

		btnCancel.setOnClickListener(v -> dialog.dismiss());
		btnDelete.setOnClickListener(v -> {
            BackupDirectoryHelper.deleteAllBackups(this);
            BackupPrefs.clearLastAutoBackup(this);
            loadBackups();
            Toast.makeText(this, "All backups deleted", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

		dialog.show();
	}

    private String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (navView != null) navView.setCheckedItem(R.id.nav_settings);
    }

    @Override
	public void onBackPressed() {
		if (adapter != null && adapter.isSelectionMode()) {
			adapter.setSelectionMode(false);
			setupMainToolbar();
		} else {
			super.onBackPressed();
		}
	}
}