package com.omnixys.invoice.models.entitys;

import com.omnixys.invoice.models.enums.StatusType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Entity
@Table(name = "invoice")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Invoice {
    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Version
    private int version;

    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private StatusType status;
    private LocalDateTime dueDate;

    @Transient
    private List<UUID> payments;
    @Column(name = "payments")
    private String paymentsStr;

    @CreationTimestamp
    private LocalDateTime created;
    @UpdateTimestamp
    private LocalDateTime updated;

    private String username;

    /**
     * Fügt eine neue Zahlungs-ID zur Rechnung hinzu und aktualisiert die persistente Darstellung.
     *
     * @param paymentId Die ID der neuen Zahlung
     */
    public void addPayment(UUID paymentId) {
        if (this.payments == null || this.payments.isEmpty()) {
            this.payments = new java.util.ArrayList<>();
        }

        if (!this.payments.contains(paymentId)) {
            this.payments.add(paymentId);
        }

        buildPaymentsStr(); // wichtig: manuell aufrufen!
    }


    @PrePersist
    @PreUpdate
    private void buildPaymentsStr() {
        if (payments == null || payments.isEmpty()) {
            // NULL in der DB-Spalte
            paymentsStr = null;
            return;
        }
        final var stringList = payments.stream()
            .map(UUID::toString)
            .toList();
        paymentsStr = String.join(",", stringList);
    }

    @PostLoad
    @SuppressWarnings("java:S6204")
    private void loadPayments() {
        if (paymentsStr == null) {
            // NULL in der DB-Spalte
            payments = emptyList();
            return;
        }
        final var paymentsArray = paymentsStr.split(",");
        payments = Arrays.stream(paymentsArray)
            .map(UUID::fromString)
            .collect(Collectors.toList());
    }
}
