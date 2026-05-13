package com.example.brainy.activities;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brainy.R;
import com.example.brainy.adapters.EntryAdapter;
import com.example.brainy.api.ApiClient;
import com.example.brainy.api.ApiService;
import com.example.brainy.api.models.Category;
import com.example.brainy.api.models.Entry;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
    private android.widget.Spinner spinnerCategoryFilter, spinnerStatusFilter;
    private FloatingActionButton fabAdd;

    private EntryAdapter adapter;
    private List<Entry> entries = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    private ApiService apiService;

    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private Integer selectedCategoryId = null;
    private String selectedStatus = null;
    private boolean isInitializing = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rvEntries = findViewById(R.id.rvEntries);
        tvEmpty = findViewById(R.id.tvEmpty);
        etSearch = findViewById(R.id.etSearch);
        spinnerCategoryFilter = findViewById(R.id.spinnerCategoryFilter);
        spinnerStatusFilter = findViewById(R.id.spinnerStatusFilter);
        fabAdd = findViewById(R.id.fabAdd);

        apiService = ApiClient.getApiService();

        rvEntries.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EntryAdapter(entries, entry -> {
            Intent intent = new Intent(MainActivity.this, EntryDetailActivity.class);
            intent.putExtra("entry_id", entry.getId());
            ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                    MainActivity.this,
                    R.anim.slide_left,
                    R.anim.fade_out
            );
            startActivity(intent, options.toBundle());
        });
        rvEntries.setAdapter(adapter);

        // Animación de entrada para el FAB
        Animation fabAnim = AnimationUtils.loadAnimation(this, R.anim.fab_scale_up);
        fabAdd.startAnimation(fabAnim);

        fabAdd.setOnClickListener(v -> {
            AnimatorSet pressAnim = (AnimatorSet) AnimatorInflater.loadAnimator(
                    MainActivity.this, R.animator.fab_press_anim);
            pressAnim.setTarget(fabAdd);
            pressAnim.start();

            fabAdd.postDelayed(() -> {
                Intent intent = new Intent(MainActivity.this, EntryFormActivity.class);
                ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                        MainActivity.this,
                        R.anim.slide_up,
                        R.anim.fade_out
                );
                startActivity(intent, options.toBundle());
            }, 150);
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_hub);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_hub) {
                return true;
            } else if (itemId == R.id.nav_add) {
                Intent intent = new Intent(MainActivity.this, EntryFormActivity.class);
                ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                        MainActivity.this,
                        R.anim.slide_up,
                        R.anim.fade_out
                );
                startActivity(intent, options.toBundle());
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                        MainActivity.this,
                        R.anim.slide_left,
                        R.anim.fade_out
                );
                startActivity(intent, options.toBundle());
                finish();
                return true;
            }
            return false;
        });

        // TextWatcher para búsqueda - ignorar cambios durante inicialización
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isInitializing) return; // Ignorar durante inicialización
                searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> loadEntries();
                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        setupStatusFilter();
        loadCategories();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isInitializing) {
            loadEntries();
        }
    }

    private void setupStatusFilter() {
        String[] statuses = {"Todos", "Pendiente", "En progreso", "Completado", "Abandonado"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, statuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatusFilter.setAdapter(adapter);

        spinnerStatusFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String text = statuses[position];
                switch (text) {
                    case "Todos": selectedStatus = null; break;
                    case "Pendiente": selectedStatus = "pendiente"; break;
                    case "En progreso": selectedStatus = "en_progreso"; break;
                    case "Completado": selectedStatus = "completado"; break;
                    case "Abandonado": selectedStatus = "abandonado"; break;
                }
                if (!isInitializing) loadEntries();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                selectedStatus = null;
            }
        });
    }

    private void loadCategories() {
        apiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categories = response.body();

                    List<String> categoryNames = new ArrayList<>();
                    categoryNames.add("Todas");
                    for (Category cat : categories) {
                        categoryNames.add(cat.getName());
                    }

                    android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(MainActivity.this,
                            android.R.layout.simple_spinner_item, categoryNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerCategoryFilter.setAdapter(adapter);

                    spinnerCategoryFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                            String text = categoryNames.get(position);
                            if (text.equals("Todas")) {
                                selectedCategoryId = null;
                            } else {
                                for (Category cat : categories) {
                                    if (cat.getName().equals(text)) {
                                        selectedCategoryId = cat.getId();
                                        break;
                                    }
                                }
                            }
                            if (!isInitializing) loadEntries();
                        }

                        @Override
                        public void onNothingSelected(android.widget.AdapterView<?> parent) {
                            selectedCategoryId = null;
                        }
                    });

                    // Inicialización completada: cargar entries UNA SOLA VEZ
                    isInitializing = false;
                    loadEntries();
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                // Si falla la carga de categorías, reintentar con otra IP
                if (ApiClient.switchToNextUrl()) {
                    apiService = ApiClient.getApiService();
                    loadCategories();
                } else {
                    // Si no hay más IPs, igual cargamos entries
                    isInitializing = false;
                    loadEntries();
                }
            }
        });
    }

    private void loadEntries() {
        String search = etSearch.getText().toString().trim();
        String searchParam = search.isEmpty() ? null : search;

        apiService.getEntries(searchParam, selectedCategoryId, selectedStatus, "-updated_at")
                .enqueue(new Callback<List<Entry>>() {
                    @Override
                    public void onResponse(Call<List<Entry>> call, Response<List<Entry>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            entries = response.body();
                            adapter.updateEntries(entries);

                            if (entries.isEmpty()) {
                                tvEmpty.setVisibility(View.VISIBLE);
                                rvEntries.setVisibility(View.GONE);
                            } else {
                                tvEmpty.setVisibility(View.GONE);
                                rvEntries.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Entry>> call, Throwable t) {
                        if (ApiClient.switchToNextUrl()) {
                            apiService = ApiClient.getApiService();
                            loadEntries();
                        } else {
                            tvEmpty.setVisibility(View.VISIBLE);
                            tvEmpty.setText("Error al cargar las entradas");
                            rvEntries.setVisibility(View.GONE);
                        }
                    }
                });
    }
}
