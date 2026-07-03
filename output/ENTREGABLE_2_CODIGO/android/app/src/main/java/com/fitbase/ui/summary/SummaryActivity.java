package com.fitbase.ui.summary;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.fitbase.R;

/**
 * Pantalla de resumen post-sesión (standalone).
 * Normalmente el resumen se muestra DENTRO de WorkoutActivity (fase RESUMEN).
 * Esta Activity solo se usa si se navega directamente desde notificación.
 */
public class SummaryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        TextView tvSeries = findViewById(R.id.tvSummarySeries);
        TextView tvVolumen = findViewById(R.id.tvSummaryVolumen);
        TextView tvImpacto = findViewById(R.id.tvSummaryImpacto);
        Button btnCerrar = findViewById(R.id.btnSummaryCerrar);

        // Datos del intent
        int series = getIntent().getIntExtra("series", 0);
        int volumen = getIntent().getIntExtra("volumen", 0);
        String impacto = getIntent().getStringExtra("impacto");

        tvSeries.setText(series + " series");
        tvVolumen.setText(volumen + " kg total");
        tvImpacto.setText(impacto != null ? impacto : "Sesión registrada correctamente.");

        btnCerrar.setOnClickListener(v -> finish());
    }
}
