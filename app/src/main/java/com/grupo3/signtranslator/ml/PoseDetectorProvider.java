package com.grupo3.signtranslator.ml;

import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

/**
 * Crea los detectores de ML Kit Pose Detection.
 * <ul>
 *   <li>STREAM_MODE: para video en vivo (usa tracking entre frames, más rápido).</li>
 *   <li>SINGLE_IMAGE_MODE: para fotos y galería (analiza cada imagen de forma independiente).</li>
 * </ul>
 */
public final class PoseDetectorProvider {

    private PoseDetectorProvider() {
    }

    public static PoseDetector createStreamDetector() {
        PoseDetectorOptions options = new PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build();
        return PoseDetection.getClient(options);
    }

    public static PoseDetector createStaticDetector() {
        PoseDetectorOptions options = new PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE)
                .build();
        return PoseDetection.getClient(options);
    }
}
