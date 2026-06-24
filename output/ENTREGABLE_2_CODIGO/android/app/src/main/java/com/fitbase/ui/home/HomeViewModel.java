package com.fitbase.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fitbase.data.api.ApiClient;
import com.fitbase.data.health.HealthConnectReader;
import com.fitbase.data.model.AusenciaResponse;
import com.fitbase.data.model.Ejercicio;
import com.fitbase.data.model.MacrosResponse;
import com.fitbase.data.model.Sesion;
import com.fitbase.data.model.SesionResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel para pantalla principal.
 * DEMO: lee pasos y calorías REALES de Health Connect (Zepp + FatSecret).
 *       Sesión mostrada pero NO arranca entrenamiento ni escribe en BBDD.
 * REAL: todo conectado al backend.
 */
public class HomeViewModel extends AndroidViewModel {

    private static final String FECHA_INICIO = "2026-08-31";

    private final MutableLiveData<MacrosResponse> macros = new MutableLiveData<>();
    private final MutableLiveData<SesionResponse> sesionHoy = new MutableLiveData<>();
    private final MutableLiveData<Boolean> modoDemo = new MutableLiveData<>();
    private final MutableLiveData<AusenciaResponse> ausenciaDetectada = new MutableLiveData<>();

    public HomeViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<MacrosResponse> getMacros() { return macros; }
    public LiveData<SesionResponse> getSesionHoy() { return sesionHoy; }
    public LiveData<Boolean> isModoDemo() { return modoDemo; }
    public LiveData<AusenciaResponse> getAusenciaDetectada() { return ausenciaDetectada; }

    public void cargarDatosDelDia() {
        verificarModoDemo();
        cargarMacros();
        cargarSesion();
    }

    public void checkAusencia() {
        if (Boolean.TRUE.equals(modoDemo.getValue())) return;

        ApiClient.getApi().checkAusencia("check_ausencia").enqueue(new Callback<AusenciaResponse>() {
            @Override
            public void onResponse(Call<AusenciaResponse> call, Response<AusenciaResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ausenciaDetectada.postValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<AusenciaResponse> call, Throwable t) {}
        });
    }

    public void registrarAusenciaExtendida() {
        // TODO: date picker → API registrar_ausencia
    }

    private void verificarModoDemo() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date fechaInicio = sdf.parse(FECHA_INICIO);
            Date hoy = new Date();
            modoDemo.setValue(hoy.before(fechaInicio));
        } catch (ParseException e) {
            modoDemo.setValue(false);
        }
    }

    private void cargarMacros() {
        if (Boolean.TRUE.equals(modoDemo.getValue())) {
            // DEMO: leer datos REALES de Health Connect (pasos Zepp + calorías FatSecret)
            // Objetivos fijos hasta que empiece el programa
            MacrosResponse base = new MacrosResponse();
            base.caloriasObjetivo = 3280;
            base.proteinaG = 156;
            base.carbosG = 488;
            base.grasasG = 78;
            base.aguaMl = 3200;
            base.pasosObjetivo = 8000;
            base.esDiaEntreno = true;
            base.fase = "pre-programa";

            // Intentar leer datos reales de Health Connect
            if (HealthConnectReader.isAvailable(getApplication())) {
                HealthConnectReader reader = new HealthConnectReader(getApplication());
                reader.leerDatosHoy(datos -> {
                    base.pasosActuales = datos.pasos;
                    base.caloriasConsumidas = datos.caloriasConsumidas;
                    base.proteinaConsumidaG = datos.proteinaG;
                    base.carbosConsumidosG = datos.carbosG;
                    base.grasasConsumidasG = datos.grasasG;
                    // Agua no viene de HC, dejar en 0
                    macros.postValue(base);
                });
            } else {
                // HC no disponible — mostrar 0 consumido
                macros.setValue(base);
            }
            return;
        }

        // MODO REAL: backend devuelve datos consolidados (HC + Sheets)
        ApiClient.getApi().getMacrosHoy("macros_hoy").enqueue(new Callback<MacrosResponse>() {
            @Override
            public void onResponse(Call<MacrosResponse> call, Response<MacrosResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    macros.postValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<MacrosResponse> call, Throwable t) {
                MacrosResponse fallback = new MacrosResponse();
                fallback.caloriasObjetivo = 3280;
                fallback.proteinaG = 156;
                fallback.carbosG = 488;
                fallback.grasasG = 78;
                fallback.aguaMl = 3200;
                fallback.pasosObjetivo = 8000;
                macros.postValue(fallback);
            }
        });
    }

    private void cargarSesion() {
        if (Boolean.TRUE.equals(modoDemo.getValue())) {
            // Demo: 3 sesiones en ciclo (Gym / Natación / Movilidad)
            // Rota según día del año % 3
            int diaAno = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR);
            int tipo = diaAno % 3;

            SesionResponse mockSesion = new SesionResponse();
            mockSesion.sesion = new Sesion();
            mockSesion.ejercicios = new ArrayList<>();

            switch (tipo) {
                case 0: // Día de GYM (Push)
                    mockSesion.sesion.setTipo("Push — Pecho & Hombros");
                    mockSesion.sesion.setDuracionEstimadaMin(72);
                    mockSesion.ejercicios.add(crearEjercicioDemo("Press Inclinado Mancuernas", 4, "8-10", 22f, 180));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Press Plano Barra", 4, "6-8", 50f, 180));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Elevaciones Laterales", 4, "12-15", 10f, 90));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Fondos en Paralelas", 3, "8-12", 0f, 120));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Press Militar Mancuernas", 3, "10-12", 16f, 120));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Extensión Tríceps Polea", 3, "12-15", 20f, 90));
                    mockSesion.mensaje = "Día de Gimnasio · Demo cíclica";
                    break;

                case 1: // Día de NATACIÓN
                    mockSesion.sesion.setTipo("Natación — Técnica");
                    mockSesion.sesion.setDuracionEstimadaMin(50);
                    mockSesion.ejercicios.add(crearEjercicioDemo("Calentamiento 200m suave", 1, "1", 0f, 60));
                    mockSesion.ejercicios.add(crearEjercicioDemo("4×50m Crol técnica", 4, "1", 0f, 30));
                    mockSesion.ejercicios.add(crearEjercicioDemo("4×50m Espalda", 4, "1", 0f, 30));
                    mockSesion.ejercicios.add(crearEjercicioDemo("200m variado", 1, "1", 0f, 60));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Vuelta calma 100m", 1, "1", 0f, 0));
                    mockSesion.mensaje = "Día de Piscina · Demo cíclica";
                    break;

                case 2: // Día de MOVILIDAD
                    mockSesion.sesion.setTipo("Movilidad & Flexibilidad");
                    mockSesion.sesion.setDuracionEstimadaMin(35);
                    mockSesion.ejercicios.add(crearEjercicioDemo("Cat-Cow", 3, "10", 0f, 30));
                    mockSesion.ejercicios.add(crearEjercicioDemo("World's Greatest Stretch", 3, "8/lado", 0f, 30));
                    mockSesion.ejercicios.add(crearEjercicioDemo("90/90 Hip Rotations", 3, "10/lado", 0f, 30));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Shoulder CARs", 3, "5/lado", 0f, 30));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Deep Squat Hold", 3, "30s", 0f, 30));
                    mockSesion.mensaje = "Día de Movilidad · Demo cíclica";
                    break;
            }
            sesionHoy.setValue(mockSesion);
            return;
        }

        ApiClient.getApi().getSesionHoy("sesion_hoy").enqueue(new Callback<SesionResponse>() {
            @Override
            public void onResponse(Call<SesionResponse> call, Response<SesionResponse> response) {
                if (response.isSuccessful()) {
                    sesionHoy.postValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<SesionResponse> call, Throwable t) {
                sesionHoy.postValue(null);
            }
        });
    }

    private Ejercicio crearEjercicioDemo(String nombre, int series, String reps, float peso, int descanso) {
        Ejercicio ej = new Ejercicio();
        ej.setNombre(nombre);
        ej.setSeriesPlan(series);
        ej.setRepsPlan(reps);
        ej.setPesoSugerido(peso);
        ej.setDescansoSeg(descanso);
        return ej;
    }
}
