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

        // Animación de entrada
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

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_timeline);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_timeline) {
                return true;
            } else if (itemId == R.id.nav_hub) {
                Intent intent = new Intent(TimelineActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_add) {
                Intent intent = new Intent(TimelineActivity.this, EntryFormActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent intent = new Intent(TimelineActivity.this, ProfileActivity.class);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });

        // TextWatcher para búsqueda
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

    private void setupStatusFilter() {
        String[] statuses = {"Todos", "Pendiente", "En progreso", "Completado", "Abandonado"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, statuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapter);

        spinnerStatus.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedStatus = statusToKey(statuses[position]);
                if (!isInitializing) loadTimeline();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                selectedStatus = null;
            }
        });
    }

    // Convierte texto de estado a su clave interna, o null para "Todos"
    private String statusToKey(String text) {
        switch (text) {
            case "Pendiente": return "pendiente";
            case "En progreso": return "en_progreso";
            case "Completado": return "completado";
            case "Abandonado": return "abandonado";
            default: return null;
        }
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
                if (ApiClient.switchToNextUrl()) {
                    apiService = ApiClient.getApiService();
                    loadCategories();
                } else {
                    isInitializing = false;
                    loadTimeline();
                }
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
                String text = categoryNames.get(position);
                selectedCategoryId = text.equals("Todas") ? null : findCategoryIdByName(text);
                if (!isInitializing) loadTimeline();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                selectedCategoryId = null;
            }
        });
    }

    private Integer findCategoryIdByName(String name) {
        for (Category cat : categories) {
            if (cat.getName().equals(name)) return cat.getId();
        }
        return null;
    }

    private void loadTimeline() {
        String search = etSearch.getText().toString().trim();
        String searchParam = search.isEmpty() ? null : search;

        apiService.getTimeline(selectedCategoryId, selectedStatus)
                .enqueue(new Callback<List<TimelineYear>>() {
                    @Override
                    public void onResponse(Call<List<TimelineYear>> call, Response<List<TimelineYear>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            timelineData = response.body();

                            // Si hay búsqueda, filtrar localmente por título
                            if (searchParam != null && !searchParam.isEmpty()) {
                                List<TimelineYear> filtered = new ArrayList<>();
                                for (TimelineYear year : timelineData) {
                                    List<Entry> matchingEntries = new ArrayList<>();
                                    for (Entry entry : year.getEntries()) {
                                        if (entry.getTitle() != null &&
                                                entry.getTitle().toLowerCase().contains(searchParam.toLowerCase())) {
                                            matchingEntries.add(entry);
                                        }
                                    }
                                    if (!matchingEntries.isEmpty()) {
                                        TimelineYear filteredYear = new TimelineYear();
                                        filteredYear.setYear(year.getYear());
                                        filteredYear.setEntries(matchingEntries);
                                        filtered.add(filteredYear);
                                    }
                                }
                                timelineData = filtered;
                            }

                            adapter.updateData(timelineData);

                            if (timelineData.isEmpty()) {
                                tvEmpty.setVisibility(View.VISIBLE);
                                rvTimeline.setVisibility(View.GONE);
                            } else {
                                tvEmpty.setVisibility(View.GONE);
                                rvTimeline.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<List<TimelineYear>> call, Throwable t) {
                        if (ApiClient.switchToNextUrl()) {
                            apiService = ApiClient.getApiService();
                            loadTimeline();
                        } else {
                            tvEmpty.setVisibility(View.VISIBLE);
                            tvEmpty.setText("Error al cargar la línea temporal");
                            rvTimeline.setVisibility(View.GONE);
                        }
                    }
                });
    }

    // Adaptador interno para la línea temporal (agrupado por año)
    private static class TimelineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_YEAR_HEADER = 0;
        private static final int TYPE_ENTRY = 1;

        private List<TimelineYear> data;
        private OnEntryClickListener listener;

        // Lista plana para el RecyclerView: cada elemento es un Object que puede ser un String (año) o un Entry
		
        private List<Object> flatList = new ArrayList<>();

        public interface OnEntryClickListener {
            void onEntryClick(Entry entry);
        }

        TimelineAdapter(List<TimelineYear> data, OnEntryClickListener listener) {
            this.data = data;
            this.listener = listener;
            buildFlatList();
        }

        void updateData(List<TimelineYear> newData) {
            this.data = newData;
            buildFlatList();
            notifyDataSetChanged();
        }

        private void buildFlatList() {
            flatList.clear();
            for (TimelineYear year : data) {
                flatList.add(year.getYear()); // Header del año
                flatList.addAll(year.getEntries()); // Entradas de ese año
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (flatList.get(position) instanceof String) {
                return TYPE_YEAR_HEADER;
            }
            return TYPE_ENTRY;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_YEAR_HEADER) {
                View view = inflater.inflate(R.layout.item_timeline_year, parent, false);
                return new YearViewHolder(view);
            } else {
                View view = inflater.inflate(R.layout.item_timeline_entry, parent, false);
                return new EntryViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof YearViewHolder) {
                String year = (String) flatList.get(position);
                ((YearViewHolder) holder).tvYear.setText(year);
            } else if (holder instanceof EntryViewHolder) {
                Entry entry = (Entry) flatList.get(position);
                EntryViewHolder vh = (EntryViewHolder) holder;
                vh.tvTitle.setText(entry.getTitle());
                if (entry.getCategoryName() != null) {
                    vh.tvCategory.setText(entry.getCategoryName());
                    vh.tvCategory.setVisibility(View.VISIBLE);
                } else {
                    vh.tvCategory.setVisibility(View.GONE);
                }
                vh.itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onEntryClick(entry);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return flatList.size();
        }

        static class YearViewHolder extends RecyclerView.ViewHolder {
            TextView tvYear;

            YearViewHolder(@NonNull View itemView) {
                super(itemView);
                tvYear = itemView.findViewById(R.id.tvTimelineYear);
            }
        }

        static class EntryViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvCategory;

            EntryViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTimelineEntryTitle);
                tvCategory = itemView.findViewById(R.id.tvTimelineEntryCategory);
            }
        }
    }
}
