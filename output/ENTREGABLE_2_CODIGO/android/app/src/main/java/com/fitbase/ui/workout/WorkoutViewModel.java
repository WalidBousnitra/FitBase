package com.fitbase.ui.workout;

import android.os.CountDownTimer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fitbase.data.api.ApiClient;
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
 * Referencia: REG-DEV-01 (ui.md) §4, REG-LOG-01 (motor_pesos.md)
 */
public class WorkoutViewModel extends ViewModel {

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

    private CountDownTimer timer;
    private CountDownTimer cronometroTotal;

    public LiveData<Ejercicio> getEjercicioActual() { return ejercicioActual; }
    public LiveData<Integer> getSerieActual() { return serieActual; }
    public LiveData<Integer> getTimerSegundos() { return timerSegundos; }
    public LiveData<Boolean> isSesionCompletada() { return sesionCompletada; }
    public LiveData<Integer> getTiempoTotalSegundos() { return tiempoTotalSegundos; }
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
                    ejercicios = response.body().ejercicios;
                    iniciarSesion();
                }
            }

            @Override
            public void onFailure(Call<SesionResponse> call, Throwable t) {
                // TODO: cargar desde cache Room
            }
        });
    }

    private void iniciarSesion() {
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
        serieCompletadaActual++;
        volumenTotal += pesoUsado * reps;

        // Enviar log al backend
        Map<String, Object> datos = new HashMap<>();
        datos.put("accion", "guardar_log");
        datos.put("sesion_id", sesionId);
        datos.put("ejercicio_id", ejercicios.get(indiceEjercicio).getEjercicioId());
        datos.put("plan_id", ejercicios.get(indiceEjercicio).getPlanId());
        datos.put("num_serie", serieCompletadaActual);
        datos.put("num_peso_usado_kg", pesoUsado);
        datos.put("num_reps_completadas", reps);
        datos.put("num_rir_percibido", rirPercibido);
        datos.put("str_sensacion", rirPercibido <= 1 ? "duro" : "bien");

        ApiClient.getApi().enviarDatos(datos).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                // Log guardado
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                // TODO: guardar en Room para sync posterior
            }
        });

        serieActual.setValue(serieCompletadaActual + 1);
    }

    public boolean quedanSeriesPorHacer() {
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
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {}

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {}
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (timer != null) timer.cancel();
        if (cronometroTotal != null) cronometroTotal.cancel();
    }
}
