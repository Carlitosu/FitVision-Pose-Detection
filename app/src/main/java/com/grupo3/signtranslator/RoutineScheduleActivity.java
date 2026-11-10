package com.grupo3.signtranslator;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RoutineScheduleActivity extends AppCompatActivity {

    private static final String TAG = "RoutineSchedule";
    private static final String PREFS_NAME = "routines_prefs";
    private static final String KEY_ROUTINES = "routines_list";

    private EditText etRoutineTitle;
    private Chip chipMon, chipTue, chipWed, chipThu, chipFri, chipSat, chipSun;
    private TimePicker timePicker;
    private Button btnSaveSchedule;
    private TextView tvEmptyRoutines;
    private RecyclerView rvRoutines;

    private final List<Routine> routineList = new ArrayList<>();
    private RoutineAdapter adapter;

    // Estado para saber si estamos editando una rutina existente
    private Routine editingRoutine = null;
    private int editingPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine_schedule);

        // Asegurar la creación del canal de notificaciones en el sistema
        AlarmaReceiver.crearCanalNotificaciones(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Agenda de Rutinas");
        }

        // Vistas del formulario
        etRoutineTitle = findViewById(R.id.etRoutineTitle);
        chipMon = findViewById(R.id.chipMon);
        chipTue = findViewById(R.id.chipTue);
        chipWed = findViewById(R.id.chipWed);
        chipThu = findViewById(R.id.chipThu);
        chipFri = findViewById(R.id.chipFri);
        chipSat = findViewById(R.id.chipSat);
        chipSun = findViewById(R.id.chipSun);
        timePicker = findViewById(R.id.timePicker);
        btnSaveSchedule = findViewById(R.id.btnSaveSchedule);

        // Vistas de la lista
        tvEmptyRoutines = findViewById(R.id.tvEmptyRoutines);
        rvRoutines = findViewById(R.id.rvRoutines);

        configurarRecyclerView();
        cargarRutinas();

        btnSaveSchedule.setOnClickListener(v -> guardarONualizarRutina());
    }

    private void configurarRecyclerView() {
        adapter = new RoutineAdapter(routineList, new RoutineAdapter.OnRoutineActionListener() {
            @Override
            public void onDelete(Routine routine, int position) {
                eliminarRutina(routine, position);
            }

            @Override
            public void onEdit(Routine routine, int position) {
                cargarRutinaEnFormulario(routine, position);
            }
        });
        rvRoutines.setLayoutManager(new LinearLayoutManager(this));
        rvRoutines.setAdapter(adapter);
    }

    private void cargarRutinaEnFormulario(Routine routine, int position) {
        editingRoutine = routine;
        editingPosition = position;

        etRoutineTitle.setText(routine.getTitle());

        List<Integer> days = routine.getDays();
        chipMon.setChecked(days.contains(Calendar.MONDAY));
        chipTue.setChecked(days.contains(Calendar.TUESDAY));
        chipWed.setChecked(days.contains(Calendar.WEDNESDAY));
        chipThu.setChecked(days.contains(Calendar.THURSDAY));
        chipFri.setChecked(days.contains(Calendar.FRIDAY));
        chipSat.setChecked(days.contains(Calendar.SATURDAY));
        chipSun.setChecked(days.contains(Calendar.SUNDAY));

        timePicker.setHour(routine.getHour());
        timePicker.setMinute(routine.getMinute());

        btnSaveSchedule.setText("Actualizar Rutina");
        etRoutineTitle.requestFocus();
        Toast.makeText(this, "Editando '" + routine.getTitle() + "'", Toast.LENGTH_SHORT).show();
    }

    private void guardarONualizarRutina() {
        timePicker.clearFocus();

        String title = etRoutineTitle.getText().toString().trim();
        if (title.isEmpty()) {
            etRoutineTitle.setError("Ingresa un título para la rutina");
            etRoutineTitle.requestFocus();
            return;
        }

        List<Integer> selectedDays = obtenerDiasSeleccionados();
        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos un día de la semana", Toast.LENGTH_SHORT).show();
            return;
        }

        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();

        if (editingRoutine != null && editingPosition != -1) {
            // Actualizar rutina existente
            cancelarAlarma(editingRoutine);

            Routine rutinaActualizada = new Routine(editingRoutine.getId(), title, hour, minute, selectedDays);
            programarAlarma(rutinaActualizada);

            routineList.set(editingPosition, rutinaActualizada);
            guardarRutinasEnPrefs();
            adapter.notifyItemChanged(editingPosition);

            resetearFormulario();
            Toast.makeText(this, "¡Rutina '" + title + "' actualizada!", Toast.LENGTH_SHORT).show();
        } else {
            // Crear nueva rutina
            long id = System.currentTimeMillis();
            Routine nuevaRutina = new Routine(id, title, hour, minute, selectedDays);
            programarAlarma(nuevaRutina);

            routineList.add(0, nuevaRutina);
            guardarRutinasEnPrefs();
            adapter.notifyItemInserted(0);
            actualizarVisibilidadLista();

            resetearFormulario();
            Toast.makeText(this, "¡Rutina '" + title + "' agendada a las " + nuevaRutina.getFormattedTime() + "!", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetearFormulario() {
        editingRoutine = null;
        editingPosition = -1;
        btnSaveSchedule.setText("Guardar Rutina");
        etRoutineTitle.setText("");
        desmarcarChips();
    }

    private List<Integer> obtenerDiasSeleccionados() {
        List<Integer> days = new ArrayList<>();
        if (chipMon.isChecked()) days.add(Calendar.MONDAY);
        if (chipTue.isChecked()) days.add(Calendar.TUESDAY);
        if (chipWed.isChecked()) days.add(Calendar.WEDNESDAY);
        if (chipThu.isChecked()) days.add(Calendar.THURSDAY);
        if (chipFri.isChecked()) days.add(Calendar.FRIDAY);
        if (chipSat.isChecked()) days.add(Calendar.SATURDAY);
        if (chipSun.isChecked()) days.add(Calendar.SUNDAY);
        return days;
    }

    private void desmarcarChips() {
        chipMon.setChecked(false);
        chipTue.setChecked(false);
        chipWed.setChecked(false);
        chipThu.setChecked(false);
        chipFri.setChecked(false);
        chipSat.setChecked(false);
        chipSun.setChecked(false);
    }

    private void eliminarRutina(Routine routine, int position) {
        if (editingRoutine != null && editingRoutine.getId() == routine.getId()) {
            resetearFormulario();
        }
        cancelarAlarma(routine);
        routineList.remove(position);
        adapter.notifyItemRemoved(position);
        guardarRutinasEnPrefs();
        actualizarVisibilidadLista();
        Toast.makeText(this, "Rutina eliminada", Toast.LENGTH_SHORT).show();
    }

    private void actualizarVisibilidadLista() {
        tvEmptyRoutines.setVisibility(routineList.isEmpty() ? View.VISIBLE : View.GONE);
        rvRoutines.setVisibility(routineList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void guardarRutinasEnPrefs() {
        JSONArray array = new JSONArray();
        for (Routine r : routineList) {
            array.put(r.toJson());
        }
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_ROUTINES, array.toString())
                .apply();
    }

    @SuppressWarnings("NotifyDataSetChanged")
    private void cargarRutinas() {
        routineList.clear();
        String jsonStr = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_ROUTINES, null);
        if (jsonStr != null) {
            try {
                JSONArray array = new JSONArray(jsonStr);
                for (int i = 0; i < array.length(); i++) {
                    Routine r = Routine.fromJson(array.getJSONObject(i));
                    if (r != null) {
                        routineList.add(r);
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error al cargar las rutinas", e);
            }
        }
        adapter.notifyDataSetChanged();
        actualizarVisibilidadLista();
    }

    private void programarAlarma(Routine routine) {
        AlarmaReceiver.programarAlarma(this, routine);
    }

    private void cancelarAlarma(Routine routine) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmaReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (int) (routine.getId() % Integer.MAX_VALUE),
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}