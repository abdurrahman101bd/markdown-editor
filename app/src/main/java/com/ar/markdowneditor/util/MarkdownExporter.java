package com.ar.markdowneditor.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import io.noties.markwon.Markwon;

public class MarkdownExporter {

    private static final String EXPORT_DIR = "exports";

    public static File exportAsMarkdown(Context context, String title, String content) throws IOException {
        File dir = new File(context.getExternalFilesDir(null), EXPORT_DIR);
        if (!dir.exists()) dir.mkdirs();

        String safeName = sanitize(title) + ".md";
        File outFile = new File(dir, safeName);

        try (OutputStream os = new FileOutputStream(outFile)) {
            os.write(content.getBytes());
        }
        return outFile;
    }

    public static File exportAsPdf(Context context, String title, String content, int pageWidthPx, int pageHeightPx) throws IOException {
        Markwon markwon = Markwon.create(context);

        TextView textView = new TextView(context);
        textView.setTextColor(0xFF000000);
        textView.setTextSize(14);
        textView.setPadding(40, 40, 40, 40);
        markwon.setMarkdown(textView, content);

        int widthSpec = View.MeasureSpec.makeMeasureSpec(pageWidthPx, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        textView.measure(widthSpec, heightSpec);
        int totalHeight = textView.getMeasuredHeight();
        textView.layout(0, 0, pageWidthPx, totalHeight);

        PdfDocument document = new PdfDocument();
        int pageCount = Math.max(1, (int) Math.ceil((double) totalHeight / pageHeightPx));

        for (int i = 0; i < pageCount; i++) {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidthPx, pageHeightPx, i + 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            canvas.translate(0, -i * pageHeightPx);
            textView.draw(canvas);
            document.finishPage(page);
        }

        File dir = new File(context.getExternalFilesDir(null), EXPORT_DIR);
        if (!dir.exists()) dir.mkdirs();
        File outFile = new File(dir, sanitize(title) + ".pdf");

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            document.writeTo(fos);
        } finally {
            document.close();
        }
        return outFile;
    }

    public static android.net.Uri getShareableUri(Context context, File file) {
        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
    }

    private static String sanitize(String title) {
        String t = (title == null || title.trim().isEmpty()) ? "untitled" : title.trim();
        return t.replaceAll("[^a-zA-Z0-9-_ ]", "_");
    }
}
