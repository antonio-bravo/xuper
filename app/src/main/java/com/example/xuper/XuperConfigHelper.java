package com.example.xuper;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Clase auxiliar para gestionar los ajustes de Xuper TV desde la UI.
 */
public class XuperConfigHelper {

    private static final String PREFS_NAME = "XuperPrefs";

    /**
     * Guarda los datos de conexión. Úsalo en tu pantalla de configuración.
     */
    public static void guardarAjustes(Context context, String url, String usuario, String password) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        if (url != null && !url.isEmpty()) {
            editor.putString("api_url", url);
        }
        editor.putString("user", usuario);
        editor.putString("pass", password);
        editor.apply();
    }

    /**
     * Recupera los ajustes actuales.
     */
    public static String[] obtenerAjustes(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return new String[] {
            prefs.getString("api_url", "https://api.thexupertv.com/api/v1/"),
            prefs.getString("user", ""),
            prefs.getString("pass", "")
        };
    }
}