package com.fitbase.ui.workout;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fitbase.data.api.ApiClient;
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
    private final MutableLiveData<ResumenSesionResponse.Resumen> resumen = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private String sesionId;
    private long tiempoInicioMs;

    public WorkoutViewModel(@NonNull Application application) {
        super(application);
    }

    // ─── LiveData getters ─────────────────────────────────

    public LiveData<FaseWorkout> getFaseWorkout() { return faseWorkout; }
    public LiveData<SesionResponse> getSesionData() { return sesionData; }
    public LiveData<Integer> getEjercicioActualIdx() { return ejercicioActualIdx; }
    public LiveData<Integer> getSerieActual() { return serieActual; }
    public LiveData<Boolean> getTimerActivo() { return timerActivo; }
    public LiveData<ResumenSesionResponse.Resumen> getResumen() { return resumen; }
    public LiveData<String> getError() { return error; }

    // ─── Cargar sesión desde backend ──────────────────────

    public void cargarSesion() {
        faseWorkout.postValue(FaseWorkout.CARGANDO);
        tiempoInicioMs = System.currentTimeMillis();

        ApiClient.getApi().getSesionHoy("sesion_hoy").enqueue(new Callback<SesionResponse>() {
            @Override
            public void onResponse(Call<SesionResponse> call, Response<SesionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SesionResponse data = response.body();
                    sesionData.postValue(data);

                    if (data.getSesion() != null) {
                        sesionId = data.getSesion().getSesionId();
                        faseWorkout.postValue(FaseWorkout.CALENTAMIENTO);
                    } else {
                        error.postValue("No hay sesión para hoy");
                        faseWorkout.postValue(FaseWorkout.ERROR);
                    }
                } else {
                    error.postValue("Error al cargar sesión");
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

    // ─── Transiciones de fase ─────────────────────────────

    /** Calentamiento completado → pasar a ejercicios. */
    public void iniciarEjercicios() {
        ejercicioActualIdx.postValue(0);
        serieActual.postValue(1);
        faseWorkout.postValue(FaseWorkout.EJERCICIOS);
    }

    /** Tras última serie del último ejercicio → estiramientos. */
    public void iniciarEstiramientos() {
        faseWorkout.postValue(FaseWorkout.ESTIRAMIENTOS);
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
                return;
            }
        }
        // VOL, FZA, DELOAD o sin datos → directo a resumen (Wilson 2012: no interferir)
        completarSesionEnBackend();
    }

    /** Cardio completado (o skipped) → resumen. */
    public void finalizarSesion() {
        completarSesionEnBackend();
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
                // Serie guardada OK
            }
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                // Offline → guardar en Room para sync posterior
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
            ejercicioActualIdx.postValue(idx + 1);
            serieActual.postValue(1);
            timerActivo.postValue(true); // Descanso entre ejercicios
        } else {
            // Último ejercicio completado → estiramientos
            iniciarEstiramientos();
        }
    }

    // ─── Completar sesión ─────────────────────────────────

    private void completarSesionEnBackend() {
        if (sesionId == null) {
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
                    resumen.postValue(response.body().getResumen());
                }
                faseWorkout.postValue(FaseWorkout.RESUMEN);
            }

            @Override
            public void onFailure(Call<ResumenSesionResponse> call, Throwable t) {
                faseWorkout.postValue(FaseWorkout.RESUMEN);
            }
        });
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
