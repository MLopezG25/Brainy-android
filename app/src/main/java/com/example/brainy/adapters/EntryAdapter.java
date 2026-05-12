package com.example.brainy.adapters;

import android.animation.AnimatorSet;

import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brainy.R;
import com.example.brainy.api.models.Entry;

import java.util.List;

public class EntryAdapter extends RecyclerView.Adapter<EntryAdapter.EntryViewHolder> {

    private List<Entry> entries;
    private OnEntryClickListener listener;
    private int lastAnimatedPosition = -1;

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
        animateItem(holder.itemView, position);
    }

    @Override
    public int getItemCount() {
        return entries != null ? entries.size() : 0;
    }

    public void updateEntries(List<Entry> newEntries) {
        this.entries = newEntries;
        lastAnimatedPosition = -1; // Resetear para que se vuelvan a animar
        notifyDataSetChanged();
    }

    private void animateItem(View view, int position) {
        // Solo animar si no se ha animado antes esta posición
        if (position <= lastAnimatedPosition) {
            return;
        }
        lastAnimatedPosition = position;

        // Configurar estado inicial (invisible y escalado)
        view.setAlpha(0f);
        view.setScaleX(0.8f);
        view.setScaleY(0.8f);
        view.setTranslationY(60f);

        // Crear animaciones
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        fadeIn.setDuration(350);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1f);
        scaleX.setDuration(350);

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.8f, 1f);
        scaleY.setDuration(350);

        ObjectAnimator slideUp = ObjectAnimator.ofFloat(view, "translationY", 60f, 0f);
        slideUp.setDuration(350);

        // Agrupar animaciones
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(fadeIn, scaleX, scaleY, slideUp);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.setStartDelay(position * 80L); // Retraso escalonado: 80ms por item
        animatorSet.start();
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
