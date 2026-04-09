// DailyTaskManager.java
package com.example.notewise;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.*;

public class DailyTaskManager {

    public interface TaskCallback {
        void onTaskReady(BlockerTask task);
    }

    private static boolean isRedirecting = false;

    public static void startRedirect() {
        isRedirecting = true;
    }

    public static void endRedirect() {
        isRedirecting = false;
    }

    public static boolean isRedirecting() {
        return isRedirecting;
    }

    public static class BlockerTask {
        public String type; // "quiz" or "flashcard"
        public String id;
        public String title;
        public Object taskObject; // Quiz or FlashcardSet instance
    }

    public static void updateDailyTask() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference quizRef = FirebaseDatabase.getInstance().getReference("quizzes").child(uid);
        DatabaseReference flashRef = FirebaseDatabase.getInstance().getReference("flashcards").child(uid);

        // Fetch both and pick the most urgent due task
        quizRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot quizSnap) {
                flashRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot flashSnap) {
                        BlockerTask task = findMostUrgentTask(quizSnap, flashSnap);
                        storeTask(task);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private static BlockerTask findMostUrgentTask(DataSnapshot quizSnap, DataSnapshot flashSnap) {
        long now = System.currentTimeMillis();
        BlockerTask best = null;
        long bestDue = Long.MAX_VALUE;

        // Check quizzes
        for (DataSnapshot qs : quizSnap.getChildren()) {
            Quiz quiz = qs.getValue(Quiz.class);
            if (quiz != null && quiz.getNextReview() > 0 && quiz.getNextReview() <= now) {
                if (quiz.getNextReview() < bestDue) {
                    bestDue = quiz.getNextReview();
                    best = new BlockerTask();
                    best.type = "quiz";
                    best.id = qs.getKey();
                    best.title = quiz.getTitle();
                    best.taskObject = quiz;
                }
            }
        }

        // Check flashcards
        for (DataSnapshot fs : flashSnap.getChildren()) {
            FlashcardSet set = fs.getValue(FlashcardSet.class);
            if (set != null && set.getTimestamp() != null && !set.getTimestamp().isEmpty()) {
                long ts = Long.parseLong(set.getTimestamp());
                if (ts <= now && ts < bestDue) {
                    bestDue = ts;
                    best = new BlockerTask();
                    best.type = "flashcard";
                    best.id = fs.getKey();
                    best.title = set.getTitle();
                    best.taskObject = set;
                }
            }
        }
        return best;
    }

    private static void storeTask(BlockerTask task) {
        SharedPreferences prefs = RecallNoteApp.getAppContext().getSharedPreferences("ActiveRecallPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (task == null) {
            editor.remove("blocker_task_type");
            editor.remove("blocker_task_id");
            editor.remove("blocker_task_title");
        } else {
            editor.putString("blocker_task_type", task.type);
            editor.putString("blocker_task_id", task.id);
            editor.putString("blocker_task_title", task.title);
        }
        editor.apply();
    }

    public static BlockerTask getCurrentBlockerTask() {
        SharedPreferences prefs = RecallNoteApp.getAppContext().getSharedPreferences("ActiveRecallPrefs", Context.MODE_PRIVATE);
        String type = prefs.getString("blocker_task_type", null);
        if (type == null) return null;
        BlockerTask task = new BlockerTask();
        task.type = type;
        task.id = prefs.getString("blocker_task_id", "");
        task.title = prefs.getString("blocker_task_title", "");
        return task;
    }

    public static void clearBlockerTask() {
        SharedPreferences prefs = RecallNoteApp.getAppContext().getSharedPreferences("ActiveRecallPrefs", Context.MODE_PRIVATE);
        prefs.edit().remove("blocker_task_type").remove("blocker_task_id").remove("blocker_task_title").apply();
    }

    public static void resetAllSchedules(Context context) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        // 1. Reset Quizzes in Firebase
        DatabaseReference quizRef = FirebaseDatabase.getInstance().getReference("quizzes").child(uid);
        quizRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    // Set nextReview to 0 so they aren't "pending" anymore
                    ds.getRef().child("nextReview").setValue(0);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 2. Reset Flashcards in Firebase
        DatabaseReference flashRef = FirebaseDatabase.getInstance().getReference("flashcards").child(uid);
        flashRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {

                    // THE FIX: Set this to an empty string instead of "0"
                    ds.getRef().child("timestamp").setValue("");

                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 3. Clear all Local Preferences (Blocking, Success Dates, and Opt-ins)
        SharedPreferences prefs = context.getSharedPreferences("ActiveRecallPrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        // 4. Stop the current redirect logic
        endRedirect();
    }
}