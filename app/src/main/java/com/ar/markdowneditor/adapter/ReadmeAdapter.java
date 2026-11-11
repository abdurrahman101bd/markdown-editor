package com.ar.markdowneditor.adapter;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.ar.markdowneditor.R;
import com.ar.markdowneditor.data.ReadmeEntity;
import com.ar.markdowneditor.util.ViewModePrefs;
import com.ar.markdowneditor.util.CardSelectionHelper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReadmeAdapter extends RecyclerView.Adapter<ReadmeAdapter.VH> {

    public interface Listener {
        void onClick(ReadmeEntity item);
        void onLongClick(ReadmeEntity item);
        void onSelectionChanged(int count);
    }

    private List<ReadmeEntity> items = new ArrayList<>();
    private final Set<Long> selectedIds = new HashSet<>();
    private boolean selectionMode = false;
    private final Listener listener;
	private int viewMode = ViewModePrefs.MODE_LIST;
	
	private static final int VIEW_TYPE_LIST = 0;
	private static final int VIEW_TYPE_GRID = 1;
	
    public ReadmeAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<ReadmeEntity> newItems) {	
		List<ReadmeEntity> filtered = new ArrayList<>();
		for (ReadmeEntity e : newItems) {
			boolean titleEmpty = e.title == null || e.title.trim().isEmpty();
			boolean contentEmpty = e.content == null || e.content.trim().isEmpty();
			if (!titleEmpty || !contentEmpty) {
				filtered.add(e);
			}
		}

		DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
				@Override public int getOldListSize() { return items.size(); }
				@Override public int getNewListSize() { return filtered.size(); }
				@Override public boolean areItemsTheSame(int oldPos, int newPos) {
					return items.get(oldPos).id == filtered.get(newPos).id;
				}
				@Override public boolean areContentsTheSame(int oldPos, int newPos) {
					ReadmeEntity a = items.get(oldPos), b = filtered.get(newPos);
					return a.title.equals(b.title) && a.content.equals(b.content) && a.updatedAt == b.updatedAt;
				}
			});
		items = filtered;
		result.dispatchUpdatesTo(this);
	}

    public void setSelectionMode(boolean enabled) {
        selectionMode = enabled;
        if (!enabled) selectedIds.clear();
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }
	
	public void setViewMode(int mode) {
		if (this.viewMode != mode) {
			this.viewMode = mode;
			notifyDataSetChanged();
		}
	}

    public List<Long> getSelectedIds() {
        return new ArrayList<>(selectedIds);
    }

    public List<ReadmeEntity> getSelectedItems() {
        List<ReadmeEntity> result = new ArrayList<>();
        for (ReadmeEntity e : items) {
            if (selectedIds.contains(e.id)) result.add(e);
        }
        return result;
    }

    public void selectAll() {
        selectedIds.clear();
        for (ReadmeEntity e : items) {
            selectedIds.add(e.id);
        }
        listener.onSelectionChanged(selectedIds.size());
        notifyDataSetChanged();
    }

    private void toggleSelection(ReadmeEntity item) {
        if (selectedIds.contains(item.id)) selectedIds.remove(item.id);
        else selectedIds.add(item.id);
        listener.onSelectionChanged(selectedIds.size());
        notifyDataSetChanged();
    }
	
	
	@Override
	public int getItemViewType(int position) {
		return viewMode == ViewModePrefs.MODE_GRID ? VIEW_TYPE_GRID : VIEW_TYPE_LIST;
	}

    @NonNull
	@Override
	public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		int layoutRes = (viewType == VIEW_TYPE_GRID)
            ? R.layout.item_readme_grid
            : R.layout.item_readme;
		View v = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
		return new VH(v);
	}

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ReadmeEntity item = items.get(position);
        if (item.title == null || item.title.trim().isEmpty()) {
            holder.title.setVisibility(View.GONE);
        } else {
        holder.title.setVisibility(View.VISIBLE);
        holder.title.setText(item.title);
        }
        holder.preview.setText(item.content);
        holder.date.setText(DateUtils.getRelativeTimeSpanString(item.updatedAt));

        boolean isSelected = selectedIds.contains(item.id);
        CardSelectionHelper.applySelection(holder.itemView, isSelected);

        holder.checkBox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(isSelected);

        holder.itemView.setOnClickListener(v -> {
            if (selectionMode) toggleSelection(item);
            else listener.onClick(item);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (!selectionMode) listener.onLongClick(item);
            toggleSelection(item);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, preview, date;
        CheckBox checkBox;
        VH(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_title);
            preview = itemView.findViewById(R.id.text_preview);
            date = itemView.findViewById(R.id.text_date);
            checkBox = itemView.findViewById(R.id.check_select);
        }
    }
}
