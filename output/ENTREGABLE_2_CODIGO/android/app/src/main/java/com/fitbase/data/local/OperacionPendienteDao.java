package com.fitbase.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * DAO para la cola de operaciones pendientes.
 * Se usa cuando enviarDatos() falla por falta de red.
 */
@Dao
public interface OperacionPendienteDao {

    @Insert
    long insertar(OperacionPendiente operacion);

    @Delete
    void eliminar(OperacionPendiente operacion);

    @Update
    void actualizar(OperacionPendiente operacion);

    /** Obtiene todas las pendientes ordenadas por antigueedad (FIFO) */
    @Query("SELECT * FROM cola_pendientes ORDER BY creadoEn ASC")
    List<OperacionPendiente> obtenerTodas();

    /** Cuenta pendientes (para mostrar badge o log) */
    @Query("SELECT COUNT(*) FROM cola_pendientes")
    int contarPendientes();

    /** Elimina una por ID (tras exito) */
    @Query("DELETE FROM cola_pendientes WHERE id = :id")
    void eliminarPorId(long id);
}
