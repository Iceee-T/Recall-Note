package com.example.notewise;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
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

public class FlashcardActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FlashcardAdapter adapter;
    private List<FlashcardSet> flashcardList;
    private DatabaseReference mDatabase;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard);

        // Firebase Initialization
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            mDatabase = FirebaseDatabase.getInstance().getReference("flashcards").child(currentUserId);
        }

        // Initialize UI
        recyclerView = findViewById(R.id.recyclerViewFlashcards);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        flashcardList = new ArrayList<>();

        // Inside FlashcardActivity.java
        adapter = new FlashcardAdapter(flashcardList, set -> {
            Intent intent = new Intent(FlashcardActivity.this, TakeCardActivity.class);
            // Ensure this key matches what TakeCardActivity retrieves
            intent.putExtra("set_cards", set);
            startActivity(intent);
        }, (set, position) -> { // Fix: Added 'position' parameter here
            showDeleteDialog(set);
        });
        recyclerView.setAdapter(adapter);

        // Add Button (FAB)
        FloatingActionButton fabAdd = findViewById(R.id.fabAddFlashcard);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(FlashcardActivity.this, Generateflashcard.class);
            startActivity(intent);
        });

        setupBottomNav();
        fetchFlashcards();
        DailyTaskManager.updateDailyTask();
    }


    private void fetchFlashcards() {
        if (mDatabase == null) return;

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                flashcardList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    FlashcardSet set = ds.getValue(FlashcardSet.class);
                    if (set != null) {
                        set.setId(ds.getKey());
                        flashcardList.add(set);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FlashcardActivity.this, "Load failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBottomNav() {
        findViewById(R.id.layoutHome).setOnClickListener(v -> {
            startActivity(new Intent(this, HomepageActivity.class));
            finish();
        });

        findViewById(R.id.layoutQuiz).setOnClickListener(v -> {
            // Assuming your Quiz list activity is named QuizActivity
            startActivity(new Intent(this, QuizActivity.class));
            finish();
        });

        findViewById(R.id.layoutNotebook).setOnClickListener(v -> {
            startActivity(new Intent(this, NotebooksActivity.class));
            finish();
        });
        // layoutFlashcard is already active here
    }

    private void showDeleteDialog(FlashcardSet set) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete Flashcard Set")
                .setMessage("Are you sure you want to delete '" + set.getTitle() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Delete from Firebase using the unique ID of the set
                    if (set.getId() != null) {
                        mDatabase.child(set.getId()).removeValue()
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(FlashcardActivity.this, "Set deleted successfully", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(FlashcardActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        DailyTaskManager.updateDailyTask();
    }
}