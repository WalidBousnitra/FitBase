package com.fitbase.ui.summary;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.fitbase.R;

/**
 * Pantalla de fin de sesión.
 * Muestra: puntuación, volumen total, tiempo, PRs, tips.
 * Referencia: REG-DEV-01 (ui.md) § 5
 */
public class SummaryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        // Datos del intent
        int tiempoTotal = getIntent().getIntExtra("tiempo_total", 0);
        float volumenTotal = getIntent().getFloatExtra("volumen_total", 0);

        // Vistas
        TextView tvTiempo = findViewById(R.id.tvTiempoTotal);
        TextView tvVolumen = findViewById(R.id.tvVolumenTotal);
        TextView tvPuntuacion = findViewById(R.id.tvPuntuacion);
        TextView tvTips = findViewById(R.id.tvTips);

        // Mostrar datos
        int min = tiempoTotal / 60;
        tvTiempo.setText(String.format("⏱ %dh %02dmin", min / 60, min % 60));
        tvVolumen.setText(String.format("📊 %.0f kg", volumenTotal));

        // Puntuación (heurística: basada en completar la sesión)
        int puntuacion = calcularPuntuacion(tiempoTotal, volumenTotal);
        tvPuntuacion.setText(String.valueOf(puntuacion));

        // Tips personalizados (REG-DEV-01 § 5.2)
        tvTips.setText(generarTips());

        // Botón cerrar
        findViewById(R.id.btnCerrar).setOnClickListener(v -> finish());
    }

    private int calcularPuntuacion(int tiempoSeg, float volumen) {
        // Heurística simple: 70 base + bonus por completar
        int puntos = 70;
        if (tiempoSeg > 3600) puntos += 10; // >1h
        if (volumen > 5000) puntos += 10;   // Buen volumen
        if (volumen > 8000) puntos += 10;   // Volumen alto
        return Math.min(puntos, 100);
    }

    private String generarTips() {
        // Tips basados en tipo de sesión (ui.md § 5.2)
        StringBuilder tips = new StringBuilder();
        tips.append("💡 PARA HOY:\n\n");
        tips.append("• Toma batido proteína + carbos en los próximos 30 min\n");
        tips.append("• Duerme mínimo 8h esta noche\n");
        tips.append("• Mantén actividad ligera (paseo)\n");
        tips.append("• Hidratación extra: +500ml");
        return tips.toString();
    }
}
