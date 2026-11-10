package com.grupo3.signtranslator;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetector;
import com.grupo3.signtranslator.databinding.ActivityStaticAnalysisBinding;
import com.grupo3.signtranslator.logic.Exercise;
import com.grupo3.signtranslator.logic.ExerciseRecognizer;
import com.grupo3.signtranslator.logic.PostureAnalyzer;
import com.grupo3.signtranslator.logic.RepCounter;
import com.grupo3.signtranslator.ml.PoseDetectorProvider;
import com.grupo3.signtranslator.tts.SpeechManager;
import com.grupo3.signtranslator.ui.ExerciseSpinner;
import com.grupo3.signtranslator.ui.GraphicOverlay;
import com.grupo3.signtranslator.ui.PoseGraphic;
import com.grupo3.signtranslator.util.ImageUtils;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Analiza una foto o un video de la galería con ML Kit en modo SINGLE_IMAGE.
 * Las fotos muestran el esqueleto y la postura; los videos se muestrean cada 250 ms
 * (hasta 60 s), cuentan repeticiones y reportan la corrección más frecuente.
 * El selector permite forzar el ejercicio y repite el análisis con la misma foto o video.
 */
public class StaticAnalysisActivity extends AppCompatActivity {

    private static final String TAG = "StaticAnalysis";
    private static final int MAX_IMAGE_SIZE = 1280;
    private static final int MAX_VIDEO_FRAME_SIZE = 720;
    private static final long FRAME_STEP_US = 250_000L;
    private static final long MAX_VIDEO_US = 60_000_000L;
    private static final int PROGRESS_EVERY_FRAMES = 10;

    private ActivityStaticAnalysisBinding binding;
    private PoseDetector poseDetector;
    private SpeechManager speechManager;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private String spokenSummary = "";

    private Uri sourceUri;
    private boolean sourceIsVideo;
    /** null = detección automática; de lo contrario, el ejercicio elegido en el selector. */
    private Exercise manualExercise = null;
    /** Última pose de la foto, para repetir el análisis al cambiar el ejercicio. */
    private Pose lastPose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStaticAnalysisBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        poseDetector = PoseDetectorProvider.createStaticDetector();
        speechManager = new SpeechManager(this);

        ExerciseSpinner.bind(this, binding.spinnerExercise, this::onManualExerciseChanged);
        binding.btnSpeak.setOnClickListener(v -> speechManager.speak(spokenSummary));
        binding.btnClose.setOnClickListener(v -> finish());

        sourceUri = getIntent().getData();
        if (sourceUri == null) {
            Toast.makeText(this, R.string.error_no_image, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        sourceIsVideo = isVideo(sourceUri);
        if (sourceIsVideo) {
            analyzeVideo(sourceUri);
        } else {
            loadAndAnalyzeImage(sourceUri);
        }
    }

    /** Cambia el ejercicio desde el selector y repite el análisis con el archivo ya cargado. */
    private void onManualExerciseChanged(Exercise manual) {
        manualExercise = manual;
        if (sourceIsVideo) {
            analyzeVideo(sourceUri);
        } else if (lastPose != null) {
            showImageResult(lastPose);
        }
    }

    private boolean isVideo(Uri uri) {
        String type = getContentResolver().getType(uri);
        return type != null && type.startsWith("video/");
    }

    /** La decodificación ocurre en segundo plano; el análisis y la UI, en el hilo principal. */
    private void loadAndAnalyzeImage(Uri uri) {
        binding.progress.setVisibility(View.VISIBLE);
        ioExecutor.execute(() -> {
            try {
                Bitmap bitmap = ImageUtils.loadBitmap(this, uri, MAX_IMAGE_SIZE);
                runOnUiThread(() -> analyzeImage(bitmap));
            } catch (IOException e) {
                Log.e(TAG, "No se pudo cargar la imagen", e);
                runOnUiThread(() -> showError(R.string.error_load_image));
            }
        });
    }

    private void analyzeImage(Bitmap bitmap) {
        binding.ivPhoto.setImageBitmap(bitmap);

        // La imagen se muestra con fitCenter, por eso el overlay usa FIT_CENTER
        binding.graphicOverlay.setScaleMode(GraphicOverlay.FIT_CENTER);
        binding.graphicOverlay.setImageSourceInfo(bitmap.getWidth(), bitmap.getHeight(), false);

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        poseDetector.process(image)
                .addOnSuccessListener(pose -> {
                    binding.progress.setVisibility(View.GONE);
                    lastPose = pose;
                    showImageResult(pose);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al analizar la imagen", e);
                    showError(R.string.error_detection);
                });
    }

    private void showImageResult(Pose pose) {
        binding.graphicOverlay.clear();
        if (pose.getAllPoseLandmarks().isEmpty()) {
            binding.tvExercise.setText(R.string.no_person);
            binding.tvDetail.setText("");
            binding.tvFeedback.setText("");
            spokenSummary = getString(R.string.no_person);
            return;
        }

        Exercise exercise = manualExercise != null ? manualExercise : ExerciseRecognizer.classify(pose);
        if (exercise == Exercise.NONE) {
            drawPose(pose, false);
            binding.tvExercise.setText(R.string.no_exercise);
            binding.tvDetail.setText("");
            binding.tvFeedback.setText("");
            spokenSummary = getString(R.string.no_exercise);
            return;
        }

        PostureAnalyzer.Report report = PostureAnalyzer.analyze(exercise, pose);
        drawPose(pose, !report.alerts.isEmpty());

        String name = getString(exercise.getNameRes());
        binding.tvExercise.setText(getString(R.string.result_exercise, name));
        binding.tvDetail.setText(report.angle == null
                ? getString(R.string.result_angle_unavailable)
                : getString(R.string.result_angle, Math.round(report.angle)));

        String feedback = joinAlerts(report.alerts);
        binding.tvFeedback.setText(feedback);
        spokenSummary = name + ". " + feedback;
        speechManager.speak(spokenSummary);
    }

    private void drawPose(Pose pose, boolean warning) {
        binding.graphicOverlay.add(new PoseGraphic(binding.graphicOverlay, pose, warning));
        binding.graphicOverlay.postInvalidate();
    }

    /** Muestrea el video en segundo plano, cuenta repeticiones y muestra el resumen al final. */
    private void analyzeVideo(Uri uri) {
        binding.progress.setVisibility(View.VISIBLE);
        binding.tvDetail.setText(R.string.static_analyzing);
        // Mientras se analiza no se puede cambiar el ejercicio, para no lanzar dos análisis a la vez
        binding.spinnerExercise.setEnabled(false);
        ioExecutor.execute(() -> {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(this, uri);
                long durationUs = readDurationUs(retriever);
                Bitmap thumbnail = retriever.getFrameAtTime(0, frameOption());
                if (thumbnail == null) {
                    throw new IOException("El video no tiene fotogramas");
                }
                Bitmap preview = ImageUtils.scaleDown(thumbnail, MAX_IMAGE_SIZE);
                runOnUiThread(() -> binding.ivPhoto.setImageBitmap(preview));

                VideoStats stats = scanVideo(retriever, Math.min(durationUs, MAX_VIDEO_US), manualExercise);
                runOnUiThread(() -> showVideoResult(stats));
            } catch (Exception e) {
                Log.e(TAG, "No se pudo analizar el video", e);
                runOnUiThread(() -> showError(R.string.error_video));
            } finally {
                releaseQuietly(retriever);
            }
        });
    }

    /**
     * Ejecuta en el hilo de fondo: un fotograma cada {@link #FRAME_STEP_US}.
     * Si {@code forced} no es null, ese es el ejercicio para todo el video.
     */
    private VideoStats scanVideo(MediaMetadataRetriever retriever, long limitUs, Exercise forced)
            throws Exception {
        VideoStats stats = new VideoStats();
        ExerciseRecognizer recognizer = new ExerciseRecognizer();
        RepCounter counter = new RepCounter();
        Exercise current = Exercise.NONE;

        for (long t = 0; t < limitUs; t += FRAME_STEP_US) {
            Bitmap frame = retriever.getFrameAtTime(t, frameOption());
            if (frame == null) {
                continue;
            }
            Bitmap scaled = ImageUtils.scaleDown(frame, MAX_VIDEO_FRAME_SIZE);
            if (scaled != frame) {
                frame.recycle();
            }
            Pose pose = Tasks.await(poseDetector.process(InputImage.fromBitmap(scaled, 0)));
            scaled.recycle();
            stats.frames++;

            Exercise detected = forced != null ? forced : recognizer.update(pose);
            if (detected != Exercise.NONE && detected != current) {
                // Al cambiar de ejercicio se guardan las repeticiones del anterior
                stats.reps += counter.getReps();
                counter.setExercise(detected);
                current = detected;
            }
            if (current != Exercise.NONE) {
                stats.addExerciseFrame(current);
                PostureAnalyzer.Report report = PostureAnalyzer.analyze(current, pose);
                for (int alert : report.alerts) {
                    stats.addAlert(alert);
                }
                if (report.angle != null && counter.update(report.angle)
                        && counter.getLastRepMin() > current.getDeepAngle()) {
                    stats.addAlert(current.getDepthAlertRes());
                }
            }

            if (stats.frames % PROGRESS_EVERY_FRAMES == 0) {
                int percent = (int) (100 * t / limitUs);
                runOnUiThread(() -> binding.tvDetail.setText(getString(R.string.static_video_progress, percent)));
            }
        }
        stats.reps += counter.getReps();
        stats.exercise = stats.dominantExercise();
        return stats;
    }

    @SuppressWarnings("SetTextI18n")
    private void showVideoResult(VideoStats stats) {
        binding.progress.setVisibility(View.GONE);
        binding.spinnerExercise.setEnabled(true);
        if (stats.exercise == Exercise.NONE) {
            binding.tvExercise.setText(R.string.no_exercise);
            binding.tvDetail.setText(getString(R.string.result_frames, stats.frames));
            binding.tvFeedback.setText("");
            spokenSummary = getString(R.string.no_exercise);
            return;
        }

        String name = getString(stats.exercise.getNameRes());
        binding.tvExercise.setText(getString(R.string.result_exercise, name));
        binding.tvDetail.setText(getString(R.string.result_reps, stats.reps) + "\n"
                + getString(R.string.result_frames, stats.frames));
        Integer topAlert = stats.topAlert();
        binding.tvFeedback.setText(topAlert == null ? getString(R.string.feedback_ok) : getString(topAlert));
        spokenSummary = name + ". " + getString(R.string.result_reps, stats.reps);
        speechManager.speak(spokenSummary);
    }

    private void showError(int messageRes) {
        binding.progress.setVisibility(View.GONE);
        binding.spinnerExercise.setEnabled(true);
        binding.tvDetail.setText(messageRes);
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show();
    }

    private String joinAlerts(List<Integer> alerts) {
        if (alerts.isEmpty()) {
            return getString(R.string.feedback_ok);
        }
        StringBuilder text = new StringBuilder();
        for (int alert : alerts) {
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append(getString(alert));
        }
        return text.toString();
    }

    private static void releaseQuietly(MediaMetadataRetriever retriever) {
        try {
            retriever.release();
        } catch (IOException e) {
            Log.w(TAG, "No se pudo liberar el lector de video", e);
        }
    }

    private static long readDurationUs(MediaMetadataRetriever retriever) {
        String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        try {
            return duration == null ? 0L : Long.parseLong(duration) * 1000L;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /** OPTION_CLOSEST (API 27+) busca el fotograma más cercano; en versiones anteriores solo keyframes. */
    @SuppressLint("InlinedApi")
    private static int frameOption() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
                ? MediaMetadataRetriever.OPTION_CLOSEST
                : MediaMetadataRetriever.OPTION_CLOSEST_SYNC;
    }

    /** Acumulado del análisis de un video. */
    private static final class VideoStats {
        int frames;
        int reps;
        Exercise exercise = Exercise.NONE;
        private final Map<Exercise, Integer> exerciseFrames = new EnumMap<>(Exercise.class);
        private final Map<Integer, Integer> alertCounts = new HashMap<>();

        void addExerciseFrame(Exercise exercise) {
            exerciseFrames.merge(exercise, 1, Integer::sum);
        }

        void addAlert(int alertRes) {
            alertCounts.merge(alertRes, 1, Integer::sum);
        }

        Exercise dominantExercise() {
            Exercise best = Exercise.NONE;
            int bestCount = 0;
            for (Map.Entry<Exercise, Integer> entry : exerciseFrames.entrySet()) {
                if (entry.getValue() > bestCount) {
                    best = entry.getKey();
                    bestCount = entry.getValue();
                }
            }
            return best;
        }

        /** Corrección que más veces apareció, o null si no hubo ninguna. */
        Integer topAlert() {
            Integer best = null;
            int bestCount = 0;
            for (Map.Entry<Integer, Integer> entry : alertCounts.entrySet()) {
                if (entry.getValue() > bestCount) {
                    best = entry.getKey();
                    bestCount = entry.getValue();
                }
            }
            return best;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (poseDetector != null) {
            poseDetector.close();
        }
        if (speechManager != null) {
            speechManager.shutdown();
        }
        ioExecutor.shutdownNow();
    }
}