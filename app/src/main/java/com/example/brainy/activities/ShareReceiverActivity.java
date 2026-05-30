package com.example.brainy.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Actividad "invisible" que recibe el contenido compartido desde otras apps y debería aparecer en los menús de compartir de las apps externas :)

public class ShareReceiverActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Recibir el intent de compartir
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null && type.startsWith("text/")) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            String sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT);

            // Parsear el contenido compartido
            String title = "";
            String url = "";
            String notes = "";

            if (!TextUtils.isEmpty(sharedSubject)) {
                title = sharedSubject.trim();
            }

            if (!TextUtils.isEmpty(sharedText)) {
                // Intentar extraer una URL del texto compartido
                url = extractUrl(sharedText);

                if (TextUtils.isEmpty(title)) {
                    // Si no hay subject, usar la primera línea como título
                    String[] lines = sharedText.split("\n");
                    title = lines[0].trim();
                    // Limitar título a 100 caracteres
                    if (title.length() > 100) {
                        title = title.substring(0, 100);
                    }
                }

                // Si hay URL, guardar el texto restante como notas (sin la URL)
                if (!TextUtils.isEmpty(url)) {
                    notes = sharedText.replace(url, "").trim();
                    // Si el título es igual a la URL, intentar mejorar
                    if (title.equals(url) || title.isEmpty()) {
                        title = extractPageTitle(sharedText);
                    }
                } else {
                    notes = sharedText;
                }
            }

            // Si no hay título, poner un placeholder
            if (TextUtils.isEmpty(title)) {
                title = "Sin título";
            }

            // Abrir EntryFormActivity con los datos precargados
            Intent formIntent = new Intent(this, EntryFormActivity.class);
            formIntent.putExtra("prefill_title", title);
            if (!TextUtils.isEmpty(url)) {
                formIntent.putExtra("prefill_url", url);
            }
            if (!TextUtils.isEmpty(notes)) {
                formIntent.putExtra("prefill_notes", notes);
            }
            formIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(formIntent);
        }

        // Cerrar esta actividad (no debe quedarse en el back stack)
        finish();
    }

      // Extrae la primera URL que encuentre en un texto.

    private String extractUrl(String text) {
        // Patrón para detectar URLs
        Pattern urlPattern = Pattern.compile(
                "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)"
        );
        Matcher matcher = urlPattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }


    // Intenta extraer un título descriptivo del texto compartido.

    private String extractPageTitle(String text) {
        // Quitar la URL del texto
        String withoutUrl = text.replaceAll("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+", "").trim();
        // Quitar signos de puntuación al inicio
        withoutUrl = withoutUrl.replaceAll("^[\\s\\-:;.,!?]+", "").trim();

        if (!TextUtils.isEmpty(withoutUrl)) {
            // Tomar la primera línea o primeros 100 caracteres
            String[] lines = withoutUrl.split("\n");
            String result = lines[0].trim();
            if (result.length() > 100) {
                result = result.substring(0, 100);
            }
            return result;
        }
        return "Enlace";
    }
}
