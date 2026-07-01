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
import com.fitbase.util.Constants;

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
            base.caloriasObjetivo = Constants.CALORIAS_FALLBACK;
            base.proteinaG = Constants.PROTEINA_FALLBACK_G;
            base.carbosG = Constants.CARBOS_FALLBACK_G;
            base.grasasG = Constants.GRASAS_FALLBACK_G;
            base.aguaMl = 3200;
            base.pasosObjetivo = Constants.PASOS_OBJETIVO;
            base.esDiaEntreno = true;
            base.fase = "pre-programa";

            // Intentar leer datos reales de Health Connect
            if (HealthConnectReader.isAvailable(getApplication())) {
                HealthConnectReader reader = new HealthConnectReader(getApplication());
                reader.leerDatosHoy(datos -> {
                    base.pasosActuales = datos.pasos;
                    // HC nutrition: use if available, else demo values
                    if (datos.caloriasConsumidas > 0) {
                        base.caloriasConsumidas = datos.caloriasConsumidas;
                        base.proteinaConsumidaG = datos.proteinaG;
                        base.carbosConsumidosG = datos.carbosG;
                        base.grasasConsumidasG = datos.grasasG;
                    } else {
                        // FatSecret no sincroniza a HC en la mayoría de dispositivos
                        // Mostrar valores demo para que la UI no quede vacía
                        rellenarNutricionDemo(base);
                    }
                    macros.postValue(base);
                });
            } else {
                // HC no disponible — mostrar datos demo completos
                rellenarNutricionDemo(base);
                macros.setValue(base);
            }
            return;
        }

        // MODO REAL: backend devuelve objetivos, HC devuelve consumido
        ApiClient.getApi().getMacrosHoy("macros_hoy").enqueue(new Callback<MacrosResponse>() {
            @Override
            public void onResponse(Call<MacrosResponse> call, Response<MacrosResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    MacrosResponse objetivos = response.body();
                    // Merge con datos reales de Health Connect (pasos Zepp + nutrición FatSecret)
                    if (HealthConnectReader.isAvailable(getApplication())) {
                        HealthConnectReader reader = new HealthConnectReader(getApplication());
                        reader.leerDatosHoy(datos -> {
                            objetivos.pasosActuales = datos.pasos;
                            objetivos.caloriasConsumidas = datos.caloriasConsumidas;
                            objetivos.proteinaConsumidaG = datos.proteinaG;
                            objetivos.carbosConsumidosG = datos.carbosG;
                            objetivos.grasasConsumidasG = datos.grasasG;
                            macros.postValue(objetivos);
                        });
                    } else {
                        macros.postValue(objetivos);
                    }
                }
            }

            @Override
            public void onFailure(Call<MacrosResponse> call, Throwable t) {
                MacrosResponse fallback = new MacrosResponse();
                fallback.caloriasObjetivo = Constants.CALORIAS_FALLBACK;
                fallback.proteinaG = Constants.PROTEINA_FALLBACK_G;
                fallback.carbosG = Constants.CARBOS_FALLBACK_G;
                fallback.grasasG = Constants.GRASAS_FALLBACK_G;
                fallback.aguaMl = 3200;
                fallback.pasosObjetivo = Constants.PASOS_OBJETIVO;
                macros.postValue(fallback);
            }
        });
    }

    private void cargarSesion() {
        if (Boolean.TRUE.equals(modoDemo.getValue())) {
            // Demo: Sesión según día de la semana REAL (split PPL + Hombros/Brazos)
            // Prioridades: Estética V-taper > Postura > Hipertrofia priorizada > Flexibilidad
            int diaSemana = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);

            SesionResponse mockSesion = new SesionResponse();
            mockSesion.sesion = new Sesion();
            mockSesion.ejercicios = new ArrayList<>();

            switch (diaSemana) {
                case Calendar.MONDAY: // PUSH: Hombros > Pecho > Tríceps
                    mockSesion.sesion.setTipo("Push — Hombros & Pecho");
                    mockSesion.sesion.setDuracionEstimadaMin(75);
                    // HOMBROS primero (P1: V-taper)
                    mockSesion.ejercicios.add(crearEjercicioDemo("Press Militar Mancuernas", 4, "8-10", 18f, 180));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Elev. Laterales Sentado", 4, "12-15", 10f, 90));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Elev. Laterales Polea Media", 3, "12-15", 7f, 90));
                    // PECHO (P5 — no priorizar sobre hombros)
                    mockSesion.ejercicios.add(crearEjercicioDemo("Press Inclinado Mancuernas", 4, "8-10", 22f, 150));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Cruces Polea Alta", 3, "10-12", 15f, 90));
                    // TRÍCEPS
                    mockSesion.ejercicios.add(crearEjercicioDemo("Press Francés 30° Barra Z", 3, "10-12", 18f, 120));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Extensión Unilateral Polea", 3, "12-15", 8f, 60));
                    // CORRECTIVO POSTURAL (P2)
                    mockSesion.ejercicios.add(crearEjercicioDemo("Face Pulls (retracción)", 3, "15-20", 12f, 60));
                    mockSesion.mensaje = "Hombros PRIMERO · P1 Estética V-taper";
                    break;

                case Calendar.TUESDAY: // NATACIÓN
                case Calendar.THURSDAY:
                    mockSesion.sesion.setTipo("Natación — Técnica + Postura");
                    mockSesion.sesion.setDuracionEstimadaMin(50);
                    mockSesion.ejercicios.add(crearEjercicioDemo("Calentamiento 200m suave", 1, "1", 0f, 60));
                    mockSesion.ejercicios.add(crearEjercicioDemo("4×50m Crol técnica", 4, "1", 0f, 30));
                    mockSesion.ejercicios.add(crearEjercicioDemo("4×50m Espalda (extensión torácica)", 4, "1", 0f, 30));
                    mockSesion.ejercicios.add(crearEjercicioDemo("200m variado", 1, "1", 0f, 60));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Vuelta calma 100m", 1, "1", 0f, 0));
                    mockSesion.mensaje = "Natación: postura + cardio bajo impacto";
                    break;

                case Calendar.WEDNESDAY: // PIERNA + CORE
                    mockSesion.sesion.setTipo("Pierna + Core");
                    mockSesion.sesion.setDuracionEstimadaMin(75);
                    mockSesion.ejercicios.add(crearEjercicioDemo("Sentadilla Barra", 4, "6-8", 60f, 180));
                    mockSesion.ejercicios.add(crearEjercicioDemo("RDL Barra", 4, "8-10", 50f, 150));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Hip Thrust", 3, "10-12", 70f, 120));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Extensión Cuádriceps", 3, "12-15", 40f, 90));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Curl Femoral Tumbado", 3, "10-12", 30f, 90));
                    // CORE (anti-extensión + anti-rotación)
                    mockSesion.ejercicios.add(crearEjercicioDemo("Plancha + Hollow Hold", 3, "30s", 0f, 60));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Press Pallof", 3, "12/lado", 10f, 60));
                    // FLEXIBILIDAD (P4: aductores)
                    mockSesion.ejercicios.add(crearEjercicioDemo("Estiramiento aductores 30s×3", 3, "30s", 0f, 30));
                    mockSesion.mensaje = "Compuestos primero · Flex. aductores al final";
                    break;

                case Calendar.FRIDAY: // PULL: Espalda + Bíceps + POSTURA
                    mockSesion.sesion.setTipo("Pull — Espalda & Postura");
                    mockSesion.sesion.setDuracionEstimadaMin(80);
                    // ESPALDA (P1: V-taper + P2: postura)
                    mockSesion.ejercicios.add(crearEjercicioDemo("Dominadas (agarre neutro)", 4, "6-8", 0f, 180));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Remo Neutro Mancuerna", 4, "8-10", 28f, 150));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Remo Unilateral con Rotación", 3, "10-12", 22f, 120));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Kelso Shrug (retracción)", 3, "12-15", 16f, 90));
                    // BÍCEPS (P3: 2ª prioridad muscular)
                    mockSesion.ejercicios.add(crearEjercicioDemo("Curl Z Barra", 3, "8-10", 25f, 90));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Curl Predicador Máquina", 3, "10-12", 20f, 90));
                    // POSTURA (P2: correctivo)
                    mockSesion.ejercicios.add(crearEjercicioDemo("Band Pull-Aparts", 3, "15-20", 0f, 45));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Wall Angels (test postural)", 3, "8-10", 0f, 60));
                    mockSesion.mensaje = "P2 Postura: Wall Angels + retracción escapular";
                    break;

                case Calendar.SATURDAY: // HOMBROS + BRAZOS + POSTURA (DÍA CLAVE V-TAPER)
                    mockSesion.sesion.setTipo("Hombros + Brazos · V-Taper");
                    mockSesion.sesion.setDuracionEstimadaMin(80);
                    // HOMBROS — volumen extra (14-18 ser/sem total)
                    mockSesion.ejercicios.add(crearEjercicioDemo("Press Hombro Mancuernas", 4, "8-10", 18f, 150));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Elev. Laterales Sentado", 4, "12-15", 10f, 90));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Elev. Laterales Polea (tras nuca)", 3, "12-15", 7f, 90));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Pájaro inclinado (rear delt)", 3, "12-15", 8f, 90));
                    // BÍCEPS
                    mockSesion.ejercicios.add(crearEjercicioDemo("Curl Zottman", 3, "10-12", 12f, 90));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Curl Inclinado 45°", 3, "10-12", 10f, 90));
                    // TRÍCEPS
                    mockSesion.ejercicios.add(crearEjercicioDemo("Extensión Overhead Polea", 3, "10-12", 20f, 90));
                    // POSTURA + FLEX (P2 + P4)
                    mockSesion.ejercicios.add(crearEjercicioDemo("Rotación externa banda", 3, "15/lado", 0f, 45));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Estiramiento tríceps overhead 30s", 3, "30s", 0f, 30));
                    mockSesion.mensaje = "DÍA CLAVE V-TAPER · Hombros + Postura + Flex";
                    break;

                case Calendar.SUNDAY: // DESCANSO ACTIVO
                default:
                    mockSesion.sesion.setTipo("Descanso Activo — Movilidad");
                    mockSesion.sesion.setDuracionEstimadaMin(25);
                    mockSesion.ejercicios.add(crearEjercicioDemo("Cat-Cow (extensión torácica)", 3, "10", 0f, 30));
                    mockSesion.ejercicios.add(crearEjercicioDemo("World's Greatest Stretch", 3, "8/lado", 0f, 30));
                    mockSesion.ejercicios.add(crearEjercicioDemo("90/90 Hip Rotations", 3, "10/lado", 0f, 30));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Wall Angels (progresión)", 3, "8-10", 0f, 60));
                    mockSesion.ejercicios.add(crearEjercicioDemo("Estiramiento aductores + overhead tríceps", 3, "30s/lado", 0f, 30));
                    mockSesion.mensaje = "Recuperación · P2 Postura + P4 Flexibilidad";
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

    /** Rellena nutrición demo según hora del día (simula desayuno/comida/cena) */
    private void rellenarNutricionDemo(MacrosResponse base) {
        int hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        float factor;
        if (hora < 10) factor = 0.15f;       // Solo desayuno
        else if (hora < 14) factor = 0.35f;   // Desayuno + snack
        else if (hora < 18) factor = 0.55f;   // + comida
        else if (hora < 21) factor = 0.75f;   // + merienda
        else factor = 0.88f;                   // + cena

        base.caloriasConsumidas = (int)(base.caloriasObjetivo * factor);
        base.proteinaConsumidaG = (int)(base.proteinaG * factor);
        base.carbosConsumidosG = (int)(base.carbosG * factor);
        base.grasasConsumidasG = (int)(base.grasasG * factor);
    }
}
