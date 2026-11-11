package com.ar.markdowneditor;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.ar.markdowneditor.adapter.ReadmeAdapter;
import com.ar.markdowneditor.data.ReadmeEntity;
import com.ar.markdowneditor.data.ReadmeRepository;
import com.ar.markdowneditor.util.ThemeUtils;
import com.ar.markdowneditor.util.ViewModePrefs;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.List;

public class ArchiveActivity extends BaseActivity implements ReadmeAdapter.Listener {

    private RecyclerView recyclerView;
    private ReadmeAdapter adapter;
    private ReadmeRepository repository;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive);
        setupDrawer(R.id.nav_archive);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        repository = new ReadmeRepository(this);

        recyclerView = findViewById(R.id.recycler_readmes);
        adapter = new ReadmeAdapter(this);
        recyclerView.setAdapter(adapter);
        applyViewMode();

        repository.getArchiveLive().observe(this, list -> {
            adapter.submitList(list);
            findViewById(R.id.text_empty).setVisibility(
				(list == null || list.isEmpty()) ? android.view.View.VISIBLE : android.view.View.GONE);
        });

        setupMainToolbar();
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

    @Override
    protected void onResume() {
        super.onResume();
        if (navView != null) navView.setCheckedItem(R.id.nav_archive);

        applyViewMode();

        if (adapter != null && adapter.isSelectionMode()) {
            adapter.setSelectionMode(false);
            setupMainToolbar();
        }
    }

    private void setupMainToolbar() {
        toolbar.getMenu().clear();
        toolbar.setTitle("Archive");
        toolbar.setNavigationIcon(R.drawable.ic_menu);

        int iconColor = ContextCompat.getColor(this, R.color.md_toolbar_icon);
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(iconColor);
        }

        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        toolbar.setOnMenuItemClickListener(null);
    }

    private void setupSelectionToolbar() {
        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.menu_archive_selection);
        toolbar.setNavigationIcon(R.drawable.ic_close);
        toolbar.setTitle("0 selected");

        int iconColor = ContextCompat.getColor(this, R.color.md_text_primary);

        Menu menu = toolbar.getMenu();
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
            setupMainToolbar();
        });
        toolbar.setOnMenuItemClickListener(this::onSelectionMenuItem);
    }

    private boolean onSelectionMenuItem(MenuItem item) {
        int id = item.getItemId();
        List<ReadmeEntity> selected = adapter.getSelectedItems();

        if (id == R.id.action_select_all) {
            adapter.selectAll();
            return true;
        } else if (id == R.id.action_restore) {
            if (!selected.isEmpty()) {
                List<Long> ids = new ArrayList<>();
                for (ReadmeEntity e : selected) ids.add(e.id);
                repository.unarchiveIds(ids);
                Toast.makeText(this, ids.size() + " unarchived", Toast.LENGTH_SHORT).show();
            }
            adapter.setSelectionMode(false);
            setupMainToolbar();
            return true;
        } else if (id == R.id.action_delete) {
            if (!selected.isEmpty()) {
                moveToTrashWithUndo(selected);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onClick(ReadmeEntity item) {
        if (adapter.isSelectionMode()) return;

        Intent intent = new Intent(this, EditorActivity.class);
        intent.putExtra(MainActivity.EXTRA_READMES_ID, item.id);
        startActivity(intent);
    }

    @Override
    public void onLongClick(ReadmeEntity item) {
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

    private void moveToTrashWithUndo(List<ReadmeEntity> selected) {
		if (selected.isEmpty()) return;

		List<Long> ids = new ArrayList<>();
		for (ReadmeEntity e : selected) ids.add(e.id);

		repository.moveToTrash(ids);

		adapter.setSelectionMode(false);
		setupMainToolbar();

		String message = selected.size() == 1
            ? "Moved to trash"
            : selected.size() + " items moved to trash";

		Snackbar snackbar = Snackbar.make(recyclerView, message, Snackbar.LENGTH_LONG);
		snackbar.setAction("UNDO", v -> repository.restoreFromTrash(ids));
		snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.md_on_primary));
        snackbar.setTextColor(ContextCompat.getColor(this, R.color.md_on_secondary));
        snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.md_primary));
		snackbar.show();
	}
}