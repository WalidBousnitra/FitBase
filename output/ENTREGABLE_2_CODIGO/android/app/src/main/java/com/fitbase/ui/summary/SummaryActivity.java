package com.fitbase.ui.summary;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.fitbase.R;
import com.fitbase.util.FeedbackHelper;

/**
 * Pantalla de fin de sesión.
 * Muestra: puntuación, volumen total, tiempo, PRs, tips.
 */
public class SummaryActivity extends AppCompatActivity {

    private FeedbackHelper feedback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        feedback = FeedbackHelper.getInstance(this);

        // Datos del intent
        int tiempoTotal = getIntent().getIntExtra("tiempo_total", 0);
        float volumenTotal = getIntent().getFloatExtra("volumen_total", 0);

        // Vistas
        TextView tvTiempo = findViewById(R.id.tvTiempoTotal);
        TextView tvVolumen = findViewById(R.id.tvVolumenTotal);
        TextView tvPuntuacion = findViewById(R.id.tvPuntuacion);
        TextView tvTips = findViewById(R.id.tvTips);

        // Mostrar datos — sin emojis, texto limpio
        int min = tiempoTotal / 60;
        tvTiempo.setText(String.format("%dh %02dmin", min / 60, min % 60));
        tvVolumen.setText(String.format("%.0f kg", volumenTotal));

        // Puntuación
        int puntuacion = calcularPuntuacion(tiempoTotal, volumenTotal);
        tvPuntuacion.setText(String.valueOf(puntuacion));

        // Tips
        tvTips.setText(generarTips());

        // Botón cerrar con scale-on-press
        View btnCerrar = findViewById(R.id.btnCerrar);
        aplicarScaleOnPress(btnCerrar);
        btnCerrar.setOnClickListener(v -> {
            feedback.tap();
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private int calcularPuntuacion(int tiempoSeg, float volumen) {
        int puntos = 70;
        if (tiempoSeg > 3600) puntos += 10;
        if (volumen > 5000) puntos += 10;
        if (volumen > 8000) puntos += 10;
        return Math.min(puntos, 100);
    }

    private String generarTips() {
        StringBuilder tips = new StringBuilder();
        tips.append("Para hoy:\n\n");
        tips.append("• Toma batido proteína + carbos en los próximos 30 min\n");
        tips.append("• Duerme mínimo 8h esta noche\n");
        tips.append("• Mantén actividad ligera (paseo)\n");
        tips.append("• Hidratación extra: +500ml");
        return tips.toString();
    }

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
}
