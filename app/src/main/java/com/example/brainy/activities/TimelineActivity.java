package com.example.brainy.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brainy.R;
import com.example.brainy.adapters.TimelineAdapter;
import com.example.brainy.api.ApiClient;
import com.example.brainy.api.ApiService;
import com.example.brainy.api.models.Category;
import com.example.brainy.api.models.Entry;
import com.example.brainy.api.models.TimelineYear;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TimelineActivity extends AppCompatActivity {

    private RecyclerView rvTimeline;
    private TextView tvEmpty;
    private TextInputEditText etSearch;
    private android.widget.Spinner spinnerCategory, spinnerStatus;

    private TimelineAdapter adapter;
    private List<TimelineYear> timelineData = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    private ApiService apiService;

    private Integer selectedCategoryId = null;
    private String selectedStatus = null;
    private boolean isInitializing = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

        View root = findViewById(android.R.id.content);
        root.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));

        rvTimeline = findViewById(R.id.rvTimeline);
        tvEmpty = findViewById(R.id.tvTimelineEmpty);
        etSearch = findViewById(R.id.etTimelineSearch);
        spinnerCategory = findViewById(R.id.spinnerTimelineCategory);
        spinnerStatus = findViewById(R.id.spinnerTimelineStatus);

        apiService = ApiClient.getApiService();

        rvTimeline.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TimelineAdapter(timelineData, entry -> {
            Intent intent = new Intent(TimelineActivity.this, EntryDetailActivity.class);
            intent.putExtra("entry_id", entry.getId());
            startActivity(intent);
        });
        rvTimeline.setAdapter(adapter);

        setupBottomNav();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isInitializing) return;
                loadTimeline();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        setupStatusFilter();
        loadCategories();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_timeline);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_timeline) return true;
            if (id == R.id.nav_hub) { startActivity(new Intent(this, MainActivity.class)); finish(); return true; }
            if (id == R.id.nav_add) { startActivity(new Intent(this, EntryFormActivity.class)); return true; }
            if (id == R.id.nav_random) { startActivity(new Intent(this, RandomActivity.class)); return true; }
            if (id == R.id.nav_profile) { startActivity(new Intent(this, ProfileActivity.class)); finish(); return true; }
            return false;
        });
    }

    private void setupStatusFilter() {
        String[] statuses = {"Todos", "Pendiente", "En progreso", "Completado", "Abandonado"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, statuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapter);

        spinnerStatus.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedStatus = position == 0 ? null : statuses[position].toLowerCase().replace(" ", "_");
                if (!isInitializing) loadTimeline();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { selectedStatus = null; }
        });
    }

    private void loadCategories() {
        apiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categories = response.body();
                    setupCategorySpinner();
                    isInitializing = false;
                    loadTimeline();
                }
            }
            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                if (ApiClient.switchToNextUrl()) { apiService = ApiClient.getApiService(); loadCategories(); }
                else { isInitializing = false; loadTimeline(); }
            }
        });
    }

    private void setupCategorySpinner() {
        List<String> categoryNames = new ArrayList<>();
        categoryNames.add("Todas");
        for (Category cat : categories) categoryNames.add(cat.getName());

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categoryNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        spinnerCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedCategoryId = position == 0 ? null : categories.get(position - 1).getId();
                if (!isInitializing) loadTimeline();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { selectedCategoryId = null; }
        });
    }

    private void loadTimeline() {
        String search = etSearch.getText().toString().trim();

        apiService.getTimeline(selectedCategoryId, selectedStatus)
                .enqueue(new Callback<List<TimelineYear>>() {
                    @Override
                    public void onResponse(Call<List<TimelineYear>> call, Response<List<TimelineYear>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            timelineData = response.body();

                            if (!search.isEmpty()) {
                                List<TimelineYear> filtered = new ArrayList<>();
                                for (TimelineYear year : timelineData) {
                                    List<Entry> matching = new ArrayList<>();
                                    for (Entry e : year.getEntries()) {
                                        if (e.getTitle() != null && e.getTitle().toLowerCase().contains(search.toLowerCase()))
                                            matching.add(e);
                                    }
                                    if (!matching.isEmpty()) {
                                        TimelineYear y = new TimelineYear();
                                        y.setYear(year.getYear());
                                        y.setEntries(matching);
                                        filtered.add(y);
                                    }
                                }
                                timelineData = filtered;
                            }

                            adapter.updateData(timelineData);
                            tvEmpty.setVisibility(timelineData.isEmpty() ? View.VISIBLE : View.GONE);
                            rvTimeline.setVisibility(timelineData.isEmpty() ? View.GONE : View.VISIBLE);
                        }
                    }
                    @Override
                    public void onFailure(Call<List<TimelineYear>> call, Throwable t) {
                        if (ApiClient.switchToNextUrl()) { apiService = ApiClient.getApiService(); loadTimeline(); }
                        else { tvEmpty.setVisibility(View.VISIBLE); tvEmpty.setText("Error al cargar"); rvTimeline.setVisibility(View.GONE); }
                    }
                });
    }
}