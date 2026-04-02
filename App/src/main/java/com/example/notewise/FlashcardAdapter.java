package com.example.notewise;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FlashcardAdapter extends RecyclerView.Adapter<FlashcardAdapter.ViewHolder> {

    private List<FlashcardSet> flashcardSets;
    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;

    // Interface for regular clicks
    public interface OnItemClickListener {
        void onItemClick(FlashcardSet set);
    }

    // Interface for long clicks (Delete logic)
    public interface OnItemLongClickListener {
        void onItemLongClick(FlashcardSet set, int position);
    }

    public FlashcardAdapter(List<FlashcardSet> flashcardSets, OnItemClickListener listener, OnItemLongClickListener longClickListener) {
        this.flashcardSets = flashcardSets;
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_flash_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FlashcardSet set = flashcardSets.get(position);

        // 1. ADDED NUMBERING
        String numberedTitle = (position + 1) + ". " + set.getTitle();
        holder.tvTitle.setText(numberedTitle);

        // 2. FIXED TYPE ERROR (Using set.getType() instead of getDescription())
        holder.tvCardType.setText("Type: " + (set.getType() != null ? set.getType() : "AI Generated"));

        // 3. SET CARD COUNT
        int count = (set.getCards() != null) ? set.getCards().size() : 0;
        holder.tvCount.setText(count + " Cards");

        // 4. REGULAR CLICK
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(set);
            }
        });

        // 5. LONG CLICK (For Deleting)
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                // Haptic feedback makes it feel like a real button press
                v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                longClickListener.onItemLongClick(set, position);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return flashcardSets != null ? flashcardSets.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCardType, tvCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ensure these IDs match your item_flash_card.xml exactly
            tvTitle = itemView.findViewById(R.id.tvFlashcardTitle);
            tvCardType = itemView.findViewById(R.id.tvCardType);
            tvCount = itemView.findViewById(R.id.tvCardCount);
        }
    }
}