package com.example.brainy.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;

import com.example.brainy.R;
import com.example.brainy.api.ApiClient;
import com.example.brainy.api.ApiService;
import com.example.brainy.api.models.Category;
import com.example.brainy.api.models.Entry;
import com.example.brainy.api.models.Subcategory;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EntryFormActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etUrl, etNotes;
    private AutoCompleteTextView autoCompleteCategory, autoCompleteSubcategory, autoCompleteStatus;
    private MaterialButton btnSave;

    private ApiService apiService;
    private List<Category> categories = new ArrayList<>();
    private List<Subcategory> subcategories = new ArrayList<>();
    private int editingEntryId = -1;

    // Datos precargados desde fetch-imdb
    private List<Map<String, Object>> pendingFieldValues = null;

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

        apiService = ApiClient.getApiService();

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

        btnSave.setOnClickListener(v -> saveEntry());

        // Detectar cuando se pega/escribe una URL de IMDB para autocompletar
        etUrl.addTextChangedListener(new TextWatcher() {
            private boolean isFetching = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFetching) return;
                String url = s.toString().trim();
                // Detectar URLs de IMDB: imdb.com/title/ttXXXXXX o m.imdb.com, etc.
                if (url.contains("imdb.com/title/") || url.matches(".*tt\\d{7,8}.*")) {
                    isFetching = true;
                    fetchImdbData(url);
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
                ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                        EntryFormActivity.this,
                        R.anim.slide_right,
                        R.anim.fade_out
                );
                startActivity(intent, options.toBundle());
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent intent = new Intent(EntryFormActivity.this, ProfileActivity.class);
                ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                        EntryFormActivity.this,
                        R.anim.slide_left,
                        R.anim.fade_out
                );
                startActivity(intent, options.toBundle());
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
            if (prefillUrl != null) {
                etUrl.setText(prefillUrl);
            }
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

    private void fetchImdbData(String url) {
        Toast.makeText(this, "Obteniendo datos de IMDB...", Toast.LENGTH_SHORT).show();

        Map<String, String> body = new HashMap<>();
        body.put("url", url);

        apiService.fetchImdbData(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
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

    private void autoSelectCategoryByMediaType(String mediaType) {
        // Buscar la categoría "Cine" (asumiendo que existe)
        for (Category cat : categories) {
            if (cat.getName().equalsIgnoreCase("Cine")) {
                autoCompleteCategory.setText(cat.getName(), false);
                // Cargar subcategorías de Cine
                loadSubcategoriesAndSelect(cat.getId(), mediaType);
                break;
            }
        }
    }

    private void loadSubcategoriesAndSelect(int categoryId, String mediaType) {
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

                    // Seleccionar subcategoría según media_type
                    String targetSubcat = "movie".equals(mediaType) ? "Película" : "Serie";
                    for (Subcategory sub : subcategories) {
                        if (sub.getName().equalsIgnoreCase(targetSubcat)) {
                            autoCompleteSubcategory.setText(sub.getName(), false);
                            break;
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Subcategory>> call, Throwable t) {
                // Si falla, no pasa nada, el usuario puede seleccionar manualmente
            }
        });
    }

    private void setupStatusDropdown() {
        String[] statuses = {"Pendiente", "En progreso", "Completado", "Abandonado"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, statuses);
        autoCompleteStatus.setAdapter(adapter);
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

        String statusValue;
        switch (statusName) {
            case "Pendiente": statusValue = "pendiente"; break;
            case "En progreso": statusValue = "en_progreso"; break;
            case "Completado": statusValue = "completado"; break;
            case "Abandonado": statusValue = "abandonado"; break;
            default: statusValue = "pendiente";
        }

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

        // Incluir field_values si hay pendientes del fetch-imdb
        if (pendingFieldValues != null && !pendingFieldValues.isEmpty()) {
            entryData.put("field_values", pendingFieldValues);
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
                if (response.isSuccessful()) {
                    Toast.makeText(EntryFormActivity.this, "Entrada guardada", Toast.LENGTH_SHORT).show();
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

    private void loadEntryForEdit(int entryId) {
        apiService.getEntry(entryId).enqueue(new Callback<Entry>() {
            @Override
            public void onResponse(Call<Entry> call, Response<Entry> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Entry entry = response.body();
                    etTitle.setText(entry.getTitle());

                    if (entry.getCategoryName() != null) {
                        autoCompleteCategory.setText(entry.getCategoryName(), false);
                    }

                    if (entry.getStatus() != null) {
                        String[] statuses = {"Pendiente", "En progreso", "Completado", "Abandonado"};
                        for (String s : statuses) {
                            String statusKey;
                            switch (s) {
                                case "Pendiente": statusKey = "pendiente"; break;
                                case "En progreso": statusKey = "en_progreso"; break;
                                case "Completado": statusKey = "completado"; break;
                                case "Abandonado": statusKey = "abandonado"; break;
                                default: statusKey = "";
                            }
                            if (statusKey.equals(entry.getStatus())) {
                                autoCompleteStatus.setText(s, false);
                                break;
                            }
                        }
                    }

                    if (entry.getDescription() != null) {
                        String desc = entry.getDescription();
                        // Si la description contiene una URL con nuestro formato, separarla
                        if (desc.startsWith("URL: ")) {
                            int endOfUrl = desc.indexOf("\n\n");
                            if (endOfUrl != -1) {
                                String urlPart = desc.substring(5, endOfUrl); // Quitar "URL: "
                                String notesPart = desc.substring(endOfUrl + 2);
                                etUrl.setText(urlPart);
                                etNotes.setText(notesPart);
                            } else {
                                // Solo hay URL, sin notas
                                etUrl.setText(desc.substring(5));
                            }
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
