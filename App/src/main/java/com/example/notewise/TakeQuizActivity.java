package com.example.notewise;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TakeQuizActivity extends AppCompatActivity {

    private List<QuestionModel> questionList;
    private ArrayList<Integer> userAnswers = new ArrayList<>(); // Track selected indices
    private int currentIdx = 0, score = 0, selectedOptionIndex = -1;
    private long startTimeMillis;

    private TextView tvQuizTitle, tvQuestionCount, tvQuestionText, tvTimer;
    private LinearLayout optionsContainer;
    private ProgressBar progressBar;
    private MaterialButton btnNext;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_quiz);

        initializeViews();

        Quiz quiz = (Quiz) getIntent().getSerializableExtra("QUIZ_OBJECT");
        if (quiz != null && quiz.getQuestions() != null && !quiz.getQuestions().isEmpty()) {
            currentQuiz = quiz;
            tvQuizTitle.setText(quiz.getTitle());
            this.questionList = quiz.getQuestions();

            // Initialize userAnswers with -1 (meaning no answer yet)
            for (int i = 0; i < questionList.size(); i++) userAnswers.add(-1);

            startTimeMillis = System.currentTimeMillis();
            loadQuestion();

            // Optional: Start timer if passed from previous screen
            if (getIntent().getBooleanExtra("USE_TIMER", false)) {
                startTimer(getIntent().getStringExtra("SELECTED_TIME"));
            }
        } else {
            fetchAnyQuizFromFirebase();
        }

        btnNext.setOnClickListener(v -> handleNext());
        findViewById(R.id.btnExit).setOnClickListener(v -> finish());
    }

    private Quiz currentQuiz;


    private void fetchAnyQuizFromFirebase() {
        String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        com.google.firebase.database.DatabaseReference mDatabase =
                com.google.firebase.database.FirebaseDatabase.getInstance().getReference("quizzes").child(currentUserId);

        mDatabase.limitToFirst(1).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChildren()) {
                    for (com.google.firebase.database.DataSnapshot quizSnapshot : snapshot.getChildren()) {
                        Quiz quiz = quizSnapshot.getValue(Quiz.class);
                        if (quiz != null && quiz.getQuestions() != null && !quiz.getQuestions().isEmpty()) {
                            // Successfully found a quiz! Now set it up.

                            currentQuiz = quiz;  // <-- store it
                            currentQuiz.setId(quizSnapshot.getKey()); // ensure ID is set

                            tvQuizTitle.setText(quiz.getTitle());
                            questionList = quiz.getQuestions();

                            userAnswers.clear();
                            for (int i = 0; i < questionList.size(); i++) userAnswers.add(-1);

                            startTimeMillis = System.currentTimeMillis();
                            loadQuestion();
                            return; // Exit once we find one
                        }
                    }
                } else {
                    // If the user has NO quizzes created, we can't block them effectively.
                    Toast.makeText(TakeQuizActivity.this, "Create a quiz first to enable blocking!", Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                finish();
            }
        });
    }

    private void initializeViews() {
        tvQuizTitle = findViewById(R.id.tvQuizTitle);
        tvQuestionCount = findViewById(R.id.tvQuestionCount);
        tvQuestionText = findViewById(R.id.tvQuestionText);
        tvTimer = findViewById(R.id.tvTimer);
        optionsContainer = findViewById(R.id.optionsContainer);
        progressBar = findViewById(R.id.quizProgressBar);
        btnNext = findViewById(R.id.btnNext);
    }

    private void startTimer(String timeStr) {
        if (timeStr == null) return;
        int minutes = Integer.parseInt(timeStr.split(" ")[0]);
        countDownTimer = new CountDownTimer(minutes * 60000, 1000) {
            @Override
            public void onTick(long millis) {
                int mins = (int) (millis / 60000);
                int secs = (int) (millis % 60000) / 1000;
                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs));
            }
            @Override
            public void onFinish() {
                tvTimer.setText("00:00");
                Toast.makeText(TakeQuizActivity.this, "Time's up!", Toast.LENGTH_SHORT).show();
                finishQuiz(); // Automatically ends the quiz and calculates the current score
            }
        }.start();
    }

    private void loadQuestion() {
        selectedOptionIndex = -1; // Reset for new question
        optionsContainer.removeAllViews();

        QuestionModel q = questionList.get(currentIdx);
        tvQuestionText.setText(q.getQuestionText());
        tvQuestionCount.setText("Question " + (currentIdx + 1) + " of " + questionList.size());

        // Update Progress
        progressBar.setProgress((int) (((float) (currentIdx + 1) / questionList.size()) * 100));

        // Dynamic Button Text
        btnNext.setText(currentIdx == questionList.size() - 1 ? "Finish Quiz" : "Next Question");

        // Generate Choice Buttons
        for (int i = 0; i < q.getChoices().size(); i++) {
            final int index = i;
            MaterialButton btn = new MaterialButton(this);
            btn.setText(q.getChoices().get(i));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 20);
            btn.setLayoutParams(params);
            btn.setPadding(48, 48, 48, 48);
            btn.setAllCaps(false);
            btn.setTextColor(Color.WHITE);
            btn.setBackgroundResource(R.drawable.option_button_bg); // Ensure this exists

            btn.setOnClickListener(v -> {
                selectedOptionIndex = index;
                userAnswers.set(currentIdx, index);
                highlightSelection(index);
            });
            optionsContainer.addView(btn);
        }
    }

    private void highlightSelection(int index) {
        for (int i = 0; i < optionsContainer.getChildCount(); i++) {
            MaterialButton b = (MaterialButton) optionsContainer.getChildAt(i);
            if (i == index) {
                b.setStrokeWidth(4);
                b.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#D1E3E7")));
                b.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#26D1E3E7")));
            } else {
                b.setStrokeWidth(0);
                b.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1E1E1E")));
            }
        }
    }

    private void handleNext() {
        if (selectedOptionIndex == -1) {
            Toast.makeText(this, "Please select an answer", Toast.LENGTH_SHORT).show();
            return;
        }

        // Scoring
        if (selectedOptionIndex == questionList.get(currentIdx).getCorrectOptionIndex()) {
            score++;
        }

        if (currentIdx < questionList.size() - 1) {
            currentIdx++;
            loadQuestion();
        } else {
            finishQuiz();
        }
    }

    private void finishQuiz() {
        // 1. Stop the background timer
        if (countDownTimer != null) countDownTimer.cancel();

        // 2. Bulletproof Time Calculation (New)
        long timeTakenMillis = System.currentTimeMillis() - startTimeMillis;
        int minutes = (int) (timeTakenMillis / 1000) / 60;
        int seconds = (int) (timeTakenMillis / 1000) % 60;
        String finalTimeString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

        // 3. Update the quiz's next review date in Firebase (Your original logic)
        if (currentQuiz != null) {
            long newNextReview = System.currentTimeMillis() + 24 * 60 * 60 * 1000; // 1 day
            currentQuiz.setNextReview(newNextReview);
            DatabaseReference quizRef = FirebaseDatabase.getInstance().getReference("quizzes")
                    .child(FirebaseAuth.getInstance().getUid())
                    .child(currentQuiz.getId());
            quizRef.child("nextReview").setValue(newNextReview);
        }

        // 4. Prepare the Intent and send all the data
        Intent intent = new Intent(this, QuizResultActivity.class);
        intent.putExtra("SCORE", score);
        intent.putExtra("TOTAL_QUESTIONS", questionList.size());
        intent.putExtra("QUIZ_OBJECT", getIntent().getSerializableExtra("QUIZ_OBJECT"));
        intent.putExtra("QUESTIONS", (java.io.Serializable) questionList);
        intent.putIntegerArrayListExtra("USER_ANSWERS", userAnswers);

        // 5. Send the fixed time
        intent.putExtra("TIME_TAKEN", finalTimeString);
        intent.putExtra("TIMER_ENABLED", countDownTimer != null);

        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // We call the toast to inform the user
        super.onBackPressed();
        Toast.makeText(this, "Complete the quiz to unlock your phone!", Toast.LENGTH_SHORT).show();

        // To satisfy Android Studio's requirement for the super call
        // without actually closing the activity, we just don't call it.
        // If your compiler STOPS you from building, use this line:
        // super.onBackPressed();

        // NOTE: If you MUST call super to pass the build error,
        // the activity WILL close. In that case, the AppBlockerService
        // will detect the window change and immediately relaunch the quiz anyway.
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        finish(); // Kills the activity so the blocker can instantly restart it if they try to switch apps
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) {
            finish();
        }
    }
}