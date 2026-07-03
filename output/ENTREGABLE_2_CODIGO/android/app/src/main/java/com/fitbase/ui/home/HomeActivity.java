package com.fitbase.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.fitbase.R;
import com.fitbase.data.health.HealthConnectBridge;
import com.fitbase.data.model.VistaMañanaResponse;
import com.fitbase.ui.plan.PlanAnualActivity;
import com.fitbase.ui.progression.ProgressionActivity;
import com.fitbase.ui.workout.WorkoutActivity;
import com.fitbase.util.FeedbackHelper;

import java.util.Set;

/**
 * Pantalla principal — Vista Mañana.
 *
 * FLUJO USUARIO:
 *   1. Se despierta, se pesa, abre la app.
 *   2. Ve: sueño, macros del día (cambian por fase), movilidad matutina,
 *      pasos/cardio objetivo, tipo de sesión.
 *   3. A lo largo del día: ve macros RESTANTES a simple vista.
 *   4. Cuando llega al gym: pulsa "Empezar entreno".
 */
public class HomeActivity extends AppCompatActivity {

    private HomeViewModel viewModel;
    private FeedbackHelper feedback;

    @SuppressWarnings("unchecked")
    private final ActivityResultLauncher<Set<String>> hcPermLauncher =
            registerForActivityResult(HealthConnectBridge.getPermissionContract(),
                    granted -> { if (viewModel != null) viewModel.cargarDatosDelDia(); });

    // ─── Vistas: Header ───
    private TextView tvSaludo, tvFecha, tvTipoDia, tvFaseNombre;

    // ─── Vistas: Sueño ───
    private TextView tvSleepScore, tvFcReposo, tvAvisoFatiga;

    // ─── Vistas: Macros restantes ───
    private TextView tvCaloriasRestantes, tvProtRestante, tvCarbosRestantes, tvGrasasRestantes;
    private ProgressBar progressCalorias;
    private TextView tvAgua;

    // ─── Vistas: Pasos y Cardio ───
    private TextView tvPasos, tvPasosObj, tvCardioInfo;
    private ProgressBar progressPasos;

    // ─── Vistas: Movilidad matutina ───
    private LinearLayout layoutMovilidad;
    private TextView tvMovilidadTitulo;

    // ─── Vistas: Sesión ───
    private TextView tvSesionTipo, tvSesionFase;
    private View btnEmpezarEntreno;

    // ─── Vistas: Navegación ───
    private View btnPlanAnual, btnProgresion;

    // ─── Vistas: Banners ───
    private View bannerAusencia;
    private TextView tvAusenciaMensaje;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        feedback = FeedbackHelper.getInstance(this);
        vincularVistas();
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        observarDatos();

        // Pedir permisos Health Connect
        HealthConnectBridge.requestPermissionsIfNeeded(this, hcPermLauncher);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.cargarDatosDelDia();
    }

    private void vincularVistas() {
        tvSaludo = findViewById(R.id.tvSaludo);
        tvFecha = findViewById(R.id.tvFecha);
        tvTipoDia = findViewById(R.id.tvTipoDia);
        tvFaseNombre = findViewById(R.id.tvFaseNombre);

        tvSleepScore = findViewById(R.id.tvSleepScore);
        tvFcReposo = findViewById(R.id.tvFcReposo);
        tvAvisoFatiga = findViewById(R.id.tvAvisoFatiga);

        tvCaloriasRestantes = findViewById(R.id.tvCaloriasRestantes);
        tvProtRestante = findViewById(R.id.tvProtRestante);
        tvCarbosRestantes = findViewById(R.id.tvCarbosRestantes);
        tvGrasasRestantes = findViewById(R.id.tvGrasasRestantes);
        progressCalorias = findViewById(R.id.progressCalorias);
        tvAgua = findViewById(R.id.tvAgua);

        tvPasos = findViewById(R.id.tvPasos);
        tvPasosObj = findViewById(R.id.tvPasosObj);
        tvCardioInfo = findViewById(R.id.tvCardioInfo);
        progressPasos = findViewById(R.id.progressPasos);

        layoutMovilidad = findViewById(R.id.layoutMovilidad);
        tvMovilidadTitulo = findViewById(R.id.tvMovilidadTitulo);

        tvSesionTipo = findViewById(R.id.tvSesionTipo);
        tvSesionFase = findViewById(R.id.tvSesionFase);
        btnEmpezarEntreno = findViewById(R.id.btnEmpezarEntreno);
        btnPlanAnual = findViewById(R.id.btnPlanAnual);
        btnProgresion = findViewById(R.id.btnProgresion);

        bannerAusencia = findViewById(R.id.bannerAusencia);
        tvAusenciaMensaje = findViewById(R.id.tvAusenciaMensaje);

        // Navegación
        btnEmpezarEntreno.setOnClickListener(v -> {
            feedback.vibrateLight();
            startActivity(new Intent(this, WorkoutActivity.class));
        });
        btnPlanAnual.setOnClickListener(v -> startActivity(new Intent(this, PlanAnualActivity.class)));
        btnProgresion.setOnClickListener(v -> startActivity(new Intent(this, ProgressionActivity.class)));
    }

    private void observarDatos() {
        // Header
        tvSaludo.setText(viewModel.getSaludoHora());
        tvFecha.setText(viewModel.getFechaFormateada());

        // Vista mañana (endpoint único con todo)
        viewModel.getVistaMañana().observe(this, this::actualizarVistaMañana);

        // Macros consumidas (Health Connect / FatSecret) → calcular restantes
        viewModel.getCaloriasConsumidas().observe(this, c -> recalcularMacrosRestantes());
        viewModel.getProteinaConsumida().observe(this, p -> recalcularMacrosRestantes());
        viewModel.getCarbosConsumidos().observe(this, c -> recalcularMacrosRestantes());
        viewModel.getGrasasConsumidas().observe(this, g -> recalcularMacrosRestantes());
        viewModel.getPasosActuales().observe(this, p -> actualizarPasos());

        // Loading y errores
        viewModel.getCargando().observe(this, loading -> {
            // Mostrar/ocultar skeleton
        });
    }

    private void actualizarVistaMañana(VistaMañanaResponse vista) {
        if (vista == null) return;

        // ── Tipo de día ──
        String tipoDiaTexto;
        switch (vista.getTipoDia()) {
            case "gym": tipoDiaTexto = "🏋️ DÍA DE GYM"; break;
            case "natacion": tipoDiaTexto = "🏊 NATACIÓN"; break;
            default: tipoDiaTexto = "😴 DESCANSO"; break;
        }
        tvTipoDia.setText(tipoDiaTexto);

        // ── Fase ──
        if (vista.getFase() != null) {
            tvFaseNombre.setText(vista.getFase().nombre);
            tvFaseNombre.setVisibility(View.VISIBLE);
        }

        // ── Sueño ──
        if (vista.getSueno() != null) {
            Integer score = vista.getSueno().sleepScore;
            Integer fc = vista.getSueno().hrReposo;
            tvSleepScore.setText(score != null ? score + "/100" : "—");
            tvFcReposo.setText(fc != null ? fc + " bpm" : "—");

            if (score != null && score < 60) {
                tvAvisoFatiga.setText("⚠️ Sueño pobre — sesión reducida");
                tvAvisoFatiga.setVisibility(View.VISIBLE);
            } else {
                tvAvisoFatiga.setVisibility(View.GONE);
            }
        }

        // ── Macros objetivo (cambian por fase) ──
        recalcularMacrosRestantes();

        // ── Cardio y pasos ──
        if (vista.getCardio() != null) {
            tvPasosObj.setText("/ " + vista.getCardio().pasosObjetivo);
            progressPasos.setMax(vista.getCardio().pasosObjetivo);

            if (vista.getCardio().cardioPostGymMin > 0) {
                tvCardioInfo.setText("⏱️ " + vista.getCardio().cardioPostGymMin + " min " +
                        (vista.getCardio().modalidad != null ? vista.getCardio().modalidad : "bici") +
                        " post-gym");
                tvCardioInfo.setVisibility(View.VISIBLE);
            } else {
                tvCardioInfo.setVisibility(View.GONE);
            }
        }
        actualizarPasos();

        // ── Movilidad matutina ──
        if (vista.getMovilidadMatutina() != null) {
            tvMovilidadTitulo.setText("🧘 Movilidad matutina (" +
                    vista.getMovilidadMatutina().duracionMin + " min)");
            layoutMovilidad.removeAllViews();
            for (VistaMañanaResponse.EjercicioMovilidad ej : vista.getMovilidadMatutina().ejercicios) {
                TextView tv = new TextView(this);
                tv.setText("• " + ej.nombre + " — " + ej.reps);
                tv.setTextColor(getColor(R.color.text_secondary));
                tv.setPadding(0, 4, 0, 4);
                layoutMovilidad.addView(tv);
            }
            layoutMovilidad.setVisibility(View.VISIBLE);
        }

        // ── Sesión del día ──
        if (vista.esGym()) {
            btnEmpezarEntreno.setVisibility(View.VISIBLE);
        } else {
            btnEmpezarEntreno.setVisibility(View.GONE);
        }

        // ── Agua ──
        if (vista.getMacros() != null) {
            tvAgua.setText(String.format("💧 %,.0f L", vista.getMacros().aguaMl / 1000f));
        }

        // ── Aviso ausencia ──
        if (vista.getAvisoAusencia() != null) {
            tvAusenciaMensaje.setText(vista.getAvisoAusencia().mensaje);
            bannerAusencia.setVisibility(View.VISIBLE);
        } else {
            bannerAusencia.setVisibility(View.GONE);
        }
    }

    private void recalcularMacrosRestantes() {
        VistaMañanaResponse vista = viewModel.getVistaMañana().getValue();
        if (vista == null || vista.getMacros() == null) return;

        Integer consumidas = viewModel.getCaloriasConsumidas().getValue();
        Integer protCons = viewModel.getProteinaConsumida().getValue();
        Integer carbCons = viewModel.getCarbosConsumidos().getValue();
        Integer grasCons = viewModel.getGrasasConsumidas().getValue();

        int kcalObj = vista.getMacros().calorias;
        int kcalCons = consumidas != null ? consumidas : 0;
        int kcalRest = kcalObj - kcalCons;

        tvCaloriasRestantes.setText(String.valueOf(Math.max(0, kcalRest)));
        progressCalorias.setMax(kcalObj);
        progressCalorias.setProgress(kcalCons);

        int protObj = vista.getMacros().proteinaG;
        int protC = protCons != null ? protCons : 0;
        tvProtRestante.setText((protObj - protC) + "g");

        int carbObj = vista.getMacros().carbosG;
        int carbC = carbCons != null ? carbCons : 0;
        tvCarbosRestantes.setText((carbObj - carbC) + "g");

        int grasObj = vista.getMacros().grasasG;
        int grasC = grasCons != null ? grasCons : 0;
        tvGrasasRestantes.setText((grasObj - grasC) + "g");
    }

    private void actualizarPasos() {
        Integer pasos = viewModel.getPasosActuales().getValue();
        int p = pasos != null ? pasos : 0;
        tvPasos.setText(String.format("%,d", p));
        progressPasos.setProgress(p);
    }
}
