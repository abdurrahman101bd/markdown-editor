package com.ar.markdowneditor.util;

import java.io.File;

public class BackupInfo {
    public File file;
    public String fileName;
    public long sizeBytes;
    public long modifiedAt;

    public int noteCount;
    public int activeCount;
    public int archivedCount;
    public int trashCount;

    public int theme;
    public int viewMode;
    public int autosaveSec;
    public int autoremoveDays;
    public int autobackupDays;
    public boolean screenshotAllowed;
    public boolean biometricEnabled;

    public boolean detailsLoaded = false;

    public BackupInfo(File file) {
        this.file = file;
        this.fileName = file.getName();
        this.sizeBytes = file.length();
        this.modifiedAt = file.lastModified();
    }
}