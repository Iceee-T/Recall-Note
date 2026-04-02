package com.example.notewise;

import java.io.Serializable;

public class Note implements Serializable {
    private String id;
    private String title;
    private String content;
    private String timestamp;

    // Required empty constructor for Firebase
    public Note() {}

    public Note(String title, String content, String timestamp) {
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}