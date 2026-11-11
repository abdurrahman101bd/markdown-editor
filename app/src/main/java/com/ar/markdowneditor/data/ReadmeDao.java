package com.ar.markdowneditor.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ReadmeDao {

    // MainActivity — active notes (not deleted, not archived)
    @Query("SELECT * FROM readmes WHERE isDeleted = 0 AND isArchived = 0 ORDER BY updatedAt DESC")
    LiveData<List<ReadmeEntity>> getAllLive();

    // TrashActivity
    @Query("SELECT * FROM readmes WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    LiveData<List<ReadmeEntity>> getTrashLive();

    // ArchiveActivity
    @Query("SELECT * FROM readmes WHERE isArchived = 1 AND isDeleted = 0 ORDER BY updatedAt DESC")
    LiveData<List<ReadmeEntity>> getArchiveLive();

    @Query("SELECT * FROM readmes WHERE id = :id")
    ReadmeEntity getByIdSync(long id);

    @Insert
    long insert(ReadmeEntity entity);

    @Update
    void update(ReadmeEntity entity);

    @Delete
    void delete(ReadmeEntity entity);

    @Query("DELETE FROM readmes WHERE id IN (:ids)")
    void deleteByIds(List<Long> ids);

    // Move to trash
    @Query("UPDATE readmes SET isDeleted = 1, deletedAt = :timestamp WHERE id IN (:ids)")
    void moveToTrash(List<Long> ids, long timestamp);

    // Restore from trash
    @Query("UPDATE readmes SET isDeleted = 0, deletedAt = 0 WHERE id IN (:ids)")
    void restoreFromTrash(List<Long> ids);

    // Archive / Unarchive
    @Query("UPDATE readmes SET isArchived = 1 WHERE id IN (:ids)")
    void archiveIds(List<Long> ids);

    @Query("UPDATE readmes SET isArchived = 0 WHERE id IN (:ids)")
    void unarchiveIds(List<Long> ids);

    // Auto-remove old trash
    @Query("DELETE FROM readmes WHERE isDeleted = 1 AND deletedAt < :cutoff")
    void purgeOldTrash(long cutoff);
    
    // For merge 
    @Query("SELECT * FROM readmes ORDER BY updatedAt DESC")
    List<ReadmeEntity> getAllSync();
    
    // Truncate all notes (for full replace)
    @Query("DELETE FROM readmes")
    void deleteAll();

}