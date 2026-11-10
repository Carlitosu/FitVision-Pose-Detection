package com.grupo3.signtranslator.camera;

import android.annotation.SuppressLint;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetector;
import com.grupo3.signtranslator.ui.GraphicOverlay;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Analizador de CameraX: recibe cada frame de la cámara, lo envía a ML Kit
 * (modo STREAM), limpia el {@link GraphicOverlay} y entrega la pose detectada al callback,
 * que decide cómo dibujarla.
 *
 * <p>Procesa un frame a la vez: si ML Kit aún trabaja en el anterior, el nuevo se descarta.
 * Así la traducción se mantiene al día en lugar de acumular retraso.
 */
public class PoseAnalyzer implements ImageAnalysis.Analyzer {

    private static final String TAG = "PoseAnalyzer";

    private final PoseDetector detector;
    private final GraphicOverlay overlay;
    private final boolean frontCamera;
    private final Consumer<Pose> onPose;
    private final AtomicBoolean busy = new AtomicBoolean(false);

    public PoseAnalyzer(PoseDetector detector, GraphicOverlay overlay,
                        boolean frontCamera, Consumer<Pose> onPose) {
        this.detector = detector;
        this.overlay = overlay;
        this.frontCamera = frontCamera;
        this.onPose = onPose;
    }

    @Override
    @SuppressLint("UnsafeOptInUsageError") // ImageProxy.getImage() es experimental en CameraX
    public void analyze(@NonNull ImageProxy imageProxy) {
        if (!busy.compareAndSet(false, true)) {
            // ML Kit sigue con el frame anterior: se descarta este para no acumular retraso
            imageProxy.close();
            return;
        }

        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            busy.set(false);
            imageProxy.close();
            return;
        }

        int rotation = imageProxy.getImageInfo().getRotationDegrees();
        InputImage inputImage = InputImage.fromMediaImage(mediaImage, rotation);

        // Con rotación de 90° o 270° el ancho y el alto de la imagen ya rotada se intercambian
        boolean swap = rotation == 90 || rotation == 270;
        int width = swap ? mediaImage.getHeight() : mediaImage.getWidth();
        int height = swap ? mediaImage.getWidth() : mediaImage.getHeight();

        detector.process(inputImage)
                .addOnSuccessListener(pose -> {
                    overlay.setImageSourceInfo(width, height, frontCamera);
                    overlay.clear();
                    onPose.accept(pose);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error al detectar la pose", e))
                .addOnCompleteListener(task -> {
                    busy.set(false);
                    imageProxy.close();
                });
    }
}
