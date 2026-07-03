package com.fitbase.ui.workout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.fitbase.R;
import com.fitbase.data.model.Ejercicio;
import com.fitbase.data.model.ResumenSesionResponse;
import com.fitbase.service.TimerService;
import com.fitbase.ui.summary.SummaryActivity;
import com.fitbase.util.FeedbackHelper;

import java.util.Locale;

/**
 * Pantalla de entrenamiento — flujo completo en fases:
 *
 *   1. CALENTAMIENTO: Muestra movilidad dinámica + activación según tipo sesión
 *   2. EJERCICIOS: Uno a uno, serie a serie, con registro RIR + timer flotante
 *   3. ESTIRAMIENTOS: Lista de estiramientos estáticos post-entreno
 *   4. CARDIO: Si fase DEF/MNT → bici 15-20 min
 *   5. RESUMEN: Volumen, RIR medio, impacto → cierre
 *
 * Timer usa TimerService (Foreground Service + Overlay) para funcionar
 * fuera de la app con Hyper Island personalizable.
 */
public class WorkoutActivity extends AppCompatActivity {

    private WorkoutViewModel viewModel;
    private FeedbackHelper feedback;

    // ─── Layouts por fase ───
    private View layoutCargando;
    private View layoutCalentamiento;
    private View layoutEjercicio;
    private View layoutRegistro;
    private View layoutTimer;
    private View layoutEstiramientos;
    private View layoutCardio;
    private View layoutResumen;

    // ─── Calentamiento ───
    private TextView tvCalentamientoTitulo;
    private LinearLayout listCalentamiento;
    private Button btnIniciarEjercicios;

    // ─── Ejercicio activo ───
    private TextView tvNombreEjercicio, tvPesoSugerido, tvMotorDetalle;
    private TextView tvRepsObjetivo, tvRirObjetivo, tvSerieInfo, tvProgreso;

    // ─── Registro RIR ───
    private NumberPicker pickerReps;
    private Button[] botonesRir; // fácil, bien, duro, fallo
    private Button btnSensacionFacil, btnSensacionBien, btnSensacionDuro, btnSensacionFallo;

    // ─── Timer ───
    private TextView tvTimerCountdown, tvProximaSerie;

    // ─── Estiramientos ───
    private LinearLayout listEstiramientos;
    private Button btnFinEstiramientos;

    // ─── Cardio ───
    private TextView tvCardioInfo;
    private Button btnFinCardio, btnSkipCardio;

    // ─── Resumen ───
    private TextView tvResumenSeries, tvResumenVolumen, tvResumenRir, tvResumenIntensidad, tvResumenImpacto;
    private Button btnCerrarSesion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        feedback = FeedbackHelper.getInstance(this);
        vincularVistas();
        viewModel = new ViewModelProvider(this).get(WorkoutViewModel.class);
        observarDatos();

        viewModel.cargarSesion();
    }

    @Override
    protected void onResume() {
        super.onResume();
        TimerService.setAppEnPrimerPlano(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        TimerService.setAppEnPrimerPlano(false);
    }

    private void vincularVistas() {
        layoutCargando = findViewById(R.id.layoutCargando);
        layoutCalentamiento = findViewById(R.id.layoutCalentamiento);
        layoutEjercicio = findViewById(R.id.layoutEjercicio);
        layoutRegistro = findViewById(R.id.layoutRegistro);
        layoutTimer = findViewById(R.id.layoutTimer);
        layoutEstiramientos = findViewById(R.id.layoutEstiramientos);
        layoutCardio = findViewById(R.id.layoutCardio);
        layoutResumen = findViewById(R.id.layoutResumen);

        // Calentamiento
        tvCalentamientoTitulo = findViewById(R.id.tvCalentamientoTitulo);
        listCalentamiento = findViewById(R.id.listCalentamiento);
        btnIniciarEjercicios = findViewById(R.id.btnIniciarEjercicios);
        btnIniciarEjercicios.setOnClickListener(v -> {
            feedback.vibrateLight();
            viewModel.iniciarEjercicios();
        });

        // Ejercicio
        tvNombreEjercicio = findViewById(R.id.tvNombreEjercicio);
        tvPesoSugerido = findViewById(R.id.tvPesoSugerido);
        tvMotorDetalle = findViewById(R.id.tvMotorDetalle);
        tvRepsObjetivo = findViewById(R.id.tvRepsObjetivo);
        tvRirObjetivo = findViewById(R.id.tvRirObjetivo);
        tvSerieInfo = findViewById(R.id.tvSerieInfo);
        tvProgreso = findViewById(R.id.tvProgreso);

        // Registro
        pickerReps = findViewById(R.id.pickerReps);
        pickerReps.setMinValue(0);
        pickerReps.setMaxValue(30);
        pickerReps.setWrapSelectorWheel(false);

        btnSensacionFacil = findViewById(R.id.btnSensacionFacil);
        btnSensacionBien = findViewById(R.id.btnSensacionBien);
        btnSensacionDuro = findViewById(R.id.btnSensacionDuro);
        btnSensacionFallo = findViewById(R.id.btnSensacionFallo);

        btnSensacionFacil.setOnClickListener(v -> registrarConSensacion("facil", 3));
        btnSensacionBien.setOnClickListener(v -> registrarConSensacion("bien", 2));
        btnSensacionDuro.setOnClickListener(v -> registrarConSensacion("duro", 1));
        btnSensacionFallo.setOnClickListener(v -> registrarConSensacion("fallo", 0));

        // Timer
        tvTimerCountdown = findViewById(R.id.tvTimerCountdown);
        tvProximaSerie = findViewById(R.id.tvProximaSerie);

        // Estiramientos
        listEstiramientos = findViewById(R.id.listEstiramientos);
        btnFinEstiramientos = findViewById(R.id.btnFinEstiramientos);
        btnFinEstiramientos.setOnClickListener(v -> viewModel.iniciarCardioOResumen());

        // Cardio
        tvCardioInfo = findViewById(R.id.tvCardioInfo);
        btnFinCardio = findViewById(R.id.btnFinCardio);
        btnSkipCardio = findViewById(R.id.btnSkipCardio);
        btnFinCardio.setOnClickListener(v -> viewModel.finalizarSesion());
        btnSkipCardio.setOnClickListener(v -> viewModel.finalizarSesion());

        // Resumen
        tvResumenSeries = findViewById(R.id.tvResumenSeries);
        tvResumenVolumen = findViewById(R.id.tvResumenVolumen);
        tvResumenRir = findViewById(R.id.tvResumenRir);
        tvResumenIntensidad = findViewById(R.id.tvResumenIntensidad);
        tvResumenImpacto = findViewById(R.id.tvResumenImpacto);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        btnCerrarSesion.setOnClickListener(v -> finish());
    }

    private void observarDatos() {
        viewModel.getFaseWorkout().observe(this, this::cambiarFase);

        viewModel.getEjercicioActualIdx().observe(this, idx -> mostrarEjercicio());
        viewModel.getSerieActual().observe(this, serie -> mostrarEjercicio());

        viewModel.getTimerActivo().observe(this, activo -> {
            if (Boolean.TRUE.equals(activo)) {
                mostrarTimer();
            } else {
                ocultarTimer();
            }
        });

        viewModel.getResumen().observe(this, this::mostrarResumen);
    }

    // ─── Cambio de fase ───────────────────────────────────

    private void cambiarFase(WorkoutViewModel.FaseWorkout fase) {
        ocultarTodos();
        switch (fase) {
            case CARGANDO:
                layoutCargando.setVisibility(View.VISIBLE);
                break;
            case CALENTAMIENTO:
                layoutCalentamiento.setVisibility(View.VISIBLE);
                mostrarCalentamiento();
                break;
            case EJERCICIOS:
                layoutEjercicio.setVisibility(View.VISIBLE);
                layoutRegistro.setVisibility(View.VISIBLE);
                mostrarEjercicio();
                break;
            case ESTIRAMIENTOS:
                layoutEstiramientos.setVisibility(View.VISIBLE);
                mostrarEstiramientos();
                break;
            case CARDIO:
                layoutCardio.setVisibility(View.VISIBLE);
                mostrarCardio();
                break;
            case RESUMEN:
                layoutResumen.setVisibility(View.VISIBLE);
                break;
            case ERROR:
                finish();
                break;
        }
    }

    private void ocultarTodos() {
        layoutCargando.setVisibility(View.GONE);
        layoutCalentamiento.setVisibility(View.GONE);
        layoutEjercicio.setVisibility(View.GONE);
        layoutRegistro.setVisibility(View.GONE);
        layoutTimer.setVisibility(View.GONE);
        layoutEstiramientos.setVisibility(View.GONE);
        layoutCardio.setVisibility(View.GONE);
        layoutResumen.setVisibility(View.GONE);
    }

    // ─── Calentamiento ────────────────────────────────────

    /**
     * Calentamiento basado en evidencia:
     *   - Rodrigues 2020: prevención lesiones (no mejora fuerza aguda)
     *   - Page 2012: NO estático pre-entreno (reduce fuerza)
     *   - calentamiento.md §3: Movilidad dinámica + Activación específica + Series aproximación
     */
    private void mostrarCalentamiento() {
        tvCalentamientoTitulo.setText("🔥 Calentamiento (10-15 min)");
        listCalentamiento.removeAllViews();

        // Fase 2: Movilidad dinámica (calentamiento.md §3, 5 min)
        addCalentamientoItem("Círculos de cadera", "10/lado");
        addCalentamientoItem("Gato-vaca", "10 reps");
        addCalentamientoItem("Dislocaciones con banda", "10 reps");

        // Fase 3: Activación específica según tipo sesión (calentamiento.md §3)
        if (viewModel.getSesionData().getValue() != null &&
            viewModel.getSesionData().getValue().getSesion() != null) {
            String tipo = viewModel.getSesionData().getValue().getSesion().getTipo();
            if (tipo != null) {
                switch (tipo.toLowerCase()) {
                    case "push":
                        addCalentamientoItem("Face pulls ligeros", "15 reps");
                        addCalentamientoItem("Rotación externa banda", "10/lado");
                        break;
                    case "pull":
                        addCalentamientoItem("Dead hangs", "20s");
                        addCalentamientoItem("Retracción escapular", "15 reps");
                        break;
                    case "pierna":
                        addCalentamientoItem("Glute bridges", "15 reps");
                        addCalentamientoItem("Sentadillas sin peso", "10 reps");
                        break;
                    default: // Hombros+Brazos
                        addCalentamientoItem("Rotación externa banda", "10/lado");
                        addCalentamientoItem("Face pulls ligeros", "15 reps");
                        break;
                }
            }
        }

        // Fase 4: Series de aproximación (calentamiento.md §3)
        // 40% x10 → 60% x6 → 75% x3 → 85% x1-2 para compuesto pesado
        addCalentamientoItem("Series aproximación 1er compuesto", "40% → 60% → 75% → 85%");
    }

    private void addCalentamientoItem(String nombre, String reps) {
        TextView tv = new TextView(this);
        tv.setText("• " + nombre + " — " + reps);
        tv.setTextSize(15);
        tv.setPadding(0, 8, 0, 8);
        listCalentamiento.addView(tv);
    }

    // ─── Ejercicio actual ─────────────────────────────────

    private void mostrarEjercicio() {
        Ejercicio ej = viewModel.getEjercicioActual();
        if (ej == null) return;

        Integer idx = viewModel.getEjercicioActualIdx().getValue();
        Integer serie = viewModel.getSerieActual().getValue();
        int total = viewModel.getTotalEjercicios();

        tvNombreEjercicio.setText(ej.getNombreCorto());
        tvPesoSugerido.setText(ej.getPesoTexto());
        tvMotorDetalle.setText(ej.getMotorDetalle() != null ? ej.getMotorDetalle() : "");
        tvRepsObjetivo.setText("Reps: " + ej.getRepsPlan());
        tvRirObjetivo.setText("RIR objetivo: " + ej.getRirObjetivo());
        tvSerieInfo.setText(String.format(Locale.getDefault(),
                "Serie %d / %d", serie != null ? serie : 1, ej.getSeriesPlan()));
        tvProgreso.setText(String.format(Locale.getDefault(),
                "Ejercicio %d / %d", (idx != null ? idx : 0) + 1, total));

        // Pre-set reps picker al valor objetivo
        try {
            String repsStr = ej.getRepsPlan().split("-")[0].replaceAll("[^0-9]", "");
            int repsDefault = Integer.parseInt(repsStr);
            pickerReps.setValue(repsDefault);
        } catch (NumberFormatException e) {
            pickerReps.setValue(10);
        }
    }

    // ─── Registro de serie ────────────────────────────────

    private void registrarConSensacion(String sensacion, int rir) {
        feedback.vibrateLight();
        int reps = pickerReps.getValue();
        viewModel.registrarSerie(reps, rir, sensacion);
    }

    // ─── Timer ────────────────────────────────────────────

    private void mostrarTimer() {
        layoutRegistro.setVisibility(View.GONE);
        layoutTimer.setVisibility(View.VISIBLE);

        Ejercicio ej = viewModel.getEjercicioActual();
        int segundos = ej != null ? ej.getDescansoSeg() : 120;

        tvProximaSerie.setText("Descanso — próxima serie en:");

        // Iniciar TimerService (foreground + overlay flotante)
        Intent timerIntent = new Intent(this, TimerService.class);
        timerIntent.putExtra("segundos", segundos);
        timerIntent.putExtra("ejercicio", ej != null ? ej.getNombreCorto() : "");
        startForegroundService(timerIntent);
    }

    private void ocultarTimer() {
        layoutTimer.setVisibility(View.GONE);
        layoutRegistro.setVisibility(View.VISIBLE);
    }

    // ─── Estiramientos ────────────────────────────────────

    /**
     * Estiramientos post-entreno BASADOS EN SESIÓN ACTUAL.
     * Evidencia: Page 2012 — estático 30s por grupo TRABAJADO.
     * Bandy 1997 — 30s = 60s (no hay beneficio adicional al estirar más).
     * Los ejercicios se seleccionan según el tipo de sesión (Push/Pull/Pierna/Hombros).
     */
    private void mostrarEstiramientos() {
        listEstiramientos.removeAllViews();

        SesionResponse data = viewModel.getSesionData().getValue();
        String tipo = (data != null && data.getSesion() != null) ?
                data.getSesion().getTipo().toUpperCase() : "PUSH";

        // Page 2012: "músculos principales trabajados" → depende del split
        switch (tipo) {
            case "PUSH":
                addEstiramientoItem("Pectoral en marco de puerta", "30s/lado");
                addEstiramientoItem("Estiramiento deltoides (brazo cruzado)", "30s/lado");
                addEstiramientoItem("Extensión tríceps overhead", "30s/brazo");
                break;
            case "PULL":
            case "ESPALDA":
                addEstiramientoItem("Estiramiento dorsal en barra", "30s");
                addEstiramientoItem("Estiramiento bíceps en pared", "30s/brazo");
                addEstiramientoItem("Rotación torácica tumbado", "30s/lado");
                break;
            case "PIERNA":
                addEstiramientoItem("Cuádriceps de pie", "30s/pierna");
                addEstiramientoItem("Isquios de pie (pierna en banco)", "30s/pierna");
                addEstiramientoItem("Estiramiento psoas/flexor cadera", "30s/lado");
                addEstiramientoItem("Aductores en mariposa", "30s");
                break;
            case "HOMBROS":
            case "HOMBROS+BRAZOS":
                addEstiramientoItem("Deltoides posterior (brazo cruzado)", "30s/lado");
                addEstiramientoItem("Extensión tríceps overhead", "30s/brazo");
                addEstiramientoItem("Estiramiento bíceps en pared", "30s/brazo");
                addEstiramientoItem("Rotación externa pasiva", "30s/lado");
                break;
            default:
                addEstiramientoItem("Pectoral en marco de puerta", "30s/lado");
                addEstiramientoItem("Estiramiento dorsal", "30s");
                addEstiramientoItem("Cuádriceps de pie", "30s/pierna");
                break;
        }
    }

    private void addEstiramientoItem(String nombre, String duracion) {
        TextView tv = new TextView(this);
        tv.setText("• " + nombre + " — " + duracion);
        tv.setTextSize(15);
        tv.setPadding(0, 8, 0, 8);
        listEstiramientos.addView(tv);
    }

    // ─── Cardio ───────────────────────────────────────────

    /**
     * Muestra info de cardio con justificación científica.
     * Esta pantalla SOLO aparece si la fase lo exige (DEF/MNT).
     * Wilson 2012: bici no interfiere con hipertrofia. Viana 2019: LISS = HIIT para grasa.
     */
    private void mostrarCardio() {
        SesionResponse data = viewModel.getSesionData().getValue();
        String fase = (data != null && data.getSesion() != null) ? data.getSesion().getFase() : "DEF";

        String texto;
        if ("DEF".equals(fase)) {
            texto = "🚴 15-20 min bici estática\n60-70% FC máx (LISS)\n\n" +
                    "¿Por qué? Fase DEFINICIÓN → Viana 2019: LISS post-gym\n" +
                    "aumenta déficit calórico sin interferir (Wilson 2012)";
        } else {
            texto = "🚴 10 min bici estática\n60-70% FC máx (LISS)\n\n" +
                    "¿Por qué? Fase MANTENIMIENTO → mantener\n" +
                    "capacidad aeróbica (Wilson 2012: bici no interfiere)";
        }
        tvCardioInfo.setText(texto);
    }

    // ─── Resumen ──────────────────────────────────────────

    private void mostrarResumen(ResumenSesionResponse.Resumen res) {
        if (res == null) return;
        tvResumenSeries.setText(String.valueOf(res.seriesTotales));
        tvResumenVolumen.setText(res.volumenTotalKg + " kg");
        tvResumenRir.setText(String.valueOf(res.rirMedio));
        tvResumenIntensidad.setText(res.intensidadPercibida);
        tvResumenImpacto.setText(res.impacto);
    }

    // ─── Timer broadcast receiver ─────────────────────────

    @Override
    protected void onStart() {
        super.onStart();
        // Register for timer finished broadcasts
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
                .registerReceiver(timerFinishedReceiver,
                        new android.content.IntentFilter(TimerService.ACTION_TIMER_FINISHED));
    }

    @Override
    protected void onStop() {
        super.onStop();
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(timerFinishedReceiver);
    }

    private final android.content.BroadcastReceiver timerFinishedReceiver =
            new android.content.BroadcastReceiver() {
                @Override
                public void onReceive(android.content.Context context, Intent intent) {
                    feedback.vibrateStrong();
                    viewModel.timerCompletado();
                }
            };
}
