package com.example.notewise;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class NotesListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NoteAdapter adapter;
    private List<Note> noteList = new ArrayList<>();
    private String notebookId, notebookName;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_list);

        // 1. Get Data passed from NotebooksActivity
        notebookId = getIntent().getStringExtra("notebook_id");
        notebookName = getIntent().getStringExtra("notebook_name");

        if (notebookId == null) {
            Toast.makeText(this, "Error: Notebook not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Setup Header
        TextView tvHeader = findViewById(R.id.tvNotebookTitle);
        tvHeader.setText(notebookName);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // 3. Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference("notes").child(notebookId);

        // 4. Setup RecyclerView with Click Listener
        recyclerView = findViewById(R.id.recyclerViewNotes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new NoteAdapter(noteList, (note, position) -> {
            Intent intent = new Intent(NotesListActivity.this, Note_EditorActivity.class);
            intent.putExtra("existing_note", note);
            intent.putExtra("notebook_id", notebookId);
            startActivity(intent);
        }, (note, position) -> {
            // This is the new Long Click Listener
            showDeleteConfirmation(note, position);
        });
        recyclerView.setAdapter(adapter);

        // 5. FAB - Create new note in THIS notebook
        FloatingActionButton fab = findViewById(R.id.fabAddNote);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, Note_EditorActivity.class);
            intent.putExtra("notebook_id", notebookId);
            startActivity(intent);
        });

        // 6. Navigation Bar Logic
        setupBottomNav();

        loadNotes();
    }

    private void setupBottomNav() {
        findViewById(R.id.layoutHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, HomepageActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        findViewById(R.id.layoutQuiz).setOnClickListener(v -> {
            startActivity(new Intent(this, QuizActivity.class));
        });

        findViewById(R.id.layoutNotebook).setOnClickListener(v -> {
            recyclerView.smoothScrollToPosition(0);
        });

        findViewById(R.id.layoutFlashcard).setOnClickListener(v -> {
            startActivity(new Intent(this, FlashcardActivity.class));
            finish();
        });
    }

    private void loadNotes() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                noteList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Note note = data.getValue(Note.class);
                    if (note != null) {
                        note.setId(data.getKey());
                        noteList.add(0, note);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(NotesListActivity.this, "Sync Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirmation(Note note, int position) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteNote(note.getId(), position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteNote(String noteId, int position) {
        // In this activity, mDatabase is: notes/userId/notebookId
        mDatabase.child(noteId).removeValue().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
            // The list will auto-refresh because you have a ValueEventListener in loadNotes()
        });
    }
}