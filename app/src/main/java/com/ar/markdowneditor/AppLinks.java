package com.ar.markdowneditor.util;

public class AppLinks {

    public static final String GITHUB_USERNAME = "abdurrahman101bd";
    public static final String REPO_NAME = "markdown-editor";
    
    public static final String REPO_URL = "https://github.com/" + GITHUB_USERNAME + "/" + REPO_NAME;
    public static final String DOCS_URL = "https://abdurrahman101bd.github.io/documentation/markdown-editor";
    public static final String PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.ar.markdowneditor";

    public static final String BUG_REPORT_URL = REPO_URL + "/issues/new?labels=bug&template=bug_report.md";
    public static final String FEATURE_REQUEST_URL = REPO_URL + "/issues/new?labels=enhancement&template=feature_request.md";
    public static final String GENERAL_FEEDBACK_URL = REPO_URL + "/issues/new?labels=feedback";

    public static class Library {
        public final String name;
        public final String url;
        public Library(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }

    public static Library[] getLibraries() {
        return new Library[]{
                new Library("Markwon", "https://github.com/noties/Markwon"),
                new Library("Glide", "https://github.com/bumptech/glide"),
                new Library("Room (AndroidX)", "https://developer.android.com/training/data-storage/room"),
                new Library("WorkManager (AndroidX)", "https://developer.android.com/topic/libraries/architecture/workmanager"),
                new Library("Biometric (AndroidX)", "https://developer.android.com/training/sign-in/biometric-auth"),
                new Library("Material Components", "https://github.com/material-components/material-components-android"),
                new Library("DocumentFile (AndroidX)", "https://developer.android.com/reference/androidx/documentfile/provider/DocumentFile")
        };
    }
}