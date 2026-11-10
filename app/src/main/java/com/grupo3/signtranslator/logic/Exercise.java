package com.grupo3.signtranslator.logic;

import com.grupo3.signtranslator.R;

/**
 * Ejercicios que la app sabe contar y corregir. Los ángulos son grados en la articulación
 * principal de cada ejercicio. "Abajo" y "arriba" son las dos fases de la repetición.
 */
public enum Exercise {

    NONE(R.string.exercise_none, 0f, 0f, 0f, 0),
    /** Rodilla: abajo < 110°, arriba > 155°. */
    SQUAT(R.string.exercise_squat, 110f, 155f, 100f, R.string.alert_squat_deeper),
    /** Codo: abajo < 100°, arriba > 150°. */
    PUSHUP(R.string.exercise_pushup, 100f, 150f, 90f, R.string.alert_pushup_deeper);

    /** Ejercicios que el usuario puede elegir a mano. La posición 0 del selector es "automático". */
    public static final Exercise[] MANUAL_CHOICES = {SQUAT, PUSHUP};

    private final int nameRes;
    /** Ángulo articular que marca la fase "abajo" de la repetición. */
    private final float downAngle;
    /** Ángulo articular que marca la fase "arriba"; al cruzarlo se completa la repetición. */
    private final float upAngle;
    /** Ángulo mínimo que debe alcanzar la repetición; si no lo alcanza, se pide más amplitud. */
    private final float deepAngle;
    private final int depthAlertRes;

    Exercise(int nameRes, float downAngle, float upAngle, float deepAngle, int depthAlertRes) {
        this.nameRes = nameRes;
        this.downAngle = downAngle;
        this.upAngle = upAngle;
        this.deepAngle = deepAngle;
        this.depthAlertRes = depthAlertRes;
    }

    public int getNameRes() {
        return nameRes;
    }

    public float getDownAngle() {
        return downAngle;
    }

    public float getUpAngle() {
        return upAngle;
    }

    public float getDeepAngle() {
        return deepAngle;
    }

    public int getDepthAlertRes() {
        return depthAlertRes;
    }
}
