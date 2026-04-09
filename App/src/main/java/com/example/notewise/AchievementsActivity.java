package com.example.notewise;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

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

        userRef.child("notes").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                setupAch(findViewById(R.id.ach_1), "The Wordsmith", "Create 10 notes.", (int)s.getChildrenCount(), 10);
            }
            @Override public void onCancelled(DatabaseError e) {}
        });

        userRef.child("stats").child("boldCount").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                int count = s.exists() ? s.getValue(Integer.class) : 0;
                setupAch(findViewById(R.id.ach_2), "Master Stylist", "Use Bold 20 times.", count, 20);
            }
            @Override public void onCancelled(DatabaseError e) {}
        });

        userRef.child("stats").child("aiCount").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                int count = s.exists() ? s.getValue(Integer.class) : 0;
                setupAch(findViewById(R.id.ach_3), "AI Visionary", "Summarize 5 notes.", count, 5);
            }
            @Override public void onCancelled(DatabaseError e) {}
        });

        userRef.child("quizzes").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                setupAch(findViewById(R.id.ach_4), "Quick Learner", "Generate 5 quizzes.", (int)s.getChildrenCount(), 5);
            }
            @Override public void onCancelled(DatabaseError e) {}
        });

        userRef.child("notebooks").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                setupAch(findViewById(R.id.ach_5), "Notebook Collector", "Create 3 notebooks.", (int)s.getChildrenCount(), 3);
            }
            @Override public void onCancelled(DatabaseError e) {}
        });

        userRef.child("stats").child("assignedCount").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                int count = s.exists() ? s.getValue(Integer.class) : 0;
                setupAch(findViewById(R.id.ach_6), "Stay Organized", "Assign 10 notes.", count, 10);
            }
            @Override public void onCancelled(DatabaseError e) {}
        });

        userRef.child("stats").child("photoCount").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                int count = s.exists() ? s.getValue(Integer.class) : 0;
                setupAch(findViewById(R.id.ach_7), "Flash Artist", "Add 5 photos.", count, 5);
            }
            @Override public void onCancelled(DatabaseError e) {}
        });

        userRef.child("stats").child("perfectQuizzes").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                int count = s.exists() ? s.getValue(Integer.class) : 0;
                setupAch(findViewById(R.id.ach_8), "Recall Master", "3 perfect scores.", count, 3);
            }
            @Override public void onCancelled(DatabaseError e) {}
        });

        userRef.child("notebooks").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                // Count how many notebooks exist under the user's 'notebooks' node
                int notebookCount = (int) s.getChildrenCount();

                // Update the UI using the setupAch helper method
                setupAch(findViewById(R.id.ach_notebook_master),
                        "Architect",
                        "Create 5 notebooks to organize your thoughts.",
                        notebookCount,
                        5);
            }

            @Override
            public void onCancelled(DatabaseError e) {
                // Handle database errors here
            }
        });
    }

    private void setupAch(View v, String title, String desc, int cur, int total) {
        if (v == null) return;

        TextView tvTitle = v.findViewById(R.id.tvTitle);
        TextView tvProg = v.findViewById(R.id.tvProgressText);
        ProgressBar pb = v.findViewById(R.id.pbAchievement);
        ImageView ivIcon = v.findViewById(R.id.ivIcon);

        tvTitle.setText(title);
        ((TextView)v.findViewById(R.id.tvDesc)).setText(desc);
        tvProg.setText(cur + "/" + total);
        pb.setMax(total);
        pb.setProgress(cur);

        if (cur >= total && total > 0) {
            int goldColor = Color.parseColor("#FFD700");
            tvProg.setTextColor(goldColor);
            ivIcon.setColorFilter(goldColor);
        } else {
            // Reset to default white if not achieved
            ivIcon.setColorFilter(Color.parseColor("#FFFFFF"));
            tvProg.setTextColor(Color.parseColor("#FFFFFF"));
        }
    }
}