package com.example.notewise;

import com.google.ai.client.generativeai.type.GenerationConfig;
import android.util.Log;
import com.example.notewise.BuildConfig;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.BlockThreshold;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.SafetySetting;
import com.google.ai.client.generativeai.type.HarmCategory;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiQuizHelper {

    private GenerativeModelFutures model;
    private static final String TAG = "GEMINI_DEBUG";

    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;



    // Original Quiz Callback
    public interface QuizCallback {
        void onSuccess(List<QuestionModel> questions);
        void onError(String error);
    }

    public interface FlashcardCallback {
        void onSuccess(List<Flashcard> flashcards);
        void onError(String error);
    }

    // New Summary Callback for the added feature
    public interface SummaryCallback {
        void onSuccess(String summary);
        void onError(String error);
    }

    /**
     * Original logic for generating a quiz from note content
     */
    public static void generateQuiz(String noteContent, String difficulty, String numQuestions, QuizCallback callback) {

        // 1. Configure Safety Settings (Original Logic)
        List<SafetySetting> safetySettings = new ArrayList<>();
        safetySettings.add(new SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH));
        safetySettings.add(new SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH));

        // 2. Initialize Model (Using 1.5-flash for speed)
        GenerativeModel gm = new GenerativeModel(
                "gemini-2.5-flash",
                API_KEY,
                null,
                safetySettings
        );
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        // 3. Create the specialized Prompt (Original Logic)
        String prompt = "You are an educational assistant. Based on the notes provided below, generate a quiz.\n\n" +
                "NOTES:\n" + noteContent + "\n\n" +
                "REQUIREMENTS:\n" +
                "- Difficulty: " + difficulty + "\n" +
                "- Number of Questions: " + numQuestions + "\n" +
                "- Format: Return ONLY a raw JSON array. No markdown code blocks, no preamble.\n" +
                "- Question Structure: Each object must contain 'questionText' (String), 'choices' (Array of 4 Strings), " +
                "'correctOptionIndex' (Integer 0-3), and 'type' (String 'Multiple Choice').";

        Content content = new Content.Builder().addText(prompt).build();

        // 4. Execute Async Request
        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String rawResponse = result.getText();
                Log.d(TAG, "Raw Response: " + rawResponse);

                if (rawResponse == null || rawResponse.isEmpty()) {
                    callback.onError("AI returned an empty response.");
                    return;
                }

                try {
                    // Clean response (Original JSON cleaning logic)
                    String cleanedJson = rawResponse.trim();
                    if (cleanedJson.startsWith("```")) {
                        cleanedJson = cleanedJson.replaceAll("(?s)^```(?:json)?\\n|\\n```$", "");
                    }

                    // Parse JSON
                    Gson gson = new Gson();
                    Type listType = new TypeToken<ArrayList<QuestionModel>>(){}.getType();
                    List<QuestionModel> questions = gson.fromJson(cleanedJson, listType);

                    if (questions == null || questions.isEmpty()) {
                        callback.onError("No questions could be extracted.");
                    } else {
                        callback.onSuccess(questions);
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Parsing Error: " + e.getMessage());
                    callback.onError("Data format error. Please try again.");
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Connection Error", t);
                callback.onError("API Error: " + t.getLocalizedMessage());
            }
        }, executor);
    }

    /**
     * New Added Feature: Summarize text using AI
     */
    public static void generateSummary(String noteContent, SummaryCallback callback) {

        // Use same safety settings as quiz generation
        List<SafetySetting> safetySettings = new ArrayList<>();
        safetySettings.add(new SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH));
        safetySettings.add(new SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH));

        GenerativeModel gm = new GenerativeModel(
                "gemini-2.5-flash",
                API_KEY,
                null,
                safetySettings
        );
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        // Summarization Prompt
        String prompt = "Summarize the following notes into a concise paragraph. " +
                "Focus on the key concepts and most important information.\n\n" +
                "NOTES:\n" + noteContent;

        Content content = new Content.Builder().addText(prompt).build();

        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String summary = result.getText();
                if (summary != null && !summary.isEmpty()) {
                    callback.onSuccess(summary.trim());
                } else {
                    callback.onError("AI could not generate a summary.");
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Summarization Error", t);
                callback.onError("API Error: " + t.getLocalizedMessage());
            }
        }, executor);
    }
    public static void generateFlashcards(String noteContent, String type, String difficulty, String count, FlashcardCallback callback) {
        GenerativeModel gm = new GenerativeModel(
                "gemini-2.5-flash",
                API_KEY,
                null,
                new ArrayList<>() // Add safety settings here if needed
        );
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        String taskDetail = type.equalsIgnoreCase("Terminology")
                ? "Create Terminology flashcards. Front = Term, Back = Definition."
                : "Create Identification flashcards. Front = Sentence with a blank (____), Back = The missing word.";

        String prompt = "Context: " + noteContent + "\n\n" +
                "Task: " + taskDetail + "\n" +
                "Difficulty: " + difficulty + "\n" +
                "Count: " + count + "\n\n" +
                "Output: Return ONLY a raw JSON array. Objects must have 'front' and 'back' fields.";

        Content content = new Content.Builder().addText(prompt).build();
        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    String raw = result.getText().trim();
                    if (raw.startsWith("```")) raw = raw.replaceAll("(?s)^```(?:json)?\\n|\\n```$", "");

                    Gson gson = new Gson();
                    Type listType = new TypeToken<ArrayList<Flashcard>>(){}.getType();
                    List<Flashcard> cards = gson.fromJson(raw, listType);
                    callback.onSuccess(cards != null ? cards : new ArrayList<>());
                } catch (Exception e) { callback.onError("Parsing error"); }
            }
            @Override
            public void onFailure(Throwable t) { callback.onError(t.getMessage()); }
        }, executor);
    }

    // REPLACE your generateSummaryFromText method with this static version:
    public static void generateSummaryFromText(String fullText, SummaryCallback callback) {
        // 1. Initialize the model (You were missing this!)
        List<SafetySetting> safetySettings = new ArrayList<>();
        safetySettings.add(new SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH));
        safetySettings.add(new SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH));

        GenerativeModel gm = new GenerativeModel(
                "gemini-2.5-flash",
                API_KEY,
                null,
                safetySettings
        );
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        // 2. Execute Prompt
        String prompt = "You are an expert study assistant. I will provide text from a document. " +
                "Please create a comprehensive study note using HTML tags for formatting (<b>, <i>, <ul>, <li>). " +
                "Structure it with:\n" +
                "1. A clear Title\n" +
                "2. Key Concepts in bullet points\n" +
                "3. Detailed explanations\n\n" +
                "Text:\n" + fullText;

        Content content = new Content.Builder().addText(prompt).build();
        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String text = result.getText();
                if (text != null) {
                    callback.onSuccess(text);
                } else {
                    callback.onError("AI returned empty response (Safety filter likely)");
                }
            }
            @Override
            public void onFailure(Throwable t) {
                callback.onError(t.getMessage());
            }
        }, executor);
    }

    public interface StructuredNoteCallback {
        void onSuccess(String title, String htmlContent);
        void onError(String error);
    }

    public static void generateStructuredNote(String fullText, StructuredNoteCallback callback) {
        List<SafetySetting> safetySettings = new ArrayList<>();
        safetySettings.add(new SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH));
        safetySettings.add(new SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH));

        GenerativeModel gm = new GenerativeModel(
                "gemini-2.5-flash",
                API_KEY,
                null,
                safetySettings
        );
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        // Prompt that asks for a JSON object with "title" and "content"
        String prompt = "You are an expert study assistant. Create a well-structured study note from the text below.\n" +
                "Return ONLY a valid JSON object with two keys: \"title\" (a short, descriptive title) and \"content\" (HTML formatted study note).\n" +
                "In the content, use <b>, <i>, <ul>, <li> for structure. Include key concepts and explanations.\n\n" +
                "Text:\n" + fullText;

        Content content = new Content.Builder().addText(prompt).build();
        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String raw = result.getText();
                if (raw == null || raw.isEmpty()) {
                    callback.onError("AI returned empty response");
                    return;
                }
                try {
                    // Clean JSON (remove markdown if present)
                    String cleaned = raw.trim();
                    if (cleaned.startsWith("```")) {
                        cleaned = cleaned.replaceAll("(?s)^```(?:json)?\\n|\\n```$", "");
                    }
                    // Parse JSON
                    org.json.JSONObject obj = new org.json.JSONObject(cleaned);
                    String title = obj.getString("title");
                    String contentHtml = obj.getString("content");
                    callback.onSuccess(title, contentHtml);
                } catch (Exception e) {
                    Log.e(TAG, "JSON parse error", e);
                    callback.onError("Failed to parse AI response: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "API error", t);
                callback.onError("API error: " + t.getMessage());
            }
        }, executor);
    }

}