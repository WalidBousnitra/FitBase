package com.fitbase.ui.workout;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.fitbase.ui.BaseActivity;
import androidx.lifecycle.ViewModelProvider;

import com.fitbase.R;
import com.fitbase.data.model.Ejercicio;
import com.fitbase.data.model.ResumenSesionResponse;
import com.fitbase.data.model.SesionResponse;
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
public class WorkoutActivity extends BaseActivity {

    private WorkoutViewModel viewModel;
    private FeedbackHelper feedback;

    // ─── Layouts por fase ───
    private View layoutCargando;
    private View layoutRutina;
    private View layoutEjercicio;
    private View layoutRegistro;
    private View layoutTimer;
    private View layoutCardio;
    private View layoutResumen;

    // ─── Rutina (calentamiento/estiramientos) ───
    private TextView tvRutinaProgreso, tvRutinaTitulo, tvRutinaNombre, tvRutinaValor;
    private View contenidoRutina;

    // ─── Ejercicio activo ───
    private View contenidoEjercicio;
    private TextView tvNombreEjercicio, tvPesoSugerido, tvMotorDetalle;
    private TextView tvRepsObjetivo, tvRirObjetivo, tvSerieInfo, tvProgreso;
    private TextView tvNotaEjercicio;
    private TextView tvSuperserie;
    private Button btnPesoMenos, btnPesoMas;

    // ─── Registro RIR ───
    private NumberPicker pickerReps;
    private Button[] botonesRir; // fácil, bien, duro, fallo
    private Button btnSensacionFacil, btnSensacionBien, btnSensacionDuro, btnSensacionFallo;

    // ─── Timer ───
    private Chronometer tvTimerCountdown;
    private TextView tvProximaSerie, tvProximaInfo;
    // Avanza a la siguiente serie en el instante EXACTO en que el Chronometer
    // en pantalla llega a 0, sin esperar al broadcast de TimerService (que
    // corre en su propio Handler de 1s y puede llegar con hasta ~1s de
    // retraso respecto a lo que el usuario ve en pantalla). El broadcast
    // sigue siendo necesario para fuera de la app, así que ambos caminos
    // comparten timerYaAvanzado para no vibrar/avanzar dos veces.
    private final Handler timerLocalHandler = new Handler(Looper.getMainLooper());
    private boolean timerYaAvanzado = false;
    // Instante objetivo (elapsedRealtime) del descanso actual — leído por el
    // OnChronometerTickListener (ver vincularVistas) como segundo camino de
    // avance automático. -1 = no hay descanso activo.
    private long timerFinishElapsedMs = -1;
    // Ver reanudarTimerSiAplica(): true justo antes de forzar timerActivo=true
    // por un descanso retomado, para que el observer no lo pise con una
    // cuenta atrás nueva de duración completa.
    private boolean suprimirProximoMostrarTimer = false;
    private static final String PREFS_TIMER = "fitbase_timer_activo";

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
        com.fitbase.util.InsetsHelper.aplicarInsetsSistema(this);

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
        // Si volvemos a la app (por el motivo que sea, no solo tocando la
        // notificación), la alarma de "descanso terminado" se calla sola —
        // no hace falta que el usuario busque el botón "Detener".
        com.fitbase.service.TimerDetenerReceiver.detenerAlarma(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        TimerService.setAppEnPrimerPlano(false);
    }

    private void vincularVistas() {
        layoutCargando = findViewById(R.id.layoutCargando);
        layoutRutina = findViewById(R.id.layoutRutina);
        layoutEjercicio = findViewById(R.id.layoutEjercicio);
        layoutRegistro = findViewById(R.id.layoutRegistro);
        layoutTimer = findViewById(R.id.layoutTimer);
        layoutCardio = findViewById(R.id.layoutCardio);
        layoutResumen = findViewById(R.id.layoutResumen);

        // Rutina (calentamiento/estiramientos) — pantalla minimalista, un item
        // cada vez, swipe para avanzar (igual que Ejercicio).
        tvRutinaProgreso = findViewById(R.id.tvRutinaProgreso);
        tvRutinaTitulo = findViewById(R.id.tvRutinaTitulo);
        tvRutinaNombre = findViewById(R.id.tvRutinaNombre);
        tvRutinaValor = findViewById(R.id.tvRutinaValor);
        contenidoRutina = findViewById(R.id.contenidoRutina);
        layoutRutina.setOnTouchListener(new SwipeListener(this) {
            @Override public void onSwipeLeft() {
                feedback.vibrateLight();
                viewModel.siguienteItemRutina();
            }
            @Override public void onSwipeRight() {
                feedback.vibrateLight();
                viewModel.itemAnteriorRutina();
            }
        });

        // Ejercicio
        contenidoEjercicio = findViewById(R.id.contenidoEjercicio);
        tvNombreEjercicio = findViewById(R.id.tvNombreEjercicio);
        tvPesoSugerido = findViewById(R.id.tvPesoSugerido);
        tvMotorDetalle = findViewById(R.id.tvMotorDetalle);
        tvRepsObjetivo = findViewById(R.id.tvRepsObjetivo);
        tvRirObjetivo = findViewById(R.id.tvRirObjetivo);
        tvNotaEjercicio = findViewById(R.id.tvNotaEjercicio);
        tvSuperserie = findViewById(R.id.tvSuperserie);
        tvSerieInfo = findViewById(R.id.tvSerieInfo);
        tvProgreso = findViewById(R.id.tvProgreso);

        // Peso ajustable — el sugerido por el motor es solo el punto de
        // partida; lo que quede aquí es lo que se manda a ejercicios_log.
        btnPesoMenos = findViewById(R.id.btnPesoMenos);
        btnPesoMas = findViewById(R.id.btnPesoMas);
        btnPesoMenos.setOnClickListener(v -> ajustarPeso(-1));
        btnPesoMas.setOnClickListener(v -> ajustarPeso(1));

        // Registro
        pickerReps = findViewById(R.id.pickerReps);
        pickerReps.setMinValue(0);
        pickerReps.setMaxValue(30);
        pickerReps.setWrapSelectorWheel(false);

        btnSensacionFacil = findViewById(R.id.btnSensacionFacil);
        btnSensacionBien = findViewById(R.id.btnSensacionBien);
        btnSensacionDuro = findViewById(R.id.btnSensacionDuro);
        btnSensacionFallo = findViewById(R.id.btnSensacionFallo);

        // Los 4 botones son el selector de RIR (Repeticiones en Recámara):
        // Fácil=3, Bien=2, Duro=1, Fallo=0. Es el único dato de esfuerzo que se
        // registra — se retiró str_sensacion por ser el mismo dato duplicado.
        btnSensacionFacil.setOnClickListener(v -> registrarConRir(3));
        btnSensacionBien.setOnClickListener(v -> registrarConRir(2));
        btnSensacionDuro.setOnClickListener(v -> registrarConRir(1));
        btnSensacionFallo.setOnClickListener(v -> registrarConRir(0));

        // Swipe derecha = volver al ejercicio sin registrar (cancelar), por si
        // se entra aquí sin querer o el usuario cambia de opinión.
        layoutRegistro.setOnTouchListener(new SwipeListener(this) {
            @Override public void onSwipeLeft() { }
            @Override public void onSwipeRight() {
                feedback.vibrateLight();
                viewModel.cancelarRegistroRpe();
            }
        });

        // Timer
        tvTimerCountdown = findViewById(R.id.tvTimerCountdown);
        tvProximaSerie = findViewById(R.id.tvProximaSerie);
        tvProximaInfo = findViewById(R.id.tvProximaInfo);
        // Segundo camino, atado DIRECTAMENTE al Chronometer que el usuario ve
        // en pantalla (tick nativo ~1/seg) — si por lo que sea el Handler
        // programado en mostrarTimer() no llega a disparar (o llega tarde),
        // esto igual detecta "ya pasó de 0" y avanza. Antes solo había un
        // camino (Handler + broadcast); si ninguno de los dos disparaba, el
        // Chronometer se quedaba contando en negativo sin que nada avanzara.
        tvTimerCountdown.setOnChronometerTickListener(c -> {
            if (timerFinishElapsedMs > 0 && SystemClock.elapsedRealtime() >= timerFinishElapsedMs) {
                avanzarSiguienteSerieAutomatico();
            }
        });

        // Gestos: swipe izquierda para avanzar (ui.md REG-DEV-01 §4.2/§4.4)
        layoutEjercicio.setOnTouchListener(new SwipeListener(this) {
            @Override public void onSwipeLeft() {
                Ejercicio ej = viewModel.getEjercicioActual();
                if (ej != null && ej.necesitaPesoManual()) {
                    feedback.vibrateStrong();
                    android.widget.Toast.makeText(WorkoutActivity.this,
                            "Ajusta el peso (− / +) antes de completar la serie",
                            android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                feedback.vibrateLight();
                viewModel.mostrarRegistroRpe();
            }
            @Override public void onSwipeRight() { }
        });
        layoutTimer.setOnTouchListener(new SwipeListener(this) {
            @Override public void onSwipeLeft() {
                feedback.vibrateLight();
                saltarDescanso();
            }
            @Override public void onSwipeRight() { }
        });

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
        viewModel.getRutinaIdx().observe(this, idx -> mostrarItemRutina());

        viewModel.getTimerActivo().observe(this, activo -> {
            if (Boolean.TRUE.equals(activo)) {
                // Al reanudar un descanso tras un cierre/apagón (ver
                // reanudarTimerSiAplica), este flag evita que este observer
                // reinicie la cuenta atrás completa justo después de que ya
                // se haya mostrado con el tiempo restante correcto.
                if (suprimirProximoMostrarTimer) {
                    suprimirProximoMostrarTimer = false;
                } else {
                    mostrarTimer();
                }
            } else {
                ocultarTimer();
            }
        });

        // Ejercicio y Registro RPE son pantallas completas y excluyentes
        // (ui.md §4.2/§4.3) — nunca las dos a la vez.
        viewModel.getMostrandoRegistro().observe(this, mostrando -> {
            if (Boolean.TRUE.equals(mostrando)) {
                layoutEjercicio.setVisibility(View.GONE);
                layoutRegistro.setVisibility(View.VISIBLE);
            } else {
                layoutRegistro.setVisibility(View.GONE);
                if (viewModel.getFaseWorkout().getValue() == WorkoutViewModel.FaseWorkout.EJERCICIOS
                        && Boolean.FALSE.equals(viewModel.getTimerActivo().getValue())) {
                    layoutEjercicio.setVisibility(View.VISIBLE);
                }
            }
        });

        viewModel.getResumen().observe(this, this::mostrarResumen);

        // Ramadán (cultura.md §5): aviso una vez al empezar la sesión — el
        // volumen ya viene recalculado -30% desde el backend, esto es solo
        // el recordatorio de timing/intensidad.
        viewModel.getSesionData().observe(this, data -> {
            if (data != null && data.isRamadanActivo() && data.getRamadanNota() != null) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Ramadán")
                        .setMessage(data.getRamadanNota())
                        .setPositiveButton("Entendido", null)
                        .show();
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage(error)
                        .setPositiveButton("OK", (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
            }
        });
    }

    // ─── Cambio de fase ───────────────────────────────────

    private void cambiarFase(WorkoutViewModel.FaseWorkout fase) {
        // Fundido cruzado entre fases (calentamiento→ejercicio→timer→cardio→
        // resumen) — más amigable que el corte seco de setVisibility a secas.
        androidx.transition.TransitionManager.beginDelayedTransition(
                (android.view.ViewGroup) findViewById(R.id.rootWorkout),
                new androidx.transition.Fade().setDuration(200));
        ocultarTodos();
        switch (fase) {
            case CARGANDO:
                layoutCargando.setVisibility(View.VISIBLE);
                break;
            case CALENTAMIENTO:
                layoutRutina.setVisibility(View.VISIBLE);
                tvRutinaTitulo.setText("Calentamiento");
                mostrarItemRutina();
                break;
            case EJERCICIOS:
                // Empieza mostrando el ejercicio (registro RPE llega por swipe).
                // Si había un descanso en curso al cerrarse la app, esto lo
                // retoma (o lo da por terminado si ya venció) — ver
                // reanudarTimerSiAplica().
                layoutEjercicio.setVisibility(View.VISIBLE);
                mostrarEjercicio();
                reanudarTimerSiAplica();
                break;
            case ESTIRAMIENTOS:
                layoutRutina.setVisibility(View.VISIBLE);
                tvRutinaTitulo.setText("Estiramientos");
                mostrarItemRutina();
                break;
            case CARDIO:
                layoutCardio.setVisibility(View.VISIBLE);
                mostrarCardio();
                break;
            case RESUMEN:
                layoutResumen.setVisibility(View.VISIBLE);
                break;
            case ERROR:
                // No cerramos aquí: el observer de getError() ya muestra un
                // AlertDialog (cancelable=false) cuyo botón OK llama a finish().
                // Cerrar también aquí competía en una carrera con ese diálogo
                // y ganaba casi siempre, así que la pantalla se cerraba sola
                // en milisegundos sin que se llegase a leer el mensaje.
                break;
        }
    }

    private void ocultarTodos() {
        layoutCargando.setVisibility(View.GONE);
        layoutRutina.setVisibility(View.GONE);
        layoutEjercicio.setVisibility(View.GONE);
        layoutRegistro.setVisibility(View.GONE);
        layoutTimer.setVisibility(View.GONE);
        layoutCardio.setVisibility(View.GONE);
        layoutResumen.setVisibility(View.GONE);
    }

    // ─── Rutina (Calentamiento / Estiramientos) ───────────

    /**
     * Un item cada vez, igual de minimalista que la pantalla de Ejercicio —
     * ambas rutinas vienen del backend (getCalentamiento_/getEstiramientos_
     * en Codigo.gs), nunca hardcodeadas aquí. Swipe izquierda → siguiente
     * item (o siguiente fase si era el último).
     */
    private void mostrarItemRutina() {
        SesionResponse.ItemRutina item = viewModel.getItemRutinaActual();
        int total = viewModel.getTotalItemsRutina();
        if (item == null || total == 0) return;

        Integer idx = viewModel.getRutinaIdx().getValue();
        tvRutinaNombre.setText(item.nombre);
        tvRutinaValor.setText(item.reps);
        tvRutinaProgreso.setText(String.format(Locale.getDefault(),
                "%d / %d", (idx != null ? idx : 0) + 1, total));
        fadeIn(contenidoRutina);
    }

    /** Fundido de entrada corto para contenido que cambia (swipe entre items/ejercicios). */
    private void fadeIn(View v) {
        if (v == null) return;
        v.setAlpha(0f);
        v.animate().alpha(1f).setDuration(180).start();
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
        // Solo avisos de seguridad (⚠️, ej. dolor codo) — el resto de notas
        // (P1/P2/"Compuesto"...) son metadata interna, no para el usuario.
        if (ej.getNotas() != null && ej.getNotas().startsWith("⚠️")) {
            tvNotaEjercicio.setText(ej.getNotas());
            tvNotaEjercicio.setVisibility(View.VISIBLE);
        } else {
            tvNotaEjercicio.setVisibility(View.GONE);
        }
        tvSuperserie.setVisibility(
                ej.getSupersetGrupo() != null && !ej.getSupersetGrupo().isEmpty() ? View.VISIBLE : View.GONE);
        // "Serie X / Y" + indicador de volumen adaptativo (MAV) si el motor
        // subió/bajó las series de este ejercicio por readiness.
        String serieInfo = String.format(Locale.getDefault(),
                "Serie %d / %d", serie != null ? serie : 1, ej.getSeriesPlan());
        String volTxt = ej.getVolumenAdaptativoTexto();
        if (volTxt != null) serieInfo += "  ·  " + volTxt;
        tvSerieInfo.setText(serieInfo);
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
        fadeIn(contenidoEjercicio);
    }

    /**
     * Ajusta el peso real de la serie/ejercicio actual (botones −/+).
     * @param signo +1 o -1 — el tamaño del paso lo decide el propio ejercicio
     *              según su equipo (ver Ejercicio.getPasoPesoKg()).
     */
    private void ajustarPeso(int signo) {
        Ejercicio ej = viewModel.getEjercicioActual();
        if (ej == null) return;
        feedback.vibrateLight();
        ej.ajustarPesoActual(signo * ej.getPasoPesoKg());
        tvPesoSugerido.setText(ej.getPesoTexto());
    }

    // ─── Registro de serie ────────────────────────────────

    private void registrarConRir(int rir) {
        feedback.vibrateLight();
        int reps = pickerReps.getValue();
        viewModel.registrarSerie(reps, rir);
    }

    // ─── Timer ────────────────────────────────────────────

    private void mostrarTimer() {
        mostrarTimer(null);
    }

    /**
     * @param finishWhenMsForzado si viene de {@link #reanudarTimerSiAplica()}
     *                            (descanso retomado tras cierre/apagón), el
     *                            instante de fin YA guardado — se usa el
     *                            tiempo restante real en vez de reiniciar la
     *                            duración completa del descanso. Null en el
     *                            caso normal (descanso recién empezado).
     */
    private void mostrarTimer(Long finishWhenMsForzado) {
        layoutEjercicio.setVisibility(View.GONE);
        layoutRegistro.setVisibility(View.GONE);
        layoutTimer.setVisibility(View.VISIBLE);

        Ejercicio ej = viewModel.getEjercicioActual();
        int segundos = ej != null ? ej.getDescansoSeg() : 120;

        tvProximaSerie.setText("Descanso — próxima serie en:");
        // ejercicioActualIdx/serieActual ya reflejan la PRÓXIMA serie/ejercicio
        // en este punto (se actualizan antes de activar el timer).
        Integer serie = viewModel.getSerieActual().getValue();
        if (ej != null) {
            tvProximaInfo.setText(String.format(Locale.getDefault(),
                    "Próximo: Serie %d / %d — %s", serie != null ? serie : 1,
                    ej.getSeriesPlan(), ej.getPesoTexto()));
        } else {
            tvProximaInfo.setText("");
        }

        // Un único instante para los tres relojes (pantalla, notificación,
        // Hyper Island) — así cuentan exactamente lo mismo, sin desfases.
        long ahoraMs = System.currentTimeMillis();
        long finishWhenMs = finishWhenMsForzado != null ? finishWhenMsForzado : ahoraMs + (segundos * 1000L);
        long finishElapsedMs = SystemClock.elapsedRealtime() + (finishWhenMs - ahoraMs);

        // Iniciar TimerService (foreground + notificación/Hyper Island)
        Intent timerIntent = new Intent(this, TimerService.class);
        timerIntent.putExtra("segundos", segundos);
        timerIntent.putExtra("ejercicio", ej != null ? ej.getNombreCorto() : "");
        timerIntent.putExtra("finish_elapsed_ms", finishElapsedMs);
        timerIntent.putExtra("finish_when_ms", finishWhenMs);
        startForegroundService(timerIntent);

        // Cronómetro nativo EN PANTALLA — mismo instante que el de arriba, así
        // no depende de que lleguen broadcasts de tick para actualizarse.
        timerFinishElapsedMs = finishElapsedMs;
        tvTimerCountdown.setCountDown(true);
        tvTimerCountdown.setBase(finishElapsedMs);
        tvTimerCountdown.start();

        // Avance automático en el instante EXACTO en que este Chronometer
        // llega a 0 — no espera al broadcast de TimerService (ver campo
        // timerLocalHandler). El OnChronometerTickListener (vincularVistas)
        // es el segundo camino, atado al propio Chronometer visible.
        timerLocalHandler.removeCallbacksAndMessages(null);
        timerYaAvanzado = false;
        long retrasoMs = finishElapsedMs - SystemClock.elapsedRealtime();
        timerLocalHandler.postDelayed(this::avanzarSiguienteSerieAutomatico, Math.max(retrasoMs, 0));

        // Persistir el instante de fin (SharedPreferences, sobrevive a que
        // maten el proceso o se apague el móvil) — al reabrir, si el
        // descanso seguía en curso, reanudarTimerSiAplica() retoma el tiempo
        // restante en vez de perder la cuenta atrás. Va con el sesionId para
        // que un descanso de OTRA sesión (de ayer, de una prueba anterior)
        // nunca se confunda con uno de hoy.
        getSharedPreferences(PREFS_TIMER, MODE_PRIVATE).edit()
                .putLong("finish_when_ms", finishWhenMs)
                .putString("sesion_id", viewModel.getSesionId())
                .apply();
    }

    /** Llamado UNA vez, por el primero de los dos caminos (local o broadcast) que llegue. */
    private void avanzarSiguienteSerieAutomatico() {
        if (timerYaAvanzado) return;
        timerYaAvanzado = true;
        limpiarTimerGuardado();
        feedback.vibrateStrong();
        viewModel.timerCompletado();
    }

    private void ocultarTimer() {
        tvTimerCountdown.stop();
        timerFinishElapsedMs = -1;
        layoutTimer.setVisibility(View.GONE);
        // Siempre se vuelve al ejercicio (próxima serie/ejercicio), nunca
        // directo al registro — el registro solo llega por swipe explícito.
        if (viewModel.getFaseWorkout().getValue() == WorkoutViewModel.FaseWorkout.EJERCICIOS) {
            layoutEjercicio.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Se llama al entrar en fase EJERCICIOS (recién empezada o retomada tras
     * cierre/apagón). Si había un descanso en curso guardado:
     *   - si aún le queda tiempo → lo retoma con el tiempo restante real;
     *   - si ya venció mientras la app no estaba activa → se da por
     *     terminado sin más espera (el motor ya asume que el descanso pasó).
     * Si no había nada guardado, no hace nada (caso normal).
     */
    private void reanudarTimerSiAplica() {
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_TIMER, MODE_PRIVATE);
        long finishWhenMs = prefs.getLong("finish_when_ms", -1);
        if (finishWhenMs <= 0) return;

        // Descarta un descanso guardado de OTRA sesión (p.ej. de ayer, o de
        // una prueba anterior con la app) — solo se retoma el de HOY.
        String sesionGuardada = prefs.getString("sesion_id", null);
        if (sesionGuardada == null || !sesionGuardada.equals(viewModel.getSesionId())) {
            limpiarTimerGuardado();
            return;
        }

        if (finishWhenMs <= System.currentTimeMillis()) {
            limpiarTimerGuardado();
            feedback.vibrateStrong();
            viewModel.timerCompletado();
            return;
        }

        suprimirProximoMostrarTimer = true;
        viewModel.reanudarTimerActivo();
        mostrarTimer(finishWhenMs);
    }

    private void limpiarTimerGuardado() {
        getSharedPreferences(PREFS_TIMER, MODE_PRIVATE).edit().clear().apply();
    }

    /** Swipe izquierda en el timer → para el descanso antes de tiempo. */
    private void saltarDescanso() {
        // Cancela el avance automático programado — si no, dispararía tarde
        // (al cumplirse la duración original) sobre la serie que ya se saltó.
        timerLocalHandler.removeCallbacksAndMessages(null);
        timerYaAvanzado = true;
        limpiarTimerGuardado();
        stopService(new Intent(this, TimerService.class));
        viewModel.saltarDescanso();
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
            texto = "15-20 min bici estática\n60-70% FC máx (LISS)\n\n" +
                    "¿Por qué? Fase DEFINICIÓN → Viana 2019: LISS post-gym\n" +
                    "aumenta déficit calórico sin interferir (Wilson 2012)";
        } else {
            texto = "10 min bici estática\n60-70% FC máx (LISS)\n\n" +
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
        // TimerService manda el aviso con sendBroadcast() normal (no
        // LocalBroadcastManager) — hay que registrar el receptor igual,
        // si no, este receiver nunca se entera y el timer nunca "termina"
        // para la UI (aunque el Chronometer siga contando en negativo).
        androidx.core.content.ContextCompat.registerReceiver(this, timerFinishedReceiver,
                new android.content.IntentFilter(TimerService.ACTION_TIMER_FINISHED),
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(timerFinishedReceiver);
        // Fuera de la app manda el camino del broadcast (TimerService ya
        // gestiona vibración/heads-up ahí) — se cancela el local para no
        // vibrar por duplicado si el Handler disparase justo al salir.
        timerLocalHandler.removeCallbacksAndMessages(null);
    }

    private final android.content.BroadcastReceiver timerFinishedReceiver =
            new android.content.BroadcastReceiver() {
                @Override
                public void onReceive(android.content.Context context, Intent intent) {
                    // Camino "fuera de la app" (o si el local llega tarde) —
                    // comparte guard con avanzarSiguienteSerieAutomatico para
                    // no vibrar/avanzar dos veces.
                    avanzarSiguienteSerieAutomatico();
                }
            };
}
