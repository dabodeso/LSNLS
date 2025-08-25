package com.lsnls.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

public class HistorialJornadaDTO {
    private Long id;
    private Long jornadaId;
    private String jornadaNombre;
    private Long cuestionarioId;
    private Long comboId;
    private String tipoAsignacion;
    private String estadoAsignacion;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaAsignacion;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaUso;
    
    private Long preguntaUsadaId;
    private String notas;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getJornadaId() { return jornadaId; }
    public void setJornadaId(Long jornadaId) { this.jornadaId = jornadaId; }
    
    public String getJornadaNombre() { return jornadaNombre; }
    public void setJornadaNombre(String jornadaNombre) { this.jornadaNombre = jornadaNombre; }
    
    public Long getCuestionarioId() { return cuestionarioId; }
    public void setCuestionarioId(Long cuestionarioId) { this.cuestionarioId = cuestionarioId; }
    
    public Long getComboId() { return comboId; }
    public void setComboId(Long comboId) { this.comboId = comboId; }
    
    public String getTipoAsignacion() { return tipoAsignacion; }
    public void setTipoAsignacion(String tipoAsignacion) { this.tipoAsignacion = tipoAsignacion; }
    
    public String getEstadoAsignacion() { return estadoAsignacion; }
    public void setEstadoAsignacion(String estadoAsignacion) { this.estadoAsignacion = estadoAsignacion; }
    
    public LocalDateTime getFechaAsignacion() { return fechaAsignacion; }
    public void setFechaAsignacion(LocalDateTime fechaAsignacion) { this.fechaAsignacion = fechaAsignacion; }
    
    public LocalDateTime getFechaUso() { return fechaUso; }
    public void setFechaUso(LocalDateTime fechaUso) { this.fechaUso = fechaUso; }
    
    public Long getPreguntaUsadaId() { return preguntaUsadaId; }
    public void setPreguntaUsadaId(Long preguntaUsadaId) { this.preguntaUsadaId = preguntaUsadaId; }
    
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
}
