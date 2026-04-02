package com.example.notewise;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignupActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private EditText etEmail, etPassword, etConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        etEmail = findViewById(R.id.editTextTextEmailAddress);
        etPassword = findViewById(R.id.editTextTextPassword);
        etConfirmPassword = findViewById(R.id.editTextTextPassword2);
        Button btnSignUp = findViewById(R.id.button2);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // --- FIX 1: Eye Icon Logic ---
        setupPasswordVisibilityToggle(etPassword);
        setupPasswordVisibilityToggle(etConfirmPassword);

        btnSignUp.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();
            String confirmPass = etConfirmPassword.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // --- FIX 2: Prevent Internal Error (6 Character Limit) ---
            if (pass.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pass.equals(confirmPass)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String userId = mAuth.getCurrentUser().getUid();
                            mDatabase.child("users").child(userId).child("email").setValue(email);
                            Toast.makeText(this, "Account Created!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                            finish();
                        } else {
                            // Detailed error message from Firebase
                            Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    // Helper method to handle the eye icon click
    @SuppressLint("ClickableViewAccessibility")
    private void setupPasswordVisibilityToggle(EditText editText) {
        editText.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (editText.getRight() - editText.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    int selection = editText.getSelectionEnd();
                    if (editText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                        editText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0); // Keep eye icon visible
                    } else {
                        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0);
                    }
                    editText.setSelection(selection);
                    return true;
                }
            }
            return false;
        });
    }
}