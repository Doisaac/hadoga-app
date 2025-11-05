package com.hadoga.hadoga.model.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.hadoga.hadoga.model.entities.Sucursal;

import java.util.List;

@Dao
public interface SucursalDao {
    @Insert
    void insert(Sucursal sucursal);

    @Update
    void update(Sucursal sucursal);

    @Delete
    void delete(Sucursal sucursal);

    @Query("SELECT * FROM sucursal ORDER BY nombre_sucursal ASC")
    List<Sucursal> getAllSucursales();

    @Query("SELECT * FROM sucursal WHERE codigo_sucursal = :codigo LIMIT 1")
    Sucursal getSucursalByCodigo(String codigo);

    @Query("SELECT * FROM sucursal WHERE estado_sincronizacion = 'PENDIENTE'")
    List<Sucursal> getPendientes();

    @Query("SELECT * FROM sucursal WHERE estado_sincronizacion = :estado")
    List<Sucursal> getByEstado(String estado);
}
