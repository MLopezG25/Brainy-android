package com.example.brainy.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.brainy.R;
import com.example.brainy.api.ApiClient;
import com.example.brainy.api.ApiService;

/**
 * Actividad base que centraliza:
 * - Acceso al ApiService (getApiService)
 * - SharedPreferences (getPrefs)
 * - Redirección a login cuando la sesión expira (goToLogin)
 * - Animación de entrada fade_in
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected ApiService apiService;
    protected SharedPreferences prefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiService = ApiClient.getApiService();
        prefs = getSharedPreferences("brainy_prefs", MODE_PRIVATE);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        // Animación fade_in de entrada en todas las activities que hereden
        findViewById(android.R.id.content)
                .startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.fade_in));
    }

    /** Redirige al login limpiando sesión. */
    protected void goToLogin() {
        ApiClient.clearSession(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}