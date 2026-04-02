package com.example.notewise;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class QuizAdapter extends RecyclerView.Adapter<QuizAdapter.QuizViewHolder> {

    private List<Quiz> quizList;
    private Context context;
    private OnQuizClickListener listener;
    private OnQuizLongClickListener longClickListener;

    public interface OnQuizLongClickListener {
        void onQuizLongClick(Quiz quiz, int position);
    }

    public interface OnQuizClickListener {
        void onQuizClick(Quiz quiz);
    }

    public QuizAdapter(Context context, List<Quiz> quizList, OnQuizClickListener listener, OnQuizLongClickListener longClickListener) {
        this.context = context;
        this.quizList = quizList;
        this.listener = listener;
        this.longClickListener = longClickListener; // Initialize it here
    }

    @NonNull
    @Override
    public QuizViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quiz, parent, false);
        return new QuizViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuizViewHolder holder, int position) {
        Quiz quiz = quizList.get(position);

        holder.tvTitle.setText(quiz.getTitle());
        holder.tvDetails.setText(quiz.getQuestionCount() + " Questions • " +
                (quiz.getDescription() != null ? quiz.getDescription() : "AI Generated"));

        holder.itemView.setOnClickListener(v -> {
            // Pass the entire Quiz object (including AI questions if present)
            Intent intent = new Intent(context, QuizPreviewActivity.class);
            intent.putExtra("SELECTED_QUIZ", quiz);
            context.startActivity(intent);

            if (listener != null) listener.onQuizClick(quiz);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                longClickListener.onQuizLongClick(quiz, position);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return quizList != null ? quizList.size() : 0;
    }

    public static class QuizViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDetails;
        public QuizViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvQuizTitle);
            tvDetails = itemView.findViewById(R.id.tvQuizDetails);
        }
    }
}