package com.lsnls.dto;

import java.util.List;

public class CrearComboDTO {
    private List<PreguntaMultiplicadoraDTO> preguntasMultiplicadoras;
    private String tipo;

    public List<PreguntaMultiplicadoraDTO> getPreguntasMultiplicadoras() { 
        return preguntasMultiplicadoras; 
    }
    
    public void setPreguntasMultiplicadoras(List<PreguntaMultiplicadoraDTO> preguntasMultiplicadoras) { 
        this.preguntasMultiplicadoras = preguntasMultiplicadoras; 
    }

    public String getTipo() { 
        return tipo; 
    }
    
    public void setTipo(String tipo) { 
        this.tipo = tipo; 
    }

    public static class PreguntaMultiplicadoraDTO {
        private Long id;
        private String factor;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getFactor() { return factor; }
        public void setFactor(String factor) { this.factor = factor; }
    }

    // Utilidad para obtener el slot/hueco por índice
    public static String getSlotPorIndice(int idx) {
        switch (idx) {
            case 0: return "PM1";
            case 1: return "PM2";
            case 2: return "PM3";
        }
        return null;
    }
} 