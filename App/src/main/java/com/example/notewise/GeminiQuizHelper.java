package com.example.notewise;

import android.util.Log;
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
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Helper class to interact with Google Gemini AI for generating quizzes,
 * flashcards, and summaries with "High Priority" highlighting support.
 */
public class GeminiQuizHelper {

    private static final String TAG = "GEMINI_DEBUG";
    // Replace with your actual secure API Key management
    private static final String API_KEY = "AIzaSyCemHxlvPRMbCMudPGvcFSuzsDvtfQW19Q";
    private static final String MODEL_NAME = "gemini-1.5-flash";

    public static void generateSummary(String text, SummaryCallback summaryCallback) {
    }

    public interface QuizCallback {
        void onSuccess(List<QuestionModel> questions);
        void onError(String error);
    }

    public interface FlashcardCallback {
        void onSuccess(List<Flashcard> flashcards);
        void onError(String error);
    }

    public interface SummaryCallback {
        void onSuccess(String summary);
        void onError(String error);
    }

    /**
     * Initializes the Gemini model with specific safety thresholds.
     */
    private static GenerativeModelFutures getModel() {
        List<SafetySetting> safetySettings = new ArrayList<>();
        safetySettings.add(new SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH));
        safetySettings.add(new SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH));

        GenerativeModel gm = new GenerativeModel(MODEL_NAME, API_KEY, null, safetySettings);
        return GenerativeModelFutures.from(gm);
    }

    /**
     * Generates a quiz. Prioritizes text in **bold** or [[brackets]].
     */
    public static void generateQuiz(String noteContent, String difficulty, String numQuestions, QuizCallback callback) {
        String prompt = "You are an educator. Generate a quiz from the notes below.\n\n" +
                "NOTES:\n" + noteContent + "\n\n" +
                "HIGH PRIORITY INSTRUCTION:\n" +
                "- Any text in **bold** or [[brackets]] is highly important.\n" +
                "- Ensure at least 60% of the questions are about these prioritized topics.\n\n" +
                "REQUIREMENTS:\n" +
                "- Difficulty: " + difficulty + "\n" +
                "- Number of Questions: " + numQuestions + "\n" +
                "- Format: Return ONLY a raw JSON array. No preamble.\n" +
                "- Structure: Array of objects with 'questionText', 'choices' (4), 'correctOptionIndex', and 'type'.";

        executeRequest(getModel(), prompt, new InternalParser<List<QuestionModel>>() {
            @Override
            public List<QuestionModel> parse(String raw) {
                Type listType = new TypeToken<ArrayList<QuestionModel>>(){}.getType();
                return new Gson().fromJson(cleanJson(raw), listType);
            }
            @Override
            public void onSuccess(List<QuestionModel> result) { callback.onSuccess(result); }
            @Override
            public void onError(String error) { callback.onError(error); }
        });
    }

    /**
     * Generates flashcards. Prioritizes text in **bold** or [[brackets]].
     */
    public static void generateFlashcards(String noteContent, String type, String difficulty, String count, FlashcardCallback callback) {
        String taskDetail = type.equalsIgnoreCase("Terminology") ? "Front=Term, Back=Definition" : "Front=Sentence blank, Back=Word";

        String prompt = "Generate study flashcards. Prioritize text in **bold** or [[brackets]].\n\n" +
                "NOTES:\n" + noteContent + "\n\n" +
                "TASK: " + taskDetail + "\n" +
                "Count: " + count + "\n" +
                "Output: ONLY raw JSON array of objects with 'front' and 'back' fields.";

        executeRequest(getModel(), prompt, new InternalParser<List<Flashcard>>() {
            @Override
            public List<Flashcard> parse(String raw) {
                Type listType = new TypeToken<ArrayList<Flashcard>>(){}.getType();
                return new Gson().fromJson(cleanJson(raw), listType);
            }
            @Override
            public void onSuccess(List<Flashcard> result) { callback.onSuccess(result); }
            @Override
            public void onError(String error) { callback.onError(error); }
        });
    }

    /**
     * Generates comprehensive study notes using HTML formatting.
     */
    public static void generateSummaryFromText(String fullText, SummaryCallback callback) {
        String prompt = "Create comprehensive study notes using HTML (<b>, <i>, <ul>, <li>).\n" +
                "Structure: Title, Key Concepts, and detailed explanations.\n\n" +
                "Text:\n" + fullText;

        executeRequest(getModel(), prompt, new InternalParser<String>() {
            @Override
            public String parse(String raw) { return raw; }
            @Override
            public void onSuccess(String result) { callback.onSuccess(result); }
            @Override
            public void onError(String error) { callback.onError(error); }
        });
    }

    /**
     * CLEANJSON FIX: Instead of using problematic regex (replaceAll),
     * this manually finds the start and end of the JSON array/object.
     */
    private static String cleanJson(String raw) {
        if (raw == null || raw.isEmpty()) return "[]";

        String s = raw.trim();

        // Find the JSON boundaries to ignore markdown code block markers
        int start = s.indexOf("[");
        int end = s.lastIndexOf("]");

        if (start != -1 && end != -1 && end > start) {
            return s.substring(start, end + 1);
        }

        return s;
    }

    private interface InternalParser<T> {
        T parse(String raw);
        void onSuccess(T result);
        void onError(String error);
    }

    /**
     * Generic wrapper for model execution to reduce repeated code.
     */
    private static <T> void executeRequest(GenerativeModelFutures model, String prompt, InternalParser<T> parser) {
        Content content = new Content.Builder().addText(prompt).build();
        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    String raw = result.getText();
                    if (raw == null) {
                        parser.onError("Empty AI response");
                        return;
                    }
                    parser.onSuccess(parser.parse(raw));
                } catch (Exception e) {
                    parser.onError("Data parsing failed");
                }
            }
            @Override
            public void onFailure(Throwable t) {
                parser.onError(t.getLocalizedMessage());
            }
        }, executor);
    }
}