package com.grupo3.signtranslator.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;

import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.grupo3.signtranslator.logic.PoseMath;

/**
 * Dibuja el esqueleto corporal completo (torso, brazos y piernas) a partir de los puntos de
 * ML Kit. No dibuja manos ni dedos. Se pinta en rojo cuando hay una corrección de postura.
 */
public class PoseGraphic extends GraphicOverlay.Graphic {

    private static final float JOINT_RADIUS = 8f;
    private static final float LINE_WIDTH = 6f;
    private static final int ALERT_COLOR = Color.rgb(255, 64, 64);

    /** Articulaciones del cuerpo que se marcan con un círculo (sin manos ni dedos). */
    private static final int[] BODY_JOINTS = {
            PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE,
            PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE,
            PoseLandmark.LEFT_HEEL, PoseLandmark.RIGHT_HEEL,
            PoseLandmark.LEFT_FOOT_INDEX, PoseLandmark.RIGHT_FOOT_INDEX
    };

    private final Pose pose;
    private final Paint jointPaint;
    private final Paint centerPaint;
    private final Paint leftPaint;
    private final Paint rightPaint;

    public PoseGraphic(GraphicOverlay overlay, Pose pose, boolean warning) {
        super(overlay);
        this.pose = pose;
        jointPaint = createPaint(Color.WHITE, Paint.Style.FILL);
        centerPaint = createPaint(warning ? ALERT_COLOR : Color.YELLOW, Paint.Style.STROKE);
        leftPaint = createPaint(warning ? ALERT_COLOR : Color.rgb(0, 200, 255), Paint.Style.STROKE);
        rightPaint = createPaint(warning ? ALERT_COLOR : Color.rgb(255, 120, 0), Paint.Style.STROKE);
    }

    @Override
    public void draw(Canvas canvas) {
        // Torso
        line(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER, centerPaint);
        line(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP, centerPaint);
        line(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, leftPaint);
        line(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, rightPaint);

        // Brazos
        line(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, leftPaint);
        line(canvas, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST, leftPaint);
        line(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, rightPaint);
        line(canvas, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST, rightPaint);

        // Piernas y pies
        line(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, leftPaint);
        line(canvas, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE, leftPaint);
        line(canvas, PoseLandmark.LEFT_ANKLE, PoseLandmark.LEFT_HEEL, leftPaint);
        line(canvas, PoseLandmark.LEFT_ANKLE, PoseLandmark.LEFT_FOOT_INDEX, leftPaint);
        line(canvas, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, rightPaint);
        line(canvas, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE, rightPaint);
        line(canvas, PoseLandmark.RIGHT_ANKLE, PoseLandmark.RIGHT_HEEL, rightPaint);
        line(canvas, PoseLandmark.RIGHT_ANKLE, PoseLandmark.RIGHT_FOOT_INDEX, rightPaint);

        for (int type : BODY_JOINTS) {
            PointF point = PoseMath.point(pose, type);
            if (point != null) {
                canvas.drawCircle(translateX(point.x), translateY(point.y), JOINT_RADIUS, jointPaint);
            }
        }
    }

    private void line(Canvas canvas, int startType, int endType, Paint paint) {
        PointF start = PoseMath.point(pose, startType);
        PointF end = PoseMath.point(pose, endType);
        if (start == null || end == null) {
            return;
        }
        canvas.drawLine(
                translateX(start.x), translateY(start.y),
                translateX(end.x), translateY(end.y),
                paint);
    }

    private static Paint createPaint(int color, Paint.Style style) {
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStyle(style);
        paint.setStrokeWidth(LINE_WIDTH);
        paint.setAntiAlias(true);
        return paint;
    }
}
