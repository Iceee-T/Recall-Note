package com.example.notewise;

import java.io.Serializable;
import java.util.List;

public class FlashcardSet implements Serializable {
    private String id;
    private String title;
    private String type;
    private String timestamp;
    private List<Flashcard> cards;

    // Required for Firebase
    public FlashcardSet() {}

    public FlashcardSet(String title, String type, List<Flashcard> cards) {
        this.title = title;
        this.type = type;
        this.cards = cards;
        this.timestamp = String.valueOf(System.currentTimeMillis());
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public List<Flashcard> getCards() { return cards; }
    public void setCards(List<Flashcard> cards) { this.cards = cards; }
}