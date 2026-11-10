package com.grupo3.signtranslator.logic;

/**
 * Contador de repeticiones con máquina de estados: START → UP → DOWN → UP (repetición).
 * Suaviza el ángulo con un promedio exponencial para absorber el ruido de la detección.
 */
public final class RepCounter {

    /** Peso de la muestra nueva en el suavizado (1 = sin suavizar). */
    private static final float SMOOTHING = 0.5f;

    private enum Phase { START, UP, DOWN }

    private Exercise exercise = Exercise.NONE;
    private Phase phase = Phase.START;
    private float smoothed = Float.NaN;
    private float minInRep = Float.NaN;
    private float lastRepMin = Float.NaN;
    private int reps = 0;

    /** Cambia el ejercicio y reinicia el conteo. */
    public void setExercise(Exercise exercise) {
        this.exercise = exercise;
        reset();
    }

    /** Pone el contador a cero sin cambiar el ejercicio. */
    public void reset() {
        phase = Phase.START;
        smoothed = Float.NaN;
        minInRep = Float.NaN;
        lastRepMin = Float.NaN;
        reps = 0;
    }

    /**
     * Procesa el ángulo articular de un frame.
     * @return true si este frame completó una repetición
     */
    public boolean update(float angle) {
        smoothed = Float.isNaN(smoothed) ? angle : smoothed + SMOOTHING * (angle - smoothed);
        switch (phase) {
            case START:
                // Solo empieza a contar cuando la persona está arriba; evita contar desde abajo
                if (smoothed >= exercise.getUpAngle()) {
                    phase = Phase.UP;
                }
                return false;
            case UP:
                if (smoothed <= exercise.getDownAngle()) {
                    phase = Phase.DOWN;
                    minInRep = smoothed;
                }
                return false;
            case DOWN:
                minInRep = Math.min(minInRep, smoothed);
                if (smoothed >= exercise.getUpAngle()) {
                    phase = Phase.UP;
                    reps++;
                    lastRepMin = minInRep;
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    public int getReps() {
        return reps;
    }

    /** Ángulo mínimo alcanzado en la última repetición completada (NaN si no hay ninguna). */
    public float getLastRepMin() {
        return lastRepMin;
    }
}
