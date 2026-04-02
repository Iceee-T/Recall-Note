package com.example.notewise;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class QuestionModel implements Serializable {
    private String questionText = "";
    private List<String> choices = new ArrayList<>();
    private int correctOptionIndex = 0;
    private boolean isSelected = false;
    private String type = "Multiple Choice";

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }

    public QuestionModel() {}

    public String getQuestionText() { return questionText != null ? questionText : ""; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public List<String> getChoices() {
        if (choices == null) choices = new ArrayList<>();
        return choices;
    }
    public void setChoices(List<String> choices) { this.choices = choices; }

    public int getCorrectOptionIndex() { return correctOptionIndex; }
    public void setCorrectOptionIndex(int correctOptionIndex) { this.correctOptionIndex = correctOptionIndex; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}