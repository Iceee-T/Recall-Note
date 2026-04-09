package com.example.notewise;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        Button btnSignup = findViewById(R.id.btnSignup);

        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
        if (!android.provider.Settings.canDrawOverlays(this)) {
            android.content.Intent intent = new android.content.Intent(
                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:" + getPackageName())
            );
            startActivity(intent);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        handleBlockerRedirection();
    }

    private void handleBlockerRedirection() {
        // Only redirect if user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        DailyTaskManager.BlockerTask task = DailyTaskManager.getCurrentBlockerTask();
        if (task == null) return;

        // DO NOT call setDailyCompletion() here!
        // The user must complete the task first.

        if ("quiz".equals(task.type)) {
            fetchQuizAndLaunch(task.id);
        } else if ("flashcard".equals(task.type)) {
            fetchFlashcardAndLaunch(task.id);
        }
    }

    private void fetchQuizAndLaunch(String quizId) {
        String uid = FirebaseAuth.getInstance().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("quizzes")
                .child(uid).child(quizId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Quiz quiz = snapshot.getValue(Quiz.class);
                if (quiz != null) {
                    Intent intent = new Intent(MainActivity.this, TakeQuizActivity.class);
                    intent.putExtra("QUIZ_OBJECT", quiz);
                    startActivity(intent);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchFlashcardAndLaunch(String setId) {
        String uid = FirebaseAuth.getInstance().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("flashcards")
                .child(uid).child(setId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                FlashcardSet set = snapshot.getValue(FlashcardSet.class);
                if (set != null) {
                    Intent intent = new Intent(MainActivity.this, TakeCardActivity.class);
                    intent.putExtra("set_cards", set);
                    startActivity(intent);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

}
