package com.example.brainy.activities;

import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brainy.R;
import com.example.brainy.api.ApiClient;
import com.example.brainy.api.ApiService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ImportActivity extends AppCompatActivity {

    private TextInputEditText etJsonInput;
    private MaterialButton btnImport;
    private MaterialCardView cardResult;
    private TextView tvResultMessage, tvResultErrors;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        // Animación de entrada
        View root = findViewById(android.R.id.content);
        root.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));

        // usamos getApiService para que incluya las cookies de sesión
        apiService = ApiClient.getApiService();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etJsonInput = findViewById(R.id.etJsonInput);
        btnImport = findViewById(R.id.btnImport);
        cardResult = findViewById(R.id.cardResult);
        tvResultMessage = findViewById(R.id.tvResultMessage);
        tvResultErrors = findViewById(R.id.tvResultErrors);

        btnImport.setOnClickListener(v -> importData());
    }

    private void importData() {
        String text = etJsonInput.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Pega el JSON o CSV primero", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Map<String, Object>> entriesList;

        // Detectar si empieza con "RYM Album" - CSV de RateYourMusic
        if (text.startsWith("\"RYM Album\"") || text.startsWith("RYM Album")) {
            try {
                entriesList = parseRymCsv(text);
                Toast.makeText(this, "CSV de RateYourMusic detectado, convirtiendo...", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Error al leer CSV de RYM: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
        } else if (text.startsWith("[")) {
            // Es JSON
            try {
                entriesList = parseJson(text);
            } catch (JSONException e) {
                Toast.makeText(this, "JSON inválido: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            Toast.makeText(this, "Formato no reconocido. Pega un JSON o un CSV de RateYourMusic.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (entriesList.isEmpty()) {
            Toast.makeText(this, "No se encontraron entradas para importar", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar loading
        btnImport.setEnabled(false);
        btnImport.setText("Importando...");
        cardResult.setVisibility(View.GONE);

        apiService.importJson(entriesList).enqueue(importCallback());
    }

    // Callback compartido para mostrar resultados de importación
    private Callback<Map<String, Object>> importCallback() {
        return new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                btnImport.setEnabled(true);
                btnImport.setText("Importar");

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();
                    String mensaje = (String) result.get("mensaje");
                    Object erroresObj = result.get("errores");

                    tvResultMessage.setText(mensaje != null ? mensaje : "Importación completada");

                    if (erroresObj instanceof List) {
                        List<?> errores = (List<?>) erroresObj;
                        if (!errores.isEmpty()) {
                            StringBuilder sb = new StringBuilder("Errores:\n");
                            for (Object err : errores) {
                                if (err instanceof Map) {
                                    Map<?, ?> errMap = (Map<?, ?>) err;
                                    sb.append("- Índice ").append(errMap.get("indice"))
                                            .append(": ").append(errMap.get("error")).append("\n");
                                }
                            }
                            tvResultErrors.setText(sb.toString());
                            tvResultErrors.setVisibility(View.VISIBLE);
                        } else {
                            tvResultErrors.setVisibility(View.GONE);
                        }
                    } else {
                        tvResultErrors.setVisibility(View.GONE);
                    }
                    cardResult.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(ImportActivity.this,
                            "Error del servidor: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnImport.setEnabled(true);
                btnImport.setText("Importar");
                Toast.makeText(ImportActivity.this,
                        "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
    }


     // Parsea un array JSON a List<Map<String, Object>>
    private List<Map<String, Object>> parseJson(String jsonText) throws JSONException {
        JSONArray jsonArray = new JSONArray(jsonText);
        List<Map<String, Object>> entriesList = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            Map<String, Object> map = new HashMap<>();

            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = obj.get(key);

                if (value instanceof JSONArray) {
                    JSONArray arr = (JSONArray) value;
                    List<Object> list = new ArrayList<>();
                    for (int j = 0; j < arr.length(); j++) {
                        list.add(arr.get(j));
                    }
                    map.put(key, list);
                } else if (value instanceof JSONObject) {
                    JSONObject innerObj = (JSONObject) value;
                    Map<String, Object> innerMap = new HashMap<>();
                    Iterator<String> innerKeys = innerObj.keys();
                    while (innerKeys.hasNext()) {
                        String ik = innerKeys.next();
                        innerMap.put(ik, innerObj.get(ik));
                    }
                    map.put(key, innerMap);
                } else {
                    map.put(key, value);
                }
            }

            entriesList.add(map);
        }

        return entriesList;
    }

    // Parsea el CSV de RateYourMusic. "w" significa wishlist (pendiente) y "o" = owned (completado)

    private List<Map<String, Object>> parseRymCsv(String csvText) {
        List<Map<String, Object>> entries = new ArrayList<>();

        String[] lines = csvText.split("\n");
        if (lines.length < 2) return entries;

        // Saltar cabecera (primera línea)
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            String[] values = parseCsvLine(line);
            if (values.length < 7) continue;

            Map<String, Object> entry = new HashMap<>();

            // Construir nombre del artista: First Name + Last Name
            String firstName = getValue(values, 1);
            String lastName = getValue(values, 2);
            String artistName = (firstName + " " + lastName).trim();

            // Título del álbum (columna 5)
            String title = getValue(values, 5);

            // Título final: "Artista - Álbum" o solo el título
            if (!artistName.isEmpty()) {
                entry.put("title", artistName + " - " + title);
            } else {
                entry.put("title", title);
            }

            // Descripción con año de lanzamiento (columna 6)
            String releaseDate = getValue(values, 6);
            if (!releaseDate.isEmpty()) {
                entry.put("description", "Año: " + releaseDate);
            }

            // Categoría: Música, Subcategoría: Álbum
            entry.put("category_name", "Música");
            entry.put("subcategory_name", "Álbum");

            String ownership = getValue(values, 8);
            if ("w".equals(ownership)) {
                entry.put("status", "pendiente");
            } else if ("o".equals(ownership)) {
                entry.put("status", "completado");
            }

            // Media Type como tag (columna 10)
            String mediaType = getValue(values, 10);
            if (!mediaType.isEmpty()) {
                List<String> tags = new ArrayList<>();
                tags.add(mediaType.toLowerCase());
                entry.put("tags", tags);
            }

            // Rating (columna 7) como field_value
            String rating = getValue(values, 7);
            if (!rating.isEmpty() && !"0".equals(rating)) {
                List<Map<String, Object>> fieldValues = new ArrayList<>();
                Map<String, Object> fv = new HashMap<>();
                fv.put("key", "rating");
                fv.put("value", rating);
                fieldValues.add(fv);
                entry.put("field_values", fieldValues);
            }

            entries.add(entry);
        }

        return entries;
    }

     //  Obtiene un valor de un array, o string vacío si no existe

    private String getValue(String[] values, int index) {
        if (index < values.length) {
            return values[index].trim();
        }
        return "";
    }


     //  Parsea una línea de CSV respetando comillas

    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());

        return fields.toArray(new String[0]);
    }
}
