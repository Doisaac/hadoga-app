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

    @Query("SELECT * FROM doctor WHERE estado_sincronizacion = :estado")
    List<Doctor> getByEstado(String estado);

    @Query("SELECT * FROM doctor WHERE estado_sincronizacion = 'PENDIENTE'")
    List<Doctor> getPendientes();

    @Query("SELECT * FROM doctor WHERE numero_colegiado = :colegiado LIMIT 1")
    Doctor getDoctorByColegiado(String colegiado);

    @Query("SELECT * FROM doctor WHERE sucursal_asignada = :codigoSucursal")
    List<Doctor> getDoctoresDeSucursal(String codigoSucursal);

    @Query("SELECT * FROM doctor WHERE estado_sincronizacion = 'ELIMINADO_PENDIENTE'")
    List<Doctor> getEliminadosPendientes();

    @Query("SELECT * FROM doctor")
    List<Doctor> getAllDoctores();
}
