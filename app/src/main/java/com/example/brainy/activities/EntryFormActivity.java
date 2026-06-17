package com.example.brainy.activities;

import android.content.Intent;
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
import com.example.brainy.utils.SessionManager;
import com.example.brainy.utils.StatusUtils;
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
    private SessionManager session;
    private List<Category> categories = new ArrayList<>();
    private List<Subcategory> subcategories = new ArrayList<>();
    private int editingEntryId = -1;

    private List<Map<String, Object>> pendingFieldValues = null;
    private String pendingImageUrl = null;
    private boolean isLoadingForEdit = false;
    private boolean isFetching = false;
    private boolean formModified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_form);

        session = SessionManager.getInstance(this);

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

        // Animaciones de entrada
        etTitle.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        etUrl.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        autoCompleteCategory.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        autoCompleteSubcategory.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        autoCompleteStatus.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        etNotes.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        btnSave.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_in));

        setupStatusDropdown();
        loadCategories();

        TextWatcher modifiedWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { formModified = true; }
        };
        etTitle.addTextChangedListener(modifiedWatcher);
        etNotes.addTextChangedListener(modifiedWatcher);

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

        // Detectar URL pegada para autocompletar
        etUrl.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isFetching || isLoadingForEdit) return;
                String url = s.toString().trim();
                if (url.isEmpty()) return;
                formModified = true;
                dispatchFetch(url);
            }
        });

        setupBottomNav();

        // Precarga desde Share Intent
        Intent intent = getIntent();
        if (intent.hasExtra("prefill_title")) {
            String t = intent.getStringExtra("prefill_title");
            if (t != null) etTitle.setText(t);
        }
        if (intent.hasExtra("prefill_url")) {
            String u = intent.getStringExtra("prefill_url");
            if (u != null) setUrlSilently(u);
        }
        if (intent.hasExtra("prefill_notes")) {
            String n = intent.getStringExtra("prefill_notes");
            if (n != null) etNotes.setText(n);
        }

        if (intent.hasExtra("entry_id")) {
            editingEntryId = intent.getIntExtra("entry_id", -1);
            if (editingEntryId != -1) loadEntryForEdit(editingEntryId);
        }
    }

    // ─── Fetch unificado ────────────────────────────────────────

    /** Detecta el tipo de URL y dispara el fetch correspondiente. */
    private void dispatchFetch(String url) {
        isFetching = true;
        if (url.contains("imdb.com/title/") || url.matches(".*tt\\d{7,8}.*")) {
            fetchFromApi(url, "IMDB", "tmdb_api_key", "api_key",
                    apiService::fetchImdbData,
                    this::processImdbResponse);
        } else if (url.contains("discogs.com/") && (url.contains("/release/") || url.contains("/master/") || url.contains("/artist/"))) {
            fetchFromApi(url, "Discogs", "discogs_token", "discogs_token",
                    apiService::fetchDiscogsData,
                    data -> processFetchedData(data, "title", null));
        } else if (url.contains("wikipedia.org/wiki/")) {
            fetchFromApi(url, "Wikipedia", null, null,
                    apiService::fetchWikipediaData,
                    data -> processFetchedData(data, "page_title", "extract"));
        } else if (url.contains("books.google.") || url.contains("openlibrary.org") || url.matches(".*\\b(978[0-9]{10}|979[0-9]{10}|[0-9]{9}[0-9Xx])\\b.*")) {
            fetchFromApi(url, "Google Books", "google_books_api_key", "api_key",
                    apiService::fetchBooksData,
                    data -> processFetchedData(data, "title", "description"));
        } else {
            isFetching = false;
        }
    }

    @FunctionalInterface
    private interface FetchCall {
        Call<Map<String, Object>> call(Map<String, String> body);
    }

    @FunctionalInterface
    private interface FetchProcessor {
        void process(Map<String, Object> data);
    }

    /** Método genérico que unifica los 4 fetch. */
    private void fetchFromApi(String url, String label,
                              String prefsKey, String bodyKey,
                              FetchCall fetchCall, FetchProcessor processor) {
        Toast.makeText(this, "Obteniendo datos de " + label + "...", Toast.LENGTH_SHORT).show();
        Map<String, String> body = new HashMap<>();
        body.put("url", url);
        if (prefsKey != null) {
            String credential = session.getSharedPreferences("brainy_prefs", MODE_PRIVATE).getString(prefsKey, "");
            if (!credential.isEmpty()) body.put(bodyKey, credential);
        }

        fetchCall.call(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                isFetching = false;
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body();
                    if (data.containsKey("error")) {
                        Toast.makeText(EntryFormActivity.this, "Error: " + data.get("error"), Toast.LENGTH_LONG).show();
                        return;
                    }
                    processor.process(data);
                    Toast.makeText(EntryFormActivity.this, "Datos cargados desde " + label, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(EntryFormActivity.this, "No se pudieron obtener datos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                isFetching = false;
                if (ApiClient.switchToNextUrl()) {
                    apiService = ApiClient.getApiService();
                    fetchFromApi(url, label, prefsKey, bodyKey, fetchCall, processor);
                } else {
                    Toast.makeText(EntryFormActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /** Procesa la respuesta de IMDB (especial por media_type). */
    private void processImdbResponse(Map<String, Object> data) {
        String title = (String) data.get("title");
        if (title != null && !title.isEmpty()) etTitle.setText(title);

        String overview = (String) data.get("overview");
        if (overview != null && !overview.isEmpty()) etNotes.setText(overview);

        String posterPath = (String) data.get("poster_path");
        if (posterPath != null && !posterPath.isEmpty()) pendingImageUrl = posterPath;

        Object fvObj = data.get("field_values");
        if (fvObj instanceof List) pendingFieldValues = (List<Map<String, Object>>) fvObj;

        String mediaType = (String) data.get("media_type");
        if (mediaType != null) autoSelectCategoryByMediaType(mediaType);
    }

    /** Procesa datos comunes de fetch (título, imagen, field_values, categoría). */
    private void processFetchedData(Map<String, Object> data, String titleKey, String notesKey) {
        if (titleKey != null) {
            String title = (String) data.get(titleKey);
            if (title != null && !title.isEmpty()) etTitle.setText(title);
        }
        if (notesKey != null) {
            String notes = (String) data.get(notesKey);
            if (notes != null && !notes.isEmpty()) etNotes.setText(notes);
        }

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

    // ─── Autoselección de categoría ─────────────────────────────

    private void autoSelectCategoryByMediaType(String mediaType) {
        for (Category cat : categories) {
            if (cat.getName().equalsIgnoreCase("Cine")) {
                autoCompleteCategory.setText(cat.getName(), false);
                String targetSubcat = "movie".equals(mediaType) ? "Película" : "Serie";
                loadSubcategoriesAndSelectByName(cat.getId(), targetSubcat);
                return;
            }
        }
    }

    private void autoSelectCategoryAndSubcategory(String categoryName, String subcategoryName) {
        for (Category cat : categories) {
            if (cat.getName().equalsIgnoreCase(categoryName)) {
                autoCompleteCategory.setText(cat.getName(), false);
                loadSubcategoriesAndSelectByName(cat.getId(), subcategoryName);
                return;
            }
        }
    }

    private void loadSubcategoriesAndSelectByName(int categoryId, String targetSubcategoryName) {
        apiService.getSubcategories(categoryId).enqueue(new Callback<List<Subcategory>>() {
            @Override
            public void onResponse(Call<List<Subcategory>> call, Response<List<Subcategory>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    subcategories = response.body();
                    List<String> names = new ArrayList<>();
                    for (Subcategory sub : subcategories) names.add(sub.getName());
                    autoCompleteSubcategory.setAdapter(new ArrayAdapter<>(EntryFormActivity.this,
                            android.R.layout.simple_dropdown_item_1line, names));
                    for (Subcategory sub : subcategories) {
                        if (sub.getName().equalsIgnoreCase(targetSubcategoryName)) {
                            autoCompleteSubcategory.setText(sub.getName(), false);
                            break;
                        }
                    }
                }
            }
            @Override
            public void onFailure(Call<List<Subcategory>> call, Throwable t) {}
        });
    }

    // ─── Setup UI ───────────────────────────────────────────────

    private void setupStatusDropdown() {
        autoCompleteStatus.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, StatusUtils.getFormLabels()));
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_add);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_add) return true;
            Intent intent = null;
            if (itemId == R.id.nav_hub) intent = new Intent(this, MainActivity.class);
            else if (itemId == R.id.nav_timeline) intent = new Intent(this, TimelineActivity.class);
            else if (itemId == R.id.nav_profile) intent = new Intent(this, ProfileActivity.class);
            if (intent != null) { startActivity(intent); finish(); return true; }
            return false;
        });
    }

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

    private void setUrlSilently(String url) {
        isLoadingForEdit = true;
        etUrl.setText(url);
        isLoadingForEdit = false;
    }

    // ─── Carga de datos ─────────────────────────────────────────

    private void loadCategories() {
        apiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.code() == 401 || response.code() == 403) { goToLogin(); return; }
                if (response.isSuccessful() && response.body() != null) {
                    categories = response.body();
                    List<String> names = new ArrayList<>();
                    for (Category cat : categories) names.add(cat.getName());
                    autoCompleteCategory.setAdapter(new ArrayAdapter<>(EntryFormActivity.this,
                            android.R.layout.simple_dropdown_item_1line, names));
                    autoCompleteCategory.setThreshold(0);
                    autoCompleteCategory.setOnClickListener(v -> autoCompleteCategory.showDropDown());
                    autoCompleteCategory.setOnItemClickListener((parent, view, position, id) -> {
                        formModified = true;
                        loadSubcategories(categories.get(position).getId());
                    });
                }
            }
            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                if (ApiClient.switchToNextUrl()) { apiService = ApiClient.getApiService(); loadCategories(); }
                else Toast.makeText(EntryFormActivity.this, "Error al cargar categorías", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSubcategories(int categoryId) {
        apiService.getSubcategories(categoryId).enqueue(new Callback<List<Subcategory>>() {
            @Override
            public void onResponse(Call<List<Subcategory>> call, Response<List<Subcategory>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    subcategories = response.body();
                    List<String> names = new ArrayList<>();
                    for (Subcategory sub : subcategories) names.add(sub.getName());
                    autoCompleteSubcategory.setAdapter(new ArrayAdapter<>(EntryFormActivity.this,
                            android.R.layout.simple_dropdown_item_1line, names));
                }
            }
            @Override
            public void onFailure(Call<List<Subcategory>> call, Throwable t) {
                if (ApiClient.switchToNextUrl()) { apiService = ApiClient.getApiService(); loadSubcategories(categoryId); }
                else Toast.makeText(EntryFormActivity.this, "Error al cargar subcategorías", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─── Guardar ────────────────────────────────────────────────

    private void saveEntry() {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) { Toast.makeText(this, "El título es obligatorio", Toast.LENGTH_SHORT).show(); return; }

        String categoryName = autoCompleteCategory.getText().toString();
        String subcategoryName = autoCompleteSubcategory.getText().toString();
        String statusValue = StatusUtils.toKey(autoCompleteStatus.getText().toString());
        if (statusValue == null) statusValue = StatusUtils.PENDIENTE;

        Integer categoryId = null;
        for (Category cat : categories) { if (cat.getName().equals(categoryName)) { categoryId = cat.getId(); break; } }

        Integer subcategoryId = null;
        for (Subcategory sub : subcategories) { if (sub.getName().equals(subcategoryName)) { subcategoryId = sub.getId(); break; } }

        String url = etUrl.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        StringBuilder desc = new StringBuilder();
        if (!url.isEmpty()) desc.append("URL: ").append(url);
        if (!notes.isEmpty()) {
            if (desc.length() > 0) desc.append("\n\n");
            desc.append(notes);
        }

        Map<String, Object> entryData = new HashMap<>();
        entryData.put("title", title);
        if (categoryId != null) entryData.put("category", categoryId);
        if (subcategoryId != null) entryData.put("subcategory", subcategoryId);
        entryData.put("status", statusValue);
        if (desc.length() > 0) entryData.put("description", desc.toString());
        if (pendingImageUrl != null && !pendingImageUrl.isEmpty()) entryData.put("image_url", pendingImageUrl);
        if (pendingFieldValues != null && !pendingFieldValues.isEmpty()) entryData.put("field_values", pendingFieldValues);
        if (layoutCompletedDate.getVisibility() == View.VISIBLE) {
            String cd = etCompletedDate.getText().toString().trim();
            if (!cd.isEmpty()) entryData.put("completed_date", cd);
        }

        Call<Entry> call = editingEntryId != -1
                ? apiService.updateEntry(editingEntryId, entryData)
                : apiService.createEntry(entryData);

        call.enqueue(new Callback<Entry>() {
            @Override
            public void onResponse(Call<Entry> call, Response<Entry> response) {
                if (response.code() == 401 || response.code() == 403) { goToLogin(); return; }
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
                if (ApiClient.switchToNextUrl()) { apiService = ApiClient.getApiService(); saveEntry(); }
                else Toast.makeText(EntryFormActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
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

    // ─── Carga para edición ─────────────────────────────────────

    private void loadEntryForEdit(int entryId) {
        apiService.getEntry(entryId).enqueue(new Callback<Entry>() {
            @Override
            public void onResponse(Call<Entry> call, Response<Entry> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Entry entry = response.body();
                    etTitle.setText(entry.getTitle());

                    if (entry.getCategoryName() != null) {
                        autoCompleteCategory.setText(entry.getCategoryName(), false);
                        for (Category cat : categories) {
                            if (cat.getName().equals(entry.getCategoryName())) {
                                if (entry.getSubcategoryName() != null)
                                    loadSubcategoriesAndSelectByName(cat.getId(), entry.getSubcategoryName());
                                else loadSubcategories(cat.getId());
                                break;
                            }
                        }
                    }

                    if (entry.getStatus() != null) {
                        autoCompleteStatus.setText(StatusUtils.toDisplay(entry.getStatus()), false);
                        if ("completado".equals(entry.getStatus()) || "abandonado".equals(entry.getStatus())) {
                            layoutCompletedDate.setVisibility(View.VISIBLE);
                            if (entry.getCompletedDate() != null && !entry.getCompletedDate().isEmpty())
                                etCompletedDate.setText(entry.getCompletedDate());
                        }
                    }

                    if (entry.getDescription() != null) {
                        String desc = entry.getDescription();
                        if (desc.startsWith("URL: ")) {
                            int end = desc.indexOf("\n\n");
                            String urlPart = end != -1 ? desc.substring(5, end) : desc.substring(5);
                            String notesPart = end != -1 ? desc.substring(end + 2) : "";
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
                if (ApiClient.switchToNextUrl()) { apiService = ApiClient.getApiService(); loadEntryForEdit(entryId); }
                else Toast.makeText(EntryFormActivity.this, "Error al cargar la entrada", Toast.LENGTH_SHORT).show();
            }
        });
    }
}