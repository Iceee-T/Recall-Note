package com.example.notewise;

import java.io.Serializable;
import java.util.List;

/**
 * Optimized Quiz model to support Firebase, manual creation,
 * and AI-generated content.
 */
public class Quiz implements Serializable {
    private String id;
    private String title;
    private String description;
    private int questionCount;
    private String timestamp;
    private List<QuestionModel> questions;
    private long nextReview;

    // 1. Required empty constructor for Firebase
    public Quiz() {}

    // 2. Original constructor for manual description
    public Quiz(String title, String description, int questionCount) {
        this.title = title;
        this.description = description;
        this.questionCount = questionCount;
    }

    // 3. NEW: AI-Optimization Constructor
    // This resolves the "Cannot resolve constructor" error in GenerateQuiz.java
    public Quiz(String title, List<QuestionModel> questions) {
        this.title = title;
        this.questions = questions;
        // Automatically sync the count with the list size
        if (questions != null) {
            this.questionCount = questions.size();
        }
    }

    public long getNextReview() { return nextReview; }
    public void setNextReview(long nextReview) { this.nextReview = nextReview; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    // --- Getters and Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getQuestionCount() {
        // If we have a list, return its size, otherwise return the stored count
        if (questions != null && !questions.isEmpty()) {
            return questions.size();
        }
        return questionCount;
    }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }

    public List<QuestionModel> getQuestions() { return questions; }

    public void setQuestions(List<QuestionModel> questions) {
        this.questions = questions;
        if (questions != null) {
            this.questionCount = questions.size();
        }
    }
}