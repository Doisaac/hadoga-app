package com.hadoga.hadoga.model.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.hadoga.hadoga.model.dao.DoctorDao;
import com.hadoga.hadoga.model.dao.SucursalDao;
import com.hadoga.hadoga.model.dao.UsuarioDao;
import com.hadoga.hadoga.model.entities.Doctor;
import com.hadoga.hadoga.model.entities.Sucursal;
import com.hadoga.hadoga.model.entities.Usuario;

@Database(entities = {Usuario.class, Sucursal.class, Doctor.class}, version = 3)
public abstract class HadogaDatabase extends RoomDatabase {
    private static volatile HadogaDatabase INSTANCIADB;

    public abstract UsuarioDao usuarioDao();
    public abstract SucursalDao sucursalDao();
    public abstract DoctorDao doctorDao();

    public static HadogaDatabase getInstance(Context context) {
        if (INSTANCIADB == null) {
            synchronized (HadogaDatabase.class) {
                if (INSTANCIADB == null) {
                    INSTANCIADB = Room.databaseBuilder(context.getApplicationContext(), HadogaDatabase.class, "hadoga.db"
                            ).fallbackToDestructiveMigration().allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCIADB;
    }

}
