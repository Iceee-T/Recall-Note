package com.example.notewise;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.ViewHolder> {

    private final List<QuestionModel> questionList;

    public QuestionAdapter(List<QuestionModel> questionList) {
        this.questionList = questionList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_question, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuestionModel question = questionList.get(position);

        holder.tvNumber.setText("Question #" + (position + 1));

        // Focus listener to update counter in Activity
        holder.etDescription.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && v.getContext() instanceof ManualquizActivity) {
                ((ManualquizActivity) v.getContext()).updateQuestionCounter(holder.getAdapterPosition());
            }
        });

        if (holder.descriptionWatcher != null) {
            holder.etDescription.removeTextChangedListener(holder.descriptionWatcher);
        }

        holder.etDescription.setText(question.getQuestionText());
        holder.descriptionWatcher = new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                question.setQuestionText(s.toString());
            }
        };
        holder.etDescription.addTextChangedListener(holder.descriptionWatcher);

        holder.cbDeleteSelection.setOnCheckedChangeListener(null);
        holder.cbDeleteSelection.setChecked(question.isSelected());
        holder.cbDeleteSelection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            question.setSelected(isChecked);
        });

        holder.tvAddChoice.setOnClickListener(v -> {
            question.getChoices().add("");
            refreshChoices(holder, question);
        });

        refreshChoices(holder, question);
    }

    private void refreshChoices(ViewHolder holder, QuestionModel question) {
        holder.choicesLayout.removeAllViews();

        for (int i = 0; i < question.getChoices().size(); i++) {
            final int index = i;
            View choiceView = LayoutInflater.from(holder.itemView.getContext())
                    .inflate(R.layout.item_choice_input, holder.choicesLayout, false);

            RadioButton rb = choiceView.findViewById(R.id.rbCorrect);
            EditText et = choiceView.findViewById(R.id.etChoiceText);

            et.setText(question.getChoices().get(index));
            rb.setChecked(index == question.getCorrectOptionIndex());

            // Add focus listener to choices too
            et.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && v.getContext() instanceof ManualquizActivity) {
                    ((ManualquizActivity) v.getContext()).updateQuestionCounter(holder.getAdapterPosition());
                }
            });

            et.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (index < question.getChoices().size()) {
                        question.getChoices().set(index, s.toString());
                    }
                }
            });

            rb.setOnClickListener(v -> {
                question.setCorrectOptionIndex(index);
                refreshChoices(holder, question);
            });

            holder.choicesLayout.addView(choiceView);
        }
    }

    @Override
    public int getItemCount() { return questionList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumber, tvAddChoice;
        EditText etDescription;
        CheckBox cbDeleteSelection;
        LinearLayout choicesLayout;
        TextWatcher descriptionWatcher;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumber = itemView.findViewById(R.id.tvQuestionNumber);
            etDescription = itemView.findViewById(R.id.etDescription);
            cbDeleteSelection = itemView.findViewById(R.id.cbDeleteSelection);
            choicesLayout = itemView.findViewById(R.id.choicesLayout);
            tvAddChoice = itemView.findViewById(R.id.tvAddChoice);
        }
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void afterTextChanged(Editable s) {}
    }
}