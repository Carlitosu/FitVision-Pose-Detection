package com.grupo3.signtranslator.util;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;

import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;
import java.io.InputStream;

/** Utilidades para cargar imágenes de la galería o de la cámara nativa. */
public final class ImageUtils {

    private ImageUtils() {
    }

    /**
     * Carga una imagen desde un Uri, la reduce si supera {@code maxSize} píxeles
     * y corrige la orientación indicada en sus metadatos EXIF.
     */
    public static Bitmap loadBitmap(Context context, Uri uri, int maxSize) throws IOException {
        ContentResolver resolver = context.getContentResolver();

        // 1) Leer solo las dimensiones para calcular el factor de reducción
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        decode(resolver, uri, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("El archivo no es una imagen válida");
        }

        // 2) Decodificar la imagen reducida
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxSize);
        Bitmap bitmap = decode(resolver, uri, options);
        if (bitmap == null) {
            throw new IOException("No se pudo decodificar la imagen");
        }

        // 3) Corregir la rotación (las fotos de cámara suelen guardarse "de lado")
        int degrees = readRotation(resolver, uri);
        if (degrees == 0) {
            return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    /** Reduce el bitmap para que su lado mayor no supere {@code maxSize}. Devuelve el mismo objeto si ya cabe. */
    public static Bitmap scaleDown(Bitmap bitmap, int maxSize) {
        int largest = Math.max(bitmap.getWidth(), bitmap.getHeight());
        if (largest <= maxSize) {
            return bitmap;
        }
        float ratio = maxSize / (float) largest;
        return Bitmap.createScaledBitmap(bitmap,
                Math.round(bitmap.getWidth() * ratio),
                Math.round(bitmap.getHeight() * ratio),
                true);
    }

    private static Bitmap decode(ContentResolver resolver, Uri uri, BitmapFactory.Options options)
            throws IOException {
        try (InputStream in = openStream(resolver, uri)) {
            return BitmapFactory.decodeStream(in, null, options);
        }
    }

    private static int readRotation(ContentResolver resolver, Uri uri) throws IOException {
        try (InputStream in = openStream(resolver, uri)) {
            int orientation = new ExifInterface(in).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        }
    }

    private static InputStream openStream(ContentResolver resolver, Uri uri) throws IOException {
        InputStream in = resolver.openInputStream(uri);
        if (in == null) {
            throw new IOException("No se pudo abrir la imagen: " + uri);
        }
        return in;
    }

    private static int calculateInSampleSize(int width, int height, int maxSize) {
        int sampleSize = 1;
        while (Math.max(width, height) / sampleSize > maxSize) {
            sampleSize *= 2;
        }
        return sampleSize;
    }
}
