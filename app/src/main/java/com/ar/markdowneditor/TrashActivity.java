package com.ar.markdowneditor;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ar.markdowneditor.adapter.ReadmeAdapter;
import com.ar.markdowneditor.data.ReadmeEntity;
import com.ar.markdowneditor.data.ReadmeRepository;
import com.ar.markdowneditor.util.ThemeUtils;
import com.ar.markdowneditor.util.PrefsHelper;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.ar.markdowneditor.util.ViewModePrefs;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;

public class TrashActivity extends BaseActivity implements ReadmeAdapter.Listener {

    private RecyclerView recyclerView;
    private ReadmeAdapter adapter;
    private ReadmeRepository repository;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);
        setupDrawer(R.id.nav_trash);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        repository = new ReadmeRepository(this);

		int autoRemoveDays = PrefsHelper.getAutoremoveDays(this);
		if (autoRemoveDays > 0) {
			repository.purgeOldTrash(autoRemoveDays);
		}

        recyclerView = findViewById(R.id.recycler_readmes);
		adapter = new ReadmeAdapter(this);
		recyclerView.setAdapter(adapter);
		applyViewMode(); 

        repository.getTrashLive().observe(this, list -> {
            adapter.submitList(list);
            findViewById(R.id.text_empty).setVisibility(
				(list == null || list.isEmpty()) ? View.VISIBLE : View.GONE);
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
        if (navView != null) navView.setCheckedItem(R.id.nav_trash);

		applyViewMode(); 

        if (adapter != null && adapter.isSelectionMode()) {
            adapter.setSelectionMode(false);
            setupMainToolbar();
        }
    }

    private void setupMainToolbar() {
        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.menu_trash_main);
        toolbar.setTitle("Trash");
        toolbar.setNavigationIcon(R.drawable.ic_menu);

        int iconColor = ContextCompat.getColor(this, R.color.md_toolbar_icon);
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(iconColor);
        }

        Menu menu = toolbar.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getIcon() != null) {
                item.getIcon().setTint(iconColor);
            }
        }

        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_empty_trash) {
                confirmEmptyTrash();
                return true;
            }
            return false;
        });
    }

    private void setupSelectionToolbar() {
        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.menu_trash_selection);
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
                repository.restoreFromTrash(ids);
                Toast.makeText(this, ids.size() + " restored", Toast.LENGTH_SHORT).show();
            }
            adapter.setSelectionMode(false);
            setupMainToolbar();
            return true;
        } else if (id == R.id.action_delete_forever) {
            if (!selected.isEmpty()) {
                confirmDeleteForever(selected);
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
        intent.putExtra("from_trash", true);
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

    private void confirmEmptyTrash() {
		if (adapter.getItemCount() == 0) {
			Toast.makeText(this, "Trash is already empty", Toast.LENGTH_SHORT).show();
			return;
		}

		ContextThemeWrapper wrapper = new ContextThemeWrapper(
            this,
            com.google.android.material.R.style.Theme_Material3_DayNight);

		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(wrapper);
		View dialogView = LayoutInflater.from(wrapper).inflate(R.layout.dialog_empty_trash, null);

		View btnCancel = dialogView.findViewById(R.id.btn_cancel);
		View btnEmpty  = dialogView.findViewById(R.id.btn_empty);

		builder.setView(dialogView);
		AlertDialog dialog = builder.create();

		if (dialog.getWindow() != null) {
			dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
		}

		btnCancel.setOnClickListener(v -> dialog.dismiss());
		btnEmpty.setOnClickListener(v -> {
			repository.emptyTrash();
			Toast.makeText(this, "Trash emptied", Toast.LENGTH_SHORT).show();
			dialog.dismiss();
		});

		dialog.show();
	}

    private void confirmDeleteForever(List<ReadmeEntity> selected) {
		ContextThemeWrapper wrapper = new ContextThemeWrapper(
            this,
            com.google.android.material.R.style.Theme_Material3_DayNight);

		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(wrapper);
		View dialogView = LayoutInflater.from(wrapper).inflate(R.layout.dialog_delete_forever, null);

		View btnCancel = dialogView.findViewById(R.id.btn_cancel);
		View btnDelete = dialogView.findViewById(R.id.btn_delete);

		builder.setView(dialogView);
		AlertDialog dialog = builder.create();

		if (dialog.getWindow() != null) {
			dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
		}

		btnCancel.setOnClickListener(v -> dialog.dismiss());
		btnDelete.setOnClickListener(v -> {
			List<Long> ids = new ArrayList<>();
			for (ReadmeEntity e : selected) ids.add(e.id);
			repository.deleteByIds(ids);
			Toast.makeText(this, ids.size() + " deleted", Toast.LENGTH_SHORT).show();
			adapter.setSelectionMode(false);
			setupMainToolbar();
			dialog.dismiss();
		});

		dialog.show();
	}
}
