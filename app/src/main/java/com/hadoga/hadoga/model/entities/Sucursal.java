package com.hadoga.hadoga.model.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "sucursal",
        indices = {@Index(value = "codigo_sucursal", unique = true)}
)
public class Sucursal {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "nombre_sucursal")
    private String nombreSucursal;

    @ColumnInfo(name = "codigo_sucursal")
    private String codigoSucursal;

    @ColumnInfo(name = "departamento")
    private String departamento;

    @ColumnInfo(name = "direccion_completa")
    private String direccionCompleta;

    @ColumnInfo(name = "telefono")
    private String telefono;

    @ColumnInfo(name = "correo")
    private String correo;

    // Constructores
    public Sucursal() {
    }

    public Sucursal(String nombreSucursal, String codigoSucursal, String departamento,
                    String direccionCompleta, String telefono, String correo) {
        this.nombreSucursal = nombreSucursal;
        this.codigoSucursal = codigoSucursal;
        this.departamento = departamento;
        this.direccionCompleta = direccionCompleta;
        this.telefono = telefono;
        this.correo = correo;
    }



    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombreSucursal() {
        return nombreSucursal;
    }

    public void setNombreSucursal(String nombreSucursal) {
        this.nombreSucursal = nombreSucursal;
    }

    public String getCodigoSucursal() {
        return codigoSucursal;
    }

    public void setCodigoSucursal(String codigoSucursal) {
        this.codigoSucursal = codigoSucursal;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public String getDireccionCompleta() {
        return direccionCompleta;
    }

    public void setDireccionCompleta(String direccionCompleta) {
        this.direccionCompleta = direccionCompleta;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }
}
