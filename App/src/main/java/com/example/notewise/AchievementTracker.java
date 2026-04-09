package com.example.notewise;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AchievementTracker {
    private Context context;
    private SharedPreferences prefs;
    private DatabaseReference mDatabase;
    private String userId;

    public AchievementTracker(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences("AchievementPrefs", Context.MODE_PRIVATE);
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
        this.userId = FirebaseAuth.getInstance().getUid();

        prefs.edit().clear().apply();

        this.mDatabase = FirebaseDatabase.getInstance().getReference();
        this.userId = FirebaseAuth.getInstance().getUid();
    }

    public void startGlobalTracking() {
        if (userId == null) return;
        DatabaseReference userRef = mDatabase.child("users").child(userId);

        // 1. Track Notebooks (Architect)
        userRef.child("notebooks").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                int count = (int) s.getChildrenCount();
                checkAndNotify("Architect", "Create 5 notebooks to organize your thoughts.", count, 5);
            }
            @Override public void onCancelled(DatabaseError e) {}
        });

        // 2. Track Perfect Quiz Scores (Recall Master)
        // Adjust the path "quiz_scores" to match your actual Firebase structure
        userRef.child("quiz_scores").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                int perfectScores = 0;
                for (DataSnapshot postSnapshot : s.getChildren()) {
                    // Assuming you store score and total in each quiz entry
                    Integer score = postSnapshot.child("score").getValue(Integer.class);
                    Integer total = postSnapshot.child("total").getValue(Integer.class);
                    if (score != null && total != null && score.equals(total)) {
                        perfectScores++;
                    }
                }
                checkAndNotify("Recall Master", "Achieve 3 perfect scores in quizzes.", perfectScores, 3);
            }
            @Override public void onCancelled(DatabaseError e) {}
        });
    }

    private void checkAndNotify(String title, String desc, int cur, int total) {
        // Logic: Only notify if the goal is met and we haven't shown it before
        if (cur >= total && total > 0) {
            boolean alreadyShown = prefs.getBoolean("shown_" + title, false);
            if (!alreadyShown) {
                showSystemNotification(title, desc);
                prefs.edit().putBoolean("shown_" + title, true).apply();
            }
        }
    }

    private void showSystemNotification(String title, String desc) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "achievement_channel_v10";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Achievements", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.star_on)
                .setContentTitle("Achievement Unlocked: " + title)
                .setContentText(desc)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}