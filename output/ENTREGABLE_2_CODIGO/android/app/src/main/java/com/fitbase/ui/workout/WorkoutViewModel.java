package com.fitbase.ui.workout;

import android.app.Application;
import android.os.CountDownTimer;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fitbase.data.api.ApiClient;
import com.fitbase.data.local.SyncManager;
import com.fitbase.data.model.Ejercicio;
import com.fitbase.data.model.EjercicioLog;
import com.fitbase.data.model.GenericResponse;
import com.fitbase.data.model.SesionResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel para el flujo de entrenamiento.
 * Gestiona: ejercicio actual, series, timer, registro.
 * Usa AndroidViewModel para acceso a contexto (Room offline queue).
 * Referencia: REG-DEV-01 (ui.md) §4, REG-LOG-01 (motor_pesos.md)
 */
public class WorkoutViewModel extends AndroidViewModel {

    private final Application app;
    private List<Ejercicio> ejercicios = new ArrayList<>();
    private int indiceEjercicio = 0;
    private int serieCompletadaActual = 0;
    private String sesionId;
    private long tiempoInicioMs;
    private float volumenTotal = 0; // kg × reps acumulado

    private final MutableLiveData<Ejercicio> ejercicioActual = new MutableLiveData<>();
    private final MutableLiveData<Integer> serieActual = new MutableLiveData<>(1);
    private final MutableLiveData<Integer> timerSegundos = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> sesionCompletada = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> tiempoTotalSegundos = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(true);

    private CountDownTimer timer;
    private CountDownTimer cronometroTotal;

    public WorkoutViewModel(@NonNull Application application) {
        super(application);
        this.app = application;
        // Al crear el ViewModel, intentar sincronizar pendientes
        SyncManager.sincronizar(app);
    }

    public LiveData<Ejercicio> getEjercicioActual() { return ejercicioActual; }
    public LiveData<Integer> getSerieActual() { return serieActual; }
    public LiveData<Integer> getTimerSegundos() { return timerSegundos; }
    public LiveData<Boolean> isSesionCompletada() { return sesionCompletada; }
    public LiveData<Integer> getTiempoTotalSegundos() { return tiempoTotalSegundos; }
    public LiveData<Boolean> isCargando() { return cargando; }
    public int getIndiceEjercicio() { return indiceEjercicio; }
    public int getTotalEjercicios() { return ejercicios.size(); }
    public String getSesionId() { return sesionId; }
    public float getVolumenTotal() { return volumenTotal; }

    public void cargarSesion() {
        ApiClient.getApi().getSesionHoy("sesion_hoy").enqueue(new Callback<SesionResponse>() {
            @Override
            public void onResponse(Call<SesionResponse> call, Response<SesionResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().sesion != null) {
                    sesionId = response.body().sesion.getSesionId();
                    ejercicios = response.body().ejercicios != null
                            ? response.body().ejercicios : new ArrayList<>();
                    iniciarSesion();
                } else {
                    cargarSesionDemo();
                }
            }

            @Override
            public void onFailure(Call<SesionResponse> call, Throwable t) {
                cargarSesionDemo();
            }
        });
    }

    /**
     * Carga ejercicios de demo según día de la semana.
     * Respeta prioridades: P1 Estética V-taper > P2 Postura > P3 Hipertrofia > P4 Flexibilidad.
     */
    private void cargarSesionDemo() {
        sesionId = "DEMO_" + System.currentTimeMillis();
        ejercicios = new ArrayList<>();

        int diaSemana = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK);

        switch (diaSemana) {
            case java.util.Calendar.MONDAY: // PUSH: Hombros > Pecho > Tríceps
                ejercicios.add(crearEjDemo("Press Militar Mancuernas", 4, "8-10", 18f, 180));
                ejercicios.add(crearEjDemo("Elev. Laterales Sentado", 4, "12-15", 10f, 90));
                ejercicios.add(crearEjDemo("Elev. Laterales Polea Media", 3, "12-15", 7f, 90));
                ejercicios.add(crearEjDemo("Press Inclinado Mancuernas", 4, "8-10", 22f, 150));
                ejercicios.add(crearEjDemo("Cruces Polea Alta", 3, "10-12", 15f, 90));
                ejercicios.add(crearEjDemo("Press Francés 30° Barra Z", 3, "10-12", 18f, 120));
                ejercicios.add(crearEjDemo("Extensión Unilateral Polea", 3, "12-15", 8f, 60));
                ejercicios.add(crearEjDemo("Face Pulls (retracción)", 3, "15-20", 12f, 60));
                break;

            case java.util.Calendar.WEDNESDAY: // PIERNA + CORE
                ejercicios.add(crearEjDemo("Sentadilla Barra", 4, "6-8", 60f, 180));
                ejercicios.add(crearEjDemo("RDL Barra", 4, "8-10", 50f, 150));
                ejercicios.add(crearEjDemo("Hip Thrust", 3, "10-12", 70f, 120));
                ejercicios.add(crearEjDemo("Extensión Cuádriceps", 3, "12-15", 40f, 90));
                ejercicios.add(crearEjDemo("Curl Femoral Tumbado", 3, "10-12", 30f, 90));
                ejercicios.add(crearEjDemo("Plancha + Hollow Hold", 3, "30s", 0f, 60));
                ejercicios.add(crearEjDemo("Press Pallof", 3, "12/lado", 10f, 60));
                break;

            case java.util.Calendar.FRIDAY: // PULL: Espalda + Bíceps + Postura
                ejercicios.add(crearEjDemo("Dominadas (agarre neutro)", 4, "6-8", 0f, 180));
                ejercicios.add(crearEjDemo("Remo Neutro Mancuerna", 4, "8-10", 28f, 150));
                ejercicios.add(crearEjDemo("Remo Unilateral con Rotación", 3, "10-12", 22f, 120));
                ejercicios.add(crearEjDemo("Kelso Shrug (retracción)", 3, "12-15", 16f, 90));
                ejercicios.add(crearEjDemo("Curl Z Barra", 3, "8-10", 25f, 90));
                ejercicios.add(crearEjDemo("Curl Predicador Máquina", 3, "10-12", 20f, 90));
                ejercicios.add(crearEjDemo("Band Pull-Aparts", 3, "15-20", 0f, 45));
                ejercicios.add(crearEjDemo("Wall Angels (test postural)", 3, "8-10", 0f, 60));
                break;

            case java.util.Calendar.SATURDAY: // HOMBROS + BRAZOS (DÍA CLAVE V-TAPER)
                ejercicios.add(crearEjDemo("Press Hombro Mancuernas", 4, "8-10", 18f, 150));
                ejercicios.add(crearEjDemo("Elev. Laterales Sentado", 4, "12-15", 10f, 90));
                ejercicios.add(crearEjDemo("Elev. Laterales Polea (tras nuca)", 3, "12-15", 7f, 90));
                ejercicios.add(crearEjDemo("Pájaro inclinado (rear delt)", 3, "12-15", 8f, 90));
                ejercicios.add(crearEjDemo("Curl Zottman", 3, "10-12", 12f, 90));
                ejercicios.add(crearEjDemo("Curl Inclinado 45°", 3, "10-12", 10f, 90));
                ejercicios.add(crearEjDemo("Extensión Overhead Polea", 3, "10-12", 20f, 90));
                ejercicios.add(crearEjDemo("Rotación externa banda", 3, "15/lado", 0f, 45));
                break;

            default: // Martes/Jueves (natación) o Domingo → Push por defecto
                ejercicios.add(crearEjDemo("Press Militar Mancuernas", 4, "8-10", 18f, 180));
                ejercicios.add(crearEjDemo("Elev. Laterales Sentado", 4, "12-15", 10f, 90));
                ejercicios.add(crearEjDemo("Press Inclinado Mancuernas", 4, "8-10", 22f, 150));
                ejercicios.add(crearEjDemo("Cruces Polea Alta", 3, "10-12", 15f, 90));
                ejercicios.add(crearEjDemo("Press Francés 30° Barra Z", 3, "10-12", 18f, 120));
                ejercicios.add(crearEjDemo("Face Pulls (retracción)", 3, "15-20", 12f, 60));
                break;
        }
        iniciarSesion();
    }

    private Ejercicio crearEjDemo(String nombre, int series, String reps, float peso, int descanso) {
        Ejercicio ej = new Ejercicio();
        ej.setNombre(nombre);
        ej.setSeriesPlan(series);
        ej.setRepsPlan(reps);
        ej.setPesoSugerido(peso);
        ej.setDescansoSeg(descanso);
        return ej;
    }

    private void iniciarSesion() {
        cargando.setValue(false);
        tiempoInicioMs = System.currentTimeMillis();
        indiceEjercicio = 0;
        serieCompletadaActual = 0;
        volumenTotal = 0;

        if (!ejercicios.isEmpty()) {
            ejercicioActual.setValue(ejercicios.get(0));
            serieActual.setValue(1);
        }

        // Cronómetro total
        cronometroTotal = new CountDownTimer(Long.MAX_VALUE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seg = (int) ((System.currentTimeMillis() - tiempoInicioMs) / 1000);
                tiempoTotalSegundos.postValue(seg);
            }

            @Override
            public void onFinish() {}
        };
        cronometroTotal.start();
    }

    /**
     * Registra una serie completada.
     * Envía datos al backend para el motor de progresión (ACSM 2009).
     */
    public void registrarSerie(int reps, int rirPercibido, float pesoUsado) {
        if (indiceEjercicio >= ejercicios.size()) return;
        serieCompletadaActual++;
        volumenTotal += pesoUsado * reps;

        Ejercicio ejActual = ejercicios.get(indiceEjercicio);

        // Enviar log al backend
        Map<String, Object> datos = new HashMap<>();
        datos.put("accion", "guardar_log");
        datos.put("sesion_id", sesionId);
        datos.put("ejercicio_id", ejActual.getEjercicioId());
        datos.put("plan_id", ejActual.getPlanId());
        datos.put("num_serie", serieCompletadaActual);
        datos.put("num_peso_usado_kg", pesoUsado);
        datos.put("num_reps_completadas", reps);
        datos.put("num_rir_percibido", rirPercibido);
        datos.put("str_sensacion", rirPercibido <= 1 ? "duro" : "bien");

        ApiClient.getApi().enviarDatos(datos).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().ok) {
                    // Servidor respondio pero con error — encolar para reintento
                    SyncManager.encolar(app, datos);
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                // Sin red o timeout — encolar en Room para sync posterior
                SyncManager.encolar(app, datos);
            }
        });

        serieActual.setValue(serieCompletadaActual + 1);
    }

    public boolean quedanSeriesPorHacer() {
        if (indiceEjercicio >= ejercicios.size()) return false;
        Ejercicio ej = ejercicios.get(indiceEjercicio);
        return serieCompletadaActual < ej.getSeriesPlan();
    }

    public void siguienteEjercicio() {
        indiceEjercicio++;
        serieCompletadaActual = 0;

        if (indiceEjercicio >= ejercicios.size()) {
            // Sesión terminada
            completarSesionEnBackend();
            sesionCompletada.setValue(true);
        } else {
            ejercicioActual.setValue(ejercicios.get(indiceEjercicio));
            serieActual.setValue(1);
        }
    }

    /**
     * Inicia timer de descanso.
     * Descanso según evidencia (Schoenfeld 2016):
     *   - Compuestos: 3-5 min
     *   - Aislamiento: 1.5-2 min
     */
    public void iniciarTimer(int segundos) {
        if (timer != null) timer.cancel();

        timer = new CountDownTimer(segundos * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerSegundos.postValue((int) (millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                timerSegundos.postValue(0);
                // Auto-avanza a siguiente serie (ui.md § 4.5)
            }
        };
        timer.start();
    }

    public void saltarTimer() {
        if (timer != null) {
            timer.cancel();
            timerSegundos.setValue(0);
        }
    }

    private void completarSesionEnBackend() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("accion", "completar_sesion");
        datos.put("sesion_id", sesionId);

        ApiClient.getApi().enviarDatos(datos).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().ok) {
                    SyncManager.encolar(app, datos);
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                SyncManager.encolar(app, datos);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (timer != null) timer.cancel();
        if (cronometroTotal != null) cronometroTotal.cancel();
    }
}
