package com.example.notewise;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.PropertyName;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class GenerateQuiz extends AppCompatActivity {

    private Spinner spNotebook, spNote, spQCount, spDifficulty;
    private TextView etContent;
    private Button btnGenerate;
    private TextView tvCancel;

    private DatabaseReference mDatabase;
    private String currentUserId;
    private ValueEventListener notebookListener;

    private List<Notebook> allNotebooks = new ArrayList<>();
    private List<Note> filteredNotes = new ArrayList<>();

    private List<String> notebookNames = new ArrayList<>();
    private List<String> noteTitles = new ArrayList<>();
    private ArrayAdapter<String> notebookAdapter, noteAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_quiz);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // UI Binding
        spNotebook = findViewById(R.id.spinnerNotebook);
        spNote = findViewById(R.id.spinnerNote);
        spQCount = findViewById(R.id.spinnerQCount);
        spDifficulty = findViewById(R.id.spinnerDifficulty);
        etContent = findViewById(R.id.etContentPreview);
        btnGenerate = findViewById(R.id.btnGenerate);
        tvCancel = findViewById(R.id.tvCancel);

        setupStaticSpinners();
        setupAdapters();
        fetchNotebooks();

        tvCancel.setOnClickListener(v -> finish());

        btnGenerate.setOnClickListener(v -> {
            String content = etContent.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(this, "Please select a note with content", Toast.LENGTH_SHORT).show();
                return;
            }

            btnGenerate.setEnabled(false);
            btnGenerate.setText("Generating...");

            String countStr = spQCount.getSelectedItem().toString().split(" ")[0];
            String difficulty = spDifficulty.getSelectedItem().toString();

            GeminiQuizHelper.generateQuiz(content, countStr, difficulty, new GeminiQuizHelper.QuizCallback() {
                @Override
                // SUCCESS: Now uses the correct 'QuestionModel' type
                public void onSuccess(List<QuestionModel> questions) {
                    runOnUiThread(() -> {
                        btnGenerate.setEnabled(true);
                        btnGenerate.setText("Generate Quiz");

                        if (questions != null && !questions.isEmpty()) {
                            // Formatting name: "Notebook: Note"
                            String notebookName = spNotebook.getSelectedItem().toString();
                            String noteTitle = spNote.getSelectedItem().toString();
                            String customTitle = notebookName + ": " + noteTitle;

                            // Call save method
                            saveQuizToFirebase(customTitle, questions);
                        } else {
                            Toast.makeText(GenerateQuiz.this, "AI returned no questions", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        btnGenerate.setEnabled(true);
                        btnGenerate.setText("Generate Quiz");
                        Toast.makeText(GenerateQuiz.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }

    private void setupStaticSpinners() {
        String[] qCounts = {"5 Questions", "10 Questions", "15 Questions", "20 Questions"};
        // Use the custom layout here to prevent cutting off text
        ArrayAdapter<String> qAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_custom, qCounts);
        // Use standard dropdown for the actual list
        qAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spQCount.setAdapter(qAdapter);

        String[] levels = {"Easy", "Medium", "Hard"};
        ArrayAdapter<String> dAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_custom, levels);
        dAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDifficulty.setAdapter(dAdapter);
    }

    private void setupAdapters() {
        // Apply custom layout to Notebook and Note spinners
        notebookAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_custom, notebookNames);
        notebookAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spNotebook.setAdapter(notebookAdapter);

        noteAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_custom, noteTitles);
        noteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spNote.setAdapter(noteAdapter);

        spNotebook.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    clearNotes();
                } else if (position == 1) {
                    fetchNotesFromPath("notes/general_notes/" + currentUserId);
                } else {
                    int index = position - 2;
                    if (index >= 0 && index < allNotebooks.size()) {
                        fetchNotesFromPath("notes/" + allNotebooks.get(index).getId());
                    }
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spNote.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && !filteredNotes.isEmpty() && (position - 1) < filteredNotes.size()) {
                    Note selectedNote = filteredNotes.get(position - 1);
                    if (selectedNote != null && selectedNote.getContent() != null) {
                        etContent.setText(Html.fromHtml(selectedNote.getContent(), Html.FROM_HTML_MODE_LEGACY));
                    }
                } else {
                    etContent.setText("");
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void fetchNotebooks() {
        DatabaseReference nbRef = mDatabase.child("users").child(currentUserId).child("notebooks");
        notebookListener = nbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allNotebooks.clear();
                notebookNames.clear();
                notebookNames.add("Select Notebook");
                notebookNames.add("General Notes");

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Notebook nb = ds.getValue(Notebook.class);
                    if (nb != null) {
                        nb.setId(ds.getKey());
                        allNotebooks.add(nb);
                        // Add the title to names list (this uses getTitle which is mapped to 'name')
                        notebookNames.add(nb.getTitle());
                    }
                }
                notebookAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchNotesFromPath(String path) {
        mDatabase.child(path).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                filteredNotes.clear();
                noteTitles.clear();
                noteTitles.add("Select Note");
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Note note = ds.getValue(Note.class);
                    if (note != null) {
                        note.setId(ds.getKey());
                        filteredNotes.add(note);
                        noteTitles.add(note.getTitle());
                    }
                }
                spNote.setSelection(0);
                etContent.setText("");
                noteAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GenerateQuiz.this, "Denied: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearNotes() {
        filteredNotes.clear();
        noteTitles.clear();
        noteTitles.add("Select Note");
        noteAdapter.notifyDataSetChanged();
        etContent.setText("");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (notebookListener != null) {
            mDatabase.child("users").child(currentUserId).child("notebooks").removeEventListener(notebookListener);
        }
    }

    public static class Notebook {
        private String id, title;
        public Notebook() {}
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        // FIX: Mapped to 'name' based on your Firebase screenshot
        @PropertyName("name")
        public String getTitle() { return title; }

        @PropertyName("name")
        public void setTitle(String title) { this.title = title; }

        @NonNull
        @Override
        public String toString() {
            return title != null ? title : "";
        }
    }

    public static class Note {
        private String id, title, content;
        public Note() {}
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        // Mapped to 'title' as per standard logic
        @PropertyName("title")
        public String getTitle() { return title; }

        @PropertyName("title")
        public void setTitle(String title) { this.title = title; }

        @PropertyName("content")
        public String getContent() { return content; }

        @PropertyName("content")
        public void setContent(String content) { this.content = content; }
    }

    // Update the parameter type to List<GeminiQuizHelper.QuestionModel>
    private void saveQuizToFirebase(String title, List<QuestionModel> questions) {
        DatabaseReference quizRef = FirebaseDatabase.getInstance().getReference("quizzes").child(currentUserId);
        String quizId = quizRef.push().getKey();

        if (quizId != null) {
            Quiz newQuiz = new Quiz();
            newQuiz.setId(quizId);
            newQuiz.setTitle(title);
            newQuiz.setQuestions(questions);
            newQuiz.setQuestionCount(questions.size());
            newQuiz.setTimestamp(String.valueOf(System.currentTimeMillis()));
            newQuiz.setDescription("AI Generated from " + spNote.getSelectedItem().toString());

            quizRef.child(quizId).setValue(newQuiz).addOnSuccessListener(aVoid -> {
                Toast.makeText(GenerateQuiz.this, "Quiz Generated!", Toast.LENGTH_SHORT).show();

                // CHANGE: Go directly to Preview instead of the List
                Intent intent = new Intent(GenerateQuiz.this, QuizPreviewActivity.class);

                // Pass the object we just created so the preview can display it immediately
                intent.putExtra("SELECTED_QUIZ", newQuiz);

                startActivity(intent);

                // Finish this activity so the user doesn't "go back" into the generator
                finish();
            }).addOnFailureListener(e -> {
                Toast.makeText(GenerateQuiz.this, "Save Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
        DailyTaskManager.updateDailyTask();

    }
}