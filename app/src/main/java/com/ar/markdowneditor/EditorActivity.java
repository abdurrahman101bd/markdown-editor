package com.ar.markdowneditor;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;

import com.ar.markdowneditor.data.ReadmeEntity;
import com.ar.markdowneditor.data.ReadmeRepository;
import com.ar.markdowneditor.util.MarkdownExporter;
import com.ar.markdowneditor.util.ThemeUtils;
import com.ar.markdowneditor.util.UndoRedoManager;
import com.ar.markdowneditor.util.PrefsHelper;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import org.commonmark.node.FencedCodeBlock;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.MarkwonConfiguration;
import io.noties.markwon.MarkwonSpansFactory;
import io.noties.markwon.RenderProps;
import io.noties.markwon.SpanFactory;
import io.noties.markwon.core.MarkwonTheme;
import io.noties.markwon.core.spans.CodeBlockSpan;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.ext.tables.TableTheme;
import io.noties.markwon.image.glide.GlideImagesPlugin;
import io.noties.markwon.linkify.LinkifyPlugin;
import io.noties.markwon.ext.tasklist.TaskListPlugin;
import io.noties.markwon.html.HtmlPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

public class EditorActivity extends BaseActivity {

    private static final long PREVIEW_DEBOUNCE_MS = 300;
    private static final long UNDO_DEBOUNCE_MS = 800;
    
    private boolean fromTrash = false;
    
    private long getAutosaveDelayMs() {
        return PrefsHelper.getAutosaveSeconds(this) * 1000L;
    }
	
    private static final String PREFS_NAME = "editor_prefs";
    private static final String KEY_VIEW_MODE = "saved_view_mode";

	private enum ViewMode { SPLIT, PREVIEW_ONLY, EDIT_ONLY }
	private enum OpenMode { REPLACE, APPEND }

	private OpenMode pendingOpenMode = OpenMode.REPLACE;

    private ReadmeRepository repository;
    private Markwon markwon;
    private long readmeId = -1;
    private ReadmeEntity current;

    private EditText editTitle, editContent;
    private TextView textPreview;
    private View editScroll, previewScroll, dividerContentBottom;
    private ImageButton btnUndo, btnRedo, btnViewToggle, btnOverflow;
    private ImageButton qiHeading;
    private final View[] headingButtons = new View[7];
    private boolean headingPanelOpen = false;

    private final UndoRedoManager undoRedo = new UndoRedoManager();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private ViewMode currentMode = ViewMode.SPLIT;
    private boolean autosaveEnabled = true;
    private boolean dirty = false;
	
	private OnBackPressedCallback backPressedCallback;
	private ActivityResultLauncher<Intent> saveAsLauncher;
	private ActivityResultLauncher<Intent> openLauncher;
	
    private final Runnable previewRunnable = this::renderPreview;
    private final Runnable undoPushRunnable = () -> {
		undoRedo.push(editContent.getText().toString());
		updateUndoRedoState();
	};
    private final Runnable autosaveRunnable = this::save;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedModeName = prefs.getString(KEY_VIEW_MODE, ViewMode.SPLIT.name());
        try {
            currentMode = ViewMode.valueOf(savedModeName);
        } catch (Exception e) {
            currentMode = ViewMode.SPLIT;
        }

        repository = new ReadmeRepository(this);
        TableTheme tableTheme = new TableTheme.Builder()
			.tableBorderColor(ContextCompat.getColor(this, R.color.md_text_secondary))
			.tableBorderWidth(dpToPx(1))
			.tableCellPadding(dpToPx(12))
			.tableHeaderRowBackgroundColor(ContextCompat.getColor(this, R.color.md_surface_variant))
			.build();

		markwon = Markwon.builder(this)
			.usePlugin(TablePlugin.create(tableTheme))
			.usePlugin(StrikethroughPlugin.create())
			.usePlugin(LinkifyPlugin.create())
			.usePlugin(GlideImagesPlugin.create(this))			
			.usePlugin(HtmlPlugin.create())	
			.usePlugin(TaskListPlugin.create(
                ContextCompat.getColor(this, R.color.md_primary),        
                ContextCompat.getColor(this, R.color.md_text_secondary), 
                ContextCompat.getColor(this, R.color.md_surface_variant) 
            ))

			.usePlugin(new AbstractMarkwonPlugin() {
				@Override
				public void configureTheme(@NonNull MarkwonTheme.Builder builder) {				
					builder.blockQuoteColor(ContextCompat.getColor(EditorActivity.this, R.color.md_primary));
					builder.blockQuoteWidth(dpToPx(6));

					builder.codeBlockBackgroundColor(ContextCompat.getColor(EditorActivity.this, R.color.md_surface_variant));
					builder.codeBlockTextColor(ContextCompat.getColor(EditorActivity.this, R.color.md_text_primary));
					builder.codeBlockMargin(dpToPx(12));

					builder.codeBackgroundColor(ContextCompat.getColor(EditorActivity.this, R.color.md_surface_variant));
					builder.codeTextColor(ContextCompat.getColor(EditorActivity.this, R.color.md_primary));
				}

				@Override
				public void configureSpansFactory(@NonNull MarkwonSpansFactory.Builder builder) {			
					builder.setFactory(FencedCodeBlock.class, (configuration, renderProps) ->
					new CodeBlockSpan(configuration.theme())
					);
				}
			})
			.build();	

        bindViews();
        setupToolbar();
        setupQuickInsertBar();
        setupHeadingButton();
        setupTextWatchers();
		setupBackPressedCallback();
		setupActivityResultLaunchers();

        readmeId = getIntent().getLongExtra(MainActivity.EXTRA_READMES_ID, -1);
        fromTrash = getIntent().getBooleanExtra("from_trash", false);

        if (readmeId == -1) {
            current = new ReadmeEntity();
            current.id = 0;
            current.title = "";
            current.content = "";
            current.updatedAt = System.currentTimeMillis();
            editTitle.setText("");
            editContent.setText("");
            undoRedo.init("");
            renderPreview();
            applyViewMode();
            updateUndoRedoState();
            dirty = false;
        } else {
            loadReadme();
        }
    }

    private void bindViews() {
        editTitle = findViewById(R.id.edit_title);
        editContent = findViewById(R.id.edit_content);
        textPreview = findViewById(R.id.text_preview);
        editScroll = findViewById(R.id.edit_scroll);
        previewScroll = findViewById(R.id.preview_scroll);
        dividerContentBottom = findViewById(R.id.divider_content_bottom);
        btnUndo = findViewById(R.id.btn_undo);
        btnRedo = findViewById(R.id.btn_redo);
        btnViewToggle = findViewById(R.id.btn_view_toggle);
        btnOverflow = findViewById(R.id.btn_overflow);
        qiHeading = findViewById(R.id.qi_heading);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            save();
            finish();
        });

        btnUndo.setOnClickListener(v -> {
            focusEditorAndShowKeyboard();
            String text = undoRedo.undo();
            if (text != null) setEditorTextSilently(text);
            updateUndoRedoState();
        });
        btnRedo.setOnClickListener(v -> {
            focusEditorAndShowKeyboard();
            String text = undoRedo.redo();
            if (text != null) setEditorTextSilently(text);
            updateUndoRedoState();
        });

        btnViewToggle.setOnClickListener(v ->{
            toggleFullScreenMode();
            hideKeyboardAndClearFocus();
        });
        btnOverflow.setOnClickListener(v -> {
            hideKeyboardAndClearFocus();
            showOverflowMenu(v);
        });

        updateUndoRedoState();
        applyViewMode();
    }
	
	private void setupBackPressedCallback() {
		backPressedCallback = new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				save();
				setEnabled(false);
				getOnBackPressedDispatcher().onBackPressed();
			}
		};
		getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
	}
	
	private void setupActivityResultLaunchers() {
		saveAsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
		result -> {
			if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
			Uri uri = result.getData().getData();
			if (uri == null) return;

			try (OutputStream os = getContentResolver().openOutputStream(uri)) {
				if (os != null) {
					os.write(editContent.getText().toString().getBytes(StandardCharsets.UTF_8));
				}
				Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
				Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			}
		});

		openLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
		result -> {
			if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
			Uri uri = result.getData().getData();
			if (uri == null) return;

			try (java.io.InputStream is = getContentResolver().openInputStream(uri)) {
				if (is == null) return;

				java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
				byte[] buf = new byte[8192];
				int len;
				while ((len = is.read(buf)) > 0) {
					buffer.write(buf, 0, len);
				}
				String opened = buffer.toString(StandardCharsets.UTF_8.name());

				String newContent;
				if (pendingOpenMode == OpenMode.REPLACE) {
					newContent = opened;
				} else {
					String existing = editContent.getText().toString();
					newContent = existing.isEmpty() ? opened : existing + "\n\n" + opened;
				}

				editContent.setText(newContent);
				undoRedo.init(newContent);
				updateUndoRedoState();
				renderPreview();
				markDirtyAndAutosave();
				pendingOpenMode = OpenMode.REPLACE;

			} catch (Exception e) {
				Toast.makeText(this, "Open failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			}
		});
	}

    private void switchLayoutFromMenu() {
        if (currentMode == ViewMode.SPLIT) {
            currentMode = ViewMode.PREVIEW_ONLY;
        } else {
            currentMode = ViewMode.SPLIT;
        }
        applyViewMode();
    }

    private void toggleFullScreenMode() {
        if (currentMode == ViewMode.PREVIEW_ONLY) {
            currentMode = ViewMode.EDIT_ONLY;
        } else if (currentMode == ViewMode.EDIT_ONLY) {
            currentMode = ViewMode.PREVIEW_ONLY;
        }
        applyViewMode();
    }

    private void applyViewMode() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_VIEW_MODE, currentMode.name())
            .apply();

        switch (currentMode) {
            case SPLIT:
                previewScroll.setVisibility(View.VISIBLE);
                editScroll.setVisibility(View.VISIBLE);
                btnViewToggle.setVisibility(View.GONE);
                if (dividerContentBottom != null) {
                    dividerContentBottom.setVisibility(View.GONE);
                }
                renderPreview();
                break;

            case PREVIEW_ONLY:
                renderPreview();
                previewScroll.setVisibility(View.VISIBLE);
                editScroll.setVisibility(View.GONE);
                btnViewToggle.setVisibility(View.VISIBLE);
                btnViewToggle.setImageResource(R.drawable.ic_eye_off);
                btnUndo.setVisibility(View.GONE);
                btnRedo.setVisibility(View.GONE);
                if (dividerContentBottom != null) {
                    dividerContentBottom.setVisibility(View.GONE);
                }
                break;

            case EDIT_ONLY:
                previewScroll.setVisibility(View.GONE);
                editScroll.setVisibility(View.VISIBLE);
                btnViewToggle.setVisibility(View.VISIBLE);
                btnViewToggle.setImageResource(R.drawable.ic_eye);
                btnUndo.setVisibility(View.VISIBLE);
                btnRedo.setVisibility(View.VISIBLE);
                if (dividerContentBottom != null) {
                    dividerContentBottom.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    private void showOverflowMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_editor_overflow, popup.getMenu());

        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
        int colorOnSurface = typedValue.data;

        MenuItem themeItem = popup.getMenu().findItem(R.id.menu_switch_theme);
        if (themeItem != null) {
            int currentTheme = ThemeUtils.getSavedMode(this);
            if (currentTheme == ThemeUtils.THEME_DARK) {
                themeItem.setIcon(R.drawable.ic_moon);
            } else {
                themeItem.setIcon(R.drawable.ic_sun);
            }
        }

        for (int i = 0; i < popup.getMenu().size(); i++) {
            MenuItem item = popup.getMenu().getItem(i);
            if (item.getIcon() != null) {
                android.graphics.drawable.Drawable icon = androidx.core.graphics.drawable.DrawableCompat.wrap(item.getIcon().mutate());
                androidx.core.graphics.drawable.DrawableCompat.setTint(icon, colorOnSurface);
                item.setIcon(icon);
            }
        }

        try {
            Field[] fields = popup.getClass().getDeclaredFields();
            for (Field field : fields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(popup);
                    Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                    Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                    setForceIcons.invoke(menuPopupHelper, true);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.menu_open) {
				if (editContent.getText() != null && !editContent.getText().toString().trim().isEmpty()) {
					confirmOpenFile();
				} else {
					pendingOpenMode = OpenMode.REPLACE;
					openMarkdownFile();
				}
				return true;
			} else if (id == R.id.menu_save) {
                save();
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.menu_save_as) {
                saveMarkdownAs();
                return true;
            } else if (id == R.id.menu_share) {
                showShareDialog();
                return true;
            } else if (id == R.id.menu_clear) {
                showClearDialog();
                return true;
            } else if (id == R.id.menu_print) {
                printContent();
                return true;
            } else if (id == R.id.menu_switch_layout) {
                switchLayoutFromMenu();
                return true;
            } else if (id == R.id.menu_switch_view) {
                save();     
                Intent intent = new Intent(this, MarkdownViewerActivity.class);
                intent.putExtra("markdown_content", editContent.getText().toString());
                intent.putExtra("theme_mode", ThemeUtils.getSavedMode(this));    
                String title = editTitle.getText() == null ? "" : editTitle.getText().toString().trim();
                intent.putExtra("note_title", title.isEmpty() ? "Untitled" : title);    
                startActivity(intent);
                return true;
            } else if (id == R.id.menu_switch_theme) {
                toggleTheme();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void toggleTheme() {
        int current = ThemeUtils.getSavedMode(this);
        int next = (current == ThemeUtils.THEME_DARK)
            ? ThemeUtils.THEME_LIGHT
            : ThemeUtils.THEME_DARK;
        ThemeUtils.setMode(this, next);
        recreate();
    }

    // Helper Methods for Focus & Keyboard
    private void focusEditorAndShowKeyboard() {
        editContent.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(editContent, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboardAndClearFocus() {
		View view = getCurrentFocus();
		if (view == null) view = editContent;

		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
		view.clearFocus();
	}

    // Quick Insert Bar
    private void setupQuickInsertBar() {
        setInsertAction(R.id.qi_bold, "**", "**", "");
        setInsertAction(R.id.qi_italic, "_", "_", "");
        
        // Link dialog action
        ImageButton btnLink = findViewById(R.id.qi_link);
        btnLink.setOnClickListener(v -> {
            focusEditorAndShowKeyboard();
            showLinkDialog();
        });

        setInsertAction(R.id.qi_list, "* ", "", "");
        setInsertAction(R.id.qi_strike, "~~", "~~", "");
        setInsertAction(R.id.qi_code, "```\n", "\n```", "");
        setInsertAction(R.id.qi_quote, "> ", "", "");
        setInsertAction(R.id.qi_hr, "\n---\n", "", "");
        setInsertAction(R.id.qi_image, "![", "]()", "");

        // Table dialog action
        ImageButton btnTable = findViewById(R.id.qi_table);
        btnTable.setOnClickListener(v -> {
            focusEditorAndShowKeyboard();
            showTableDialog();
        });
    }

    private void setInsertAction(int viewId, String prefix, String suffix, String placeholder) {
        ImageButton btn = findViewById(viewId);
        btn.setOnClickListener(v -> {
            focusEditorAndShowKeyboard();
            insertAtCursor(prefix, suffix, placeholder);
        });
    }

    private void insertAtCursor(String prefix, String suffix, String placeholder) {
        int start = Math.max(editContent.getSelectionStart(), 0);
        int end = Math.max(editContent.getSelectionEnd(), 0);
        Editable editable = editContent.getText();

        String selected = editable.subSequence(start, end).toString();
        String middle = selected.isEmpty() ? placeholder : selected;
        String insertion = prefix + middle + suffix;

        editable.replace(start, end, insertion);

        int cursor = selected.isEmpty()
            ? start + prefix.length()
            : start + insertion.length();

        editContent.setSelection(Math.min(cursor, editable.length()));
    }

    private void showLinkDialog() {
        ContextThemeWrapper wrapper = new ContextThemeWrapper(this, com.google.android.material.R.style.Theme_Material3_DayNight);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(wrapper);
        View dialogView = LayoutInflater.from(wrapper).inflate(R.layout.dialog_insert_link, null);

        TextInputEditText editLinkText = dialogView.findViewById(R.id.edit_link_text);
        TextInputEditText editLinkUrl = dialogView.findViewById(R.id.edit_link_url);
        View btnClose = dialogView.findViewById(R.id.btn_close);
        View btnOk = dialogView.findViewById(R.id.btn_ok);

        int start = editContent.getSelectionStart();
        int end = editContent.getSelectionEnd();
        if (start >= 0 && end > start) {
            String selectedText = editContent.getText().subSequence(start, end).toString();
            editLinkText.setText(selectedText);
        }

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnOk.setOnClickListener(v -> {
            String text = editLinkText.getText() != null ? editLinkText.getText().toString() : "";
            String url = editLinkUrl.getText() != null ? editLinkUrl.getText().toString() : "";

            String markdownLink = "[" + text + "](" + url + ")";
            insertAtCursor(markdownLink, "", "");

            dialog.dismiss();
            focusEditorAndShowKeyboard();
        });

        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void showTableDialog() {
        ContextThemeWrapper wrapper = new ContextThemeWrapper(this, com.google.android.material.R.style.Theme_Material3_DayNight);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(wrapper);

        View dialogView = LayoutInflater.from(wrapper).inflate(R.layout.dialog_insert_table, null);

        TextInputEditText editRows = dialogView.findViewById(R.id.edit_table_rows);
        TextInputEditText editCols = dialogView.findViewById(R.id.edit_table_cols);
        View btnClose = dialogView.findViewById(R.id.btn_close);
        View btnCancel = dialogView.findViewById(R.id.btn_cancel);
        View btnInsert = dialogView.findViewById(R.id.btn_insert);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View.OnClickListener dismissListener = v -> dialog.dismiss();
        btnClose.setOnClickListener(dismissListener);
        btnCancel.setOnClickListener(dismissListener);

        btnInsert.setOnClickListener(v -> {
            int rows = 3;
            int cols = 3;

            try {
                if (editRows.getText() != null && !editRows.getText().toString().trim().isEmpty()) {
                    rows = Integer.parseInt(editRows.getText().toString().trim());
                }
                if (editCols.getText() != null && !editCols.getText().toString().trim().isEmpty()) {
                    cols = Integer.parseInt(editCols.getText().toString().trim());
                }
            } catch (NumberFormatException ignored) {}

            String tableMarkdown = generateMarkdownTable(rows, cols);
            insertAtCursor("\n" + tableMarkdown + "\n", "", "");

            dialog.dismiss();
            focusEditorAndShowKeyboard();
        });

        dialog.show();
    }

    private String generateMarkdownTable(int rows, int cols) {
        StringBuilder sb = new StringBuilder();
        sb.append("|");
        for (int c = 1; c <= cols; c++) {
            sb.append(" Header ").append(c).append(" |");
        }
        sb.append("\n");
        sb.append("|");
        for (int c = 1; c <= cols; c++) {
            sb.append(" --- |");
        }
        sb.append("\n");
        for (int r = 2; r <= rows; r++) {
            sb.append("|");
            for (int c = 1; c <= cols; c++) {
                sb.append(" Cell ").append(r).append(":").append(c).append(" |");
            }
            if (r < rows) sb.append("\n");
        }

        return sb.toString();
    }

    private void setupHeadingButton() {
        qiHeading.setOnClickListener(v -> {
            focusEditorAndShowKeyboard();
            if (headingPanelOpen) {
                closeHeadingPanel();
            } else {
                openHeadingPanel();
            }
        });
    }

    private void openHeadingPanel() {
        headingPanelOpen = true;

        LinearLayout parent = (LinearLayout) qiHeading.getParent();
        int headingIndex = parent.indexOfChild(qiHeading);

        qiHeading.setVisibility(View.GONE);

        int[] headingIconIds = {
            R.drawable.ic_heading1,
            R.drawable.ic_heading2,
            R.drawable.ic_heading3,
            R.drawable.ic_heading4,
            R.drawable.ic_heading5,
            R.drawable.ic_heading6
        };

        for (int level = 1; level <= 6; level++) {
            final int finalLevel = level;
            final String hashes = repeat('#', finalLevel) + " ";
            final int iconRes = headingIconIds[finalLevel - 1];

            ImageButton btn = createHeadingButton(iconRes, "Heading " + finalLevel);
            btn.setOnClickListener(v -> {
                focusEditorAndShowKeyboard();
                insertAtCursor(hashes, "", "");
                closeHeadingPanel();
            });

            parent.addView(btn, headingIndex + level);
            headingButtons[level - 1] = btn;
        }

        ImageButton close = createHeadingButton(R.drawable.ic_close, "Close");
        close.setOnClickListener(v -> closeHeadingPanel());
        parent.addView(close, headingIndex + 7);
        headingButtons[6] = close;
    }

    private void closeHeadingPanel() {
        headingPanelOpen = false;

        LinearLayout parent = (LinearLayout) qiHeading.getParent();
        for (int i = 0; i < headingButtons.length; i++) {
            if (headingButtons[i] != null) {
                parent.removeView(headingButtons[i]);
                headingButtons[i] = null;
            }
        }

        qiHeading.setVisibility(View.VISIBLE);
    }

    private ImageButton createHeadingButton(int iconRes, String contentDesc) {
        ImageButton btn = new ImageButton(this);
        btn.setImageResource(iconRes);
        btn.setContentDescription(contentDesc);
        btn.setBackgroundResource(R.drawable.bg_icon_button);
        btn.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);

        int size = dpToPx(40);
        int padding = dpToPx(8);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        btn.setLayoutParams(lp);
        btn.setPadding(padding, padding, padding, padding);

        btn.setImageTintList(
            androidx.core.content.ContextCompat.getColorStateList(
                this, R.color.md_icon_active));

        return btn;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private String repeat(char c, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) sb.append(c);
        return sb.toString();
    }

    private void setupTextWatchers() {
        editTitle.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { markDirtyAndAutosave(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        editContent.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                handler.removeCallbacks(previewRunnable);
                handler.postDelayed(previewRunnable, PREVIEW_DEBOUNCE_MS);

                handler.removeCallbacks(undoPushRunnable);
                handler.postDelayed(undoPushRunnable, UNDO_DEBOUNCE_MS);
                
                btnUndo.setAlpha(1f);
		        btnRedo.setAlpha(0.35f);

                markDirtyAndAutosave();
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }
    
    private void markDirtyAndAutosave() {
        dirty = true;
        if (autosaveEnabled) {
            handler.removeCallbacks(autosaveRunnable);
            handler.postDelayed(autosaveRunnable, getAutosaveDelayMs());
        }
    }

    private void renderPreview() {
        if (editContent == null || editContent.getText() == null) return;    
        String content = editContent.getText().toString();
    
        if (textPreview != null) {
            markwon.setMarkdown(textPreview, content);
        }
    }

    private void setEditorTextSilently(String text) {
        undoRedo.setSuppress(true);
        int cursor = Math.min(editContent.getSelectionStart(), text.length());
        editContent.setText(text);
        editContent.setSelection(Math.max(0, cursor));
        undoRedo.setSuppress(false);
        renderPreview();
        updateUndoRedoState();
        markDirtyAndAutosave();
    }

    private void updateUndoRedoState() {
        btnUndo.setAlpha(undoRedo.canUndo() ? 1f : 0.35f);
        btnRedo.setAlpha(undoRedo.canRedo() ? 1f : 0.35f);
    }

    private void loadReadme() {
        repository.getByIdAsync(readmeId, entity -> runOnUiThread(() -> {
            if (entity == null) {
                finish();
                return;
            }
            current = entity;
            editTitle.setText(entity.title);
            editContent.setText(entity.content);
            undoRedo.init(entity.content);
            renderPreview();
            applyViewMode();
            updateUndoRedoState();
            dirty = false;
        }));
    }
	
	private void confirmOpenFile() {
		ContextThemeWrapper wrapper = new ContextThemeWrapper(
            this,
            com.google.android.material.R.style.Theme_Material3_DayNight);

		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(wrapper);
		View dialogView = LayoutInflater.from(wrapper).inflate(R.layout.dialog_replace, null);

		View btnAppend  = dialogView.findViewById(R.id.btn_append);
		View btnReplace = dialogView.findViewById(R.id.btn_replace);

		builder.setView(dialogView);
		AlertDialog dialog = builder.create();

		if (dialog.getWindow() != null) {
			dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
		}

		btnAppend.setOnClickListener(v -> {
			pendingOpenMode = OpenMode.APPEND;
			dialog.dismiss();
			openMarkdownFile();
		});
		btnReplace.setOnClickListener(v -> {
			pendingOpenMode = OpenMode.REPLACE;
			dialog.dismiss();
			openMarkdownFile();
		});

		dialog.show();
	}
    
	private void openMarkdownFile() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("*/*");
		intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
							"text/markdown",
							"text/x-markdown",
							"text/plain"
						});
		openLauncher.launch(intent);
	}    

    private void save() {
		if (current == null) return;

		String newTitle = editTitle.getText() == null ? "" : editTitle.getText().toString().trim();
		String newContent = editContent.getText() == null ? "" : editContent.getText().toString();

		boolean isEmpty = newTitle.isEmpty() && newContent.trim().isEmpty();

		if (current.id == 0) {
			if (isEmpty) {
				dirty = false;
				return;
			}
			current.title = newTitle;
			current.content = newContent;
			current.updatedAt = System.currentTimeMillis();
			repository.insert(current, newId -> current.id = newId);
			dirty = false;
			return;
		}

		if (isEmpty) {
			repository.deleteByIds(java.util.Collections.singletonList(current.id));
			current.id = 0;
			dirty = false;
			return;
		}

		boolean changed = !newTitle.equals(current.title == null ? "" : current.title)
			|| !newContent.equals(current.content == null ? "" : current.content);

		if (fromTrash && changed) {
			current.isDeleted = false;
			current.deletedAt = 0;
			fromTrash = false;
		}

		if (!changed) {
			dirty = false;
			return;
		}

		current.title = newTitle;
		current.content = newContent;
		current.updatedAt = System.currentTimeMillis();
		repository.update(current);
		dirty = false;
	}
    
    private void saveMarkdownAs() {
        if (editContent.getText() == null || editContent.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, R.string.please_enter_text, Toast.LENGTH_SHORT).show();
            return;
        }
        save();

        String title = editTitle.getText() == null ? "" : editTitle.getText().toString().trim();
        if (title.isEmpty()) title = "untitled";
        String safeName = title.replaceAll("[^a-zA-Z0-9-_ ]", "_") + ".md";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/markdown");
        intent.putExtra(Intent.EXTRA_TITLE, safeName);

        saveAsLauncher.launch(intent);
    }
    
    private void showShareDialog() {
        if (editContent.getText() == null || editContent.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, R.string.please_enter_text, Toast.LENGTH_SHORT).show();
            return;
        }

        ContextThemeWrapper wrapper = new ContextThemeWrapper(
                this,
                com.google.android.material.R.style.Theme_Material3_DayNight);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(wrapper);
        View dialogView = LayoutInflater.from(wrapper).inflate(R.layout.dialog_share, null);

        LinearLayout optionPlainText = dialogView.findViewById(R.id.option_plain_text);
        LinearLayout optionMarkdown  = dialogView.findViewById(R.id.option_markdown);
        LinearLayout optionPdf       = dialogView.findViewById(R.id.option_pdf);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        optionPlainText.setOnClickListener(v -> {
            exportPlainText();
            dialog.dismiss();
        });
        optionMarkdown.setOnClickListener(v -> {
            exportMarkdown();
            dialog.dismiss();
        });

        optionPdf.setOnClickListener(v -> {
            exportPdf();
           dialog.dismiss();
        });

        dialog.show();
    }
        
    private void exportPlainText() {
        save();
        try {
            String title = editTitle.getText() == null ? "" : editTitle.getText().toString();
            String content = editContent.getText().toString();

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, title);
            intent.putExtra(Intent.EXTRA_TITLE, title);
            intent.putExtra(Intent.EXTRA_TEXT, content);

            startActivity(Intent.createChooser(intent, getString(R.string.share_via)));
        } catch (Exception e) {
            Toast.makeText(this, "Share failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportMarkdown() {
		if (editContent.getText() == null || editContent.getText().toString().trim().isEmpty()) {
			Toast.makeText(this, R.string.please_enter_text, Toast.LENGTH_SHORT).show();
			return;
		}
		save();
		try {
			String title = editTitle.getText() == null ? "" : editTitle.getText().toString();
			String content = editContent.getText().toString();
			File file = MarkdownExporter.exportAsMarkdown(this, title, content);
			shareFile(file, "text/markdown");
		} catch (Exception e) {
			Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}

	private void exportPdf() {
		if (editContent.getText() == null || editContent.getText().toString().trim().isEmpty()) {
			Toast.makeText(this, R.string.please_enter_text, Toast.LENGTH_SHORT).show();
			return;
		}
		save();
		try {
			String title = editTitle.getText() == null ? "" : editTitle.getText().toString();
			String content = editContent.getText().toString();
			File file = MarkdownExporter.exportAsPdf(this, title, content, 1240, 1754);
			shareFile(file, "application/pdf");
		} catch (Exception e) {
			Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}

    private void shareFile(File file, String mime) {
        android.net.Uri uri = MarkdownExporter.getShareableUri(this, file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mime);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, getString(R.string.share_via)));
    }
    
    private void showClearDialog() {
        if (editContent.getText() == null || editContent.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, R.string.please_enter_text, Toast.LENGTH_SHORT).show();
            return;
        }

        ContextThemeWrapper wrapper = new ContextThemeWrapper(
                this,
                com.google.android.material.R.style.Theme_Material3_DayNight);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(wrapper);
        View dialogView = LayoutInflater.from(wrapper).inflate(R.layout.dialog_clear, null);
        View btnCancel = dialogView.findViewById(R.id.btn_cancel);
        View btnOk = dialogView.findViewById(R.id.btn_ok);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnOk.setOnClickListener(v -> {
            editContent.setText("");
            updateUndoRedoState();
            markDirtyAndAutosave();
            renderPreview();

            dialog.dismiss();
            Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show();
            focusEditorAndShowKeyboard();
        });
        dialog.show();
    }
    
    private void printContent() {
        if (editContent.getText() == null || editContent.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, R.string.please_enter_text, Toast.LENGTH_SHORT).show();
            return;
        }
        
		save();
		PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
		String jobName = (editTitle.getText() == null || editTitle.getText().toString().trim().isEmpty())
            ? "Markdown Document"
            : editTitle.getText().toString().trim();

		PrintDocumentAdapter adapter = new PrintDocumentAdapter() {
			@Override
			public void onLayout(PrintAttributes oldAttrs, PrintAttributes newAttrs,
								 CancellationSignal signal, LayoutResultCallback callback, Bundle extras) {

				if (signal.isCanceled()) {
					callback.onLayoutCancelled();
					return;
				}
				PrintDocumentInfo info = new PrintDocumentInfo.Builder(jobName + ".pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build();
				callback.onLayoutFinished(info, true);
			}

			@Override
			public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
								CancellationSignal signal, WriteResultCallback callback) {

				File tempPdf = null;
				try (FileOutputStream fos = new FileOutputStream(destination.getFileDescriptor())) {
					tempPdf = MarkdownExporter.exportAsPdf(
						EditorActivity.this,
						jobName,
						editContent.getText().toString(),
						1240, 1754);

					java.io.FileInputStream fis = new java.io.FileInputStream(tempPdf);
					byte[] buf = new byte[8192];
					int len;
					while ((len = fis.read(buf)) > 0) {
						fos.write(buf, 0, len);
					}
					fis.close();

					callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
				} catch (Exception e) {
					callback.onWriteFailed(e.getMessage());
				} finally {
					if (tempPdf != null && tempPdf.exists()) {
						//noinspection ResultOfMethodCallIgnored
						tempPdf.delete();
					}
				}
			}
		};

		if (printManager != null) {
			printManager.print(jobName, adapter, new PrintAttributes.Builder().build());
		}
	}

    
    @Override
    protected void onPause() {
        super.onPause();
        if (dirty) save();
    }

}
