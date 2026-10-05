package com.flashjobweb.repository;

import com.flashjobweb.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {
    List<MessageEntity> findByApplicationIdOrderBySentAtAsc(UUID applicationId);
    long countByApplicationIdAndIsReadFalseAndSenderIdNot(UUID applicationId, UUID senderId);
}
