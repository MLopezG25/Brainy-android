package com.example.brainy.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.example.brainy.R;
import com.example.brainy.api.ApiClient;
import com.example.brainy.api.ApiService;
import com.example.brainy.api.models.Entry;
import com.example.brainy.api.models.Tag;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EntryDetailActivity extends AppCompatActivity {

    private MaterialCardView posterContainer;
    private ImageView ivDetailPoster;
    private TextView tvTitle, tvNotes, tvSourceUrl;
    private Chip chipCategory, chipSubcategory, chipStatus, chipTags, chipCompletedDate, chipCreatedAt, chipUpdatedAt;
    private MaterialButton btnEdit, btnDelete;
    private MaterialCardView cardFieldValues, cardNotes, cardSource;
    private LinearLayout fieldValuesContainer;
    private ApiService apiService;
    private int entryId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_detail);

        posterContainer = findViewById(R.id.posterContainer);
        posterContainer.setVisibility(View.GONE);
        ivDetailPoster = findViewById(R.id.ivDetailPoster);
        tvTitle = findViewById(R.id.tvTitle);
        tvNotes = findViewById(R.id.tvNotes);
        tvSourceUrl = findViewById(R.id.tvSourceUrl);
        chipCategory = findViewById(R.id.chipCategory);
        chipSubcategory = findViewById(R.id.chipSubcategory);
        chipStatus = findViewById(R.id.chipStatus);
        chipTags = findViewById(R.id.chipTags);
        chipCompletedDate = findViewById(R.id.chipCompletedDate);
        chipCreatedAt = findViewById(R.id.chipCreatedAt);
        chipUpdatedAt = findViewById(R.id.chipUpdatedAt);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        cardFieldValues = findViewById(R.id.cardFieldValues);
        cardNotes = findViewById(R.id.cardNotes);
        cardSource = findViewById(R.id.cardSource);
        fieldValuesContainer = findViewById(R.id.fieldValuesContainer);

        apiService = ApiClient.getApiService();

        entryId = getIntent().getIntExtra("entry_id", -1);
        if (entryId != -1) {
            loadEntry(entryId);
        }

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(EntryDetailActivity.this, EntryFormActivity.class);
            intent.putExtra("entry_id", entryId);
            startActivity(intent);
            finish();
        });

        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("¿Eliminar entrada?")
                .setMessage("Esta acción no se puede deshacer. ¿Estás seguro?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Sí, borrar", (dialog, which) -> deleteEntry())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deleteEntry() {
        apiService.deleteEntry(entryId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EntryDetailActivity.this, "Entrada eliminada", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EntryDetailActivity.this, "Error al eliminar", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (ApiClient.switchToNextUrl()) {
                    apiService = ApiClient.getApiService();
                    deleteEntry();
                } else {
                    Toast.makeText(EntryDetailActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void animateViewsIn() {
        tvTitle.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        chipCategory.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        chipSubcategory.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        chipStatus.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        chipTags.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        if (chipCompletedDate.getVisibility() == View.VISIBLE) {
            chipCompletedDate.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        }
        if (chipCreatedAt.getVisibility() == View.VISIBLE) {
            chipCreatedAt.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        }
        if (chipUpdatedAt.getVisibility() == View.VISIBLE) {
            chipUpdatedAt.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        }
        btnEdit.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        btnDelete.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
    }

    private void loadEntry(int entryId) {
        apiService.getEntry(entryId).enqueue(new Callback<Entry>() {
            @Override
            public void onResponse(Call<Entry> call, Response<Entry> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Entry entry = response.body();
                    tvTitle.setText(entry.getTitle());

                    // Cargar imagen del póster si existe
                    loadPosterImage(entry);

                    if (entry.getCategoryName() != null) {
                        chipCategory.setText(entry.getCategoryName());
                        chipCategory.setVisibility(View.VISIBLE);
                    }

                    if (entry.getSubcategoryName() != null) {
                        chipSubcategory.setText(entry.getSubcategoryName());
                        chipSubcategory.setVisibility(View.VISIBLE);
                    }

                    chipStatus.setText(entry.getStatusDisplay());
                    chipStatus.setVisibility(View.VISIBLE);

                    if (entry.getTags() != null && !entry.getTags().isEmpty()) {
                        StringBuilder tagsStr = new StringBuilder();
                        for (Tag tag : entry.getTags()) {
                            if (tagsStr.length() > 0) tagsStr.append(", ");
                            tagsStr.append(tag.getName());
                        }
                        chipTags.setText(tagsStr.toString());
                        chipTags.setVisibility(View.VISIBLE);
                    }

                    // Mostrar completed_date si el estado es "completado"
                    showCompletedDate(entry);

                    // Mostrar field_values
                    showFieldValues(entry);

                    // Mostrar notas si existen (sin la URL)
                    showNotes(entry);

                    // Mostrar fuente/origen si hay URL
                    showSource(entry);

                    // Mostrar created_at y updated_at
                    showDates(entry);

                    animateViewsIn();
                }
            }

            @Override
            public void onFailure(Call<Entry> call, Throwable t) {
                if (ApiClient.switchToNextUrl()) {
                    apiService = ApiClient.getApiService();
                    loadEntry(entryId);
                } else {
                    Toast.makeText(EntryDetailActivity.this, "Error al cargar la entrada", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showCompletedDate(Entry entry) {
        // solo mostramos el chip si el estado es completado o abandonado y hay fecha
        boolean tieneFecha = entry.getCompletedDate() != null && !entry.getCompletedDate().isEmpty();
        boolean estadoValido = "completado".equals(entry.getStatus()) || "abandonado".equals(entry.getStatus());

        if (tieneFecha && estadoValido) {
            String dateStr = entry.getCompletedDate();
            // Formatear la fecha si viene en formato ISO
            try {
                String[] parts = dateStr.split("T")[0].split("-");
                if (parts.length == 3) {
                    dateStr = parts[2] + "/" + parts[1] + "/" + parts[0];
                }
            } catch (Exception ignored) {}

            String prefijo = "completado".equals(entry.getStatus()) ? "Completado: " : "Abandonado: ";
            chipCompletedDate.setText(prefijo + dateStr);
            chipCompletedDate.setVisibility(View.VISIBLE);
        } else {
            chipCompletedDate.setVisibility(View.GONE);
        }
    }

    private void showFieldValues(Entry entry) {
        List<Map<String, Object>> fieldValues = entry.getFieldValues();
        if (fieldValues == null || fieldValues.isEmpty()) {
            return;
        }

        fieldValuesContainer.removeAllViews();

        for (Map<String, Object> fv : fieldValues) {
            String label = fv.get("label") != null ? fv.get("label").toString() : "";
            String value = fv.get("value") != null ? fv.get("value").toString() : "";
            String fieldType = fv.get("field_type") != null ? fv.get("field_type").toString() : "text";

            if (value.isEmpty()) continue;

            View rowView = getLayoutInflater().inflate(R.layout.item_field_value, fieldValuesContainer, false);
            TextView tvLabel = rowView.findViewById(R.id.tvFieldLabel);
            TextView tvValue = rowView.findViewById(R.id.tvFieldValue);

            tvLabel.setText(label + ":");
            tvValue.setText(value);

            if ("person".equals(fieldType)) {
                tvValue.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        ContextCompat.getDrawable(this, R.drawable.ic_person), null, null, null
                );
                tvValue.setCompoundDrawablePadding(8);
            }

            fieldValuesContainer.addView(rowView);
        }

        cardFieldValues.setVisibility(View.VISIBLE);
        cardFieldValues.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
    }

    private void showNotes(Entry entry) {
        String desc = entry.getDescription();
        if (desc == null || desc.isEmpty()) {
            return;
        }

        String notesText;
        if (desc.startsWith("URL: ")) {
            // Extraer solo las notas (lo que va después de la URL)
            int endOfUrl = desc.indexOf("\n\n");
            if (endOfUrl != -1) {
                notesText = desc.substring(endOfUrl + 2);
            } else {
                notesText = ""; // Solo había URL, sin notas
            }
        } else {
            notesText = desc;
        }

        if (!notesText.isEmpty()) {
            tvNotes.setText(notesText);
            cardNotes.setVisibility(View.VISIBLE);
            cardNotes.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        }
    }

    //  Extrae la URL del campo description (formato "URL: https://...")
    private String extractUrlFromDescription(Entry entry) {
        String desc = entry.getDescription();
        if (desc == null || !desc.startsWith("URL: ")) return null;
        int endOfUrl = desc.indexOf("\n\n");
        return endOfUrl != -1 ? desc.substring(5, endOfUrl) : desc.substring(5);
    }

    // Abre una URL en el navegador
    private void openUrl(String url) {
        if (url != null && !url.isEmpty()) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
    }

    private void showSource(Entry entry) {
        String sourceUrl = extractUrlFromDescription(entry);
        if (sourceUrl != null) {
            tvSourceUrl.setText(sourceUrl);
            cardSource.setVisibility(View.VISIBLE);
            cardSource.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
            // Click en texto o card abre el navegador
            View.OnClickListener openLink = v -> openUrl(sourceUrl);
            tvSourceUrl.setOnClickListener(openLink);
            cardSource.setOnClickListener(openLink);
        }
    }

    // Muestra las fechas de creación y última actualización
    private void showDates(Entry entry) {
        if (entry.getCreatedAt() != null && !entry.getCreatedAt().isEmpty()) {
            String dateStr = formatDateTime(entry.getCreatedAt());
            chipCreatedAt.setText("Creado: " + dateStr);
            chipCreatedAt.setVisibility(View.VISIBLE);
        }
        if (entry.getUpdatedAt() != null && !entry.getUpdatedAt().isEmpty()) {
            String dateStr = formatDateTime(entry.getUpdatedAt());
            chipUpdatedAt.setText("Actualizado: " + dateStr);
            chipUpdatedAt.setVisibility(View.VISIBLE);
        }
    }

    // Formatea una fecha ISO a formato legible DD/MM/YYYY
    private String formatDateTime(String isoDate) {
        try {
            String datePart = isoDate.split("T")[0];
            String[] parts = datePart.split("-");
            if (parts.length == 3) {
                return parts[2] + "/" + parts[1] + "/" + parts[0];
            }
        } catch (Exception ignored) {}
        return isoDate;
    }

    private void loadPosterImage(Entry entry) {
        String imageUrl = entry.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Hacer visible el contenedor del póster
            posterContainer.setVisibility(View.VISIBLE);

            // Cargar imagen con Glide
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.entry_card_placeholder)
                    .transition(DrawableTransitionOptions.withCrossFade(300))
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(16)))
                    .override(800, 1200)
                    .into(ivDetailPoster);

            // Animar entrada del contenedor completo (imagen + overlay)
            posterContainer.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        } else {
            posterContainer.setVisibility(View.GONE);
        }
    }
}
