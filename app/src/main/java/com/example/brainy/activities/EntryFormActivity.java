package com.example.brainy.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EntryFormActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etNotes;
    private AutoCompleteTextView autoCompleteCategory, autoCompleteSubcategory, autoCompleteStatus;
    private MaterialButton btnSave;

    private ApiService apiService;
    private List<Category> categories = new ArrayList<>();
    private List<Subcategory> subcategories = new ArrayList<>();
    private int editingEntryId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_form);

        etTitle = findViewById(R.id.etTitle);
        etNotes = findViewById(R.id.etNotes);
        autoCompleteCategory = findViewById(R.id.autoCompleteCategory);
        autoCompleteSubcategory = findViewById(R.id.autoCompleteSubcategory);
        autoCompleteStatus = findViewById(R.id.autoCompleteStatus);
        btnSave = findViewById(R.id.btnSave);

        apiService = ApiClient.getApiService();

        setupStatusDropdown();
        loadCategories();

        btnSave.setOnClickListener(v -> saveEntry());

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_add);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_add) {
                return true;
            } else if (itemId == R.id.nav_hub) {
                startActivity(new Intent(EntryFormActivity.this, MainActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(EntryFormActivity.this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });

        if (getIntent().hasExtra("entry_id")) {
            editingEntryId = getIntent().getIntExtra("entry_id", -1);
            if (editingEntryId != -1) {
                loadEntryForEdit(editingEntryId);
            }
        }
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
                Toast.makeText(EntryFormActivity.this, "Error al cargar categorías", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(EntryFormActivity.this, "Error al cargar subcategorías", Toast.LENGTH_SHORT).show();
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

        String notes = etNotes.getText().toString().trim();
        if (notes.isEmpty()) {
            notes = null;
        }

        java.util.Map<String, Object> entryData = new java.util.HashMap<>();
        entryData.put("title", title);
        if (categoryId != null) entryData.put("category", categoryId);
        if (subcategoryId != null) entryData.put("subcategory", subcategoryId);
        entryData.put("status", statusValue);
        if (notes != null) entryData.put("description", notes);

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
                Toast.makeText(EntryFormActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
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
                        etNotes.setText(entry.getDescription());
                    }
                }
            }

            @Override
            public void onFailure(Call<Entry> call, Throwable t) {
                Toast.makeText(EntryFormActivity.this, "Error al cargar la entrada", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
