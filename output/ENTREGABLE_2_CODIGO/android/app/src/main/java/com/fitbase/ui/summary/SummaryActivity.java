package com.fitbase.ui.summary;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.fitbase.R;
import com.fitbase.ui.BaseActivity;

/**
 * Pantalla de resumen post-sesión (standalone).
 * Normalmente el resumen se muestra DENTRO de WorkoutActivity (fase RESUMEN).
 * Esta Activity solo se usa si se navega directamente desde notificación.
 */
public class SummaryActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);
        com.fitbase.util.InsetsHelper.aplicarInsetsSistema(this);

        TextView tvSeries = findViewById(R.id.tvSummarySeries);
        TextView tvVolumen = findViewById(R.id.tvSummaryVolumen);
        TextView tvRir = findViewById(R.id.tvSummaryRir);
        TextView tvIntensidad = findViewById(R.id.tvSummaryIntensidad);
        TextView tvImpacto = findViewById(R.id.tvSummaryImpacto);
        Button btnCerrar = findViewById(R.id.btnSummaryCerrar);

        // Datos del intent
        int series = getIntent().getIntExtra("series", 0);
        int volumen = getIntent().getIntExtra("volumen", 0);
        float rirMedio = getIntent().getFloatExtra("rir_medio", 0f);
        String intensidad = getIntent().getStringExtra("intensidad");
        String impacto = getIntent().getStringExtra("impacto");

        tvSeries.setText(String.valueOf(series));
        tvVolumen.setText(volumen + " kg");
        tvRir.setText(String.valueOf(rirMedio));
        tvIntensidad.setText(intensidad != null ? intensidad : "");
        tvIntensidad.setVisibility(intensidad != null ? View.VISIBLE : View.GONE);
        tvImpacto.setText(impacto != null ? impacto : "Sesión registrada correctamente.");

        btnCerrar.setOnClickListener(v -> finish());
    }
}
