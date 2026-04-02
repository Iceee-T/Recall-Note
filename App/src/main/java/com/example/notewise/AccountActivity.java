package com.example.notewise;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AccountActivity extends AppCompatActivity {

    private TextView tvDisplayName, tvDisplayEmail;
    private View btnSignOut;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private ValueEventListener userProfileListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        // Point to the 'users' node
        mDatabase = FirebaseDatabase.getInstance().getReference("users");

        // UI Bindings
        tvDisplayName = findViewById(R.id.tvDisplayName);
        tvDisplayEmail = findViewById(R.id.tvDisplayEmail);
        btnSignOut = findViewById(R.id.btnSignOut);
        ImageButton btnBack = findViewById(R.id.btnBack);

        // Responsive System Bars Handling
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Fetch User Data if Logged In
        if (currentUser != null) {
            loadUserProfile(currentUser.getUid());
        } else {
            redirectToLogin();
        }

        // Back Navigation
        btnBack.setOnClickListener(v -> finish());

        // Sign Out Logic
        btnSignOut.setOnClickListener(v -> {
            // 1. Get the current User ID to know which node to detach from
            FirebaseUser user = mAuth.getCurrentUser();

            // 2. IMPORTANT: Remove the listener BEFORE signing out
            if (user != null && userProfileListener != null) {
                mDatabase.child(user.getUid()).removeEventListener(userProfileListener);
            }

            // 3. Now sign out safely
            mAuth.signOut();
            Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show();
            redirectToLogin();
        });
    }

    private void loadUserProfile(String uid) {
        // Store the listener in our variable
        userProfileListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String email = snapshot.child("email").getValue(String.class);
                    tvDisplayEmail.setText(email != null ? email : "Not set");

                    if (snapshot.hasChild("name")) {
                        tvDisplayName.setText(snapshot.child("name").getValue(String.class));
                    } else if (email != null) {
                        tvDisplayName.setText(email.split("@")[0]);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // This is what triggers the "Sync failed" Toast.
                // We check if we are still logged in before showing it.
                if (mAuth.getCurrentUser() != null) {
                    Toast.makeText(AccountActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };

        // Attach the listener
        mDatabase.child(uid).addValueEventListener(userProfileListener);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(AccountActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // This ensures no memory leaks or permission errors if the activity closes
        if (mAuth.getCurrentUser() != null && userProfileListener != null) {
            mDatabase.child(mAuth.getCurrentUser().getUid()).removeEventListener(userProfileListener);
        }
    }
}