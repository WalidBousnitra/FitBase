package com.fitbase.ui.workout;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.lifecycle.ViewModelProvider;

import com.fitbase.R;
import com.fitbase.data.model.Ejercicio;
import com.fitbase.service.TimerService;
import com.fitbase.ui.summary.SummaryActivity;
import com.fitbase.util.FeedbackHelper;

/**
 * Pantalla de entrenamiento en el gym.
 * Flujo: Ejercicio → Registro RIR → Timer → Siguiente serie/ejercicio
 * Referencia: REG-DEV-01 (ui.md) § 4
 */
public class WorkoutActivity extends AppCompatActivity {

    private WorkoutViewModel viewModel;

    // Vistas ejercicio activo
    private TextView tvNombreEjercicio;
    private TextView tvPesoSugerido;
    private TextView tvRepsObjetivo;
    private TextView tvSerieActual;
    private TextView tvProgreso;
    private TextView tvTiempoTotal;

    // Vistas registro RIR
    private View layoutRegistro;
    private TextView tvRepsInput;
    private Button[] botonesRir;

    // Vistas timer
    private View layoutTimer;
    private TextView tvTimerCountdown;
    private TextView tvProximaSerie;
    private View layoutCargando;

    private int estadoActual = ESTADO_EJERCICIO;
    private static final int ESTADO_EJERCICIO = 0;
    private static final int ESTADO_REGISTRO = 1;
    private static final int ESTADO_TIMER = 2;

    private FeedbackHelper feedback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        feedback = FeedbackHelper.getInstance(this);
        vincularVistas();
        viewModel = new ViewModelProvider(this).get(WorkoutViewModel.class);
        observarDatos();
        configurarGestos();

        viewModel.cargarSesion();
    }

    private void vincularVistas() {
        tvNombreEjercicio = findViewById(R.id.tvNombreEjercicio);
        tvPesoSugerido = findViewById(R.id.tvPesoSugerido);
        tvRepsObjetivo = findViewById(R.id.tvRepsObjetivo);
        tvSerieActual = findViewById(R.id.tvSerieActual);
        tvProgreso = findViewById(R.id.tvProgreso);
        tvTiempoTotal = findViewById(R.id.tvTiempoTotal);

        layoutRegistro = findViewById(R.id.layoutRegistro);
        tvRepsInput = findViewById(R.id.tvRepsInput);

        layoutTimer = findViewById(R.id.layoutTimer);
        tvTimerCountdown = findViewById(R.id.tvTimerCountdown);
        tvProximaSerie = findViewById(R.id.tvProximaSerie);
        layoutCargando = findViewById(R.id.layoutCargando);

        // Botones RIR (0, 1, 2, 3, 4+)
        botonesRir = new Button[5];
        botonesRir[0] = findViewById(R.id.btnRir0);
        botonesRir[1] = findViewById(R.id.btnRir1);
        botonesRir[2] = findViewById(R.id.btnRir2);
        botonesRir[3] = findViewById(R.id.btnRir3);
        botonesRir[4] = findViewById(R.id.btnRir4);
    }

    private void observarDatos() {
        // Loading state
        viewModel.isCargando().observe(this, cargando -> {
            boolean enCarga = Boolean.TRUE.equals(cargando);
            layoutCargando.setVisibility(enCarga ? View.VISIBLE : View.GONE);
            if (enCarga) {
                layoutCargando.bringToFront();
            }
        });

        // Ejercicio actual
        viewModel.getEjercicioActual().observe(this, this::mostrarEjercicio);

        // Serie actual
        viewModel.getSerieActual().observe(this, serie -> {
            if (serie != null) {
                Ejercicio ej = viewModel.getEjercicioActual().getValue();
                if (ej != null) {
                    tvSerieActual.setText(String.format("Serie %d / %d", serie, ej.getSeriesPlan()));
                }
            }
        });

        // Timer de descanso
        viewModel.getTimerSegundos().observe(this, segundos -> {
            if (segundos != null && segundos > 0) {
                int min = segundos / 60;
                int seg = segundos % 60;
                tvTimerCountdown.setText(String.format("%d:%02d", min, seg));
                // Tick háptico en últimos 5 segundos
                if (segundos <= 5) {
                    feedback.tick();
                }
            } else if (segundos != null && segundos == 0 && estadoActual == ESTADO_TIMER) {
                // Timer terminó → volver a estado ejercicio
                layoutTimer.setVisibility(View.GONE);
                estadoActual = ESTADO_EJERCICIO;
                stopService(new Intent(this, TimerService.class));
            }
        });

        // Sesión completada
        viewModel.isSesionCompletada().observe(this, completada -> {
            if (Boolean.TRUE.equals(completada)) {
                irAResumen();
            }
        });

        // Tiempo total
        viewModel.getTiempoTotalSegundos().observe(this, totalSeg -> {
            if (totalSeg != null) {
                int min = totalSeg / 60;
                int seg = totalSeg % 60;
                tvTiempoTotal.setText(String.format("%d:%02d", min, seg));
            }
        });
    }

    private void mostrarEjercicio(Ejercicio ejercicio) {
        if (ejercicio == null) return;

        estadoActual = ESTADO_EJERCICIO;
        layoutRegistro.setVisibility(View.GONE);
        layoutTimer.setVisibility(View.GONE);

        tvNombreEjercicio.setText(ejercicio.getNombre());

        if (ejercicio.getPesoSugerido() > 0) {
            tvPesoSugerido.setText(String.format("%.1f kg", ejercicio.getPesoSugerido()));
            tvPesoSugerido.setVisibility(View.VISIBLE);
        } else {
            tvPesoSugerido.setVisibility(View.GONE);
        }

        tvRepsObjetivo.setText(ejercicio.getRepsPlan() + " reps");

        int indice = viewModel.getIndiceEjercicio();
        int total = viewModel.getTotalEjercicios();
        tvProgreso.setText(String.format("%d/%d", indice + 1, total));
    }

    private void configurarGestos() {
        // Swipe izquierda → completar serie
        View contenedor = findViewById(R.id.contenedorPrincipal);
        contenedor.setOnTouchListener(new SwipeListener(this) {
            @Override
            public void onSwipeLeft() {
                feedback.tap();
                if (estadoActual == ESTADO_EJERCICIO) {
                    mostrarRegistroRIR();
                } else if (estadoActual == ESTADO_TIMER) {
                    saltarTimer();
                }
            }

            @Override
            public void onSwipeRight() {
                // Volver o cancelar
            }
        });

        // Botones RIR
        for (int i = 0; i < botonesRir.length; i++) {
            final int rir = i;
            aplicarScaleOnPress(botonesRir[i]);
            botonesRir[i].setOnClickListener(v -> confirmarSerie(rir));
        }
    }

    /**
     * Scale-on-press: efecto de presión sutil (0.95) con spring bounce.
     */
    private void aplicarScaleOnPress(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    SpringAnimation springX = new SpringAnimation(v, SpringAnimation.SCALE_X, 1f);
                    springX.getSpring().setStiffness(SpringForce.STIFFNESS_MEDIUM);
                    springX.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
                    springX.start();
                    SpringAnimation springY = new SpringAnimation(v, SpringAnimation.SCALE_Y, 1f);
                    springY.getSpring().setStiffness(SpringForce.STIFFNESS_MEDIUM);
                    springY.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
                    springY.start();
                    break;
            }
            return false;
        });
    }

    private void mostrarRegistroRIR() {
        // Reps + RIR se muestran en la MISMA pantalla (layout unificado)
        estadoActual = ESTADO_REGISTRO;
        layoutRegistro.setVisibility(View.VISIBLE);

        Ejercicio ej = viewModel.getEjercicioActual().getValue();
        if (ej != null) {
            // Reps por defecto = objetivo (editable en la misma pantalla)
            String reps = ej.getRepsPlan();
            if (reps.contains("-")) {
                tvRepsInput.setText(reps.split("-")[1]); // Tomar max del rango
            } else {
                tvRepsInput.setText(reps);
            }
            // Los botones RIR se muestran debajo del input de reps en el mismo layout
        }
    }

    /**
     * Confirma serie con RIR seleccionado.
     * Regla ACSM: Si completó +1-2 reps con RIR >= objetivo → motor sugiere subir peso.
     */
    private void confirmarSerie(int rirPercibido) {
        feedback.confirm();
        int repsReales;
        try {
            repsReales = Integer.parseInt(tvRepsInput.getText().toString());
        } catch (NumberFormatException e) {
            repsReales = 10; // fallback
        }

        Ejercicio ej = viewModel.getEjercicioActual().getValue();
        float pesoUsado = ej != null ? ej.getPesoSugerido() : 0;

        viewModel.registrarSerie(repsReales, rirPercibido, pesoUsado);

        // Si quedan más series → Timer
        if (viewModel.quedanSeriesPorHacer()) {
            iniciarTimer();
        } else {
            // Siguiente ejercicio
            viewModel.siguienteEjercicio();
        }
    }

    private void iniciarTimer() {
        estadoActual = ESTADO_TIMER;
        layoutRegistro.setVisibility(View.GONE);
        layoutTimer.setVisibility(View.VISIBLE);

        Ejercicio ej = viewModel.getEjercicioActual().getValue();
        int descansoSeg = ej != null ? ej.getDescansoSeg() : 120;
        int minInit = descansoSeg / 60;
        int segInit = descansoSeg % 60;
        tvTimerCountdown.setText(String.format("%d:%02d", minInit, segInit));

        // Info próxima serie
        Integer serieActual = viewModel.getSerieActual().getValue();
        if (serieActual != null && ej != null) {
            tvProximaSerie.setText(String.format("Próximo: Serie %d/%d\n%.1f kg x %s",
                    serieActual + 1, ej.getSeriesPlan(),
                    ej.getPesoSugerido(), ej.getRepsPlan()));
        }

        // Iniciar timer con servicio foreground
        viewModel.iniciarTimer(descansoSeg);
        Intent timerIntent = new Intent(this, TimerService.class);
        timerIntent.putExtra("segundos", descansoSeg);
        timerIntent.putExtra("ejercicio_nombre", ej != null ? ej.getNombre() : "");
        try {
            ContextCompat.startForegroundService(this, timerIntent);
        } catch (Exception e) {
            // Fallback defensivo para evitar cierre de la pantalla si el SO bloquea FGS.
            startService(timerIntent);
        }
    }

    private void saltarTimer() {
        viewModel.saltarTimer();
        layoutTimer.setVisibility(View.GONE);
        estadoActual = ESTADO_EJERCICIO;
        // Timer service se para
        stopService(new Intent(this, TimerService.class));
    }

    private void irAResumen() {
        feedback.success();
        Intent intent = new Intent(this, SummaryActivity.class);
        intent.putExtra("sesion_id", viewModel.getSesionId());
        Integer tiempoSeg = viewModel.getTiempoTotalSegundos().getValue();
        intent.putExtra("tiempo_total", tiempoSeg != null ? tiempoSeg : 0);
        intent.putExtra("volumen_total", viewModel.getVolumenTotal());
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, TimerService.class));
    }
}
