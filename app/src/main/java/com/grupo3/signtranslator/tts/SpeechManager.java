package com.grupo3.signtranslator.tts;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

/**
 * Envoltorio de {@link TextToSpeech} que pronuncia en español: conteo de repeticiones y
 * correcciones de postura. Si el motor aún no está listo, guarda el último texto y lo dice al inicializarse.
 */
public class SpeechManager implements TextToSpeech.OnInitListener {

    private static final String UTTERANCE_ID = "posture-coach";

    private final TextToSpeech tts;
    private boolean ready = false;
    private String pendingText;

    public SpeechManager(Context context) {
        tts = new TextToSpeech(context.getApplicationContext(), this);
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) {
            return;
        }
        int result = tts.setLanguage(Locale.forLanguageTag("es-MX"));
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            result = tts.setLanguage(Locale.forLanguageTag("es"));
        }
        ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
        if (ready && pendingText != null) {
            speak(pendingText);
            pendingText = null;
        }
    }

    /** Pronuncia el texto interrumpiendo cualquier frase anterior (p. ej. un conteo). */
    public void speak(String text) {
        enqueue(text, TextToSpeech.QUEUE_FLUSH);
    }

    /** Pronuncia el texto después de lo que ya suena, sin interrumpirlo (p. ej. una corrección). */
    public void speakQueued(String text) {
        enqueue(text, TextToSpeech.QUEUE_ADD);
    }

    private void enqueue(String text, int queueMode) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (!ready) {
            pendingText = text;
            return;
        }
        tts.speak(text, queueMode, null, UTTERANCE_ID);
    }

    /** Detiene la voz en curso sin liberar el motor. */
    public void stop() {
        pendingText = null;
        tts.stop();
    }

    /** Libera el motor de TTS. Llamar desde onDestroy. */
    public void shutdown() {
        tts.stop();
        tts.shutdown();
    }
}
