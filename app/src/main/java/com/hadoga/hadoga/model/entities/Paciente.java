package com.hadoga.hadoga.model.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(
        tableName = "paciente",
        foreignKeys = @ForeignKey(
                entity = Sucursal.class,
                parentColumns = "codigo_sucursal",
                childColumns = "codigo_sucursal_asignada",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index(value = "codigo_sucursal_asignada"),
                @Index(value = "correo_electronico", unique = true)
        }
)
public class Paciente implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "nombre")
    private String nombre;

    @ColumnInfo(name = "apellido")
    private String apellido;

    @ColumnInfo(name = "fecha_nacimiento")
    private String fechaNacimiento;

    @ColumnInfo(name = "sexo")
    private String sexo;

    @ColumnInfo(name = "correo_electronico")
    private String correoElectronico;

    @ColumnInfo(name = "numero_telefono")
    private String numeroTelefono;

    @ColumnInfo(name = "direccion")
    private String direccion;

    @ColumnInfo(name = "observaciones")
    private String observaciones;

    @ColumnInfo(name = "diabetes")
    private boolean diabetes;

    @ColumnInfo(name = "anemia")
    private boolean anemia;

    @ColumnInfo(name = "gastritis")
    private boolean gastritis;

    @ColumnInfo(name = "hipertension_hta")
    private boolean hipertensionHta;

    @ColumnInfo(name = "hemorragias")
    private boolean hemorragias;

    @ColumnInfo(name = "asma")
    private boolean asma;

    @ColumnInfo(name = "trastornos_cardiacos")
    private boolean trastornosCardiacos;

    @ColumnInfo(name = "convulsiones")
    private boolean convulsiones;

    @ColumnInfo(name = "tiroides")
    private boolean tiroides;

    @ColumnInfo(name = "codigo_sucursal_asignada")
    private String codigoSucursalAsignada;

    @ColumnInfo(name = "foto_uri")
    private String fotoUri;

    @ColumnInfo(name = "estado_sincronizacion")
    private String estadoSincronizacion = "PENDIENTE";

    // Constructor principal
    public Paciente(String nombre, String apellido, String fechaNacimiento, String sexo,
                    String correoElectronico, String numeroTelefono, String direccion,
                    String observaciones, boolean diabetes, boolean anemia, boolean gastritis,
                    boolean hipertensionHta, boolean hemorragias, boolean asma,
                    boolean trastornosCardiacos, boolean convulsiones, boolean tiroides,
                    String codigoSucursalAsignada, String fotoUri) {

        this.nombre = nombre;
        this.apellido = apellido;
        this.fechaNacimiento = fechaNacimiento;
        this.sexo = sexo;
        this.correoElectronico = correoElectronico;
        this.numeroTelefono = numeroTelefono;
        this.direccion = direccion;
        this.observaciones = observaciones;
        this.diabetes = diabetes;
        this.anemia = anemia;
        this.gastritis = gastritis;
        this.hipertensionHta = hipertensionHta;
        this.hemorragias = hemorragias;
        this.asma = asma;
        this.trastornosCardiacos = trastornosCardiacos;
        this.convulsiones = convulsiones;
        this.tiroides = tiroides;
        this.codigoSucursalAsignada = codigoSucursalAsignada;
        this.fotoUri = fotoUri;
    }

    // Constructor vacío
    public Paciente() {
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

    public String getSexo() {
        return sexo;
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

    public String getCorreoElectronico() {
        return correoElectronico;
    }

    public void setCorreoElectronico(String correoElectronico) {
        this.correoElectronico = correoElectronico;
    }

    public String getNumeroTelefono() {
        return numeroTelefono;
    }

    public void setNumeroTelefono(String numeroTelefono) {
        this.numeroTelefono = numeroTelefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public boolean isDiabetes() {
        return diabetes;
    }

    public void setDiabetes(boolean diabetes) {
        this.diabetes = diabetes;
    }

    public boolean isAnemia() {
        return anemia;
    }

    public void setAnemia(boolean anemia) {
        this.anemia = anemia;
    }

    public boolean isGastritis() {
        return gastritis;
    }

    public void setGastritis(boolean gastritis) {
        this.gastritis = gastritis;
    }

    public boolean isHipertensionHta() {
        return hipertensionHta;
    }

    public void setHipertensionHta(boolean hipertensionHta) {
        this.hipertensionHta = hipertensionHta;
    }

    public boolean isHemorragias() {
        return hemorragias;
    }

    public void setHemorragias(boolean hemorragias) {
        this.hemorragias = hemorragias;
    }

    public boolean isAsma() {
        return asma;
    }

    public void setAsma(boolean asma) {
        this.asma = asma;
    }

    public boolean isTrastornosCardiacos() {
        return trastornosCardiacos;
    }

    public void setTrastornosCardiacos(boolean trastornosCardiacos) {
        this.trastornosCardiacos = trastornosCardiacos;
    }

    public boolean isConvulsiones() {
        return convulsiones;
    }

    public void setConvulsiones(boolean convulsiones) {
        this.convulsiones = convulsiones;
    }

    public boolean isTiroides() {
        return tiroides;
    }

    public void setTiroides(boolean tiroides) {
        this.tiroides = tiroides;
    }

    public String getCodigoSucursalAsignada() {
        return codigoSucursalAsignada;
    }

    public void setCodigoSucursalAsignada(String codigoSucursalAsignada) {
        this.codigoSucursalAsignada = codigoSucursalAsignada;
    }

    public String getEstadoSincronizacion() {
        return estadoSincronizacion;
    }

    public void setEstadoSincronizacion(String estadoSincronizacion) {
        this.estadoSincronizacion = estadoSincronizacion;
    }

    public String getFotoUri() {
        return fotoUri;
    }

    public void setFotoUri(String fotoUri) {
        this.fotoUri = fotoUri;
    }
}