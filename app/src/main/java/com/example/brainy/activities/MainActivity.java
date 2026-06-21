package com.example.brainy.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brainy.R;
import com.example.brainy.adapters.EntryAdapter;
import com.example.brainy.api.ApiClient;
import com.example.brainy.api.ApiService;
import com.example.brainy.api.models.Category;
import com.example.brainy.api.models.Entry;
import com.example.brainy.api.models.Tag;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvEntries;
    private TextView tvEmpty;
    private TextInputEditText etSearch;
    private android.widget.Spinner spinnerCategoryFilter, spinnerStatusFilter, spinnerTagFilter;

    private EntryAdapter adapter;
    private List<Entry> entries = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    private List<Tag> tags = new ArrayList<>();
    private ApiService apiService;

    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private Integer selectedCategoryId = null;
    private String selectedStatus = null;
    private String selectedTagIds = null;
    private boolean isInitializing = true;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View root = findViewById(android.R.id.content);
        root.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.fade_in));

        rvEntries = findViewById(R.id.rvEntries);
        tvEmpty = findViewById(R.id.tvEmpty);
        etSearch = findViewById(R.id.etSearch);
        spinnerCategoryFilter = findViewById(R.id.spinnerCategoryFilter);
        spinnerStatusFilter = findViewById(R.id.spinnerStatusFilter);
        spinnerTagFilter = findViewById(R.id.spinnerTagFilter);

        apiService = ApiClient.getApiService();
        preferences = getSharedPreferences("brainy_prefs", MODE_PRIVATE);

        int columns = preferences.getInt("grid_columns", 3);
        rvEntries.setLayoutManager(new GridLayoutManager(this, columns));
        adapter = new EntryAdapter(entries, entry -> {
            Intent intent = new Intent(MainActivity.this, EntryDetailActivity.class);
            intent.putExtra("entry_id", entry.getId());
            startActivity(intent);
        });
        rvEntries.setAdapter(adapter);

        setupBottomNav();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isInitializing) return;
                searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> loadEntries();
                searchHandler.postDelayed(searchRunnable, 300);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        setupStatusFilter();
        loadCategories();
        loadTags();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isInitializing) {
            int columns = preferences.getInt("grid_columns", 3);
            GridLayoutManager layoutManager = (GridLayoutManager) rvEntries.getLayoutManager();
            if (layoutManager != null && layoutManager.getSpanCount() != columns) {
                rvEntries.setLayoutManager(new GridLayoutManager(this, columns));
                rvEntries.setAdapter(adapter);
            }
            loadEntries();
        }
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_hub);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_hub) return true;
            if (id == R.id.nav_add) { startActivity(new Intent(this, EntryFormActivity.class)); return true; }
            if (id == R.id.nav_random) { startActivity(new Intent(this, RandomActivity.class)); return true; }
            if (id == R.id.nav_timeline) { startActivity(new Intent(this, TimelineActivity.class)); finish(); return true; }
            if (id == R.id.nav_profile) { startActivity(new Intent(this, ProfileActivity.class)); finish(); return true; }
            return false;
        });
    }

    private void setupStatusFilter() {
        String[] statuses = {"Estado", "Pendiente", "En progreso", "Completado", "Abandonado"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, statuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatusFilter.setAdapter(adapter);

        spinnerStatusFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedStatus = position == 0 ? null : statuses[position].toLowerCase().replace(" ", "_");
                if (!isInitializing) loadEntries();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { selectedStatus = null; }
        });
    }

    private void loadCategories() {
        apiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.code() == 401 || response.code() == 403) { goToLogin(); return; }
                if (response.isSuccessful() && response.body() != null) {
                    categories = response.body();
                    setupCategorySpinner();
                    isInitializing = false;
                    loadEntries();
                }
            }
            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                if (ApiClient.switchToNextUrl()) { apiService = ApiClient.getApiService(); loadCategories(); }
                else { isInitializing = false; loadEntries(); }
            }
        });
    }

    private void setupCategorySpinner() {
        List<String> names = new ArrayList<>();
        names.add("Categoría");
        for (Category cat : categories) names.add(cat.getName());

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoryFilter.setAdapter(adapter);

        spinnerCategoryFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                selectedCategoryId = pos == 0 ? null : categories.get(pos - 1).getId();
                if (!isInitializing) loadEntries();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { selectedCategoryId = null; }
        });
    }

    private void loadTags() {
        apiService.getTags().enqueue(new Callback<List<Tag>>() {
            @Override
            public void onResponse(Call<List<Tag>> call, Response<List<Tag>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tags = response.body();
                    setupTagFilter();
                }
            }
            @Override
            public void onFailure(Call<List<Tag>> call, Throwable t) {}
        });
    }

    private void setupTagFilter() {
        List<String> names = new ArrayList<>();
        names.add("Tag");
        for (Tag tag : tags) names.add(tag.getName());

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTagFilter.setAdapter(adapter);

        spinnerTagFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                selectedTagIds = pos == 0 ? null : String.valueOf(tags.get(pos - 1).getId());
                if (!isInitializing) loadEntries();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { selectedTagIds = null; }
        });
    }

    private void loadEntries() {
        String search = etSearch.getText().toString().trim();
        apiService.getEntries(search.isEmpty() ? null : search, selectedCategoryId, selectedStatus, "-updated_at", selectedTagIds)
                .enqueue(new Callback<List<Entry>>() {
                    @Override
                    public void onResponse(Call<List<Entry>> call, Response<List<Entry>> response) {
                        if (response.code() == 401 || response.code() == 403) { goToLogin(); return; }
                        if (response.isSuccessful() && response.body() != null) {
                            entries = response.body();
                            adapter.updateEntries(entries);
                            tvEmpty.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
                            rvEntries.setVisibility(entries.isEmpty() ? View.GONE : View.VISIBLE);
                        }
                    }
                    @Override
                    public void onFailure(Call<List<Entry>> call, Throwable t) {
                        if (ApiClient.switchToNextUrl()) { apiService = ApiClient.getApiService(); loadEntries(); }
                        else { tvEmpty.setVisibility(View.VISIBLE); tvEmpty.setText("Error al cargar"); rvEntries.setVisibility(View.GONE); }
                    }
                });
    }

    private void goToLogin() {
        ApiClient.clearSession(this);
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}