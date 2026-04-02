package com.example.notewise;

import java.io.Serializable;

public class Notebook implements Serializable {
    private String id;
    private String name;
    private int noteCount;

    public Notebook() {} // Required for Firebase

    public Notebook(String name, int noteCount) {
        this.name = name;
        this.noteCount = noteCount;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getNoteCount() { return noteCount; }
    public void setNoteCount(int noteCount) { this.noteCount = noteCount; }
}