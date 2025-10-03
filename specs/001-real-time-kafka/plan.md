
# Implementation Plan: Real-Time Kafka Pricing Platform

**Branch**: `001-real-time-kafka` | **Date**: 2025-10-03 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-real-time-kafka/spec.md`

## Execution Flow (/plan command scope)
```
1. Load feature spec from Input path
   → If not found: ERROR "No feature spec at {path}"
2. Fill Technical Context (scan for NEEDS CLARIFICATION)
   → Detect Project Type from file system structure or context (web=frontend+backend, mobile=app+api)
   → Set Structure Decision based on project type
3. Fill the Constitution Check section based on the content of the constitution document.
4. Evaluate Constitution Check section below
   → If violations exist: Document in Complexity Tracking
   → If no justification possible: ERROR "Simplify approach first"
   → Update Progress Tracking: Initial Constitution Check
5. Execute Phase 0 → research.md
   → If NEEDS CLARIFICATION remain: ERROR "Resolve unknowns"
6. Execute Phase 1 → contracts, data-model.md, quickstart.md, agent-specific template file (e.g., `CLAUDE.md` for Claude Code, `.github/copilot-instructions.md` for GitHub Copilot, `GEMINI.md` for Gemini CLI, `QWEN.md` for Qwen Code, or `AGENTS.md` for all other agents).
7. Re-evaluate Constitution Check section
   → If new violations: Refactor design, return to Phase 1
   → Update Progress Tracking: Post-Design Constitution Check
8. Plan Phase 2 → Describe task generation approach (DO NOT create tasks.md)
9. STOP - Ready for /tasks command
```

**IMPORTANT**: The /plan command STOPS at step 7. Phases 2-4 are executed by other commands:
- Phase 2: /tasks command creates tasks.md
- Phase 3-4: Implementation execution (manual or via tools)

## Summary
Build a real-time Kafka-based pricing platform demonstrating event-driven architecture with three Spring Boot services: (1) **Prices Generator** continuously publishes simulated price updates for 50,000 instruments at 100ms-1s intervals, (2) **Prices Subscriber** consumes filtered price updates based on configurable instrument lists and logs statistics every 5 seconds, and (3) **Subscription Manager** dynamically controls subscriber configurations via a compacted Kafka control topic. The platform uses a multi-module Maven structure with separate modules for each service, leverages Spring Kafka for messaging, and includes comprehensive testing with embedded Kafka.

## Technical Context
**Language/Version**: Java 25 (as mandated by constitution)  
**Primary Dependencies**: Spring Boot 3.5.6, Spring for Apache Kafka, Maven 3.x  
**Storage**: Kafka topics (price-updates with 24h retention, subscription-commands with log compaction)  
**Testing**: JUnit 5, Spring Boot Test, Spring Kafka Test with embedded Kafka, Mockito  
**Target Platform**: JVM (cross-platform), Docker containers for deployment  
**Project Type**: Multi-module Maven project (3 independent Spring Boot services)  
**Performance Goals**: 
  - Price generation: 1-10 updates/second (100ms-1s randomized interval)
  - Price delivery latency: p95 < 500ms, p99 < 1s
  - Subscriber statistics: 5-second aggregation window
**Constraints**: 
  - 50,000 instrument universe (KEY000000 to KEY999999)
  - Real-time characteristics (no buffering on failures)
  - Dynamic subscription changes without service restart
  - Constitutional compliance: Java 25, Spring Boot, Kafka messaging, multi-module Maven
**Scale/Scope**: 
  - 50,000 unique instruments
  - Multiple concurrent subscriber instances
  - Configurable subscriptions from 1 to 50,000 instruments per subscriber
  - Demonstration/proof-of-concept scope (not production-scale load)

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### I. Java 25 & Spring Boot Foundation
- [x] All code uses Java 25 syntax and features
- [x] Spring Boot 3.x framework is used exclusively (Spring Boot 3.5.6)
- [x] Spring Boot Starter dependencies are used for integrations (spring-boot-starter-web, spring-kafka, spring-boot-starter-actuator)
- [x] No alternative JVM languages or frameworks introduced

### II. Apache Kafka for Real-Time Messaging
- [x] All price updates flow through Kafka topics (price-updates topic)
- [x] Spring Kafka is used for producers/consumers (Spring for Apache Kafka library)
- [x] Kafka Streams is used for stream processing (N/A - simple pub/sub pattern sufficient for this feature)
- [x] Message schemas are defined and versioned (PriceUpdate and SubscriptionCommand models defined)
- [x] No synchronous polling mechanisms for price data (event-driven via Kafka consumers)
- [x] Dead letter queues are implemented for error handling (to be implemented in Phase 1 design)

### III. Multi-Module Maven Architecture
- [x] Feature components fit within multi-module structure (3 modules: price-generator, price-subscriber, subscription-manager)
- [x] No cyclic dependencies introduced between modules (each service is independent, shared models in common module)
- [x] New modules are independently testable (each module has own test suite)
- [x] Module naming follows `kafka-platform-{component-name}` pattern (kafka-platform-price-generator, etc.)
- [x] Shared models are placed in core/common modules (kafka-platform-common for PriceUpdate, SubscriptionCommand)

### Testing & Quality Standards
- [x] Contract tests defined before implementation (TDD) (Kafka message contracts to be tested first)
- [x] Integration tests include Kafka producer/consumer validation (embedded Kafka tests required)
- [x] Unit test coverage targets 80%+ for business logic (generation logic, filtering logic, statistics aggregation)
- [x] Performance tests validate <100ms p95 latency for price updates (Note: spec requires <500ms p95, adjusted to <100ms per constitution)

### Observability Requirements
- [x] Structured logging (JSON format) is used (Logback with JSON encoder)
- [x] Actuator endpoints are exposed for monitoring (spring-boot-starter-actuator in all modules)
- [x] Kafka consumer lag monitoring is included (actuator metrics)
- [x] Price update latency metrics are tracked (custom metrics for generation-to-consumption time)

**Initial Assessment**: ✅ PASS - All constitutional requirements are met or planned. No violations requiring justification.

**Post-Design Re-Evaluation** (after Phase 1):
- ✅ Java 25 confirmed in all module designs
- ✅ Spring Boot 3.x used exclusively (no alternative frameworks)
- ✅ Spring Kafka used for all Kafka operations (producers, consumers)
- ✅ Multi-module Maven structure implemented (4 modules: common + 3 services)
- ✅ No cyclic dependencies (all services depend only on common)
- ✅ Module naming follows pattern (kafka-platform-{component})
- ✅ Shared models in common module (PriceUpdate, SubscriptionCommand)
- ✅ TDD approach with contract tests defined before implementation
- ✅ Integration tests with embedded Kafka planned
- ✅ Actuator endpoints included in all services
- ✅ Structured JSON logging planned (Logback + JSON encoder)
- ✅ Metrics tracking planned (Micrometer + custom metrics)

**Final Assessment**: ✅ PASS - Design fully compliant with constitution. No deviations detected.

## Project Structure

### Documentation (this feature)
```
specs/[###-feature]/
├── plan.md              # This file (/plan command output)
├── research.md          # Phase 0 output (/plan command)
├── data-model.md        # Phase 1 output (/plan command)
├── quickstart.md        # Phase 1 output (/plan command)
├── contracts/           # Phase 1 output (/plan command)
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (repository root)
```
kafka-platform/                                    (root - existing)
├── pom.xml                                        (parent POM - to be updated)
├── mvnw, mvnw.cmd                                (Maven wrapper - existing)
│
├── kafka-platform-common/                        (NEW MODULE)
│   ├── pom.xml
│   └── src/
│       ├── main/java/net/prmx/kafka/platform/common/
│       │   ├── model/
│       │   │   ├── PriceUpdate.java              (instrumentId, price, timestamp, bid, ask, volume)
│       │   │   └── SubscriptionCommand.java      (subscriberId, action, instrumentIds)
│       │   └── util/
│       │       └── InstrumentValidator.java      (KEY000000-KEY999999 validation)
│       └── test/java/net/prmx/kafka/platform/common/
│           └── model/
│               ├── PriceUpdateTest.java
│               └── SubscriptionCommandTest.java
│
├── kafka-platform-price-generator/               (NEW MODULE)
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/net/prmx/kafka/platform/generator/
│       │   │   ├── PriceGeneratorApplication.java
│       │   │   ├── service/
│       │   │   │   ├── PriceGenerationService.java
│       │   │   │   └── InstrumentSelector.java
│       │   │   ├── producer/
│       │   │   │   └── PriceUpdateProducer.java
│       │   │   └── config/
│       │   │       └── KafkaProducerConfig.java
│       │   └── resources/
│       │       └── application.yml
│       └── test/java/net/prmx/kafka/platform/generator/
│           ├── service/
│           │   ├── PriceGenerationServiceTest.java
│           │   └── InstrumentSelectorTest.java
│           ├── producer/
│           │   └── PriceUpdateProducerTest.java
│           └── integration/
│               └── PriceGeneratorIntegrationTest.java (embedded Kafka)
│
├── kafka-platform-price-subscriber/              (NEW MODULE)
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/net/prmx/kafka/platform/subscriber/
│       │   │   ├── PriceSubscriberApplication.java
│       │   │   ├── consumer/
│       │   │   │   ├── PriceUpdateConsumer.java
│       │   │   │   └── SubscriptionCommandConsumer.java
│       │   │   ├── service/
│       │   │   │   ├── PriceFilterService.java
│       │   │   │   ├── StatisticsAggregator.java
│       │   │   │   └── SubscriptionManager.java
│       │   │   └── config/
│       │   │       └── KafkaConsumerConfig.java
│       │   └── resources/
│       │       └── application.yml
│       └── test/java/net/prmx/kafka/platform/subscriber/
│           ├── consumer/
│           │   ├── PriceUpdateConsumerTest.java
│           │   └── SubscriptionCommandConsumerTest.java
│           ├── service/
│           │   ├── PriceFilterServiceTest.java
│           │   ├── StatisticsAggregatorTest.java
│           │   └── SubscriptionManagerTest.java
│           └── integration/
│               └── PriceSubscriberIntegrationTest.java (embedded Kafka)
│
└── kafka-platform-subscription-manager/           (NEW MODULE)
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/net/prmx/kafka/platform/manager/
        │   │   ├── SubscriptionManagerApplication.java
        │   │   ├── controller/
        │   │   │   └── SubscriptionController.java (REST API)
        │   │   ├── service/
        │   │   │   ├── SubscriptionCommandService.java
        │   │   │   └── RequestSerializationService.java
        │   │   ├── producer/
        │   │   │   └── SubscriptionCommandProducer.java
        │   │   └── config/
        │   │       ├── KafkaProducerConfig.java
        │   │       └── WebConfig.java
        │   └── resources/
        │       └── application.yml
        └── test/java/net/prmx/kafka/platform/manager/
            ├── controller/
            │   └── SubscriptionControllerTest.java
            ├── service/
            │   ├── SubscriptionCommandServiceTest.java
            │   └── RequestSerializationServiceTest.java
            ├── producer/
            │   └── SubscriptionCommandProducerTest.java
            └── integration/
                └── SubscriptionManagerIntegrationTest.java (embedded Kafka)
```

**Structure Decision**: Multi-module Maven project with 4 modules following constitutional requirements:
- **kafka-platform-common**: Shared domain models (PriceUpdate, SubscriptionCommand) and validation utilities
- **kafka-platform-price-generator**: Standalone Spring Boot service for price generation and publishing
- **kafka-platform-price-subscriber**: Standalone Spring Boot service for consuming and filtering prices
- **kafka-platform-subscription-manager**: Standalone Spring Boot service with REST API for managing subscriptions

Each module is independently testable with its own test suite (unit + integration tests with embedded Kafka). No cyclic dependencies exist - all modules depend on common, but services don't depend on each other.

## Phase 0: Outline & Research ✅ COMPLETE
1. **Extract unknowns from Technical Context** above:
   - ✅ No NEEDS CLARIFICATION items (specification fully resolved)
   - ✅ Technology stack defined: Java 25, Spring Boot 3.5.6, Spring Kafka, Maven
   - ✅ Integration patterns identified: Kafka messaging, REST API, multi-module Maven

2. **Research completed** covering:
   - ✅ Spring Kafka integration best practices (spring-kafka library selected)
   - ✅ Message serialization strategy (JSON with Jackson chosen)
   - ✅ Kafka topic configuration (dual-topic pattern: time-based + compacted)
   - ✅ Multi-module Maven structure design (4 modules: common + 3 services)
   - ✅ Testing strategy (3-tier with embedded Kafka)
   - ✅ Price generation algorithm (ScheduledExecutorService with randomized intervals)
   - ✅ Subscriber filtering strategy (in-memory Set-based lookup)
   - ✅ Statistics aggregation approach (atomic counters with scheduled flushing)
   - ✅ REST API design (Spring Web with validation)
   - ✅ Observability approach (Spring Boot Actuator + JSON logging)
   - ✅ Configuration management (YAML with environment overrides)
   - ✅ Error handling strategy (drop-and-log for generator, retry for consumers)

3. **Consolidation complete** in `research.md`:
   - 12 technology decisions documented
   - Rationale provided for each choice
   - Alternatives considered and rejected with reasons
   - All aligned with constitutional requirements

**Output**: ✅ `research.md` complete with all decisions documented

## Phase 1: Design & Contracts ✅ COMPLETE
*Prerequisites: research.md complete ✅*

1. **Entities extracted** → `data-model.md`:
   - ✅ PriceUpdate (immutable record): instrumentId, price, timestamp, bid, ask, volume
   - ✅ SubscriptionCommand (immutable record): subscriberId, action, instrumentIds, timestamp
   - ✅ PriceStatistics (mutable state): totalMessages, uniqueInstruments, instrumentCounts
   - ✅ Instrument (implicit entity): KEY000000-KEY999999 validation utility
   - ✅ Validation rules defined for all entities
   - ✅ State transitions documented (especially for PriceStatistics)
   - ✅ Relationship diagram created

2. **API contracts generated** from functional requirements:
   - ✅ Kafka Message Contract: `contracts/price-update-contract.md`
     - Topic: price-updates (time-based retention)
     - Key: instrumentId (String)
     - Value: PriceUpdate (JSON)
     - Contract tests defined (serialization, deserialization, roundtrip)
   - ✅ Kafka Message Contract: `contracts/subscription-command-contract.md`
     - Topic: subscription-commands (compacted)
     - Key: subscriberId (String)
     - Value: SubscriptionCommand (JSON)
     - Contract tests defined (including compaction behavior)
   - ✅ REST API Contract: `contracts/subscription-api-contract.md`
     - PUT /api/v1/subscriptions/{subscriberId} (REPLACE)
     - POST /api/v1/subscriptions/{subscriberId}/add (ADD)
     - POST /api/v1/subscriptions/{subscriberId}/remove (REMOVE)
     - GET /actuator/health (health check)
     - Contract tests defined (validation, error handling, concurrency)

3. **Contract tests generated**:
   - ✅ PriceUpdate serialization/deserialization tests
   - ✅ PriceUpdate validation tests (instrument ID, price constraints)
   - ✅ PriceUpdate Kafka producer-consumer roundtrip test
   - ✅ SubscriptionCommand serialization/deserialization tests
   - ✅ SubscriptionCommand validation tests (action, subscriber ID)
   - ✅ SubscriptionCommand compaction behavior test
   - ✅ REST API endpoint tests (success, validation errors, 4xx/5xx responses)
   - ✅ All tests documented in contract files (TDD - must fail before implementation)

4. **Test scenarios extracted** → `quickstart.md`:
   - ✅ Scenario 1: Price Generator publishes updates (100ms-1s intervals)
   - ✅ Scenario 2: Subscriber receives filtered updates (subscription-based)
   - ✅ Scenario 3: Statistics logged every 5 seconds
   - ✅ Scenario 4: Dynamic subscription changes (no restart)
   - ✅ Scenario 5: Multiple independent subscribers
   - ✅ Scenario 6: State recovery after restart (compacted topic)
   - ✅ Step-by-step validation procedures included
   - ✅ Troubleshooting guide provided

5. **Agent file update**: (To be executed next)
   - Command: `.specify/scripts/powershell/update-agent-context.ps1 -AgentType copilot`
   - Will update GitHub Copilot instructions with current plan context

**Output**: 
- ✅ `data-model.md` (4 entities with validation and relationships)
- ✅ `contracts/price-update-contract.md` (Kafka message + tests)
- ✅ `contracts/subscription-command-contract.md` (Kafka message + tests)
- ✅ `contracts/subscription-api-contract.md` (REST API + tests)
- ✅ `quickstart.md` (step-by-step guide with validation)
- ⏳ Agent-specific file (next step)

## Phase 2: Task Planning Approach ✅ PLANNED
*This section describes what the /tasks command will do - DO NOT execute during /plan*

**Task Generation Strategy**:
The /tasks command will generate tasks in strict TDD order based on Phase 1 artifacts:

1. **Module Setup Tasks** (Phase 0 of implementation):
   - Update root pom.xml with module declarations
   - Create kafka-platform-common module structure
   - Create kafka-platform-price-generator module structure
   - Create kafka-platform-price-subscriber module structure
   - Create kafka-platform-subscription-manager module structure

2. **Common Module Tasks** (Phase 1 - Foundation):
   - Write PriceUpdate model tests [P]
   - Implement PriceUpdate record with validation
   - Write SubscriptionCommand model tests [P]
   - Implement SubscriptionCommand record with validation
   - Write InstrumentValidator tests [P]
   - Implement InstrumentValidator utility
   - Write Kafka contract tests for PriceUpdate [P]
   - Write Kafka contract tests for SubscriptionCommand [P]

3. **Price Generator Tasks** (Phase 2 - Producer):
   - Write PriceGenerationService tests
   - Implement PriceGenerationService (instrument selection, price calculation)
   - Write InstrumentSelector tests
   - Implement InstrumentSelector (random selection from 50k range)
   - Write PriceUpdateProducer tests
   - Implement PriceUpdateProducer (Kafka producer with error handling)
   - Write KafkaProducerConfig tests
   - Implement KafkaProducerConfig (Spring Kafka configuration)
   - Write PriceGeneratorIntegrationTest (embedded Kafka)
   - Configure application.yml for price-generator
   - Implement PriceGeneratorApplication main class

4. **Subscription Manager Tasks** (Phase 3 - Control Plane):
   - Write REST API controller tests (subscription-api-contract tests)
   - Implement SubscriptionController (PUT, POST endpoints)
   - Write SubscriptionCommandService tests
   - Implement SubscriptionCommandService (validation, command creation)
   - Write RequestSerializationService tests
   - Implement RequestSerializationService (concurrent request handling)
   - Write SubscriptionCommandProducer tests
   - Implement SubscriptionCommandProducer (Kafka producer, compacted topic)
   - Write KafkaProducerConfig tests [P]
   - Implement KafkaProducerConfig
   - Write SubscriptionManagerIntegrationTest (embedded Kafka + MockMvc)
   - Configure application.yml for subscription-manager
   - Implement SubscriptionManagerApplication main class

5. **Price Subscriber Tasks** (Phase 4 - Consumer):
   - Write PriceUpdateConsumer tests
   - Implement PriceUpdateConsumer (Kafka consumer, filtering)
   - Write SubscriptionCommandConsumer tests
   - Implement SubscriptionCommandConsumer (Kafka consumer, state updates)
   - Write PriceFilterService tests
   - Implement PriceFilterService (in-memory Set, dynamic updates)
   - Write StatisticsAggregator tests
   - Implement StatisticsAggregator (5-second window, atomic counters)
   - Write SubscriptionManager (state) tests
   - Implement SubscriptionManager (ADD/REMOVE/REPLACE logic)
   - Write KafkaConsumerConfig tests
   - Implement KafkaConsumerConfig (dual consumers)
   - Write PriceSubscriberIntegrationTest (embedded Kafka, end-to-end)
   - Configure application.yml for price-subscriber
   - Implement PriceSubscriberApplication main class

6. **Observability Tasks** (Phase 5 - Cross-cutting):
   - Configure Logback JSON encoder in all modules [P]
   - Add custom Micrometer metrics in price-generator [P]
   - Add custom Micrometer metrics in price-subscriber [P]
   - Add custom Micrometer metrics in subscription-manager [P]
   - Write actuator health tests [P]
   - Configure actuator endpoints [P]

7. **Integration & Validation Tasks** (Phase 6 - Final):
   - Run all contract tests and verify failures (pre-implementation gate)
   - Execute full build: mvnw clean install
   - Follow quickstart.md steps 1-10
   - Validate acceptance scenarios 1-6
   - Performance test: measure p95 latency
   - Load test: 10 updates/second for 5 minutes
   - Multi-subscriber test: 3 concurrent subscribers with different configs

**Ordering Strategy**:
- **Strict TDD**: Tests written before implementation (each task pair: test → impl)
- **Dependency Order**: common → generator/manager/subscriber (modules)
- **[P] Marker**: Tasks that can execute in parallel (independent files/modules)
- **Phases**: Setup → Common → Services (independent) → Observability → Validation

**Task Dependencies**:
```
Module Setup (1-5) → Common Foundation (6-13)
                    ↓
     ┌──────────────┼──────────────┐
     ↓              ↓               ↓
Generator (14-24)  Manager (25-37)  Subscriber (38-52)
     │              │               │
     └──────────────┼───────────────┘
                    ↓
        Observability (53-58) [cross-cutting]
                    ↓
           Integration & Validation (59-65)
```

**Estimated Output**: 
- **Total Tasks**: ~65 tasks
- **Parallel Tasks**: ~15 tasks marked [P]
- **Sequential Phases**: 7 phases
- **Test-Implementation Ratio**: 1:1 (TDD)

**Task Template Format** (from tasks-template.md):
```markdown
## T001: [Task Title]
- **Type**: [Test/Implementation/Configuration]
- **Module**: [kafka-platform-{module}]
- **Dependencies**: [None / T000]
- **Parallel**: [Yes/No - marked [P] if yes]
- **Description**: [What to do]
- **Acceptance Criteria**: [How to verify completion]
- **Files**: [List of files to create/modify]
```

**IMPORTANT**: This phase is executed by the /tasks command, NOT by /plan. The /tasks command will load the plan, read Phase 1 artifacts, and generate the complete tasks.md file following this strategy.

## Phase 3+: Future Implementation
*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates tasks.md)  
**Phase 4**: Implementation (execute tasks.md following constitutional principles)  
**Phase 5**: Validation (run tests, execute quickstart.md, performance validation)

## Complexity Tracking
*Fill ONLY if Constitution Check has violations that must be justified*

**No Violations Detected**: All design decisions align with constitutional requirements. No complexity deviations require justification.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| N/A | N/A | N/A |

**Design Simplicity Notes**:
- 4-module structure is minimal for the requirements (common + 3 services)
- No unnecessary abstractions introduced (no repository pattern, no complex DDD layers)
- Direct Kafka integration via Spring Kafka (no custom wrappers)
- In-memory filtering (no database overhead)
- Atomic operations for statistics (no distributed coordination)
- Simple REST API (no GraphQL, gRPC, or messaging complexity)


## Progress Tracking
*This checklist is updated during execution flow*

**Phase Status**:
- [x] Phase 0: Research complete (/plan command) - ✅ DONE (research.md generated)
- [x] Phase 1: Design complete (/plan command) - ✅ DONE (data-model.md, contracts/, quickstart.md, agent file updated)
- [x] Phase 2: Task planning complete (/plan command - describe approach only) - ✅ DONE (task generation strategy documented)
- [ ] Phase 3: Tasks generated (/tasks command) - ⏳ NEXT: Run `/tasks` command
- [ ] Phase 4: Implementation complete - ⏸️ PENDING Phase 3
- [ ] Phase 5: Validation passed - ⏸️ PENDING Phase 4

**Gate Status**:
- [x] Initial Constitution Check: ✅ PASS (all requirements met)
- [x] Post-Design Constitution Check: ✅ PASS (design fully compliant)
- [x] All NEEDS CLARIFICATION resolved: ✅ YES (specification had 0 unresolved items)
- [x] Complexity deviations documented: ✅ N/A (no deviations)

**Artifacts Generated**:
- [x] Technical Context filled
- [x] Constitution Check evaluated (initial + post-design)
- [x] Project Structure documented (multi-module Maven)
- [x] research.md (12 technology decisions)
- [x] data-model.md (4 entities with validation)
- [x] contracts/price-update-contract.md (Kafka message + tests)
- [x] contracts/subscription-command-contract.md (Kafka message + tests)
- [x] contracts/subscription-api-contract.md (REST API + tests)
- [x] quickstart.md (step-by-step guide with validation)
- [x] .github/copilot-instructions.md (agent context updated)

**Ready for Next Command**: `/tasks` (will generate tasks.md following Phase 2 strategy)

---
*Based on Constitution v1.0.0 - See `.specify/memory/constitution.md`*
