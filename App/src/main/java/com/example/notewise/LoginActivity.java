package com.example.notewise;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword;
    private ImageButton btnBack;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 2. Initialize UI Elements (Matching your XML IDs)
        etEmail = findViewById(R.id.editTextTextEmailAddress);
        etPassword = findViewById(R.id.editTextTextPassword);
        btnLogin = findViewById(R.id.btnLogin2);
        tvForgotPassword = findViewById(R.id.forgotPassword);
        btnBack = findViewById(R.id.btnBack);

        // 3. Login Logic
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please enter login details", Toast.LENGTH_SHORT).show();
                return;
            }

            // Firebase Sign In
            mAuth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, HomepageActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, "Login Failed: Check credentials", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // 4. Forgot Password Logic (Secure Reset)
        tvForgotPassword.setOnClickListener(v -> {
            EditText emailInput = new EditText(this);
            emailInput.setHint("Registered Email Address");
            emailInput.setPadding(50, 40, 50, 40);

            new AlertDialog.Builder(this)
                    .setTitle("Reset Password")
                    .setMessage("Enter your email to receive a secure reset link.")
                    .setView(emailInput)
                    .setPositiveButton("Send Link", (dialog, which) -> {
                        String email = emailInput.getText().toString().trim();
                        if (!email.isEmpty()) {
                            mAuth.sendPasswordResetEmail(email)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(LoginActivity.this, "Check your email inbox!", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // 5. Password Visibility Toggle (Eye Icon Logic)
        etPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (etPassword.getRight() - etPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    int selection = etPassword.getSelectionEnd();
                    if (etPassword.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                        etPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    } else {
                        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    }
                    etPassword.setSelection(selection);
                    return true;
                }
            }
            return false;
        });

        // 6. Navigation Logic
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        });
    }
}