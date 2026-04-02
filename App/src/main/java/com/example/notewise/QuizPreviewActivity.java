package com.example.notewise;

import android.content.Intent;
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

public class QuizPreviewActivity extends AppCompatActivity {
    private TextView tvHeader, tvQuestionPreview, tvQuestionCount, tvEstTime;
    private LinearLayout optionsContainer;
    private SwitchMaterial switchTimer;
    private Spinner spinnerTime;
    private MaterialButton btnStartQuiz;

    // The Source of Truth
    private Quiz receivedQuiz;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_preview);

        initializeViews();
        receiveAndSetData();
        setupTimerLogic();

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

    private void updateEstTimeText() {
        if (switchTimer.isChecked() && spinnerTime.getSelectedItem() != null) {
            tvEstTime.setText("Estimated Time: " + spinnerTime.getSelectedItem().toString());
        } else {
            tvEstTime.setText("Estimated Time: No Limit");
        }
    }

    private void startQuizExecution() {
        if (receivedQuiz == null) return;

        Intent intent = new Intent(this, TakeQuizActivity.class);
        // PASS THE ENTIRE OBJECT FORWARD
        intent.putExtra("QUIZ_OBJECT", receivedQuiz);
        intent.putExtra("USE_TIMER", switchTimer.isChecked());
        intent.putExtra("SELECTED_TIME", spinnerTime.getSelectedItem().toString());

        startActivity(intent);
    }
}