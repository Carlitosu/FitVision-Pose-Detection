package com.grupo3.signtranslator.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Vista transparente que se superpone a la cámara o a la imagen y dibuja
 * los resultados de ML Kit. Convierte las coordenadas de la imagen de análisis
 * a coordenadas de pantalla, teniendo en cuenta la rotación y el escalado.
 */
public class GraphicOverlay extends View {

    /** Escala para la vista previa de la cámara (PreviewView usa FILL_CENTER). */
    public static final int FILL_CENTER = 0;
    /** Escala para ImageView con scaleType fitCenter (imagen completa con bandas). */
    public static final int FIT_CENTER = 1;

    private final List<Graphic> graphics = new ArrayList<>();

    private int imageWidth;
    private int imageHeight;
    private boolean mirrorHorizontally;
    private int scaleMode = FILL_CENTER;

    private float scale = 1f;
    private float offsetX;
    private float offsetY;

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** Define el modo de escalado que coincide con la vista que se muestra debajo. */
    public void setScaleMode(int mode) {
        scaleMode = mode;
        updateTransformation();
    }

    /**
     * Informa el tamaño de la imagen analizada (ya rotada) y si debe reflejarse
     * horizontalmente (cámara frontal).
     */
    public void setImageSourceInfo(int width, int height, boolean mirror) {
        imageWidth = width;
        imageHeight = height;
        mirrorHorizontally = mirror;
        updateTransformation();
    }

    /** Elimina todos los gráficos pendientes de dibujo. */
    public void clear() {
        graphics.clear();
    }

    /** Agrega un gráfico que se dibujará en el siguiente frame. */
    public void add(Graphic graphic) {
        graphics.add(graphic);
    }

    private void updateTransformation() {
        if (imageWidth <= 0 || imageHeight <= 0 || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        float scaleX = getWidth() / (float) imageWidth;
        float scaleY = getHeight() / (float) imageHeight;
        scale = scaleMode == FILL_CENTER ? Math.max(scaleX, scaleY) : Math.min(scaleX, scaleY);
        offsetX = (getWidth() - imageWidth * scale) / 2f;
        offsetY = (getHeight() - imageHeight * scale) / 2f;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        updateTransformation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Graphic graphic : graphics) {
            graphic.draw(canvas);
        }
    }

    /** Elemento dibujable base; las subclases implementan {@link #draw(Canvas)}. */
    public abstract static class Graphic {

        protected final GraphicOverlay overlay;

        protected Graphic(GraphicOverlay overlay) {
            this.overlay = overlay;
        }

        public abstract void draw(Canvas canvas);

        protected float translateX(float x) {
            float screenX = x * overlay.scale + overlay.offsetX;
            return overlay.mirrorHorizontally ? overlay.getWidth() - screenX : screenX;
        }

        protected float translateY(float y) {
            return y * overlay.scale + overlay.offsetY;
        }
    }
}
