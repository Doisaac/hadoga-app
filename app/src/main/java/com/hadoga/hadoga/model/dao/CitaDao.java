package com.hadoga.hadoga.model.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.hadoga.hadoga.model.entities.Cita;

import java.util.List;

@Dao
public interface CitaDao {
    @Insert
    void insertar(Cita cita);

    @Update
    void actualizar(Cita cita);

    @Delete
    void eliminar(Cita cita);

    @Query("DELETE FROM cita")
    void eliminarTodas();

    @Query("SELECT * FROM cita WHERE id = :idCita LIMIT 1")
    Cita obtenerPorId(int idCita);

    @Query("SELECT * FROM cita ORDER BY fecha_hora DESC")
    List<Cita> obtenerTodas();

    @Query("SELECT * FROM cita WHERE codigo_sucursal_asignada = :codigoSucursal ORDER BY fecha_hora DESC")
    List<Cita> obtenerPorSucursal(String codigoSucursal);

    @Query("SELECT * FROM cita WHERE paciente_id = :pacienteId ORDER BY fecha_hora DESC")
    List<Cita> obtenerPorPaciente(int pacienteId);

    @Query("SELECT * FROM cita WHERE estado = :estado ORDER BY fecha_hora DESC")
    List<Cita> obtenerPorEstado(String estado);

    @Query("SELECT * FROM cita WHERE fecha_hora BETWEEN :desde AND :hasta ORDER BY fecha_hora ASC")
    List<Cita> obtenerPorRangoFechas(String desde, String hasta);

    @Query("UPDATE cita SET estado = :nuevoEstado WHERE id = :citaId")
    void actualizarEstado(int citaId, String nuevoEstado);

    @Query("SELECT COUNT(*) FROM cita WHERE estado = :estado")
    int contarPorEstado(String estado);

    @Query("SELECT COUNT(*) FROM cita " +
            "WHERE codigo_sucursal_asignada = :codigoSucursal " +
            "AND estado = 'pendiente' " +
            "AND fecha_hora BETWEEN :desde AND :hasta")
    int contarSolapadas(String codigoSucursal, String desde, String hasta);

    @Query("SELECT COUNT(*) FROM cita " +
            "WHERE codigo_sucursal_asignada = :codigoSucursal " +
            "AND estado = 'pendiente' " +
            "AND id != :idCita " +
            "AND fecha_hora BETWEEN :desde AND :hasta")
    int contarSolapadasExcluyendo(int idCita, String codigoSucursal, String desde, String hasta);
}
