package com.hadoga.hadoga.model.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.hadoga.hadoga.model.entities.Paciente;

import java.util.List;

@Dao
public interface PacienteDao {
    @Insert
    void insertar(Paciente paciente);

    @Update
    void actualizar(Paciente paciente);

    @Delete
    void eliminar(Paciente paciente);

    @Query("SELECT * FROM paciente ORDER BY id DESC")
    List<Paciente> obtenerTodos();

    @Query("DELETE FROM paciente")
    void eliminarTodos();

    @Query("SELECT * FROM paciente WHERE sucursal_id = :sucursalId ORDER BY id DESC")
    List<Paciente> obtenerPorSucursal(int sucursalId);

    @Query("SELECT * FROM paciente WHERE id = :id LIMIT 1")
    Paciente obtenerPorId(int id);
}