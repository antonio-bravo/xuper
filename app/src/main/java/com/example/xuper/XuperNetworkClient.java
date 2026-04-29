package com.example.xuper;

import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Cliente para manejar la conexión con el servidor de Xuper TV.
 */
public class XuperNetworkClient {

    private String baseUrl;
    private final OkHttpClient client;
    private final Context context;

    public XuperNetworkClient(Context context) {
        this.context = context;
        this.client = new OkHttpClient();
        
        // Cargar la URL configurada (o usar la original por defecto)
        SharedPreferences prefs = context.getSharedPreferences("XuperPrefs", Context.MODE_PRIVATE);
        this.baseUrl = prefs.getString("api_url", "https://api.thexupertv.com/api/v1/");
    }

    /**
     * Realiza el inicio de sesión automático usando las credenciales guardadas.
     */
    public void iniciarSesion(String deviceId) {
        SharedPreferences prefs = context.getSharedPreferences("XuperPrefs", Context.MODE_PRIVATE);
        String usuario = prefs.getString("user", "");
        String contrasena = prefs.getString("pass", "");

        if (usuario.isEmpty() || contrasena.isEmpty()) {
            System.err.println("Xuper: Credenciales no configuradas.");
            return;
        }

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("account", usuario);
            jsonBody.put("password", contrasena);
            jsonBody.put("device_id", deviceId);

            RequestBody body = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(baseUrl + "login")
                    .post(body)
                    .addHeader("Accept", "application/json")
                    .addHeader("X-App-Id", "thexupertv")
                    .addHeader("X-App-Version", "4.34.3")
                    .addHeader("X-Platform", "android")
                    .addHeader("X-Device-Id", deviceId)
                    .addHeader("User-Agent", "XuperTV/4.34.3 (Android)")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    System.err.println("Xuper Login Fallido: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String result = response.body().string();
                        // Aquí puedes procesar el token de acceso recibido
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Obtiene la lista de contenido para mostrarla en tu App.
     */
    public void obtenerListaContenido(Callback callback) {
        Request request = new Request.Builder()
                .url(baseUrl + "get_list") // Basado en el patrón de la API
                .get()
                .addHeader("X-App-Version", "4.34.3")
                .build();
        client.newCall(request).enqueue(callback);
    }
}
