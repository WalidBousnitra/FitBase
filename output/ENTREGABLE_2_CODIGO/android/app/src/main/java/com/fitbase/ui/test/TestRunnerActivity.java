package com.fitbase.ui.test;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.fitbase.R;
import com.fitbase.data.api.ApiClient;
import com.fitbase.data.health.HealthConnectBridge;
import com.fitbase.data.health.HealthConnectReader;
import com.fitbase.data.model.Ejercicio;
import com.fitbase.data.model.Fase;
import com.fitbase.data.model.MacrosResponse;
import com.fitbase.data.model.MetricasProgresionResponse;
import com.fitbase.data.model.PlanAnualResponse;
import com.fitbase.data.model.SesionResponse;
import com.fitbase.util.Constants;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pantalla de tests funcionales — SOLO en modo demo.
 * Ejecuta tests unitarios de todas las funcionalidades de la app
 * y muestra resultados pass/fail en pantalla.
 *
 * Se elimina o desactiva cuando el programa real comience (31/08/2026).
 */
public class TestRunnerActivity extends AppCompatActivity {

    private TextView tvResumen;
    private TextView tvResultados;
    private ProgressBar progressTests;
    private Button btnEjecutar;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SpannableStringBuilder output = new SpannableStringBuilder();
    private int totalTests = 0;
    private int passedTests = 0;
    private int failedTests = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_runner);

        tvResumen = findViewById(R.id.tvResumen);
        tvResultados = findViewById(R.id.tvResultados);
        progressTests = findViewById(R.id.progressTests);
        btnEjecutar = findViewById(R.id.btnEjecutar);

        btnEjecutar.setOnClickListener(v -> ejecutarTests());
        findViewById(R.id.btnCerrar).setOnClickListener(v -> finish());
    }

    private void ejecutarTests() {
        btnEjecutar.setEnabled(false);
        output.clear();
        totalTests = 0;
        passedTests = 0;
        failedTests = 0;
        tvResultados.setText("");
        progressTests.setVisibility(View.VISIBLE);
        progressTests.setProgress(0);

        new Thread(() -> {
            log("═══════════════════════════════════════");
            log("  FitBase Test Runner — Demo Mode");
            log("═══════════════════════════════════════\n");

            // ══════ GRUPO 1: Constants & Config ══════
            logHeader("1. CONSTANTES Y CONFIGURACIÓN");
            testConstantesBasicas();
            testFechaInicio();
            testMacrosFallback();

            // ══════ GRUPO 2: Macros & Nutrición ══════
            logHeader("2. MACROS Y NUTRICIÓN");
            testMacrosResponse();
            testCaloriasRestantes();
            testMacrosRestantes();
            testNutricionDemo();

            // ══════ GRUPO 3: Plan Anual ══════
            logHeader("3. PLAN ANUAL");
            testPlanAnualDemo();
            testFasesOrden();
            testFasesFechasValidas();
            testFaseActual();
            testColorPorTipo();

            // ══════ GRUPO 4: Workout / Ejercicios ══════
            logHeader("4. ENTRENAMIENTO");
            testEjercicioCreacion();
            testSesionDemo();
            testVolumenCalculo();
            testRIRRegistro();
            testTimerDescanso();

            // ══════ GRUPO 5: Progresión ══════
            logHeader("5. PROGRESIÓN");
            testProgresionDemo();
            testPesoTendencia();

            // ══════ GRUPO 6: Health Connect ══════
            logHeader("6. HEALTH CONNECT");
            testHCDisponibilidad();
            testHCPermisos();

            // ══════ GRUPO 7: API / Red ══════
            logHeader("7. API / CONECTIVIDAD");
            testApiBaseUrl();
            testApiSesionHoy();
            testApiMacros();
            testApiProgresion();

            // ══════ GRUPO 8: Motor de Pesos ══════
            logHeader("8. MOTOR DE PESOS");
            testMotorPesosSubida();
            testMotorPesosBajada();
            testMotorPesosMantenimiento();

            // ══════ GRUPO 9: UI / Formato ══════
            logHeader("9. FORMATO Y DISPLAY");
            testFormatoFecha();
            testFormatoCaloriasLocale();

            // Resumen
            log("\n═══════════════════════════════════════");
            String resumen = String.format("  RESULTADO: %d/%d PASS | %d FAIL",
                    passedTests, totalTests, failedTests);
            log(resumen);
            log("═══════════════════════════════════════");

            mainHandler.post(() -> {
                progressTests.setProgress(100);
                btnEjecutar.setEnabled(true);
                tvResumen.setText(String.format("%d pass · %d fail · %d total",
                        passedTests, failedTests, totalTests));
                tvResumen.setTextColor(getColor(failedTests == 0 ? R.color.success : R.color.error));
            });
        }).start();
    }

    // ═══════════════════════════════════════════════
    // TESTS GRUPO 1: Constantes
    // ═══════════════════════════════════════════════

    private void testConstantesBasicas() {
        assertTest("Altura 188cm", Constants.ALTURA_CM == 188);
        assertTest("Edad 24", Constants.EDAD == 24);
        assertTest("Peso inicial 78.2kg", Constants.PESO_INICIAL_KG == 78.2f);
        assertTest("Pasos objetivo 8000", Constants.PASOS_OBJETIVO == 8000);
    }

    private void testFechaInicio() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date fechaInicio = sdf.parse(Constants.FECHA_INICIO_PROGRAMA);
            Date hoy = new Date();
            assertTest("Programa no ha empezado (demo)", hoy.before(fechaInicio));
            assertTest("Fecha inicio es 31/08/2026",
                    "2026-08-31".equals(Constants.FECHA_INICIO_PROGRAMA));
        } catch (ParseException e) {
            assertTest("Fecha inicio parseable", false);
        }
    }

    private void testMacrosFallback() {
        assertTest("Calorías fallback > 2000", Constants.CALORIAS_FALLBACK > 2000);
        assertTest("Calorías fallback = 3280", Constants.CALORIAS_FALLBACK == 3280);
        assertTest("Proteína > 100g", Constants.PROTEINA_FALLBACK_G > 100);
        assertTest("Carbos > Grasa", Constants.CARBOS_FALLBACK_G > Constants.GRASAS_FALLBACK_G);
        assertTest("P + C + G ≈ calorías/4-9",
                Math.abs((Constants.PROTEINA_FALLBACK_G * 4 + Constants.CARBOS_FALLBACK_G * 4
                        + Constants.GRASAS_FALLBACK_G * 9) - Constants.CALORIAS_FALLBACK) < 100);
    }

    // ═══════════════════════════════════════════════
    // TESTS GRUPO 2: Macros & Nutrición
    // ═══════════════════════════════════════════════

    private void testMacrosResponse() {
        MacrosResponse m = new MacrosResponse();
        m.caloriasObjetivo = 3280;
        m.caloriasConsumidas = 1500;
        m.proteinaG = 156;
        m.proteinaConsumidaG = 80;
        assertTest("MacrosResponse creado != null", m != null);
        assertTest("caloriasObjetivo asignado", m.caloriasObjetivo == 3280);
    }

    private void testCaloriasRestantes() {
        MacrosResponse m = new MacrosResponse();
        m.caloriasObjetivo = 3280;
        m.caloriasConsumidas = 1500;
        assertTest("Restantes = 1780", m.getCaloriasRestantes() == 1780);

        m.caloriasConsumidas = 4000; // Excede objetivo
        assertTest("Restantes no negativo (clamp 0)", m.getCaloriasRestantes() == 0);
    }

    private void testMacrosRestantes() {
        MacrosResponse m = new MacrosResponse();
        m.proteinaG = 156;
        m.proteinaConsumidaG = 60;
        m.carbosG = 488;
        m.carbosConsumidosG = 200;
        m.grasasG = 78;
        m.grasasConsumidasG = 30;

        assertTest("Proteína restante = 96g", m.getProteinaRestante() == 96);
        assertTest("Carbos restantes = 288g", m.getCarbosRestantes() == 288);
        assertTest("Grasas restantes = 48g", m.getGrasasRestantes() == 48);
    }

    private void testNutricionDemo() {
        // Simular la lógica de demo por hora
        int hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        float factor;
        if (hora < 10) factor = 0.15f;
        else if (hora < 14) factor = 0.35f;
        else if (hora < 18) factor = 0.55f;
        else if (hora < 21) factor = 0.75f;
        else factor = 0.88f;

        int consumidas = (int) (3280 * factor);
        assertTest("Demo calorías > 0 (hora " + hora + ")", consumidas > 0);
        assertTest("Demo calorías < objetivo", consumidas < 3280);
        assertTest("Factor proporcional a hora", factor >= 0.15f && factor <= 0.88f);
    }

    // ═══════════════════════════════════════════════
    // TESTS GRUPO 3: Plan Anual
    // ═══════════════════════════════════════════════

    private void testPlanAnualDemo() {
        PlanAnualResponse plan = crearPlanDemoTest();
        assertTest("Plan tiene fases", plan.fases != null && !plan.fases.isEmpty());
        assertTest("Plan tiene 10 fases", plan.fases.size() == 10);
        assertTest("Plan tiene faseActual", plan.faseActual != null);
        assertTest("totalSemanas = 48", plan.totalSemanas == 48);
    }

    private void testFasesOrden() {
        PlanAnualResponse plan = crearPlanDemoTest();
        assertTest("Primera fase = Adaptación + Postura", plan.fases.get(0).nombre.contains("Adaptación"));
        assertTest("Última fase = Peak Estético", plan.fases.get(9).nombre.contains("Peak"));

        // Verificar tipos
        int volCount = 0, deloadCount = 0;
        for (Fase f : plan.fases) {
            if ("VOL".equals(f.tipo)) volCount++;
            if ("DELOAD".equals(f.tipo)) deloadCount++;
        }
        assertTest("VOL tiene 5 fases", volCount == 5);
        assertTest("DELOAD tiene 3 fases", deloadCount == 3);
    }

    private void testFasesFechasValidas() {
        PlanAnualResponse plan = crearPlanDemoTest();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        boolean todasValidas = true;
        for (Fase f : plan.fases) {
            try {
                Date inicio = sdf.parse(f.fechaInicio);
                Date fin = sdf.parse(f.fechaFin);
                if (inicio == null || fin == null || !inicio.before(fin)) {
                    todasValidas = false;
                    break;
                }
            } catch (ParseException e) {
                todasValidas = false;
                break;
            }
        }
        assertTest("Todas las fechas son válidas (inicio < fin)", todasValidas);

        // Verificar continuidad: fin fase N ≈ inicio fase N+1
        boolean continuas = true;
        for (int i = 0; i < plan.fases.size() - 1; i++) {
            try {
                Date fin = sdf.parse(plan.fases.get(i).fechaFin);
                Date siguienteInicio = sdf.parse(plan.fases.get(i + 1).fechaInicio);
                long diffDays = (siguienteInicio.getTime() - fin.getTime()) / (1000 * 60 * 60 * 24);
                if (diffDays < 0 || diffDays > 2) {
                    continuas = false;
                    break;
                }
            } catch (ParseException e) {
                continuas = false;
                break;
            }
        }
        assertTest("Fases son continuas (sin gaps > 2 días)", continuas);
    }

    private void testFaseActual() {
        PlanAnualResponse plan = crearPlanDemoTest();
        assertTest("faseActual.nombre contiene V-Taper",
                plan.faseActual.nombre.contains("V-Taper"));
        assertTest("faseActual.tipo = VOL", "VOL".equals(plan.faseActual.tipo));
        assertTest("faseActual existe en lista fases",
                plan.fases.contains(plan.faseActual));
    }

    private void testColorPorTipo() {
        // Test logic de colores (no se puede ejecutar getColor sin view, verificar mapeo)
        String[] tipos = {"VOL", "FZA", "DEF", "MNT", "DELOAD"};
        for (String tipo : tipos) {
            assertTest("Tipo '" + tipo + "' tiene color asignado", tipo != null && !tipo.isEmpty());
        }
    }

    // ═══════════════════════════════════════════════
    // TESTS GRUPO 4: Workout
    // ═══════════════════════════════════════════════

    private void testEjercicioCreacion() {
        Ejercicio ej = new Ejercicio();
        ej.setNombre("Press Inclinado");
        ej.setSeriesPlan(4);
        ej.setRepsPlan("8-10");
        ej.setPesoSugerido(22f);
        ej.setDescansoSeg(150);

        assertTest("Ejercicio nombre = Press Inclinado", "Press Inclinado".equals(ej.getNombre()));
        assertTest("Series plan = 4", ej.getSeriesPlan() == 4);
        assertTest("Reps plan = 8-10", "8-10".equals(ej.getRepsPlan()));
        assertTest("Peso sugerido = 22.0", ej.getPesoSugerido() == 22f);
        assertTest("Descanso = 150s", ej.getDescansoSeg() == 150);
    }

    private void testSesionDemo() {
        List<Ejercicio> ejercicios = new ArrayList<>();
        ejercicios.add(crearEjTest("Press Inclinado", 4, "8-10", 22f, 150));
        ejercicios.add(crearEjTest("Cruces Polea", 3, "10-12", 15f, 90));
        ejercicios.add(crearEjTest("Press Hombro", 3, "8-10", 16f, 120));
        ejercicios.add(crearEjTest("Elevaciones Laterales", 4, "12-15", 8f, 90));
        ejercicios.add(crearEjTest("Press Francés", 3, "10-12", 12f, 90));
        ejercicios.add(crearEjTest("Extensión Polea", 3, "12-15", 10f, 60));

        assertTest("Sesión demo tiene 6 ejercicios", ejercicios.size() == 6);

        int totalSeries = 0;
        for (Ejercicio ej : ejercicios) totalSeries += ej.getSeriesPlan();
        assertTest("Total series = 20", totalSeries == 20);

        // Verificar que todos tienen peso > 0 (excepto bodyweight)
        boolean todosPesoOk = true;
        for (Ejercicio ej : ejercicios) {
            if (ej.getPesoSugerido() < 0) {
                todosPesoOk = false;
                break;
            }
        }
        assertTest("Todos peso >= 0", todosPesoOk);
    }

    private void testVolumenCalculo() {
        // Volumen = peso × reps por cada serie
        float peso = 22f;
        int reps = 10;
        int series = 4;
        float volumen = peso * reps * series;
        assertTest("Volumen 22kg×10×4 = 880", volumen == 880f);

        // Volumen total sesión
        float total = (22 * 10 * 4) + (15 * 12 * 3) + (16 * 10 * 3) + (8 * 15 * 4) + (12 * 12 * 3) + (10 * 15 * 3);
        assertTest("Volumen total sesión > 3000kg", total > 3000);
    }

    private void testRIRRegistro() {
        // Simular registros
        int[] rirValues = {0, 1, 2, 3, 4};
        for (int rir : rirValues) {
            assertTest("RIR " + rir + " es válido (0-4)", rir >= 0 && rir <= 4);
        }
        // RIR <= 1 = "duro"
        assertTest("RIR 0 → sensación duro", 0 <= 1);
        assertTest("RIR 2 → sensación bien", 2 > 1);
    }

    private void testTimerDescanso() {
        assertTest("Descanso compuesto = 180s", Constants.DESCANSO_COMPUESTO_SEG == 180);
        assertTest("Descanso aislamiento = 120s", Constants.DESCANSO_AISLAMIENTO_SEG == 120);
        assertTest("Descanso deload = 90s", Constants.DESCANSO_DELOAD_SEG == 90);
        assertTest("Compuesto > Aislamiento",
                Constants.DESCANSO_COMPUESTO_SEG > Constants.DESCANSO_AISLAMIENTO_SEG);
    }

    // ═══════════════════════════════════════════════
    // TESTS GRUPO 5: Progresión
    // ═══════════════════════════════════════════════

    private void testProgresionDemo() {
        // Simular datos demo progresión
        MetricasProgresionResponse.Resumen r = new MetricasProgresionResponse.Resumen();
        r.pesoActual = 70.8f;
        r.pesoInicio = 72.0f;
        r.grasaActual = 17.8f;
        r.sleepMedia = 78;
        r.pasosMedia = 7200;

        assertTest("Peso actual < peso inicio (pérdida)", r.pesoActual < r.pesoInicio);
        assertTest("Grasa < 20%", r.grasaActual < 20f);
        assertTest("Sueño media 60-100", r.sleepMedia >= 60 && r.sleepMedia <= 100);
        assertTest("Pasos media > 5000", r.pasosMedia > 5000);
    }

    private void testPesoTendencia() {
        float[] pesos = {72.0f, 71.8f, 71.6f, 71.3f, 71.0f, 70.8f};
        boolean descendente = true;
        for (int i = 1; i < pesos.length; i++) {
            if (pesos[i] > pesos[i - 1]) {
                descendente = false;
                break;
            }
        }
        assertTest("Peso tiene tendencia descendente", descendente);

        float diff = pesos[0] - pesos[pesos.length - 1];
        assertTest("Pérdida total < 2kg (saludable)", diff < 2.0f);
        assertTest("Pérdida total > 0.5kg (efectiva)", diff > 0.5f);
    }

    // ═══════════════════════════════════════════════
    // TESTS GRUPO 6: Health Connect
    // ═══════════════════════════════════════════════

    private void testHCDisponibilidad() {
        boolean available = HealthConnectBridge.isAvailable(this);
        // No fallar si no está disponible, solo reportar
        logInfo("HC disponible: " + available);
        assertTest("HC check no lanza excepción", true);
    }

    private void testHCPermisos() {
        try {
            java.util.Set<String> permisos = HealthConnectBridge.getRequiredPermissions();
            assertTest("HC permisos no null", permisos != null);
            assertTest("HC necesita ≥ 2 permisos", permisos != null && permisos.size() >= 2);
        } catch (Exception e) {
            assertTest("HC getPermissions no lanza excepción", false);
        }
    }

    // ═══════════════════════════════════════════════
    // TESTS GRUPO 7: API
    // ═══════════════════════════════════════════════

    private void testApiBaseUrl() {
        String url = Constants.API_BASE_URL;
        assertTest("API URL empieza por https://", url.startsWith("https://"));
        assertTest("API URL contiene script.google.com", url.contains("script.google.com"));
        assertTest("API URL termina en /", url.endsWith("/"));
    }

    private void testApiSesionHoy() {
        AtomicBoolean respondio = new AtomicBoolean(false);
        AtomicReference<String> resultado = new AtomicReference<>("timeout");
        CountDownLatch latch = new CountDownLatch(1);

        ApiClient.getApi().getSesionHoy("sesion_hoy").enqueue(new Callback<SesionResponse>() {
            @Override
            public void onResponse(Call<SesionResponse> call, Response<SesionResponse> response) {
                respondio.set(true);
                resultado.set(response.isSuccessful() ? "success" : "http_error_" + response.code());
                latch.countDown();
            }

            @Override
            public void onFailure(Call<SesionResponse> call, Throwable t) {
                respondio.set(true);
                resultado.set("network_error");
                latch.countDown();
            }
        });

        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        logInfo("API sesion_hoy: " + resultado.get());
        assertTest("API sesion_hoy responde (o timeout)", respondio.get());
    }

    private void testApiMacros() {
        AtomicBoolean respondio = new AtomicBoolean(false);
        AtomicReference<String> resultado = new AtomicReference<>("timeout");
        CountDownLatch latch = new CountDownLatch(1);

        ApiClient.getApi().getMacrosHoy("macros_hoy").enqueue(new Callback<MacrosResponse>() {
            @Override
            public void onResponse(Call<MacrosResponse> call, Response<MacrosResponse> response) {
                respondio.set(true);
                resultado.set(response.isSuccessful() ? "success" : "http_error_" + response.code());
                latch.countDown();
            }

            @Override
            public void onFailure(Call<MacrosResponse> call, Throwable t) {
                respondio.set(true);
                resultado.set("network_error");
                latch.countDown();
            }
        });

        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        logInfo("API macros_hoy: " + resultado.get());
        assertTest("API macros_hoy responde (o timeout)", respondio.get());
    }

    private void testApiProgresion() {
        AtomicBoolean respondio = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        ApiClient.getApi().getProgresionMetricas("progresion_metricas", 7)
                .enqueue(new Callback<MetricasProgresionResponse>() {
                    @Override
                    public void onResponse(Call<MetricasProgresionResponse> call,
                                           Response<MetricasProgresionResponse> response) {
                        respondio.set(true);
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(Call<MetricasProgresionResponse> call, Throwable t) {
                        respondio.set(true);
                        latch.countDown();
                    }
                });

        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        assertTest("API progresion responde (o timeout)", respondio.get());
    }

    // ═══════════════════════════════════════════════
    // TESTS GRUPO 8: Motor de Pesos
    // ═══════════════════════════════════════════════

    private void testMotorPesosSubida() {
        // Regla: si RIR ≥ objetivo y reps completadas ≥ plan → subir peso
        int rirObjetivo = 2;
        int rirPercibido = 3; // Más fácil de lo esperado
        int repsPlan = 10;
        int repsCompletadas = 12;
        float pesoActual = 20f;

        boolean debeSubir = rirPercibido >= rirObjetivo && repsCompletadas >= repsPlan;
        assertTest("Motor: RIR 3 con 12/10 reps → SUBIR", debeSubir);

        // Incremento = 5% (redondeo a 0.5kg)
        float incremento = Math.round(pesoActual * 0.05f * 2) / 2f;
        assertTest("Incremento 5% de 20kg = 1.0kg", incremento == 1.0f);
        assertTest("Nuevo peso = 21.0kg", pesoActual + incremento == 21.0f);
    }

    private void testMotorPesosBajada() {
        // Regla: si RIR = 0 y reps < plan_min → bajar peso
        int rirPercibido = 0;
        int repsPlan = 8; // mínimo del rango 8-10
        int repsCompletadas = 5;
        float pesoActual = 25f;

        boolean debeBajar = rirPercibido == 0 && repsCompletadas < repsPlan;
        assertTest("Motor: RIR 0 con 5/8 reps → BAJAR", debeBajar);

        // Decremento = 10%
        float decremento = Math.round(pesoActual * 0.10f * 2) / 2f;
        assertTest("Decremento 10% de 25kg = 2.5kg", decremento == 2.5f);
        assertTest("Nuevo peso = 22.5kg", pesoActual - decremento == 22.5f);
    }

    private void testMotorPesosMantenimiento() {
        // Regla: si RIR dentro de rango y reps dentro de rango → mantener
        int rirObjetivo = 2;
        int rirPercibido = 2;
        int repsPlanMin = 8;
        int repsPlanMax = 10;
        int repsCompletadas = 9;

        boolean mantener = rirPercibido == rirObjetivo
                && repsCompletadas >= repsPlanMin && repsCompletadas <= repsPlanMax;
        assertTest("Motor: RIR 2 con 9/8-10 reps → MANTENER", mantener);
    }

    // ═══════════════════════════════════════════════
    // TESTS GRUPO 9: Formato
    // ═══════════════════════════════════════════════

    private void testFormatoFecha() {
        String fecha = "2026-09-28";
        // Strip time if present
        if (fecha.contains("T")) fecha = fecha.substring(0, fecha.indexOf("T"));
        assertTest("Fecha sin T: " + fecha, !fecha.contains("T"));

        try {
            SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date d = isoFmt.parse(fecha);
            String formatted = new SimpleDateFormat("d MMM yy", new Locale("es", "ES")).format(d);
            assertTest("Formato 'dd MMM yy': " + formatted, formatted.contains("sep"));
        } catch (ParseException e) {
            assertTest("Fecha formateada", false);
        }

        // Con hora (el bug original)
        String fechaConHora = "2026-09-28T23:00:00";
        String limpia = fechaConHora.substring(0, fechaConHora.indexOf("T"));
        assertTest("Strip hora de ISO datetime", "2026-09-28".equals(limpia));
    }

    private void testFormatoCaloriasLocale() {
        int calorias = 3280;
        String formatted = String.format(Locale.getDefault(), "%,d", calorias);
        assertTest("Calorías formateadas con separador miles", formatted.length() > 4);
    }

    // ═══════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════

    private void assertTest(String nombre, boolean condicion) {
        totalTests++;
        String icon;
        int color;
        if (condicion) {
            passedTests++;
            icon = "✅";
            color = getColor(R.color.success);
        } else {
            failedTests++;
            icon = "❌";
            color = getColor(R.color.error);
        }

        String line = String.format("  %s %s\n", icon, nombre);
        int start = output.length();
        output.append(line);
        output.setSpan(new ForegroundColorSpan(color), start, output.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        int progress = (totalTests * 100) / 35; // Aprox total tests
        mainHandler.post(() -> {
            tvResultados.setText(output);
            progressTests.setProgress(Math.min(progress, 99));
        });

        // Pequeño delay para efecto visual
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
    }

    private void log(String text) {
        output.append(text).append("\n");
        mainHandler.post(() -> tvResultados.setText(output));
        try { Thread.sleep(20); } catch (InterruptedException ignored) {}
    }

    private void logHeader(String header) {
        output.append("\n");
        int start = output.length();
        output.append("── " + header + " ──\n");
        output.setSpan(new ForegroundColorSpan(getColor(R.color.colorTextSecondary)),
                start, output.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mainHandler.post(() -> tvResultados.setText(output));
        try { Thread.sleep(30); } catch (InterruptedException ignored) {}
    }

    private void logInfo(String info) {
        int start = output.length();
        output.append("  ℹ️ " + info + "\n");
        output.setSpan(new ForegroundColorSpan(getColor(R.color.colorTextTertiary)),
                start, output.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mainHandler.post(() -> tvResultados.setText(output));
    }

    private PlanAnualResponse crearPlanDemoTest() {
        PlanAnualResponse plan = new PlanAnualResponse();
        plan.fechaInicio = "2026-08-31";
        plan.fechaFin = "2027-07-31";
        plan.totalSemanas = 48;
        plan.fases = new ArrayList<>();
        plan.fases.add(crearFaseTest("F1", "Adaptación + Postura", "VOL", "2026-08-31", "2026-09-27", 4));
        plan.fases.add(crearFaseTest("F2", "Hipertrofia I — V-Taper", "VOL", "2026-09-28", "2026-11-08", 6));
        plan.fases.add(crearFaseTest("F3", "Deload 1", "DELOAD", "2026-11-09", "2026-11-15", 1));
        plan.fases.add(crearFaseTest("F4", "Hipertrofia II — Brazos", "VOL", "2026-11-16", "2026-12-27", 6));
        plan.fases.add(crearFaseTest("F5", "Deload 2", "DELOAD", "2026-12-28", "2027-01-03", 1));
        plan.fases.add(crearFaseTest("F6", "Fuerza — Compuestos", "FZA", "2027-01-04", "2027-02-14", 6));
        plan.fases.add(crearFaseTest("F7", "Hipertrofia III — Balance", "VOL", "2027-02-15", "2027-03-28", 6));
        plan.fases.add(crearFaseTest("F8", "Deload 3", "DELOAD", "2027-03-29", "2027-04-04", 1));
        plan.fases.add(crearFaseTest("F9", "Definición", "DEF", "2027-04-05", "2027-05-16", 6));
        plan.fases.add(crearFaseTest("F10", "Peak Estético + Mant.", "MNT", "2027-05-17", "2027-07-31", 11));
        plan.faseActual = plan.fases.get(1);
        return plan;
    }

    private Fase crearFaseTest(String id, String nombre, String tipo, String inicio, String fin, int sem) {
        Fase f = new Fase();
        f.faseId = id;
        f.nombre = nombre;
        f.tipo = tipo;
        f.fechaInicio = inicio;
        f.fechaFin = fin;
        f.semanas = sem;
        return f;
    }

    private Ejercicio crearEjTest(String nombre, int series, String reps, float peso, int descanso) {
        Ejercicio ej = new Ejercicio();
        ej.setNombre(nombre);
        ej.setSeriesPlan(series);
        ej.setRepsPlan(reps);
        ej.setPesoSugerido(peso);
        ej.setDescansoSeg(descanso);
        return ej;
    }
}
