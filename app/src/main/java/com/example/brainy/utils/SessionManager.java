package com.example.brainy.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Centraliza toda la gestión de SharedPreferences:
 * - Estado de login (is_logged_in)
 * - Datos del usuario (username, email)
 * - Cookies de sesión (sessionid, csrftoken)
 * - Configuración (server_url, grid_columns, dark_mode)
 * - API keys (tmdb_api_key, discogs_token, google_books_api_key)
 */
public final class SessionManager {

    private static final String PREFS_NAME = "brainy_prefs";
    private static SessionManager instance;
    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    // ─── Login / User ──────────────────────────────────────────

    public boolean isLoggedIn() {
        return prefs.getBoolean("is_logged_in", false);
    }

    public void setLoggedIn(boolean value) {
        prefs.edit().putBoolean("is_logged_in", value).apply();
    }

    public String getUsername() {
        return prefs.getString("username", "");
    }

    public void setUsername(String value) {
        prefs.edit().putString("username", value).apply();
    }

    public String getEmail() {
        return prefs.getString("email", "");
    }

    public void setEmail(String value) {
        prefs.edit().putString("email", value).apply();
    }

    // ─── Cookies ───────────────────────────────────────────────

    public String getSessionCookies() {
        return prefs.getString("session_cookies", "");
    }

    public void setSessionCookies(String value) {
        prefs.edit().putString("session_cookies", value).apply();
    }

    // ─── Server ────────────────────────────────────────────────

    public String getServerUrl() {
        return prefs.getString("server_url", "http://10.0.2.2:8000/");
    }

    public void setServerUrl(String value) {
        prefs.edit().putString("server_url", value).apply();
    }

    // ─── Grid ──────────────────────────────────────────────────

    public int getGridColumns() {
        return prefs.getInt("grid_columns", 3);
    }

    public void setGridColumns(int value) {
        prefs.edit().putInt("grid_columns", value).apply();
    }

    // ─── Theme ─────────────────────────────────────────────────

    public boolean isDarkMode() {
        return prefs.getBoolean("dark_mode", false);
    }

    public void setDarkMode(boolean value) {
        prefs.edit().putBoolean("dark_mode", value).apply();
    }

    // ─── API Keys ──────────────────────────────────────────────

    public String getTmdbApiKey() {
        return prefs.getString("tmdb_api_key", "");
    }

    public void setTmdbApiKey(String value) {
        prefs.edit().putString("tmdb_api_key", value).apply();
    }

    public String getDiscogsToken() {
        return prefs.getString("discogs_token", "");
    }

    public void setDiscogsToken(String value) {
        prefs.edit().putString("discogs_token", value).apply();
    }

    public String getGoogleBooksApiKey() {
        return prefs.getString("google_books_api_key", "");
    }

    public void setGoogleBooksApiKey(String value) {
        prefs.edit().putString("google_books_api_key", value).apply();
    }

    // ─── Clear ─────────────────────────────────────────────────

    public void clearSession() {
        prefs.edit()
                .putBoolean("is_logged_in", false)
                .putString("session_cookies", "")
                .putString("username", "")
                .putString("email", "")
                .apply();
    }

    public void clearAll() {
        prefs.edit().clear().apply();
    }
}