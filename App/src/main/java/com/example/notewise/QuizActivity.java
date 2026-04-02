package com.example.notewise;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class QuizActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private QuizAdapter adapter;
    private List<Quiz> quizList = new ArrayList<>();

    private DatabaseReference mDatabase;
    private String currentUserId;
    private TextView tvHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        // 1. Security check
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference("quizzes").child(currentUserId);

        // 2. Initialize UI
        tvHeader = findViewById(R.id.tvHeader);
        recyclerView = findViewById(R.id.recyclerViewQuizzes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new QuizAdapter(this, quizList, quiz -> {
            // Single click (handled by existing listener)
        }, (quiz, position) -> {
            // This is the new Long Click:
            showDeleteQuizDialog(quiz, position);
        });
        recyclerView.setAdapter(adapter);

        // 3. Check if we arrived from AI Generation or regular Navigation
        if (getIntent().hasExtra("AI_QUESTIONS")) {
            handleAiGeneratedData();
        } else {
            loadSavedQuizzes();
        }

        setupNavigation();
    }

    /**
     * Logic for handling temporary AI-generated quizzes.
     */
    private void handleAiGeneratedData() {
        ArrayList<QuestionModel> aiQuestions = (ArrayList<QuestionModel>) getIntent().getSerializableExtra("AI_QUESTIONS");

        if (aiQuestions != null && !aiQuestions.isEmpty()) {
            tvHeader.setText("Review AI Quiz");

            // Create a temporary Quiz object using your AI-optimized constructor
            Quiz aiQuiz = new Quiz("AI Quiz: " + System.currentTimeMillis() / 1000, aiQuestions);
            aiQuiz.setDescription("Generated from your notes");

            quizList.clear();
            quizList.add(aiQuiz);
            adapter.notifyDataSetChanged();

            // Prompt user to save the quiz permanently
            showSavePrompt(aiQuiz);
        } else {
            loadSavedQuizzes();
        }
    }

    private void showSavePrompt(Quiz aiQuiz) {
        new AlertDialog.Builder(this)
                .setTitle("Save AI Quiz?")
                .setMessage("This quiz is temporary. Would you like to save it to your collection permanently?")
                .setPositiveButton("Save", (dialog, which) -> saveQuizToFirebase(aiQuiz))
                .setNegativeButton("Just Review", null)
                .show();
    }

    private void saveQuizToFirebase(Quiz quiz) {
        String quizId = mDatabase.push().getKey();
        if (quizId != null) {
            quiz.setId(quizId);
            mDatabase.child(quizId).setValue(quiz)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Quiz saved successfully!", Toast.LENGTH_SHORT).show();
                        loadSavedQuizzes(); // Switch view to all saved quizzes
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error saving quiz", Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * Standard logic to load quizzes from Firebase Realtime Database.
     */
    private void loadSavedQuizzes() {
        tvHeader.setText("Quizzes");
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                quizList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Quiz quiz = ds.getValue(Quiz.class);
                    if (quiz != null) {
                        quiz.setId(ds.getKey());
                        quizList.add(quiz);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(QuizActivity.this, "Database Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupNavigation() {
        findViewById(R.id.layoutNotebook).setOnClickListener(v -> {
            startActivity(new Intent(this, NotebooksActivity.class));
            finish();
        });

        // RESTORED DESIGN: Goes directly to AddquizActivity
        findViewById(R.id.fabAdd).setOnClickListener(v -> {
            startActivity(new Intent(QuizActivity.this, AddquizActivity.class));
        });

        findViewById(R.id.layoutHome).setOnClickListener(v -> {
            startActivity(new Intent(this, HomepageActivity.class));
            finish();
        });

        findViewById(R.id.layoutFlashcard).setOnClickListener(v -> {
            startActivity(new Intent(this, FlashcardActivity.class));
            finish();
        });
    }

    private void showDeleteQuizDialog(Quiz quiz, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Quiz")
                .setMessage("Are you sure you want to delete '" + quiz.getTitle() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteQuizFromFirebase(quiz.getId());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteQuizFromFirebase(String quizId) {
        // This removes the quiz metadata and all questions nested inside it
        mDatabase.child(quizId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Quiz deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}