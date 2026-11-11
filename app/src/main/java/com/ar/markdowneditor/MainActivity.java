package com.ar.markdowneditor;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.recyclerview.widget.GridLayoutManager;
import android.view.Menu;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ar.markdowneditor.adapter.ReadmeAdapter;
import com.ar.markdowneditor.data.ReadmeEntity;
import com.ar.markdowneditor.data.ReadmeRepository;
import com.ar.markdowneditor.util.MarkdownExporter;
import com.ar.markdowneditor.util.ThemeUtils;
import com.ar.markdowneditor.util.ViewModePrefs;
import com.ar.markdowneditor.util.BackupScheduler;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import android.view.LayoutInflater;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends BaseActivity implements ReadmeAdapter.Listener {

    private int lastListSize = 0;
    public static final String EXTRA_READMES_ID = "extra_readme_id";

    private static final String PREFS_EXPORT = "export_prefs";
    private static final String KEY_EXPORT_TREE_URI = "export_tree_uri";
    private static final String EXPORT_FOLDER_NAME = "Markdown Editor";

    private RecyclerView recyclerView;
    private ReadmeAdapter adapter;
    private ReadmeRepository repository;
    private Toolbar toolbar;

    private ActivityResultLauncher<Intent> folderPickerLauncher;
    private List<ReadmeEntity> pendingExportItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupDrawer(R.id.nav_all_notes);

        repository = new ReadmeRepository(this);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        recyclerView = findViewById(R.id.recycler_readmes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReadmeAdapter(this);
        recyclerView.setAdapter(adapter);
		applyViewMode();

        repository.getAllLive().observe(this, list -> {
            adapter.submitList(list);
            findViewById(R.id.empty_state).setVisibility(  
				list.isEmpty() ? View.VISIBLE : View.GONE);

            int newSize = list == null ? 0 : list.size();
            if (newSize > lastListSize) {
                recyclerView.scrollToPosition(0);
            }
            lastListSize = newSize;
        });

        FloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> createNewReadme());

        setupFolderPickerLauncher();
        BackupScheduler.schedule(this);
        
    }
	
	private void applyViewMode() {
        int mode = ViewModePrefs.getMode(this);
        adapter.setViewMode(mode);

        if (mode == ViewModePrefs.MODE_GRID) {
            StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(
                    2, StaggeredGridLayoutManager.VERTICAL);
            sglm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
            recyclerView.setLayoutManager(sglm);
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    private void setupFolderPickerLauncher() {
        folderPickerLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
		result -> {
			if (result.getResultCode() != RESULT_OK || result.getData() == null) {
				pendingExportItems = null;
				return;
			}
			Uri treeUri = result.getData().getData();
			if (treeUri == null) {
				pendingExportItems = null;
				return;
			}

			final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
				| Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
			try {
				getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
			} catch (Exception ignored) {}

			getSharedPreferences(PREFS_EXPORT, MODE_PRIVATE)
				.edit()
				.putString(KEY_EXPORT_TREE_URI, treeUri.toString())
				.apply();

			if (pendingExportItems != null) {
				writeFilesToTree(treeUri, pendingExportItems);
				pendingExportItems = null;
			}
		});
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (navView != null) {
            navView.setCheckedItem(R.id.nav_all_notes);
        }
		applyViewMode();
    }

    private void createNewReadme() {
        openEditor(-1);
    }

    private void openEditor(long id) {
        Intent intent = new Intent(this, EditorActivity.class);
        intent.putExtra(EXTRA_READMES_ID, id);
        startActivity(intent);
    }
    
    @Override
    public void onClick(ReadmeEntity item) {
        androidx.appcompat.app.AlertDialog loadingDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(new android.widget.ProgressBar(this))
                .setCancelable(false)
                .create();
                
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            }                
                
        loadingDialog.show();

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            openEditor(item.id);
            loadingDialog.dismiss();
        }, 300);
    }

    @Override
    public void onLongClick(ReadmeEntity item) {
        adapter.setSelectionMode(true);
        startSelectionToolbar();
    }

    @Override
    public void onSelectionChanged(int count) {
        if (count == 0) {
            adapter.setSelectionMode(false);
            resetToolbar();
        } else {
            toolbar.setTitle(count + " selected");
        }
    }

    private void startSelectionToolbar() {
        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.menu_selection);
        toolbar.setNavigationIcon(R.drawable.ic_close);

        Menu menu = toolbar.getMenu();
        int iconColor = ContextCompat.getColor(this, R.color.md_text_primary);
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getIcon() != null) {
                item.getIcon().setTint(iconColor);
            }
        }
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(iconColor);
        }

        toolbar.setNavigationOnClickListener(v -> {
            adapter.setSelectionMode(false);
            resetToolbar();
        });
        toolbar.setOnMenuItemClickListener(this::onSelectionMenuItem);
        toolbar.setTitle("1 selected");
    }

    private void resetToolbar() {
        toolbar.getMenu().clear();
        toolbar.setTitle(R.string.app_name);
        toolbar.setNavigationIcon(R.drawable.ic_menu);
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(ContextCompat.getColor(this, R.color.md_toolbar_icon));
        }
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
    }

    private boolean onSelectionMenuItem(MenuItem item) {
        int id = item.getItemId();
        List<ReadmeEntity> selected = adapter.getSelectedItems();

        if (id == R.id.action_delete) {
            deleteWithUndo(selected);
            adapter.setSelectionMode(false);
            resetToolbar();
            return true;
        } else if (id == R.id.action_copy) {
            copyToClipboard(selected);
            adapter.setSelectionMode(false);
            resetToolbar();
            return true;
        } else if (id == R.id.action_export) {
            exportSelected(selected);
            adapter.setSelectionMode(false);
            resetToolbar();
            return true;
        } else if (id == R.id.action_archive) {
            if (!selected.isEmpty()) {
                List<Long> ids = new ArrayList<>();
                for (ReadmeEntity e : selected) ids.add(e.id);
                repository.archiveIds(ids);
                Toast.makeText(this, ids.size() + " archived", Toast.LENGTH_SHORT).show();
            }
            adapter.setSelectionMode(false);
            resetToolbar();
            return true;
        } else if (id == R.id.action_select_all) {
            adapter.selectAll();
            return true;
        }
        return false;
    }

    private void deleteWithUndo(List<ReadmeEntity> selected) {
		List<Long> ids = new ArrayList<>();
		for (ReadmeEntity e : selected) ids.add(e.id);

		repository.moveToTrash(ids);

		String message = selected.size() == 1
            ? "Moved to trash"
            : selected.size() + " items moved to trash";

		Snackbar snackbar = Snackbar.make(recyclerView, message, Snackbar.LENGTH_LONG);
		snackbar.setAction("UNDO", v -> {
			repository.restoreFromTrash(ids);
		});
		snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.md_on_primary));
        snackbar.setTextColor(ContextCompat.getColor(this, R.color.md_on_secondary));
        snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.md_primary));
		snackbar.show();
	}

    private void copyToClipboard(List<ReadmeEntity> items) {
        StringBuilder sb = new StringBuilder();
        for (ReadmeEntity e : items) {
            sb.append(e.content).append("\n\n");
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("readme", sb.toString().trim()));
        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
    }

    // Export to Downloads/Markdown Editor
    private void exportSelected(List<ReadmeEntity> items) {
        if (items.isEmpty()) {
            Toast.makeText(this, "Nothing selected", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_EXPORT, MODE_PRIVATE);
        String savedUri = prefs.getString(KEY_EXPORT_TREE_URI, null);

        if (savedUri != null) {
            Uri treeUri = Uri.parse(savedUri);
            try {
                DocumentFile tree = DocumentFile.fromTreeUri(this, treeUri);
                if (tree != null && tree.canWrite()) {
                    writeFilesToTree(treeUri, items);
                    return;
                }
            } catch (Exception ignored) {}
        }

        pendingExportItems = items;
        openFolderPicker();
    }

    private void openFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
						| Intent.FLAG_GRANT_WRITE_URI_PERMISSION
						| Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
						| Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        folderPickerLauncher.launch(intent);
    }

    private void writeFilesToTree(Uri treeUri, List<ReadmeEntity> items) {
        new Thread(() -> {
            int success = 0;
            int failed = 0;

            try {
                DocumentFile tree = DocumentFile.fromTreeUri(this, treeUri);
                if (tree == null || !tree.canWrite()) {
                    runOnUiThread(() -> Toast.makeText(this,
													   "Cannot write to folder", Toast.LENGTH_SHORT).show());
                    return;
                }

                DocumentFile subDir = null;
                for (DocumentFile f : tree.listFiles()) {
                    if (f.isDirectory() && EXPORT_FOLDER_NAME.equals(f.getName())) {
                        subDir = f;
                        break;
                    }
                }
                if (subDir == null) {
                    subDir = tree.createDirectory(EXPORT_FOLDER_NAME);
                }
                if (subDir == null) {
                    runOnUiThread(() -> Toast.makeText(this,
													   "Failed to create folder", Toast.LENGTH_SHORT).show());
                    return;
                }

                for (ReadmeEntity e : items) {
                    try {
                        String safeTitle = (e.title == null || e.title.trim().isEmpty())
							? "untitled"
							: e.title.trim().replaceAll("[^a-zA-Z0-9-_ ]", "_");
                        String fileName = safeTitle + ".md";

                        DocumentFile existing = subDir.findFile(fileName);
                        if (existing != null) existing.delete();

                        DocumentFile file = subDir.createFile("text/markdown", fileName);
                        if (file == null) {
                            failed++;
                            continue;
                        }

                        try (OutputStream os = getContentResolver().openOutputStream(file.getUri())) {
                            if (os != null) {
                                os.write((e.content == null ? "" : e.content)
										 .getBytes(StandardCharsets.UTF_8));
                            }
                        }
                        success++;
                    } catch (Exception ex) {
                        failed++;
                    }
                }
            } catch (Exception ex) {
                runOnUiThread(() -> Toast.makeText(this,
												   "Export failed: " + ex.getMessage(), Toast.LENGTH_SHORT).show());
                return;
            }

            final int finalSuccess = success;
            final int finalFailed = failed;
            runOnUiThread(() -> {
                String msg = finalSuccess + " file(s) saved to " + EXPORT_FOLDER_NAME;
                if (finalFailed > 0) msg += ", " + finalFailed + " failed";
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    private void showExitDialog() {
        ContextThemeWrapper wrapper = new ContextThemeWrapper(
			this,
			com.google.android.material.R.style.Theme_Material3_DayNight);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(wrapper);
        View dialogView = LayoutInflater.from(wrapper).inflate(R.layout.dialog_exit, null);
        View btnCancel = dialogView.findViewById(R.id.btn_cancel);
        View btnExit = dialogView.findViewById(R.id.btn_exit);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnExit.setOnClickListener(v -> {
            dialog.dismiss();
            finishAffinity();
        });
        dialog.show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (adapter != null && adapter.isSelectionMode()) {
            adapter.setSelectionMode(false);
            resetToolbar();
        } else {
            showExitDialog();
        }
    }
}