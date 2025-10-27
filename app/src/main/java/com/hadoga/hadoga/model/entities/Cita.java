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
                        parentColumns = "id",
                        childColumns = "sucursal_id",
                        onDelete = CASCADE
                ),
                @ForeignKey(
                        entity = Paciente.class,
                        parentColumns = "id",
                        childColumns = "paciente_id",
                        onDelete = CASCADE
                )
        },
        indices = {@Index("sucursal_id"), @Index("paciente_id")}
)
public class Cita implements Serializable {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "sucursal_id")
    private int sucursalId;

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
    public Cita(int sucursalId, int pacienteId, String fechaHora,
                String motivo, String notas, String estado) {
        this.sucursalId = sucursalId;
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

    public int getSucursalId() {
        return sucursalId;
    }

    public void setSucursalId(int sucursalId) {
        this.sucursalId = sucursalId;
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