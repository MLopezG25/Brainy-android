package com.example.brainy.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;

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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoToRegister = findViewById(R.id.btnGoToRegister);

        apiService = ApiClient.getApiService();

        // Animación de entrada para los elementos del login
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        etUsername.startAnimation(fadeIn);
        etPassword.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        btnLogin.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_in));
        btnGoToRegister.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));

        btnLogin.setOnClickListener(v -> login());

        btnGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                    LoginActivity.this,
                    R.anim.slide_left,
                    R.anim.fade_out
            );
            startActivity(intent, options.toBundle());
        });
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
                    Toast.makeText(LoginActivity.this, "Sesión iniciada", Toast.LENGTH_SHORT).show();
                    // Navegar al MainActivity con animación
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_left, R.anim.fade_out);
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
