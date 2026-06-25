package com.lsnls.repository;

import com.lsnls.entity.AuditLog;
import com.lsnls.entity.EditLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EditLockRepository extends JpaRepository<EditLock, Long> {

    Optional<EditLock> findByEntityTypeAndEntityId(AuditLog.EntityType entityType, Long entityId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EditLock e WHERE e.entityType = :entityType AND e.entityId = :entityId")
    Optional<EditLock> findForUpdate(@Param("entityType") AuditLog.EntityType entityType,
                                     @Param("entityId") Long entityId);

    void deleteByEntityTypeAndEntityId(AuditLog.EntityType entityType, Long entityId);

    @Modifying
    @Query("DELETE FROM EditLock e WHERE e.expiresAt < :now")
    int deleteExpiredBefore(@Param("now") LocalDateTime now);
}
