package com.fitbase.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.fitbase.R;
import com.fitbase.ui.plan.PlanAnualActivity;
import com.fitbase.ui.workout.WorkoutActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Pantalla principal de la mañana.
 * Muestra: macros del día, sesión planificada, métricas.
 * Al abrir detecta si hubo días sin usar la app → redistribuye volumen.
 * Para ausencias largas (≥1 semana) → opción de registrar manualmente.
 * Referencia: REG-DEV-01 (ui.md) § 3
 */
public class HomeActivity extends AppCompatActivity {

    private HomeViewModel viewModel;

    // Vistas
    private TextView tvFecha;
    private TextView tvCaloriasObjetivo;
    private TextView tvMacros;
    private TextView tvPasos;
    private TextView tvAgua;
    private TextView tvSesionHoy;
    private View btnEmpezarEntreno;
    private View btnPlanAnual;
    private View btnRegistrarAusencia;
    private View bannerDemo;
    private View bannerAusencia;
    private TextView tvAusenciaMensaje;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        vincularVistas();
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        observarDatos();
        configurarClicks();

        // Mostrar fecha actual
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE d MMM", new Locale("es", "ES"));
        tvFecha.setText(sdf.format(new Date()));

        // Cargar datos + verificar ausencia automática
        viewModel.cargarDatosDelDia();
        viewModel.checkAusencia();
    }

    private void vincularVistas() {
        tvFecha = findViewById(R.id.tvFecha);
        tvCaloriasObjetivo = findViewById(R.id.tvCaloriasObjetivo);
        tvMacros = findViewById(R.id.tvMacros);
        tvPasos = findViewById(R.id.tvPasos);
        tvAgua = findViewById(R.id.tvAgua);
        tvSesionHoy = findViewById(R.id.tvSesionHoy);
        btnEmpezarEntreno = findViewById(R.id.btnEmpezarEntreno);
        btnPlanAnual = findViewById(R.id.btnPlanAnual);
        btnRegistrarAusencia = findViewById(R.id.btnRegistrarAusencia);
        bannerDemo = findViewById(R.id.bannerDemo);
        bannerAusencia = findViewById(R.id.bannerAusencia);
        tvAusenciaMensaje = findViewById(R.id.tvAusenciaMensaje);
    }

    private void observarDatos() {
        // Macros del día — conectado con FatSecret
        viewModel.getMacros().observe(this, macros -> {
            if (macros != null) {
                tvCaloriasObjetivo.setText(String.valueOf(macros.caloriasObjetivo));
                String macrosTexto = String.format(Locale.getDefault(),
                        "P %dg  C %dg  G %dg",
                        macros.proteinaG, macros.carbosG, macros.grasasG);
                tvMacros.setText(macrosTexto);
                tvAgua.setText(String.format(Locale.getDefault(), "%.1fL", macros.aguaMl / 1000f));
            }
        });

        // Sesión del día
        viewModel.getSesionHoy().observe(this, sesionResp -> {
            if (sesionResp != null && sesionResp.sesion != null) {
                String texto = String.format("Hoy: %s (%d ejercicios, ~%d min)",
                        sesionResp.sesion.getTipo(),
                        sesionResp.ejercicios != null ? sesionResp.ejercicios.size() : 0,
                        sesionResp.sesion.getDuracionEstimadaMin());
                tvSesionHoy.setText(texto);
                btnEmpezarEntreno.setVisibility(View.VISIBLE);
            } else {
                tvSesionHoy.setText("Día de descanso");
                btnEmpezarEntreno.setVisibility(View.GONE);
            }
        });

        // Detectar días perdidos (no abrió la app)
        viewModel.getAusenciaDetectada().observe(this, ausencia -> {
            if (ausencia != null && ausencia.totalPerdidos > 0) {
                bannerAusencia.setVisibility(View.VISIBLE);
                tvAusenciaMensaje.setText(ausencia.mensaje);
            } else {
                bannerAusencia.setVisibility(View.GONE);
            }
        });

        // Modo demo (si fecha actual < FECHA_INICIO)
        viewModel.isModoDemo().observe(this, esDemo -> {
            bannerDemo.setVisibility(esDemo ? View.VISIBLE : View.GONE);
        });
    }

    private void configurarClicks() {
        btnEmpezarEntreno.setOnClickListener(v -> {
            Intent intent = new Intent(this, WorkoutActivity.class);
            startActivity(intent);
        });

        btnPlanAnual.setOnClickListener(v -> {
            Intent intent = new Intent(this, PlanAnualActivity.class);
            startActivity(intent);
        });

        // Botón para registrar ausencia extendida (≥1 semana)
        btnRegistrarAusencia.setOnClickListener(v -> mostrarDialogoAusencia());
    }

    /**
     * Diálogo para registrar ausencia extendida manualmente.
     * Para períodos ≥1 semana, permite indicar fechas y redistribuir plan.
     */
    private void mostrarDialogoAusencia() {
        new AlertDialog.Builder(this)
                .setTitle("Registrar ausencia extendida")
                .setMessage("¿Has estado sin entrenar 1 semana o más? "
                        + "Indica las fechas para redistribuir tu plan correctamente.")
                .setPositiveButton("Registrar", (d, w) -> {
                    // Abrir pantalla de registro de ausencia con date pickers
                    viewModel.registrarAusenciaExtendida();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.cargarDatosDelDia();
    }
}
