package com.example.brainy.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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

    private TextView tvTitle, tvNotes;
    private Chip chipCategory, chipSubcategory, chipStatus, chipTags;
    private MaterialButton btnEdit, btnDelete, btnOpenImdb;
    private MaterialCardView cardFieldValues, cardNotes;
    private LinearLayout fieldValuesContainer;
    private ApiService apiService;
    private int entryId;
    private String imdbUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_detail);

        tvTitle = findViewById(R.id.tvTitle);
        tvNotes = findViewById(R.id.tvNotes);
        chipCategory = findViewById(R.id.chipCategory);
        chipSubcategory = findViewById(R.id.chipSubcategory);
        chipStatus = findViewById(R.id.chipStatus);
        chipTags = findViewById(R.id.chipTags);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        btnOpenImdb = findViewById(R.id.btnOpenImdb);
        cardFieldValues = findViewById(R.id.cardFieldValues);
        cardNotes = findViewById(R.id.cardNotes);
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

        btnOpenImdb.setOnClickListener(v -> {
            if (imdbUrl != null && !imdbUrl.isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(imdbUrl));
                startActivity(browserIntent);
            }
        });
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

                    // Mostrar field_values
                    showFieldValues(entry);

                    // Mostrar notas si existen (sin la URL)
                    showNotes(entry);

                    // Mostrar botón IMDB si hay URL
                    showImdbButton(entry);

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

    private void showImdbButton(Entry entry) {
        String desc = entry.getDescription();
        if (desc != null && desc.startsWith("URL: ")) {
            int endOfUrl = desc.indexOf("\n\n");
            if (endOfUrl != -1) {
                imdbUrl = desc.substring(5, endOfUrl);
            } else {
                imdbUrl = desc.substring(5);
            }

            // Detectar el dominio para poner el texto adecuado
            String buttonText = "Abrir enlace";
            if (imdbUrl != null) {
                String lowerUrl = imdbUrl.toLowerCase();
                if (lowerUrl.contains("imdb.com")) {
                    buttonText = "Abrir en IMDB";
                } else if (lowerUrl.contains("discogs.com")) {
                    buttonText = "Abrir en Discogs";
                } else if (lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be")) {
                    buttonText = "Abrir en YouTube";
                } else if (lowerUrl.contains("spotify.com")) {
                    buttonText = "Abrir en Spotify";
                } else if (lowerUrl.contains("goodreads.com")) {
                    buttonText = "Abrir en Goodreads";
                } else if (lowerUrl.contains("letterboxd.com")) {
                    buttonText = "Abrir en Letterboxd";
                } else if (lowerUrl.contains("myanimelist.net")) {
                    buttonText = "Abrir en MyAnimeList";
                } else if (lowerUrl.contains("steamcommunity.com") || lowerUrl.contains("store.steampowered.com")) {
                    buttonText = "Abrir en Steam";
                } else if (lowerUrl.contains("wikipedia.org")) {
                    buttonText = "Abrir en Wikipedia";
                } else {
                    buttonText = "Abrir enlace";
                }
            }

            btnOpenImdb.setText(buttonText);
            btnOpenImdb.setVisibility(View.VISIBLE);
            btnOpenImdb.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        }
    }
}
