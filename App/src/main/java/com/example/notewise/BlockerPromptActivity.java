package com.example.notewise;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class BlockerPromptActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocker_prompt);

        TextView tvMessage = findViewById(R.id.tvTaskMessage);
        DailyTaskManager.BlockerTask task = DailyTaskManager.getCurrentBlockerTask();

        if (task != null) {
            String msg = "Your " + (task.type.equals("quiz") ? "Quiz" : "Flashcard Set") +
                    " for today:\n\"" + task.title + "\"";
            tvMessage.setText(msg);
        } else {
            tvMessage.setText("Complete your daily active recall task in RecallNote");
        }

        // Wait 3 seconds, then actually launch the quiz or flashcards!
        findViewById(android.R.id.content).postDelayed(() -> {
            if (task != null) {
                String currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
                if (currentUid == null) return;

                if ("quiz".equals(task.type)) {
                    // FETCH FROM FIREBASE SO IT NEVER CRASHES
                    com.google.firebase.database.DatabaseReference ref = com.google.firebase.database.FirebaseDatabase.getInstance()
                            .getReference("quizzes").child(currentUid).child(task.id);

                    ref.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                        @Override
                        public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                            Quiz currentQuiz = snapshot.getValue(Quiz.class);
                            Intent quizIntent = new Intent(BlockerPromptActivity.this, TakeQuizActivity.class);
                            quizIntent.putExtra("QUIZ_OBJECT", currentQuiz);
                            quizIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(quizIntent);
                            finish();
                        }
                        @Override
                        public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                            finish();
                        }
                    });
                } else if ("flashcard".equals(task.type)) {
                    // ... (Keep your flashcard logic here if you need it later) ...
                }
            } else {
                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(homeIntent);
                finish();
            }
        }, 3000);
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Complete the quiz to unlock your phone!", Toast.LENGTH_SHORT).show();
        // Do NOT call super
    }
}