package com.fitbase.data.local;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import com.fitbase.data.api.ApiClient;
import com.fitbase.data.model.GenericResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Gestor de sincronizacion offline.
 *
 * Flujo:
 * 1. WorkoutViewModel intenta POST al backend.
 * 2. Si falla (onFailure o response.isSuccessful()==false) → encola en Room.
 * 3. SyncManager.sincronizar() se llama:
 *    - Al abrir la app (FitBaseApp.onCreate)
 *    - Al recuperar conectividad (NetworkCallback)
 *    - Tras cada serie exitosa (piggyback)
 * 4. Procesa cola FIFO. Si exito → elimina. Si falla → incrementa reintentos.
 *
 * MAX_REINTENTOS = 50 (suficiente para semanas sin conexion).
 */
public class SyncManager {

    private static final String TAG = "SyncManager";
    private static final int MAX_REINTENTOS = 50;
    private static final Gson gson = new Gson();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private static volatile boolean sincronizando = false;

    /**
     * Encola una operacion fallida para reintento posterior.
     * Se ejecuta en background thread.
     */
    public static void encolar(Context context, Map<String, Object> datos) {
        executor.execute(() -> {
            try {
                OperacionPendiente op = new OperacionPendiente();
                op.datosJson = gson.toJson(datos);
                FitBaseDatabase.getInstance(context).operacionPendienteDao().insertar(op);
                Log.d(TAG, "Operacion encolada: " + datos.get("accion"));
            } catch (Exception e) {
                Log.e(TAG, "Error al encolar", e);
            }
        });
    }

    /**
     * Intenta sincronizar todas las operaciones pendientes.
     * Thread-safe: solo un proceso a la vez.
     */
    public static void sincronizar(Context context) {
        if (sincronizando) return;
        if (!hayConexion(context)) return;

        executor.execute(() -> {
            sincronizando = true;
            try {
                OperacionPendienteDao dao = FitBaseDatabase.getInstance(context)
                        .operacionPendienteDao();
                List<OperacionPendiente> pendientes = dao.obtenerTodas();

                if (pendientes.isEmpty()) {
                    sincronizando = false;
                    return;
                }

                Log.d(TAG, "Sincronizando " + pendientes.size() + " operaciones pendientes");

                for (OperacionPendiente op : pendientes) {
                    if (op.reintentos >= MAX_REINTENTOS) {
                        // Demasiados reintentos — mantener en cola pero no reintentar ahora
                        continue;
                    }

                    try {
                        Map<String, Object> datos = gson.fromJson(op.datosJson, MAP_TYPE);
                        Call<GenericResponse> call = ApiClient.getApi().enviarDatos(datos);
                        Response<GenericResponse> response = call.execute(); // Sincrono

                        if (response.isSuccessful() && response.body() != null
                                && response.body().ok) {
                            // Exito — eliminar de la cola
                            dao.eliminarPorId(op.id);
                            Log.d(TAG, "Sincronizado OK: " + datos.get("accion"));
                        } else {
                            // Fallo del servidor — reintentar luego
                            op.reintentos++;
                            op.ultimoIntento = System.currentTimeMillis();
                            dao.actualizar(op);
                        }
                    } catch (Exception e) {
                        // Fallo de red — parar (probablemente sin conexion)
                        op.reintentos++;
                        op.ultimoIntento = System.currentTimeMillis();
                        dao.actualizar(op);
                        Log.w(TAG, "Fallo sync, parando: " + e.getMessage());
                        break;
                    }
                }
            } finally {
                sincronizando = false;
            }
        });
    }

    /**
     * Registra un NetworkCallback para sincronizar al recuperar red.
     * Llamar una vez desde FitBaseApp.onCreate().
     */
    public static void registrarCallbackConectividad(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                // Red disponible — intentar sincronizar pendientes
                sincronizar(context.getApplicationContext());
            }
        });
    }

    private static boolean hayConexion(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
