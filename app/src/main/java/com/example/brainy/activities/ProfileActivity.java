package com.example.brainy.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;

import com.example.brainy.R;
import com.example.brainy.api.ApiClient;
import com.example.brainy.api.ApiService;
import com.example.brainy.api.models.StatsResponse;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView textUsername, textEmail;
    private TextView textTotalEntries, textPendiente, textEnProgreso, textCompletado, textAbandonado;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        textUsername = findViewById(R.id.text_username);
        textEmail = findViewById(R.id.text_email);
        textTotalEntries = findViewById(R.id.text_total_entries);
        textPendiente = findViewById(R.id.text_pendiente);
        textEnProgreso = findViewById(R.id.text_en_progreso);
        textCompletado = findViewById(R.id.text_completado);
        textAbandonado = findViewById(R.id.text_abandonado);

        apiService = ApiClient.getApiService();

        MaterialButton buttonSettings = findViewById(R.id.button_settings);
        buttonSettings.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        buttonSettings.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        MaterialButton buttonLogout = findViewById(R.id.button_logout);
        buttonLogout.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        buttonLogout.setOnClickListener(v -> logout());

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_profile);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_profile) {
                return true;
            } else if (itemId == R.id.nav_hub) {
                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_timeline) {
                Intent intent = new Intent(ProfileActivity.this, TimelineActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_add) {
                Intent intent = new Intent(ProfileActivity.this, EntryFormActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        loadUserInfo();
        loadStats();
    }

    private void loadUserInfo() {
        SharedPreferences preferences = getSharedPreferences("brainy_prefs", MODE_PRIVATE);
        String username = preferences.getString("username", "Usuario");
        String email = preferences.getString("email", "");
        textUsername.setText(username);
        textEmail.setText(email);
    }

    private void loadStats() {
        apiService.getStats().enqueue(new Callback<StatsResponse>() {
            @Override
            public void onResponse(Call<StatsResponse> call, Response<StatsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    StatsResponse stats = response.body();
                    textTotalEntries.setText(String.valueOf(stats.getTotalEntries()));
                    textPendiente.setText(String.valueOf(stats.getPendiente()));
                    textEnProgreso.setText(String.valueOf(stats.getEnProgreso()));
                    textCompletado.setText(String.valueOf(stats.getCompletado()));
                    textAbandonado.setText(String.valueOf(stats.getAbandonado()));
                }
            }

            @Override
            public void onFailure(Call<StatsResponse> call, Throwable t) {
                if (ApiClient.switchToNextUrl()) {
                    apiService = ApiClient.getApiService();
                    loadStats();
                } else {
                    Toast.makeText(ProfileActivity.this, "Error al cargar estadísticas", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void logout() {
        apiService.logout().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                // Limpiamos las preferencias y volvemos al login
                logoutAndGoToLogin();
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                // Aunque falle la red, limpiamos las preferencias y volvemos al login
                logoutAndGoToLogin();
            }
        });
    }

    private void logoutAndGoToLogin() {
        SharedPreferences preferences = getSharedPreferences("brainy_prefs", MODE_PRIVATE);
        preferences.edit().clear().apply();
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
