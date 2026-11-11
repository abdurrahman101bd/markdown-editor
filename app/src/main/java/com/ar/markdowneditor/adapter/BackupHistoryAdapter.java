package com.ar.markdowneditor.adapter;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ar.markdowneditor.R;
import com.ar.markdowneditor.util.BackupInfo;
import com.ar.markdowneditor.util.CardSelectionHelper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BackupHistoryAdapter extends RecyclerView.Adapter<BackupHistoryAdapter.VH> {

    public interface Listener {
        void onClick(BackupInfo info);
        void onLongClick(BackupInfo info);
        void onSelectionChanged(int count);
    }

    private List<BackupInfo> items = new ArrayList<>();
    private final Set<String> selectedNames = new HashSet<>();
    private boolean selectionMode = false;
    private final Listener listener;

    public BackupHistoryAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<BackupInfo> newItems) {
        items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setSelectionMode(boolean enabled) {
        selectionMode = enabled;
        if (!enabled) selectedNames.clear();
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public List<BackupInfo> getSelectedItems() {
        List<BackupInfo> result = new ArrayList<>();
        for (BackupInfo b : items) {
            if (selectedNames.contains(b.fileName)) result.add(b);
        }
        return result;
    }

    public void selectAll() {
        selectedNames.clear();
        for (BackupInfo b : items) selectedNames.add(b.fileName);
        listener.onSelectionChanged(selectedNames.size());
        notifyDataSetChanged();
    }

    private void toggleSelection(BackupInfo info) {
        if (selectedNames.contains(info.fileName)) selectedNames.remove(info.fileName);
        else selectedNames.add(info.fileName);
        listener.onSelectionChanged(selectedNames.size());
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_backup, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        BackupInfo item = items.get(position);

        CharSequence relative = DateUtils.getRelativeTimeSpanString(item.modifiedAt);
        holder.date.setText(relative);

        String size = humanSize(item.sizeBytes);
        String summary;
        if (item.detailsLoaded) {
            summary = item.noteCount + " notes • " + size;
        } else {
            summary = size;
        }
        holder.summary.setText(summary);

        boolean isSelected = selectedNames.contains(item.fileName);
        CardSelectionHelper.applySelection(holder.itemView, isSelected);

        holder.checkBox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(isSelected);

        holder.itemView.setOnClickListener(v -> {
            if (selectionMode) toggleSelection(item);
            else listener.onClick(item);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (!selectionMode) {
                listener.onLongClick(item);
                toggleSelection(item);
            } else {
                toggleSelection(item);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView date, summary;
        CheckBox checkBox;
        VH(View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.text_backup_date);
            summary = itemView.findViewById(R.id.text_backup_summary);
            checkBox = itemView.findViewById(R.id.check_select);
        }
    }
}