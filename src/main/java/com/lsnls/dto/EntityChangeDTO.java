package com.lsnls.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityChangeDTO {
    private String entityType;
    private Long entityId;
    private String entityLabel;
    private String usuarioNombre;
    private Long version;
    private String mensaje;
}
