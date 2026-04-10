package com.example.brainy.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brainy.R;
import com.example.brainy.api.models.Entry;

import java.util.List;

public class EntryAdapter extends RecyclerView.Adapter<EntryAdapter.EntryViewHolder> {

    private List<Entry> entries;
    private OnEntryClickListener listener;

    public interface OnEntryClickListener {
        void onEntryClick(Entry entry);
    }

    public EntryAdapter(List<Entry> entries, OnEntryClickListener listener) {
        this.entries = entries;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_entry, parent, false);
        return new EntryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntryViewHolder holder, int position) {
        Entry entry = entries.get(position);
        holder.bind(entry, listener);
    }

    @Override
    public int getItemCount() {
        return entries != null ? entries.size() : 0;
    }

    public void updateEntries(List<Entry> newEntries) {
        this.entries = newEntries;
        notifyDataSetChanged();
    }

    static class EntryViewHolder extends RecyclerView.ViewHolder {

        private TextView tvTitle;
        private TextView tvCategory;
        private TextView tvStatus;

        EntryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }

        void bind(final Entry entry, final OnEntryClickListener listener) {
            tvTitle.setText(entry.getTitle());

            // Mostrar categoría y subcategoría si existen
            String categoryText = "";
            if (entry.getCategoryName() != null) {
                categoryText = entry.getCategoryName();
            }
            if (entry.getSubcategoryName() != null) {
                categoryText += " · " + entry.getSubcategoryName();
            }
            tvCategory.setText(categoryText);

            // Mostrar el estado en español
            tvStatus.setText("Estado: " + entry.getStatusDisplay());

            // Click en la tarjeta para ir al detalle
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEntryClick(entry);
                }
            });
        }
    }
}
