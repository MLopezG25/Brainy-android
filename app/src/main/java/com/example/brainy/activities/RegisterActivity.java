package com.example.brainy.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etEmail, etPassword;
    private MaterialButton btnRegister, btnGoToLogin;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoToLogin = findViewById(R.id.btnGoToLogin);

        apiService = ApiClient.getApiService();

        // Animaciones de entrada
        etUsername.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        etEmail.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        etPassword.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        btnRegister.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_in));
        btnGoToLogin.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));

        btnRegister.setOnClickListener(v -> register());

        btnGoToLogin.setOnClickListener(v -> {
            finish();
        });
    }

    private void register() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("Registrando...");

        Map<String, String> data = new HashMap<>();
        data.put("username", username);
        data.put("email", email);
        data.put("password", password);

        apiService.register(data).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                btnRegister.setEnabled(true);
                btnRegister.setText("Registrarse");

                if (response.isSuccessful()) {
                    // Guardar sesión permanente y datos del usuario
                    SharedPreferences prefs = getSharedPreferences("brainy_prefs", MODE_PRIVATE);
                    prefs.edit()
                        .putBoolean("is_logged_in", true)
                        .putString("username", username)
                        .putString("email", email)
                        .apply();
                    ApiClient.saveSessionCookies(RegisterActivity.this);

                    Toast.makeText(RegisterActivity.this, "Cuenta creada correctamente", Toast.LENGTH_SHORT).show();
                    // Ir directamente al MainActivity
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String error = "Error al registrarse";
                    if (response.body() != null && response.body().containsKey("error")) {
                        error = response.body().get("error").toString();
                    }
                    Toast.makeText(RegisterActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (ApiClient.switchToNextUrl()) {
                    apiService = ApiClient.getApiService();
                    register();
                } else {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Registrarse");
                    Toast.makeText(RegisterActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
