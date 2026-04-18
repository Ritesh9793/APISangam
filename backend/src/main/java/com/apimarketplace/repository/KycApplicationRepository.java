package com.apimarketplace.repository;

import com.apimarketplace.entity.KycApplication;
import com.apimarketplace.entity.enums.KycStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KycApplicationRepository extends JpaRepository<KycApplication, UUID> {
    Optional<KycApplication> findByUserId(UUID userId);

    Optional<KycApplication> findFirstByVerificationReferenceIdsCsvContaining(String referenceId);

    List<KycApplication> findByStatusOrderByUpdatedAtDesc(KycStatus status);

    List<KycApplication> findByStatusInOrderByUpdatedAtDesc(List<KycStatus> statuses);
}
