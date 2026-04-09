package com.example.notewise;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class HomepageActivity extends AppCompatActivity {
    private FloatingActionButton fabMain;
    private com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton fabUpload, fabCreateNote;
    private boolean isMenuOpen = false;
    private String pendingHighlightNoteId = null;

    private RecyclerView recyclerView;
    private NoteAdapter adapter;
    private List<Note> noteList = new ArrayList<>();
    private DatabaseReference mDatabase;
    private String currentUserId;

    private EditText etNoteContent, tvTitle;
    private String notebookId, noteId; // noteId is now accessible to saveAndExit()
    private DatabaseReference noteRef;
    private ValueEventListener notesListener;
    private boolean isStudyModeActive = false;
    private android.app.NotificationManager notificationManager;
    private android.app.ProgressDialog loadingDialog;


    private final ActivityResultLauncher<String[]> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    // Show loading dialog
                    showLoadingDialog();
                    // Process file in background
                    processUploadedFile(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        PDFBoxResourceLoader.init(getApplicationContext());

        DailyTaskManager.updateDailyTask();

        AchievementTracker tracker = new AchievementTracker(this);
        tracker.startGlobalTracking();

        // Initialize FABs
        fabMain = findViewById(R.id.fab);
        fabUpload = findViewById(R.id.fabUpload);
        fabCreateNote = findViewById(R.id.fabCreateNote);

        fabMain.setOnClickListener(v -> toggleFabMenu());

        // Action for Create Note
        fabCreateNote.setOnClickListener(v -> {
            toggleFabMenu(); // Close menu
            startActivity(new Intent(HomepageActivity.this, Note_EditorActivity.class));
        });

        // Action for Upload
        fabUpload.setOnClickListener(v -> {
            // Manually close the menu after clicking upload
            fabUpload.setVisibility(View.GONE);
            fabCreateNote.setVisibility(View.GONE);
            fabMain.setImageResource(android.R.drawable.ic_input_add);
            isMenuOpen = false;

            // Launch the picker
            filePickerLauncher.launch(new String[]{
                    "text/plain",
                    "application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            });
        });

        // 1. Initialize Firebase & Auth
        // We get the UID to ensure the homepage only loads THIS user's general notes
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            // Optimized Path: notes -> general_notes -> {userId}
            mDatabase = FirebaseDatabase.getInstance().getReference("notes")
                    .child("general_notes")
                    .child(currentUserId);
        } else {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        notificationManager = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        LinearLayout layoutStudy = findViewById(R.id.layoutStudy);
        ImageView ivStudyIcon = findViewById(R.id.ivStudyIcon);
        TextView tvStudyText = findViewById(R.id.tvStudyText);

        layoutStudy.setOnClickListener(v -> {
            toggleStudyMode(ivStudyIcon, tvStudyText);
        });

        // 2. Initialize UI Components
        ImageView btnMenu = findViewById(R.id.imageButtonMenu);
        LinearLayout layoutNotebook = findViewById(R.id.layoutNotebook);
        LinearLayout layoutQuiz = findViewById(R.id.layoutQuiz);
        LinearLayout layoutFlashcard = findViewById(R.id.layoutFlashcard);

        // 3. Setup RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // When clicking an existing note, pass the user-specific path
        // Inside HomepageActivity.java
        adapter = new NoteAdapter(noteList, (note, position) -> {
            // Single Click: Open Note
            Intent intent = new Intent(HomepageActivity.this, Note_EditorActivity.class);
            intent.putExtra("existing_note", note);
            intent.putExtra("notebook_id", "general_notes/" + currentUserId);
            startActivity(intent);
        }, (note, position) -> {
            // Long Click: Trigger Delete
            showDeleteConfirmation(note, position);
        });

        recyclerView.setAdapter(adapter);

        // 4. Navigation and Menu
        layoutNotebook.setOnClickListener(v -> startActivity(new Intent(this, NotebooksActivity.class)));
        layoutQuiz.setOnClickListener(v -> startActivity(new Intent(this, QuizActivity.class)));
        layoutFlashcard.setOnClickListener(v -> startActivity(new Intent(this, FlashcardActivity.class)));
        btnMenu.setOnClickListener(this::showPopupMenu);
    }

    private void showLoadingDialog() {
        loadingDialog = new android.app.ProgressDialog(this);
        loadingDialog.setMessage("AI is creating a study note from your file...");
        loadingDialog.setCancelable(false);
        loadingDialog.show();
    }
    private void handleBlockerRedirection() {
        // Only redirect if user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        DailyTaskManager.BlockerTask task = DailyTaskManager.getCurrentBlockerTask();
        if (task == null) return;

        // DO NOT call setDailyCompletion() here!
        // The user must complete the task first.

        if ("quiz".equals(task.type)) {
            fetchQuizAndLaunch(task.id);
        } else if ("flashcard".equals(task.type)) {
            fetchFlashcardAndLaunch(task.id);
        }
    }

    private void fetchQuizAndLaunch(String quizId) {
        String uid = FirebaseAuth.getInstance().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("quizzes")
                .child(uid).child(quizId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Quiz quiz = snapshot.getValue(Quiz.class);
                if (quiz != null) {
                    Intent intent = new Intent(HomepageActivity.this, TakeQuizActivity.class);
                    intent.putExtra("QUIZ_OBJECT", quiz);
                    startActivity(intent);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchFlashcardAndLaunch(String setId) {
        String uid = FirebaseAuth.getInstance().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("flashcards")
                .child(uid).child(setId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                FlashcardSet set = snapshot.getValue(FlashcardSet.class);
                if (set != null) {
                    Intent intent = new Intent(HomepageActivity.this, TakeCardActivity.class);
                    intent.putExtra("set_cards", set);
                    startActivity(intent);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void toggleFabMenu() {
        if (!isMenuOpen) {
            // Show buttons
            fabUpload.show();
            fabCreateNote.show();
            fabMain.setImageResource(android.R.drawable.ic_menu_close_clear_cancel); // Change + to X
            isMenuOpen = true;
        } else {
            // Hide buttons
            fabUpload.hide();
            fabCreateNote.hide();
            fabMain.setImageResource(android.R.drawable.ic_input_add); // Change X back to +
            isMenuOpen = false;
        }
    }

    private void toggleStudyMode(ImageView icon, TextView text) {
        // 1. Check if permission is granted
        if (!notificationManager.isNotificationPolicyAccessGranted()) {
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Please allow 'Do Not Disturb' access for NoteWise", Toast.LENGTH_LONG).show();
            return;
        }

        isStudyModeActive = !isStudyModeActive;

        if (isStudyModeActive) {
            // TURN ON: Silence notifications
            notificationManager.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY);

            // Change color to indicate it's active
            icon.setColorFilter(getResources().getColor(R.color.teal_accent));
            text.setTextColor(getResources().getColor(R.color.teal_accent));
            Toast.makeText(this, "Study Mode: Do Not Disturb ON", Toast.LENGTH_SHORT).show();
        } else {
            // TURN OFF: Restore notifications
            notificationManager.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL);

            // Restore grey color
            icon.setColorFilter(getResources().getColor(R.color.text_grey));
            text.setTextColor(getResources().getColor(R.color.text_grey));
            Toast.makeText(this, "Study Mode: Do Not Disturb OFF", Toast.LENGTH_SHORT).show();
        }
    }

    private String extractTextFromUri(Uri uri, String mimeType) throws IOException {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) {
                runOnUiThread(() -> Toast.makeText(this, "Cannot open file", Toast.LENGTH_SHORT).show());
                return null;
            }

            // Plain text
            if ("text/plain".equals(mimeType)) {
                try (java.util.Scanner scanner = new java.util.Scanner(is, "UTF-8")) {
                    scanner.useDelimiter("\\A");
                    return scanner.hasNext() ? scanner.next() : "";
                }
            }
            // PDF
            else if ("application/pdf".equals(mimeType)) {
                try {
                    com.tom_roush.pdfbox.pdmodel.PDDocument doc = com.tom_roush.pdfbox.pdmodel.PDDocument.load(is);
                    com.tom_roush.pdfbox.text.PDFTextStripper stripper = new com.tom_roush.pdfbox.text.PDFTextStripper();
                    String text = stripper.getText(doc);
                    doc.close();
                    if (text == null || text.trim().isEmpty()) {
                        runOnUiThread(() -> {
                            Toast.makeText(this,
                                    "This PDF contains no selectable text (scanned or image-based). Please use a text-based PDF.",
                                    Toast.LENGTH_LONG).show();
                        });
                        return null;
                    }
                    return text;
                } catch (Exception e) {
                    Log.e("PDF_EXTRACT", "PDF parsing failed", e);
                    runOnUiThread(() -> Toast.makeText(this, "PDF error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    return null;
                }
            }
            // DOCX (Word)
            else if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(mimeType)) {
                try {
                    org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument(is);
                    StringBuilder sb = new StringBuilder();
                    for (org.apache.poi.xwpf.usermodel.XWPFParagraph para : doc.getParagraphs()) {
                        sb.append(para.getText()).append("\n");
                    }
                    doc.close();
                    return sb.toString();
                } catch (Exception e) {
                    Log.e("DOCX_EXTRACT", "DOCX parsing failed", e);
                    runOnUiThread(() -> Toast.makeText(this, "DOCX parsing error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    return null;
                }
            }
            else {
                runOnUiThread(() -> Toast.makeText(this, "Unsupported file type: " + mimeType, Toast.LENGTH_SHORT).show());
                return null;
            }
        }
    }

    private void highlightNewNoteWhenAdded(String noteId) {
        pendingHighlightNoteId = noteId;
        // Force the adapter to re-check – the listener will soon add the note.
        // We'll check inside the listener.
    }

    private void processUploadedFile(Uri uri) {
        // Run on background thread
        new Thread(() -> {
            try {
                // 1. Get MIME type
                String mimeType = getContentResolver().getType(uri);
                if (mimeType == null) mimeType = "text/plain";

                // 2. Extract text
                String extractedText = extractTextFromUri(uri, mimeType);
                if (extractedText == null || extractedText.isEmpty()) {
                    String finalMimeType = mimeType;
                    runOnUiThread(() -> {
                        dismissLoadingDialog();
                        // Only show generic message if it wasn't already handled by PDF branch
                        // (PDF branch already shows its own toast)
                        if (!"application/pdf".equals(finalMimeType)) {
                            Toast.makeText(this, "Could not read file content.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }

                // Limit input to 3000 characters to keep AI output manageable
                if (extractedText.length() > 3000) {
                    extractedText = extractedText.substring(0, 3000);
                    Log.d("AI_INPUT", "Truncated input to 3000 chars");
                }

                // 3. Call AI to generate a structured note (title + HTML content)
                GeminiQuizHelper.generateStructuredNote(extractedText, new GeminiQuizHelper.StructuredNoteCallback() {
                    @Override
                    public void onSuccess(String title, String htmlContent) {
                        // 4. Save to Firebase (general notes)
                        saveAiGeneratedNote(title, htmlContent);
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            dismissLoadingDialog();
                            Toast.makeText(HomepageActivity.this,
                                    "AI error: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    dismissLoadingDialog();
                    Toast.makeText(HomepageActivity.this,
                            "Error processing file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void saveAiGeneratedNote(String title, String htmlContent) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference notesRef = FirebaseDatabase.getInstance()
                .getReference("notes")
                .child("general_notes")
                .child(userId);

        String noteId = notesRef.push().getKey();
        Note newNote = new Note();
        newNote.setId(noteId);
        newNote.setTitle(title);
        newNote.setContent(htmlContent);

        notesRef.child(noteId).setValue(newNote)
                .addOnSuccessListener(aVoid -> {
                    runOnUiThread(() -> {
                        dismissLoadingDialog();
                        Toast.makeText(HomepageActivity.this,
                                "Study note created: " + title, Toast.LENGTH_SHORT).show();

                        // The Firebase listener will automatically add the note.
                        // We just need to highlight it when it appears.
                        // Store the new note's ID so we can highlight it after the listener adds it.
                        highlightNewNoteWhenAdded(noteId);
                        Log.d("HIGHLIGHT", "Pending highlight ID set to " + noteId);
                    });
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        dismissLoadingDialog();
                        Toast.makeText(HomepageActivity.this,
                                "Failed to save note: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                });
    }

    private void highlightNewNote(int position) {
        highlightWithRetry(position, 0);
    }

    private void highlightWithRetry(int position, int attempt) {
        if (attempt > 5) {
            Log.d("HIGHLIGHT", "Failed to highlight after 5 attempts");
            return;
        }
        recyclerView.postDelayed(() -> {
            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
            if (holder != null && holder.itemView != null) {
                View itemView = holder.itemView;
                android.graphics.drawable.Drawable originalBg = itemView.getBackground();
                itemView.setBackgroundColor(getResources().getColor(R.color.teal_accent));
                itemView.postDelayed(() -> {
                    if (originalBg != null) {
                        itemView.setBackground(originalBg);
                    } else {
                        itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                    }
                }, 3000);
                Log.d("HIGHLIGHT", "Highlight applied at position " + position);
            } else {
                Log.d("HIGHLIGHT", "Retry attempt " + attempt + " for position " + position);
                highlightWithRetry(position, attempt + 1);
            }
        }, 150);
    }


    private void loadGeneralNotes() {
        notesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                noteList.clear();
                int newNotePosition = -1;
                for (DataSnapshot noteSnapshot : snapshot.getChildren()) {
                    Note note = noteSnapshot.getValue(Note.class);
                    if (note != null) {
                        note.setId(noteSnapshot.getKey());
                        noteList.add(0, note); // add at top
                        // Check if this is the note we just created
                        if (pendingHighlightNoteId != null && pendingHighlightNoteId.equals(note.getId())) {
                            newNotePosition = 0;
                            pendingHighlightNoteId = null;
                            Log.d("HIGHLIGHT", "Found note to highlight: " + note.getId());
                        }
                    }
                }
                adapter.notifyDataSetChanged();

                // Highlight the new note if found
                if (newNotePosition != -1) {
                    highlightNewNote(newNotePosition);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    Toast.makeText(HomepageActivity.this, "Sync failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };
        mDatabase.addValueEventListener(notesListener);
    }

    private void showPopupMenu(View view) {
        Context wrapper = new ContextThemeWrapper(this, R.style.CustomPopupMenuStyle);
        PopupMenu popup = new PopupMenu(wrapper, view);
        popup.getMenuInflater().inflate(R.menu.burger_menu, popup.getMenu());

        // Reflection to show icons in the popup menu
        try {
            java.lang.reflect.Field field = popup.getClass().getDeclaredField("mPopup");
            field.setAccessible(true);
            Object menuHelper = field.get(popup);
            menuHelper.getClass().getDeclaredMethod("setForceShowIcon", boolean.class).invoke(menuHelper, true);
        } catch (Exception e) { e.printStackTrace(); }

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_account) startActivity(new Intent(this, AccountActivity.class));
            else if (id == R.id.menu_notifications) startActivity(new Intent(this, NotificationsActivity.class));
            else if (id == R.id.menu_settings) startActivity(new Intent(this, SettingsActivity.class));
            else if (id == R.id.menu_calendar) startActivity(new Intent(this, CalendarActivity.class));
            else if (id == R.id.menu_achievements) {startActivity(new Intent(HomepageActivity.this, AchievementsActivity.class));
            }
            return true;
        });
        popup.show();
    }

    private void showDeleteConfirmation(Note note, int position) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteGeneralNote(note.getId());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteGeneralNote(String noteId) {
        // This targets your general notes path: notes/userId/noteId
        mDatabase.child(noteId).removeValue()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Start listening to Firebase every time the page becomes visible
        loadGeneralNotes();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Stop listening when the page is hidden to save battery and memory
        if (mDatabase != null && notesListener != null) {
            mDatabase.removeEventListener(notesListener);
        }
    }
}