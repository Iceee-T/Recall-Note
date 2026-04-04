package com.example.notewise;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

public class Note_EditorActivity extends AppCompatActivity {

    private EditText etNoteContent, tvTitle;
    private FrameLayout noteContainer;
    private String notebookId, noteId;
    private DatabaseReference noteRef;

    // Image/View Interaction Variables
    private View currentlySelectedView = null;
    private LinearLayout currentFloatingMenu = null;
    private float dX, dY;
    private int initialWidth, initialHeight;
    private float initialTouchX, initialTouchY;
    private boolean isResizing = false;

    // Undo/Redo Stacks
    private Stack<CharSequence> undoStack = new Stack<>();
    private Stack<CharSequence> redoStack = new Stack<>();
    private boolean isUndoRedoOperation = false;

    private final ActivityResultLauncher<String[]> attachmentLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    String mimeType = getContentResolver().getType(uri);

                    if (mimeType != null && mimeType.startsWith("image/")) {
                        addNewImageBlock(uri);
                    } else {
                        // Show options for Documents
                        showDocumentOptions(uri);
                    }
                }
            });

    private final ActivityResultLauncher<Void> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    addNewImageBlockFromBitmap(bitmap);
                }
            });
    private void showDocumentOptions(Uri uri) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        // Initialize Views
        etNoteContent = findViewById(R.id.etNoteContent);
        tvTitle = findViewById(R.id.tvTitle);
        noteContainer = findViewById(R.id.noteContainer);
        TextView tvTimestamp = findViewById(R.id.tvTimestamp);

        // --- LISTENERS ---
        // Header
        findViewById(R.id.btnBack).setOnClickListener(v -> saveAndExit());
        findViewById(R.id.btnSave).setOnClickListener(v -> {
            saveNoteToFirebase();
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btnUndo).setOnClickListener(v -> undo());
        findViewById(R.id.btnRedo).setOnClickListener(v -> redo());

        // Formatting Toolbar
        findViewById(R.id.btnAlignLeft).setOnClickListener(v -> {
            // Manually trigger an undo snapshot before applying style
            undoStack.push(new android.text.SpannableString(etNoteContent.getText()));
            applyAlignment(Layout.Alignment.ALIGN_NORMAL);
        });

        findViewById(R.id.btnAlignCenter).setOnClickListener(v -> {
            saveStateToUndo();
            applyAlignment(Layout.Alignment.ALIGN_CENTER);
        });

        findViewById(R.id.btnAlignRight).setOnClickListener(v -> {
            saveStateToUndo();
            applyAlignment(Layout.Alignment.ALIGN_OPPOSITE);
        });

        findViewById(R.id.btnBullet).setOnClickListener(v -> {
            saveStateToUndo();
            applyBulletPoints();
        });

        findViewById(R.id.btnHighlight).setOnClickListener(v -> {
            saveStateToUndo();
            showHighlightColorPicker(); // Use the new picker we discussed
        });

        // Bottom Nav
        // Inside onCreate
        findViewById(R.id.btnAttach).setOnClickListener(v -> {
            com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog =
                    new com.google.android.material.bottomsheet.BottomSheetDialog(this);

            // Make sure you create this layout in Step 4 below!
            View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_add, null);

            // 1. Logic for Photos Button
            sheetView.findViewById(R.id.layoutPhotos).setOnClickListener(view -> {
                attachmentLauncher.launch(new String[]{"image/*"});
                bottomSheetDialog.dismiss();
            });

            // 2. Logic for Camera Button
            sheetView.findViewById(R.id.layoutCamera).setOnClickListener(view -> {
                cameraLauncher.launch(null);
                bottomSheetDialog.dismiss();
            });

            // 3. Logic for the Close (X) Button (to match your photo)
            sheetView.findViewById(R.id.btnCloseSheet).setOnClickListener(view -> {
                bottomSheetDialog.dismiss();
            });

            bottomSheetDialog.setContentView(sheetView);
            bottomSheetDialog.show();
        });
        findViewById(R.id.btnSummary).setOnClickListener(v -> performSummary());
        findViewById(R.id.btnStyle).setOnClickListener(this::showStyleMenu);

        tvTimestamp.setText(new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(new Date()));

        // Firebase Setup
        notebookId = getIntent().getStringExtra("notebook_id");

// If no specific notebook was passed, default to the user's general_notes folder
        if (notebookId == null) {
            String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
            notebookId = "general_notes/" + currentUserId;
        }

        noteRef = FirebaseDatabase.getInstance().getReference("notes").child(notebookId);

        // Load Note
        Note existingNote = (Note) getIntent().getSerializableExtra("existing_note");
        if (existingNote != null) {
            noteId = existingNote.getId();
            tvTitle.setText(existingNote.getTitle());
            etNoteContent.setText(Html.fromHtml(existingNote.getContent(), Html.FROM_HTML_MODE_COMPACT));
        }

        setupUndoRedoTracker();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() { saveAndExit(); }
        });
    }

    // Reliable Deselect Logic
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (currentlySelectedView != null) {
                Rect outRect = new Rect();
                currentlySelectedView.getGlobalVisibleRect(outRect);
                Rect menuRect = new Rect();
                if (currentFloatingMenu != null) currentFloatingMenu.getGlobalVisibleRect(menuRect);

                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY()) &&
                        !menuRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    deselectImage();
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void deselectImage() {
        if (currentlySelectedView != null) {
            currentlySelectedView.setBackground(null);
            currentlySelectedView = null;
            if (currentFloatingMenu != null) {
                noteContainer.removeView(currentFloatingMenu);
                currentFloatingMenu = null;
            }
        }
    }

    private void addNewImageBlock(Uri uri) {
        FrameLayout frame = new FrameLayout(this);
        frame.setLayoutParams(new FrameLayout.LayoutParams(500, 500));
        frame.setPadding(30, 30, 30, 30);

        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setImageURI(uri);
        frame.addView(imageView);

        setupInteraction(frame);
        noteContainer.addView(frame);
    }

    private void setupInteraction(View container) {
        container.setOnTouchListener((view, event) -> {
            if (currentlySelectedView != view) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    deselectImage();
                    currentlySelectedView = view;
                    view.setBackgroundResource(R.drawable.selection_border);
                    showSubtleMenu(view);
                }
                return true;
            }

            int h = 100; // Handle size
            boolean corner = (event.getX() < h || event.getX() > view.getWidth() - h) &&
                    (event.getY() < h || event.getY() > view.getHeight() - h);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isResizing = corner;
                    initialWidth = view.getWidth();
                    initialHeight = view.getHeight();
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    dX = view.getX() - event.getRawX();
                    dY = view.getY() - event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (isResizing) {
                        float deltaX = event.getRawX() - initialTouchX;
                        // Constrain width so photo doesn't exceed screen width
                        int maxW = noteContainer.getWidth() - 40;
                        int newWidth = Math.min(maxW, Math.max(200, (int) (initialWidth + deltaX)));
                        view.getLayoutParams().width = newWidth;
                        view.getLayoutParams().height = (int) (initialHeight * ((float) newWidth / initialWidth));
                        view.requestLayout();
                    } else {
                        view.setX(event.getRawX() + dX);
                        view.setY(event.getRawY() + dY);
                    }
                    updateMenuPosition(view);
                    break;
                case MotionEvent.ACTION_UP:
                    isResizing = false;
                    break;
            }
            return true;
        });
    }

    private void showSubtleMenu(View anchor) {
        if (currentFloatingMenu != null) noteContainer.removeView(currentFloatingMenu);

        currentFloatingMenu = new LinearLayout(this);
        currentFloatingMenu.setOrientation(LinearLayout.HORIZONTAL);
        currentFloatingMenu.setBackgroundResource(R.drawable.subtle_menu_bg);
        currentFloatingMenu.setPadding(10, 5, 10, 5);
        currentFloatingMenu.setElevation(8f);

        addMenuAction("Wrap", v -> toggleWrapText(anchor));
        addMenuAction("Crop", v -> triggerCrop(anchor));
        addMenuAction("Lasso", v -> triggerLasso(anchor));
        addMenuAction("Front", v -> { anchor.setTranslationZ(20f); anchor.bringToFront(); });
        addMenuAction("Back", v -> {
            anchor.setTranslationZ(-20f);
            noteContainer.removeView(anchor);
            noteContainer.addView(anchor, 0);
        });
        addMenuAction("Delete", v -> { noteContainer.removeView(anchor); deselectImage(); });

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2);
        currentFloatingMenu.setLayoutParams(lp);
        noteContainer.addView(currentFloatingMenu);
        updateMenuPosition(anchor);
    }

    private void addMenuAction(String label, View.OnClickListener listener) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(Color.WHITE);
        tv.setPadding(25, 12, 25, 12);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setOnClickListener(listener);
        currentFloatingMenu.addView(tv);
    }

    private void updateMenuPosition(View anchor) {
        if (currentFloatingMenu == null) return;
        currentFloatingMenu.setX(Math.max(10, anchor.getX()));
        currentFloatingMenu.setY(Math.max(0, anchor.getY() - 120));
    }

    private void toggleWrapText(View anchor) {
        Editable text = etNoteContent.getText();

        // Remove old wrap spans to prevent accumulation
        LeadingMarginSpan[] old = text.getSpans(0, text.length(), LeadingMarginSpan.class);
        for (LeadingMarginSpan s : old) text.removeSpan(s);

        // Calculate margin: Image width + padding
        // Clamp margin to 70% of screen so text never "shoots outside"
        int margin = (int) (anchor.getWidth() + 20);
        int maxMargin = (int) (noteContainer.getWidth() * 0.7f);
        if (margin > maxMargin) margin = maxMargin;

        // Apply MS Word style wrap: Indent the text block next to the photo
        text.setSpan(new LeadingMarginSpan.Standard(margin, 0), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Snap photo to left margin for a clean look
        anchor.animate().x(16).setDuration(300).start();
    }

    // Improved Crop Logic: Performs a 10% inset crop
    private void triggerCrop(View anchor) {
        if (!(anchor instanceof FrameLayout)) return;

        // 1. Find the ImageView inside the FrameLayout
        ImageView iv = (ImageView) ((FrameLayout) anchor).getChildAt(0);

        // 2. Convert current view to Bitmap
        iv.setDrawingCacheEnabled(true);
        Bitmap original = Bitmap.createBitmap(iv.getDrawingCache());
        iv.setDrawingCacheEnabled(false);

        // 3. Calculate Crop (Example: remove 10% from all sides)
        int width = original.getWidth();
        int height = original.getHeight();
        int newW = (int) (width * 0.8);
        int newH = (int) (height * 0.8);

        try {
            Bitmap cropped = Bitmap.createBitmap(original, width/10, height/10, newW, newH);
            iv.setImageBitmap(cropped);
            Toast.makeText(this, "Image Cropped", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Crop failed: Image too small", Toast.LENGTH_SHORT).show();
        }
    }

    // Improved Lasso Logic: Performs a Circular Mask (Visual Cutout)
    private void triggerLasso(View anchor) {
        if (!(anchor instanceof FrameLayout)) return;

        ImageView iv = (ImageView) ((FrameLayout) anchor).getChildAt(0);

        iv.setDrawingCacheEnabled(true);
        Bitmap source = Bitmap.createBitmap(iv.getDrawingCache());
        iv.setDrawingCacheEnabled(false);

        // 1. Create a transparent canvas
        Bitmap output = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(output);

        android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        paint.setColor(android.graphics.Color.BLACK);

        // 2. Draw a circular mask
        float radius = Math.min(source.getWidth(), source.getHeight()) / 2f;
        canvas.drawCircle(source.getWidth()/2f, source.getHeight()/2f, radius, paint);

        // 3. Mask the original image into the circle (PorterDuff Mode)
        paint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(source, 0, 0, paint);

        iv.setImageBitmap(output);
        Toast.makeText(this, "Lasso (Circle Mask) Applied", Toast.LENGTH_SHORT).show();
    }
    // --- NEW FORMATTING LOGIC ---

    private void applyAlignment(Layout.Alignment alignment) {
        Editable str = etNoteContent.getText();
        int start = etNoteContent.getSelectionStart();
        int end = etNoteContent.getSelectionEnd();

        // 1. Find the exact start of the paragraph
        int lineStart = start;
        while (lineStart > 0 && str.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }

        // 2. Find the exact end of the paragraph
        int lineEnd = end;
        while (lineEnd < str.length() && str.charAt(lineEnd) != '\n') {
            lineEnd++;
        }

        // CRITICAL FIX: If we aren't at the very end of the text,
        // we must include the newline character itself in the span.
        if (lineEnd < str.length() && str.charAt(lineEnd) == '\n') {
            lineEnd++;
        }

        // 3. Remove existing alignment spans in this range
        AlignmentSpan[] existingSpans = str.getSpans(lineStart, lineEnd, AlignmentSpan.class);
        for (AlignmentSpan span : existingSpans) {
            str.removeSpan(span);
        }

        // 4. Apply the new alignment with the PARAGRAPH flag
        // The range now strictly follows the (start ... \n) rule.
        try {
            str.setSpan(new AlignmentSpan.Standard(alignment),
                    lineStart,
                    lineEnd,
                    Spannable.SPAN_PARAGRAPH);
        } catch (Exception e) {
            // Fallback to prevent crash if logic fails
            e.printStackTrace();
        }
    }

    private void applyHighlight() {
        Editable str = etNoteContent.getText();
        int start = etNoteContent.getSelectionStart();
        int end = etNoteContent.getSelectionEnd();

        if (start == end) {
            Toast.makeText(this, "Select text to highlight", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if selection already has a highlight
        BackgroundColorSpan[] spans = str.getSpans(start, end, BackgroundColorSpan.class);
        if (spans.length > 0) {
            // Toggle OFF: Remove existing highlight
            for (BackgroundColorSpan span : spans) {
                str.removeSpan(span);
            }
        } else {
            // Toggle ON: Apply yellow highlight
            str.setSpan(new BackgroundColorSpan(Color.YELLOW), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void showHighlightColorPicker() {
        String[] colors = {"Yellow", "Cyan", "Light Green", "Pink", "None"};
        int[] hexColors = {Color.YELLOW, Color.CYAN, Color.GREEN, Color.MAGENTA, Color.TRANSPARENT};

        new AlertDialog.Builder(this)
                .setTitle("Choose Highlight Color")
                .setItems(colors, (dialog, which) -> {
                    applyColorToText(hexColors[which]);
                })
                .show();
    }

    private void applyColorToText(int color) {
        Editable str = etNoteContent.getText();
        int start = etNoteContent.getSelectionStart();
        int end = etNoteContent.getSelectionEnd();

        if (start == end) return;

        // Remove existing highlights in that specific selection first
        BackgroundColorSpan[] spans = str.getSpans(start, end, BackgroundColorSpan.class);
        for (BackgroundColorSpan span : spans) str.removeSpan(span);

        // Apply new color (unless "None" was selected)
        if (color != Color.TRANSPARENT) {
            str.setSpan(new BackgroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void applyBulletPoints() {
        Editable str = etNoteContent.getText();
        int start = etNoteContent.getSelectionStart();
        int end = etNoteContent.getSelectionEnd();

        String selectedText = str.subSequence(start, end).toString();
        String[] lines = selectedText.split("\n");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            // Only add bullet if the line is NOT empty and NOT just whitespace
            if (!line.trim().isEmpty()) {
                if (!line.trim().startsWith("•")) {
                    sb.append("• ").append(line);
                } else {
                    sb.append(line);
                }
            } else {
                sb.append(line); // Keep the blank line as is
            }
            if (i < lines.length - 1) sb.append("\n");
        }
        str.replace(start, end, sb.toString());
    }

    // --- UNDO / REDO LOGIC ---

    private void saveStateToUndo() {
        // We use SpannableString to "freeze" the current styles and text
        Spannable currentText = new android.text.SpannableString(etNoteContent.getText());
        undoStack.push(currentText);
        if (undoStack.size() > 50) undoStack.remove(0);
    }
    private void setupUndoRedoTracker() {
        etNoteContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Store the rich text state BEFORE the change occurs
                if (!isUndoRedoOperation) {
                    undoStack.push(new android.text.SpannableString(etNoteContent.getText()));
                    redoStack.clear(); // Clear redo on new manual changes
                }
            }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (undoStack.size() > 50) undoStack.remove(0); // Limit memory
            }
        });
    }

    private void undo() {
        if (!undoStack.isEmpty()) {
            isUndoRedoOperation = true;
            // Save current state to redo stack before moving back
            redoStack.push(new android.text.SpannableString(etNoteContent.getText()));

            // Restore the previous state
            etNoteContent.setText(undoStack.pop());

            // Move cursor to the end of the restored text
            etNoteContent.setSelection(etNoteContent.getText().length());
            isUndoRedoOperation = false;
        }
    }

    private void redo() {
        if (!redoStack.isEmpty()) {
            isUndoRedoOperation = true;
            // Save current state to undo stack before moving forward
            undoStack.push(new android.text.SpannableString(etNoteContent.getText()));

            // Restore the redo state
            etNoteContent.setText(redoStack.pop());

            etNoteContent.setSelection(etNoteContent.getText().length());
            isUndoRedoOperation = false;
        }
    }

    // --- KEEP ORIGINAL IMAGE & SAVE LOGIC ---

    private String convertToHtmlWithAlignment(Spannable text) {
        // Start with the standard conversion for bold/italic/bullets
        String html = Html.toHtml(text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);

        // Manual Fix for Alignment:
        // Android uses <p dir="ltr">. We will replace it with aligned versions
        // by checking the actual spans in your EditText.
        StringBuilder finalHtml = new StringBuilder();
        String[] lines = html.split("(?<=</p>)"); // Split by paragraph tags

        AlignmentSpan[] spans = text.getSpans(0, text.length(), AlignmentSpan.class);

        // If no alignment is found, just return the standard HTML
        if (spans.length == 0) return html;

        // This is a simplified reliable approach:
        // Because Html.toHtml is limited, we ensure the final string
        // explicitly contains the align property Firebase needs to see.
        return html.replace("<p dir=\"ltr\">", "<p dir=\"ltr\" align=\"center\">")
                // Note: This logic assumes most users want centered text saved.
                // For a 100% perfect multi-align fix, we'd use a custom TagHandler.
                .replace("text-align:start", "text-align:center");
    }


    // You can delete generateSavableHtml() entirely!

    private void saveNoteToFirebase() {
        String title = tvTitle.getText().toString().trim();

        // Just standard Android HTML generation!
        String htmlContent = Html.toHtml(etNoteContent.getText(), Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);

        if (title.isEmpty() && etNoteContent.getText().toString().trim().isEmpty()) return;
        if (title.isEmpty()) title = "Untitled Note";

        if (noteId == null) {
            noteId = noteRef.push().getKey();
        }

        String time = new java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date());
        Note note = new Note(title, htmlContent, time);

        if (noteId != null) {
            noteRef.child(noteId).setValue(note);
        }
    }

    private void saveAndExit() {
        String title = tvTitle.getText().toString().trim();

        // Just standard Android HTML generation!
        String htmlContent = Html.toHtml(etNoteContent.getText(), Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);

        if (title.isEmpty() && etNoteContent.getText().toString().trim().isEmpty()) {
            finish();
            return;
        }

        if (title.isEmpty()) title = "Untitled Note";

        if (noteId == null) {
            noteId = noteRef.push().getKey();
        }

        String time = new java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date());
        Note note = new Note(title, htmlContent, time);

        if (noteId != null) {
            noteRef.child(noteId).setValue(note);
        }
        finish();
    }

    // [All Original Methods kept: dispatchTouchEvent, deselectImage, addNewImageBlock,
    // setupInteraction, showSubtleMenu, addMenuAction, updateMenuPosition, toggleWrapText,
    // triggerCrop, triggerLasso, showStyleMenu, performSummary]

    private void performSummary() {
        final String text = etNoteContent.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Note is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("AI Summarizer")
                .setMessage("Summarize this note using Gemini AI?")
                .setPositiveButton("Yes", (d, w) -> {
                    Toast.makeText(this, "Summarizing...", Toast.LENGTH_SHORT).show();

                    GeminiQuizHelper.generateSummary(text, new GeminiQuizHelper.SummaryCallback() {
                        @Override
                        public void onSuccess(String summary) {
                            runOnUiThread(() -> {
                                String currentHtml = Html.toHtml(etNoteContent.getText(), Html.FROM_HTML_MODE_LEGACY);
                                // Prepend the real AI summary to the content
                                etNoteContent.setText(Html.fromHtml(
                                        "<b>AI Summary:</b> " + summary + "<br>---<br>" + currentHtml,
                                        Html.FROM_HTML_MODE_LEGACY
                                ));
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() ->
                                    Toast.makeText(Note_EditorActivity.this, "Error: " + error, Toast.LENGTH_LONG).show()
                            );
                        }
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showStyleMenu(View view) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(this, view);
        popup.getMenu().add(0, 1, 0, "Bold");
        popup.getMenu().add(0, 2, 1, "Italic");
        popup.getMenu().add(0, 3, 2, "Underline");
        popup.getMenu().add(0, 6, 5, "Clear Formatting");
        popup.setOnMenuItemClickListener(item -> {
            int start = etNoteContent.getSelectionStart();
            int end = etNoteContent.getSelectionEnd();
            if (start == end) { start = 0; end = etNoteContent.getText().length(); }
            Editable str = etNoteContent.getText();
            switch (item.getItemId()) {
                case 1: str.setSpan(new StyleSpan(Typeface.BOLD), start, end, 0); break;
                case 2: str.setSpan(new StyleSpan(Typeface.ITALIC), start, end, 0); break;
                case 3: str.setSpan(new UnderlineSpan(), start, end, 0); break;
                case 6:
                    Object[] spans = str.getSpans(start, end, android.text.style.CharacterStyle.class);
                    for (Object span : spans) str.removeSpan(span);
                    break;
            }
            return true;
        });
        popup.show();
    }
    // Add this at the end of the file
    private void addNewImageBlockFromBitmap(Bitmap bitmap) {
        FrameLayout frame = new FrameLayout(this);
        frame.setLayoutParams(new FrameLayout.LayoutParams(500, 500));
        frame.setPadding(30, 30, 30, 30);

        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setImageBitmap(bitmap);

        frame.addView(imageView);
        setupInteraction(frame); // This makes it draggable/resizable
        noteContainer.addView(frame);
    }
}