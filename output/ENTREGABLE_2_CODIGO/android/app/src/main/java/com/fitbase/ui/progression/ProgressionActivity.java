package com.fitbase.ui.progression;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fitbase.R;
import com.fitbase.data.model.MetricasProgresionResponse;
import com.google.android.material.button.MaterialButton;

import java.util.Collections;
import java.util.List;

/**
 * Pantalla de progresion de metricas clave.
 * Muestra historico de: Peso, Grasa%, Sueno, HRV, FC reposo, Volumen entreno.
 *
 * Lee ÚNICAMENTE de la BBDD (backend) — no toca Health Connect directamente.
 * La sincronización Health Connect → BBDD ocurre una vez al día en
 * SplashActivity (ver DailySyncManager), antes de que se pueda llegar aquí.
 */
public class ProgressionActivity extends AppCompatActivity {

    private ProgressionViewModel viewModel;
    private ProgressBar progressBar;
    private TextView tvError;
    private int diasSeleccionados = 7;

    // Cards de resumen
    private TextView tvPesoActual, tvPesoCambio;
    private TextView tvGrasaActual, tvGrasaCambio;
    private TextView tvSleepMedia;
    private TextView tvPasosMedia;

    // Listas de datos
    private RecyclerView rvSubjetiva;
    private ProgresionChartView chartProgresion;
    private TextView tvEstadoEmoji;

    // Filtro de rango (7d/30d/90d) — el seleccionado se marca en tonal accent,
    // el resto en superficie neutra (ver actualizarFiltroSeleccionado).
    private MaterialButton btn7d, btn30d, btn90d;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progression);
        com.fitbase.util.InsetsHelper.aplicarInsetsSistema(this);

        vincularVistas();
        viewModel = new ViewModelProvider(this).get(ProgressionViewModel.class);
        observar();
        viewModel.cargar(diasSeleccionados);
    }

    private void vincularVistas() {
        progressBar = findViewById(R.id.progressBar);
        tvError = findViewById(R.id.tvError);

        tvPesoActual = findViewById(R.id.tvPesoActual);
        tvPesoCambio = findViewById(R.id.tvPesoCambio);
        tvGrasaActual = findViewById(R.id.tvGrasaActual);
        tvGrasaCambio = findViewById(R.id.tvGrasaCambio);
        tvSleepMedia = findViewById(R.id.tvSleepMedia);
        tvPasosMedia = findViewById(R.id.tvPasosMedia);

        chartProgresion = findViewById(R.id.chartProgresion);
        tvEstadoEmoji = findViewById(R.id.tvEstadoEmoji);
        rvSubjetiva = findViewById(R.id.rvSubjetiva);

        rvSubjetiva.setLayoutManager(new LinearLayoutManager(this));

        // Header compartido (partial_header.xml) — sin acción a la derecha.
        ((TextView) findViewById(R.id.tvHeaderTitulo)).setText("Progresión");
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());

        // Botón 7d / 30d / 90d
        btn7d = findViewById(R.id.btn7d);
        btn30d = findViewById(R.id.btn30d);
        btn90d = findViewById(R.id.btn90d);
        btn7d.setOnClickListener(v -> { diasSeleccionados = 7; viewModel.cargar(7); actualizarFiltroSeleccionado(btn7d); });
        btn30d.setOnClickListener(v -> { diasSeleccionados = 30; viewModel.cargar(30); actualizarFiltroSeleccionado(btn30d); });
        btn90d.setOnClickListener(v -> { diasSeleccionados = 90; viewModel.cargar(90); actualizarFiltroSeleccionado(btn90d); });
        actualizarFiltroSeleccionado(btn7d);
    }

    /** Marca visualmente qué filtro de rango está activo (chip tonal accent vs. superficie neutra). */
    private void actualizarFiltroSeleccionado(MaterialButton activo) {
        for (MaterialButton btn : new MaterialButton[]{btn7d, btn30d, btn90d}) {
            boolean seleccionado = btn == activo;
            btn.setBackgroundResource(seleccionado ? R.drawable.bg_chip_accent : R.drawable.button_secondary);
            btn.setTextColor(getColor(seleccionado ? R.color.colorAccentPrimary : R.color.colorTextSecondary));
        }
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
            tvGrasaCambio.setText("Sin datos");
            tvSleepMedia.setText("—");
            tvPasosMedia.setText("—");
            chartProgresion.setDatos(null);
            tvEstadoEmoji.setText("—");
            rvSubjetiva.setAdapter(new MetricaAdapter(Collections.emptyList(), MetricaAdapter.TIPO_SUBJETIVA));
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
                if (data.resumen.grasaInicio != null) {
                    float diffGrasa = data.resumen.grasaActual - data.resumen.grasaInicio;
                    String signoGrasa = diffGrasa >= 0 ? "+" : "";
                    tvGrasaCambio.setText(String.format("%s%.1f%%", signoGrasa, diffGrasa));
                    // Al revés que el peso: bajar % de grasa es lo deseable
                    // (independientemente de si estás en bulk o cut).
                    tvGrasaCambio.setTextColor(getColor(
                            diffGrasa <= 0 ? R.color.success : R.color.warning));
                } else {
                    tvGrasaCambio.setText("Sin datos");
                }
            } else {
                tvGrasaActual.setText("—%");
                tvGrasaCambio.setText("Sin datos");
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

        // Grafica: Peso, Grasa, Sueno y Pasos — las 4 metricas de las cards de arriba,
        // todas salen de metricas_zepp (centralizado).
        chartProgresion.setDatos(data.zepp);

        // Energia/estres: emoji del registro subjetivo mas reciente (lista en
        // orden ascendente por fecha, igual que zepp — el ultimo es el actual).
        if (data.subjetiva != null && !data.subjetiva.isEmpty()) {
            rvSubjetiva.setAdapter(new MetricaAdapter(data.subjetiva, MetricaAdapter.TIPO_SUBJETIVA));
            MetricasProgresionResponse.SubjetivaEntry ultimo = data.subjetiva.get(data.subjetiva.size() - 1);
            tvEstadoEmoji.setText(emojiEstado(ultimo.energia, ultimo.estres));
        } else {
            tvEstadoEmoji.setText("—");
        }
    }

    /**
     * Un unico emoji resumen del ultimo registro subjetivo (energia/estres,
     * escala 1-5 cada uno). Score = energia - estres, rango -4..+4.
     */
    private String emojiEstado(Integer energia, Integer estres) {
        if (energia == null && estres == null) return "—";
        int e = energia != null ? energia : 3;
        int s = estres != null ? estres : 3;
        int score = e - s;
        if (score >= 3) return "🤩";
        if (score >= 1) return "🙂";
        if (score == 0) return "😐";
        if (score >= -2) return "😕";
        return "😩";
    }
}
