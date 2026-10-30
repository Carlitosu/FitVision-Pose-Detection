package com.grupo3.signtranslator;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.grupo3.signtranslator.databinding.ActivityMainBinding;
import com.grupo3.signtranslator.util.ThemeManager;

import java.io.File;
import java.io.IOException;

/** Menú principal: cámara en vivo, foto con la cámara nativa o imagen/video de la galería. */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private Uri pendingPhotoUri;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    openLivePreview();
                } else {
                    Toast.makeText(this, R.string.permission_camera_denied, Toast.LENGTH_LONG).show();
                }
            });

    /** Abre la app de cámara del sistema y guarda la foto en {@link #pendingPhotoUri}. */
    private final ActivityResultLauncher<Uri> takePhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && pendingPhotoUri != null) {
                    openStaticAnalysis(pendingPhotoUri);
                } else {
                    Toast.makeText(this, R.string.photo_cancelled, Toast.LENGTH_SHORT).show();
                }
            });

    /** Selector del sistema para imágenes o videos; concede lectura temporal del archivo elegido. */
    private final ActivityResultLauncher<String[]> pickMediaLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    openStaticAnalysis(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Antes de super.onCreate para que la actividad ya use el tema elegido por el usuario
        ThemeManager.applySavedMode(this);
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnLive.setOnClickListener(v -> onLiveClicked());
        binding.btnCapture.setOnClickListener(v -> takePhoto());
        binding.btnGallery.setOnClickListener(v ->
                pickMediaLauncher.launch(new String[]{"image/*", "video/*"}));
        binding.btnTips.setOnClickListener(v ->
                startActivity(new Intent(this, TipsActivity.class)));
        binding.btnCredits.setOnClickListener(v ->
                startActivity(new Intent(this, CreditsActivity.class)));
    }

    /** Icono de tema en la barra superior: sol en modo claro, luna en modo oscuro. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.action_theme).setIcon(ThemeManager.isDarkNow(this)
                ? R.drawable.ic_theme_moon
                : R.drawable.ic_theme_sun);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_theme) {
            // Al cambiar el tema la actividad se recrea y el icono se actualiza solo
            ThemeManager.setDark(this, !ThemeManager.isDarkNow(this));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onLiveClicked() {
        boolean granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        if (granted) {
            openLivePreview();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void takePhoto() {
        try {
            File dir = new File(getCacheDir(), "captures");
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("No se pudo crear el directorio");
            }
            File photo = File.createTempFile("capture_" + System.currentTimeMillis(), ".jpg", dir);
            pendingPhotoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photo);
            takePhotoLauncher.launch(pendingPhotoUri);
        } catch (IOException e) {
            Toast.makeText(this, R.string.error_create_file, Toast.LENGTH_SHORT).show();
        }
    }

    private void openLivePreview() {
        startActivity(new Intent(this, LivePreviewActivity.class));
    }

    private void openStaticAnalysis(Uri uri) {
        Intent intent = new Intent(this, StaticAnalysisActivity.class);
        intent.setData(uri);
        // Permite a la actividad leer el archivo concedido por la cámara o la galería
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }
}
