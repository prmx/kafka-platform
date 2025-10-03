<!--
SYNC IMPACT REPORT - Constitution Update
=========================================
Version Change: Initial → 1.0.0
Modified Principles: N/A (initial creation)
Added Sections:
  - Core Principles (3 principles)
  - Technology Stack & Architecture Requirements
  - Development Standards
  - Governance

Templates Requiring Updates:
  ✅ .specify/templates/plan-template.md - Reviewed, aligns with constitution principles
  ✅ .specify/templates/spec-template.md - Reviewed, aligns with functional requirements approach
  ✅ .specify/templates/tasks-template.md - Reviewed, aligns with TDD and modular structure

Follow-up TODOs: None - all placeholders filled
-->

# Kafka Platform Constitution

## Core Principles

### I. Java 25 & Spring Boot Foundation (NON-NEGOTIABLE)
The platform MUST be built exclusively using Java 25 as the programming language and Spring Boot 3.x as the application framework. No alternative JVM languages or frameworks are permitted.

**Rationale**: Java 25 provides the latest language features, performance improvements, and security updates. Spring Boot offers production-ready features, dependency injection, autoconfiguration, and extensive ecosystem support for enterprise applications. This standardization ensures consistent development practices, predictable behavior, and long-term maintainability.

**Rules**:
- All source code MUST use Java 25 syntax and features
- Spring Boot MUST be used for application bootstrapping, configuration, and dependency management
- Spring Boot Starter dependencies MUST be used for all framework integrations
- The `pom.xml` MUST specify `<java.version>25</java.version>`
- Spring Boot version MUST be 3.x or higher (currently 3.5.6)

### II. Apache Kafka for Real-Time Messaging (NON-NEGOTIABLE)
All real-time price updates and event-driven communication MUST flow through Apache Kafka. No alternative messaging systems are permitted for price data distribution.

**Rationale**: Apache Kafka provides high-throughput, fault-tolerant, distributed streaming capabilities essential for real-time pricing platforms. Its publish-subscribe model, message persistence, and stream processing support make it the industry standard for financial data pipelines requiring millisecond latency and guaranteed delivery.

**Rules**:
- All price updates MUST be published to Kafka topics
- Spring Kafka MUST be used for producer and consumer implementations
- Kafka Streams MUST be used for stream processing requirements
- No synchronous REST/HTTP polling for price data is permitted
- Message schemas MUST be defined and versioned
- Consumer groups MUST be properly configured for scalability
- Dead letter queues MUST be implemented for error handling

### III. Multi-Module Maven Architecture (NON-NEGOTIABLE)
The project MUST follow a multi-module Maven structure to enforce modularity, separation of concerns, and independent component testing.

**Rationale**: Multi-module Maven projects enable clear boundaries between components, parallel development, selective builds, and independent versioning. For a pricing platform, this allows separation of concerns between pricing engines, data ingestion, API layers, and storage components while maintaining a unified build lifecycle.

**Rules**:
- The root `pom.xml` MUST define a parent project with module declarations
- Each logical component MUST reside in its own Maven module
- Modules MUST declare explicit dependencies in their `pom.xml`
- No cyclic dependencies between modules are permitted
- Shared models and utilities MUST reside in dedicated modules
- Each module MUST be independently testable
- Module naming MUST follow the pattern: `kafka-platform-{component-name}`

**Expected Module Structure** (minimum):
```
kafka-platform/                    (root parent POM)
├── kafka-platform-core/           (domain models, interfaces)
├── kafka-platform-messaging/      (Kafka producers/consumers)
├── kafka-platform-pricing/        (pricing logic and engines)
├── kafka-platform-api/            (REST API layer)
└── kafka-platform-storage/        (persistence layer)
```

## Technology Stack & Architecture Requirements

### Mandatory Dependencies
The following Spring Boot starters and libraries MUST be included:
- `spring-boot-starter-web` - REST API endpoints
- `spring-boot-starter-actuator` - Health checks and metrics
- `spring-kafka` - Kafka integration
- `kafka-streams` - Stream processing
- `spring-boot-starter-test` - Testing framework
- `spring-kafka-test` - Kafka testing utilities

### Optional but Recommended
- `spring-boot-starter-data-redis` - Caching layer for pricing data
- `lombok` - Boilerplate reduction (already included)
- `spring-boot-starter-validation` - Input validation

### Testing Requirements
- Unit tests MUST cover all business logic with minimum 80% coverage
- Integration tests MUST validate Kafka producer/consumer contracts
- Embedded Kafka MUST be used for integration testing
- Contract tests MUST validate API endpoints before implementation (TDD)
- Performance tests MUST validate <100ms p95 latency for price updates

### Observability Requirements
- Spring Boot Actuator endpoints MUST be exposed for monitoring
- Structured logging (JSON format) MUST be used for all logs
- Kafka consumer lag MUST be monitored
- Price update latency MUST be tracked and exposed as metrics

## Development Standards

### Code Quality
- Code MUST compile without warnings
- Static analysis (e.g., SonarLint, Checkstyle) MUST pass
- No use of deprecated APIs is permitted
- Exception handling MUST be comprehensive and logged
- Configuration MUST use `application.properties` or `application.yml`

### Versioning
- Semantic versioning MUST be followed: MAJOR.MINOR.PATCH
- MAJOR: Breaking API changes or incompatible Kafka message schema changes
- MINOR: New features, backward-compatible additions
- PATCH: Bug fixes, performance improvements

### Maven Build Requirements
- The project MUST build with `mvnw clean install` without errors
- All tests MUST pass during the build
- Maven Compiler Plugin MUST target Java 25
- Spring Boot Maven Plugin MUST be configured for executable JAR packaging

## Governance

### Constitutional Authority
This constitution supersedes all other development practices and guidelines. Any deviation from these principles MUST be documented with explicit justification in the `Complexity Tracking` section of the implementation plan.

### Amendment Process
1. Amendments MUST be proposed with clear rationale
2. All dependent templates (`.specify/templates/*.md`) MUST be reviewed for consistency
3. Version MUST be incremented according to semantic versioning rules
4. A Sync Impact Report MUST be generated and prepended to this file
5. All stakeholders MUST approve breaking changes (MAJOR version bumps)

### Compliance Verification
- All feature specifications MUST be reviewed against this constitution
- Implementation plans MUST include a "Constitution Check" section
- Code reviews MUST verify adherence to Java 25, Spring Boot, Kafka, and multi-module requirements
- CI/CD pipelines MUST enforce Java version, dependency restrictions, and test coverage

### Migration and Refactoring
- Any migration away from mandated technologies REQUIRES a constitutional amendment
- Legacy code violations MUST be tracked and remediated
- New features MUST NOT introduce architectural deviations

### Runtime Development Guidance
For agent-specific implementation guidance aligned with this constitution, refer to:
- `.github/copilot-instructions.md` (GitHub Copilot)
- `CLAUDE.md` (Claude Code)
- `GEMINI.md` (Gemini CLI)
- `QWEN.md` (Qwen Code)
- `AGENTS.md` (all other agents)

**Version**: 1.0.0 | **Ratified**: 2025-10-03 | **Last Amended**: 2025-10-03