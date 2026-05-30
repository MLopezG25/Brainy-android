package com.example.brainy.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.example.brainy.R;
import com.example.brainy.api.models.Entry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntryAdapter extends RecyclerView.Adapter<EntryAdapter.EntryViewHolder> {

    private List<Entry> entries;
    private OnEntryClickListener listener;
    private int lastAnimatedPosition = -1;

    // colores por categoría
    private static final Map<String, Integer> categoryColors = new HashMap<>();
    static {
        categoryColors.put("Cine", 0xFFD4A843);
        categoryColors.put("Música", 0xFFC4912E);
        categoryColors.put("Literatura", 0xFF8B6F47);
        categoryColors.put("Videojuegos", 0xFFB8863C);
        categoryColors.put("Arte", 0xFFE8A87C);
        categoryColors.put("Ciencia", 0xFF6B8E6B);
        categoryColors.put("Historia", 0xFFC8966A);
        categoryColors.put("Arquitectura", 0xFF9E9E9E);
    }

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
        lastAnimatedPosition = -1;
        notifyDataSetChanged();
    }

    private void animateItem(View view, int position) {
        if (position <= lastAnimatedPosition) {
            return;
        }
        lastAnimatedPosition = position;

        view.setAlpha(0f);
        view.setTranslationY(40f);

        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setStartDelay(position * 40L)
                .start();
    }

    static class EntryViewHolder extends RecyclerView.ViewHolder {

        private ImageView ivPoster;
        private TextView tvTitle;
        private TextView tvCategory;
        private ImageView ivStatus;

        EntryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.ivPoster);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            ivStatus = itemView.findViewById(R.id.ivStatus);
        }

        void bind(final Entry entry, final OnEntryClickListener listener) {
            tvTitle.setText(entry.getTitle());

            // Cargar imagen con Glide si hay image_url
            String imageUrl = entry.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(ivPoster.getContext())
                        .load(imageUrl)
                        .placeholder(android.R.color.transparent)
                        .transition(DrawableTransitionOptions.withCrossFade(200))
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(12)))
                        .override(600, 900)
                        .into(ivPoster);
            } else {
                Glide.with(ivPoster.getContext()).clear(ivPoster);
                ivPoster.setImageDrawable(null);
                ivPoster.setBackgroundResource(R.drawable.entry_card_placeholder);
            }

            // Mostrar categoría y subcategoría si existen
            String categoryText = "";
            if (entry.getCategoryName() != null) {
                categoryText = entry.getCategoryName();
            }
            if (entry.getSubcategoryName() != null) {
                categoryText += " · " + entry.getSubcategoryName();
            }
            tvCategory.setText(categoryText);

            // Color por categoría
            if (entry.getCategoryName() != null && categoryColors.containsKey(entry.getCategoryName())) {
                int color = categoryColors.get(entry.getCategoryName());
                tvCategory.setTextColor(color);
            } else {
                tvCategory.setTextColor(
                        ContextCompat.getColor(tvCategory.getContext(), R.color.golden_on_surface_variant));
            }

            // Mostrar estado como icono custom
            int statusIconRes;
            switch (entry.getStatus()) {
                case "pendiente":
                    statusIconRes = R.drawable.ic_status_pending;
                    break;
                case "en_progreso":
                    statusIconRes = R.drawable.ic_status_progress;
                    break;
                case "completado":
                    statusIconRes = R.drawable.ic_status_completed;
                    break;
                case "abandonado":
                    statusIconRes = R.drawable.ic_status_abandoned;
                    break;
                default:
                    statusIconRes = R.drawable.ic_status_pending;
            }
            ivStatus.setImageResource(statusIconRes);

            // Click en la tarjeta para ir al detalle
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEntryClick(entry);
                }
            });
        }
    }
}
