package com.example.brainy.utils;

import android.content.Context;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brainy.activities.LoginActivity;
import com.example.brainy.api.ApiClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Callback genérico para peticiones Retrofit.
 * Unifica el manejo de:
 * - Respuestas exitosas (onSuccess)
 * - Errores 401/403 (redirige a login)
 * - Failover automático de URLs (reintenta con la siguiente IP)
 * - Errores genéricos de red (onNetworkError)
 */
public abstract class RetrofitCallback<T> implements Callback<T> {

    private final AppCompatActivity activity;
    private final String retryMethodName;

    public RetrofitCallback(AppCompatActivity activity, String retryMethodName) {
        this.activity = activity;
        this.retryMethodName = retryMethodName;
    }

    @Override
    public void onResponse(Call<T> call, Response<T> response) {
        int code = response.code();
        if (code == 401 || code == 403) {
            goToLogin();
            return;
        }
        if (response.isSuccessful() && response.body() != null) {
            onSuccess(response.body());
        } else {
            onError(code, "Error del servidor");
        }
    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {
        if (ApiClient.switchToNextUrl()) {
            retry();
        } else {
            onNetworkError(t.getMessage());
        }
    }

    /** Se ejecuta cuando la petición es exitosa (2xx). */
    public abstract void onSuccess(T data);

    /** Se ejecuta cuando el código de error no es 401/403 ni de red. */
    public void onError(int code, String message) {
        // Por defecto no hace nada, las subclases pueden sobrescribir
    }

    /** Se ejecuta cuando falla la red y se agotaron las IPs de failover. */
    public void onNetworkError(String message) {
        // Por defecto no hace nada, las subclases pueden sobrescribir
    }

    /** Redirige al login y limpia la sesión. */
    protected void goToLogin() {
        ApiClient.clearSession(activity);
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    /** Reintenta la petición después de cambiar de URL. */
    protected abstract void retry();
}