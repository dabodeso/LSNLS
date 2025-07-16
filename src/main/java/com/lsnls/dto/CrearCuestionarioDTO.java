package com.lsnls.dto;

import java.util.List;

public class CrearCuestionarioDTO {
    private List<Long> preguntasNormales;
    private List<PreguntaMultiplicadoraDTO> preguntasMultiplicadoras;
    private String tematica;
    private String notasDireccion;

    public List<Long> getPreguntasNormales() { return preguntasNormales; }
    public void setPreguntasNormales(List<Long> preguntasNormales) { this.preguntasNormales = preguntasNormales; }
    public List<PreguntaMultiplicadoraDTO> getPreguntasMultiplicadoras() { return preguntasMultiplicadoras; }
    public void setPreguntasMultiplicadoras(List<PreguntaMultiplicadoraDTO> preguntasMultiplicadoras) { this.preguntasMultiplicadoras = preguntasMultiplicadoras; }
    public String getTematica() { return tematica; }
    public void setTematica(String tematica) { this.tematica = tematica; }
    public String getNotasDireccion() { return notasDireccion; }
    public void setNotasDireccion(String notasDireccion) { this.notasDireccion = notasDireccion; }

    public static class PreguntaMultiplicadoraDTO {
        private Long id;
        private String factor;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getFactor() { return factor; }
        public void setFactor(String factor) { this.factor = factor; }
    }

    // Utilidad para obtener el slot/hueco por índice y tipo
    public static String getSlotPorIndice(int idx, boolean esMultiplicadora) {
        if (!esMultiplicadora) {
            switch (idx) {
                case 0: return "1LS";
                case 1: return "2NLS";
                case 2: return "3LS";
                case 3: return "4NLS";
            }
        } else {
            switch (idx) {
                case 0: return "PM1";
                case 1: return "PM2";
                case 2: return "PM3";
            }
        }
        return null;
    }
} 