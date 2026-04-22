package com.chat.chat.Repository;

import com.chat.chat.Entity.UploadFileMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UploadFileMetadataRepository extends JpaRepository<UploadFileMetadataEntity, Long> {
    Optional<UploadFileMetadataEntity> findByPublicUrl(String publicUrl);
}
