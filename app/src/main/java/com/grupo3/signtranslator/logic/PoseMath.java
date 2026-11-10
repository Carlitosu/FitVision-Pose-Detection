package com.grupo3.signtranslator.logic;

import android.graphics.PointF;

import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

/**
 * Geometría sobre los puntos corporales de ML Kit Pose Detection (hombros, codos, muñecas,
 * caderas, rodillas, tobillos y pies). No se usa ningún punto de manos ni dedos.
 */
public final class PoseMath {

    /** Confianza mínima para considerar visible un punto. */
    public static final float MIN_LIKELIHOOD = 0.5f;

    private PoseMath() {
    }

    /** Posición del punto en píxeles de la imagen, o null si no es visible. */
    public static PointF point(Pose pose, int type) {
        PoseLandmark landmark = pose.getPoseLandmark(type);
        if (landmark == null || landmark.getInFrameLikelihood() < MIN_LIKELIHOOD) {
            return null;
        }
        return landmark.getPosition();
    }

    /** Ángulo en grados formado por A-B-C con vértice en B, o null si falta algún punto. */
    public static Float angle(Pose pose, int typeA, int typeB, int typeC) {
        PointF a = point(pose, typeA);
        PointF b = point(pose, typeB);
        PointF c = point(pose, typeC);
        if (a == null || b == null || c == null) {
            return null;
        }
        return angle(a, b, c);
    }

    /**
     * Ángulo en grados en B usando el producto punto: cos θ = (BA · BC) / (|BA| |BC|).
     * Si los dos segmentos son degenerados (punto repetido), devuelve 180° (cuerpo recto).
     */
    public static float angle(PointF a, PointF b, PointF c) {
        double bax = a.x - b.x;
        double bay = a.y - b.y;
        double bcx = c.x - b.x;
        double bcy = c.y - b.y;
        double norm = Math.hypot(bax, bay) * Math.hypot(bcx, bcy);
        if (norm < 1e-6) {
            return 180f;
        }
        double cos = (bax * bcx + bay * bcy) / norm;
        cos = Math.max(-1.0, Math.min(1.0, cos));
        return (float) Math.toDegrees(Math.acos(cos));
    }

    /**
     * Promedio del ángulo del lado izquierdo y del derecho. Si uno de los lados no es visible
     * (lo habitual en vista lateral), usa solo el visible. Devuelve null si ninguno lo es.
     */
    public static Float bilateralAngle(Pose pose,
                                       int leftA, int leftB, int leftC,
                                       int rightA, int rightB, int rightC) {
        return average(angle(pose, leftA, leftB, leftC), angle(pose, rightA, rightB, rightC));
    }

    /**
     * Inclinación del torso en grados respecto a la vertical: 0° = de pie, 90° = horizontal.
     * Se mide desde el punto medio de las caderas hasta el de los hombros.
     */
    public static Float torsoLeanDegrees(Pose pose) {
        PointF leftShoulder = point(pose, PoseLandmark.LEFT_SHOULDER);
        PointF rightShoulder = point(pose, PoseLandmark.RIGHT_SHOULDER);
        PointF leftHip = point(pose, PoseLandmark.LEFT_HIP);
        PointF rightHip = point(pose, PoseLandmark.RIGHT_HIP);
        if (leftShoulder == null || rightShoulder == null || leftHip == null || rightHip == null) {
            return null;
        }
        float dx = (leftShoulder.x + rightShoulder.x) / 2f - (leftHip.x + rightHip.x) / 2f;
        float dy = (leftShoulder.y + rightShoulder.y) / 2f - (leftHip.y + rightHip.y) / 2f;
        return (float) Math.toDegrees(Math.atan2(Math.abs(dx), Math.abs(dy)));
    }

    private static Float average(Float a, Float b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return (a + b) / 2f;
    }
}
