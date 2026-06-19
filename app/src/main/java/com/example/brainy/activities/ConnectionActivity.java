package com.example.brainy.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brainy.R;
import com.example.brainy.api.ApiClient;
import com.example.brainy.api.ApiService;
import com.example.brainy.api.models.ConnectionResponse;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConnectionActivity extends AppCompatActivity {

    private RecyclerView rvConnections;
    private TextView tvEmpty;
    private MaterialButton btnGenerate;
    private ConnectionAdapter adapter;
    private List<ConnectionResponse.Connection> connections = new ArrayList<>();
    private ApiService apiService;
    private int entryId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connections);

        entryId = getIntent().getIntExtra("entry_id", -1);
        apiService = ApiClient.getApiService();

        rvConnections = findViewById(R.id.rvConnections);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnGenerate = findViewById(R.id.btnGenerateConnections);

        rvConnections.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ConnectionAdapter(connections, conn -> {
            Intent intent = new Intent(this, EntryDetailActivity.class);
            intent.putExtra("entry_id", conn.getRelatedEntry().getId());
            startActivity(intent);
        });
        rvConnections.setAdapter(adapter);

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        btnGenerate.setOnClickListener(v -> generateConnections());

        loadConnections();
    }

    private void loadConnections() {
        apiService.getConnections(entryId).enqueue(new Callback<ConnectionResponse>() {
            @Override
            public void onResponse(Call<ConnectionResponse> call, Response<ConnectionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    connections = response.body().getConnections();
                    if (connections == null || connections.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvConnections.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rvConnections.setVisibility(View.VISIBLE);
                        adapter.updateConnections(connections);
                    }
                }
            }

            @Override
            public void onFailure(Call<ConnectionResponse> call, Throwable t) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("Error al cargar conexiones");
            }
        });
    }

    private void generateConnections() {
        btnGenerate.setEnabled(false);
        btnGenerate.setText("Generando...");

        apiService.generateConnections().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                btnGenerate.setEnabled(true);
                btnGenerate.setText("Generar conexiones automáticas");
                loadConnections();
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnGenerate.setEnabled(true);
                btnGenerate.setText("Generar conexiones automáticas");
                Toast.makeText(ConnectionActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static class ConnectionAdapter extends RecyclerView.Adapter<ConnectionAdapter.ViewHolder> {
        private List<ConnectionResponse.Connection> data;
        private OnConnectionClickListener listener;

        interface OnConnectionClickListener {
            void onClick(ConnectionResponse.Connection conn);
        }

        ConnectionAdapter(List<ConnectionResponse.Connection> data, OnConnectionClickListener listener) {
            this.data = data;
            this.listener = listener;
        }

        void updateConnections(List<ConnectionResponse.Connection> newData) {
            this.data = newData;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ConnectionResponse.Connection conn = data.get(position);
            holder.text1.setText(conn.getRelatedEntry().getTitle());
            holder.text2.setText(conn.getLabel());
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onClick(conn);
            });
        }

        @Override
        public int getItemCount() {
            return data != null ? data.size() : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}