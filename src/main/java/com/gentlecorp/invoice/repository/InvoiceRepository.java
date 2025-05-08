package com.omnixys.invoice.repository;

import com.omnixys.invoice.models.entitys.Invoice;
import com.omnixys.invoice.models.enums.StatusType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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

    List<Invoice> findByStatus(@NonNull StatusType statusType);
    List<Invoice> findByUsername(@NonNull String username);
}

