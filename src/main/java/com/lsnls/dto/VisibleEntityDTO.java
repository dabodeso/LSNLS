package com.lsnls.dto;

import lombok.Data;

@Data
public class VisibleEntityDTO {
    private String entityType;
    private Long entityId;
    private Long version;
}
