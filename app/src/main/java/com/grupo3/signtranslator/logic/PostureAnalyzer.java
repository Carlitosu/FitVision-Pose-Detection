package com.grupo3.signtranslator.logic;

import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.grupo3.signtranslator.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Calcula el ángulo principal de cada ejercicio y las correcciones de postura de un frame.
 * Las correcciones son heurísticas y deben calibrarse con grabaciones reales.
 */
public final class PostureAnalyzer {

    /** Inclinación máxima del torso en sentadilla (grados desde la vertical). */
    private static final float MAX_SQUAT_LEAN_DEG = 50f;
    /** Ángulo cadera-tobillo mínimo en flexión; 180° significa cuerpo recto. */
    private static final float MIN_PUSHUP_ALIGNMENT_DEG = 150f;

    private PostureAnalyzer() {
    }

    /** Resultado de un frame: ángulo principal (puede ser null) y correcciones pendientes. */
    public static final class Report {
        public final Float angle;
        /** Identificadores de cadenas (R.string) de las correcciones, la más importante primero. */
        public final List<Integer> alerts;

        Report(Float angle, List<Integer> alerts) {
            this.angle = angle;
            this.alerts = alerts;
        }
    }

    public static Report analyze(Exercise exercise, Pose pose) {
        List<Integer> alerts = new ArrayList<>();
        Float angle = null;
        switch (exercise) {
            case SQUAT:
                angle = PoseMath.bilateralAngle(pose,
                        PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE,
                        PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE);
                Float lean = PoseMath.torsoLeanDegrees(pose);
                if (lean != null && lean > MAX_SQUAT_LEAN_DEG) {
                    alerts.add(R.string.alert_back_straight);
                }
                break;
            case PUSHUP:
                angle = PoseMath.bilateralAngle(pose,
                        PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST,
                        PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST);
                Float alignment = PoseMath.bilateralAngle(pose,
                        PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_ANKLE,
                        PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_ANKLE);
                if (alignment != null && alignment < MIN_PUSHUP_ALIGNMENT_DEG) {
                    alerts.add(R.string.alert_body_straight);
                }
                break;
            default:
                break;
        }
        return new Report(angle, alerts);
    }
}
