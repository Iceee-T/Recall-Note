package com.example.notewise;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {
    private List<Note> noteList;
    private OnNoteClickListener listener;
    private OnNoteLongClickListener longClickListener;

    public interface OnNoteClickListener {
        void onNoteClick(Note note, int position);
    }

    public interface OnNoteLongClickListener {
        void onNoteLongClick(Note note, int position);
    }

    public NoteAdapter(List<Note> noteList, OnNoteClickListener listener, OnNoteLongClickListener longClickListener) {
        this.noteList = noteList;
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        if (noteList == null || noteList.isEmpty()) return;

        Note note = noteList.get(position);
        holder.tvTitle.setText(note.getTitle());

        // FIX: Render HTML so the preview doesn't show raw <b> tags
        if (note.getContent() != null) {
            holder.tvContent.setText(Html.fromHtml(note.getContent(), Html.FROM_HTML_MODE_LEGACY));
        }

        holder.tvTime.setText(note.getTimestamp());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNoteClick(note, position);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                longClickListener.onNoteLongClick(note, position);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return noteList != null ? noteList.size() : 0;
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvTime;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvTime = itemView.findViewById(R.id.tvTimestamp); // Matches XML ID
        }
    }
}