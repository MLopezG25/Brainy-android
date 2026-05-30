package com.example.brainy.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.brainy.R;
import com.example.brainy.api.ApiClient;
import com.example.brainy.api.ApiService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etPassword;
    private MaterialButton btnLogin, btnGoToRegister;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Aplicar modo oscuro antes de inflar la vista
        SharedPreferences prefs = getSharedPreferences("brainy_prefs", MODE_PRIVATE);
        if (prefs.getBoolean("dark_mode", false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);

        // Si ya hay sesión guardada, restaurar cookies e ir al MainActivity
        if (prefs.getBoolean("is_logged_in", false)) {
            // Inicializar con la IP guardada (no la por defecto)
            ApiClient.init(this);
            // Restaurar las cookies de sesión
            ApiClient.restoreSessionCookies(this);
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        // Inicializar ApiClient con la IP guardada en preferencias
        ApiClient.init(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoToRegister = findViewById(R.id.btnGoToRegister);

        apiService = ApiClient.getApiService();

        // Animar elementos al entrar con efecto escalonado y rebote
        animateEntry();

        btnLogin.setOnClickListener(v -> login());

        btnGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void animateEntry() {
        OvershootInterpolator overshoot = new OvershootInterpolator(1.5f);

        // Usuario: fade + slide desde la izquierda
        etUsername.setAlpha(0f);
        etUsername.setTranslationX(-80f);
        etUsername.animate().alpha(1f).translationX(0f).setDuration(500)
                .setInterpolator(overshoot).setStartDelay(200).start();

        // Contraseña: fade + slide desde la izquierda
        etPassword.setAlpha(0f);
        etPassword.setTranslationX(-80f);
        etPassword.animate().alpha(1f).translationX(0f).setDuration(500)
                .setInterpolator(overshoot).setStartDelay(350).start();

        // Botón login: fade + rebote
        btnLogin.setAlpha(0f);
        btnLogin.setScaleX(0.5f);
        btnLogin.setScaleY(0.5f);
        btnLogin.animate().alpha(1f).scaleX(1.1f).scaleY(1.1f).setDuration(400)
                .setInterpolator(overshoot).setStartDelay(500)
                .withEndAction(() -> btnLogin.animate().scaleX(1f).scaleY(1f).setDuration(200).start());

        // Botón registro: fade + slide desde abajo
        btnGoToRegister.setAlpha(0f);
        btnGoToRegister.setTranslationY(40f);
        btnGoToRegister.animate().alpha(1f).translationY(0f).setDuration(400)
                .setStartDelay(650).start();
    }

    private void login() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Iniciando sesión...");

        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", username);
        credentials.put("password", password);

        apiService.login(credentials).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Iniciar sesión");

                if (response.isSuccessful()) {
                    // Guardar sesión permanente
                    SharedPreferences prefs = getSharedPreferences("brainy_prefs", MODE_PRIVATE);
                    prefs.edit().putBoolean("is_logged_in", true).apply();
                    ApiClient.saveSessionCookies(LoginActivity.this);

                    Toast.makeText(LoginActivity.this, "Sesión iniciada", Toast.LENGTH_SHORT).show();
                    // Navegar al MainActivity
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (ApiClient.switchToNextUrl()) {
                    apiService = ApiClient.getApiService();
                    login();
                } else {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Iniciar sesión");
                    Toast.makeText(LoginActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
