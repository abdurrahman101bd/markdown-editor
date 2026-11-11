package com.ar.markdowneditor.data;

import android.content.Context;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReadmeRepository {

    private final ReadmeDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ReadmeRepository(Context context) {
        dao = AppDatabase.getInstance(context).readmeDao();
    }

    public LiveData<List<ReadmeEntity>> getAllLive() {
        return dao.getAllLive();
    }

    public LiveData<List<ReadmeEntity>> getTrashLive() {
        return dao.getTrashLive();
    }

    public LiveData<List<ReadmeEntity>> getArchiveLive() {
        return dao.getArchiveLive();
    }

    public interface OnInsertedListener {
        void onInserted(long id);
    }

    public void insert(ReadmeEntity entity, OnInsertedListener listener) {
        executor.execute(() -> {
            long id = dao.insert(entity);
            if (listener != null) listener.onInserted(id);
        });
    }

    public void update(ReadmeEntity entity) {
        executor.execute(() -> dao.update(entity));
    }

    public void deleteByIds(List<Long> ids) {
        executor.execute(() -> dao.deleteByIds(ids));
    }

    public void moveToTrash(List<Long> ids) {
        executor.execute(() -> dao.moveToTrash(ids, System.currentTimeMillis()));
    }

    public void restoreFromTrash(List<Long> ids) {
        executor.execute(() -> dao.restoreFromTrash(ids));
    }

    public void purgeOldTrash(long daysOld) {
        long cutoff = System.currentTimeMillis() - (daysOld * 24L * 60 * 60 * 1000);
        executor.execute(() -> dao.purgeOldTrash(cutoff));
    }

    public void emptyTrash() {
        executor.execute(() -> dao.purgeOldTrash(System.currentTimeMillis() + 1000));
    }

    public void archiveIds(List<Long> ids) {
        executor.execute(() -> dao.archiveIds(ids));
    }

    public void unarchiveIds(List<Long> ids) {
        executor.execute(() -> dao.unarchiveIds(ids));
    }

    public interface OnLoadedListener {
        void onLoaded(ReadmeEntity entity);
    }

    public void getByIdAsync(long id, OnLoadedListener listener) {
        executor.execute(() -> {
            ReadmeEntity e = dao.getByIdSync(id);
            listener.onLoaded(e);
        });
    }

	public void deleteAllNotes() {
		executor.execute(() -> dao.deleteAll());
	}

	public interface OnListLoadedListener {
		void onLoaded(List<ReadmeEntity> list);
	}

	public void getAllSync(OnListLoadedListener listener) {
		executor.execute(() -> {
			List<ReadmeEntity> list = dao.getAllSync();
			listener.onLoaded(list);
		});
	}

	public void insertAll(List<ReadmeEntity> entities, Runnable onComplete) {
		executor.execute(() -> {
			for (ReadmeEntity e : entities) {
				e.id = 0;
				dao.insert(e);
			}
			if (onComplete != null) onComplete.run();
		});
	}
}