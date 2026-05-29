package com.example.brainy.api;

import android.content.Context;
import android.content.SharedPreferences;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.content.Intent;

import okhttp3.Interceptor;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// Con solución externa que encontré para problemas de testing de distintas IPs

public class ApiClient {

    // URL por defecto (emulador conecta al host)
    private static final String DEFAULT_URL = "http://10.0.2.2:8000/";

    // IPs de respaldo por si la principal no funciona
    private static final String[] FALLBACK_URLS = {
            "http://10.0.2.2:8000/",
            "http://127.0.0.1:8000/",
            "http://192.168.1.53:8000/",
            "http://192.168.1.63:8000/",
    };

    private static Retrofit retrofit = null;
    private static OkHttpClient httpClient = null;
    private static String activeBaseUrl = null;
    private static CookieManager cookieManager = null;
    private static int currentFallbackIndex = -1;
    private static Context appContext = null;

    public static void init(Context context) {
        appContext = context.getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("brainy_prefs", Context.MODE_PRIVATE);
        String savedUrl = prefs.getString("server_url", DEFAULT_URL);
        if (!savedUrl.endsWith("/")) savedUrl += "/";
        activeBaseUrl = savedUrl;
        retrofit = buildRetrofit(activeBaseUrl);
    }

    public static void setCustomBaseUrl(Context context, String newUrl) {
        if (!newUrl.endsWith("/")) newUrl += "/";
        SharedPreferences prefs = context.getSharedPreferences("brainy_prefs", Context.MODE_PRIVATE);
        prefs.edit().putString("server_url", newUrl).apply();
        activeBaseUrl = newUrl;
        retrofit = buildRetrofit(activeBaseUrl);
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            activeBaseUrl = DEFAULT_URL;
            retrofit = buildRetrofit(activeBaseUrl);
        }
        return retrofit;
    }

    private static Retrofit buildRetrofit(String baseUrl) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(getHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static OkHttpClient getHttpClient() {
        if (httpClient == null) {
            cookieManager = new CookieManager();
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Interceptor para detectar 401 y limpiar sesión caducada
            Interceptor authInterceptor = chain -> {
                okhttp3.Request request = chain.request();
                Response response = chain.proceed(request);
                if (response.code() == 401 && appContext != null) {
                    clearSession(appContext);
                }
                return response;
            };

            httpClient = new OkHttpClient.Builder()
                    .cookieJar(new JavaNetCookieJar(cookieManager))
                    .addInterceptor(authInterceptor)
                    .addInterceptor(logging)
                    .connectTimeout(3, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        }
        return httpClient;
    }

    public static void saveSessionCookies(Context context) {
        if (cookieManager == null) return;
        List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
        StringBuilder sb = new StringBuilder();
        for (HttpCookie cookie : cookies) {
            if ("sessionid".equals(cookie.getName()) || "csrftoken".equals(cookie.getName())) {
                if (sb.length() > 0) sb.append("; ");
                sb.append(cookie.getName()).append("=").append(cookie.getValue());
            }
        }
        if (sb.length() > 0) {
            SharedPreferences prefs = context.getSharedPreferences("brainy_prefs", Context.MODE_PRIVATE);
            prefs.edit().putString("session_cookies", sb.toString()).apply();
        }
    }

    public static void restoreSessionCookies(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("brainy_prefs", Context.MODE_PRIVATE);
        String cookiesStr = prefs.getString("session_cookies", "");
        if (cookiesStr.isEmpty()) return;

        getHttpClient();

        String[] cookiePairs = cookiesStr.split("; ");
        for (String pair : cookiePairs) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                HttpCookie cookie = new HttpCookie(parts[0], parts[1]);
                cookie.setPath("/");
                cookie.setVersion(0);
                if (activeBaseUrl != null) {
                    try { cookieManager.getCookieStore().add(URI.create(activeBaseUrl), cookie); } catch (Exception ignored) {}
                }
                for (String url : FALLBACK_URLS) {
                    try { cookieManager.getCookieStore().add(URI.create(url), cookie); } catch (Exception ignored) {}
                }
            }
        }
    }

    public static ApiService getApiService() {
        return getClient().create(ApiService.class);
    }

    // Prueba la siguiente IP de respaldo cuando falla la conexion
    public static synchronized boolean switchToNextUrl() {
        currentFallbackIndex++;
        if (currentFallbackIndex < FALLBACK_URLS.length) {
            String oldUrl = activeBaseUrl;
            activeBaseUrl = FALLBACK_URLS[currentFallbackIndex];
            retrofit = buildRetrofit(activeBaseUrl);

            if (cookieManager != null && oldUrl != null) {
                try {
                    List<HttpCookie> oldCookies = cookieManager.getCookieStore().get(URI.create(oldUrl));
                    for (HttpCookie c : oldCookies) {
                        cookieManager.getCookieStore().add(URI.create(activeBaseUrl), c);
                    }
                } catch (Exception ignored) {}
            }
            return true;
        }
        return false;
    }

    // Vuelve a la primera URL
    public static void resetUrlIndex() {
        currentFallbackIndex = -1;
        activeBaseUrl = DEFAULT_URL;
        retrofit = buildRetrofit(activeBaseUrl);
    }

    public static String getActiveBaseUrl() {
        return activeBaseUrl;
    }

    // Limpia la sesión guardada (cuando el servidor devuelve 401).
    public static void clearSession(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("brainy_prefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean("is_logged_in", false)
                .putString("session_cookies", "")
                .apply();
        if (cookieManager != null) {
            cookieManager.getCookieStore().removeAll();
        }
    }
}
