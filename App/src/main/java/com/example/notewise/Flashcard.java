package com.example.notewise;

import java.io.Serializable;

public class Flashcard implements Serializable {
    private String front, back;

    // Spaced Repetition Fields
    private int interval = 0;        // Days until next review
    private int repetitions = 0;     // Number of successful reviews
    private float easeFactor = 2.5f; // Multiplier for difficulty
    private long nextReviewDate;     // When it is due
    private int originalIndex;

    public Flashcard() {
        this.nextReviewDate = System.currentTimeMillis();
    }

    public Flashcard(String front, String back) {
        this.front = front;
        this.back = back;
        this.nextReviewDate = System.currentTimeMillis();
    }

    public int getOriginalIndex() { return originalIndex; }
    public void setOriginalIndex(int originalIndex) { this.originalIndex = originalIndex;
}

    // Getters and Setters for all fields...
    public int getInterval() { return interval; }
    public void setInterval(int interval) { this.interval = interval; }
    public int getRepetitions() { return repetitions; }
    public void setRepetitions(int repetitions) { this.repetitions = repetitions; }
    public float getEaseFactor() { return easeFactor; }
    public void setEaseFactor(float easeFactor) { this.easeFactor = easeFactor; }
    public long getNextReviewDate() { return nextReviewDate; }
    public void setNextReviewDate(long nextReviewDate) { this.nextReviewDate = nextReviewDate; }
    public String getFront() { return front; }
    public String getBack() { return back; }
}