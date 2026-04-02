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

        // 1. Get Data from Intent
        // Ensure "TOTAL" matches the key used in TakeQuizActivity's finishQuiz()
        int score = getIntent().getIntExtra("SCORE", 0);
        int total = getIntent().getIntExtra("TOTAL_QUESTIONS", 0);
        String quizTitle = getIntent().getStringExtra("QUIZ_TITLE");
        String timeElapsed = getIntent().getStringExtra("TIME_ELAPSED");
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

        tvScore.setText(score + " / " + total);
        tvTime.setText(timeElapsed);
        if (quizTitle != null) tvTopicTitle.setText(quizTitle);

        if (total > 0) {
            int progress = (int) (((float) score / total) * 100);
            progressBar.setProgress(progress);
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
            // CASE: Partial Score -> 10 Minute Temporary Pass
            long tenMinutesFromNow = System.currentTimeMillis() + (10 * 1000);
            editor.putLong("temp_unlock_time", tenMinutesFromNow);
            android.widget.Toast.makeText(this, "Good effort! Unlocked for 10 minutes.", android.widget.Toast.LENGTH_LONG).show();
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

        btnDone.setOnClickListener(v -> {
            if (score < total) {
                Intent retryIntent = new Intent(this, TakeQuizActivity.class);
                retryIntent.putExtra("QUIZ_OBJECT", quizObject);
                retryIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(retryIntent);
            } else {
                startActivity(new Intent(this, HomepageActivity.class));
            }
            finish();
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