package com.grupo3.signtranslator;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.ZoomState;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetector;
import com.grupo3.signtranslator.camera.PoseAnalyzer;
import com.grupo3.signtranslator.databinding.ActivityLivePreviewBinding;
import com.grupo3.signtranslator.logic.Exercise;
import com.grupo3.signtranslator.logic.ExerciseRecognizer;
import com.grupo3.signtranslator.logic.PostureAnalyzer;
import com.grupo3.signtranslator.logic.RepCounter;
import com.grupo3.signtranslator.ml.PoseDetectorProvider;
import com.grupo3.signtranslator.tts.SpanishNumbers;
import com.grupo3.signtranslator.tts.SpeechManager;
import com.grupo3.signtranslator.ui.ExerciseSpinner;
import com.grupo3.signtranslator.ui.GraphicOverlay;
import com.grupo3.signtranslator.ui.PoseGraphic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Entrenamiento en vivo: CameraX alimenta ML Kit en modo STREAM, se calculan los ángulos
 * articulares, se cuentan las repeticiones (con voz) y se muestran las correcciones de postura.
 */
public class LivePreviewActivity extends AppCompatActivity {

    private static final String TAG = "LivePreview";

    /** Resolución de análisis: suficiente para el cuerpo completo y más rápida que la nativa. */
    private static final Size ANALYSIS_SIZE = new Size(640, 480);
    /** Tiempo mínimo entre dos correcciones habladas, para no saturar al usuario. */
    private static final long ALERT_COOLDOWN_MS = 4000L;

    private ActivityLivePreviewBinding binding;
    private PoseDetector poseDetector;
    private final ExerciseRecognizer recognizer = new ExerciseRecognizer();
    private final RepCounter counter = new RepCounter();
    private SpeechManager speechManager;
    private ExecutorService analysisExecutor;
    private ProcessCameraProvider cameraProvider;

    // Variables para el cronómetro interactivo en vivo
    private Handler handler;
    private Runnable runnable;
    private int segundosTranscurridos = 0;
    private boolean estaCorriendo = false;

    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private boolean voiceEnabled = true;
    /** null = el ejercicio se detecta solo; de lo contrario, el que eligió el usuario. */
    private Exercise manualExercise = null;
    private Exercise activeExercise = Exercise.NONE;
    private long lastAlertTime = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLivePreviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // FIT_CENTER muestra la imagen completa (con bandas) en lugar de recortar los costados
        binding.previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
        binding.graphicOverlay.setScaleMode(GraphicOverlay.FIT_CENTER);

        poseDetector = PoseDetectorProvider.createStreamDetector();
        speechManager = new SpeechManager(this);
        analysisExecutor = Executors.newSingleThreadExecutor();

        // Inicializar Cronómetro
        handler = new Handler(Looper.getMainLooper());
        configurarCronometro();

        ExerciseSpinner.bind(this, binding.spinnerExercise, this::onManualExerciseChanged);
        binding.btnControls.setOnClickListener(v -> toggleControls());
        binding.btnReset.setOnClickListener(v -> resetCounter());
        binding.btnSwitchCamera.setOnClickListener(v -> switchCamera());
        binding.btnToggleVoice.setOnClickListener(v -> toggleVoice());
        updateVoiceButton();

        boolean granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        if (granted) {
            startCamera();
        } else {
            Toast.makeText(this, R.string.permission_camera_denied, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @SuppressWarnings("SetTextI18n")
    private void configurarCronometro() {
        runnable = new Runnable() {
            @Override
            public void run() {
                if (estaCorriendo) {
                    segundosTranscurridos++;
                    int minutos = segundosTranscurridos / 60;
                    int segundos = segundosTranscurridos % 60;
                    String tiempoFormateado = String.format(Locale.getDefault(), "%02d:%02d", minutos, segundos);
                    binding.tvCronometro.setText(tiempoFormateado);
                    handler.postDelayed(this, 1000);
                }
            }
        };

        binding.btnStartCronometro.setOnClickListener(v -> {
            if (!estaCorriendo) {
                estaCorriendo = true;
                handler.post(runnable);
            }
        });

        binding.btnPauseCronometro.setOnClickListener(v -> {
            estaCorriendo = false;
            handler.removeCallbacks(runnable);
        });

        binding.btnResetCronometro.setOnClickListener(v -> {
            estaCorriendo = false;
            handler.removeCallbacks(runnable);
            segundosTranscurridos = 0;
            binding.tvCronometro.setText("00:00");
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "No se obtuvo el proveedor de cámara", e);
                Toast.makeText(this, R.string.error_camera, Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Vincula vista previa y análisis al ciclo de vida. Usa formato 4:3 (más alto que el 16:9
     * por defecto, así cabe el cuerpo completo) y la lente de mayor campo de visión disponible.
     */
    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    private void bindCameraUseCases() {
        if (cameraProvider == null) {
            return;
        }
        cameraProvider.unbindAll();

        ResolutionSelector previewResolution = new ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .build();
        Preview preview = new Preview.Builder()
                .setResolutionSelector(previewResolution)
                .build();
        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

        ResolutionSelector analysisResolution = new ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(new ResolutionStrategy(
                        ANALYSIS_SIZE, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                .build();
        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setResolutionSelector(analysisResolution)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        boolean frontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT;
        analysis.setAnalyzer(analysisExecutor,
                new PoseAnalyzer(poseDetector, binding.graphicOverlay, frontCamera, this::onPoseResult));

        CameraSelector selector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .addCameraFilter(LivePreviewActivity::pickWidestCamera)
                .build();
        try {
            Camera camera = cameraProvider.bindToLifecycle(this, selector, preview, analysis);
            // Zoom mínimo = máximo campo de visión que ofrece el hardware
            ZoomState zoom = camera.getCameraInfo().getZoomState().getValue();
            if (zoom != null) {
                camera.getCameraControl().setZoomRatio(zoom.getMinZoomRatio());
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "La cámara seleccionada no está disponible", e);
            Toast.makeText(this, R.string.error_camera, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Entre las cámaras disponibles con la misma orientación, elige la de menor distancia focal
     * (la de gran angular). Si no hay datos, conserva la lista original.
     */
    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    private static List<CameraInfo> pickWidestCamera(List<CameraInfo> infos) {
        CameraInfo widest = null;
        float shortest = Float.MAX_VALUE;
        for (CameraInfo info : infos) {
            float focal = shortestFocalLength(info);
            if (focal < shortest) {
                shortest = focal;
                widest = info;
            }
        }
        return widest == null ? infos : Collections.singletonList(widest);
    }

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    private static float shortestFocalLength(CameraInfo info) {
        try {
            float[] focals = Camera2CameraInfo.from(info)
                    .getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
            if (focals == null || focals.length == 0) {
                return Float.MAX_VALUE;
            }
            float min = focals[0];
            for (float f : focals) {
                min = Math.min(min, f);
            }
            return min;
        } catch (IllegalStateException e) {
            return Float.MAX_VALUE;
        }
    }

    /** Se ejecuta en cada frame analizado; el overlay ya fue limpiado por el analizador. */
    private void onPoseResult(Pose pose) {
        boolean personFound = !pose.getAllPoseLandmarks().isEmpty();
        binding.tvStatus.setText(personFound ? R.string.status_detected : R.string.status_searching);

        Exercise detected = recognizer.update(pose);
        Exercise target = manualExercise != null ? manualExercise : detected;
        if (target != Exercise.NONE && target != activeExercise) {
            activeExercise = target;
            counter.setExercise(target);
            binding.tvReps.setText("0");
        }

        if (activeExercise == Exercise.NONE) {
            binding.tvExercise.setText(R.string.exercise_none);
            binding.tvAngle.setText(R.string.angle_empty);
            binding.tvFeedback.setText(R.string.waiting_exercise);
            drawPose(pose, false);
            return;
        }

        binding.tvExercise.setText(activeExercise.getNameRes());
        PostureAnalyzer.Report report = PostureAnalyzer.analyze(activeExercise, pose);
        List<Integer> alerts = new ArrayList<>(report.alerts);

        if (report.angle != null) {
            binding.tvAngle.setText(getString(R.string.angle_format, Math.round(report.angle)));
            if (counter.update(report.angle)) {
                onRepCompleted(alerts);
            }
        } else {
            binding.tvAngle.setText(R.string.angle_empty);
        }

        drawPose(pose, !alerts.isEmpty());
        showFeedback(alerts);
    }

    /** Actualiza el contador, dice el número en voz alta y añade la corrección de profundidad. */
    private void onRepCompleted(List<Integer> alerts) {
        int reps = counter.getReps();
        binding.tvReps.setText(String.valueOf(reps));
        if (voiceEnabled) {
            speechManager.speak(SpanishNumbers.exclamation(reps));
        }
        if (counter.getLastRepMin() > activeExercise.getDeepAngle()) {
            alerts.add(0, activeExercise.getDepthAlertRes());
        }
    }

    /** Muestra la corrección más importante y la dice en voz alta, con un intervalo mínimo. */
    private void showFeedback(List<Integer> alerts) {
        if (alerts.isEmpty()) {
            binding.tvFeedback.setText(R.string.feedback_ok);
            return;
        }
        int alertRes = alerts.get(0);
        binding.tvFeedback.setText(alertRes);
        long now = SystemClock.elapsedRealtime();
        if (voiceEnabled && now - lastAlertTime > ALERT_COOLDOWN_MS) {
            speechManager.speakQueued(getString(alertRes));
            lastAlertTime = now;
        }
    }

    private void drawPose(Pose pose, boolean warning) {
        binding.graphicOverlay.add(new PoseGraphic(binding.graphicOverlay, pose, warning));
        binding.graphicOverlay.postInvalidate();
    }

    /** Cambia el ejercicio desde el selector; null vuelve a la detección automática. */
    private void onManualExerciseChanged(Exercise manual) {
        manualExercise = manual;
        // Fuerza la reevaluación en el siguiente frame
        activeExercise = Exercise.NONE;
        resetCounter();
    }

    private void resetCounter() {
        counter.reset();
        binding.tvReps.setText("0");
    }

    private void switchCamera() {
        lensFacing = lensFacing == CameraSelector.LENS_FACING_BACK
                ? CameraSelector.LENS_FACING_FRONT
                : CameraSelector.LENS_FACING_BACK;
        recognizer.reset();
        activeExercise = Exercise.NONE;
        resetCounter();
        bindCameraUseCases();
    }

    /** Muestra u oculta el panel de controles para dejar libre la vista de la cámara. */
    private void toggleControls() {
        boolean show = binding.controlsPanel.getVisibility() != View.VISIBLE;
        binding.controlsPanel.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnControls.setText(show ? R.string.btn_controls_hide : R.string.btn_controls_show);
    }

    private void toggleVoice() {
        voiceEnabled = !voiceEnabled;
        if (!voiceEnabled) {
            speechManager.stop();
        }
        updateVoiceButton();
    }

    private void updateVoiceButton() {
        binding.btnToggleVoice.setText(voiceEnabled ? R.string.btn_voice_on : R.string.btn_voice_off);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        if (poseDetector != null) {
            poseDetector.close();
        }
        if (speechManager != null) {
            speechManager.shutdown();
        }
        if (analysisExecutor != null) {
            analysisExecutor.shutdown();
        }
    }
}