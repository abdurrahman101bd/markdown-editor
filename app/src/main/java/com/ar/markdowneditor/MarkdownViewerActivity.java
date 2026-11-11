package com.ar.markdowneditor;

import android.os.Bundle;
import android.webkit.WebView;
import com.ar.markdowneditor.util.ThemeUtils;
import com.google.android.material.appbar.MaterialToolbar;
import androidx.core.content.ContextCompat;

public class MarkdownViewerActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_markdown_viewer);

        MaterialToolbar toolbar = findViewById(R.id.toolbarMarkdown);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        String noteTitle = getIntent().getStringExtra("note_title");
        if (noteTitle == null || noteTitle.isEmpty()) {
            noteTitle = "Markdown Preview";
        }
        toolbar.setTitle(noteTitle);
        
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.md_background));
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.md_text_primary));
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(ContextCompat.getColor(this, R.color.md_text_primary));
        }

        WebView wvMarkdown = findViewById(R.id.wvMarkdown);
        
        String markdownText = getIntent().getStringExtra("markdown_content");
        if (markdownText == null) markdownText = ""; 
        
        int themeMode = getIntent().getIntExtra("theme_mode", ThemeUtils.getSavedMode(this));
        
        wvMarkdown.getSettings().setJavaScriptEnabled(true);
        wvMarkdown.getSettings().setDomStorageEnabled(true);
        wvMarkdown.setBackgroundColor(0x00000000);

        boolean isDark = false;
        if (themeMode == ThemeUtils.THEME_DARK) {
            isDark = true;
        } else if (themeMode == ThemeUtils.THEME_SYSTEM) {
            int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            isDark = (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES);
        }

        String bgColor = isDark ? "#121212" : "#FFFFFF";
        String textColor = isDark ? "#E8EAED" : "#202124";
        String primaryColor = isDark ? "#FFFFFF" : "#121212";
        String codeBgColor = isDark ? "#2C2C2C" : "#F1F3F4";
        String borderColor = isDark ? "#3C4043" : "#EEEEEE";

        String escapedContent = markdownText
                .replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("\n", "\\n")
                .replace("\r", "");

        String htmlData = "<html><head>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<script src='marked.min.js'></script>" +
            "<style>" +
            "body { font-family: sans-serif; padding: 16px; line-height: 1.6; background-color: " + bgColor + "; color: " + textColor + "; margin: 0; word-wrap: break-word; }" +
            "h1, h2, h3, h4, h5, h6 { color: " + primaryColor + "; border-bottom: 1px solid " + borderColor + "; padding-bottom: 0.3em; margin-top: 24px; }" +
            "a { color: #3b82f6; text-decoration: none; }" +
            "pre { background: " + codeBgColor + "; padding: 16px; border-radius: 6px; overflow: auto; border: 1px solid " + borderColor + "; }" +
            "code { background: " + codeBgColor + "; padding: 0.2em 0.4em; border-radius: 3px; font-family: monospace; font-size: 85%; }" +
            "pre code { background: none; padding: 0; font-size: 100%; }" +
            "blockquote { border-left: 0.25em solid " + primaryColor + "; color: " + textColor + "; padding: 0 1em; margin: 0; opacity: 0.8; }" +
            "table { border-collapse: collapse; width: 100%; margin-bottom: 16px; }" +
            "table th, table td { border: 1px solid " + borderColor + "; padding: 6px 13px; }" +
            "table th { background-color: " + codeBgColor + "; }" +
            "img { max-width: 100%; box-sizing: content-box; }" +
            "</style></head><body>" +
            "<div id='content'></div>" +
            "<script>" +
            "try {" +
            "  var rawText = `" + escapedContent + "`;" +
            "  document.getElementById('content').innerHTML = marked.parse(rawText);" +
            "} catch(e) {" +
            "  document.getElementById('content').innerText = 'Error parsing markdown: ' + e.message;" +
            "}" +
            "</script>" +
            "</body></html>";

        wvMarkdown.loadDataWithBaseURL("file:///android_asset/", htmlData, "text/html", "UTF-8", null);
    }
}