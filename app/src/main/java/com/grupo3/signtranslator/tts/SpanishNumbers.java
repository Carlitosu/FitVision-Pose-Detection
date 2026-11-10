package com.grupo3.signtranslator.tts;

/** Convierte el número de una repetición en la exclamación que se pronuncia: "¡Uno!", "¡Dos!"… */
public final class SpanishNumbers {

    private static final String[] WORDS = {
            "Cero", "Uno", "Dos", "Tres", "Cuatro", "Cinco", "Seis", "Siete", "Ocho", "Nueve",
            "Diez", "Once", "Doce", "Trece", "Catorce", "Quince", "Dieciséis", "Diecisiete",
            "Dieciocho", "Diecinueve", "Veinte"
    };

    private SpanishNumbers() {
    }

    /** Desde 21 en adelante se pronuncian las cifras. */
    public static String exclamation(int n) {
        String word = n >= 0 && n < WORDS.length ? WORDS[n] : String.valueOf(n);
        return "¡" + word + "!";
    }
}
