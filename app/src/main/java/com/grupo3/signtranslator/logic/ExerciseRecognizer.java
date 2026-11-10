package com.grupo3.signtranslator.logic;

import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

/**
 * Reconoce el ejercicio automáticamente a partir de la orientación del torso:
 * torso casi horizontal → flexión; torso casi vertical con piernas visibles → sentadilla.
 * Exige varios frames seguidos con la misma clasificación para no cambiar de ejercicio
 * por un parpadeo de la detección.
 */
public final class ExerciseRecognizer {

    /** Frames consecutivos necesarios para confirmar un cambio de ejercicio. */
    private static final int CONFIRM_FRAMES = 8;
    /** Torso más inclinado que esto (grados desde la vertical) indica flexión. */
    private static final float PUSHUP_LEAN_DEG = 65f;
    /** Torso más vertical que esto, con piernas visibles, indica sentadilla. */
    private static final float SQUAT_LEAN_DEG = 45f;

    private Exercise confirmed = Exercise.NONE;
    private Exercise candidate = Exercise.NONE;
    private int streak = 0;

    /** Devuelve el ejercicio confirmado después de procesar este frame. */
    public Exercise update(Pose pose) {
        Exercise raw = classify(pose);
        if (raw == Exercise.NONE) {
            streak = 0;
            candidate = Exercise.NONE;
            return confirmed;
        }
        if (raw == candidate) {
            streak++;
        } else {
            candidate = raw;
            streak = 1;
        }
        if (streak >= CONFIRM_FRAMES) {
            confirmed = raw;
        }
        return confirmed;
    }

    public void reset() {
        confirmed = Exercise.NONE;
        candidate = Exercise.NONE;
        streak = 0;
    }

    /** Clasificación de un único frame, sin historial. */
    public static Exercise classify(Pose pose) {
        Float lean = PoseMath.torsoLeanDegrees(pose);
        if (lean == null) {
            return Exercise.NONE;
        }
        if (lean >= PUSHUP_LEAN_DEG) {
            return Exercise.PUSHUP;
        }
        if (lean <= SQUAT_LEAN_DEG && legsVisible(pose)) {
            return Exercise.SQUAT;
        }
        return Exercise.NONE;
    }

    private static boolean legsVisible(Pose pose) {
        return PoseMath.bilateralAngle(pose,
                PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE,
                PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE) != null;
    }
}
