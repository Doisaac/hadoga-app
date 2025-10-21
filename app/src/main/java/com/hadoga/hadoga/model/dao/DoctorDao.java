package com.hadoga.hadoga.model.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.hadoga.hadoga.model.entities.Doctor;

import java.util.List;

@Dao
public interface DoctorDao {
    @Insert
    void insertar(Doctor doctor);

    @Update
    void actualizar(Doctor doctor);

    @Delete
    void eliminar(Doctor doctor);

    @Query("SELECT * FROM doctor ORDER BY nombre ASC")
    List<Doctor> obtenerTodos();

    @Query("SELECT * FROM doctor WHERE id = :id")
    Doctor obtenerPorId(int id);

    @Query("SELECT * FROM doctor WHERE sucursal_asignada = :idSucursal")
    List<Doctor> obtenerPorSucursal(int idSucursal);
}
