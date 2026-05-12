package com.example.brainy.activities;

import android.os.Bundle;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brainy.R;
import com.example.brainy.api.ApiClient;
import com.example.brainy.api.ApiService;
import com.example.brainy.api.models.Entry;
import com.example.brainy.api.models.Tag;
import com.google.android.material.chip.Chip;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EntryDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvNotes;
    private Chip chipCategory, chipSubcategory, chipStatus, chipTags;
    private ApiService apiService;

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

        apiService = ApiClient.getApiService();

        int entryId = getIntent().getIntExtra("entry_id", -1);
        if (entryId != -1) {
            loadEntry(entryId);
        }
    }

    private void animateViewsIn() {
        tvTitle.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        chipCategory.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        chipSubcategory.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        chipStatus.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        chipTags.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        tvNotes.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
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
                        chipCategory.setVisibility(android.view.View.VISIBLE);
                    }

                    if (entry.getSubcategoryName() != null) {
                        chipSubcategory.setText(entry.getSubcategoryName());
                        chipSubcategory.setVisibility(android.view.View.VISIBLE);
                    }

                    chipStatus.setText(entry.getStatusDisplay());
                    chipStatus.setVisibility(android.view.View.VISIBLE);

                    if (entry.getTags() != null && !entry.getTags().isEmpty()) {
                        StringBuilder tagsStr = new StringBuilder();
                        for (Tag tag : entry.getTags()) {
                            if (tagsStr.length() > 0) tagsStr.append(", ");
                            tagsStr.append(tag.getName());
                        }
                        chipTags.setText(tagsStr.toString());
                        chipTags.setVisibility(android.view.View.VISIBLE);
                    }

                    if (entry.getDescription() != null && !entry.getDescription().isEmpty()) {
                        tvNotes.setText(entry.getDescription());
                    }

                    // Animar la entrada de los elementos
                    animateViewsIn();
                }
            }

            @Override
            public void onFailure(Call<Entry> call, Throwable t) {
                Toast.makeText(EntryDetailActivity.this, "Error al cargar la entrada", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
