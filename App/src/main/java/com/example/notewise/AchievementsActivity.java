package com.example.notewise;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
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
        setContentView(R.layout.activity_achievements);

        // "AchievementPrefs" stores which achievements have already popped up
        prefs = getSharedPreferences("AchievementPrefs", MODE_PRIVATE);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        userId = FirebaseAuth.getInstance().getUid();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        if (userId != null) {
            startTracking();
        }
    }

    private void startTracking() {
        DatabaseReference userRef = mDatabase.child("users").child(userId);

        // 1. The Wordsmith (10 Notes)
        userRef.child("notes").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                setupAch(findViewById(R.id.ach_1), "The Wordsmith", "Create 10 notes.", (int)s.getChildrenCount(), 10);
            }
            @Override public void onCancelled(DatabaseError e) {}
        });

        // 2. Master Stylist (20 Bold)
        userRef.child("stats").child("boldCount").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                int count = s.exists() ? s.getValue(Integer.class) : 0;
                setupAch(findViewById(R.id.ach_2), "Master Stylist", "Use Bold 20 times.", count, 20);
            }
            @Override public void onCancelled(DatabaseError e) {}
        });

        // 3. AI Visionary (5 Summaries)
        userRef.child("stats").child("aiCount").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                int count = s.exists() ? s.getValue(Integer.class) : 0;
                setupAch(findViewById(R.id.ach_3), "AI Visionary", "Summarize 5 notes.", count, 5);
            }
            @Override public void onCancelled(DatabaseError e) {}
        });

        // 4. Quick Learner (5 Quizzes)
        userRef.child("quizzes").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                setupAch(findViewById(R.id.ach_4), "Quick Learner", "Generate 5 quizzes.", (int)s.getChildrenCount(), 5);
            }
            @Override public void onCancelled(DatabaseError e) {}
        });

        // 5. Notebook Collector (3 Notebooks)
        userRef.child("notebooks").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                setupAch(findViewById(R.id.ach_5), "Notebook Collector", "Create 3 notebooks.", (int)s.getChildrenCount(), 3);
            }
            @Override public void onCancelled(DatabaseError e) {}
        });

        // 7. Flash Artist (5 Photos)
        userRef.child("stats").child("photoCount").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                int count = s.exists() ? s.getValue(Integer.class) : 0;
                setupAch(findViewById(R.id.ach_7), "Flash Artist", "Add 5 photos.", count, 5);
            }
            @Override public void onCancelled(DatabaseError e) {}
        });

        // 9. Architect (Subsequent achievement: 5 Notebooks)
        userRef.child("notebooks").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                setupAch(findViewById(R.id.ach_notebook_master), "Architect", "Create 5 notebooks.", (int)s.getChildrenCount(), 5);
            }
            @Override public void onCancelled(DatabaseError e) {}
        });
    }

    private void setupAch(View v, String title, String desc, int cur, int total) {
        if (v == null) return;

        // RULE: Clamp progress so it doesn't show 4/3
        int displayProgress = Math.min(cur, total);

        TextView tvTitle = v.findViewById(R.id.tvTitle);
        TextView tvProg = v.findViewById(R.id.tvProgressText);
        ProgressBar pb = v.findViewById(R.id.pbAchievement);
        ImageView ivIcon = v.findViewById(R.id.ivIcon);
        View cardUnlocked = v.findViewById(R.id.cardUnlocked);

        tvTitle.setText(title);
        ((TextView)v.findViewById(R.id.tvDesc)).setText(desc);
        tvProg.setText(displayProgress + "/" + total);
        pb.setMax(total);
        pb.setProgress(displayProgress);

        if (cur >= total && total > 0) {
            int goldColor = Color.parseColor("#FFD700");
            tvProg.setTextColor(goldColor);
            ivIcon.setColorFilter(goldColor);

            // Check if already notified
            if (!prefs.getBoolean("notified_" + title, false)) {
                Toast.makeText(this, "Achievement Unlocked: " + title + "! 🏆", Toast.LENGTH_LONG).show();
                prefs.edit().putBoolean("notified_" + title, true).apply();
            }
        } else {
            ivIcon.setColorFilter(Color.parseColor("#FFFFFF"));
            tvProg.setTextColor(Color.parseColor("#FFFFFF"));
        }

        // ALWAYS HIDE the green "Achievement Unlocked" card
        if (cardUnlocked != null) {
            cardUnlocked.setVisibility(View.GONE);
        }
    }
}