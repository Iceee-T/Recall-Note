package com.example.notewise;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AchievementsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private DatabaseReference mDatabase;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements); //

        prefs = getSharedPreferences("AchievementPrefs", MODE_PRIVATE);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        userId = FirebaseAuth.getInstance().getUid();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish()); //

        if (userId != null) {
            startTracking();
        }
    }

    private void startTracking() {
        DatabaseReference userRef = mDatabase.child("users").child(userId);

        // 1. Notes Count
        userRef.child("notes").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                setupAch(findViewById(R.id.ach_1), "The Wordsmith", "Create 10 notes.", (int)s.getChildrenCount(), 10);
            }
            @Override public void onCancelled(DatabaseError e) {}
        });

        // 2. Bold/Style Stats
        userRef.child("stats").child("boldCount").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                int count = s.exists() ? s.getValue(Integer.class) : 0;
                setupAch(findViewById(R.id.ach_2), "Master Stylist", "Use Bold 20 times.", count, 20);
            }
            @Override public void onCancelled(DatabaseError e) {}
        });

        // 3. AI Summaries
        userRef.child("stats").child("aiCount").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                int count = s.exists() ? s.getValue(Integer.class) : 0;
                setupAch(findViewById(R.id.ach_3), "AI Visionary", "Summarize 5 notes.", count, 5);
            }
            @Override public void onCancelled(DatabaseError e) {}
        });

        // 4. Quizzes Count
        userRef.child("quizzes").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                setupAch(findViewById(R.id.ach_4), "Quick Learner", "Generate 5 quizzes.", (int)s.getChildrenCount(), 5);
            }
            @Override public void onCancelled(DatabaseError e) {}
        });

        // 5. Notebooks Count
        userRef.child("notebooks").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                setupAch(findViewById(R.id.ach_5), "Notebook Collector", "Create 3 notebooks.", (int)s.getChildrenCount(), 3);
            }
            @Override public void onCancelled(DatabaseError e) {}
        });

        // 6. Assigned Notes
        userRef.child("stats").child("assignedCount").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                int count = s.exists() ? s.getValue(Integer.class) : 0;
                setupAch(findViewById(R.id.ach_6), "Stay Organized", "Assign 10 notes.", count, 10);
            }
            @Override public void onCancelled(DatabaseError e) {}
        });

        // 7. Photos Count
        userRef.child("stats").child("photoCount").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                int count = s.exists() ? s.getValue(Integer.class) : 0;
                setupAch(findViewById(R.id.ach_7), "Flash Artist", "Add 5 photos.", count, 5);
            }
            @Override public void onCancelled(DatabaseError e) {}
        });

        // 8. Perfect Quiz Scores
        userRef.child("stats").child("perfectQuizzes").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                int count = s.exists() ? s.getValue(Integer.class) : 0;
                setupAch(findViewById(R.id.ach_8), "Recall Master", "3 perfect scores.", count, 3);
            }
            @Override public void onCancelled(DatabaseError e) {}
        });
    }

    private void setupAch(View v, String title, String desc, int cur, int total) {
        if (v == null) return; // Crash prevention

        TextView tvTitle = v.findViewById(R.id.tvTitle);
        TextView tvProg = v.findViewById(R.id.tvProgressText);
        ProgressBar pb = v.findViewById(R.id.pbAchievement);

        tvTitle.setText(title);
        ((TextView)v.findViewById(R.id.tvDesc)).setText(desc);
        tvProg.setText(cur + "/" + total);
        pb.setMax(total);
        pb.setProgress(cur);

        if (cur >= total && total > 0) {
            // Show the Featured Notification Card at the top
            View featureCard = findViewById(R.id.cardUnlocked);
            TextView featureTitle = findViewById(R.id.tvUnlockedTitle);

            if (featureCard != null && featureTitle != null) {
                featureCard.setVisibility(View.VISIBLE);
                featureTitle.setText(title);
            }

            tvProg.setTextColor(Color.parseColor("#FFD700")); // Gold Color

            // Show Toast Popup ONLY once per achievement
            boolean alreadyShown = prefs.getBoolean("shown_" + title, false);
            if (!alreadyShown) {
                Toast.makeText(this, "Achievement Unlocked: " + title + "! 🏆", Toast.LENGTH_LONG).show();
                prefs.edit().putBoolean("shown_" + title, true).apply();
            }
        }
    }
}