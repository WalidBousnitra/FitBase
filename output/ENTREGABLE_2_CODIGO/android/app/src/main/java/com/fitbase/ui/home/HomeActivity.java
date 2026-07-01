package com.fitbase.ui.home;

import android.animation.ObjectAnimator;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.lifecycle.ViewModelProvider;

import com.fitbase.R;
import com.fitbase.data.health.HealthConnectBridge;
import com.fitbase.ui.plan.PlanAnualActivity;
import com.fitbase.ui.progression.ProgressionActivity;
import com.fitbase.ui.test.TestRunnerActivity;
import com.fitbase.ui.workout.WorkoutActivity;
import com.fitbase.util.FeedbackHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

/**
 * Pantalla principal.
 * Muestra: calorías/macros RESTANTES, pasos, agua, sesión del día.
 * Datos se actualizan al abrir la app (onResume).
 * En preinicio usa objetivos base y datos reales disponibles de Health Connect.
 */
public class HomeActivity extends AppCompatActivity {

    private HomeViewModel viewModel;
    private FeedbackHelper feedback;

    // Health Connect permission launcher
    @SuppressWarnings("unchecked")
    private final ActivityResultLauncher<Set<String>> hcPermLauncher =
            registerForActivityResult(HealthConnectBridge.getPermissionContract(),
                    granted -> {
                        // Reload data after permissions granted
                        if (viewModel != null) viewModel.cargarDatosDelDia();
                    });

    // Vistas
    private TextView tvSaludo;
    private TextView tvFecha;
    private TextView tvCaloriasRestantes;
    private TextView tvCaloriasConsumidas;
    private TextView tvCaloriasObjetivo;
    private ProgressBar progressCalorias;
    private TextView tvProteinaRestante;
    private TextView tvCarbosRestantes;
    private TextView tvGrasasRestantes;
    private TextView tvPasos;
    private ProgressBar progressPasos;
    private TextView tvPasosObjetivo;
    private TextView tvAgua;

    private TextView tvSesionHoy;
    private TextView tvFaseInfo;
    private View btnEmpezarEntreno;
    private View btnPlanAnual;
    private View btnProgresion;
    private View btnRegistrarAusencia;
    private View btnTestRunner;
    private View bannerDemo;
    private View bannerAusencia;
    private TextView tvAusenciaMensaje;
    private String ultimaSesionRenderizada = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        feedback = FeedbackHelper.getInstance(this);
        vincularVistas();
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        observarDatos();
        configurarClicks();

        // Saludo según hora
        int hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hora < 12) tvSaludo.setText("Buenos días");
        else if (hora < 20) tvSaludo.setText("Buenas tardes");
        else tvSaludo.setText("Buenas noches");

        // Fecha
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE d 'de' MMMM", new Locale("es", "ES"));
        tvFecha.setText(sdf.format(new Date()));

        viewModel.cargarDatosDelDia();
        viewModel.checkAusencia();
        solicitarPermisosIniciales();
    }

    /**
     * Called from ViewModel observer if HC returned 0 steps (likely no perms).
     */
    private void solicitarPermisosHC() {
        try {
            if (!HealthConnectBridge.isAvailable(this)) return;
            if (HealthConnectBridge.hasPermissions(this)) return;
            hcPermLauncher.launch(HealthConnectBridge.getRequiredPermissions());
        } catch (Exception e) {
            android.widget.Toast.makeText(this,
                    "No se pudo abrir permisos de Health Connect",
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void solicitarPermisosIniciales() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
        }
        solicitarPermisosHC();
    }

    private void vincularVistas() {
        tvSaludo = findViewById(R.id.tvSaludo);
        tvFecha = findViewById(R.id.tvFecha);
        tvCaloriasRestantes = findViewById(R.id.tvCaloriasRestantes);
        tvCaloriasConsumidas = findViewById(R.id.tvCaloriasConsumidas);
        tvCaloriasObjetivo = findViewById(R.id.tvCaloriasObjetivo);
        progressCalorias = findViewById(R.id.progressCalorias);
        tvProteinaRestante = findViewById(R.id.tvProteinaRestante);
        tvCarbosRestantes = findViewById(R.id.tvCarbosRestantes);
        tvGrasasRestantes = findViewById(R.id.tvGrasasRestantes);
        tvPasos = findViewById(R.id.tvPasos);
        progressPasos = findViewById(R.id.progressPasos);
        tvPasosObjetivo = findViewById(R.id.tvPasosObjetivo);
        tvAgua = findViewById(R.id.tvAgua);

        tvSesionHoy = findViewById(R.id.tvSesionHoy);
        tvFaseInfo = findViewById(R.id.tvFaseInfo);
        btnEmpezarEntreno = findViewById(R.id.btnEmpezarEntreno);
        btnPlanAnual = findViewById(R.id.btnPlanAnual);
        btnProgresion = findViewById(R.id.btnProgresion);
        btnRegistrarAusencia = findViewById(R.id.btnRegistrarAusencia);
        btnTestRunner = findViewById(R.id.btnTestRunner);
        bannerDemo = findViewById(R.id.bannerDemo);
        bannerAusencia = findViewById(R.id.bannerAusencia);
        tvAusenciaMensaje = findViewById(R.id.tvAusenciaMensaje);
    }

    private void observarDatos() {
        // Macros + Calorías RESTANTES (objetivo - consumido vía FatSecret→HC)
        viewModel.getMacros().observe(this, macros -> {
            if (macros == null) return;

            int restantes = macros.getCaloriasRestantes();
            tvCaloriasRestantes.setText(String.format(Locale.getDefault(), "%,d", restantes));
            String tagOrigen = "";
            if (macros.esFallback) tagOrigen = " · FALLBACK";
            else if (macros.origenDatos != null && !macros.origenDatos.isEmpty()) {
                tagOrigen = " · " + macros.origenDatos;
            }
            tvCaloriasConsumidas.setText(String.format(Locale.getDefault(), "%,d consumidas%s", macros.caloriasConsumidas, tagOrigen));
            tvCaloriasObjetivo.setText(String.format(Locale.getDefault(), "de %,d kcal", macros.caloriasObjetivo));

            // Progreso animado
                int progresoPct = macros.caloriasObjetivo > 0
                    ? (macros.caloriasConsumidas * 100) / macros.caloriasObjetivo : 0;
                animarProgreso(progressCalorias, progresoPct);

            // Macros restantes
            tvProteinaRestante.setText(String.format(Locale.getDefault(), "%dg", macros.getProteinaRestante()));
            tvCarbosRestantes.setText(String.format(Locale.getDefault(), "%dg", macros.getCarbosRestantes()));
            tvGrasasRestantes.setText(String.format(Locale.getDefault(), "%dg", macros.getGrasasRestantes()));

            // Pasos (desde Zepp → Health Connect)
            tvPasos.setText(String.format(Locale.getDefault(), "%,d", macros.pasosActuales));
            tvPasosObjetivo.setText(String.format(Locale.getDefault(), "/ %,d", macros.pasosObjetivo));
            int progresoPasos = macros.pasosObjetivo > 0
                    ? (macros.pasosActuales * 100) / macros.pasosObjetivo : 0;
            animarProgreso(progressPasos, Math.min(progresoPasos, 100));

            // Si pasos = 0 y HC disponible, pedir permisos (primera vez)
            if (macros.pasosActuales == 0 && HealthConnectBridge.isAvailable(HomeActivity.this)) {
                solicitarPermisosHC();
            }

            // Agua — solo mostrar objetivo diario
            tvAgua.setText(String.format(Locale.getDefault(), "%.1fL", macros.aguaMl / 1000f));
        });

        // Sesión del día
        viewModel.getSesionHoy().observe(this, sesionResp -> {
            if (sesionResp != null && sesionResp.sesion != null) {
                String claveSesion = (sesionResp.sesion.getSesionId() != null && !sesionResp.sesion.getSesionId().isEmpty())
                        ? sesionResp.sesion.getSesionId()
                        : String.format(Locale.getDefault(), "%s_%d",
                        sesionResp.sesion.getTipo(),
                        sesionResp.sesion.getDuracionEstimadaMin());
                String texto = String.format("%s — %d ejercicios, ~%d min",
                        sesionResp.sesion.getTipo(),
                        sesionResp.ejercicios != null ? sesionResp.ejercicios.size() : 0,
                        sesionResp.sesion.getDuracionEstimadaMin());
                tvSesionHoy.setText(texto);
                tvFaseInfo.setText(sesionResp.mensaje != null ? sesionResp.mensaje : "");
                if (btnEmpezarEntreno.getVisibility() != View.VISIBLE) {
                    btnEmpezarEntreno.setVisibility(View.VISIBLE);
                }
                if (!claveSesion.equals(ultimaSesionRenderizada)) {
                    btnEmpezarEntreno.setAlpha(0f);
                    btnEmpezarEntreno.animate().alpha(1f).setDuration(250).start();
                    ultimaSesionRenderizada = claveSesion;
                } else {
                    btnEmpezarEntreno.setAlpha(1f);
                }
            } else {
                tvSesionHoy.setText("Descanso + Movilidad");
                tvFaseInfo.setText("Recuperación activa — Wall Angels + aductores + tríceps overhead");
                btnEmpezarEntreno.setVisibility(View.GONE);
                ultimaSesionRenderizada = "";
            }
        });

        // Ausencia
        viewModel.getAusenciaDetectada().observe(this, ausencia -> {
            if (ausencia != null && ausencia.totalPerdidos > 0) {
                bannerAusencia.setVisibility(View.VISIBLE);
                tvAusenciaMensaje.setText(ausencia.mensaje);
            } else {
                bannerAusencia.setVisibility(View.GONE);
            }
        });

        // Modo demo
        viewModel.isModoDemo().observe(this, esDemo -> {
            bannerDemo.setVisibility(Boolean.TRUE.equals(esDemo) ? View.VISIBLE : View.GONE);
            btnTestRunner.setVisibility(Boolean.TRUE.equals(esDemo) ? View.VISIBLE : View.GONE);
        });
    }

    private void animarProgreso(ProgressBar bar, int target) {
        int actual = bar.getProgress();
        if (actual == target) return;
        ObjectAnimator anim = ObjectAnimator.ofInt(bar, "progress", actual, target);
        anim.setDuration(800);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();
    }

    private void configurarClicks() {
        // Aplicar scale-on-press a botones interactivos
        aplicarScaleOnPress(btnEmpezarEntreno);
        aplicarScaleOnPress(btnPlanAnual);
        aplicarScaleOnPress(btnProgresion);
        aplicarScaleOnPress(btnRegistrarAusencia);

        btnEmpezarEntreno.setOnClickListener(v -> {
            feedback.tap();
            Intent intent = new Intent(this, WorkoutActivity.class);
            if (Boolean.TRUE.equals(viewModel.isModoDemo().getValue())) {
                // Preinicio: permitir entrenamiento de prueba
                android.widget.Toast.makeText(this,
                        "Modo preinicio — entrenamiento de prueba activo",
                        android.widget.Toast.LENGTH_SHORT).show();
            }
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        btnPlanAnual.setOnClickListener(v -> {
            feedback.tap();
            Intent intent = new Intent(this, PlanAnualActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        btnProgresion.setOnClickListener(v -> {
            feedback.tap();
            Intent intent = new Intent(this, ProgressionActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        btnRegistrarAusencia.setOnClickListener(v -> mostrarDialogoAusencia());

        btnTestRunner.setOnClickListener(v -> {
            feedback.tap();
            startActivity(new Intent(this, TestRunnerActivity.class));
        });
    }

    /**
     * Scale-on-press: sutil efecto de presión (0.97) con spring physics.
     */
    private void aplicarScaleOnPress(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).start();
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

    private void mostrarDialogoAusencia() {
        new AlertDialog.Builder(this)
                .setTitle("Registrar ausencia extendida")
                .setMessage("En modo simplificado solo está disponible la detección automática de ausencia (últimos 7 días).")
                .setPositiveButton("Entendido", (d, w) -> viewModel.checkAusencia())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar datos cada vez que se abre la app (HC sync + backend)
        viewModel.cargarDatosDelDia();
        if (HealthConnectBridge.isAvailable(this) && !HealthConnectBridge.hasPermissions(this)) {
            solicitarPermisosHC();
        }
    }
}
