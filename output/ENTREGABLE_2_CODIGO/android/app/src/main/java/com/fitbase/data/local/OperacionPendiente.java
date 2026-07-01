package com.fitbase.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad Room para cola de operaciones pendientes.
 * Guarda POSTs fallidos al backend (Google Sheets) para reintentar con conexion.
 *
 * Garantiza que NINGUN registro de serie se pierde por fallo de red.
 * El usuario nunca acaba con una "BBDD vacia" al final del plan.
 */
@Entity(tableName = "cola_pendientes")
public class OperacionPendiente {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** JSON serializado del Map<String, Object> que se envia al backend */
    public String datosJson;

    /** Timestamp de creacion (epoch millis) */
    public long creadoEn;

    /** Numero de reintentos realizados */
    public int reintentos;

    /** Ultima vez que se intento (epoch millis), 0 si nunca */
    public long ultimoIntento;

    public OperacionPendiente() {
        this.creadoEn = System.currentTimeMillis();
        this.reintentos = 0;
        this.ultimoIntento = 0;
    }
}
