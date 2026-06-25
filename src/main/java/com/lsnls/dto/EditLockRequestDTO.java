package com.lsnls.dto;

import lombok.Data;

@Data
public class EditLockRequestDTO {
    private String entityType;
    private Long entityId;
}
