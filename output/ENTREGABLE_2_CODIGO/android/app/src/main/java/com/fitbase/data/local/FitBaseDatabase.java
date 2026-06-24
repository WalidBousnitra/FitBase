package com.fitbase.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Base de datos Room local.
 * Proposito principal: cola de operaciones pendientes para resiliencia offline.
 * Garantiza que datos de series NUNCA se pierden por cortes de red.
 */
@Database(entities = {OperacionPendiente.class}, version = 1, exportSchema = false)
public abstract class FitBaseDatabase extends RoomDatabase {

    private static volatile FitBaseDatabase INSTANCE;

    public abstract OperacionPendienteDao operacionPendienteDao();

    public static FitBaseDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (FitBaseDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            FitBaseDatabase.class,
                            "fitbase_local.db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
