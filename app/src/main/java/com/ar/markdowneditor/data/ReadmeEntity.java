package com.ar.markdowneditor.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "readmes")
public class ReadmeEntity {

    public boolean isDeleted = false;
    public boolean isArchived = false;
    public long deletedAt = 0;

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String title = "";

    @NonNull
    public String content = "";

    public long updatedAt;

    public ReadmeEntity() {
    }
}
