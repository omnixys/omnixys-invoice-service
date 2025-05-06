# 🧾 Omnixys Invoice Service

Der **Omnixys Invoice Service** ist ein Java-basierter Spring Boot Microservice zur Erstellung, Verwaltung und Archivierung von Rechnungen und Gutschriften. Er ist Teil des modularen Ökosystems **OmnixysSphere**, das auf Skalierbarkeit, Tracing, Sicherheit und Datenkonsistenz ausgelegt ist.

> *Powered by **OmnixysOS** – The Fabric of Modular Innovation.*

---

## ✨ Features

* Rechnungs- und Gutschriftenerstellung (PDF/JSON)
* Automatische Rechnungsnummerngenerierung
* Unterstützung mehrerer Rechnungstypen (Einzel-, Sammelrechnung)
* GraphQL API für CRUD & Suche
* Ereignisbasierte Kommunikation via Kafka
* Tracing (Tempo), Logging (Loki), Monitoring (Prometheus)
* Rollengestützte Zugriffskontrolle mit Keycloak

---

## ⚙️ Tech Stack

| Technologie   | Beschreibung                      |
| ------------- | --------------------------------- |
| Java          | Programmiersprache                |
| Spring Boot   | Framework für REST & GraphQL      |
| GraphQL       | Schnittstelle für APIs            |
| Kafka         | Messaging für Events              |
| PostgreSQL    | Persistente Datenspeicherung      |
| Keycloak      | Authentifizierung & Autorisierung |
| OpenTelemetry | Tracing & Monitoring              |
| Docker        | Containerisierung                 |

---

## 📦 Port

| Umgebung | Port   |
| -------- | ------ |
| Lokal    | `7202` |

> Siehe auch: [port-konvention.md](../port-konvention.md)

---

## 🛠️ Projektstruktur

```
src/main/java/com/omnixys/invoice/
├— controller/         # GraphQL Resolver & REST-Endpunkte
├— service/            # Business-Logik
├— model/              # Entitäten und DTOs
├— repository/         # Datenbankzugriffe (JPA)
├— kafka/              # Kafka Publisher & Consumer
├— config/             # Keycloak, Tracing, Logging etc.
└— Application.java    # Einstiegspunkt
```

---

## 🧪 Testen & Qualität

* Tests mit JUnit & Mockito
* Codeanalyse via SonarQube
* Coverage-Ziel: ≥ 80 %

---

## ▶️ Schnellstart

### 1. Klonen

```bash
git clone https://github.com/omnixys/omnixys-invoice-service.git
cd omnixys-invoice-service
```

### 2. Starten (lokal)

```bash
./gradlew bootRun
```

### 3. Oder via Docker

```bash
docker-compose up
```

---

## 🔐 Sicherheit

Dieser
