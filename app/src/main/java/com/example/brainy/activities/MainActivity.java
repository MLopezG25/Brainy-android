package com.example.brainy.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brainy.R;
import com.example.brainy.adapters.EntryAdapter;
import com.example.brainy.api.ApiClient;
import com.example.brainy.api.ApiService;
import com.example.brainy.api.models.Category;
import com.example.brainy.api.models.Entry;
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
            startActivity(intent);
        });
        rvEntries.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EntryFormActivity.class);
            startActivity(intent);
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> loadEntries();
                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        setupStatusFilter();
        loadCategories();
        loadEntries();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEntries();
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
                loadEntries();
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
                            loadEntries();
                        }

                        @Override
                        public void onNothingSelected(android.widget.AdapterView<?> parent) {
                            selectedCategoryId = null;
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {}
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
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("Error al cargar las entradas");
                        rvEntries.setVisibility(View.GONE);
                    }
                });
    }
}
