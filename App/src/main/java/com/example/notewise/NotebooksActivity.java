package com.example.notewise;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

public class NotebooksActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotebookAdapter adapter;
    private List<Notebook> notebookList = new ArrayList<>();
    private DatabaseReference mDatabase;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notebooks);

        // 1. Initialize Firebase & Auth
        mDatabase = FirebaseDatabase.getInstance().getReference();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Setup UI Components
        recyclerView = findViewById(R.id.recyclerViewNotebooks);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotebookAdapter(notebookList, (notebook, position) -> {
            // This runs when you hold the notebook
            showDeleteNotebookDialog(notebook, position);
        });
        recyclerView.setAdapter(adapter);

        // 3. Navigation Logic (Home, Quiz, and Back)
        setupNavigation();

        // 4. Floating Action Button
        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> showAddNotebookDialog());

        // 5. Load Data
        loadNotebooks();
    }

    private void setupNavigation() {

        // Home Button (Bottom Nav)
        LinearLayout layoutHome = findViewById(R.id.layoutHome);
        layoutHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomepageActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // Quiz Button (Bottom Nav)
        LinearLayout layoutQuiz = findViewById(R.id.layoutQuiz);
        layoutQuiz.setOnClickListener(v -> {
            startActivity(new Intent(this, QuizActivity.class));
        });

        findViewById(R.id.layoutFlashcard).setOnClickListener(v -> {
            startActivity(new Intent(this, FlashcardActivity.class));
            finish();
        });
    }

    private void loadNotebooks() {
        mDatabase.child("users").child(currentUserId).child("notebooks")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        notebookList.clear();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Notebook notebook = data.getValue(Notebook.class);
                            if (notebook != null) {
                                notebook.setId(data.getKey());
                                notebookList.add(notebook);
                                fetchLiveNoteCount(notebook);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Only show toast if the user is still logged in
                        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                            Toast.makeText(NotebooksActivity.this, "Sync failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void fetchLiveNoteCount(Notebook notebook) {
        mDatabase.child("notes").child(notebook.getId())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        notebook.setNoteCount((int) snapshot.getChildrenCount());
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void showAddNotebookDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_notebook, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        EditText input = dialogView.findViewById(R.id.etNotebookName);
        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnCreate).setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                createNewNotebook(name);
                dialog.dismiss();
            } else {
                input.setError("Name required");
            }
        });
        dialog.show();
    }

    private void createNewNotebook(String name) {
        DatabaseReference ref = mDatabase.child("users").child(currentUserId).child("notebooks");
        String id = ref.push().getKey();
        if (id != null) {
            ref.child(id).setValue(new Notebook(name, 0));
        }
    }

    private void showDeleteNotebookDialog(Notebook notebook, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Notebook")
                .setMessage("Are you sure? This will delete '" + notebook.getName() + "' and all notes inside it.")
                .setPositiveButton("Delete Everything", (dialog, which) -> {
                    deleteNotebook(notebook.getId(), position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteNotebook(String notebookId, int position) {
        // 1. Path to the notebook metadata
        DatabaseReference notebookRef = mDatabase.child("users").child(currentUserId).child("notebooks").child(notebookId);

        // 2. Path to the actual notes inside that notebook
        DatabaseReference notesRef = FirebaseDatabase.getInstance().getReference("notes").child(currentUserId).child(notebookId);

        // Delete both locations
        notebookRef.removeValue();
        notesRef.removeValue().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Notebook and notes deleted", Toast.LENGTH_SHORT).show();
        });
    }
}