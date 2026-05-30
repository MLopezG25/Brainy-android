package com.example.brainy.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.brainy.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsActivity extends AppCompatActivity {

    private TextView tvColumnCount;
    private MaterialButton btnDecrease, btnIncrease, btnSaveApiKeys;
    private TextInputEditText etDiscogsToken, etTmdbApiKey, etGoogleBooksApiKey;
    private SwitchMaterial switchDarkMode;
    private int currentColumns = 3;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Animación de entrada
        View root = findViewById(android.R.id.content);
        root.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));

        preferences = getSharedPreferences("brainy_prefs", MODE_PRIVATE);
        currentColumns = preferences.getInt("grid_columns", 3);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Controles de columnas
        tvColumnCount = findViewById(R.id.tvColumnCount);
        btnDecrease = findViewById(R.id.btnDecreaseColumns);
        btnIncrease = findViewById(R.id.btnIncreaseColumns);

        tvColumnCount.setText(String.valueOf(currentColumns));

        btnDecrease.setOnClickListener(v -> {
            if (currentColumns > 1) {
                currentColumns--;
                updateColumnCount();
            } else {
                Toast.makeText(this, "Mínimo 1 columna", Toast.LENGTH_SHORT).show();
            }
        });

        btnIncrease.setOnClickListener(v -> {
            if (currentColumns < 6) {
                currentColumns++;
                updateColumnCount();
            } else {
                Toast.makeText(this, "Máximo 6 columnas", Toast.LENGTH_SHORT).show();
            }
        });

        // Campos de APIs
        etDiscogsToken = findViewById(R.id.etDiscogsToken);
        etTmdbApiKey = findViewById(R.id.etTmdbApiKey);
        etGoogleBooksApiKey = findViewById(R.id.etGoogleBooksApiKey);
        btnSaveApiKeys = findViewById(R.id.btnSaveApiKeys);

        // Cargar tokens guardados
        String savedDiscogs = preferences.getString("discogs_token", "");
        String savedTmdb = preferences.getString("tmdb_api_key", "");
        String savedGoogleBooks = preferences.getString("google_books_api_key", "");
        etDiscogsToken.setText(savedDiscogs);
        etTmdbApiKey.setText(savedTmdb);
        etGoogleBooksApiKey.setText(savedGoogleBooks);

        btnSaveApiKeys.setOnClickListener(v -> saveApiKeys());

        // Modo oscuro
        switchDarkMode = findViewById(R.id.switchDarkMode);
        boolean isDarkMode = preferences.getBoolean("dark_mode", false);
        switchDarkMode.setChecked(isDarkMode);

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("dark_mode", isChecked).apply();
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        // Botón para ir a importación JSON
        MaterialButton btnGoToImport = findViewById(R.id.btnGoToImport);
        btnGoToImport.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ImportActivity.class);
            startActivity(intent);
        });
    }

    private void updateColumnCount() {
        tvColumnCount.setText(String.valueOf(currentColumns));
        preferences.edit().putInt("grid_columns", currentColumns).apply();
        Toast.makeText(this, "Columnas: " + currentColumns, Toast.LENGTH_SHORT).show();
    }

    private void saveApiKeys() {
        String discogsToken = etDiscogsToken.getText().toString().trim();
        String tmdbApiKey = etTmdbApiKey.getText().toString().trim();
        String googleBooksApiKey = etGoogleBooksApiKey.getText().toString().trim();

        preferences.edit()
                .putString("discogs_token", discogsToken)
                .putString("tmdb_api_key", tmdbApiKey)
                .putString("google_books_api_key", googleBooksApiKey)
                .apply();

        Toast.makeText(this, "Tokens guardados", Toast.LENGTH_SHORT).show();
    }
}
