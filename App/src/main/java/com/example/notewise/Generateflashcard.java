package com.example.notewise;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
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
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Generateflashcard extends AppCompatActivity {
    private Spinner spNotebook, spNote, spType, spDifficulty, spCount;
    private TextView etContent;
    private Button btnGenerate;
    private DatabaseReference mDatabase;
    private String currentUserId;

    // FIXED: Using local lists to avoid "Cannot Resolve" or Type errors
    private List<NotebookModel> allNotebooks = new ArrayList<>();
    private List<NoteModel> filteredNotes = new ArrayList<>();
    private List<String> notebookNames = new ArrayList<>();
    private List<String> noteTitles = new ArrayList<>();

    private ArrayAdapter<String> notebookAdapter, noteAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generateflashcard);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        spNotebook = findViewById(R.id.spinnerNotebook);
        spNote = findViewById(R.id.spinnerNote);
        spType = findViewById(R.id.spinnerFlashcardType);
        spDifficulty = findViewById(R.id.spinnerDifficulty);
        spCount = findViewById(R.id.spinnerQCount);
        etContent = findViewById(R.id.etContentPreview);
        btnGenerate = findViewById(R.id.btnGenerate);

        // APPLY WHITE TEXT STYLE
        setupStaticSpinners();
        setupDynamicAdapters();

        loadNotebooks();

        btnGenerate.setOnClickListener(v -> generateFlashcards());
        findViewById(R.id.tvCancel).setOnClickListener(v -> finish());
    }

    private void setupStaticSpinners() {
        // Uses the same custom white-text layout as GenerateQuiz
        applyCustomLayout(spType, new String[]{"Terminology", "Identification"});
        applyCustomLayout(spDifficulty, new String[]{"Easy", "Medium", "Hard"});
        applyCustomLayout(spCount, new String[]{"5", "10", "15"});
    }

    private void applyCustomLayout(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item_custom, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void setupDynamicAdapters() {
        notebookAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_custom, notebookNames);
        notebookAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spNotebook.setAdapter(notebookAdapter);

        noteAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_custom, noteTitles);
        noteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spNote.setAdapter(noteAdapter);
    }

    private void loadNotebooks() {
        mDatabase.child("users").child(currentUserId).child("notebooks")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        allNotebooks.clear();
                        notebookNames.clear();
                        notebookNames.add("Select Notebook");
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            NotebookModel nb = ds.getValue(NotebookModel.class);
                            if (nb != null) {
                                nb.id = ds.getKey();
                                allNotebooks.add(nb);
                                notebookNames.add(nb.name != null ? nb.name : "Unnamed");
                            }
                        }
                        notebookAdapter.notifyDataSetChanged();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        spNotebook.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    filterNotes(allNotebooks.get(position - 1).id);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void filterNotes(String notebookId) {
        mDatabase.child("notes").child(notebookId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                filteredNotes.clear();
                noteTitles.clear();
                noteTitles.add("Select Note");
                for (DataSnapshot ds : snapshot.getChildren()) {
                    NoteModel note = ds.getValue(NoteModel.class);
                    if (note != null) {
                        note.id = ds.getKey();
                        filteredNotes.add(note);
                        noteTitles.add(note.title);
                    }
                }
                noteAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        spNote.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    String content = filteredNotes.get(position - 1).content;
                    etContent.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY).toString());
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void generateFlashcards() {
        String content = etContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Select a note first", Toast.LENGTH_SHORT).show();
            return;
        }

        btnGenerate.setEnabled(false);
        btnGenerate.setText("Generating...");

        GeminiQuizHelper.generateFlashcards(content,
                spType.getSelectedItem().toString(),
                spDifficulty.getSelectedItem().toString(),
                spCount.getSelectedItem().toString(),
                new GeminiQuizHelper.FlashcardCallback() {
                    @Override
                    public void onSuccess(List<Flashcard> flashcards) {
                        Collections.shuffle(flashcards); // SHUFFLE ADDED
                        saveToFirebase(flashcards);
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            btnGenerate.setEnabled(true);
                            btnGenerate.setText("Generate");
                            Toast.makeText(Generateflashcard.this, error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void saveToFirebase(List<Flashcard> cards) {
        DatabaseReference ref = mDatabase.child("flashcards").child(currentUserId);
        String id = ref.push().getKey();
        String flashcardType = spType.getSelectedItem().toString();

        // Pass the Type here
        FlashcardSet set = new FlashcardSet(spNote.getSelectedItem().toString(), flashcardType, cards);
        set.setId(id);

        ref.child(id).setValue(set).addOnSuccessListener(aVoid -> {
            Intent intent = new Intent(Generateflashcard.this, TakeCardActivity.class);
            intent.putExtra("set", set);
            startActivity(intent);
            finish();
        });
    }

    // INTERNAL MODELS TO PREVENT "CANNOT RESOLVE" ERRORS
    public static class NotebookModel {
        public String id, name;
        public NotebookModel() {}
    }

    public static class NoteModel {
        public String id, title, content;
        public NoteModel() {}
    }
}