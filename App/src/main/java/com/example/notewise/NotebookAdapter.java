package com.example.notewise;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class NotebookAdapter extends RecyclerView.Adapter<NotebookAdapter.ViewHolder> {
    private List<Notebook> notebookList;
    private OnNotebookLongClickListener longClickListener; // Add this

    // Add this interface
    public interface OnNotebookLongClickListener {
        void onNotebookLongClick(Notebook notebook, int position);
    }

    public NotebookAdapter(List<Notebook> notebookList, OnNotebookLongClickListener longClickListener) {
        this.notebookList = notebookList;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notebook, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notebook notebook = notebookList.get(position);
        holder.tvName.setText(notebook.getName());
        holder.tvCount.setText(notebook.getNoteCount() + " notes");

        // The Bridge: Click a notebook to open its specific notes
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), NotesListActivity.class);
            intent.putExtra("notebook_id", notebook.getId());
            intent.putExtra("notebook_name", notebook.getName());
            v.getContext().startActivity(intent);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                longClickListener.onNotebookLongClick(notebook, position);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return notebookList != null ? notebookList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCount;
        public ViewHolder(View view) {
            super(view);
            tvName = view.findViewById(R.id.tvNotebookName);
            tvCount = view.findViewById(R.id.tvNoteCount);
        }
    }
}