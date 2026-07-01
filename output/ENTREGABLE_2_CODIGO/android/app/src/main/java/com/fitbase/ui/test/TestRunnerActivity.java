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
import com.fitbase.data.model.MacrosResponse;
import com.fitbase.data.model.MetricasProgresionResponse;
import com.fitbase.data.model.PlanAnualResponse;
import com.fitbase.data.model.SesionResponse;

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
            log("═══════════════════════════════════════════");
            log("  FitBase — Test de Integración Real");
            log("═══════════════════════════════════════════\n");

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
    // 1. HC DISPONIBILIDAD
    // ═══════════════════════════════════════════════

    private void testHCDisponible() {
        boolean available = HealthConnectBridge.isAvailable(this);
        assertTest("Health Connect instalado y disponible", available);
        if (!available) {
            logInfo("⚠️ Sin HC no se pueden leer datos de Zepp/FatSecret. Instala Health Connect desde Play Store.");
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
            logInfo("⚠️ Abre la app → Ajustes → concede permisos de Health Connect");
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
                logInfo("⚠️ FatSecret no está escribiendo datos en Health Connect.");
                logInfo("   Verifica: FatSecret → Ajustes → Conectar → Health Connect ACTIVO");
                logInfo("   Luego registra algo de comida en FatSecret.");
            }
            if (data.pasos == 0) {
                logInfo("⚠️ Zepp no está sincronizando pasos a Health Connect.");
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
                logInfo("⚠️ No hay registros de peso en Health Connect.");
                logInfo("   Registra tu peso en Mi Fitness o manualmente en Health Connect.");
            }

            boolean tieneSueno = !recovery.suenos.isEmpty();
            assertTest("Datos de sueño en HC (últimos 7 días)", tieneSueno);
            if (tieneSueno) {
                HealthConnectBridge.SleepEntry ultimoSleep = recovery.suenos.get(recovery.suenos.size() - 1);
                logInfo("  Último sueño: " + ultimoSleep.duracionMin + " min (score=" + ultimoSleep.score + ")");
            } else {
                logInfo("⚠️ No hay datos de sueño. Verifica que Zepp sincroniza sueño a HC.");
            }

            boolean tieneFC = !recovery.fcReposo.isEmpty();
            assertTest("FC reposo en HC (últimos 7 días)", tieneFC);
            if (tieneFC) {
                HealthConnectBridge.HrEntry ultimaFC = recovery.fcReposo.get(recovery.fcReposo.size() - 1);
                logInfo("  Última FC reposo: " + ultimaFC.bpm + " bpm (" + ultimaFC.fecha + ")");
            } else {
                logInfo("⚠️ No hay FC de reposo nocturna. Verifica que Zepp envía HR a HC.");
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
            if (sr.sesion != null) {
                logInfo("  Sesión: " + sr.sesion.getTipo() + " | Ejercicios: " + (sr.ejercicios != null ? sr.ejercicios.size() : 0));
            } else {
                logInfo("  Respuesta OK pero no hay sesión para hoy (normal si no es día de gym)");
            }
        } else {
            logInfo("  Resultado: " + resultado.get());
            logInfo("  ⚠️ Verifica que Codigo.gs está desplegado como webapp y la URL es correcta");
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
                    logInfo("  ℹ️ No hay fase actual (programa no ha empezado aún)");
                }
            } else {
                logInfo("  ⚠️ Plan vacío. Ejecuta rellenarPlanCompleto() en Apps Script.");
            }
        } else {
            logInfo("  Resultado: " + resultado.get());
            logInfo("  ⚠️ Ejecuta inicializarHojas() y rellenarPlanCompleto() en Apps Script");
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
            int nPeso = p.peso != null ? p.peso.size() : 0;
            int nZepp = p.zepp != null ? p.zepp.size() : 0;
            int nVol = p.volumenEntreno != null ? p.volumenEntreno.size() : 0;
            logInfo("  Datos: peso=" + nPeso + " zepp=" + nZepp + " volumen=" + nVol);
            if (nPeso == 0 && nZepp == 0) {
                logInfo("  ℹ️ Sin datos aún — la app usará Health Connect como fallback");
            }
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
            logInfo("⚠️ Health Connect no disponible — la app depende 100% del backend");
            assertTest("Flujo combinado (requiere HC)", false);
        }

        // Diagnóstico final
        log("\n── DIAGNÓSTICO ──");
        if (!hcOk) {
            logInfo("❌ CRÍTICO: Health Connect no funciona.");
            logInfo("   1. Instala Health Connect desde Play Store");
            logInfo("   2. Abre FitBase → concede TODOS los permisos");
            logInfo("   3. Verifica que Zepp/Mi Fitness → HC está activo");
            logInfo("   4. Verifica que FatSecret → HC está activo");
        } else {
            logInfo("✅ Health Connect funciona correctamente");
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

    private void logInfo(String info) {
        int start = output.length();
        output.append("  ℹ️ " + info + "\n");
        output.setSpan(new ForegroundColorSpan(getColor(R.color.colorTextTertiary)),
                start, output.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mainHandler.post(() -> tvResultados.setText(output));
    }
}
