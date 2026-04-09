package com.example.notewise;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.ArrayList;
import java.util.Calendar;

public class QuizPreviewActivity extends AppCompatActivity {
    private TextView tvHeader, tvQuestionPreview, tvQuestionCount, tvEstTime;
    private LinearLayout optionsContainer;
    private SwitchMaterial switchTimer;
    private Spinner spinnerTime;
    private MaterialButton btnStartQuiz;

    // The Source of Truth
    private Quiz receivedQuiz;

    // Add these below your other variables:
    private SwitchMaterial switchActiveRecall;
    private TextView tvActiveRecallDate;
    private long expirationDateMillis = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_preview);
        switchActiveRecall = findViewById(R.id.switchActiveRecall);
        tvActiveRecallDate = findViewById(R.id.tvActiveRecallDate);

        initializeViews();
        receiveAndSetData();
        setupTimerLogic();
        setupActiveRecallLogic();


        btnStartQuiz.setOnClickListener(v -> startQuizExecution());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

    }

    private void initializeViews() {
        tvHeader = findViewById(R.id.tvHeader);
        tvQuestionPreview = findViewById(R.id.tvQuestionPreview);
        tvQuestionCount = findViewById(R.id.tvQuestionCount);
        tvEstTime = findViewById(R.id.tvEstTime);
        optionsContainer = findViewById(R.id.optionsContainer);
        switchTimer = findViewById(R.id.switchTimer);
        spinnerTime = findViewById(R.id.spinnerTime);
        btnStartQuiz = findViewById(R.id.btnStartQuiz);
    }

    private void receiveAndSetData() {
        // Retrieve the full Quiz object sent from the QuizAdapter
        receivedQuiz = (Quiz) getIntent().getSerializableExtra("SELECTED_QUIZ");

        if (receivedQuiz != null) {
            tvHeader.setText(receivedQuiz.getTitle());
            tvQuestionCount.setText(receivedQuiz.getQuestionCount() + " Questions");

            // Populate the Preview Question block
            if (receivedQuiz.getQuestions() != null && !receivedQuiz.getQuestions().isEmpty()) {
                QuestionModel firstQ = receivedQuiz.getQuestions().get(0);
                tvQuestionPreview.setText(firstQ.getQuestionText());

                // Show choices for the first question in preview
                optionsContainer.removeAllViews();
                for (String choice : firstQ.getChoices()) {
                    TextView choiceTv = new TextView(this);
                    choiceTv.setText("• " + choice);
                    choiceTv.setTextColor(android.graphics.Color.GRAY);
                    choiceTv.setPadding(0, 8, 0, 8);
                    optionsContainer.addView(choiceTv);
                }
            }
        } else {
            Toast.makeText(this, "Error: Quiz data missing", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupTimerLogic() {
        String[] times = {"5 mins", "10 mins", "15 mins", "30 mins"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, times);
        spinnerTime.setAdapter(adapter);

        // ADD THIS: Listen for changes when a user picks a new time from the dropdown
        spinnerTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateEstTimeText(); // Update the text immediately
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        switchTimer.setOnCheckedChangeListener((buttonView, isChecked) -> {
            spinnerTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            updateEstTimeText();
        });
    }

    private void setupActiveRecallLogic() {
        switchActiveRecall.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                showDatePicker();
            } else {
                tvActiveRecallDate.setVisibility(View.GONE);
                expirationDateMillis = 0; // Reset the date if they turn it off
            }
        });

        // Let them click the text to change the date if they made a mistake
        tvActiveRecallDate.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    // Save the exact millisecond of the end of the day they picked
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth, 23, 59, 59);
                    expirationDateMillis = selectedDate.getTimeInMillis();

                    String dateString = (month + 1) + "/" + dayOfMonth + "/" + year;
                    tvActiveRecallDate.setText("Blocker Active Until: " + dateString);
                    tvActiveRecallDate.setVisibility(View.VISIBLE);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        // This prevents users from picking a date in the past!
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void updateEstTimeText() {
        if (switchTimer.isChecked() && spinnerTime.getSelectedItem() != null) {
            tvEstTime.setText("Estimated Time: " + spinnerTime.getSelectedItem().toString());
        } else {
            tvEstTime.setText("Estimated Time: No Limit");
        }
    }

    private void startQuizExecution() {
        if (receivedQuiz == null) return;

        // NEW FAIL-SAFE: Stop them from starting if the switch is on but no date is set!
        if (switchActiveRecall.isChecked() && expirationDateMillis == 0) {
            Toast.makeText(this, "Please select an expiration date for the blocker!", Toast.LENGTH_SHORT).show();
            // Show the picker again so they can fix it
            showDatePicker();
            return; // Stop the code here so the quiz doesn't start
        }

        // 1. Save the Blocker Settings (If enabled)
        if (switchActiveRecall.isChecked() && expirationDateMillis > 0) {
            SharedPreferences prefs = getSharedPreferences("ActiveRecallPrefs", MODE_PRIVATE);
            prefs.edit()
                    .putBoolean("opt_in_enabled", true)
                    .putString("opt_in_task_id", receivedQuiz.getId())
                    .putLong("opt_in_expiration", expirationDateMillis)
                    .putString("blocker_task_type", "quiz")
                    .putString("blocker_task_id", receivedQuiz.getId())
                    .putString("blocker_task_title", receivedQuiz.getTitle())
                    // ERASE PREVIOUS UNLOCKS:
                    .remove("last_success_date")
                    .remove("temp_unlock_time")
                    .apply();
        }

        // 2. --- THIS WAS THE MISSING PART! ---
        // Actually launch the TakeQuizActivity
        Intent intent = new Intent(this, TakeQuizActivity.class);
        intent.putExtra("QUIZ_OBJECT", receivedQuiz);
        intent.putExtra("USE_TIMER", switchTimer.isChecked());
        intent.putExtra("SELECTED_TIME", spinnerTime.getSelectedItem().toString());

        startActivity(intent);
    }
}