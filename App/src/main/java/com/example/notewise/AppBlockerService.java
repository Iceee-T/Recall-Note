package com.example.notewise;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AppBlockerService extends AccessibilityService {

    // The hardcoded "Naughty List"
    private final Set<String> BLOCKED_APPS = new HashSet<>(Arrays.asList(
            "com.facebook.katana",      // Facebook
            "com.facebook.orca",        // Messenger
            "com.instagram.android",    // Instagram
            "com.ss.android.ugc.trill", // TikTok
            "com.twitter.android",      // X
            "com.instagram.barcelona",  // Threads
            "com.mobile.legends",       // Mobile Legends
            "com.garena.game.codm"      // Call Of Duty
    ));

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Check for both window state changes AND window content changes to be more aggressive
        if (event.getPackageName() == null) return;

        String packageName = event.getPackageName().toString();

        if (BLOCKED_APPS.contains(packageName)) {
            if (!isUnlockedToday()) {
                forceLaunchQuiz();
            }
        }
    }

    private boolean isUnlockedToday() {
        SharedPreferences prefs = getSharedPreferences("ActiveRecallPrefs", MODE_PRIVATE);

        // 1. Check Full Daily Pass
        String lastSuccess = prefs.getString("last_success_date", "");
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        if (lastSuccess.equals(today)) return true;

        // 2. Check Temporary Pass (The 10-minute reward)
        long tempUnlockTime = prefs.getLong("temp_unlock_time", 0);
        if (System.currentTimeMillis() < tempUnlockTime) {
            return true; // Let them use social media!
        }

        // Time is up, or they scored 0. Block them.
        return false;
    }

    private void forceLaunchQuiz() {
        // We don't need the "Home screen" force anymore.
        // If they are blocked, we just throw the quiz in their face.
        Intent intent = new Intent(this, TakeQuizActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    @Override public void onInterrupt() {}
}
