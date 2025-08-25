package com.lsnls.dto;

import java.util.List;

public class MarcarNoUsadoDTO {
    private Long jornadaId;
    private List<Long> cuestionarioIds;
    private List<Long> comboIds;
    private String motivo;

    public Long getJornadaId() { return jornadaId; }
    public void setJornadaId(Long jornadaId) { this.jornadaId = jornadaId; }
    
    public List<Long> getCuestionarioIds() { return cuestionarioIds; }
    public void setCuestionarioIds(List<Long> cuestionarioIds) { this.cuestionarioIds = cuestionarioIds; }
    
    public List<Long> getComboIds() { return comboIds; }
    public void setComboIds(List<Long> comboIds) { this.comboIds = comboIds; }
    
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
