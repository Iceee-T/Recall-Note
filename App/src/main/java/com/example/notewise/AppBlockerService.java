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
            "com.facebook.katana",
            "com.facebook.orca",
            "com.instagram.android",
            "com.ss.android.ugc.trill",
            "com.twitter.android",
            "com.instagram.barcelona",
            "com.mobile.legends",
            "com.garena.game.codm"
    ));

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }

        if (event.getPackageName() == null) return;
        String packageName = event.getPackageName().toString();

        if (BLOCKED_APPS.contains(packageName)) {
            // NEW LOGIC: Only block if the active recall task is active and NOT unlocked
            if (shouldBlockApps()) {
                forceLaunchQuiz();
            }
        }
    }

    private boolean shouldBlockApps() {
        SharedPreferences prefs = getSharedPreferences("ActiveRecallPrefs", MODE_PRIVATE);

        // 1. Did the user enable the Active Recall switch?
        boolean optInEnabled = prefs.getBoolean("opt_in_enabled", false);
        long expiration = prefs.getLong("opt_in_expiration", 0);

        // If they didn't opt in, or the expiration time has passed, DO NOT BLOCK!
        if (!optInEnabled || System.currentTimeMillis() > expiration) {
            return false;
        }

        // 2. If it IS active, have they unlocked it today?
        String lastSuccess = prefs.getString("last_success_date", "");
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        long tempUnlockTime = prefs.getLong("temp_unlock_time", 0);

        boolean isUnlocked = lastSuccess.equals(today) || System.currentTimeMillis() < tempUnlockTime;

        // Return true (block the app) if it is NOT unlocked
        return !isUnlocked;
    }

    private void forceLaunchQuiz() {
        Intent intent = new Intent(this, BlockerPromptActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
    @Override public void onInterrupt() {}
}