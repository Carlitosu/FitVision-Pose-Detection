package com.grupo3.signtranslator.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Guarda la preferencia de modo claro u oscuro. Si el usuario nunca la eligió, la app sigue
 * el modo del sistema. AppCompatDelegate aplica el cambio a todas las actividades.
 */
public final class ThemeManager {

    private static final String PREFS = "theme_prefs";
    private static final String KEY_DARK = "dark_mode";

    private ThemeManager() {
    }

    /** Aplica el modo guardado al iniciar la app. Sin preferencia, sigue al sistema. */
    public static void applySavedMode(Context context) {
        SharedPreferences prefs = prefs(context);
        if (!prefs.contains(KEY_DARK)) {
            return;
        }
        AppCompatDelegate.setDefaultNightMode(prefs.getBoolean(KEY_DARK, false)
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }

    /** Indica si la interfaz se está mostrando en modo oscuro en este momento. */
    public static boolean isDarkNow(Context context) {
        int mode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    /** Guarda la preferencia y cambia el tema de inmediato. */
    public static void setDark(Context context, boolean dark) {
        prefs(context).edit().putBoolean(KEY_DARK, dark).apply();
        AppCompatDelegate.setDefaultNightMode(dark
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
