package com.lsnls.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReciclajeComboDTO {
    private Long jornadaId;
    private Long comboPadreId;
    private Long comboHijoId;
    private Long preguntaUsadaId;
}
