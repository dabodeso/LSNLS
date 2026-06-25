package com.lsnls.controller;

import com.lsnls.dto.VisibleEntityDTO;
import com.lsnls.service.SyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
@CrossOrigin(origins = "*")
public class SyncController {

    @Autowired
    private SyncService syncService;

    @PostMapping("/visible-changes")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<Map<String, Object>> visibleChanges(@RequestBody List<VisibleEntityDTO> items) {
        return ResponseEntity.ok(Map.of("changes", syncService.checkVisibleChanges(items)));
    }
}
