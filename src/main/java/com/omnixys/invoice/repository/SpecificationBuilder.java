package com.omnixys.invoice.repository;

import com.omnixys.invoice.models.entitys.Invoice;
import com.omnixys.invoice.models.entitys.Invoice_;
import com.omnixys.invoice.models.enums.InfoType;
import com.omnixys.invoice.models.enums.StatusType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class SpecificationBuilder {

  /**
   * Erstellt eine JPA-Spezifikation basierend auf den Suchkriterien.
   *
   * @param queryParams Suchkriterien als Map
   * @return Optionale Spezifikation für die Filterung von Rechnungen
   */
  public Optional<Specification<Invoice>> build(final Map<String, ? extends List<Object>> queryParams) {
    log.debug("build: queryParams={}", queryParams);

    if (queryParams.isEmpty()) {
      // No search criteria provided
      return Optional.empty();
    }

    final var specs = queryParams
      .entrySet()
      .stream()
      .map(this::toSpecification)
      .toList();

    if (specs.isEmpty() || specs.contains(null)) {
      return Optional.empty();
    }

    return Optional.of(Specification.allOf(specs));
  }

  /**
   * Erstellt eine OR-verknüpfte JPA-Spezifikation für die gegebenen Schlüssel.
   */
  public Optional<Specification<Invoice>> buildOr(final Map<String, ? extends List<Object>> queryParams, List<String> orKeys) {
    log.debug("buildOr: orKeys={} queryParams={}", orKeys, queryParams);
    if (queryParams.isEmpty() || orKeys.isEmpty()) return Optional.empty();

    var specs = orKeys.stream()
        .map(key -> Optional.ofNullable(queryParams.get(key))
            .map(val -> Map.entry(key, val)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(this::toSpecification)
        .filter(Objects::nonNull)
        .toList();

    return specs.isEmpty() ? Optional.empty() : Optional.of(Specification.anyOf(specs));
  }

  /**
   * Wandelt ein Suchkriterium in eine JPA-Spezifikation um.
   */
  private Specification<Invoice> toSpecification(final Map.Entry<String, ? extends List<Object>> entry) {
    log.trace("toSpec: entry={}", entry);
    final var key = entry.getKey();
    final var values = entry.getValue();

    if (values == null || values.size() != 1) {
      return null;
    }

    final var value = values.getFirst();
    return switch (key) {
      case "infoType" -> infoType(value.toString());
      case "status" -> status(value.toString());
      case "issuedBy" -> personMatch("issuedBy", value.toString());
      case "billedTo" -> personMatch("billedTo", value.toString());
      case "minAmount" -> minAmount(value.toString());
      case "maxAmount" -> maxAmount(value.toString());
      case "dueBefore" -> before(value.toString());
      case "dueAfter" -> after(value.toString());
      default -> {
        log.warn("Unbekanntes Suchkriterium: {}", key);
        yield null;
      }
    };
  }

  /**
   * Erstellt eine Spezifikation für das InfoType-Feld.
   */
  private Specification<Invoice> infoType(final String value) {
    try {
      InfoType type = InfoType.valueOf(value.toUpperCase());
      return (root, query, builder) -> builder.equal(root.get("infoType"), type);
    } catch (IllegalArgumentException e) {
      log.error("Ungültiger InfoType: {}", value);
      return null;
    }
  }

  /**
   * Erstellt eine Spezifikation für das StatusType-Feld.
   */
  private Specification<Invoice> status(final String value) {
    try {
    return (root, _, builder) -> builder.equal(
        root.get(Invoice_.status),
        StatusType.of(value)
    );
    } catch (IllegalArgumentException e) {
      log.error("Ungültiger StatusType: {}", value);
      return null;
    }
  }

  private Specification<Invoice> personMatch(String field, String uuid) {
    try {
      UUID id = UUID.fromString(uuid);
      return (root, query, cb) -> cb.equal(root.get(field), id);
    } catch (IllegalArgumentException e) {
      log.error("Ungültige UUID für {}: {}", field, uuid);
      return null;
    }
  }

  private Specification<Invoice> before(String value) {
    try {
      var date = LocalDateTime.parse(value);
      return (root, query, cb) -> cb.lessThan(root.get("dueDate"), date);
    } catch (DateTimeParseException e) {
      log.error("Ungültiges Datumsformat (before): {}", value);
      return null;
    }
  }

  private Specification<Invoice> after(String value) {
    try {
      var date = LocalDateTime.parse(value);
      return (root, query, cb) -> cb.greaterThan(root.get("dueDate"), date);
    } catch (DateTimeParseException e) {
      log.error("Ungültiges Datumsformat (after): {}", value);
      return null;
    }
  }

  private Specification<Invoice> minAmount(String value) {
    try {
      var amount = new BigDecimal(value);
      return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("amount"), amount);
    } catch (NumberFormatException e) {
      log.error("Ungültiger minAmount: {}", value);
      return null;
    }
  }

  private Specification<Invoice> maxAmount(String value) {
    try {
      var amount = new BigDecimal(value);
      return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("amount"), amount);
    } catch (NumberFormatException e) {
      log.error("Ungültiger maxAmount: {}", value);
      return null;
    }
  }
}
