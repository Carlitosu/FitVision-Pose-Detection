package com.grupo3.signtranslator.ui;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.grupo3.signtranslator.R;
import com.grupo3.signtranslator.logic.Exercise;

/**
 * Llena un {@link Spinner} con "Automático" y los ejercicios de {@link Exercise#MANUAL_CHOICES}.
 * El listener recibe null cuando se elige "Automático".
 */
public final class ExerciseSpinner {

    /** Recibe el ejercicio elegido a mano, o null si el ejercicio se detecta automáticamente. */
    public interface Listener {
        void onManualExerciseChanged(Exercise manual);
    }

    private ExerciseSpinner() {
    }

    public static void bind(Context context, Spinner spinner, Listener listener) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                context, R.array.exercise_choices, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            // El Spinner notifica también la selección inicial; solo actuamos ante un cambio real
            private int lastPosition = 0;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == lastPosition) {
                    return;
                }
                lastPosition = position;
                listener.onManualExerciseChanged(position == 0 ? null : Exercise.MANUAL_CHOICES[position - 1]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Sin acción: el spinner siempre tiene una opción seleccionada
            }
        });
    }
}
