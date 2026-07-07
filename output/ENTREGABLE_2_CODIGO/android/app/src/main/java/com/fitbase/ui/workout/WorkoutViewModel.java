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
import com.fitbase.data.model.SesionResponse;

import java.util.HashMap;
import java.util.List;
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
    // CALENTAMIENTO. Se identifica por sesionId (ya incluye la fecha), así
    // que nunca se resume una sesión de otro día por error.
    private static final String PREFS_PROGRESO = "fitbase_workout_progreso";

    // Acumulado LOCAL de la sesión en curso (se va sumando serie a serie en
    // registrarSerie) — respaldo SOLO si falla la red (el resumen "de
    // verdad" siempre viene de completar_sesion/BBDD).
    private int acumSeriesTotales = 0;
    private float acumVolumenTotalKg = 0f;
    private int acumRirSum = 0;

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
        acumSeriesTotales = 0;
        acumVolumenTotalKg = 0f;
        acumRirSum = 0;
        tiempoInicioMs = System.currentTimeMillis();

        ApiClient.getApi().getSesionHoy("sesion_hoy").enqueue(new Callback<SesionResponse>() {
            @Override
            public void onResponse(Call<SesionResponse> call, Response<SesionResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getSesion() != null) {
                    SesionResponse data = response.body();
                    sesionData.postValue(data);
                    sesionId = data.getSesion().getSesionId();
                    reanudarOEmpezar();
                } else {
                    error.postValue("No hay sesión para hoy");
                    faseWorkout.postValue(FaseWorkout.ERROR);
                }
            }

            @Override
            public void onFailure(Call<SesionResponse> call, Throwable t) {
                error.postValue("Sin conexión: " + t.getMessage());
                faseWorkout.postValue(FaseWorkout.ERROR);
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
    public void registrarSerie(int repsCompletadas, int rirPercibido) {
        List<Ejercicio> ejercicios = getEjerciciosActuales();
        if (ejercicios == null) return;

        Integer idx = ejercicioActualIdx.getValue();
        Integer serie = serieActual.getValue();
        if (idx == null || serie == null) return;

        Ejercicio ej = ejercicios.get(idx);

        // Volver a la pantalla de ejercicio (la próxima serie/ejercicio/timer
        // siempre empieza mostrando el ejercicio, no el registro).
        mostrandoRegistro.postValue(false);

        // Acumulado local — respaldo para el resumen si falla la red
        // (ver construirResumenLocal).
        acumSeriesTotales++;
        acumVolumenTotalKg += ej.getPesoActual() * repsCompletadas;
        acumRirSum += rirPercibido;

        Map<String, Object> datos = new HashMap<>();
        datos.put("accion", "guardar_log");
        datos.put("plan_id", ej.getPlanId());
        datos.put("sesion_id", sesionId);
        datos.put("ejercicio_id", ej.getEjercicioId());
        datos.put("num_serie", serie);
        // Peso REAL (ajustado por el usuario, ver Ejercicio.pesoActual) — no
        // el sugerido a secas, si no el motor nunca se entera de lo que de
        // verdad se levantó y la autorregulación de la próxima sesión queda
        // ciega.
        datos.put("num_peso_usado_kg", ej.getPesoActual());
        datos.put("num_reps_completadas", repsCompletadas);
        // Solo peso, reps y RIR — str_sensacion se retiró (limpieza 2026): los
        // botones Fácil/Bien/Duro/Fallo ya fijan el RIR (3/2/1/0), la sensación
        // era el mismo dato. El motor de cargas usa el RIR.
        datos.put("num_rir_percibido", rirPercibido);

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
            resumen.postValue(construirResumenLocal());
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
                } else {
                    // Backend respondió pero sin resumen usable (p.ej. 5xx) →
                    // encolar para que bool_completada/date_fin se marquen en
                    // cuanto haya red, y mostrar el acumulado local mientras tanto.
                    SyncManager.encolar(getApplication(), datos);
                    resumen.postValue(construirResumenLocal());
                }
                faseWorkout.postValue(FaseWorkout.RESUMEN);
            }

            @Override
            public void onFailure(Call<ResumenSesionResponse> call, Throwable t) {
                // Sin red → encolar (se completará la sesión en el backend en
                // cuanto haya conexión). Mientras tanto, mostramos el acumulado
                // local para no dejar la pantalla en blanco.
                SyncManager.encolar(getApplication(), datos);
                resumen.postValue(construirResumenLocal());
                faseWorkout.postValue(FaseWorkout.RESUMEN);
            }
        });
    }

    /**
     * Resumen a partir del acumulado LOCAL — respaldo offline (ver campos
     * acumSeriesTotales/acumVolumenTotalKg/acumRirSum en registrarSerie).
     * Misma clasificación de intensidad que usa el backend (Helms 2016).
     */
    private ResumenSesionResponse.Resumen construirResumenLocal() {
        ResumenSesionResponse.Resumen r = new ResumenSesionResponse.Resumen();
        if (acumSeriesTotales == 0) {
            r.mensaje = "Sesión sin series registradas";
            return r;
        }
        r.seriesTotales = acumSeriesTotales;
        r.volumenTotalKg = Math.round(acumVolumenTotalKg);
        r.rirMedio = Math.round((acumRirSum / (float) acumSeriesTotales) * 10) / 10f;

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
