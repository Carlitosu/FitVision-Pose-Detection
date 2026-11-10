package com.grupo3.signtranslator;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Calendar;
import java.util.List;

public class AlarmaReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmaReceiver";
    private static final String PREFS_NAME = "routines_prefs";
    private static final String KEY_ROUTINES = "routines_list";
    public static final String CHANNEL_ID = "canal_ejercicios";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            reprogramarTodasLasRutinas(context);
            return;
        }

        // Validar permiso de notificación en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Permiso de notificaciones no concedido");
                return;
            }
        }

        long routineId = intent.getLongExtra("routine_id", -1L);
        String routineTitle = intent.getStringExtra("routine_title");
        String title = (routineTitle != null && !routineTitle.isEmpty())
                ? "¡Hora de tu rutina: " + routineTitle + "!"
                : "¡Hora de tu ronda de ejercicios!";

        // Abrir LivePreviewActivity al presionar la notificación
        Intent tapIntent = new Intent(context, LivePreviewActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                (int) (routineId != -1 ? routineId : System.currentTimeMillis()),
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        crearCanalNotificaciones(context);

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_theme_sun)
                .setContentTitle(title)
                .setContentText("Es momento de activar tu cuerpo y completar tu rutina.")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSound(soundUri)
                .setVibrate(new long[]{0, 500, 250, 500})
                .setContentIntent(contentIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            int notificationId = (routineId != -1) ? (int) (routineId % Integer.MAX_VALUE) : 1001;
            notificationManager.notify(notificationId, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Error al enviar notificación de alarma", e);
        }

        // Reprogramar para la siguiente semana/día seleccionado
        if (routineId != -1) {
            reprogramarProximoDisparo(context, routineId);
        }
    }

    @SuppressWarnings("unused")
    public static void enviarNotificacionPrueba(Context context, String routineTitle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        crearCanalNotificaciones(context);

        String title = (routineTitle != null && !routineTitle.isEmpty())
                ? "¡Hora de tu rutina: " + routineTitle + "!"
                : "¡Hora de tu ronda de ejercicios!";

        Intent tapIntent = new Intent(context, LivePreviewActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_theme_sun)
                .setContentTitle(title)
                .setContentText("Es momento de activar tu cuerpo y completar tu rutina.")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSound(soundUri)
                .setVibrate(new long[]{0, 500, 250, 500})
                .setContentIntent(contentIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Error al enviar notificación de prueba", e);
        }
    }

    private void reprogramarTodasLasRutinas(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String jsonStr = prefs.getString(KEY_ROUTINES, null);
        if (jsonStr == null) return;
        try {
            JSONArray array = new JSONArray(jsonStr);
            for (int i = 0; i < array.length(); i++) {
                Routine r = Routine.fromJson(array.getJSONObject(i));
                if (r != null) {
                    programarAlarma(context, r);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error al reprogramar rutinas en el arranque", e);
        }
    }

    private void reprogramarProximoDisparo(Context context, long routineId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String jsonStr = prefs.getString(KEY_ROUTINES, null);
        if (jsonStr == null) return;
        try {
            JSONArray array = new JSONArray(jsonStr);
            for (int i = 0; i < array.length(); i++) {
                Routine r = Routine.fromJson(array.getJSONObject(i));
                if (r != null && r.getId() == routineId) {
                    programarAlarma(context, r);
                    break;
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error al reprogramar el próximo disparo de la rutina", e);
        }
    }

    public static void programarAlarma(Context context, Routine routine) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmaReceiver.class);
        intent.putExtra("routine_id", routine.getId());
        intent.putExtra("routine_title", routine.getTitle());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) (routine.getId() % Integer.MAX_VALUE),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerMillis = calcularProximoDisparo(routine.getHour(), routine.getMinute(), routine.getDays());

        Intent showIntent = new Intent(context, LivePreviewActivity.class);
        PendingIntent showOperation = PendingIntent.getActivity(
                context,
                (int) (routine.getId() % Integer.MAX_VALUE),
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(triggerMillis, showOperation);
        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
        } catch (SecurityException e) {
            Log.e(TAG, "Error al programar la alarma con setAlarmClock", e);
        }
    }

    public static long calcularProximoDisparo(int hour, int minute, List<Integer> daysOfWeek) {
        Calendar now = Calendar.getInstance();
        Calendar candidate = Calendar.getInstance();
        candidate.set(Calendar.HOUR_OF_DAY, hour);
        candidate.set(Calendar.MINUTE, minute);
        candidate.set(Calendar.SECOND, 0);
        candidate.set(Calendar.MILLISECOND, 0);

        for (int i = 0; i < 7; i++) {
            int currentDay = candidate.get(Calendar.DAY_OF_WEEK);
            if (daysOfWeek.contains(currentDay) && candidate.getTimeInMillis() > now.getTimeInMillis()) {
                return candidate.getTimeInMillis();
            }
            candidate.add(Calendar.DAY_OF_YEAR, 1);
        }
        return candidate.getTimeInMillis();
    }

    public static void crearCanalNotificaciones(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Recordatorios de Rutinas",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Avisos para iniciar tus entrenamientos programados");
                channel.enableVibration(true);
                channel.setVibrationPattern(new long[]{0, 500, 250, 500});
                Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .build();
                channel.setSound(soundUri, audioAttributes);
                channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

                nm.createNotificationChannel(channel);
            }
        }
    }
}