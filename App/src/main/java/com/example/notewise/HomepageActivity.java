package com.example.notewise;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HomepageActivity extends AppCompatActivity {
    private FloatingActionButton fabMain;
    private com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton fabUpload, fabCreateNote;
    private boolean isMenuOpen = false;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

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
            toggleFabMenu(); // Close menu
            Toast.makeText(this, "Upload Clicked", Toast.LENGTH_SHORT).show();
            // Add your upload logic/intent here
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
        FloatingActionButton fab = findViewById(R.id.fab);
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

        // 5. FAB - Create new General Note inside the user's specific general folder
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, Note_EditorActivity.class);
            intent.putExtra("notebook_id", "general_notes/" + currentUserId);
            startActivity(intent);
        });

        // 6. Start Syncing
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

    private void loadGeneralNotes() {
        notesListener = new ValueEventListener() { // Store it here
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                noteList.clear();
                for (DataSnapshot noteSnapshot : snapshot.getChildren()) {
                    Note note = noteSnapshot.getValue(Note.class);
                    if (note != null) {
                        note.setId(noteSnapshot.getKey());
                        noteList.add(0, note);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // ADD THIS CHECK: Only show error if we didn't just sign out
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