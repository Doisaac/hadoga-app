package com.hadoga.hadoga.model.entities;

import static androidx.room.ForeignKey.CASCADE;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(
        tableName = "cita",
        foreignKeys = {
                @ForeignKey(
                        entity = Sucursal.class,
                        parentColumns = "codigo_sucursal",
                        childColumns = "codigo_sucursal_asignada",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = Paciente.class,
                        parentColumns = "id",
                        childColumns = "paciente_id",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index("codigo_sucursal_asignada"),
                @Index("paciente_id")
        }
)
public class Cita implements Serializable {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "codigo_sucursal_asignada")
    private String codigoSucursalAsignada;

    @ColumnInfo(name = "paciente_id")
    private int pacienteId;

    @ColumnInfo(name = "fecha_hora")
    private String fechaHora;

    @ColumnInfo(name = "motivo")
    private String motivo;

    @ColumnInfo(name = "notas")
    private String notas;

    @ColumnInfo(name = "estado", defaultValue = "pendiente")
    private String estado;

    // Constructor principal
    public Cita(String codigoSucursalAsignada, int pacienteId, String fechaHora,
                String motivo, String notas, String estado) {
        this.codigoSucursalAsignada = codigoSucursalAsignada;
        this.pacienteId = pacienteId;
        this.fechaHora = fechaHora;
        this.motivo = motivo;
        this.notas = notas;
        this.estado = estado;
    }

    public Cita() {
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCodigoSucursalAsignada() {
        return codigoSucursalAsignada;
    }

    public void setCodigoSucursalAsignada(String codigoSucursalAsignada) {
        this.codigoSucursalAsignada = codigoSucursalAsignada;
    }

    public int getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(int pacienteId) {
        this.pacienteId = pacienteId;
    }

    public String getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(String fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}