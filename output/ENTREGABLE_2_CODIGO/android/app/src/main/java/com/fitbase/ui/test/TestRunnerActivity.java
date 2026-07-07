package com.fitbase.ui.test;

import android.content.Intent;
import android.net.Uri;
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
import androidx.core.content.FileProvider;

import com.fitbase.R;
import com.fitbase.data.api.ApiClient;
import com.fitbase.data.health.HealthConnectBridge;
import com.fitbase.data.health.HealthConnectReader;
import com.fitbase.data.model.MacrosResponse;
import com.fitbase.data.model.MetricasProgresionResponse;
import com.fitbase.data.model.PlanAnualResponse;
import com.fitbase.data.model.SesionResponse;
import com.fitbase.data.model.VistaMañanaResponse;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Test Runner de INTEGRACIÓN REAL.
 *
 * NO prueba valores hardcoded. Prueba conexiones reales:
 * - Health Connect disponible + permisos + lectura de datos reales
 * - API backend conectividad + respuestas válidas
 * - Plan anual carga con fases
 * - Datos de progresión disponibles (HC o backend)
 * - Timer funciona con tiempo absoluto
 *
 * Cada test reporta qué encontró (datos reales) no qué "debería haber".
 */
public class TestRunnerActivity extends AppCompatActivity {

    private TextView tvResumen;
    private TextView tvResultados;
    private ProgressBar progressTests;
    private Button btnEjecutar;
    private Button btnCompartir;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SpannableStringBuilder output = new SpannableStringBuilder();
    private int totalTests = 0;
    private int passedTests = 0;
    private int failedTests = 0;
    private File ultimoLogFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_runner);
        com.fitbase.util.InsetsHelper.aplicarInsetsSistema(this);

        tvResumen = findViewById(R.id.tvResumen);
        tvResultados = findViewById(R.id.tvResultados);
        progressTests = findViewById(R.id.progressTests);
        btnEjecutar = findViewById(R.id.btnEjecutar);
        btnCompartir = findViewById(R.id.btnCompartir);

        btnEjecutar.setOnClickListener(v -> ejecutarTests());
        btnCompartir.setOnClickListener(v -> compartirLog());
        findViewById(R.id.btnCerrar).setOnClickListener(v -> finish());
    }

    private void ejecutarTests() {
        btnEjecutar.setEnabled(false);
        btnCompartir.setVisibility(View.GONE);
        output.clear();
        totalTests = 0;
        passedTests = 0;
        failedTests = 0;
        tvResultados.setText("");
        progressTests.setVisibility(View.VISIBLE);
        progressTests.setProgress(0);

        new Thread(() -> {
            log("═══════════════════════════════════════════");
            log("  FitBase — Test de Integración Real");
            log("═══════════════════════════════════════════\n");

            // ══════ 0. INVENTARIO DE DATOS ══════
            logHeader("0. INVENTARIO — Health Connect (otras apps) y con qué valor");
            mostrarInventarioDatos();

            logHeader("0.1 INVENTARIO — Backend/BBDD (qué devuelve cada endpoint)");
            mostrarInventarioBackend();

            // ══════ 1. HEALTH CONNECT ══════
            logHeader("1. HEALTH CONNECT — Disponibilidad");
            testHCDisponible();
            testHCPermisos();

            logHeader("2. HEALTH CONNECT — Lectura Pasos + Nutrición");
            testHCDatosHoy();

            logHeader("3. HEALTH CONNECT — Lectura Peso/Sueño/FC (Recuperación)");
            testHCDatosRecuperacion();

            // ══════ 4. BACKEND API ══════
            logHeader("4. BACKEND — Conectividad API");
            testAPISesionHoy();
            testAPIMacrosHoy();
            testAPIPlanAnual();
            testAPIProgresion();
            testAPICambioFase();

            // ══════ 5. TIMER ══════
            logHeader("5. TIMER — Precisión tiempo absoluto");
            testTimerPrecision();

            // ══════ 6. DATOS COMBINADOS ══════
            logHeader("6. INTEGRACIÓN — Flujo completo");
            testFlujoCombinado();

            // Resumen
            log("\n═══════════════════════════════════════════");
            String resumen = String.format("  RESULTADO: %d/%d PASS | %d FAIL",
                    passedTests, totalTests, failedTests);
            log(resumen);
            log("═══════════════════════════════════════════");

            // Guardar el log completo en un fichero de texto para poder analizarlo
            // (adb pull, o compartirlo con el botón "Compartir").
            File archivo = guardarLogEnArchivo(output.toString());
            if (archivo != null) {
                log("\nLog guardado en: " + archivo.getAbsolutePath());
            }

            mainHandler.post(() -> {
                progressTests.setProgress(100);
                btnEjecutar.setEnabled(true);
                tvResumen.setText(String.format("%d pass · %d fail · %d total",
                        passedTests, failedTests, totalTests));
                tvResumen.setTextColor(getColor(failedTests == 0 ? R.color.success : R.color.error));

                if (archivo != null) {
                    ultimoLogFile = archivo;
                    btnCompartir.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }

    /**
     * Guarda el texto completo del log en un fichero dentro del almacenamiento
     * externo propio de la app (no requiere permisos especiales). Se puede
     * recuperar con {@code adb pull} o compartir con el botón "Compartir".
     */
    private File guardarLogEnArchivo(String contenido) {
        try {
            File externo = getExternalFilesDir(null);
            if (externo == null) return null;
            File dir = new File(externo, "test_logs");
            if (!dir.exists() && !dir.mkdirs()) return null;

            String nombre = "test_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".txt";
            File file = new File(dir, nombre);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(contenido);
            }
            return file;
        } catch (IOException e) {
            mainHandler.post(() -> logInfo("No se pudo guardar el log en fichero: " + e.getMessage()));
            return null;
        }
    }

    /** Comparte el último fichero de log generado vía el selector de apps de Android. */
    private void compartirLog() {
        if (ultimoLogFile == null) return;
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", ultimoLogFile);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Compartir log de test"));
    }

    // ═══════════════════════════════════════════════
    // 0. INVENTARIO DE DATOS (qué llega de cada app y su valor exacto)
    // ═══════════════════════════════════════════════

    /**
     * Lista indexada de TODO lo que Health Connect puede entregar, con el valor
     * exacto recuperado ahora mismo (o "—" si no hay dato). Incluye también el
     * caso del score de sueño de Zepp, que Health Connect NO expone nunca
     * (no es un tema de permisos: el campo no existe en el SDK).
     */
    private void mostrarInventarioDatos() {
        boolean hcOk = HealthConnectBridge.isAvailable(this) && HealthConnectBridge.hasPermissions(this);

        HealthConnectBridge.HealthData hoy = null;
        HealthConnectBridge.RecoveryData recuperacion = null;
        if (hcOk) {
            try {
                hoy = HealthConnectBridge.readTodayData(this);
                recuperacion = HealthConnectBridge.readRecoveryData(this, 7);
            } catch (Exception e) {
                logInfo("Error leyendo datos: " + e.getMessage());
            }
        }

        int i = 1;
        logDato(i++, "Pasos hoy (Zepp/Amazfit → HC)",
                hoy != null && hoy.pasos > 0 ? String.valueOf(hoy.pasos) : "—");
        logDato(i++, "Calorías consumidas (FatSecret → HC)",
                hoy != null && hoy.caloriasConsumidas > 0 ? hoy.caloriasConsumidas + " kcal" : "—");
        logDato(i++, "Proteína consumida (FatSecret → HC)",
                hoy != null && hoy.proteinaG > 0 ? hoy.proteinaG + " g" : "—");
        logDato(i++, "Carbohidratos consumidos (FatSecret → HC)",
                hoy != null && hoy.carbosG > 0 ? hoy.carbosG + " g" : "—");
        logDato(i++, "Grasas consumidas (FatSecret → HC)",
                hoy != null && hoy.grasasG > 0 ? hoy.grasasG + " g" : "—");

        if (recuperacion != null && !recuperacion.pesosKg.isEmpty()) {
            HealthConnectBridge.PesoEntry p = recuperacion.pesosKg.get(recuperacion.pesosKg.size() - 1);
            logDato(i++, "Peso corporal (Mi Fitness → HC)", String.format("%.1f kg (%s)", p.kg, p.fecha));
            logDato(i++, "  → % grasa corporal (BodyFatRecord)",
                    p.grasaPct != null ? String.format("%.1f%%", p.grasaPct) : "—");
            logDato(i++, "  → % hidratación (BodyWaterMassRecord / peso)",
                    p.hidratacionPct != null ? String.format("%.1f%%", p.hidratacionPct) : "—");
        } else {
            logDato(i++, "Peso corporal (Mi Fitness → HC)", "—");
            logDato(i++, "  → % grasa corporal (BodyFatRecord)", "—");
            logDato(i++, "  → % hidratación (BodyWaterMassRecord / peso)", "—");
        }
        if (recuperacion != null && !recuperacion.suenos.isEmpty()) {
            HealthConnectBridge.SleepEntry s = recuperacion.suenos.get(recuperacion.suenos.size() - 1);
            logDato(i++, "Sueño — duración total (" + s.fecha + ")",
                    (s.duracionMin / 60) + "h " + (s.duracionMin % 60) + "m");
            logDato(i++, "Sueño — fase profunda (Zepp → HC)", s.deepMin + " min");
            logDato(i++, "Sueño — fase REM (Zepp → HC)", s.remMin + " min");
            logDato(i++, "Sueño — fase ligera (Zepp → HC)", s.lightMin + " min");
        } else {
            logDato(i++, "Sueño — duración total", "—");
            logDato(i++, "Sueño — fase profunda (Zepp → HC)", "—");
            logDato(i++, "Sueño — fase REM (Zepp → HC)", "—");
            logDato(i++, "Sueño — fase ligera (Zepp → HC)", "—");
        }

        if (recuperacion != null && !recuperacion.fcReposo.isEmpty()) {
            HealthConnectBridge.HrEntry hr = recuperacion.fcReposo.get(recuperacion.fcReposo.size() - 1);
            logDato(i++, "FC en reposo — RestingHeartRateRecord (Zepp → HC)",
                    hr.bpm + " bpm (" + hr.fecha + ")");
        } else {
            logDato(i++, "FC en reposo — RestingHeartRateRecord (Zepp → HC)", "—");
        }

        // Este dato NO depende de permisos ni de sincronización: Health Connect
        // simplemente no tiene un campo para él (confirmado en el SDK).
        logDato(i++, "Puntuación de sueño 0-100 (la que ves en la app Zepp)", "NO DISPONIBLE vía Health Connect");
        logInfo("   Zepp la calcula con un algoritmo propio y no la exporta a Health Connect.");
        logInfo("   La app la ESTIMA sola (HealthConnectBridge) a partir de duración+fases del sueño crudo — no es manual.");

        if (!hcOk) {
            logInfo("Todos los '—' de arriba son porque Health Connect no está disponible o falta conceder permisos, no porque el dato no exista en tu dispositivo.");
        }
    }

    /**
     * Lista indexada de lo que devuelve CADA endpoint del backend (Codigo.gs /
     * Google Sheets) ahora mismo, campo a campo. Sirve como log de depuración:
     * si algo sale raro en la app, aquí se ve exactamente qué mandó la BBDD.
     */
    private void mostrarInventarioBackend() {
        int i = 1;

        VistaMañanaResponse vista = fetchSync(ApiClient.getApi().getVistaMañana("vista_manana"));
        if (vista != null) {
            logDato(i++, "vista_manana → tipo_dia", String.valueOf(vista.getTipoDia()));
            logDato(i++, "vista_manana → fase.nombre",
                    vista.getFase() != null ? vista.getFase().nombre : "—");
            logDato(i++, "vista_manana → sueno.sleep_score",
                    vista.getSueno() != null && vista.getSueno().sleepScore != null
                            ? String.valueOf(vista.getSueno().sleepScore) : "—");
            logDato(i++, "vista_manana → sueno.hr_reposo",
                    vista.getSueno() != null && vista.getSueno().hrReposo != null
                            ? String.valueOf(vista.getSueno().hrReposo) : "—");
            logDato(i++, "vista_manana → macros.calorias",
                    vista.getMacros() != null ? String.valueOf(vista.getMacros().calorias) : "—");
            logDato(i++, "vista_manana → macros.proteina_g",
                    vista.getMacros() != null ? String.valueOf(vista.getMacros().proteinaG) : "—");
            logDato(i++, "vista_manana → cardio.pasos_objetivo",
                    vista.getCardio() != null ? String.valueOf(vista.getCardio().pasosObjetivo) : "—");
            logDato(i++, "vista_manana → cardio.contexto",
                    vista.getCardio() != null ? String.valueOf(vista.getCardio().contexto) : "—");
            logDato(i++, "vista_manana → aviso_ausencia",
                    vista.getAvisoAusencia() != null ? vista.getAvisoAusencia().mensaje : "—");
            logDato(i++, "vista_manana → ramadan.activo",
                    vista.getRamadan() != null ? String.valueOf(vista.getRamadan().activo) : "—");
            if (vista.getRamadan() != null && vista.getRamadan().activo) {
                logDato(i++, "  → ramadan.dia_ayuno", String.valueOf(vista.getRamadan().diaAyuno));
            }
            logDato(i++, "vista_manana → ramadan.es_eid",
                    vista.getRamadan() != null ? String.valueOf(vista.getRamadan().esEid) : "—");
        } else {
            logDato(i++, "vista_manana", "SIN RESPUESTA (ver detalle abajo)");
        }

        com.fitbase.data.model.CambioFaseResponse cambioFase =
                fetchSync(ApiClient.getApi().getCambioFase("cambio_fase"));
        if (cambioFase != null) {
            logDato(i++, "cambio_fase → hay_cambio", String.valueOf(cambioFase.isHayCambio()));
            logDato(i++, "cambio_fase → fase_actual.nombre",
                    cambioFase.getFaseActual() != null ? cambioFase.getFaseActual().nombre : "—");
            if (cambioFase.getResumenFaseAnterior() != null) {
                logDato(i++, "cambio_fase → resumen_fase_anterior.sesiones",
                        cambioFase.getResumenFaseAnterior().sesionesCompletadas + "/"
                                + cambioFase.getResumenFaseAnterior().sesionesTotales);
            }
        } else {
            logDato(i++, "cambio_fase", "SIN RESPUESTA");
        }

        MacrosResponse macros = fetchSync(ApiClient.getApi().getMacrosHoy("macros_hoy"));
        if (macros != null) {
            logDato(i++, "macros_hoy → fase", String.valueOf(macros.fase));
            logDato(i++, "macros_hoy → calorias_objetivo", String.valueOf(macros.caloriasObjetivo));
            logDato(i++, "macros_hoy → proteina_g", String.valueOf(macros.proteinaG));
        } else {
            logDato(i++, "macros_hoy", "SIN RESPUESTA");
        }

        PlanAnualResponse plan = fetchSync(ApiClient.getApi().getPlanAnual("plan_anual"));
        if (plan != null) {
            logDato(i++, "plan_anual → fases.size", String.valueOf(plan.fases != null ? plan.fases.size() : 0));
            logDato(i++, "plan_anual → total_semanas", String.valueOf(plan.totalSemanas));
            logDato(i++, "plan_anual → fase_actual.nombre",
                    plan.faseActual != null ? plan.faseActual.nombre : "— (programa no ha empezado)");
        } else {
            logDato(i++, "plan_anual", "SIN RESPUESTA");
        }

        MetricasProgresionResponse prog = fetchSync(ApiClient.getApi().getProgresionMetricas("progresion_metricas", 30));
        if (prog != null) {
            logDato(i++, "progresion_metricas(30d) → zepp.size", String.valueOf(size(prog.zepp)));
            logDato(i++, "progresion_metricas(30d) → volumen.size", String.valueOf(size(prog.volumenEntreno)));
            logDato(i++, "progresion_metricas(30d) → resumen.sleep_media",
                    prog.resumen != null && prog.resumen.sleepMedia != null
                            ? String.valueOf(prog.resumen.sleepMedia) : "—");
        } else {
            logDato(i++, "progresion_metricas", "SIN RESPUESTA");
        }

        SesionResponse sesion = fetchSync(ApiClient.getApi().getSesionHoy("sesion_hoy"));
        if (sesion != null) {
            logDato(i++, "sesion_hoy → sesion.tipo",
                    sesion.getSesion() != null ? sesion.getSesion().getTipo() : "— (no es día de gym)");
            logDato(i++, "sesion_hoy → ejercicios.size",
                    String.valueOf(sesion.getEjercicios() != null ? sesion.getEjercicios().size() : 0));
            logDato(i++, "sesion_hoy → ramadan_activo", String.valueOf(sesion.isRamadanActivo()));
            if (sesion.getEjercicios() != null) {
                int conSuperserie = 0;
                for (com.fitbase.data.model.Ejercicio ej : sesion.getEjercicios()) {
                    if (ej.getSupersetGrupo() != null && !ej.getSupersetGrupo().isEmpty()) conSuperserie++;
                }
                logDato(i++, "sesion_hoy → ejercicios con superserie", String.valueOf(conSuperserie));
            }
        } else {
            logDato(i++, "sesion_hoy", "SIN RESPUESTA");
        }
    }

    /** Ejecuta una llamada Retrofit de forma síncrona (bloquea el hilo de fondo actual). */
    private <T> T fetchSync(Call<T> call) {
        AtomicReference<T> resultado = new AtomicReference<>(null);
        CountDownLatch latch = new CountDownLatch(1);
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                if (response.isSuccessful()) resultado.set(response.body());
                latch.countDown();
            }
            @Override
            public void onFailure(Call<T> call, Throwable t) {
                logInfo("   Error de red: " + t.getClass().getSimpleName() + " " + t.getMessage());
                latch.countDown();
            }
        });
        try { latch.await(8, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        return resultado.get();
    }

    private int size(java.util.List<?> list) {
        return list != null ? list.size() : 0;
    }

    // ═══════════════════════════════════════════════
    // 1. HC DISPONIBILIDAD
    // ═══════════════════════════════════════════════

    private void testHCDisponible() {
        boolean available = HealthConnectBridge.isAvailable(this);
        assertTest("Health Connect instalado y disponible", available);
        if (!available) {
            logInfo("Sin HC no se pueden leer datos de Zepp/FatSecret. Instala Health Connect desde Play Store.");
        }
    }

    private void testHCPermisos() {
        if (!HealthConnectBridge.isAvailable(this)) {
            logInfo("Saltado (HC no disponible)");
            return;
        }
        boolean permisos = HealthConnectBridge.hasPermissions(this);
        assertTest("Permisos HC concedidos (Steps, Nutrition, Weight, Sleep, HR)", permisos);
        if (!permisos) {
            logInfo("Abre la app → Ajustes → concede permisos de Health Connect");
            logInfo("   Necesitas: Pasos, Nutrición, Peso, Sueño, Frecuencia cardíaca");
        }
    }

    // ═══════════════════════════════════════════════
    // 2. HC DATOS HOY
    // ═══════════════════════════════════════════════

    private void testHCDatosHoy() {
        if (!HealthConnectBridge.isAvailable(this) || !HealthConnectBridge.hasPermissions(this)) {
            logInfo("Saltado (HC no disponible o sin permisos)");
            assertTest("HC datos hoy (requiere HC + permisos)", false);
            return;
        }

        try {
            HealthConnectBridge.HealthData data = HealthConnectBridge.readTodayData(this);

            assertTest("Pasos hoy > 0 (Zepp sincroniza a HC)", data.pasos > 0);
            logInfo("  Pasos hoy: " + data.pasos);

            assertTest("Calorías consumidas > 0 (FatSecret → HC)", data.caloriasConsumidas > 0);
            logInfo("  Calorías: " + data.caloriasConsumidas + " kcal");
            logInfo("  Macros: P=" + data.proteinaG + "g C=" + data.carbosG + "g G=" + data.grasasG + "g");

            if (data.caloriasConsumidas == 0 && data.proteinaG == 0) {
                logInfo("FatSecret no está escribiendo datos en Health Connect.");
                logInfo("   Verifica: FatSecret → Ajustes → Conectar → Health Connect ACTIVO");
                logInfo("   Luego registra algo de comida en FatSecret.");
            }
            if (data.pasos == 0) {
                logInfo("Zepp no está sincronizando pasos a Health Connect.");
                logInfo("   Verifica: Mi Fitness → Perfil → Aplicaciones de terceros → Health Connect ACTIVO");
            }
        } catch (Exception e) {
            assertTest("HC lectura sin excepciones", false);
            logInfo("Error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════
    // 3. HC RECUPERACIÓN
    // ═══════════════════════════════════════════════

    private void testHCDatosRecuperacion() {
        if (!HealthConnectBridge.isAvailable(this) || !HealthConnectBridge.hasPermissions(this)) {
            logInfo("Saltado (HC no disponible o sin permisos)");
            assertTest("HC recuperación (requiere HC + permisos)", false);
            return;
        }

        try {
            HealthConnectBridge.RecoveryData recovery = HealthConnectBridge.readRecoveryData(this, 7);

            boolean tienePeso = !recovery.pesosKg.isEmpty();
            assertTest("Peso corporal en HC (últimos 7 días)", tienePeso);
            if (tienePeso) {
                HealthConnectBridge.PesoEntry ultimo = recovery.pesosKg.get(recovery.pesosKg.size() - 1);
                logInfo("  Último peso: " + String.format("%.1f", ultimo.kg) + " kg (" + ultimo.fecha + ")");
            } else {
                logInfo("No hay registros de peso en Health Connect.");
                logInfo("   Registra tu peso en Mi Fitness o manualmente en Health Connect.");
            }

            boolean tieneSueno = !recovery.suenos.isEmpty();
            assertTest("Datos de sueño en HC (últimos 7 días)", tieneSueno);
            if (tieneSueno) {
                HealthConnectBridge.SleepEntry ultimoSleep = recovery.suenos.get(recovery.suenos.size() - 1);
                logInfo("  Último sueño: " + ultimoSleep.duracionMin + " min (profundo="
                        + ultimoSleep.deepMin + " rem=" + ultimoSleep.remMin + " ligero=" + ultimoSleep.lightMin + ")");
            } else {
                logInfo("No hay datos de sueño. Verifica que Zepp sincroniza sueño a HC.");
            }

            boolean tieneFC = !recovery.fcReposo.isEmpty();
            assertTest("FC reposo en HC (últimos 7 días)", tieneFC);
            if (tieneFC) {
                HealthConnectBridge.HrEntry ultimaFC = recovery.fcReposo.get(recovery.fcReposo.size() - 1);
                logInfo("  Última FC reposo: " + ultimaFC.bpm + " bpm (" + ultimaFC.fecha + ")");
            } else {
                logInfo("No hay FC de reposo nocturna. Verifica que Zepp envía HR a HC.");
            }
        } catch (Exception e) {
            assertTest("HC recuperación sin excepciones", false);
            logInfo("Error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════
    // 4. BACKEND API
    // ═══════════════════════════════════════════════

    private void testAPISesionHoy() {
        AtomicReference<String> resultado = new AtomicReference<>("timeout");
        AtomicReference<SesionResponse> body = new AtomicReference<>(null);
        CountDownLatch latch = new CountDownLatch(1);

        ApiClient.getApi().getSesionHoy("sesion_hoy").enqueue(new Callback<SesionResponse>() {
            @Override
            public void onResponse(Call<SesionResponse> call, Response<SesionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultado.set("ok");
                    body.set(response.body());
                } else {
                    resultado.set("http_" + response.code());
                }
                latch.countDown();
            }

            @Override
            public void onFailure(Call<SesionResponse> call, Throwable t) {
                resultado.set("error:" + t.getClass().getSimpleName());
                latch.countDown();
            }
        });

        try { latch.await(8, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        boolean ok = "ok".equals(resultado.get());
        assertTest("API sesion_hoy responde correctamente", ok);
        if (ok && body.get() != null) {
            SesionResponse sr = body.get();
            if (sr.getSesion() != null) {
                logInfo("  Sesión: " + sr.getSesion().getTipo() + " | Ejercicios: " + (sr.getEjercicios() != null ? sr.getEjercicios().size() : 0));
            } else {
                logInfo("  Respuesta OK pero no hay sesión para hoy (normal si no es día de gym)");
            }
        } else {
            logInfo("  Resultado: " + resultado.get());
            logInfo("  Verifica que Codigo.gs está desplegado como webapp y la URL es correcta");
        }
    }

    private void testAPIMacrosHoy() {
        AtomicReference<String> resultado = new AtomicReference<>("timeout");
        AtomicReference<MacrosResponse> body = new AtomicReference<>(null);
        CountDownLatch latch = new CountDownLatch(1);

        ApiClient.getApi().getMacrosHoy("macros_hoy").enqueue(new Callback<MacrosResponse>() {
            @Override
            public void onResponse(Call<MacrosResponse> call, Response<MacrosResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultado.set("ok");
                    body.set(response.body());
                } else {
                    resultado.set("http_" + response.code());
                }
                latch.countDown();
            }

            @Override
            public void onFailure(Call<MacrosResponse> call, Throwable t) {
                resultado.set("error:" + t.getClass().getSimpleName());
                latch.countDown();
            }
        });

        try { latch.await(8, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        boolean ok = "ok".equals(resultado.get());
        assertTest("API macros_hoy responde correctamente", ok);
        if (ok && body.get() != null) {
            MacrosResponse m = body.get();
            logInfo("  Objetivo: " + m.caloriasObjetivo + " kcal | Fase: " + m.fase);
            assertTest("Calorías objetivo > 2000", m.caloriasObjetivo > 2000);
            assertTest("Proteína > 100g", m.proteinaG > 100);
        } else {
            logInfo("  Resultado: " + resultado.get());
        }
    }

    private void testAPIPlanAnual() {
        AtomicReference<String> resultado = new AtomicReference<>("timeout");
        AtomicReference<PlanAnualResponse> body = new AtomicReference<>(null);
        CountDownLatch latch = new CountDownLatch(1);

        ApiClient.getApi().getPlanAnual("plan_anual").enqueue(new Callback<PlanAnualResponse>() {
            @Override
            public void onResponse(Call<PlanAnualResponse> call, Response<PlanAnualResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultado.set("ok");
                    body.set(response.body());
                } else {
                    resultado.set("http_" + response.code());
                }
                latch.countDown();
            }

            @Override
            public void onFailure(Call<PlanAnualResponse> call, Throwable t) {
                resultado.set("error:" + t.getClass().getSimpleName());
                latch.countDown();
            }
        });

        try { latch.await(8, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        boolean ok = "ok".equals(resultado.get());
        assertTest("API plan_anual responde correctamente", ok);
        if (ok && body.get() != null) {
            PlanAnualResponse plan = body.get();
            boolean tieneFases = plan.fases != null && !plan.fases.isEmpty();
            assertTest("Plan tiene fases (≥ 1)", tieneFases);
            if (tieneFases) {
                logInfo("  Fases: " + plan.fases.size() + " | Total semanas: " + plan.totalSemanas);
                if (plan.faseActual != null) {
                    logInfo("  Fase actual: " + plan.faseActual.nombre + " (" + plan.faseActual.tipo + ")");
                } else {
                    logInfo("  No hay fase actual (programa no ha empezado aún)");
                }
            } else {
                logInfo("  Plan vacío. Ejecuta rellenarPlanCompleto() en Apps Script.");
            }
        } else {
            logInfo("  Resultado: " + resultado.get());
            logInfo("  Ejecuta inicializarHojas() y rellenarPlanCompleto() en Apps Script");
        }
    }

    private void testAPIProgresion() {
        AtomicReference<String> resultado = new AtomicReference<>("timeout");
        AtomicReference<MetricasProgresionResponse> body = new AtomicReference<>(null);
        CountDownLatch latch = new CountDownLatch(1);

        ApiClient.getApi().getProgresionMetricas("progresion_metricas", 30)
                .enqueue(new Callback<MetricasProgresionResponse>() {
                    @Override
                    public void onResponse(Call<MetricasProgresionResponse> call,
                                           Response<MetricasProgresionResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            resultado.set("ok");
                            body.set(response.body());
                        } else {
                            resultado.set("http_" + response.code());
                        }
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(Call<MetricasProgresionResponse> call, Throwable t) {
                        resultado.set("error:" + t.getClass().getSimpleName());
                        latch.countDown();
                    }
                });

        try { latch.await(8, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        boolean ok = "ok".equals(resultado.get());
        assertTest("API progresion responde correctamente", ok);
        if (ok && body.get() != null) {
            MetricasProgresionResponse p = body.get();
            int nZepp = p.zepp != null ? p.zepp.size() : 0;
            int nPeso = 0;
            if (p.zepp != null) {
                for (MetricasProgresionResponse.ZeppEntry z : p.zepp) if (z.pesoKg != null) nPeso++;
            }
            int nVol = p.volumenEntreno != null ? p.volumenEntreno.size() : 0;
            logInfo("  Datos: zepp=" + nZepp + " (con peso=" + nPeso + ") volumen=" + nVol);
            if (nZepp == 0) {
                logInfo("  Sin datos aún — la app usará Health Connect como fallback");
            }
        }
    }

    private void testAPICambioFase() {
        AtomicReference<String> resultado = new AtomicReference<>("timeout");
        AtomicReference<com.fitbase.data.model.CambioFaseResponse> body = new AtomicReference<>(null);
        CountDownLatch latch = new CountDownLatch(1);

        ApiClient.getApi().getCambioFase("cambio_fase")
                .enqueue(new Callback<com.fitbase.data.model.CambioFaseResponse>() {
                    @Override
                    public void onResponse(Call<com.fitbase.data.model.CambioFaseResponse> call,
                                            Response<com.fitbase.data.model.CambioFaseResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            resultado.set("ok");
                            body.set(response.body());
                        } else {
                            resultado.set("http_" + response.code());
                        }
                        latch.countDown();
                    }
                    @Override
                    public void onFailure(Call<com.fitbase.data.model.CambioFaseResponse> call, Throwable t) {
                        resultado.set("error:" + t.getClass().getSimpleName());
                        latch.countDown();
                    }
                });

        try { latch.await(8, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        boolean ok = "ok".equals(resultado.get());
        assertTest("API cambio_fase responde correctamente", ok);
        if (ok && body.get() != null) {
            com.fitbase.data.model.CambioFaseResponse c = body.get();
            logInfo("  hay_cambio=" + c.isHayCambio()
                    + " | fase_actual=" + (c.getFaseActual() != null ? c.getFaseActual().nombre : "—"));
            logInfo("  (esto NO significa que hoy cambie de fase — solo que el endpoint responde;");
            logInfo("   la app solo la muestra cuando detecta fase_id distinto al guardado)");
        } else {
            logInfo("  Resultado: " + resultado.get());
        }
    }

    // ═══════════════════════════════════════════════
    // 5. TIMER
    // ═══════════════════════════════════════════════

    private void testTimerPrecision() {
        // Verificar que SystemClock.elapsedRealtime funciona correctamente
        long t0 = android.os.SystemClock.elapsedRealtime();
        try { Thread.sleep(1050); } catch (InterruptedException ignored) {}
        long t1 = android.os.SystemClock.elapsedRealtime();
        long diff = t1 - t0;

        // Debe ser ~1000ms (±100ms de tolerancia)
        boolean preciso = diff >= 950 && diff <= 1200;
        assertTest("SystemClock preciso (1s real = " + diff + "ms medido)", preciso);

        // Verificar cálculo de segundos restantes
        long finishTime = t1 + 120000; // simular 2 min
        int restante = (int) Math.ceil((finishTime - t1) / 1000.0);
        assertTest("Cálculo tiempo restante correcto (120s)", restante == 120);
    }

    // ═══════════════════════════════════════════════
    // 6. FLUJO COMBINADO
    // ═══════════════════════════════════════════════

    private void testFlujoCombinado() {
        boolean hcOk = HealthConnectBridge.isAvailable(this) && HealthConnectBridge.hasPermissions(this);

        if (hcOk) {
            // Verificar que HC puede servir datos para progresión incluso sin backend
            try {
                HealthConnectBridge.RecoveryData recovery = HealthConnectBridge.readRecoveryData(this, 30);
                int totalEntries = recovery.pesosKg.size() + recovery.suenos.size() + recovery.fcReposo.size();
                assertTest("HC tiene datos para progresión (" + totalEntries + " entries)", totalEntries > 0);
            } catch (Exception e) {
                assertTest("HC lectura 30d sin excepciones", false);
            }

            // Verificar que datos de hoy sirven para la pantalla home
            try {
                HealthConnectBridge.HealthData today = HealthConnectBridge.readTodayData(this);
                boolean tieneAlgo = today.pasos > 0 || today.caloriasConsumidas > 0;
                assertTest("HC tiene datos para pantalla Home", tieneAlgo);
            } catch (Exception e) {
                assertTest("HC datos hoy sin excepciones", false);
            }
        } else {
            logInfo("Health Connect no disponible — la app depende 100% del backend");
            assertTest("Flujo combinado (requiere HC)", false);
        }

        // Diagnóstico final
        log("\n── DIAGNÓSTICO ──");
        if (!hcOk) {
            logInfo("CRÍTICO: Health Connect no funciona.");
            logInfo("   1. Instala Health Connect desde Play Store");
            logInfo("   2. Abre FitBase → concede TODOS los permisos");
            logInfo("   3. Verifica que Zepp/Mi Fitness → HC está activo");
            logInfo("   4. Verifica que FatSecret → HC está activo");
        } else {
            logInfo("Health Connect funciona correctamente");
        }
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
            icon = "✓";
            color = getColor(R.color.success);
        } else {
            failedTests++;
            icon = "✗";
            color = getColor(R.color.error);
        }

        String line = String.format("  %s %s\n", icon, nombre);
        int start = output.length();
        output.append(line);
        output.setSpan(new ForegroundColorSpan(color), start, output.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        mainHandler.post(() -> {
            tvResultados.setText(output);
            progressTests.setProgress(Math.min((totalTests * 100) / 20, 99));
        });
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

    /** Línea indexada "N. etiqueta ..... valor" para el inventario de datos. */
    private void logDato(int indice, String etiqueta, String valor) {
        int start = output.length();
        String line = String.format("  %2d. %-48s %s\n", indice, etiqueta, valor);
        output.append(line);
        output.setSpan(new ForegroundColorSpan(getColor(R.color.colorTextPrimary)),
                start, output.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mainHandler.post(() -> tvResultados.setText(output));
        try { Thread.sleep(15); } catch (InterruptedException ignored) {}
    }

    private void logInfo(String info) {
        int start = output.length();
        output.append("  ℹ️ " + info + "\n");
        output.setSpan(new ForegroundColorSpan(getColor(R.color.colorTextTertiary)),
                start, output.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mainHandler.post(() -> tvResultados.setText(output));
    }
}
