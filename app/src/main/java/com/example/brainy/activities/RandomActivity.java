package com.example.brainy.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brainy.R;
import com.example.brainy.api.ApiClient;
import com.example.brainy.api.ApiService;
import com.example.brainy.api.models.Entry;
import com.example.brainy.utils.StatusUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RandomActivity extends AppCompatActivity {

    private MaterialCardView cardResult;
    private TextView tvTitle, tvCategory, tvDescription, tvSubtitle;
    private Chip chipStatus;
    private MaterialButton btnRandom;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_random);

        apiService = ApiClient.getApiService();

        cardResult = findViewById(R.id.cardRandomResult);
        tvTitle = findViewById(R.id.tvRandomTitle);
        tvCategory = findViewById(R.id.tvRandomCategory);
        tvDescription = findViewById(R.id.tvRandomDescription);
        tvSubtitle = findViewById(R.id.tvRandomSubtitle);
        chipStatus = findViewById(R.id.chipRandomStatus);
        btnRandom = findViewById(R.id.btnRandom);

        setupBottomNav();
        btnRandom.setOnClickListener(v -> fetchRandom());
        fetchRandom();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_random);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_random) return true;
            if (id == R.id.nav_hub) { startActivity(new Intent(this, MainActivity.class)); finish(); return true; }
            if (id == R.id.nav_add) { startActivity(new Intent(this, EntryFormActivity.class)); return true; }
            if (id == R.id.nav_timeline) { startActivity(new Intent(this, TimelineActivity.class)); finish(); return true; }
            if (id == R.id.nav_profile) { startActivity(new Intent(this, ProfileActivity.class)); finish(); return true; }
            return false;
        });
    }

    private void fetchRandom() {
        btnRandom.setEnabled(false);
        btnRandom.setText("Buscando...");

        apiService.getRandomEntry().enqueue(new Callback<Entry>() {
            @Override
            public void onResponse(Call<Entry> call, Response<Entry> response) {
                btnRandom.setEnabled(true);
                btnRandom.setText("¡Algo nuevo!");

                if (response.isSuccessful() && response.body() != null) {
                    Entry entry = response.body();
                    tvTitle.setText(entry.getTitle());
                    tvCategory.setText(
                            (entry.getCategoryName() != null ? entry.getCategoryName() : "") +
                            (entry.getSubcategoryName() != null ? " · " + entry.getSubcategoryName() : ""));
                    chipStatus.setText(StatusUtils.toDisplay(entry.getStatus()));
                    chipStatus.setVisibility(View.VISIBLE);

                    String desc = entry.getDescription();
                    if (desc != null && desc.startsWith("URL: ")) {
                        int end = desc.indexOf("\n\n");
                        desc = end != -1 ? desc.substring(end + 2) : "";
                    }
                    tvDescription.setText(desc != null && !desc.isEmpty() ? desc : "");
                    tvDescription.setVisibility(desc != null && !desc.isEmpty() ? View.VISIBLE : View.GONE);

                    cardResult.setVisibility(View.VISIBLE);
                    tvSubtitle.setText("Hoy podrías ver/leer/escuchar...");

                    cardResult.setOnClickListener(v -> {
                        Intent intent = new Intent(RandomActivity.this, EntryDetailActivity.class);
                        intent.putExtra("entry_id", entry.getId());
                        startActivity(intent);
                    });
                } else {
                    tvSubtitle.setText("No tienes nada pendiente. ¡Añade más contenido!");
                    cardResult.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<Entry> call, Throwable t) {
                btnRandom.setEnabled(true);
                btnRandom.setText("¡Algo nuevo!");
                Toast.makeText(RandomActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }
}