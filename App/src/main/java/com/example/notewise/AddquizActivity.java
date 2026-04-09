package com.example.notewise;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AddquizActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private LinearLayout btnGenerateAI, btnManual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_addquiz);

        // Apply Window Insets for immersive dark mode
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Initialize Views
        btnBack = findViewById(R.id.btnBack);
        btnGenerateAI = findViewById(R.id.btnGenerateAI);
        btnManual = findViewById(R.id.btnManual);


        // 2. Back Button Logic
        btnBack.setOnClickListener(v -> finish());

        // 3. Navigation: AI Generation
        btnGenerateAI.setOnClickListener(v -> {
            Intent intent = new Intent(AddquizActivity.this, GenerateQuiz.class);
            startActivity(intent);
        });

        // 4. Navigation: Manual Creation
        btnManual.setOnClickListener(v -> {
            Intent intent = new Intent(AddquizActivity.this, ManualquizActivity.class);
            startActivity(intent);
        });
        DailyTaskManager.updateDailyTask();
    }
}