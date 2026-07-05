package com.fitbase.ui.workout;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fitbase.data.api.ApiClient;
import com.fitbase.data.local.SyncManager;
import com.fitbase.data.model.Ejercicio;
import com.fitbase.data.model.GenericResponse;
import com.fitbase.data.model.ResumenSesionResponse;
import com.fitbase.data.model.Sesion;
import com.fitbase.data.model.SesionResponse;
import com.fitbase.util.Constants;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel para sesión de gym.
 *
 * FASES DEL WORKOUT (orden estricto):
 *   1. CALENTAMIENTO: Movilidad dinámica + activación + series aproximación
 *   2. EJERCICIOS: Serie a serie con registro RIR → timer entre series
 *   3. ESTIRAMIENTOS: Estáticos 30-60s por grupo trabajado
 *   4. CARDIO: Solo si fase DEF/MNT (15-20 min bici)
 *   5. RESUMEN: Series totales, volumen, RIR medio, impacto en plan
 */
public class WorkoutViewModel extends AndroidViewModel {

    // ─── Fases del workout ───
    public enum FaseWorkout {
        CARGANDO,
        CALENTAMIENTO,
        EJERCICIOS,
        ESTIRAMIENTOS,
        CARDIO,
        RESUMEN,
        ERROR
    }

    private final MutableLiveData<FaseWorkout> faseWorkout = new MutableLiveData<>(FaseWorkout.CARGANDO);
    private final MutableLiveData<SesionResponse> sesionData = new MutableLiveData<>();
    private final MutableLiveData<Integer> ejercicioActualIdx = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> serieActual = new MutableLiveData<>(1);
    private final MutableLiveData<Boolean> timerActivo = new MutableLiveData<>(false);
    // Dentro de la fase EJERCICIOS: false = pantalla de ejercicio (swipe para
    // completar), true = pantalla de registro RPE (ui.md §4.2/§4.3) — pantallas
    // completas y excluyentes, no las dos mitades de antes.
    private final MutableLiveData<Boolean> mostrandoRegistro = new MutableLiveData<>(false);
    // Índice del item actual dentro de CALENTAMIENTO o ESTIRAMIENTOS (mismo
    // patrón minimalista que EJERCICIOS: un item a la vez, swipe para avanzar).
    private final MutableLiveData<Integer> rutinaIdx = new MutableLiveData<>(0);
    private final MutableLiveData<ResumenSesionResponse.Resumen> resumen = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private String sesionId;
    private long tiempoInicioMs;

    // Progreso "dónde estaba" (fase/ejercicio/serie/rutina), persistido en
    // SharedPreferences en cada avance — si el móvil se apaga de golpe, se
    // cierra la app a medias, o el proceso muere por batería/memoria, al
    // reabrir se retoma exactamente donde se dejó en vez de reiniciar desde
    // CALENTAMIENTO. Se identifica por sesionId (ya incluye la fecha, tanto
    // en sesión real como demo), así que nunca se resume una sesión de otro
    // día por error.
    private static final String PREFS_PROGRESO = "fitbase_workout_progreso";

    // Acumulado LOCAL de la sesión en curso (se va sumando serie a serie en
    // registrarSerie) — respaldo SOLO si falla la red (el resumen "de
    // verdad" siempre viene de completar_sesion/BBDD, incluida la sesión
    // demo, que también se guarda ahora en ejercicios_log).
    private int demoSeriesTotales = 0;
    private float demoVolumenTotalKg = 0f;
    private int demoRirSum = 0;

    public WorkoutViewModel(@NonNull Application application) {
        super(application);
    }

    // ─── LiveData getters ─────────────────────────────────

    public LiveData<FaseWorkout> getFaseWorkout() { return faseWorkout; }
    public LiveData<SesionResponse> getSesionData() { return sesionData; }
    public LiveData<Integer> getEjercicioActualIdx() { return ejercicioActualIdx; }
    public LiveData<Integer> getSerieActual() { return serieActual; }
    public LiveData<Boolean> getTimerActivo() { return timerActivo; }
    public LiveData<Boolean> getMostrandoRegistro() { return mostrandoRegistro; }

    /**
     * Solo para retomar un descanso en curso tras cierre/apagón (ver
     * WorkoutActivity.reanudarTimerSiAplica) — la Activity ya se encarga de
     * mostrar el timer con el tiempo restante correcto; esto solo mantiene
     * consistente el estado interno del ViewModel.
     */
    public void reanudarTimerActivo() { timerActivo.postValue(true); }

    /** Swipe izquierda en pantalla de ejercicio → mostrar registro RPE. */
    public void mostrarRegistroRpe() { mostrandoRegistro.postValue(true); }

    /** Swipe derecha en registro RPE → cancelar, volver a Ejercicio sin registrar nada. */
    public void cancelarRegistroRpe() { mostrandoRegistro.postValue(false); }

    /** Swipe izquierda en el timer → saltar el resto del descanso. */
    public void saltarDescanso() { timerActivo.postValue(false); }
    public LiveData<ResumenSesionResponse.Resumen> getResumen() { return resumen; }
    public LiveData<String> getError() { return error; }
    public LiveData<Integer> getRutinaIdx() { return rutinaIdx; }

    /**
     * Rutina activa (calentamiento o estiramientos) según la fase actual —
     * viene del backend (getCalentamiento_/getEstiramientos_ en Codigo.gs),
     * nunca hardcodeada aquí.
     */
    private SesionResponse.RutinaInfo getRutinaActual() {
        SesionResponse data = sesionData.getValue();
        FaseWorkout fase = faseWorkout.getValue();
        if (data == null || fase == null) return null;
        if (fase == FaseWorkout.CALENTAMIENTO) return data.getCalentamiento();
        if (fase == FaseWorkout.ESTIRAMIENTOS) return data.getEstiramientos();
        return null;
    }

    public SesionResponse.ItemRutina getItemRutinaActual() {
        SesionResponse.RutinaInfo rutina = getRutinaActual();
        Integer idx = rutinaIdx.getValue();
        if (rutina == null || rutina.ejercicios == null || idx == null || idx >= rutina.ejercicios.size()) {
            return null;
        }
        return rutina.ejercicios.get(idx);
    }

    public int getTotalItemsRutina() {
        SesionResponse.RutinaInfo rutina = getRutinaActual();
        return (rutina != null && rutina.ejercicios != null) ? rutina.ejercicios.size() : 0;
    }

    /** Swipe izquierda en calentamiento/estiramientos → siguiente item (o siguiente fase). */
    public void siguienteItemRutina() {
        int total = getTotalItemsRutina();
        Integer idx = rutinaIdx.getValue();
        int nuevo = (idx != null ? idx : 0) + 1;
        if (nuevo >= total) {
            FaseWorkout fase = faseWorkout.getValue();
            if (fase == FaseWorkout.CALENTAMIENTO) {
                iniciarEjercicios();
            } else if (fase == FaseWorkout.ESTIRAMIENTOS) {
                iniciarCardioOResumen();
            }
        } else {
            rutinaIdx.postValue(nuevo);
            FaseWorkout fase = faseWorkout.getValue();
            if (fase != null) guardarProgresoLocal(fase, 0, 1, nuevo);
        }
    }

    /** Swipe derecha en calentamiento/estiramientos → item anterior (no hace nada si ya es el primero). */
    public void itemAnteriorRutina() {
        Integer idx = rutinaIdx.getValue();
        if (idx != null && idx > 0) {
            rutinaIdx.postValue(idx - 1);
            FaseWorkout fase = faseWorkout.getValue();
            if (fase != null) guardarProgresoLocal(fase, 0, 1, idx - 1);
        }
    }

    // ─── Cargar sesión desde backend ──────────────────────

    public void cargarSesion() {
        faseWorkout.postValue(FaseWorkout.CARGANDO);
        rutinaIdx.postValue(0);
        demoSeriesTotales = 0;
        demoVolumenTotalKg = 0f;
        demoRirSum = 0;
        tiempoInicioMs = System.currentTimeMillis();

        ApiClient.getApi().getSesionHoy("sesion_hoy").enqueue(new Callback<SesionResponse>() {
            @Override
            public void onResponse(Call<SesionResponse> call, Response<SesionResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getSesion() != null) {
                    SesionResponse data = response.body();
                    sesionData.postValue(data);
                    sesionId = data.getSesion().getSesionId();
                    reanudarOEmpezar();
                } else if (estaEnModoDemo()) {
                    // Antes de Constants.FECHA_INICIO_PROGRAMA no hay sesiones reales
                    // en sesiones_plan (el plan de 11 meses empieza ese día) — se
                    // muestra una sesión de ejemplo para poder probar toda la pantalla
                    // (PROMPT_FITBASE.md §4 "MODO DEMO": todas las pantallas navegables).
                    SesionResponse demo = crearSesionDemo();
                    sesionData.postValue(demo);
                    sesionId = demo.getSesion().getSesionId();
                    reanudarOEmpezar();
                } else {
                    error.postValue("No hay sesión para hoy");
                    faseWorkout.postValue(FaseWorkout.ERROR);
                }
            }

            @Override
            public void onFailure(Call<SesionResponse> call, Throwable t) {
                if (estaEnModoDemo()) {
                    SesionResponse demo = crearSesionDemo();
                    sesionData.postValue(demo);
                    sesionId = demo.getSesion().getSesionId();
                    reanudarOEmpezar();
                } else {
                    error.postValue("Sin conexión: " + t.getMessage());
                    faseWorkout.postValue(FaseWorkout.ERROR);
                }
            }
        });
    }

    /**
     * Si hay progreso guardado de ESTA MISMA sesión (mismo sesionId — ya
     * incluye la fecha), retoma exactamente donde se dejó (fase, ejercicio,
     * serie, item de rutina) en vez de reiniciar desde CALENTAMIENTO. Cubre
     * cierre de la app a medias, apagón del móvil, o que el sistema mate el
     * proceso por batería/memoria mientras se entrenaba.
     */
    private void reanudarOEmpezar() {
        android.content.SharedPreferences prefs = getApplication()
                .getSharedPreferences(PREFS_PROGRESO, android.content.Context.MODE_PRIVATE);
        String sesionGuardada = prefs.getString("sesion_id", null);
        if (sesionGuardada != null && sesionGuardada.equals(sesionId)) {
            try {
                FaseWorkout faseGuardada = FaseWorkout.valueOf(
                        prefs.getString("fase", FaseWorkout.CALENTAMIENTO.name()));
                rutinaIdx.postValue(prefs.getInt("rutina_idx", 0));
                ejercicioActualIdx.postValue(prefs.getInt("ejercicio_idx", 0));
                serieActual.postValue(prefs.getInt("serie", 1));
                faseWorkout.postValue(faseGuardada);
                return;
            } catch (IllegalArgumentException ignored) {
                // Fase guardada no reconocida (versión antigua de la app) — empezar de cero.
            }
        }
        faseWorkout.postValue(FaseWorkout.CALENTAMIENTO);
    }

    private void guardarProgresoLocal(FaseWorkout fase, int idxEjercicio, int serie, int idxRutina) {
        if (sesionId == null) return;
        getApplication().getSharedPreferences(PREFS_PROGRESO, android.content.Context.MODE_PRIVATE)
                .edit()
                .putString("sesion_id", sesionId)
                .putString("fase", fase.name())
                .putInt("ejercicio_idx", idxEjercicio)
                .putInt("serie", serie)
                .putInt("rutina_idx", idxRutina)
                .apply();
    }

    /** La sesión terminó (o está terminando) — no hay nada que retomar. */
    private void borrarProgresoLocal() {
        getApplication().getSharedPreferences(PREFS_PROGRESO, android.content.Context.MODE_PRIVATE)
                .edit().clear().apply();
    }

    private SimpleDateFormat formatoFecha() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    }

    private boolean estaEnModoDemo() {
        try {
            Date inicio = formatoFecha().parse(Constants.FECHA_INICIO_PROGRAMA);
            return new Date().before(inicio);
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Sesión de ejemplo para modo demo (antes de que empiece el plan real).
     * Pesos y series son solo ilustrativos — "(demo)" en motor_detalle deja
     * claro que no vienen del motor de 6 capas real. El id incluye la fecha
     * (DEMO_20260705, no "DEMO" a secas) para que cada día de pruebas quede
     * aislado en ejercicios_log — si no, el resumen de hoy sumaría también
     * las series de sesiones demo de días anteriores.
     */
    private SesionResponse crearSesionDemo() {
        Sesion sesion = new Sesion();
        sesion.setSesionId("DEMO_" + formatoFecha().format(new Date()).replace("-", ""));
        sesion.setTipo("Push");
        sesion.setDuracionEstimadaMin(60);

        List<Ejercicio> ejercicios = new ArrayList<>();
        ejercicios.add(crearEjercicioDemo("EJE_PRESS_HOMB", "Press hombro mancuernas", 4, "8-10", 3, 150, 20f));
        ejercicios.add(crearEjercicioDemo("EJE_LAT_SENT", "Elev. laterales sentado", 4, "12-15", 3, 90, 10f));
        ejercicios.add(crearEjercicioDemo("EJE_PRESS_INC", "Press inclinado mancuernas", 4, "8-10", 3, 150, 22.5f));

        SesionResponse demo = new SesionResponse();
        demo.setSesion(sesion);
        demo.setEjercicios(ejercicios);
        demo.setMensaje("Sesión de ejemplo — el plan real empieza el " + Constants.FECHA_INICIO_PROGRAMA);
        demo.setCalentamiento(crearRutinaDemo(
                "Círculos de cadera", "10/lado",
                "Gato-vaca", "10 reps",
                "Face pulls ligeros", "15 reps"));
        demo.setEstiramientos(crearRutinaDemo(
                "Pectoral en marco de puerta", "30s/lado",
                "Estiramiento deltoides (brazo cruzado)", "30s/lado",
                "Extensión tríceps overhead", "30s/brazo"));
        return demo;
    }

    /** Rutina de calentamiento/estiramientos de ejemplo (mismo shape que el backend real). */
    private SesionResponse.RutinaInfo crearRutinaDemo(String n1, String r1, String n2, String r2, String n3, String r3) {
        SesionResponse.RutinaInfo rutina = new SesionResponse.RutinaInfo();
        rutina.duracionMin = 6;
        rutina.ejercicios = new ArrayList<>();
        rutina.ejercicios.add(itemRutina(n1, r1));
        rutina.ejercicios.add(itemRutina(n2, r2));
        rutina.ejercicios.add(itemRutina(n3, r3));
        return rutina;
    }

    private SesionResponse.ItemRutina itemRutina(String nombre, String reps) {
        SesionResponse.ItemRutina item = new SesionResponse.ItemRutina();
        item.nombre = nombre;
        item.reps = reps;
        return item;
    }

    private Ejercicio crearEjercicioDemo(String id, String nombre, int series, String reps,
                                          int rir, int descansoSeg, float pesoSugerido) {
        Ejercicio ej = new Ejercicio();
        ej.setEjercicioId(id);
        ej.setNombre(nombre);
        ej.setSeriesPlan(series);
        ej.setRepsPlan(reps);
        ej.setRirObjetivo(rir);
        ej.setDescansoSeg(descansoSeg);
        ej.setPesoSugerido(pesoSugerido);
        ej.setMotorDetalle("(demo) — el plan real empieza el " + Constants.FECHA_INICIO_PROGRAMA);
        return ej;
    }

    // ─── Transiciones de fase ─────────────────────────────

    /** Calentamiento completado → pasar a ejercicios. */
    public void iniciarEjercicios() {
        ejercicioActualIdx.postValue(0);
        serieActual.postValue(1);
        faseWorkout.postValue(FaseWorkout.EJERCICIOS);
        guardarProgresoLocal(FaseWorkout.EJERCICIOS, 0, 1, 0);
    }

    /** Tras última serie del último ejercicio → estiramientos. */
    public void iniciarEstiramientos() {
        rutinaIdx.postValue(0);
        faseWorkout.postValue(FaseWorkout.ESTIRAMIENTOS);
        guardarProgresoLocal(FaseWorkout.ESTIRAMIENTOS, 0, 0, 0);
    }

    /** Estiramientos completados → cardio si aplica, sino resumen.
     * DECISIÓN BASADA EN EVIDENCIA (programacion.md §13, Wilson 2012, Viana 2019):
     *   - VOL/FZA/DELOAD: SIN cardio post-gym (minimizar interferencia)
     *   - DEF: 15-20 min bici obligatorio (aumentar déficit calórico)
     *   - MNT: 10 min bici (mantener capacidad aeróbica)
     * La app decide automáticamente según la fase — NO es elección del usuario.
     */
    public void iniciarCardioOResumen() {
        SesionResponse data = sesionData.getValue();
        if (data != null && data.getSesion() != null) {
            String fase = data.getSesion().getFase();
            // Wilson 2012 + Viana 2019: cardio SOLO en DEF y MNT
            if ("DEF".equals(fase) || "MNT".equals(fase)) {
                faseWorkout.postValue(FaseWorkout.CARDIO);
                guardarProgresoLocal(FaseWorkout.CARDIO, 0, 0, 0);
                return;
            }
        }
        // VOL, FZA, DELOAD o sin datos → directo a resumen (Wilson 2012: no interferir)
        completarSesionYMostrarResumen();
    }

    /** Cardio completado (o skipped) → resumen. */
    public void finalizarSesion() {
        completarSesionYMostrarResumen();
    }

    // ─── Registrar serie ──────────────────────────────────

    /**
     * Registra una serie en el backend y avanza el estado.
     * POST guardar_log → O(1) append en ejercicios_log.
     */
    public void registrarSerie(int repsCompletadas, int rirPercibido, String sensacion) {
        List<Ejercicio> ejercicios = getEjerciciosActuales();
        if (ejercicios == null) return;

        Integer idx = ejercicioActualIdx.getValue();
        Integer serie = serieActual.getValue();
        if (idx == null || serie == null) return;

        Ejercicio ej = ejercicios.get(idx);

        // Volver a la pantalla de ejercicio (la próxima serie/ejercicio/timer
        // siempre empieza mostrando el ejercicio, no el registro).
        mostrandoRegistro.postValue(false);

        // Acumulado local — respaldo para el resumen si falla la red (ver
        // construirResumenDemo, usado también como fallback offline).
        demoSeriesTotales++;
        demoVolumenTotalKg += ej.getPesoSugerido() * repsCompletadas;
        demoRirSum += rirPercibido;

        // La sesión demo (pre-temporada, sesionId="DEMO_yyyyMMdd") también se
        // guarda de verdad en ejercicios_log — el usuario ya tiene limpiarDatosTest() en
        // Codigo.gs (§8 LIMPIAR) para resetear ejercicios_log/metricas/sesiones
        // completadas después de probar, así que no hace falta bloquearlo aquí.
        Map<String, Object> datos = new HashMap<>();
        datos.put("accion", "guardar_log");
        datos.put("plan_id", ej.getPlanId());
        datos.put("sesion_id", sesionId);
        datos.put("ejercicio_id", ej.getEjercicioId());
        datos.put("num_serie", serie);
        datos.put("num_peso_usado_kg", ej.getPesoSugerido());
        datos.put("num_reps_completadas", repsCompletadas);
        datos.put("num_rir_percibido", rirPercibido);
        datos.put("str_sensacion", sensacion);

        ApiClient.getApi().guardarLog(datos).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (!response.isSuccessful()) {
                    // Backend respondió pero con error (p.ej. 5xx) → encolar igual que offline
                    SyncManager.encolar(getApplication(), datos);
                }
            }
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                // Sin red → encolar en Room; SyncManager reintenta al recuperar
                // conexión o al reabrir la app (nunca se pierde la serie).
                SyncManager.encolar(getApplication(), datos);
            }
        });

        // Avanzar estado local
        ej.setSerieCompletada(serie);

        if (serie >= ej.getSeriesPlan()) {
            // Última serie → siguiente ejercicio o fin
            avanzarAlSiguienteEjercicio();
        } else {
            // Más series → activar timer
            serieActual.postValue(serie + 1);
            timerActivo.postValue(true);
            guardarProgresoLocal(FaseWorkout.EJERCICIOS, idx, serie + 1, 0);
        }
    }

    /** Timer terminó → mostrar ejercicio para siguiente serie. */
    public void timerCompletado() {
        timerActivo.postValue(false);
    }

    private void avanzarAlSiguienteEjercicio() {
        List<Ejercicio> ejercicios = getEjerciciosActuales();
        Integer idx = ejercicioActualIdx.getValue();
        if (ejercicios == null || idx == null) return;

        if (idx + 1 < ejercicios.size()) {
            Ejercicio actual = ejercicios.get(idx);
            Ejercicio siguiente = ejercicios.get(idx + 1);
            // Superserie (preferencias.md §5): mismo grupo → directo al
            // siguiente ejercicio sin descanso, para no "aburrirse esperando".
            boolean esSuperserie = actual.getSupersetGrupo() != null && !actual.getSupersetGrupo().isEmpty()
                    && actual.getSupersetGrupo().equals(siguiente.getSupersetGrupo());
            ejercicioActualIdx.postValue(idx + 1);
            serieActual.postValue(1);
            timerActivo.postValue(!esSuperserie);
            guardarProgresoLocal(FaseWorkout.EJERCICIOS, idx + 1, 1, 0);
        } else {
            // Último ejercicio completado → estiramientos
            iniciarEstiramientos();
        }
    }

    // ─── Resumen y guardado de sesión ──────────────────────

    /**
     * Al llegar al resumen, se guarda AUTOMÁTICAMENTE (completar_sesion marca
     * bool_completada + date_fin) — a partir de ahí el motor de 6 capas usa
     * estos datos para ajustar la siguiente sesión de este tipo. El backend
     * devuelve también el resumen (series/volumen/RIR medio) en la misma
     * respuesta, así que no hace falta ninguna acción extra del usuario.
     */
    private void completarSesionYMostrarResumen() {
        borrarProgresoLocal(); // se está terminando — ya no hay nada que retomar
        if (sesionId == null) {
            resumen.postValue(construirResumenDemo());
            faseWorkout.postValue(FaseWorkout.RESUMEN);
            return;
        }

        Map<String, Object> datos = new HashMap<>();
        datos.put("accion", "completar_sesion");
        datos.put("sesion_id", sesionId);

        ApiClient.getApi().completarSesion(datos).enqueue(new Callback<ResumenSesionResponse>() {
            @Override
            public void onResponse(Call<ResumenSesionResponse> call, Response<ResumenSesionResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResumen() != null) {
                    ResumenSesionResponse.Resumen r = response.body().getResumen();
                    resumen.postValue(r);
                    guardarResumenDemoSiAplica(r);
                } else {
                    // Backend respondió pero sin resumen usable (p.ej. 5xx) →
                    // encolar para que bool_completada/date_fin se marquen en
                    // cuanto haya red, y mostrar el acumulado local mientras tanto.
                    SyncManager.encolar(getApplication(), datos);
                    ResumenSesionResponse.Resumen r = construirResumenDemo();
                    resumen.postValue(r);
                    guardarResumenDemoSiAplica(r);
                }
                faseWorkout.postValue(FaseWorkout.RESUMEN);
            }

            @Override
            public void onFailure(Call<ResumenSesionResponse> call, Throwable t) {
                // Sin red → encolar (se completará la sesión en el backend en
                // cuanto haya conexión). Mientras tanto, mostramos el acumulado
                // local para no dejar la pantalla en blanco.
                SyncManager.encolar(getApplication(), datos);
                ResumenSesionResponse.Resumen r = construirResumenDemo();
                resumen.postValue(r);
                guardarResumenDemoSiAplica(r);
                faseWorkout.postValue(FaseWorkout.RESUMEN);
            }
        });
    }

    /**
     * Si la sesión era la demo de pre-temporada (sesionId="DEMO_yyyyMMdd"),
     * recuerda en SharedPreferences que HOY ya se completó, con el resumen —
     * así HomeActivity puede llevar directo al resumen la próxima vez que se
     * pulse el botón de previsualización de entreno, igual que ya hace con
     * una sesión real ya completada (ver HomeActivity.actualizarVistaMañana).
     */
    private void guardarResumenDemoSiAplica(ResumenSesionResponse.Resumen r) {
        if (sesionId == null || !sesionId.startsWith("DEMO_") || r == null) return;
        getApplication().getSharedPreferences("fitbase_demo", android.content.Context.MODE_PRIVATE)
                .edit()
                .putString("fecha_completada", formatoFecha().format(new Date()))
                .putInt("series_totales", r.seriesTotales)
                .putInt("volumen_total_kg", r.volumenTotalKg)
                .putFloat("rir_medio", r.rirMedio)
                .putString("intensidad_percibida", r.intensidadPercibida)
                .putString("impacto", r.impacto)
                .apply();
    }

    /**
     * Resumen a partir del acumulado LOCAL (demo, u offline como respaldo).
     * Misma clasificación de intensidad que usa el backend (Helms 2016).
     */
    private ResumenSesionResponse.Resumen construirResumenDemo() {
        ResumenSesionResponse.Resumen r = new ResumenSesionResponse.Resumen();
        if (demoSeriesTotales == 0) {
            r.mensaje = "Sesión sin series registradas";
            return r;
        }
        r.seriesTotales = demoSeriesTotales;
        r.volumenTotalKg = Math.round(demoVolumenTotalKg);
        r.rirMedio = Math.round((demoRirSum / (float) demoSeriesTotales) * 10) / 10f;

        if (r.rirMedio <= 1) {
            r.intensidadPercibida = "Muy alta — cerca del fallo (RPE 9-10, Helms 2016)";
        } else if (r.rirMedio <= 2.5) {
            r.intensidadPercibida = "Alta — zona óptima hipertrofia (RPE 7-8, Helms 2016)";
        } else if (r.rirMedio <= 3.5) {
            r.intensidadPercibida = "Moderada — margen de progresión (RPE 6-7)";
        } else {
            r.intensidadPercibida = "Conservadora — podrías aumentar carga (RPE <6)";
        }
        r.impacto = "El motor usará estos datos para ajustar tu próxima sesión de este tipo.";
        return r;
    }

    // ─── Helpers ──────────────────────────────────────────

    public List<Ejercicio> getEjerciciosActuales() {
        SesionResponse data = sesionData.getValue();
        return data != null ? data.getEjercicios() : null;
    }

    public Ejercicio getEjercicioActual() {
        List<Ejercicio> ejs = getEjerciciosActuales();
        Integer idx = ejercicioActualIdx.getValue();
        if (ejs == null || idx == null || idx >= ejs.size()) return null;
        return ejs.get(idx);
    }

    public int getTotalEjercicios() {
        List<Ejercicio> ejs = getEjerciciosActuales();
        return ejs != null ? ejs.size() : 0;
    }

    public long getTiempoTranscurridoMs() {
        return System.currentTimeMillis() - tiempoInicioMs;
    }

    public String getSesionId() { return sesionId; }
}
