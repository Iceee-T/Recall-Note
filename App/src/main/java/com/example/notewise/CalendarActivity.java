package com.example.notewise;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private GridLayout calendarGrid;
    private TextView tvMonthYear, tvLabel, tvFlashHeader, tvQuizHeader;
    private RecyclerView rvFlashcards, rvQuizzes;
    private View selectedView = null;
    private Calendar displayCalendar;
    private Calendar selectedDate; // Persistent selection state

    private DatabaseReference flashcardRef, quizRef;
    private List<FlashcardSet> allFlashcardSets = new ArrayList<>();
    private List<Quiz> allQuizzes = new ArrayList<>();
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        // 1. Initialize Views
        calendarGrid = findViewById(R.id.calendarGrid);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        tvLabel = findViewById(R.id.tvLabel);
        tvFlashHeader = findViewById(R.id.tvFlashcardHeader);
        tvQuizHeader = findViewById(R.id.tvQuizHeader);
        rvFlashcards = findViewById(R.id.rvFlashcards);
        rvQuizzes = findViewById(R.id.rvQuizzes);

        rvFlashcards.setLayoutManager(new LinearLayoutManager(this));
        rvQuizzes.setLayoutManager(new LinearLayoutManager(this));

        displayCalendar = Calendar.getInstance();
        selectedDate = Calendar.getInstance();

        // 2. Navigation Listeners
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnPrevMonth).setOnClickListener(v -> {
            displayCalendar.add(Calendar.MONTH, -1);
            refreshUI();
        });
        findViewById(R.id.btnNextMonth).setOnClickListener(v -> {
            displayCalendar.add(Calendar.MONTH, 1);
            refreshUI();
        });

        // 3. Firebase Setup
        currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) { finish(); return; }

        flashcardRef = FirebaseDatabase.getInstance().getReference("flashcards").child(currentUid);
        quizRef = FirebaseDatabase.getInstance().getReference("quizzes").child(currentUid);

        loadData();

        findViewById(R.id.btnResetAll).setOnClickListener(v -> {
            // Show a confirmation dialog so they don't click it by accident!
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Fresh Start")
                    .setMessage("This will clear all scheduled reviews and unblock your apps. Continue?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        DailyTaskManager.resetAllSchedules(this);
                        Toast.makeText(this, "All tasks cleared. Fresh start active!", Toast.LENGTH_SHORT).show();

                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    private void loadData() {
        flashcardRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allFlashcardSets.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    FlashcardSet set = ds.getValue(FlashcardSet.class);
                    if (set != null) { set.setId(ds.getKey()); allFlashcardSets.add(set); }
                }
                loadQuizzes();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadQuizzes() {
        quizRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allQuizzes.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Quiz quiz = ds.getValue(Quiz.class);
                    if (quiz != null) { quiz.setId(ds.getKey()); allQuizzes.add(quiz); }
                }
                refreshUI();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void refreshUI() {
        generateCalendar();
        showTasksForDate(selectedDate);
    }

    private void generateCalendar() {
        calendarGrid.removeAllViews();
        tvMonthYear.setText(displayCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()).toUpperCase()
                + " " + displayCalendar.get(Calendar.YEAR));

        String[] days = {"S", "M", "T", "W", "T", "F", "S"};
        for (String day : days) {
            TextView tv = new TextView(this);
            tv.setText(day);
            tv.setTextColor(Color.GRAY);
            tv.setGravity(Gravity.CENTER);
            addToGrid(tv, 0, -1);
        }

        Calendar tempCal = (Calendar) displayCalendar.clone();
        tempCal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOffset = tempCal.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        Calendar today = Calendar.getInstance();
        long startOfToday = getStartOfDay(today);

        for (int i = 1; i <= daysInMonth; i++) {
            final int dayNum = i;
            View dayView = LayoutInflater.from(this).inflate(R.layout.calendar_day_item, calendarGrid, false);
            TextView tvDay = dayView.findViewById(R.id.tvDay);
            View dot = dayView.findViewById(R.id.dotIndicator);

            tvDay.setText(String.valueOf(dayNum));
            Calendar cellDate = (Calendar) displayCalendar.clone();
            cellDate.set(Calendar.DAY_OF_MONTH, dayNum);

            boolean hasFlashcard = false;
            boolean hasQuiz = false;
            boolean isOverdue = false;

            for (FlashcardSet set : allFlashcardSets) {
                if (set.getTimestamp() != null && !set.getTimestamp().isEmpty()) {
                    long ts = Long.parseLong(set.getTimestamp());
                    if (isSameDay(ts, cellDate)) {
                        hasFlashcard = true;
                        if (ts < startOfToday) isOverdue = true;
                    }
                }
            }

            for (Quiz q : allQuizzes) {
                if (isSameDay(q.getNextReview(), cellDate)) {
                    hasQuiz = true;
                    if (q.getNextReview() < startOfToday) isOverdue = true;
                }
            }

            if (isOverdue) {
                dot.setBackgroundColor(Color.parseColor("#FF5252"));
                dot.setVisibility(View.VISIBLE);
            } else if (hasFlashcard && hasQuiz) {
                dot.setBackgroundColor(Color.WHITE);
                dot.setVisibility(View.VISIBLE);
            } else if (hasFlashcard) {
                dot.setBackgroundColor(Color.parseColor("#4FC3F7"));
                dot.setVisibility(View.VISIBLE);
            } else if (hasQuiz) {
                dot.setBackgroundColor(Color.parseColor("#FFB74D"));
                dot.setVisibility(View.VISIBLE);
            } else {
                dot.setVisibility(View.INVISIBLE);
            }

            if (isSameDay(System.currentTimeMillis(), cellDate)) {
                tvDay.setTextColor(Color.parseColor("#4FC3F7"));
                tvDay.setTypeface(null, Typeface.BOLD);
            } else {
                tvDay.setTextColor(Color.WHITE);
                tvDay.setTypeface(null, Typeface.NORMAL);
            }

            if (isSameDay(selectedDate, cellDate)) {
                dayView.setBackgroundResource(R.drawable.option_button_bg);
            } else {
                dayView.setBackgroundColor(Color.TRANSPARENT);
            }

            dayView.setOnClickListener(v -> {
                selectedDate = (Calendar) cellDate.clone();
                refreshUI();
            });

            int row = (dayNum + firstDayOffset - 1) / 7 + 1;
            int col = (dayNum + firstDayOffset - 1) % 7;
            addToGrid(dayView, row, col);
        }
    }

    private void showTasksForDate(Calendar date) {
        List<FlashcardSet> dailyFlashcards = new ArrayList<>();
        List<Quiz> dailyQuizzes = new ArrayList<>();

        Calendar today = Calendar.getInstance();
        long endOfSelectedDay = getEndOfDay(date);

        // Filter Flashcards with SRS Overdue logic
        for (FlashcardSet set : allFlashcardSets) {
            if (set.getTimestamp() == null || set.getTimestamp().isEmpty()) continue;
            long ts = Long.parseLong(set.getTimestamp());
            if (isSameDay(date, today)) {
                if (ts <= endOfSelectedDay) dailyFlashcards.add(set);
            } else {
                if (isSameDay(ts, date)) dailyFlashcards.add(set);
            }
        }

        // Filter Quizzes with SRS Overdue logic
        for (Quiz quiz : allQuizzes) {
            long ts = quiz.getNextReview();
            if (ts <= 0) continue;
            if (isSameDay(date, today)) {
                if (ts <= endOfSelectedDay) dailyQuizzes.add(quiz);
            } else {
                if (isSameDay(ts, date)) dailyQuizzes.add(quiz);
            }
        }

        tvLabel.setText((dailyFlashcards.isEmpty() && dailyQuizzes.isEmpty()) ?
                "No tasks for this day" : "Tasks for " + date.get(Calendar.DAY_OF_MONTH));

        // Visibility and Adapters
        tvFlashHeader.setVisibility(dailyFlashcards.isEmpty() ? View.GONE : View.VISIBLE);
        rvFlashcards.setVisibility(dailyFlashcards.isEmpty() ? View.GONE : View.VISIBLE);
        if (!dailyFlashcards.isEmpty()) {
            rvFlashcards.setAdapter(new FlashcardAdapter(dailyFlashcards, set -> {
                Intent intent = new Intent(this, TakeCardActivity.class);
                intent.putExtra("set_cards", set);
                startActivity(intent);
            }, null));
        }

        tvQuizHeader.setVisibility(dailyQuizzes.isEmpty() ? View.GONE : View.VISIBLE);
        rvQuizzes.setVisibility(dailyQuizzes.isEmpty() ? View.GONE : View.VISIBLE);
        if (!dailyQuizzes.isEmpty()) {
            rvQuizzes.setAdapter(new QuizAdapter(this, dailyQuizzes, quiz -> {
                Intent intent = new Intent(this, TakeQuizActivity.class);
                intent.putExtra("QUIZ_OBJECT", quiz);
                startActivity(intent);
            }, null));
        }
    }

    private void addToGrid(View v, int row, int col) {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.rowSpec = GridLayout.spec(row);
        params.columnSpec = GridLayout.spec(col == -1 ? GridLayout.UNDEFINED : col, 1f);
        params.width = 0;
        v.setLayoutParams(params);
        calendarGrid.addView(v);
    }

    private boolean isSameDay(long timestamp, Calendar targetCal) {
        Calendar checkCal = Calendar.getInstance();
        checkCal.setTimeInMillis(timestamp);
        return checkCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                checkCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR);
    }

    private boolean isSameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    private long getStartOfDay(Calendar cal) {
        Calendar res = (Calendar) cal.clone();
        res.set(Calendar.HOUR_OF_DAY, 0);
        res.set(Calendar.MINUTE, 0);
        res.set(Calendar.SECOND, 0);
        res.set(Calendar.MILLISECOND, 0);
        return res.getTimeInMillis();
    }

    private long getEndOfDay(Calendar cal) {
        Calendar res = (Calendar) cal.clone();
        res.set(Calendar.HOUR_OF_DAY, 23);
        res.set(Calendar.MINUTE, 59);
        res.set(Calendar.SECOND, 59);
        res.set(Calendar.MILLISECOND, 999);
        return res.getTimeInMillis();
    }
}