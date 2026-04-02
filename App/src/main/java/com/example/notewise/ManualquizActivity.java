package com.example.notewise;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class ManualquizActivity extends AppCompatActivity {

    private RecyclerView rvQuestions;
    private QuestionAdapter adapter;
    private List<QuestionModel> questionList = new ArrayList<>();
    private EditText etQuizTitle;
    private TextView tvQuestionCounter;
    private DatabaseReference mDatabase;
    private LinearLayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manualquiz);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        rvQuestions = findViewById(R.id.rvQuestions);
        etQuizTitle = findViewById(R.id.etQuizTitle);
        tvQuestionCounter = findViewById(R.id.tvQuestionCounter);

        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        rvQuestions.setLayoutManager(layoutManager);
        rvQuestions.setHasFixedSize(false);

        if (questionList.isEmpty()) {
            questionList.add(new QuestionModel());
        }

        adapter = new QuestionAdapter(questionList);
        rvQuestions.setAdapter(adapter);

        // Reset counter to total/total when focusing on the Quiz Title
        etQuizTitle.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) updateQuestionCounter(-1);
        });

        findViewById(R.id.tvCancel).setOnClickListener(v -> finish());
        findViewById(R.id.tvCreate).setOnClickListener(v -> saveQuizToFirebase());
        findViewById(R.id.btnAddQuestion).setOnClickListener(v -> addNewQuestion());
        findViewById(R.id.btnDelete).setOnClickListener(v -> deleteSelectedQuestions());

        updateQuestionCounter(-1);
    }

    private void addNewQuestion() {
        questionList.add(new QuestionModel());
        int newPosition = questionList.size() - 1;
        adapter.notifyItemInserted(newPosition);
        rvQuestions.smoothScrollToPosition(newPosition);
        updateQuestionCounter(newPosition);
    }

    private void deleteSelectedQuestions() {
        List<QuestionModel> toRemove = new ArrayList<>();
        for (QuestionModel q : questionList) {
            if (q.isSelected()) toRemove.add(q);
        }

        if (toRemove.isEmpty()) {
            Toast.makeText(this, "Select questions to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        if (questionList.size() - toRemove.size() < 1) {
            Toast.makeText(this, "At least one question is required", Toast.LENGTH_SHORT).show();
            return;
        }

        questionList.removeAll(toRemove);
        adapter.notifyDataSetChanged();
        updateQuestionCounter(-1);
    }

    // Logic for dynamic counter
    public void updateQuestionCounter(int editingPosition) {
        if (tvQuestionCounter != null) {
            int total = questionList.size();
            int displayPos;

            if (editingPosition != -1) {
                // Show the specific question number being edited
                displayPos = editingPosition + 1;
            } else {
                // If not editing, show either the first visible item or total/total
                int scrollPos = layoutManager.findFirstVisibleItemPosition();
                if (scrollPos != -1 && rvQuestions.hasFocus()) {
                    displayPos = scrollPos + 1;
                } else {
                    displayPos = total; // Default "5 / 5" style
                }
            }
            tvQuestionCounter.setText(displayPos + " / " + total);
        }
    }

    private void saveQuizToFirebase() {
        String title = etQuizTitle.getText().toString().trim();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || title.isEmpty()) {
            if (title.isEmpty()) etQuizTitle.setError("Title required");
            return;
        }

        for (int i = 0; i < questionList.size(); i++) {
            if (questionList.get(i).getQuestionText().trim().isEmpty()) {
                Toast.makeText(this, "Question " + (i + 1) + " is empty", Toast.LENGTH_SHORT).show();
                rvQuestions.scrollToPosition(i);
                return;
            }
        }

        DatabaseReference userQuizzesRef = mDatabase.child("quizzes").child(user.getUid());
        String quizId = userQuizzesRef.push().getKey();

        if (quizId != null) {
            Quiz newQuiz = new Quiz(title, "Manual Quiz", questionList.size());
            newQuiz.setId(quizId);
            newQuiz.setQuestions(questionList);

            userQuizzesRef.child(quizId).setValue(newQuiz)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Quiz Saved!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
}