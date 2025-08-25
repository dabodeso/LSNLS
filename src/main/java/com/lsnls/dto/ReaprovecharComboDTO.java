package com.lsnls.dto;

import java.util.List;

public class ReaprovecharComboDTO {
    private Long comboOriginalId;
    private Long preguntaUsadaId;
    private List<Long> preguntasNoUsadasIds;
    private Long nuevaPreguntaId;
    private String factorMultiplicacion;
    private String notas;

    public Long getComboOriginalId() { return comboOriginalId; }
    public void setComboOriginalId(Long comboOriginalId) { this.comboOriginalId = comboOriginalId; }
    
    public Long getPreguntaUsadaId() { return preguntaUsadaId; }
    public void setPreguntaUsadaId(Long preguntaUsadaId) { this.preguntaUsadaId = preguntaUsadaId; }
    
    public List<Long> getPreguntasNoUsadasIds() { return preguntasNoUsadasIds; }
    public void setPreguntasNoUsadasIds(List<Long> preguntasNoUsadasIds) { this.preguntasNoUsadasIds = preguntasNoUsadasIds; }
    
    public Long getNuevaPreguntaId() { return nuevaPreguntaId; }
    public void setNuevaPreguntaId(Long nuevaPreguntaId) { this.nuevaPreguntaId = nuevaPreguntaId; }
    
    public String getFactorMultiplicacion() { return factorMultiplicacion; }
    public void setFactorMultiplicacion(String factorMultiplicacion) { this.factorMultiplicacion = factorMultiplicacion; }
    
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
}
