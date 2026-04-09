package com.example.notewise;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.Calendar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.Calendar;

public class QuizResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_result);
        boolean isTimerEnabled = getIntent().getBooleanExtra("TIMER_ENABLED", false);

        // 1. Get Data from Intent
        // Ensure "TOTAL" matches the key used in TakeQuizActivity's finishQuiz()
        int score = getIntent().getIntExtra("SCORE", 0);
        int total = getIntent().getIntExtra("TOTAL_QUESTIONS", 0);
        String quizTitle = getIntent().getStringExtra("QUIZ_TITLE");
        String timeElapsed = getIntent().getStringExtra("TIME_TAKEN");
        Quiz quizObject = (Quiz) getIntent().getSerializableExtra("QUIZ_OBJECT");

        ArrayList<QuestionModel> questions = (ArrayList<QuestionModel>) getIntent().getSerializableExtra("QUESTIONS");
        ArrayList<Integer> userAnswers = getIntent().getIntegerArrayListExtra("USER_ANSWERS");

        // Null-safety checks
        if (questions == null) questions = new ArrayList<>();
        if (userAnswers == null) userAnswers = new ArrayList<>();
        if (timeElapsed == null) timeElapsed = "00:00";

        // 2. Update Primary UI Elements
        TextView tvScore = findViewById(R.id.tvScoreRaw);
        TextView tvTopicTitle = findViewById(R.id.tvTopicTitle);
        TextView tvTime = findViewById(R.id.tvTime);
        ProgressBar progressBar = findViewById(R.id.resultProgressBar);
        com.google.android.material.button.MaterialButton btnDone = findViewById(R.id.btnDone);
        TextView tvPercent = findViewById(R.id.tvPercent);

        tvScore.setText(score + " / " + total);
        tvTime.setText(timeElapsed);
        if (quizTitle != null) {
            tvTopicTitle.setText(quizTitle);
        } else if (quizObject != null) {
            // Use your Quiz object's getter method (might be getTitle() or getName() depending on your model)
            tvTopicTitle.setText(quizObject.getTitle());
        } else {
            tvTopicTitle.setText("Daily Quiz"); // A safe fallback if both are missing
        }

        if (isTimerEnabled) {
            tvTime.setText(timeElapsed);
            tvTime.setVisibility(View.VISIBLE); // Show the time
        } else {
            tvTime.setVisibility(View.GONE); // Completely hide the TextView!

            // TIP: If you have a clock icon or a "Time:" label next to tvTime,
            // you should grab that view and set it to View.GONE right here too!
        }

        if (total > 0) {
            int progress = (int) (((float) score / total) * 100);
            progressBar.setProgress(progress);
            // ADDED: Update the actual text on the screen to match the progress!
            tvPercent.setText(progress + "%");
        } else {
            progressBar.setProgress(0);
            tvPercent.setText("0%");
        }

        // 3. App Blocker Reward & Penalty Logic
        android.content.SharedPreferences prefs = getSharedPreferences("ActiveRecallPrefs", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();

        if (score == total && total > 0) {
            // CASE: 100% -> Grant Full Daily Pass
            String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
            editor.putString("last_success_date", today);
            editor.putLong("temp_unlock_time", 0); // Clear any temp timers
            android.widget.Toast.makeText(this, "Mastery Achieved! Social Media Unlocked.", android.widget.Toast.LENGTH_LONG).show();
        }
        else if (score == 0) {
            // CASE: 0% -> Strict Block
            editor.putLong("temp_unlock_time", 0);
            android.widget.Toast.makeText(this, "Score 0. You must retake to unlock.", android.widget.Toast.LENGTH_SHORT).show();
        }
        else {
            // 1 minute = 60 seconds * 1000 milliseconds
            long oneMinuteFromNow = System.currentTimeMillis() + (60 * 1000);

            editor.putLong("temp_unlock_time", oneMinuteFromNow);

// Don't forget to update the Toast message so the user knows!
            Toast.makeText(this, "Good effort! Unlocked for 1 minute.", Toast.LENGTH_LONG).show();
        }
        editor.apply();

        // 4. Initialize Feedback RecyclerView
        RecyclerView recyclerView = findViewById(R.id.rvFeedback);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<FeedbackItem> feedbackItems = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            QuestionModel q = questions.get(i);
            int userChoice = (i < userAnswers.size()) ? userAnswers.get(i) : -1;
            boolean isCorrect = (userChoice == q.getCorrectOptionIndex());

            String answerText = (userChoice != -1) ? q.getChoices().get(userChoice) : "Skipped";
            feedbackItems.add(new FeedbackItem(i + 1, q.getQuestionText(), answerText, isCorrect));
        }
        recyclerView.setAdapter(new FeedbackAdapter(feedbackItems));

        // 5. Handle Done / Retake Button
        if (score < total) {
            btnDone.setText("Retake Quiz");
        }

        findViewById(R.id.btnDone).setOnClickListener(v -> {
            // 1. Calculate the percentage score
            double percentage = 0;
            if (total > 0) {
                percentage = ((double) score / total) * 100;
            }

            // 2. Decide if they passed (e.g., 70% or higher)
            if (percentage >= 70) {
                // THEY PASSED! Tear up the blocker task and unlock the phone.
                String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
                prefs.edit()
                        .putString("last_success_date", today)
                        .remove("temp_unlock_time")
                        .apply();

                DailyTaskManager.clearBlockerTask();
                Toast.makeText(this, "Quiz Passed! Apps Unlocked.", Toast.LENGTH_LONG).show();

                // Close the result screen normally
                finish();

            } else {
                // THEY FAILED! Do NOT unlock the phone.
                Toast.makeText(this, "Score too low (" + (int)percentage + "%). Retake the quiz to unlock apps!", Toast.LENGTH_LONG).show();

                // NEW LOGIC: Instantly throw the Blocker Screen in their face!
                boolean optInEnabled = prefs.getBoolean("opt_in_enabled", false);

                if (optInEnabled) {
                    Intent intent = new Intent(this, BlockerPromptActivity.class);
                    // CLEAR_TASK ensures they can't hit the back button to escape it
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    // If they are just taking a normal quiz (no blocker), let them leave normally
                    finish();
                }
            }
        });
    }

    private static class FeedbackItem {
        int questionNum; String questionText; String answerText; boolean isCorrect;
        FeedbackItem(int num, String q, String a, boolean correct) {
            this.questionNum = num; this.questionText = q; this.answerText = a; this.isCorrect = correct;
        }
    }

    private class FeedbackAdapter extends RecyclerView.Adapter<FeedbackAdapter.ViewHolder> {
        private final List<FeedbackItem> items;
        FeedbackAdapter(List<FeedbackItem> items) { this.items = items; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feedback, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FeedbackItem item = items.get(position);
            holder.tvNum.setText("Question #" + item.questionNum);
            holder.tvQuestion.setText(item.questionText);
            holder.tvAnswer.setText(item.answerText);

            if (item.isCorrect) {
                // Correct Answer Logic
                holder.ivIcon.setImageResource(R.drawable.ic_check);
                holder.ivIcon.setColorFilter(Color.parseColor("#76FF03"));
                holder.tvAnswer.setTextColor(Color.parseColor("#76FF03"));
            } else {
                // Wrong Answer Logic with X icon
                holder.ivIcon.setImageResource(R.drawable.ic_wrong);
                holder.ivIcon.setColorFilter(Color.parseColor("#FF1744"));
                holder.tvAnswer.setTextColor(Color.parseColor("#FF1744"));
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNum, tvQuestion, tvAnswer;
            ImageView ivIcon;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvNum = itemView.findViewById(R.id.tvQuestionNum);
                tvQuestion = itemView.findViewById(R.id.tvQuestionText);
                tvAnswer = itemView.findViewById(R.id.tvUserAnswer);
                ivIcon = itemView.findViewById(R.id.ivStatusIcon);
            }
        }
    }
}