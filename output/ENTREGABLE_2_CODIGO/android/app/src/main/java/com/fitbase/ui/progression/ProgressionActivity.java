package com.fitbase.ui.progression;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fitbase.R;
import com.fitbase.data.health.HealthConnectBridge;
import com.fitbase.data.model.MetricasProgresionResponse;

import java.util.Collections;
import java.util.Set;

/**
 * Pantalla de progresion de metricas clave.
 * Muestra historico de: Peso, Grasa%, Sueno, HRV, FC reposo, Volumen entreno.
 *
 * Si Health Connect no tiene permisos, los solicita antes de cargar.
 */
public class ProgressionActivity extends AppCompatActivity {

    private ProgressionViewModel viewModel;
    private ProgressBar progressBar;
    private TextView tvError;
    private int diasSeleccionados = 30;

    // Cards de resumen
    private TextView tvPesoActual, tvPesoCambio;
    private TextView tvGrasaActual;
    private TextView tvSleepMedia;
    private TextView tvPasosMedia;

    // Listas de datos
    private RecyclerView rvPeso, rvSueno, rvVolumen;

    // HC permission launcher
    @SuppressWarnings("unchecked")
    private final ActivityResultLauncher<Set<String>> hcPermLauncher =
            registerForActivityResult(HealthConnectBridge.getPermissionContract(),
                    granted -> {
                        if (granted != null && !granted.isEmpty()) {
                            // Permisos concedidos → recargar
                            viewModel.cargar(diasSeleccionados);
                        } else {
                            tvError.setText("Permisos de Health Connect denegados. " +
                                    "Ve a Ajustes → Apps → Health Connect → Permisos → FitBase y actívalos.");
                            tvError.setVisibility(View.VISIBLE);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progression);

        vincularVistas();
        viewModel = new ViewModelProvider(this).get(ProgressionViewModel.class);
        observar();

        // Solicitar permisos HC si no los tiene, luego cargar
        if (HealthConnectBridge.isAvailable(this) && !HealthConnectBridge.hasPermissions(this)) {
            try {
                hcPermLauncher.launch(HealthConnectBridge.getRequiredPermissions());
            } catch (Exception e) {
                viewModel.cargar(diasSeleccionados);
            }
        } else {
            viewModel.cargar(diasSeleccionados);
        }
    }

    private void vincularVistas() {
        progressBar = findViewById(R.id.progressBar);
        tvError = findViewById(R.id.tvError);

        tvPesoActual = findViewById(R.id.tvPesoActual);
        tvPesoCambio = findViewById(R.id.tvPesoCambio);
        tvGrasaActual = findViewById(R.id.tvGrasaActual);
        tvSleepMedia = findViewById(R.id.tvSleepMedia);
        tvPasosMedia = findViewById(R.id.tvPasosMedia);

        rvPeso = findViewById(R.id.rvPeso);
        rvSueno = findViewById(R.id.rvSueno);
        rvVolumen = findViewById(R.id.rvVolumen);

        rvPeso.setLayoutManager(new LinearLayoutManager(this));
        rvSueno.setLayoutManager(new LinearLayoutManager(this));
        rvVolumen.setLayoutManager(new LinearLayoutManager(this));

        // Boton 7d / 30d / 90d
        findViewById(R.id.btn7d).setOnClickListener(v -> { diasSeleccionados = 7; viewModel.cargar(7); });
        findViewById(R.id.btn30d).setOnClickListener(v -> { diasSeleccionados = 30; viewModel.cargar(30); });
        findViewById(R.id.btn90d).setOnClickListener(v -> { diasSeleccionados = 90; viewModel.cargar(90); });
    }

    private void observar() {
        viewModel.isCargando().observe(this, loading -> {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(this, err -> {
            if (err != null) {
                tvError.setText(err);
                tvError.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getDatos().observe(this, this::mostrarDatos);
    }

    private void mostrarDatos(MetricasProgresionResponse data) {
        if (data == null) {
            tvPesoActual.setText("— kg");
            tvPesoCambio.setText("Sin datos");
            tvGrasaActual.setText("—%");
            tvSleepMedia.setText("—");
            tvPasosMedia.setText("—");
            rvPeso.setAdapter(new MetricaAdapter(Collections.emptyList(), MetricaAdapter.TIPO_PESO));
            rvSueno.setAdapter(new MetricaAdapter(Collections.emptyList(), MetricaAdapter.TIPO_SUENO));
            rvVolumen.setAdapter(new MetricaAdapter(Collections.emptyList(), MetricaAdapter.TIPO_VOLUMEN));
            return;
        }
        tvError.setVisibility(View.GONE);

        // Resumen
        if (data.resumen != null) {
            if (data.resumen.pesoActual != null) {
                tvPesoActual.setText(String.format("%.1f kg", data.resumen.pesoActual));
                if (data.resumen.pesoInicio != null) {
                    float diff = data.resumen.pesoActual - data.resumen.pesoInicio;
                    String signo = diff >= 0 ? "+" : "";
                    tvPesoCambio.setText(String.format("%s%.1f kg", signo, diff));
                    tvPesoCambio.setTextColor(getColor(
                            diff >= 0 ? R.color.success : R.color.warning));
                }
            } else {
                tvPesoActual.setText("— kg");
                tvPesoCambio.setText("Sin datos");
            }

            if (data.resumen.grasaActual != null) {
                tvGrasaActual.setText(String.format("%.1f%%", data.resumen.grasaActual));
            } else {
                tvGrasaActual.setText("—%");
            }

            if (data.resumen.sleepMedia != null) {
                tvSleepMedia.setText(String.format("%d/100", data.resumen.sleepMedia));
            } else {
                tvSleepMedia.setText("—");
            }

            if (data.resumen.pasosMedia != null) {
                tvPasosMedia.setText(String.format("%,d", data.resumen.pasosMedia));
            } else {
                tvPasosMedia.setText("—");
            }
        }

        // Listas detalladas
        if (data.peso != null && !data.peso.isEmpty()) {
            rvPeso.setAdapter(new MetricaAdapter(data.peso, MetricaAdapter.TIPO_PESO));
        }
        if (data.zepp != null && !data.zepp.isEmpty()) {
            rvSueno.setAdapter(new MetricaAdapter(data.zepp, MetricaAdapter.TIPO_SUENO));
        }
        if (data.volumenEntreno != null && !data.volumenEntreno.isEmpty()) {
            rvVolumen.setAdapter(new MetricaAdapter(data.volumenEntreno, MetricaAdapter.TIPO_VOLUMEN));
        }
    }
}
