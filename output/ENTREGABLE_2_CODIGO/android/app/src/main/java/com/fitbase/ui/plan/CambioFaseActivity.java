package com.fitbase.ui.plan;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.fitbase.R;
import com.fitbase.data.api.ApiClient;
import com.fitbase.data.model.CambioFaseResponse;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Cierre de la fase que acaba de terminar (adherencia, peso, sueño) +
 * presentación animada de la fase que empieza. Se lanza:
 *   - De verdad: desde HomeActivity, cuando detecta que fase_id cambió desde
 *     la última vez (SharedPreferences) — llama a GET cambio_fase.
 *   - En demo: desde el botón de previsualización junto al de test, con
 *     datos de ejemplo, para ver la animación sin esperar a un cambio real.
 */
public class CambioFaseActivity extends AppCompatActivity {

    public static final String EXTRA_DEMO = "demo";

    private View bloqueAnterior, bloqueNueva;
    private TextView tvFlecha;
    private TextView tvFaseAnteriorNombre, tvAdherencia, tvCambioPeso, tvSleepMedia;
    private TextView tvFaseNuevaNombre, tvFaseNuevaFoco, tvFaseNuevaDetalle;
    private Button btnContinuar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cambio_fase);
        com.fitbase.util.InsetsHelper.aplicarInsetsSistema(this);

        bloqueAnterior = findViewById(R.id.bloqueAnterior);
        bloqueNueva = findViewById(R.id.bloqueNueva);
        tvFlecha = findViewById(R.id.tvFlecha);
        tvFaseAnteriorNombre = findViewById(R.id.tvFaseAnteriorNombre);
        tvAdherencia = findViewById(R.id.tvAdherencia);
        tvCambioPeso = findViewById(R.id.tvCambioPeso);
        tvSleepMedia = findViewById(R.id.tvSleepMedia);
        tvFaseNuevaNombre = findViewById(R.id.tvFaseNuevaNombre);
        tvFaseNuevaFoco = findViewById(R.id.tvFaseNuevaFoco);
        tvFaseNuevaDetalle = findViewById(R.id.tvFaseNuevaDetalle);
        btnContinuar = findViewById(R.id.btnContinuar);
        btnContinuar.setOnClickListener(v -> finish());

        if (getIntent().getBooleanExtra(EXTRA_DEMO, false)) {
            mostrarDatos(datosDemo());
            return;
        }

        ApiClient.getApi().getCambioFase("cambio_fase").enqueue(new Callback<CambioFaseResponse>() {
            @Override
            public void onResponse(Call<CambioFaseResponse> call, Response<CambioFaseResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isHayCambio()) {
                    mostrarDatos(response.body());
                } else {
                    finish(); // Nada que mostrar (llamada por error sin cambio real)
                }
            }
            @Override
            public void onFailure(Call<CambioFaseResponse> call, Throwable t) {
                finish();
            }
        });
    }

    private void mostrarDatos(CambioFaseResponse data) {
        boolean hayAnterior = data.getFaseAnterior() != null && data.getResumenFaseAnterior() != null;
        bloqueAnterior.setVisibility(hayAnterior ? View.VISIBLE : View.GONE);
        tvFlecha.setVisibility(hayAnterior ? View.VISIBLE : View.GONE);

        if (hayAnterior) {
            CambioFaseResponse.FaseResumen anterior = data.getFaseAnterior();
            CambioFaseResponse.ResumenFaseAnterior resumen = data.getResumenFaseAnterior();
            tvFaseAnteriorNombre.setText(anterior.nombre);
            tvAdherencia.setText(resumen.sesionesCompletadas + "/" + resumen.sesionesTotales);

            if (resumen.pesoInicio != null && resumen.pesoFin != null) {
                float diff = resumen.pesoFin - resumen.pesoInicio;
                String signo = diff >= 0 ? "+" : "";
                tvCambioPeso.setText(String.format(Locale.getDefault(), "%s%.1f kg", signo, diff));
            } else {
                tvCambioPeso.setText("— kg");
            }
            tvSleepMedia.setText(resumen.sleepMedia != null ? String.valueOf(resumen.sleepMedia) : "—");
        }

        CambioFaseResponse.FaseNueva nueva = data.getFaseActual();
        tvFaseNuevaNombre.setText(nueva.nombre);
        tvFaseNuevaFoco.setText(nueva.foco);
        tvFaseNuevaDetalle.setText(String.format(Locale.getDefault(),
                "%d semanas · RIR %s · %s", nueva.semanas, nueva.rirRango, nueva.nutri));

        animarEntrada(hayAnterior);
    }

    /**
     * Revelado secuencial: fase anterior primero (cierre), luego la flecha,
     * luego la fase nueva (apertura) — sensación de "pasar página".
     */
    private void animarEntrada(boolean hayAnterior) {
        bloqueNueva.setTranslationY(40f);

        if (hayAnterior) {
            bloqueAnterior.animate().alpha(1f).setDuration(400).setStartDelay(150).start();
            tvFlecha.animate().alpha(1f).setDuration(300).setStartDelay(550).start();
            bloqueNueva.animate().alpha(1f).translationY(0f).setDuration(450).setStartDelay(800)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override public void onAnimationEnd(Animator animation) {
                            feedbackSuave();
                        }
                    }).start();
        } else {
            // Primera fase del año — solo hay fase nueva que presentar.
            bloqueNueva.animate().alpha(1f).translationY(0f).setDuration(450).setStartDelay(200).start();
        }
    }

    private void feedbackSuave() {
        try {
            com.fitbase.util.FeedbackHelper.getInstance(this).success();
        } catch (Exception ignored) {}
    }

    /** Datos de ejemplo para el botón de previsualización (sin llamar al backend). */
    private CambioFaseResponse datosDemo() {
        CambioFaseResponse data = new CambioFaseResponse();
        // Reflexión no disponible (campos privados) — se construye vía Gson
        // en el caso real; para demo usamos un JSON de ejemplo parseado igual
        // que la respuesta real, así el código de pintado es idéntico.
        String json = "{"
                + "\"hay_cambio\":true,"
                + "\"fase_anterior\":{\"fase_id\":\"FAS_01\",\"nombre\":\"Adaptación + Postura\",\"tipo\":\"VOL\",\"foco\":\"Full Body + Correctivos posturales\"},"
                + "\"resumen_fase_anterior\":{\"sesiones_completadas\":18,\"sesiones_totales\":20,\"peso_inicio\":78.2,\"peso_fin\":79.1,\"sleep_media\":81},"
                + "\"fase_actual\":{\"fase_id\":\"FAS_02\",\"nombre\":\"Hipertrofia I — V-Taper\",\"tipo\":\"VOL\",\"foco\":\"Hombros+Espalda (P1: V-taper)\",\"nutri\":\"bulk\",\"rir_rango\":\"2-3\",\"semanas\":6}"
                + "}";
        return new com.google.gson.Gson().fromJson(json, CambioFaseResponse.class);
    }
}
