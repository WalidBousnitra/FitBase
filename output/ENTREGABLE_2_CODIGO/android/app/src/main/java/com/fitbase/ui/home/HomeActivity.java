package com.fitbase.ui.home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.fitbase.R;
import com.fitbase.data.health.HealthConnectBridge;
import com.fitbase.data.model.VistaMañanaResponse;
import com.fitbase.ui.plan.PlanAnualActivity;
import com.fitbase.ui.progression.ProgressionActivity;
import com.fitbase.ui.summary.SummaryActivity;
import com.fitbase.ui.test.TestRunnerActivity;
import com.fitbase.ui.workout.WorkoutActivity;
import com.fitbase.util.FeedbackHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
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
    private Button btnEmpezarEntreno;

    // ─── Vistas: Navegación ───
    private View btnPlanAnual, btnProgresion, btnTest, btnPreviewFase, btnPreviewEntreno;

    // ─── Vistas: Banners ───
    private View bannerAusencia;
    private TextView tvAusenciaMensaje;
    private View bannerRamadan;
    private TextView tvRamadanTitulo, tvRamadanDetalle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        com.fitbase.util.InsetsHelper.aplicarInsetsSistema(this);

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
        comprobarMetricasSubjetivas();
        // Por si el proceso se mató del todo y se reabre desde Home en vez de
        // Workout — la alarma de "descanso terminado" se calla sola igual.
        com.fitbase.service.TimerDetenerReceiver.detenerAlarma(this);
    }

    /**
     * Tras las 22:00, una vez al día: pregunta energía y estrés de hoy (barras
     * de 5 niveles) para captar el desgaste físico/mental del día completo.
     * Gating con SharedPreferences (mismo patrón que DailySyncManager) — se
     * marca como "ya mostrado hoy" en cuanto se muestra, así aunque se cierre
     * sin responder no vuelve a insistir hasta mañana.
     */
    private void comprobarMetricasSubjetivas() {
        if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 22) return;

        String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        SharedPreferences prefs = getSharedPreferences("fitbase_subjetiva", MODE_PRIVATE);
        if (hoy.equals(prefs.getString("fecha_mostrada", ""))) return;

        prefs.edit().putString("fecha_mostrada", hoy).apply();
        mostrarDialogoMetricasSubjetivas();
    }

    /**
     * Detecta si la fase actual es distinta a la última vista (SharedPreferences)
     * y, si es así, lanza CambioFaseActivity (resumen fase anterior + fase
     * nueva). La primera vez que se abre la app en este dispositivo (sin valor
     * guardado todavía) NO se muestra — solo guarda la fase actual como punto
     * de partida, para no disparar la pantalla en una instalación nueva a
     * mitad de plan.
     */
    private void comprobarCambioFase(String faseIdActual) {
        if (faseIdActual == null) return;
        SharedPreferences prefs = getSharedPreferences("fitbase_fase", MODE_PRIVATE);
        String faseIdGuardada = prefs.getString("ultima_fase_id", null);

        if (faseIdGuardada == null) {
            prefs.edit().putString("ultima_fase_id", faseIdActual).apply();
            return;
        }
        if (faseIdGuardada.equals(faseIdActual)) return;

        prefs.edit().putString("ultima_fase_id", faseIdActual).apply();
        startActivity(new Intent(this, com.fitbase.ui.plan.CambioFaseActivity.class));
    }

    private void mostrarDialogoMetricasSubjetivas() {
        View view = getLayoutInflater().inflate(R.layout.dialog_metricas_subjetivas, null);
        ViewGroup filaEnergia = view.findViewById(R.id.filaEnergia);
        ViewGroup filaEstres = view.findViewById(R.id.filaEstres);
        EditText etNotas = view.findViewById(R.id.etNotas);

        int[] energia = configurarSelectorNivel(filaEnergia, 3);
        int[] estres = configurarSelectorNivel(filaEstres, 3);

        new AlertDialog.Builder(this)
                .setTitle("¿Cómo ha sido tu día?")
                .setView(view)
                .setCancelable(false)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    feedback.vibrateLight();
                    String notas = etNotas.getText().toString().trim();
                    viewModel.guardarMetricasSubjetivas(energia[0], estres[0], notas);
                })
                .setNegativeButton("Ahora no", null)
                .show();
    }

    /**
     * Selector de 5 segmentos planos (nada de estrellas/emoji) — tocar uno
     * rellena hasta ahí, como un indicador de nivel de batería. Devuelve un
     * array de 1 elemento como "contenedor" mutable del valor actual (1-5),
     * legible desde el listener del diálogo.
     */
    private int[] configurarSelectorNivel(ViewGroup fila, int inicial) {
        int[] valor = {inicial};
        int total = fila.getChildCount();
        Runnable pintar = () -> {
            for (int idx = 0; idx < total; idx++) {
                fila.getChildAt(idx).setBackgroundResource(
                        idx < valor[0] ? R.drawable.bg_nivel_activo : R.drawable.button_secondary);
            }
        };
        for (int idx = 0; idx < total; idx++) {
            final int nivel = idx + 1;
            fila.getChildAt(idx).setOnClickListener(v -> {
                feedback.vibrateLight();
                valor[0] = nivel;
                pintar.run();
            });
        }
        pintar.run();
        return valor;
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
        btnTest = findViewById(R.id.btnTest);
        btnPreviewFase = findViewById(R.id.btnPreviewFase);
        btnPreviewEntreno = findViewById(R.id.btnPreviewEntreno);

        bannerAusencia = findViewById(R.id.bannerAusencia);
        tvAusenciaMensaje = findViewById(R.id.tvAusenciaMensaje);
        bannerRamadan = findViewById(R.id.bannerRamadan);
        tvRamadanTitulo = findViewById(R.id.tvRamadanTitulo);
        tvRamadanDetalle = findViewById(R.id.tvRamadanDetalle);

        // Navegación (el listener de btnEmpezarEntreno se fija en
        // actualizarVistaMañana — cambia según si ya hay sesión completada hoy)
        btnPlanAnual.setOnClickListener(v -> startActivity(new Intent(this, PlanAnualActivity.class)));
        btnProgresion.setOnClickListener(v -> startActivity(new Intent(this, ProgressionActivity.class)));
        btnTest.setOnClickListener(v -> startActivity(new Intent(this, TestRunnerActivity.class)));
        btnPreviewFase.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.fitbase.ui.plan.CambioFaseActivity.class);
            intent.putExtra(com.fitbase.ui.plan.CambioFaseActivity.EXTRA_DEMO, true);
            startActivity(intent);
        });
        // No pasa por el gate de tipo_dia: WorkoutActivity ya cae sola en su
        // sesión demo si sesion_hoy no devuelve nada (pre-temporada, día de
        // descanso/natación real, o backend aún no redesplegado) — así se
        // puede probar el flujo de gym en cualquier momento. Si el entreno
        // demo de HOY ya se completó, la segunda vez lleva al resumen en vez
        // de dejar empezar otro (mismo criterio que una sesión real completada).
        btnPreviewEntreno.setOnClickListener(v -> abrirEntrenoODemo());
    }

    private void abrirEntrenoODemo() {
        SharedPreferences prefs = getSharedPreferences("fitbase_demo", MODE_PRIVATE);
        String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        if (hoy.equals(prefs.getString("fecha_completada", null))) {
            Intent intent = new Intent(this, SummaryActivity.class);
            intent.putExtra("series", prefs.getInt("series_totales", 0));
            intent.putExtra("volumen", prefs.getInt("volumen_total_kg", 0));
            intent.putExtra("rir_medio", prefs.getFloat("rir_medio", 0f));
            intent.putExtra("intensidad", prefs.getString("intensidad_percibida", null));
            intent.putExtra("impacto", prefs.getString("impacto", null));
            startActivity(intent);
        } else {
            startActivity(new Intent(this, com.fitbase.ui.workout.WorkoutActivity.class));
        }
    }

    private void observarDatos() {
        // Header
        tvSaludo.setText(viewModel.getSaludoHora());
        tvFecha.setText(viewModel.getFechaFormateada());

        // Vista mañana (endpoint único con todo)
        viewModel.getVistaMañana().observe(this, this::actualizarVistaMañana);

        viewModel.getMetricasSubjetivasGuardadas().observe(this, ok -> {
            if (Boolean.TRUE.equals(ok)) {
                Toast.makeText(this, "Guardado — gracias", Toast.LENGTH_SHORT).show();
            } else if (Boolean.FALSE.equals(ok)) {
                Toast.makeText(this, "No se pudo guardar (sin conexión)", Toast.LENGTH_SHORT).show();
            }
        });

        // Macros consumidas (Health Connect / FatSecret) → calcular restantes
        viewModel.getCaloriasConsumidas().observe(this, c -> recalcularMacrosRestantes());
        viewModel.getProteinaConsumida().observe(this, p -> recalcularMacrosRestantes());
        viewModel.getCarbosConsumidos().observe(this, c -> recalcularMacrosRestantes());
        viewModel.getGrasasConsumidas().observe(this, g -> recalcularMacrosRestantes());
        viewModel.getPasosActuales().observe(this, p -> actualizarPasos());

        // Datos específicos de Health Connect:
        // - FC reposo: RestingHeartRateRecord, literal (el mismo valor que calcula el reloj).
        // - Sleep score: ESTIMADO a partir de datos crudos (duración/fases) — Health Connect
        //   no tiene el score real de Zepp. Se marca "≈" en la UI. Tiene prioridad sobre el
        //   valor manual del backend (ver actualizarVistaMañana) — si HC no tiene datos ese
        //   día, se usa el manual como respaldo.
        viewModel.getHcFcReposo().observe(this, fc -> {
            if (fc != null) tvFcReposo.setText(fc + " bpm");
        });
        viewModel.getHcSleepScore().observe(this, score -> {
            if (score != null) {
                tvSleepScore.setText("≈" + score + "/100");
                pintarColorSueno(score);
                if (score < 60) {
                    tvAvisoFatiga.setText("Sueño pobre — sesión reducida");
                    tvAvisoFatiga.setVisibility(View.VISIBLE);
                } else {
                    tvAvisoFatiga.setVisibility(View.GONE);
                }
            }
        });

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
            case "gym": tipoDiaTexto = "DÍA DE GYM"; break;
            case "natacion": tipoDiaTexto = "NATACIÓN"; break;
            default: tipoDiaTexto = "DESCANSO"; break;
        }
        tvTipoDia.setText(tipoDiaTexto);

        // ── Fase ──
        if (vista.getFase() != null) {
            tvFaseNombre.setText(vista.getFase().nombre);
            tvFaseNombre.setVisibility(View.VISIBLE);
            comprobarCambioFase(vista.getFase().faseId);
        }

        // ── Sueño ──
        // Prioridad: score ESTIMADO de Health Connect (ver observer de getHcSleepScore).
        // Si HC no tiene datos de sueño ese día, usamos el valor manual del backend
        // (metricas_zepp.num_sleep_score) como respaldo, mostrado sin "≈" porque es literal.
        if (vista.getSueno() != null) {
            if (viewModel.getHcSleepScore().getValue() == null) {
                Integer score = vista.getSueno().sleepScore;
                tvSleepScore.setText(score != null ? score + "/100" : "—");
                if (score != null) pintarColorSueno(score);
                if (score != null && score < 60) {
                    tvAvisoFatiga.setText("Sueño pobre — sesión reducida");
                    tvAvisoFatiga.setVisibility(View.VISIBLE);
                } else {
                    tvAvisoFatiga.setVisibility(View.GONE);
                }
            }

            if (viewModel.getHcFcReposo().getValue() == null) {
                Integer fc = vista.getSueno().hrReposo;
                tvFcReposo.setText(fc != null ? fc + " bpm" : "—");
            }
        }

        // ── Macros objetivo (cambian por fase) ──
        recalcularMacrosRestantes();

        // ── Cardio y pasos ──
        if (vista.getCardio() != null) {
            if (vista.getCardio().pasosObjetivo > 0) {
                int pasosObj = vista.getCardio().pasosObjetivo;
                tvPasosObj.setText("/ " + pasosObj + " pasos");
                progressPasos.setMax(pasosObj);
            } else {
                tvPasosObj.setText("/ — pasos");
            }

            if (vista.getCardio().cardioPostGymMin > 0) {
                String cuando = "dia_descanso".equals(vista.getCardio().contexto) ? "hoy" : "post-gym";
                tvCardioInfo.setText(vista.getCardio().cardioPostGymMin + " min " +
                        (vista.getCardio().modalidad != null ? vista.getCardio().modalidad : "bici") +
                        " " + cuando);
                tvCardioInfo.setVisibility(View.VISIBLE);
            } else {
                tvCardioInfo.setVisibility(View.GONE);
            }
        }
        actualizarPasos();

        // ── Movilidad matutina ──
        if (vista.getMovilidadMatutina() != null) {
            tvMovilidadTitulo.setText("Movilidad matutina (" +
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
        // Si ya completaste el entreno de hoy, no se puede empezar otro —
        // solo revisar el resumen (evita duplicar series en ejercicios_log
        // y que el motor recalcule mal la próxima sesión).
        if (vista.esGym() && !vista.isSesionCompletada()) {
            btnEmpezarEntreno.setText("EMPEZAR ENTRENO");
            btnEmpezarEntreno.setOnClickListener(v -> {
                feedback.vibrateLight();
                startActivity(new Intent(this, WorkoutActivity.class));
            });
            btnEmpezarEntreno.setVisibility(View.VISIBLE);
        } else if (vista.esGym() && vista.isSesionCompletada()) {
            btnEmpezarEntreno.setText("VER RESUMEN DEL ENTRENAMIENTO");
            btnEmpezarEntreno.setOnClickListener(v -> {
                feedback.vibrateLight();
                Intent intent = new Intent(this, SummaryActivity.class);
                com.fitbase.data.model.ResumenSesionResponse.Resumen r = vista.getResumenHoy();
                if (r != null) {
                    intent.putExtra("series", r.seriesTotales);
                    intent.putExtra("volumen", r.volumenTotalKg);
                    intent.putExtra("rir_medio", r.rirMedio);
                    intent.putExtra("intensidad", r.intensidadPercibida);
                    intent.putExtra("impacto", r.impacto);
                }
                startActivity(intent);
            });
            btnEmpezarEntreno.setVisibility(View.VISIBLE);
        } else {
            btnEmpezarEntreno.setVisibility(View.GONE);
        }

        // ── Agua ──
        if (vista.getMacros() != null) {
            tvAgua.setText(String.format("%,.0f L", vista.getMacros().aguaMl / 1000f));
        }

        // ── Aviso ausencia ──
        if (vista.getAvisoAusencia() != null) {
            tvAusenciaMensaje.setText(vista.getAvisoAusencia().mensaje);
            bannerAusencia.setVisibility(View.VISIBLE);
        } else {
            bannerAusencia.setVisibility(View.GONE);
        }

        // ── Ramadán / Eid (cultura.md §5-6) ──
        VistaMañanaResponse.Ramadan ramadan = vista.getRamadan();
        if (ramadan != null && ramadan.esEid) {
            tvRamadanTitulo.setText("Eid al-Fitr");
            tvRamadanDetalle.setText(ramadan.nota);
            bannerRamadan.setVisibility(View.VISIBLE);
        } else if (ramadan != null && ramadan.activo) {
            tvRamadanTitulo.setText("Ramadán · Día " + ramadan.diaAyuno);
            tvRamadanDetalle.setText(
                    ramadan.horarioAproximado + "\n\n" +
                    "Entreno: " + ramadan.timingEntreno + "\n\n" +
                    "Hidratación: " + ramadan.hidratacion + "\n\n" +
                    "Nutrición: " + ramadan.nutricion);
            bannerRamadan.setVisibility(View.VISIBLE);
        } else {
            bannerRamadan.setVisibility(View.GONE);
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

    /**
     * Blanco por defecto — solo se destaca en color de alerta cuando el sueño
     * fue muy escaso, para notarlo de un vistazo sin tener que leer el número.
     */
    private void pintarColorSueno(int score) {
        tvSleepScore.setTextColor(getColor(score < 50 ? R.color.error : R.color.text_primary));
    }

    private void actualizarPasos() {
        Integer pasos = viewModel.getPasosActuales().getValue();
        int p = pasos != null ? pasos : 0;
        tvPasos.setText(String.format("%,d", p));
        progressPasos.setProgress(p);
    }
}
