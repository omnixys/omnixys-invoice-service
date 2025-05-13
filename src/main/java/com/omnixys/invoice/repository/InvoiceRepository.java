package com.omnixys.invoice.repository;

import com.omnixys.invoice.models.entitys.Invoice;
import com.omnixys.invoice.models.enums.StatusType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {
    @NonNull
    @Override
    List<Invoice> findAll();

    @NonNull
    @Override
    List<Invoice> findAll (Specification<Invoice> spec);

    @NonNull
    @Override
    Optional<Invoice> findById(@NonNull UUID id);

    @Query("""
SELECT i FROM Invoice i
WHERE (i.issuedBy = :id OR i.billedTo = :id)
AND (:status IS NULL OR i.status = :status)
""")
    List<Invoice> findByIssuedByOrBilledToAndOptionalStatus(UUID id, UUID id2, StatusType status);

    List<Invoice> findByIssuedByOrBilledTo(UUID issuedBy, UUID billedTo);

    List<Invoice> findByIssuedByAndStatus(UUID personId, StatusType status);

    List<Invoice> findByBilledToAndStatus(UUID personId, StatusType status);
}

