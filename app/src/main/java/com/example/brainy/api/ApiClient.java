package com.example.brainy.api;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.concurrent.TimeUnit;

import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    // Lista de posibles IPs del backend (casa, trabajo, ADB Reverse)
    // La app probará cada una hasta encontrar la que funcione
    private static final String[] POSIBLE_URLS = {
            "http://192.168.1.57:8000/",   // Casa (WiFi)
            "http://10.89.255.149:8000/",  // Trabajo (WiFi)
            "http://10.127.200.164:8000/", // Trabajo (Ethernet)
            "http://127.0.0.1:8000/",      // ADB Reverse (USB)
    };

    private static Retrofit retrofit = null;
    private static OkHttpClient httpClient = null;
    private static String activeBaseUrl = null;
    private static int currentUrlIndex = 0;

    public static Retrofit getClient() {
        if (retrofit == null) {
            activeBaseUrl = POSIBLE_URLS[currentUrlIndex];
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
            CookieManager cookieManager = new CookieManager();
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            httpClient = new OkHttpClient.Builder()
                    .cookieJar(new JavaNetCookieJar(cookieManager))
                    .addInterceptor(logging)
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        }
        return httpClient;
    }

    public static ApiService getApiService() {
        return getClient().create(ApiService.class);
    }

    /**
     * Cambia a la siguiente IP disponible cuando falla la conexión.
     * Se llama automáticamente desde los callbacks onFailure.
     * @return true si hay más IPs para probar, false si ya no quedan
     */
    public static synchronized boolean switchToNextUrl() {
        currentUrlIndex++;
        if (currentUrlIndex < POSIBLE_URLS.length) {
            activeBaseUrl = POSIBLE_URLS[currentUrlIndex];
            retrofit = buildRetrofit(activeBaseUrl);
            android.util.Log.d("API_CLIENT", "Cambiando a: " + activeBaseUrl);
            return true;
        }
        // Volver al inicio para el próximo intento
        currentUrlIndex = 0;
        activeBaseUrl = POSIBLE_URLS[0];
        retrofit = buildRetrofit(activeBaseUrl);
        android.util.Log.d("API_CLIENT", "No hay más IPs, volviendo a: " + activeBaseUrl);
        return false;
    }

    /**
     * Reinicia el índice de IPs para empezar desde la primera
     */
    public static void resetUrlIndex() {
        currentUrlIndex = 0;
        activeBaseUrl = POSIBLE_URLS[0];
        retrofit = buildRetrofit(activeBaseUrl);
    }

    public static String getActiveBaseUrl() {
        return activeBaseUrl;
    }
}
