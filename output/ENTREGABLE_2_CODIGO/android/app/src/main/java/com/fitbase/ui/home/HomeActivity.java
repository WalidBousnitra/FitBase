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
import androidx.lifecycle.ViewModelProvider;

import com.fitbase.R;
import com.fitbase.data.health.HealthConnectBridge;
import com.fitbase.data.model.VistaMañanaResponse;
import com.fitbase.ui.BaseActivity;
import com.fitbase.ui.plan.PlanAnualActivity;
import com.fitbase.ui.progression.ProgressionActivity;
import com.fitbase.ui.summary.SummaryActivity;
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
public class HomeActivity extends BaseActivity {

    private HomeViewModel viewModel;
    private FeedbackHelper feedback;

    @SuppressWarnings("unchecked")
    private final ActivityResultLauncher<Set<String>> hcPermLauncher =
            registerForActivityResult(HealthConnectBridge.getPermissionContract(),
                    granted -> { if (viewModel != null) viewModel.cargarDatosDelDia(); });

    // ─── Vistas: Header ───
    private TextView tvSaludo, tvFecha, tvTipoDia, tvTipoSesion, tvFaseNombre, tvResumenEntreno, tvPausarPlan;

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
    // Siempre visible al completo, no es desplegable (relevante todos los días).
    private LinearLayout layoutMovilidad;
    private TextView tvMovilidadTitulo;

    // Core del día de descanso (recuperación activa) — colapsable, misma lógica
    // que movilidad. La tarjeta entera se oculta si el día no trae core.
    private View cardCore, headerCore;
    private LinearLayout layoutCore;
    private TextView tvCoreTitulo, tvCoreChevron;
    private boolean coreExpandido = false;

    // ─── Vistas: Sesión ───
    private TextView tvSesionTipo, tvSesionFase;
    private Button btnEmpezarEntreno;

    // ─── Vistas: Navegación ───
    private View btnPlanAnual, btnProgresion;

    // ─── Vistas: botones de desarrollo (Constants.MOSTRAR_BOTONES_DEMO) ───
    private View btnTest, btnPreviewFase, btnPreviewEntreno, btnPreviewRamadan;

    // ─── Vistas: Banners ───
    private View bannerAusencia;
    private TextView tvAusenciaMensaje;
    private View cardManana;
    private TextView tvMananaResumen;
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
        // Si venimos DIRECTOS de SplashActivity (precarga terminada hace
        // instantes), los datos ya están frescos — repetir aquí la misma
        // recarga completa (red + Health Connect) solo duplica el trabajo que
        // Splash acaba de hacer y compite por el mismo lector de Health
        // Connect, que es precisamente lo que hacía tardar varios segundos
        // más en aparecer algo en pantalla justo después de "cargar". En
        // cualquier onResume posterior (volver de otra pantalla, reabrir tras
        // un rato) sí se recarga: msDesdeCarga ya será alto.
        // sinDatos cubre el caso en que SplashActivity dio por completada la
        // precarga por timeout (8s) sin que vista_manana hubiera respondido
        // todavía (Apps Script puede tardar más en frío) — marcarCargaCompleta()
        // se llama igualmente, así que el timestamp de abajo mentía "recién
        // cargado" y esto se quedaba sin pintar nada hasta reabrir la app 2-3
        // veces (la única forma de que msDesdeCarga superase los 2000ms).
        boolean sinDatos = viewModel.getVistaMañana().getValue() == null;
        long msDesdeCarga = System.currentTimeMillis() - com.fitbase.data.cache.AppDataCache.getUltimaCargaCompletaMs();
        if (sinDatos || !com.fitbase.data.cache.AppDataCache.isCargaInicialCompleta() || msDesdeCarga > 2000) {
            viewModel.cargarDatosDelDia();
        }
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
        tvTipoSesion = findViewById(R.id.tvTipoSesion);
        tvFaseNombre = findViewById(R.id.tvFaseNombre);
        tvResumenEntreno = findViewById(R.id.tvResumenEntreno);
        tvPausarPlan = findViewById(R.id.tvPausarPlan);

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

        cardCore = findViewById(R.id.cardCore);
        headerCore = findViewById(R.id.headerCore);
        layoutCore = findViewById(R.id.layoutCore);
        tvCoreTitulo = findViewById(R.id.tvCoreTitulo);
        tvCoreChevron = findViewById(R.id.tvCoreChevron);
        headerCore.setOnClickListener(v -> {
            feedback.vibrateLight();
            coreExpandido = !coreExpandido;
            layoutCore.setVisibility(coreExpandido ? View.VISIBLE : View.GONE);
            tvCoreChevron.setText(coreExpandido ? "▾" : "▸");
        });

        tvSesionTipo = findViewById(R.id.tvSesionTipo);
        tvSesionFase = findViewById(R.id.tvSesionFase);
        btnEmpezarEntreno = findViewById(R.id.btnEmpezarEntreno);
        btnPlanAnual = findViewById(R.id.btnPlanAnual);
        btnProgresion = findViewById(R.id.btnProgresion);

        bannerAusencia = findViewById(R.id.bannerAusencia);
        tvAusenciaMensaje = findViewById(R.id.tvAusenciaMensaje);
        cardManana = findViewById(R.id.cardManana);
        tvMananaResumen = findViewById(R.id.tvMananaResumen);
        bannerRamadan = findViewById(R.id.bannerRamadan);
        tvRamadanTitulo = findViewById(R.id.tvRamadanTitulo);
        tvRamadanDetalle = findViewById(R.id.tvRamadanDetalle);

        // Navegación (el listener de btnEmpezarEntreno se fija en
        // actualizarVistaMañana — cambia según si ya hay sesión completada hoy)
        btnPlanAnual.setOnClickListener(v -> startActivity(new Intent(this, PlanAnualActivity.class)));
        btnProgresion.setOnClickListener(v -> startActivity(new Intent(this, ProgressionActivity.class)));

        tvPausarPlan.setOnClickListener(v -> mostrarDialogoPausarPlan());

        vincularBotonesDemo();
    }

    /**
     * "Pausar plan (vacaciones)" — excepciones.md §2.2. Pide fecha_inicio/fin,
     * llama a registrar_ausencia (suspende sesiones futuras en ese rango sin
     * tocar histórico real) y muestra impacto + rutina de mantenimiento sin
     * equipamiento (entrenamiento_casa.md).
     */
    private void mostrarDialogoPausarPlan() {
        java.text.SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar calInicio = Calendar.getInstance();
        Calendar calFin = Calendar.getInstance();
        calFin.add(Calendar.DAY_OF_MONTH, 7); // sugerencia por defecto: 1 semana

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        TextView tvExplicacion = new TextView(this);
        tvExplicacion.setText("Las sesiones futuras en ese rango se suspenden (no cuentan como perdidas). Al volver, el motor ajusta el peso automáticamente.");
        tvExplicacion.setPadding(0, 0, 0, pad);
        layout.addView(tvExplicacion);

        Button btnInicio = new Button(this);
        Button btnFin = new Button(this);
        btnInicio.setText("Desde: " + fmt.format(calInicio.getTime()));
        btnFin.setText("Hasta: " + fmt.format(calFin.getTime()));
        layout.addView(btnInicio);
        layout.addView(btnFin);

        btnInicio.setOnClickListener(v -> new android.app.DatePickerDialog(this, (view, y, m, d) -> {
            calInicio.set(y, m, d);
            btnInicio.setText("Desde: " + fmt.format(calInicio.getTime()));
        }, calInicio.get(Calendar.YEAR), calInicio.get(Calendar.MONTH), calInicio.get(Calendar.DAY_OF_MONTH)).show());

        btnFin.setOnClickListener(v -> new android.app.DatePickerDialog(this, (view, y, m, d) -> {
            calFin.set(y, m, d);
            btnFin.setText("Hasta: " + fmt.format(calFin.getTime()));
        }, calFin.get(Calendar.YEAR), calFin.get(Calendar.MONTH), calFin.get(Calendar.DAY_OF_MONTH)).show());

        new AlertDialog.Builder(this)
                .setTitle("Pausar plan (vacaciones)")
                .setView(layout)
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    if (calFin.getTimeInMillis() <= calInicio.getTimeInMillis()) {
                        Toast.makeText(this, "La fecha de fin debe ser posterior a la de inicio", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    registrarAusenciaBackend(fmt.format(calInicio.getTime()), fmt.format(calFin.getTime()));
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void registrarAusenciaBackend(String fechaInicio, String fechaFin) {
        java.util.Map<String, Object> datos = new java.util.HashMap<>();
        datos.put("accion", "registrar_ausencia");
        datos.put("fecha_inicio", fechaInicio);
        datos.put("fecha_fin", fechaFin);

        com.fitbase.data.api.ApiClient.getApi().registrarAusencia(datos).enqueue(
                new retrofit2.Callback<com.fitbase.data.model.AusenciaRegistroResponse>() {
            @Override
            public void onResponse(retrofit2.Call<com.fitbase.data.model.AusenciaRegistroResponse> call,
                                    retrofit2.Response<com.fitbase.data.model.AusenciaRegistroResponse> response) {
                com.fitbase.data.model.AusenciaRegistroResponse body = response.body();
                if (response.isSuccessful() && body != null && body.ok) {
                    mostrarResultadoAusencia(body);
                    if (viewModel != null) viewModel.cargarDatosDelDia();
                } else if (body != null && body.error != null) {
                    Toast.makeText(HomeActivity.this, body.error, Toast.LENGTH_LONG).show();
                } else {
                    com.fitbase.data.local.SyncManager.encolar(HomeActivity.this, datos);
                    Toast.makeText(HomeActivity.this, "Sin conexión — se aplicará en cuanto la haya.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.fitbase.data.model.AusenciaRegistroResponse> call, Throwable t) {
                com.fitbase.data.local.SyncManager.encolar(HomeActivity.this, datos);
                Toast.makeText(HomeActivity.this, "Sin conexión — se aplicará en cuanto la haya.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void mostrarResultadoAusencia(com.fitbase.data.model.AusenciaRegistroResponse body) {
        StringBuilder sb = new StringBuilder();
        sb.append(body.sesionesSuspendidas).append(" sesiones suspendidas.\n\n").append(body.impacto);
        if (body.rutinaCasa != null && body.rutinaCasa.ejercicios != null) {
            sb.append("\n\nRutina en casa (opcional, sin equipamiento, ~")
              .append(body.rutinaCasa.duracionMin).append(" min):\n");
            for (com.fitbase.data.model.AusenciaRegistroResponse.EjercicioCasa ej : body.rutinaCasa.ejercicios) {
                sb.append("• ").append(ej.nombre).append(" — ").append(ej.reps).append("\n");
            }
        }
        new AlertDialog.Builder(this)
                .setTitle("Plan pausado")
                .setMessage(sb.toString())
                .setPositiveButton("Entendido", null)
                .show();
    }

    /**
     * Botones de desarrollo (TEST/FASE/ENTRENO/RAMADÁN), todos ocultos de una
     * sola vez si Constants.MOSTRAR_BOTONES_DEMO es false — único punto de
     * control para desactivarlos antes de un release.
     */
    private void vincularBotonesDemo() {
        btnTest = findViewById(R.id.btnTest);
        btnPreviewFase = findViewById(R.id.btnPreviewFase);
        btnPreviewEntreno = findViewById(R.id.btnPreviewEntreno);
        btnPreviewRamadan = findViewById(R.id.btnPreviewRamadan);

        if (!com.fitbase.util.Constants.MOSTRAR_BOTONES_DEMO) {
            btnTest.setVisibility(View.GONE);
            btnPreviewFase.setVisibility(View.GONE);
            btnPreviewEntreno.setVisibility(View.GONE);
            btnPreviewRamadan.setVisibility(View.GONE);
            return;
        }

        btnTest.setOnClickListener(v ->
                startActivity(new Intent(this, com.fitbase.ui.test.TestRunnerActivity.class)));

        btnPreviewFase.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.fitbase.ui.plan.CambioFaseActivity.class);
            intent.putExtra(com.fitbase.ui.plan.CambioFaseActivity.EXTRA_DEMO, true);
            startActivity(intent);
        });

        // Abre el flujo de gym directamente, sin pasar por el gate de tipo_dia
        // (btnEmpezarEntreno solo aparece si vista.esGym()) — así se puede
        // probar en descanso/natación o antes del 31/08 sin esperar. La ruta
        // antigua comprobaba una sesión "demo" completada en SharedPreferences
        // para saltar a SummaryActivity, pero esa ruta quedó obsoleta: hoy la
        // finalización de sesión vive en el backend (completar_sesion +
        // vista_manana.resumen_hoy), no en prefs locales — restaurar esa rama
        // sería código muerto que nunca se dispara.
        btnPreviewEntreno.setOnClickListener(v ->
                startActivity(new Intent(this, WorkoutActivity.class)));

        btnPreviewRamadan.setOnClickListener(v -> {
            com.fitbase.data.api.ApiClient.getApi().getRamadanPreview("preview_ramadan")
                    .enqueue(new retrofit2.Callback<com.fitbase.data.model.RamadanPreviewResponse>() {
                        @Override
                        public void onResponse(retrofit2.Call<com.fitbase.data.model.RamadanPreviewResponse> call,
                                                retrofit2.Response<com.fitbase.data.model.RamadanPreviewResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                mostrarRamadan(response.body().getRamadan());
                            } else {
                                Toast.makeText(HomeActivity.this, "Sin respuesta del backend", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(retrofit2.Call<com.fitbase.data.model.RamadanPreviewResponse> call, Throwable t) {
                            Toast.makeText(HomeActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
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
        //   no tiene el score real de Zepp. Tiene prioridad sobre el valor manual del backend
        //   (ver actualizarVistaMañana) — si HC no tiene datos ese día, se usa el manual como
        //   respaldo.
        viewModel.getHcFcReposo().observe(this, fc -> {
            if (fc != null) tvFcReposo.setText(fc + " bpm");
        });
        viewModel.getHcSleepScore().observe(this, score -> {
            if (score != null) {
                tvSleepScore.setText(String.valueOf(score));
                pintarColorSueno(score);
                // Umbral alineado con calcularAjusteDia_ (Codigo.gs, 2026-d): antes
                // <60 mostraba este aviso para casi cualquier noche imperfecta, aunque
                // el efecto real en carga fuera mínimo. Ahora solo se muestra cuando el
                // backend realmente aplica el ajuste (<30).
                if (score < 30) {
                    tvAvisoFatiga.setText("Sueño pobre — carga ligeramente reducida");
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
        // El tipo de día real (gym/natación/descanso) se muestra siempre, aunque
        // estemos en pretemporada — el horario semanal ya es el real (mismo que
        // regirá cuando arranque el plan) y los botones de demo cubren la
        // previsualización manual, así que el aviso de pretemporada ya no aporta
        // nada aparte de ruido.
        String tipoDiaTexto;
        switch (vista.getTipoDia()) {
            case "gym": tipoDiaTexto = "DÍA DE GYM"; break;
            case "natacion": tipoDiaTexto = "NATACIÓN"; break;
            default: tipoDiaTexto = "DESCANSO"; break;
        }
        tvTipoDia.setText(tipoDiaTexto);

        // ── Split de hoy (Push/Pierna/Pull/Hombros+Brazos) ──
        // Solo en días de gym — para saber qué mochila preparar por la mañana
        // sin tener que entrar al flujo de entreno.
        if (vista.esGym() && vista.getTipoSesion() != null) {
            tvTipoSesion.setText(vista.getTipoSesion());
            tvTipoSesion.setVisibility(View.VISIBLE);
        } else {
            tvTipoSesion.setVisibility(View.GONE);
        }

        // ── Resumen rápido del entreno de hoy (nº ejercicios / duración / cardio) ──
        // Para decidir de un vistazo, antes de salir de casa, si hay que darse
        // prisa — sin tener que abrir el flujo completo de gym.
        if (vista.esGym() && vista.getResumenEntreno() != null) {
            VistaMañanaResponse.ResumenEntreno r = vista.getResumenEntreno();
            StringBuilder sb = new StringBuilder();
            sb.append(r.numEjercicios).append(r.numEjercicios == 1 ? " ejercicio" : " ejercicios");
            sb.append(" · ~").append(r.duracionEstMin).append(" min");
            if (r.cardioExtraMin > 0) sb.append(" · +").append(r.cardioExtraMin).append(" cardio");
            tvResumenEntreno.setText(sb.toString());
            tvResumenEntreno.setVisibility(View.VISIBLE);
        } else {
            tvResumenEntreno.setVisibility(View.GONE);
        }

        // ── Vistazo de mañana (qué mochila preparar la noche anterior) ──
        if (vista.getManana() != null) {
            VistaMañanaResponse.PreviewManana m = vista.getManana();
            String texto;
            switch (m.tipoDia) {
                case "gym":
                    StringBuilder sb = new StringBuilder(m.tipoSesion != null ? m.tipoSesion : "Gym");
                    if (m.numEjercicios != null) {
                        sb.append(" · ").append(m.numEjercicios).append(m.numEjercicios == 1 ? " ejercicio" : " ejercicios");
                    }
                    if (m.duracionEstMin != null) sb.append(" · ~").append(m.duracionEstMin).append(" min");
                    if (m.cardioExtraMin > 0) sb.append(" · +").append(m.cardioExtraMin).append(" cardio");
                    texto = sb.toString();
                    break;
                case "natacion": texto = "Natación"; break;
                default: texto = "Descanso — no hace falta preparar nada"; break;
            }
            tvMananaResumen.setText(texto);
            cardManana.setVisibility(View.VISIBLE);
        } else {
            cardManana.setVisibility(View.GONE);
        }

        // ── Fase ──
        if (vista.getFase() != null) {
            tvFaseNombre.setText(vista.getFase().nombre);
            tvFaseNombre.setVisibility(View.VISIBLE);
            comprobarCambioFase(vista.getFase().faseId);
        }

        // ── Sueño ──
        // Prioridad: score ESTIMADO de Health Connect (ver observer de getHcSleepScore).
        // Si HC no tiene datos de sueño ese día, usamos el valor manual del backend
        // (metricas_zepp.num_sleep_score) como respaldo.
        if (vista.getSueno() != null) {
            if (viewModel.getHcSleepScore().getValue() == null) {
                Integer score = vista.getSueno().sleepScore;
                tvSleepScore.setText(score != null ? String.valueOf(score) : "—");
                if (score != null) pintarColorSueno(score);
                if (score != null && score < 30) {
                    tvAvisoFatiga.setText("Sueño pobre — carga ligeramente reducida");
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
                tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
                tv.setPadding(0, 2, 0, 2);
                layoutMovilidad.addView(tv);
            }
            layoutMovilidad.setVisibility(View.VISIBLE);
        }

        // ── Core del día de descanso (recuperación activa) ──
        // Solo llega en días de descanso (hipertrofia.md §3: sube la frecuencia
        // de core a 2x/sem). En gym/natación core_dia es null → tarjeta oculta.
        VistaMañanaResponse.CoreDia core = vista.getCoreDia();
        if (core != null && core.ejercicios != null && !core.ejercicios.isEmpty()) {
            tvCoreTitulo.setText((core.titulo != null ? core.titulo : "Core")
                    + " (" + core.duracionMin + " min)");
            layoutCore.removeAllViews();
            for (VistaMañanaResponse.EjercicioMovilidad ej : core.ejercicios) {
                TextView tv = new TextView(this);
                tv.setText("• " + ej.nombre + " — " + ej.reps);
                tv.setTextColor(getColor(R.color.text_secondary));
                tv.setPadding(0, 4, 0, 4);
                layoutCore.addView(tv);
            }
            layoutCore.setVisibility(coreExpandido ? View.VISIBLE : View.GONE);
            cardCore.setVisibility(View.VISIBLE);
        } else {
            cardCore.setVisibility(View.GONE);
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
        mostrarRamadan(vista.getRamadan());
    }

    /**
     * Pinta el banner de Ramadán/Eid. Reutilizada tanto por el flujo real
     * (vista_manana, arriba) como por el botón de demo RAMADÁN (llama a
     * preview_ramadan) — mismo código de render para los dos casos.
     */
    private void mostrarRamadan(VistaMañanaResponse.Ramadan ramadan) {
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
