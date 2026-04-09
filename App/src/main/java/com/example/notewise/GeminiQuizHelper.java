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

public class GeminiQuizHelper {

    private static final String TAG = "GEMINI_DEBUG";
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

    private static GenerativeModelFutures getModel() {
        List<SafetySetting> safetySettings = new ArrayList<>();
        safetySettings.add(new SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH));
        safetySettings.add(new SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH));

        GenerativeModel gm = new GenerativeModel(MODEL_NAME, API_KEY, null, safetySettings);
        return GenerativeModelFutures.from(gm);
    }

    public static void generateQuiz(String noteContent, String difficulty, String numQuestions, QuizCallback callback) {
        String prompt = "Generate a quiz from these notes. PRIORITY RULE: Text in **bold** or [[brackets]] is highly important. " +
                "Focus 70% of questions on these marked topics.\n\n" +
                "NOTES:\n" + noteContent + "\n\n" +
                "Difficulty: " + difficulty + " | Count: " + numQuestions + "\n" +
                "Format: Return ONLY a raw JSON array of objects with 'questionText', 'choices' (4), 'correctOptionIndex', and 'type'.";

        executeRequest(getModel(), prompt, new InternalParser<List<QuestionModel>>() {
            @Override
            public List<QuestionModel> parse(String raw) {
                return new Gson().fromJson(cleanJson(raw), new TypeToken<ArrayList<QuestionModel>>(){}.getType());
            }
            @Override
            public void onSuccess(List<QuestionModel> result) { callback.onSuccess(result); }
            @Override
            public void onError(String error) { callback.onError(error); }
        });
    }

    public static void generateFlashcards(String noteContent, String type, String difficulty, String count, FlashcardCallback callback) {
        String task = type.equalsIgnoreCase("Terminology") ? "Front=Term, Back=Def" : "Front=Sentence blank, Back=Word";
        String prompt = "Create flashcards. PRIORITY RULE: Prioritize text in **bold** or [[brackets]].\n\n" +
                "NOTES:\n" + noteContent + "\n\n" +
                "TASK: " + task + " | Count: " + count + "\n" +
                "Output: ONLY raw JSON array of objects with 'front' and 'back'.";

        executeRequest(getModel(), prompt, new InternalParser<List<Flashcard>>() {
            @Override
            public List<Flashcard> parse(String raw) {
                return new Gson().fromJson(cleanJson(raw), new TypeToken<ArrayList<Flashcard>>(){}.getType());
            }
            @Override
            public void onSuccess(List<Flashcard> result) { callback.onSuccess(result); }
            @Override
            public void onError(String error) { callback.onError(error); }
        });
    }

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

    // FIXED: Uses substring instead of replaceAll to avoid regex errors
    private static String cleanJson(String raw) {
        if (raw == null || raw.isEmpty()) return "[]";
        String s = raw.trim();
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

    private static <T> void executeRequest(GenerativeModelFutures model, String prompt, InternalParser<T> parser) {
        Content content = new Content.Builder().addText(prompt).build();
        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    String raw = result.getText();
                    if (raw == null) { parser.onError("Empty response"); return; }
                    parser.onSuccess(parser.parse(raw));
                } catch (Exception e) { parser.onError("Parse error"); }
            }
            @Override
            public void onFailure(Throwable t) { parser.onError(t.getLocalizedMessage()); }
        }, executor);
    }
}