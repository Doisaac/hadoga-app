package com.hadoga.hadoga.model.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;

@Entity(
        tableName = "doctor",
        foreignKeys = @ForeignKey(
                entity = Sucursal.class,
                parentColumns = "codigo_sucursal",
                childColumns = "sucursal_asignada",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index(value = "numero_colegiado", unique = true),
                @Index(value = "sucursal_asignada")
        }
)
public class Doctor implements Serializable {
    @Exclude
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "nombre")
    private String nombre;

    @ColumnInfo(name = "apellido")
    private String apellido;

    @ColumnInfo(name = "fecha_nacimiento")
    private String fechaNacimiento;

    @ColumnInfo(name = "numero_colegiado")
    private String numeroColegiado;

    @ColumnInfo(name = "sexo")
    private String sexo;

    @ColumnInfo(name = "especialidad")
    private String especialidad;

    @ColumnInfo(name = "sucursal_asignada")
    private String sucursalAsignada;

    @ColumnInfo(name = "foto_uri")
    // URI o ruta del archivo
    private String fotoUri;

    @ColumnInfo(name = "estado_sincronizacion")
    private String estadoSincronizacion = "PENDIENTE";

    // Constructor
    public Doctor(String nombre, String apellido, String fechaNacimiento,
                  String numeroColegiado, String sexo, String especialidad,
                  String sucursalAsignada, String fotoUri) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.fechaNacimiento = fechaNacimiento;
        this.numeroColegiado = numeroColegiado;
        this.sexo = sexo;
        this.especialidad = especialidad;
        this.sucursalAsignada = sucursalAsignada;
        this.fotoUri = fotoUri;
    }

    public Doctor() {
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(String fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getNumeroColegiado() {
        return numeroColegiado;
    }

    public void setNumeroColegiado(String numeroColegiado) {
        this.numeroColegiado = numeroColegiado;
    }

    public String getSexo() {
        return sexo;
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public String getSucursalAsignada() {
        return sucursalAsignada;
    }

    public void setSucursalAsignada(String sucursalAsignada) {
        this.sucursalAsignada = sucursalAsignada;
    }

    public String getFotoUri() {
        return fotoUri;
    }

    public void setFotoUri(String fotoUri) {
        this.fotoUri = fotoUri;
    }

    public String getEstadoSincronizacion() {
        return estadoSincronizacion;
    }

    public void setEstadoSincronizacion(String estadoSincronizacion) {
        this.estadoSincronizacion = estadoSincronizacion;
    }
}
