package com.example.notewise;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Collections; // Needed for shuffling
import java.util.List;

public class TakeCardActivity extends AppCompatActivity {

    private FlashcardSet currentSet;
    private List<Flashcard> cards;
    private int index = 0;
    private boolean isBackShowing = false;

    private TextView tvProgress, tvMainText, tvCardType;
    private CardView cardContainer;
    private ImageButton btnPrev, btnNext, btnShuffle; // Added btnShuffle
    private LinearLayout ratingLayout;
    private Button btnFlip, btnAgain, btnHard, btnGood;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_card);

        // 1. FIRST: Retrieve the data from the Intent
        currentSet = (FlashcardSet) getIntent().getSerializableExtra("set_cards");

        // 2. SECOND: Initialize the Database Reference (now that currentSet is not null)
        if (currentSet != null) {
            mDatabase = FirebaseDatabase.getInstance().getReference("flashcards")
                    .child(FirebaseAuth.getInstance().getUid())
                    .child(currentSet.getId()).child("cards");


            if (currentSet.getCards() != null) {
                cards = currentSet.getCards();
                // AUTO-SHUFFLE: Mix the cards immediately
                Collections.shuffle(cards);
            }
        } else {
            // Handle the case where the set didn't pass correctly
            Toast.makeText(this, "Error: Flashcard set not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvProgress = findViewById(R.id.tvProgress);
        tvMainText = findViewById(R.id.tvMainText);
        tvCardType = findViewById(R.id.tvCardType);
        cardContainer = findViewById(R.id.cardContainer);
        btnShuffle = findViewById(R.id.btnShuffle);
        ratingLayout = findViewById(R.id.ratingLayout);
        btnAgain = findViewById(R.id.btnAgain);
        btnHard = findViewById(R.id.btnHard);
        btnGood = findViewById(R.id.btnGood);


        // SRS Button Listeners
        btnAgain.setOnClickListener(v -> handleSrsClick(0)); // Forgot
        btnHard.setOnClickListener(v -> handleSrsClick(1));  // Remembered with struggle
        btnGood.setOnClickListener(v -> handleSrsClick(2));  // Easy/Good

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Manual Shuffle Button Click
        btnShuffle.setOnClickListener(v -> {
            if (cards != null && cards.size() > 1) {
                Collections.shuffle(cards);
                index = 0; // Reset to the first card of the new order
                isBackShowing = false;
                updateUI();
                Toast.makeText(this, "Cards Shuffled!", Toast.LENGTH_SHORT).show();
            }
        });

        cardContainer.setOnClickListener(v -> flipAnimation());
        updateUI();
    }

    private void handleSrsClick(int quality) {
        Flashcard card = cards.get(index);

        // 1. THE SRS LOGIC (SM-2 Simplified)
        int interval = card.getInterval();
        float ease = card.getEaseFactor();
        int reps = card.getRepetitions();

        if (quality >= 1) { // Hard (1) or Good (2)
            if (reps == 0) interval = 1;
            else if (reps == 1) interval = 4;
            else interval = Math.round(interval * ease);

            card.setRepetitions(reps + 1);

            // If Hard, decrease ease factor (min 1.3) so it appears more often in the future
            if (quality == 1) card.setEaseFactor(Math.max(1.3f, ease - 0.2f));

            card.setInterval(interval);
            long nextReview = System.currentTimeMillis() + (interval * 24L * 60L * 60L * 1000L);
            card.setNextReviewDate(nextReview);

            // Save progress to Firebase and move to the NEXT unique card
            saveCardProgress(card, true);

        } else { // Again (0) - The Professional "Re-queue" Logic
            card.setRepetitions(0);
            card.setInterval(0);
            card.setNextReviewDate(System.currentTimeMillis());

            // Save progress and move this card to the end of the current session
            requeueCard(card);
        }
    }

    private void requeueCard(Flashcard card) {
        // Save to Firebase first so data is persistent
        mDatabase.child(String.valueOf(index)).setValue(card).addOnSuccessListener(aVoid -> {
            // Move the card to the end of our local list
            cards.remove(index);
            cards.add(card);

            // We do NOT increment 'index' because the next card in line
            // automatically slides into the current 'index' position.
            isBackShowing = false;
            updateUI();

            Toast.makeText(this, "Will try again at the end!", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveCardProgress(Flashcard card, boolean moveForward) {
        // Find the original index of the card to ensure we update the correct one in Firebase
        // If your Flashcard model has a getId(), use that instead of String.valueOf(index)
        mDatabase.child(String.valueOf(card.getOriginalIndex())).setValue(card)
                .addOnSuccessListener(aVoid -> {
            if (moveForward) {
                if (index < cards.size() - 1) {
                    index++;
                    isBackShowing = false;
                    updateUI();
                } else {
                    showSessionComplete();
                }
            }
        });
    }

    private void showSessionComplete() {
        Toast.makeText(this, "Session Finished!", Toast.LENGTH_SHORT).show();
        // You could also add a dialog here to show summary stats
        finish();
    }

    private void updateUI() {
        if (cards == null || cards.isEmpty()) return;
        Flashcard current = cards.get(index);

        // Update Text
        tvProgress.setText((index + 1) + " / " + cards.size());
        tvMainText.setText(isBackShowing ? current.getBack() : current.getFront());
        tvCardType.setText(isBackShowing ? "ANSWER" : "QUESTION");

        // Toggle Rating Layout
        if (isBackShowing) {
            ratingLayout.setVisibility(View.VISIBLE);
            // Note: btnFlip should be hidden here if you still have it in XML
        } else {
            ratingLayout.setVisibility(View.GONE);
        }
    }

    private void flipAnimation() {
        ObjectAnimator oa1 = ObjectAnimator.ofFloat(cardContainer, "scaleX", 1f, 0f);
        oa1.setDuration(150);
        oa1.setInterpolator(new AccelerateDecelerateInterpolator());

        oa1.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isBackShowing = !isBackShowing;
                updateUI();
                ObjectAnimator oa2 = ObjectAnimator.ofFloat(cardContainer, "scaleX", 0f, 1f);
                oa2.setDuration(150);
                oa2.setInterpolator(new AccelerateDecelerateInterpolator());
                oa2.start();
            }
        });
        oa1.start();
    }

    private void finishDeck() {
        // If you want to force 100% mastery here, you'd track if they hit 'Again'
        // during the session. If they didn't, grant the pass:

        android.content.SharedPreferences prefs = getSharedPreferences("ActiveRecallPrefs", MODE_PRIVATE);
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        prefs.edit().putString("last_success_date", today).apply();

        Toast.makeText(this, "Deck Mastered! Social Media Unlocked.", Toast.LENGTH_SHORT).show();
        finish();
    }
}