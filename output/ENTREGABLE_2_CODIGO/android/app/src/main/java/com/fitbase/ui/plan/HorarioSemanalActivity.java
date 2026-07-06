package com.fitbase.ui.plan;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.fitbase.R;
import com.fitbase.data.api.ApiClient;
import com.fitbase.data.model.ActualizarHorarioResponse;
import com.fitbase.data.model.HorarioSemanalResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Horario semanal — NO es un editor libre de "qué tipo a qué día".
 *
 * El split (Push/Pierna/Pull/Hombros 4x/sem + natación 2x/sem + descanso
 * 1x/sem) está diseñado para cumplir frecuencia y volumen por grupo
 * muscular (programacion.md) — dejar elegir cualquier combinación podría
 * romper esas reglas (p.ej. dos días de gym seguidos sin natación de por
 * medio, o perder un día de alguno de los 4 tipos). Lo único que de verdad
 * varía en la vida real es:
 *   (a) qué 2 días nadas — fijo a Lunes+Miércoles o Martes+Jueves, las
 *       únicas franjas que ofrece la piscina;
 *   (b) qué día descansas — el resto de días de gym se reparten SIEMPRE
 *       en el mismo orden (Push→Pierna→Pull→Hombros) sobre los 4 días que
 *       quedan libres, en orden cronológico desde el lunes.
 *
 * Con esas 2 elecciones se genera el horario completo (10 combinaciones
 * posibles: 2 opciones de natación × 5 días de descanso posibles) y se
 * manda tal cual al backend existente (accion=actualizar_horario) — el
 * backend no necesita saber que viene de un preset, solo recibe un
 * horario válido.
 */
public class HorarioSemanalActivity extends AppCompatActivity {

    // Orden ISO de la semana (lunes primero) usado tanto para generar el
    // horario como para pintar la vista previa — getDay() del backend usa
    // 0=domingo..6=sábado, aquí solo se itera en ese orden.
    private static final int[] ORDEN_SEMANA = {1, 2, 3, 4, 5, 6, 0};
    private static final String[] NOMBRE_POR_DIA = {
            "Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"
    };
    private static final String[] ABREV_POR_DIA = {
            "Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"
    };
    private static final String[] TIPOS_GYM_EN_ORDEN = {"PUSH", "PIERNA", "PULL", "HOMBR"};
    private static final Map<String, String> ETIQUETA_TIPO = new HashMap<>();
    static {
        ETIQUETA_TIPO.put("PUSH", "Push");
        ETIQUETA_TIPO.put("PIERNA", "Pierna");
        ETIQUETA_TIPO.put("PULL", "Pull");
        ETIQUETA_TIPO.put("HOMBR", "Hombros");
        ETIQUETA_TIPO.put("NATACION", "Natación");
        ETIQUETA_TIPO.put("DESCANSO", "Descanso");
    }

    // Las 2 únicas franjas de piscina disponibles (usuario/perfil/horarios.md).
    private static final int[] NAT_LUN_MIE = {1, 3};
    private static final int[] NAT_MAR_JUE = {2, 4};

    private int[] natacionElegida = NAT_MAR_JUE; // por defecto: horario actual
    private int diaDescansoElegido = 0; // Domingo, por defecto: horario actual

    private TextView opNatLunMie, opNatMarJue;
    private LinearLayout filaDescanso, filaPreview;
    private TextView tvEstado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_horario_semanal);
        com.fitbase.util.InsetsHelper.aplicarInsetsSistema(this);

        opNatLunMie = findViewById(R.id.opNatLunMie);
        opNatMarJue = findViewById(R.id.opNatMarJue);
        filaDescanso = findViewById(R.id.filaDescanso);
        filaPreview = findViewById(R.id.filaPreview);
        tvEstado = findViewById(R.id.tvEstado);

        opNatLunMie.setOnClickListener(v -> elegirNatacion(NAT_LUN_MIE));
        opNatMarJue.setOnClickListener(v -> elegirNatacion(NAT_MAR_JUE));

        ((android.widget.TextView) findViewById(R.id.tvHeaderTitulo)).setText("Horario semanal");
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());
        findViewById(R.id.btnGuardar).setOnClickListener(v -> guardar());

        redibujar();
        cargarHorarioActual();
    }

    private void elegirNatacion(int[] opcion) {
        natacionElegida = opcion;
        // Si el descanso elegido ahora cae en un día de natación, mover al
        // primer día libre disponible.
        if (esDiaNatacion(diaDescansoElegido)) {
            diaDescansoElegido = diasDescansoPosibles().get(0);
        }
        redibujar();
    }

    private boolean esDiaNatacion(int dia) {
        return dia == natacionElegida[0] || dia == natacionElegida[1];
    }

    private List<Integer> diasDescansoPosibles() {
        List<Integer> posibles = new ArrayList<>();
        for (int dia : ORDEN_SEMANA) {
            if (!esDiaNatacion(dia)) posibles.add(dia);
        }
        return posibles;
    }

    /** Genera el horario completo a partir de las 2 elecciones del usuario. */
    private Map<Integer, String> generarHorario() {
        Map<Integer, String> horario = new LinkedHashMap<>();
        int idxGym = 0;
        for (int dia : ORDEN_SEMANA) {
            if (esDiaNatacion(dia)) {
                horario.put(dia, "NATACION");
            } else if (dia == diaDescansoElegido) {
                horario.put(dia, "DESCANSO");
            } else {
                horario.put(dia, TIPOS_GYM_EN_ORDEN[idxGym]);
                idxGym++;
            }
        }
        return horario;
    }

    private void redibujar() {
        marcarSeleccion(opNatLunMie, natacionElegida == NAT_LUN_MIE);
        marcarSeleccion(opNatMarJue, natacionElegida == NAT_MAR_JUE);

        filaDescanso.removeAllViews();
        for (int dia : diasDescansoPosibles()) {
            TextView op = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            if (filaDescanso.getChildCount() > 0) lp.setMarginStart(dpToPx(4));
            op.setLayoutParams(lp);
            op.setGravity(android.view.Gravity.CENTER);
            op.setMinHeight(dpToPx(40));
            op.setTextSize(10f);
            op.setText(NOMBRE_POR_DIA[dia]);
            final int diaFinal = dia;
            op.setOnClickListener(v -> {
                diaDescansoElegido = diaFinal;
                redibujar();
            });
            marcarSeleccion(op, dia == diaDescansoElegido);
            filaDescanso.addView(op);
        }

        Map<Integer, String> horario = generarHorario();
        filaPreview.removeAllViews();
        for (int dia : ORDEN_SEMANA) {
            TextView celda = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            celda.setLayoutParams(lp);
            celda.setGravity(android.view.Gravity.CENTER);
            celda.setTextSize(9f);
            celda.setTextColor(getColor(R.color.colorTextSecondary));
            String tipo = horario.get(dia);
            celda.setText(ABREV_POR_DIA[dia] + "\n" + ETIQUETA_TIPO.get(tipo));
            filaPreview.addView(celda);
        }
    }

    private void marcarSeleccion(TextView view, boolean activo) {
        view.setBackgroundResource(activo ? R.drawable.bg_nivel_activo : R.drawable.button_secondary);
        view.setTextColor(getColor(activo ? R.color.white : R.color.colorTextSecondary));
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void cargarHorarioActual() {
        ApiClient.getApi().getHorarioSemanal("horario_semanal").enqueue(new Callback<HorarioSemanalResponse>() {
            @Override
            public void onResponse(Call<HorarioSemanalResponse> call, Response<HorarioSemanalResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().horario != null) {
                    aplicarHorarioCargado(response.body().horario);
                } else {
                    mostrarEstado("No se pudo leer el horario actual — se muestra el de por defecto.", R.color.warning);
                }
            }

            @Override
            public void onFailure(Call<HorarioSemanalResponse> call, Throwable t) {
                mostrarEstado("Sin conexión: " + t.getMessage(), R.color.error);
            }
        });
    }

    /**
     * Detecta a qué combinación (natación + descanso) corresponde el
     * horario que ya está guardado en el backend, para preseleccionarla.
     * Si el horario guardado no coincide con ninguna de las 10 combinaciones
     * válidas (p.ej. quedó de una versión anterior), se deja el valor por
     * defecto y se avisa — guardar aplicará la combinación elegida en pantalla.
     */
    private void aplicarHorarioCargado(Map<String, String> horario) {
        List<Integer> diasNatacion = new ArrayList<>();
        Integer descanso = null;
        for (Map.Entry<String, String> e : horario.entrySet()) {
            int dia;
            try {
                dia = Integer.parseInt(e.getKey());
            } catch (NumberFormatException ex) {
                continue;
            }
            if ("NATACION".equals(e.getValue())) diasNatacion.add(dia);
            if ("DESCANSO".equals(e.getValue())) descanso = dia;
        }

        boolean coincideLunMie = diasNatacion.size() == 2 && diasNatacion.contains(1) && diasNatacion.contains(3);
        boolean coincideMarJue = diasNatacion.size() == 2 && diasNatacion.contains(2) && diasNatacion.contains(4);

        if (coincideLunMie) natacionElegida = NAT_LUN_MIE;
        else if (coincideMarJue) natacionElegida = NAT_MAR_JUE;
        else {
            mostrarEstado("El horario guardado no coincide con ninguna de estas combinaciones — elige una y guarda para aplicarla.", R.color.warning);
        }

        if (descanso != null && !esDiaNatacion(descanso)) diaDescansoElegido = descanso;

        redibujar();
    }

    private void guardar() {
        Map<Integer, String> horarioInt = generarHorario();
        Map<String, String> horario = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> e : horarioInt.entrySet()) {
            horario.put(String.valueOf(e.getKey()), e.getValue());
        }

        Map<String, Object> datos = new HashMap<>();
        datos.put("accion", "actualizar_horario");
        datos.put("horario", horario);

        mostrarEstado("Guardando...", R.color.colorTextSecondary);

        ApiClient.getApi().actualizarHorario(datos).enqueue(new Callback<ActualizarHorarioResponse>() {
            @Override
            public void onResponse(Call<ActualizarHorarioResponse> call, Response<ActualizarHorarioResponse> response) {
                ActualizarHorarioResponse body = response.body();
                if (response.isSuccessful() && body != null && body.ok) {
                    mostrarEstado(body.mensaje != null ? body.mensaje
                            : "Horario actualizado (" + body.sesionesGeneradas + " sesiones regeneradas).", R.color.success);
                } else if (body != null && body.error != null) {
                    // Error de validación real (p.ej. tipo inválido) — reintentarlo
                    // igual no lo arreglaría, así que NO se encola.
                    mostrarEstado(body.error, R.color.error);
                } else {
                    // Backend caído/error de servidor — encolar como el resto de
                    // POSTs: se regenerará el horario en cuanto haya conexión,
                    // idéntico a pulsar Guardar de nuevo (guardarHorarioSemanal_
                    // es idempotente con el mismo horario).
                    com.fitbase.data.local.SyncManager.encolar(HorarioSemanalActivity.this, datos);
                    mostrarEstado("Sin conexión — se aplicará en cuanto la haya.", R.color.warning);
                }
            }

            @Override
            public void onFailure(Call<ActualizarHorarioResponse> call, Throwable t) {
                com.fitbase.data.local.SyncManager.encolar(HorarioSemanalActivity.this, datos);
                mostrarEstado("Sin conexión — se aplicará en cuanto la haya.", R.color.warning);
            }
        });
    }

    private void mostrarEstado(String texto, int colorRes) {
        tvEstado.setText(texto);
        tvEstado.setTextColor(getColor(colorRes));
        tvEstado.setVisibility(View.VISIBLE);
    }
}
