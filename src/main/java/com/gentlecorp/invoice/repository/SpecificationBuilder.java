package com.gentlecorp.invoice.repository;

import com.gentlecorp.invoice.models.entitys.Invoice;
import com.gentlecorp.invoice.models.entitys.Invoice_;
import com.gentlecorp.invoice.models.enums.InfoType;
import com.gentlecorp.invoice.models.enums.StatusType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
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
      case "customerId" -> customerId(value.toString());
      case "dueDate" -> dueDate(value.toString());
      case "created" -> created(value.toString());
      case "updated" -> updated(value.toString());
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

  /**
   * Erstellt eine Spezifikation für das customerId-Feld.
   */
  private Specification<Invoice> customerId(final String value) {
    try {
      UUID customerUUID = UUID.fromString(value);
      return (root, query, builder) -> builder.equal(root.get("customerId"), customerUUID);
    } catch (IllegalArgumentException e) {
      log.error("Ungültiges UUID-Format für customerId: {}", value);
      return null;
    }
  }

  /**
   * Erstellt eine Spezifikation für das dueDate-Feld.
   */
  private Specification<Invoice> dueDate(final String value) {
    try {
      LocalDateTime date = LocalDateTime.parse(value);
      return (root, query, builder) -> builder.equal(root.get("dueDate"), date);
    } catch (DateTimeParseException e) {
      log.error("Ungültiges Datumsformat für dueDate: {}", value);
      return null;
    }
  }

  /**
   * Erstellt eine Spezifikation für das created-Feld.
   */
  private Specification<Invoice> created(final String value) {
    try {
      LocalDateTime date = LocalDateTime.parse(value);
      return (root, query, builder) -> builder.equal(root.get("created"), date);
    } catch (DateTimeParseException e) {
      log.error("Ungültiges Datumsformat für created: {}", value);
      return null;
    }
  }

  /**
   * Erstellt eine Spezifikation für das updated-Feld.
   */
  private Specification<Invoice> updated(final String value) {
    try {
      LocalDateTime date = LocalDateTime.parse(value);
      return (root, query, builder) -> builder.equal(root.get("updated"), date);
    } catch (DateTimeParseException e) {
      log.error("Ungültiges Datumsformat für updated: {}", value);
      return null;
    }
  }
}
