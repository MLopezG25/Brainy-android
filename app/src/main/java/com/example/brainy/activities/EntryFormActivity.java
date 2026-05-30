package com.example.brainy.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.brainy.R;
import com.example.brainy.api.ApiClient;
import com.example.brainy.api.ApiService;
import com.example.brainy.api.models.Category;
import com.example.brainy.api.models.Entry;
import com.example.brainy.api.models.Subcategory;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EntryFormActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etUrl, etNotes, etCompletedDate;
    private AutoCompleteTextView autoCompleteCategory, autoCompleteSubcategory, autoCompleteStatus;
    private MaterialButton btnSave;
    private TextInputLayout layoutCompletedDate;

    private ApiService apiService;
    private List<Category> categories = new ArrayList<>();
    private List<Subcategory> subcategories = new ArrayList<>();
    private int editingEntryId = -1;

    // Datos precargados desde fetch-imdb
    private List<Map<String, Object>> pendingFieldValues = null;
    private String pendingImageUrl = null;
    private SharedPreferences preferences;
    private boolean isLoadingForEdit = false;
    // Bandera para evitar múltiples fetchs simultáneos
    private boolean isFetching = false;

    // Para saber si el usuario ha tocado algo y mostrar confirmación al salir
    private boolean formModified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_form);

        etTitle = findViewById(R.id.etTitle);
        etUrl = findViewById(R.id.etUrl);
        etNotes = findViewById(R.id.etNotes);
        autoCompleteCategory = findViewById(R.id.autoCompleteCategory);
        autoCompleteSubcategory = findViewById(R.id.autoCompleteSubcategory);
        autoCompleteStatus = findViewById(R.id.autoCompleteStatus);
        btnSave = findViewById(R.id.btnSave);
        etCompletedDate = findViewById(R.id.etCompletedDate);
        layoutCompletedDate = findViewById(R.id.layoutCompletedDate);

        apiService = ApiClient.getApiService();
        preferences = getSharedPreferences("brainy_prefs", MODE_PRIVATE);

        // Animaciones de entrada para los campos del formulario
        etTitle.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        etUrl.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        autoCompleteCategory.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        autoCompleteSubcategory.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        autoCompleteStatus.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        etNotes.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        btnSave.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_in));

        setupStatusDropdown();
        loadCategories();

        // Marcar que el formulario se ha modificado al tocar cualquier campo
        TextWatcher modifiedWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { formModified = true; }
        };
        etTitle.addTextChangedListener(modifiedWatcher);
        etNotes.addTextChangedListener(modifiedWatcher);

        // Mostrar/ocultar el campo de fecha completada según el estado seleccionado
        autoCompleteStatus.setOnItemClickListener((parent, view, position, id) -> {
            formModified = true;
            String selected = (String) parent.getItemAtPosition(position);
            if ("Completado".equals(selected) || "Abandonado".equals(selected)) {
                layoutCompletedDate.setVisibility(View.VISIBLE);
            } else {
                layoutCompletedDate.setVisibility(View.GONE);
                etCompletedDate.setText("");
            }
        });

        btnSave.setOnClickListener(v -> saveEntry());

        // Detectar cuando se pega una URL para autocompletar
        etUrl.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFetching) return;
                if (isLoadingForEdit) return;
                String url = s.toString().trim();
                if (url.isEmpty()) return;
                formModified = true;

                // Detectar URLs de IMDB: imdb.com/title/ttXXXXXX
                if (url.contains("imdb.com/title/") || url.matches(".*tt\\d{7,8}.*")) {
                    isFetching = true;
                    fetchImdbData(url);
                }
                // Detectar URLs de Discogs según tipo: discogs.com/release/, /master/ o /artist/
                else if (url.contains("discogs.com/") && (url.contains("/release/") || url.contains("/master/") || url.contains("/artist/"))) {
                    isFetching = true;
                    fetchDiscogsData(url);
                }
                // Detectar URLs de Wikipedia: wikipedia.org/wiki/
                else if (url.contains("wikipedia.org/wiki/")) {
                    isFetching = true;
                    fetchWikipediaData(url);
                }
                // Detectar URLs de libros: books.google.com, openlibrary.org o ISBN
                else if (url.contains("books.google.") || url.contains("openlibrary.org") || url.matches(".*\\b(978[0-9]{10}|979[0-9]{10}|[0-9]{9}[0-9Xx])\\b.*")) {
                    isFetching = true;
                    fetchBooksData(url);
                }
            }
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_add);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_add) {
                return true;
            } else if (itemId == R.id.nav_hub) {
                Intent intent = new Intent(EntryFormActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_timeline) {
                Intent intent = new Intent(EntryFormActivity.this, TimelineActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent intent = new Intent(EntryFormActivity.this, ProfileActivity.class);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });

        // Comprobar si venimos de un Share Intent con datos precargados
        Intent intent = getIntent();
        if (intent.hasExtra("prefill_title")) {
            String prefillTitle = intent.getStringExtra("prefill_title");
            if (prefillTitle != null) {
                etTitle.setText(prefillTitle);
            }
        }
        if (intent.hasExtra("prefill_url")) {
            String prefillUrl = intent.getStringExtra("prefill_url");
            if (prefillUrl != null) setUrlSilently(prefillUrl);
        }
        if (intent.hasExtra("prefill_notes")) {
            String prefillNotes = intent.getStringExtra("prefill_notes");
            if (prefillNotes != null) {
                etNotes.setText(prefillNotes);
            }
        }

        if (intent.hasExtra("entry_id")) {
            editingEntryId = intent.getIntExtra("entry_id", -1);
            if (editingEntryId != -1) {
                loadEntryForEdit(editingEntryId);
            }
        }
    }

    // Confirmación al ir para atrás si el formulario se ha tocado
    @Override
    public void onBackPressed() {
        if (formModified) {
            new AlertDialog.Builder(this)
                    .setTitle("Descartar cambios")
                    .setMessage("¿Seguro que quieres salir? Los cambios no se guardarán.")
                    .setPositiveButton("Salir", (dialog, which) -> EntryFormActivity.super.onBackPressed())
                    .setNegativeButton("Quedarme", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    private void fetchImdbData(String url) {
        Toast.makeText(this, "Obteniendo datos de IMDB...", Toast.LENGTH_SHORT).show();

        Map<String, String> body = new HashMap<>();
        body.put("url", url);

        // Incluir api_key de TMDB si está guardada en preferencias
        String tmdbApiKey = preferences.getString("tmdb_api_key", "");
        if (!tmdbApiKey.isEmpty()) {
            body.put("api_key", tmdbApiKey);
        }

        apiService.fetchImdbData(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                isFetching = false;
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body();

                    // Rellenar título
                    String title = (String) data.get("title");
                    if (title != null && !title.isEmpty()) {
                        etTitle.setText(title);
                    }

                    // Rellenar descripción/notas con la sinopsis
                    String overview = (String) data.get("overview");
                    if (overview != null && !overview.isEmpty()) {
                        etNotes.setText(overview);
                    }

                    // Guardar poster_path para enviarlo al guardar
                    String posterPath = (String) data.get("poster_path");
                    if (posterPath != null && !posterPath.isEmpty()) {
                        pendingImageUrl = posterPath;
                    }

                    // Guardar field_values para enviarlos al guardar
                    Object fvObj = data.get("field_values");
                    if (fvObj instanceof List) {
                        pendingFieldValues = (List<Map<String, Object>>) fvObj;
                    }

                    // Autoseleccionar categoría y subcategoría según media_type
                    String mediaType = (String) data.get("media_type");
                    if (mediaType != null) {
                        autoSelectCategoryByMediaType(mediaType);
                    }

                    Toast.makeText(EntryFormActivity.this,
                            "Datos cargados desde IMDB", Toast.LENGTH_SHORT).show();
                } else {
                    // Si falla, no pasa nada, el usuario puede escribir manualmente
                    Toast.makeText(EntryFormActivity.this,
                            "No se pudieron obtener datos automáticos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                isFetching = false;
                if (ApiClient.switchToNextUrl()) {
                    apiService = ApiClient.getApiService();
                    fetchImdbData(url);
                } else {
                    Toast.makeText(EntryFormActivity.this,
                            "Error de conexión al obtener datos", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Procesa datos comunes de fetch (título, imagen, field_values, categoría)
    private void processFetchedData(Map<String, Object> data, String titleKey, String notesKey) {
        String title = (String) data.get(titleKey);
        if (title != null && !title.isEmpty()) etTitle.setText(title);

        String notes = (String) data.get(notesKey);
        if (notes != null && !notes.isEmpty()) etNotes.setText(notes);

        String imageUrl = (String) data.get("image_url");
        if (imageUrl != null && !imageUrl.isEmpty()) pendingImageUrl = imageUrl;

        Object fvObj = data.get("field_values");
        if (fvObj instanceof List) pendingFieldValues = (List<Map<String, Object>>) fvObj;

        String categoryName = (String) data.get("category_name");
        String subcategoryName = (String) data.get("subcategory_name");
        if (categoryName != null && subcategoryName != null) {
            autoSelectCategoryAndSubcategory(categoryName, subcategoryName);
        }
    }

    private void fetchDiscogsData(String url) {
        Toast.makeText(this, "Obteniendo datos de Discogs...", Toast.LENGTH_SHORT).show();
        Map<String, String> body = new HashMap<>();
        body.put("url", url);
        String token = preferences.getString("discogs_token", "");
        if (!token.isEmpty()) body.put("discogs_token", token);

        apiService.fetchDiscogsData(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                isFetching = false;
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body();
                    if (data.containsKey("error")) {
                        Toast.makeText(EntryFormActivity.this, "Error Discogs: " + data.get("error"), Toast.LENGTH_LONG).show();
                        return;
                    }
                    processFetchedData(data, "title", null);
                    Toast.makeText(EntryFormActivity.this, "Datos cargados desde Discogs", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(EntryFormActivity.this, "No se pudieron obtener datos automáticos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                isFetching = false;
                if (ApiClient.switchToNextUrl()) {
                    apiService = ApiClient.getApiService();
                } else {
                    Toast.makeText(EntryFormActivity.this, "Error de conexión al obtener datos", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void fetchWikipediaData(String url) {
        Toast.makeText(this, "Obteniendo datos de Wikipedia...", Toast.LENGTH_SHORT).show();
        Map<String, String> body = new HashMap<>();
        body.put("url", url);

        apiService.fetchWikipediaData(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                isFetching = false;
                if (response.isSuccessful() && response.body() != null) {
                    processFetchedData(response.body(), "page_title", "extract");
                    Toast.makeText(EntryFormActivity.this, "Datos cargados desde Wikipedia", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(EntryFormActivity.this, "No se pudieron obtener datos automáticos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                isFetching = false;
                if (ApiClient.switchToNextUrl()) {
                    apiService = ApiClient.getApiService();
                } else {
                    Toast.makeText(EntryFormActivity.this, "Error de conexión al obtener datos", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void fetchBooksData(String url) {
        Toast.makeText(this, "Obteniendo datos del libro...", Toast.LENGTH_SHORT).show();
        Map<String, String> body = new HashMap<>();
        body.put("url", url);
        String apiKey = preferences.getString("google_books_api_key", "");
        if (!apiKey.isEmpty()) body.put("api_key", apiKey);

        apiService.fetchBooksData(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                isFetching = false;
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body();
                    if (data.containsKey("error")) {
                        Toast.makeText(EntryFormActivity.this, "Error: " + data.get("error"), Toast.LENGTH_LONG).show();
                        return;
                    }
                    processFetchedData(data, "title", "description");
                    Toast.makeText(EntryFormActivity.this, "Datos cargados desde Google Books", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(EntryFormActivity.this, "No se pudieron obtener datos automáticos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                isFetching = false;
                if (ApiClient.switchToNextUrl()) {
                    apiService = ApiClient.getApiService();
                } else {
                    Toast.makeText(EntryFormActivity.this, "Error de conexión al obtener datos", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void autoSelectCategoryByMediaType(String mediaType) {
        // Buscar la categoría Cine
        for (Category cat : categories) {
            if (cat.getName().equalsIgnoreCase("Cine")) {
                autoCompleteCategory.setText(cat.getName(), false);
                // Cargar subcategorías de Cine
                String targetSubcat = "movie".equals(mediaType) ? "Película" : "Serie";
                loadSubcategoriesAndSelectByName(cat.getId(), targetSubcat);
                return;
            }
        }
        // Si no se encontró, el usuario puede seleccionar manualmente
    }

    private void autoSelectCategoryAndSubcategory(String categoryName, String subcategoryName) {
        // Buscar la categoría por nombre
        for (Category cat : categories) {
            if (cat.getName().equalsIgnoreCase(categoryName)) {
                autoCompleteCategory.setText(cat.getName(), false);
                // Cargar subcategorías y seleccionar la que coincida
                loadSubcategoriesAndSelectByName(cat.getId(), subcategoryName);
                return;
            }
        }
        // Si no se encontró, el usuario puede seleccionar manualmente
    }

    private void loadSubcategoriesAndSelectByName(int categoryId, String targetSubcategoryName) {
        apiService.getSubcategories(categoryId).enqueue(new Callback<List<Subcategory>>() {
            @Override
            public void onResponse(Call<List<Subcategory>> call, Response<List<Subcategory>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    subcategories = response.body();
                    List<String> subcategoryNames = new ArrayList<>();
                    for (Subcategory sub : subcategories) {
                        subcategoryNames.add(sub.getName());
                    }

                    ArrayAdapter<String> subcategoryAdapter = new ArrayAdapter<>(EntryFormActivity.this,
                            android.R.layout.simple_dropdown_item_1line, subcategoryNames);
                    autoCompleteSubcategory.setAdapter(subcategoryAdapter);

                    // Seleccionar la subcategoría que coincida
                    for (Subcategory sub : subcategories) {
                        if (sub.getName().equalsIgnoreCase(targetSubcategoryName)) {
                            autoCompleteSubcategory.setText(sub.getName(), false);
                            break;
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Subcategory>> call, Throwable t) {
                // Si falla, ell usuario puede seleccionar manualmente
            }
        });
    }

    private void setupStatusDropdown() {
        String[] statuses = {"Pendiente", "En progreso", "Completado", "Abandonado"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, statuses);
        autoCompleteStatus.setAdapter(adapter);
    }

    // Convierte texto de estado a su clave interna
    private String statusToKey(String text) {
        switch (text) {
            case "Pendiente": return "pendiente";
            case "En progreso": return "en_progreso";
            case "Completado": return "completado";
            case "Abandonado": return "abandonado";
            default: return null;
        }
    }

    /** Convierte clave interna a texto visible */
    private String keyToStatusDisplay(String key) {
        switch (key) {
            case "pendiente": return "Pendiente";
            case "en_progreso": return "En progreso";
            case "completado": return "Completado";
            case "abandonado": return "Abandonado";
            default: return "Pendiente";
        }
    }

    // Establece la URL sin disparar el TextWatcher 
    private void setUrlSilently(String url) {
        isLoadingForEdit = true;
        etUrl.setText(url);
        isLoadingForEdit = false;
    }

    private void loadCategories() {
        apiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categories = response.body();
                    List<String> categoryNames = new ArrayList<>();
                    for (Category cat : categories) {
                        categoryNames.add(cat.getName());
                    }

                    ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(EntryFormActivity.this,
                            android.R.layout.simple_dropdown_item_1line, categoryNames);
                    autoCompleteCategory.setAdapter(categoryAdapter);

                    autoCompleteCategory.setOnItemClickListener((parent, view, position, id) -> {
                        formModified = true;
                        loadSubcategories(categories.get(position).getId());
                    });
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                if (ApiClient.switchToNextUrl()) {
                    apiService = ApiClient.getApiService();
                    loadCategories();
                } else {
                    Toast.makeText(EntryFormActivity.this, "Error al cargar categorías", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadSubcategories(int categoryId) {
        apiService.getSubcategories(categoryId).enqueue(new Callback<List<Subcategory>>() {
            @Override
            public void onResponse(Call<List<Subcategory>> call, Response<List<Subcategory>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    subcategories = response.body();
                    List<String> subcategoryNames = new ArrayList<>();
                    for (Subcategory sub : subcategories) {
                        subcategoryNames.add(sub.getName());
                    }

                    ArrayAdapter<String> subcategoryAdapter = new ArrayAdapter<>(EntryFormActivity.this,
                            android.R.layout.simple_dropdown_item_1line, subcategoryNames);
                    autoCompleteSubcategory.setAdapter(subcategoryAdapter);
                }
            }

            @Override
            public void onFailure(Call<List<Subcategory>> call, Throwable t) {
                if (ApiClient.switchToNextUrl()) {
                    apiService = ApiClient.getApiService();
                    loadSubcategories(categoryId);
                } else {
                    Toast.makeText(EntryFormActivity.this, "Error al cargar subcategorías", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveEntry() {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "El título es obligatorio", Toast.LENGTH_SHORT).show();
            return;
        }

        String categoryName = autoCompleteCategory.getText().toString();
        String subcategoryName = autoCompleteSubcategory.getText().toString();
        String statusName = autoCompleteStatus.getText().toString();

        Integer categoryId = null;
        for (Category cat : categories) {
            if (cat.getName().equals(categoryName)) {
                categoryId = cat.getId();
                break;
            }
        }

        Integer subcategoryId = null;
        for (Subcategory sub : subcategories) {
            if (sub.getName().equals(subcategoryName)) {
                subcategoryId = sub.getId();
                break;
            }
        }

        String statusValue = statusToKey(statusName);
        if (statusValue == null) statusValue = "pendiente";

        String url = etUrl.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        // Combinar URL y notas en el campo description
        StringBuilder descriptionBuilder = new StringBuilder();
        if (!url.isEmpty()) {
            descriptionBuilder.append("URL: ").append(url);
        }
        if (!notes.isEmpty()) {
            if (descriptionBuilder.length() > 0) {
                descriptionBuilder.append("\n\n");
            }
            descriptionBuilder.append(notes);
        }

        String description = descriptionBuilder.length() > 0 ? descriptionBuilder.toString() : null;

        Map<String, Object> entryData = new HashMap<>();
        entryData.put("title", title);
        if (categoryId != null) entryData.put("category", categoryId);
        if (subcategoryId != null) entryData.put("subcategory", subcategoryId);
        entryData.put("status", statusValue);
        if (description != null) entryData.put("description", description);

        // Incluir image_url si hay pendiente del fetch
        if (pendingImageUrl != null && !pendingImageUrl.isEmpty()) {
            entryData.put("image_url", pendingImageUrl);
        }

        // Incluir field_values si hay pendientes del fetch
        if (pendingFieldValues != null && !pendingFieldValues.isEmpty()) {
            entryData.put("field_values", pendingFieldValues);
        }

        // Incluir completed_date si el campo está visible y relleno
        if (layoutCompletedDate.getVisibility() == View.VISIBLE) {
            String completedDate = etCompletedDate.getText().toString().trim();
            if (!completedDate.isEmpty()) {
                entryData.put("completed_date", completedDate);
            }
        }

        Call<Entry> call;
        if (editingEntryId != -1) {
            call = apiService.updateEntry(editingEntryId, entryData);
        } else {
            call = apiService.createEntry(entryData);
        }

        call.enqueue(new Callback<Entry>() {
            @Override
            public void onResponse(Call<Entry> call, Response<Entry> response) {
                if (response.code() == 401 || response.code() == 403) {
                    goToLogin(); return;
                }
                if (response.isSuccessful()) {
                    Toast.makeText(EntryFormActivity.this, "Entrada guardada", Toast.LENGTH_SHORT).show();
                    formModified = false;
                    finish();
                } else {
                    Toast.makeText(EntryFormActivity.this, "Error al guardar", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Entry> call, Throwable t) {
                if (ApiClient.switchToNextUrl()) {
                    apiService = ApiClient.getApiService();
                    saveEntry();
                } else {
                    Toast.makeText(EntryFormActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void goToLogin() {
        ApiClient.clearSession(this);
        Intent intent = new Intent(EntryFormActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadEntryForEdit(int entryId) {
        apiService.getEntry(entryId).enqueue(new Callback<Entry>() {
            @Override
            public void onResponse(Call<Entry> call, Response<Entry> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Entry entry = response.body();
                    etTitle.setText(entry.getTitle());

                    if (entry.getCategoryName() != null) {
                        autoCompleteCategory.setText(entry.getCategoryName(), false);
                        // no se dispara el listener
                        // cargamos las subcategorias a mano
                        for (Category cat : categories) {
                            if (cat.getName().equals(entry.getCategoryName())) {
                                if (entry.getSubcategoryName() != null) {
                                    loadSubcategoriesAndSelectByName(cat.getId(), entry.getSubcategoryName());
                                } else {
                                    loadSubcategories(cat.getId());
                                }
                                break;
                            }
                        }
                    }

                    if (entry.getStatus() != null) {
                        String displayStatus = keyToStatusDisplay(entry.getStatus());
                        autoCompleteStatus.setText(displayStatus, false);

                        // Si el estado es completado o abandonado, mostrar el campo de fecha
                        if ("completado".equals(entry.getStatus()) || "abandonado".equals(entry.getStatus())) {
                            layoutCompletedDate.setVisibility(View.VISIBLE);
                            // Cargar completed_date si existe
                            if (entry.getCompletedDate() != null && !entry.getCompletedDate().isEmpty()) {
                                etCompletedDate.setText(entry.getCompletedDate());
                            }
                        }
                    }

                    if (entry.getDescription() != null) {
                        String desc = entry.getDescription();
                        // Si la description contiene una URL con nuestro formato, se separa
                        if (desc.startsWith("URL: ")) {
                            int endOfUrl = desc.indexOf("\n\n");
                            String urlPart = endOfUrl != -1 ? desc.substring(5, endOfUrl) : desc.substring(5);
                            String notesPart = endOfUrl != -1 ? desc.substring(endOfUrl + 2) : "";
                            setUrlSilently(urlPart);
                            if (!notesPart.isEmpty()) etNotes.setText(notesPart);
                        } else {
                            etNotes.setText(desc);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<Entry> call, Throwable t) {
                if (ApiClient.switchToNextUrl()) {
                    apiService = ApiClient.getApiService();
                    loadEntryForEdit(entryId);
                } else {
                    Toast.makeText(EntryFormActivity.this, "Error al cargar la entrada", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}