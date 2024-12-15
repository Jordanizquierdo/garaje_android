package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.RecyclerAdapter;

import org.json.JSONArray;
import org.json.JSONObject;
import androidx.core.app.NotificationCompat;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecyclerAdapter recyclerAdapter;

    // URLs para los comandos del ESP32
    private static final String ESP32_URL_ABRIR = "http://192.168.190.82/moverServo";  // Comando para abrir
    private static final String ESP32_URL_CERRAR = "http://192.168.190.82/cerrarServo"; // Comando para cerrar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Configurar RecyclerView
        recyclerView = findViewById(R.id.activity_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Datos de prueba para inicializar
        List<String> datos = new ArrayList<>();
        datos.add("Cargando datos...");

        recyclerAdapter = new RecyclerAdapter(datos);
        recyclerView.setAdapter(recyclerAdapter);

        // Llamar al método para leer datos desde ThingSpeak
        ThingSpeakHelper.leerDatos(datos, recyclerAdapter, this);

        // Configurar botones para enviar comandos al ESP32
        Button buttonAbrir = findViewById(R.id.button_abrir);
        buttonAbrir.setOnClickListener(v -> enviarComandoAbrir());

        Button buttonCerrar = findViewById(R.id.button_cerrar);
        buttonCerrar.setOnClickListener(v -> enviarComandoCerrar());

        // Botón para refrescar los datos
        Button buttonRefrescar = findViewById(R.id.button_refrescar);
        buttonRefrescar.setOnClickListener(v -> ThingSpeakHelper.leerDatos(recyclerAdapter.getDatos(), recyclerAdapter, this));
    }

    // Método para enviar el comando de abrir al ESP32
    private void enviarComandoAbrir() {
        enviarComandoESP32(ESP32_URL_ABRIR, "Abrir");
    }

    // Método para enviar el comando de cerrar al ESP32
    private void enviarComandoCerrar() {
        enviarComandoESP32(ESP32_URL_CERRAR, "Cerrar");
    }

    // Método genérico para enviar un comando al ESP32
    private void enviarComandoESP32(String urlCommand, String accion) {
        new Thread(() -> {
            try {
                URL url = new URL(urlCommand);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET"); // Enviar comando como GET
                urlConnection.setConnectTimeout(5000);
                urlConnection.setReadTimeout(5000);

                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();
                    Log.d("ESP32", "Respuesta del ESP32 (" + accion + "): " + response.toString());
                } else {
                    Log.e("ESP32", "Error al enviar comando de " + accion + ". Código: " + responseCode);
                }

                urlConnection.disconnect();
            } catch (Exception e) {
                Log.e("ESP32", "Error al enviar comando de " + accion + " al ESP32", e);
            }
        }).start();
    }

    // Clase para interactuar con ThingSpeak
    static class ThingSpeakHelper {
        private static final String BASE_URL = "https://api.thingspeak.com/channels/2780408/fields/1.json?api_key=L36A07JQRBQZWI3W&results=5";

        public static void leerDatos(List<String> datos, RecyclerAdapter adapter, AppCompatActivity activity) {
            new Thread(() -> {
                try {
                    URL url = new URL(BASE_URL);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.setConnectTimeout(5000);
                    urlConnection.setReadTimeout(5000);

                    BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    reader.close();

                    String response = result.toString();
                    Log.d("ThingSpeak", "Datos obtenidos: " + response);

                    List<String> nuevosDatos = new ArrayList<>();
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONArray feeds = jsonResponse.getJSONArray("feeds");

                    SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                    isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

                    DateFormatSymbols dfs = new DateFormatSymbols(new Locale("es", "ES"));
                    dfs.setWeekdays(new String[]{"", "Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"});
                    SimpleDateFormat desiredFormat = new SimpleDateFormat("EEEE d 'de' MMMM HH:mm:ss", new Locale("es", "ES"));
                    desiredFormat.setDateFormatSymbols(dfs);

                    for (int i = 0; i < feeds.length(); i++) {
                        JSONObject feed = feeds.getJSONObject(i);
                        String createdAt = feed.getString("created_at");
                        String field1 = feed.getString("field1");

                        Date date = isoFormat.parse(createdAt);
                        String formattedDate = desiredFormat.format(date);

                        nuevosDatos.add("Fecha: " + formattedDate + ", Código: " + field1);
                    }

                    activity.runOnUiThread(() -> {
                        datos.clear();
                        datos.addAll(nuevosDatos);
                        adapter.notifyDataSetChanged();
                    });

                    urlConnection.disconnect();
                } catch (Exception e) {
                    Log.e("ThingSpeak", "Error al leer datos", e);
                }
            }).start();
        }
    }
}
