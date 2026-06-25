package com.lsnls.controller;

import com.lsnls.dto.EditLockRequestDTO;
import com.lsnls.dto.VisibleEntityDTO;
import com.lsnls.entity.AuditLog;
import com.lsnls.service.EditLockService;
import com.lsnls.service.SyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/edit-locks")
@CrossOrigin(origins = "*")
public class EditLockController {

    @Autowired
    private EditLockService editLockService;

    @PostMapping("/acquire")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<Map<String, Object>> acquire(@RequestBody EditLockRequestDTO request) {
        AuditLog.EntityType type = EditLockService.parseEntityType(request.getEntityType());
        return ResponseEntity.ok(editLockService.acquire(type, request.getEntityId()));
    }

    @PostMapping("/renew")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<Map<String, Object>> renew(@RequestBody EditLockRequestDTO request) {
        AuditLog.EntityType type = EditLockService.parseEntityType(request.getEntityType());
        return ResponseEntity.ok(editLockService.renew(type, request.getEntityId()));
    }

    @PostMapping("/release")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<Void> release(@RequestBody EditLockRequestDTO request) {
        AuditLog.EntityType type = EditLockService.parseEntityType(request.getEntityType());
        editLockService.release(type, request.getEntityId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<Map<String, Object>> status(@RequestParam String entityType, @RequestParam Long entityId) {
        AuditLog.EntityType type = EditLockService.parseEntityType(entityType);
        return ResponseEntity.ok(editLockService.status(type, entityId));
    }
}
