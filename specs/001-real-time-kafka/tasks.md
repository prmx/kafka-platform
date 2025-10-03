# Tasks: Real-Time Kafka Pricing Platform

**Feature Branch**: `001-real-time-kafka`  
**Input**: Design documents from `/specs/001-real-time-kafka/`  
**Prerequisites**: ✅ plan.md, research.md, data-model.md, contracts/, quickstart.md

## Execution Status

This tasks file was generated from:
- **plan.md**: Multi-module Maven structure (4 modules), Java 25, Spring Boot 3.5.6, Spring Kafka
- **research.md**: 12 technology decisions (JSON serialization, embedded Kafka testing, etc.)
- **data-model.md**: 4 entities (PriceUpdate, SubscriptionCommand, PriceStatistics, Instrument)
- **contracts/**: 3 contract files (price-update, subscription-command, subscription-api)
- **quickstart.md**: 6 acceptance scenarios + step-by-step validation

**Total Tasks**: 67  
**Parallel Tasks**: 24 marked [P]  
**Implementation Phases**: 6

---

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- File paths are absolute from repository root
- Tasks numbered T001-T067

## Path Conventions
Multi-module Maven project structure:
```
kafka-platform/
├── pom.xml                                   (root parent POM)
├── kafka-platform-common/
├── kafka-platform-price-generator/
├── kafka-platform-price-subscriber/
└── kafka-platform-subscription-manager/
```

---

## Phase 3.1: Multi-Module Maven Setup (T001-T005)

- [ ] **T001** Update root `pom.xml` with multi-module configuration
  - **Files**: `pom.xml`
  - **Description**: Configure parent POM with Java 25, Spring Boot 3.5.6 BOM, module declarations, plugin management
  - **Acceptance**: `mvnw clean` executes without errors
  - **Details**: 
    - Set `<java.version>25</java.version>`
    - Add Spring Boot parent or dependency management
    - Declare modules: `<modules><module>kafka-platform-common</module>...</modules>`
    - Configure maven-compiler-plugin for Java 25
    - Configure spring-boot-maven-plugin
  - **Dependencies**: None

- [ ] **T002** [P] Create `kafka-platform-common` module structure
  - **Files**: 
    - `kafka-platform-common/pom.xml`
    - `kafka-platform-common/src/main/java/net/prmx/kafka/platform/common/.gitkeep`
    - `kafka-platform-common/src/test/java/net/prmx/kafka/platform/common/.gitkeep`
  - **Description**: Initialize common module with POM (no Spring Boot application), directories for models and utils
  - **Acceptance**: Module compiles with `mvnw clean compile -pl kafka-platform-common`
  - **Dependencies**: T001

- [ ] **T003** [P] Create `kafka-platform-price-generator` module structure
  - **Files**:
    - `kafka-platform-price-generator/pom.xml`
    - `kafka-platform-price-generator/src/main/java/net/prmx/kafka/platform/generator/.gitkeep`
    - `kafka-platform-price-generator/src/main/resources/application.yml`
    - `kafka-platform-price-generator/src/test/java/net/prmx/kafka/platform/generator/.gitkeep`
  - **Description**: Initialize generator module with Spring Boot starter dependencies (web, kafka, actuator, test)
  - **Acceptance**: Module compiles with `mvnw clean compile -pl kafka-platform-price-generator`
  - **Dependencies**: T001, T002 (depends on common)

- [ ] **T004** [P] Create `kafka-platform-price-subscriber` module structure
  - **Files**:
    - `kafka-platform-price-subscriber/pom.xml`
    - `kafka-platform-price-subscriber/src/main/java/net/prmx/kafka/platform/subscriber/.gitkeep`
    - `kafka-platform-price-subscriber/src/main/resources/application.yml`
    - `kafka-platform-price-subscriber/src/test/java/net/prmx/kafka/platform/subscriber/.gitkeep`
  - **Description**: Initialize subscriber module with Spring Boot starter dependencies (kafka, actuator, test)
  - **Acceptance**: Module compiles with `mvnw clean compile -pl kafka-platform-price-subscriber`
  - **Dependencies**: T001, T002 (depends on common)

- [ ] **T005** [P] Create `kafka-platform-subscription-manager` module structure
  - **Files**:
    - `kafka-platform-subscription-manager/pom.xml`
    - `kafka-platform-subscription-manager/src/main/java/net/prmx/kafka/platform/manager/.gitkeep`
    - `kafka-platform-subscription-manager/src/main/resources/application.yml`
    - `kafka-platform-subscription-manager/src/test/java/net/prmx/kafka/platform/manager/.gitkeep`
  - **Description**: Initialize manager module with Spring Boot starter dependencies (web, kafka, actuator, test, validation)
  - **Acceptance**: Module compiles with `mvnw clean compile -pl kafka-platform-subscription-manager`
  - **Dependencies**: T001, T002 (depends on common)

---

## Phase 3.2: Tests First - Common Module (T006-T017) ⚠️ TDD GATE
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**

### Contract Tests for PriceUpdate

- [ ] **T006** [P] PriceUpdate serialization/deserialization contract test
  - **Files**: `kafka-platform-common/src/test/java/net/prmx/kafka/platform/common/model/PriceUpdateSerializationTest.java`
  - **Description**: Test JSON serialization and deserialization of PriceUpdate using Jackson ObjectMapper
  - **Acceptance**: Test compiles, runs, and FAILS (PriceUpdate class doesn't exist yet)
  - **Test Cases**:
    - Serialize PriceUpdate to JSON and verify fields
    - Deserialize JSON to PriceUpdate and verify object
  - **Dependencies**: T002

- [ ] **T007** [P] PriceUpdate validation contract test
  - **Files**: `kafka-platform-common/src/test/java/net/prmx/kafka/platform/common/model/PriceUpdateValidationTest.java`
  - **Description**: Test all validation rules from data-model.md (instrument pattern, price constraints, bid/ask rules)
  - **Acceptance**: Test compiles, runs, and FAILS
  - **Test Cases**:
    - Valid KEY format accepted (KEY000000-KEY999999)
    - Invalid instrument ID rejected
    - Negative price rejected
    - Bid > price rejected
    - Ask < price rejected
    - Negative volume rejected
  - **Dependencies**: T002

### Contract Tests for SubscriptionCommand

- [ ] **T008** [P] SubscriptionCommand serialization/deserialization contract test
  - **Files**: `kafka-platform-common/src/test/java/net/prmx/kafka/platform/common/model/SubscriptionCommandSerializationTest.java`
  - **Description**: Test JSON serialization and deserialization of SubscriptionCommand
  - **Acceptance**: Test compiles, runs, and FAILS
  - **Test Cases**:
    - Serialize SubscriptionCommand to JSON
    - Deserialize JSON to SubscriptionCommand
    - Test all action types (ADD, REMOVE, REPLACE)
  - **Dependencies**: T002

- [ ] **T009** [P] SubscriptionCommand validation contract test
  - **Files**: `kafka-platform-common/src/test/java/net/prmx/kafka/platform/common/model/SubscriptionCommandValidationTest.java`
  - **Description**: Test validation rules (subscriber ID, action enum, instrument IDs list)
  - **Acceptance**: Test compiles, runs, and FAILS
  - **Test Cases**:
    - Valid actions accepted (ADD, REMOVE, REPLACE)
    - Invalid action rejected
    - Blank subscriber ID rejected
    - Subscriber ID max length (100) enforced
    - Invalid instrument ID in list rejected
    - Empty instrument list validation
  - **Dependencies**: T002

### Utility Tests

- [ ] **T010** [P] InstrumentValidator utility test
  - **Files**: `kafka-platform-common/src/test/java/net/prmx/kafka/platform/common/util/InstrumentValidatorTest.java`
  - **Description**: Test instrument ID validation logic (KEY000000-KEY999999 range)
  - **Acceptance**: Test compiles, runs, and FAILS
  - **Test Cases**:
    - isValid() returns true for KEY000000
    - isValid() returns true for KEY999999
    - isValid() returns false for KEY1000000 (out of range)
    - isValid() returns false for "INVALID"
    - validateOrThrow() succeeds for valid IDs
    - validateOrThrow() throws for invalid IDs
    - formatInstrument(0) returns "KEY000000"
    - formatInstrument(999999) returns "KEY999999"
  - **Dependencies**: T002

### Kafka Contract Tests (Embedded Kafka)

- [ ] **T011** [P] PriceUpdate Kafka producer-consumer roundtrip test
  - **Files**: `kafka-platform-common/src/test/java/net/prmx/kafka/platform/common/integration/PriceUpdateKafkaContractTest.java`
  - **Description**: Use @EmbeddedKafka to test PriceUpdate can be published and consumed via Kafka
  - **Acceptance**: Test compiles, runs, and FAILS
  - **Test Cases**:
    - Publish PriceUpdate to topic
    - Consume message and verify key (instrumentId)
    - Consume message and verify value (PriceUpdate object)
    - Verify JSON serialization works over Kafka
  - **Dependencies**: T002, T006

- [ ] **T012** [P] SubscriptionCommand Kafka producer-consumer roundtrip test
  - **Files**: `kafka-platform-common/src/test/java/net/prmx/kafka/platform/common/integration/SubscriptionCommandKafkaContractTest.java`
  - **Description**: Use @EmbeddedKafka to test SubscriptionCommand can be published and consumed
  - **Acceptance**: Test compiles, runs, and FAILS
  - **Test Cases**:
    - Publish SubscriptionCommand to topic
    - Consume message and verify key (subscriberId)
    - Consume message and verify value (SubscriptionCommand object)
    - Test compaction configuration (broker properties)
  - **Dependencies**: T002, T008

---

## Phase 3.3: Core Implementation - Common Module (T013-T017)
**ONLY after T006-T012 are failing**

- [ ] **T013** [P] Implement PriceUpdate record
  - **Files**: `kafka-platform-common/src/main/java/net/prmx/kafka/platform/common/model/PriceUpdate.java`
  - **Description**: Create immutable record with validation in compact constructor as per data-model.md
  - **Acceptance**: T006-T007 and T011 now PASS
  - **Implementation**:
    - Java record with 6 fields (instrumentId, price, timestamp, bid, ask, volume)
    - Compact constructor with validation
    - @JsonCreator and @JsonProperty annotations
    - Validation: instrument pattern, positive prices, bid<=price<=ask, volume>=0
  - **Dependencies**: T006, T007, T011 (tests must exist and fail first)

- [ ] **T014** [P] Implement SubscriptionCommand record
  - **Files**: `kafka-platform-common/src/main/java/net/prmx/kafka/platform/common/model/SubscriptionCommand.java`
  - **Description**: Create immutable record with action enum and validation
  - **Acceptance**: T008-T009 and T012 now PASS
  - **Implementation**:
    - Java record with 4 fields (subscriberId, action, instrumentIds, timestamp)
    - Nested Action enum (ADD, REMOVE, REPLACE)
    - Compact constructor with validation
    - Immutable list copy for instrumentIds
  - **Dependencies**: T008, T009, T012 (tests must exist and fail first)

- [ ] **T015** [P] Implement InstrumentValidator utility
  - **Files**: `kafka-platform-common/src/main/java/net/prmx/kafka/platform/common/util/InstrumentValidator.java`
  - **Description**: Create utility class with static validation methods
  - **Acceptance**: T010 now PASSES
  - **Implementation**:
    - Pattern: `KEY\d{6}`
    - Range check: KEY000000 to KEY999999
    - isValid(String) method
    - validateOrThrow(String) method
    - formatInstrument(int) method
  - **Dependencies**: T010 (test must exist and fail first)

- [ ] **T016** Run common module tests and verify all pass
  - **Files**: N/A (validation step)
  - **Description**: Execute `mvnw test -pl kafka-platform-common` and confirm 100% test success
  - **Acceptance**: All tests in T006-T012 now PASS, build succeeds
  - **Dependencies**: T013, T014, T015

- [ ] **T017** Build common module JAR
  - **Files**: N/A (validation step)
  - **Description**: Execute `mvnw clean install -pl kafka-platform-common` to build and install to local Maven repo
  - **Acceptance**: JAR created in target/, installed to ~/.m2/repository, no errors
  - **Dependencies**: T016

---

## Phase 3.4: Tests First - Price Generator Module (T018-T025) ⚠️ TDD GATE

- [ ] **T018** [P] PriceGenerationService unit test
  - **Files**: `kafka-platform-price-generator/src/test/java/net/prmx/kafka/platform/generator/service/PriceGenerationServiceTest.java`
  - **Description**: Test price generation logic (instrument selection, price calculation, bid/ask/volume generation)
  - **Acceptance**: Test compiles, runs, and FAILS
  - **Test Cases**:
    - generatePrice() returns valid PriceUpdate
    - Instrument ID in KEY000000-KEY999999 range
    - bid <= price <= ask
    - volume >= 0
    - timestamp is current time
  - **Dependencies**: T003, T017

- [ ] **T019** [P] InstrumentSelector unit test
  - **Files**: `kafka-platform-price-generator/src/test/java/net/prmx/kafka/platform/generator/service/InstrumentSelectorTest.java`
  - **Description**: Test random instrument selection from 50k universe
  - **Acceptance**: Test compiles, runs, and FAILS
  - **Test Cases**:
    - selectRandom() returns KEY format
    - Repeated calls return different instruments (randomness)
    - All returned instruments in valid range
  - **Dependencies**: T003, T017

- [ ] **T020** [P] PriceUpdateProducer unit test (mocked Kafka)
  - **Files**: `kafka-platform-price-generator/src/test/java/net/prmx/kafka/platform/generator/producer/PriceUpdateProducerTest.java`
  - **Description**: Test Kafka producer sends PriceUpdate with correct key and error handling
  - **Acceptance**: Test compiles, runs, and FAILS
  - **Test Cases**:
    - publishPrice() calls KafkaTemplate.send() with correct topic and key
    - Error handling: logs error and continues on failure (no retry)
  - **Dependencies**: T003, T017

- [ ] **T021** [P] KafkaProducerConfig test
  - **Files**: `kafka-platform-price-generator/src/test/java/net/prmx/kafka/platform/generator/config/KafkaProducerConfigTest.java`
  - **Description**: Test Spring Kafka producer configuration bean creation
  - **Acceptance**: Test compiles, runs, and FAILS
  - **Test Cases**:
    - Configuration creates KafkaTemplate bean
    - JSON serializer configured for value
    - Bootstrap servers configured
  - **Dependencies**: T003, T017

- [ ] **T022** [P] PriceGeneratorApplication smoke test
  - **Files**: `kafka-platform-price-generator/src/test/java/net/prmx/kafka/platform/generator/PriceGeneratorApplicationTest.java`
  - **Description**: Spring Boot context loads test
  - **Acceptance**: Test compiles, runs, and FAILS
  - **Test Cases**:
    - @SpringBootTest context loads without errors
    - All required beans are created
  - **Dependencies**: T003, T017

- [ ] **T023** [P] Price generator integration test (embedded Kafka)
  - **Files**: `kafka-platform-price-generator/src/test/java/net/prmx/kafka/platform/generator/integration/PriceGeneratorIntegrationTest.java`
  - **Description**: End-to-end test: start service, generate prices, consume from embedded Kafka
  - **Acceptance**: Test compiles, runs, and FAILS
  - **Test Cases**:
    - Service starts and generates prices
    - Prices published to price-updates topic
    - Consumer receives valid PriceUpdate messages
    - Verify generation interval (100ms-1s randomization)
  - **Dependencies**: T003, T017

---

## Phase 3.5: Core Implementation - Price Generator Module (T024-T031)

- [ ] **T024** [P] Implement InstrumentSelector service
  - **Files**: `kafka-platform-price-generator/src/main/java/net/prmx/kafka/platform/generator/service/InstrumentSelector.java`
  - **Description**: Random instrument selection using ThreadLocalRandom
  - **Acceptance**: T019 now PASSES
  - **Implementation**:
    - @Service annotation
    - selectRandom() method returns KEY000000-KEY999999
    - Use ThreadLocalRandom for efficient randomization
  - **Dependencies**: T019

- [ ] **T025** [P] Implement PriceGenerationService
  - **Files**: `kafka-platform-price-generator/src/main/java/net/prmx/kafka/platform/generator/service/PriceGenerationService.java`
  - **Description**: Price generation logic with scheduled executor as per research.md
  - **Acceptance**: T018 now PASSES
  - **Implementation**:
    - @Service annotation
    - ScheduledExecutorService for randomized intervals
    - generatePrice() creates PriceUpdate with realistic bid/ask/volume
    - startGeneration() and stopGeneration() lifecycle methods
    - Recursive scheduling with random delay (100ms-1s)
  - **Dependencies**: T018, T024

- [ ] **T026** [P] Implement PriceUpdateProducer
  - **Files**: `kafka-platform-price-generator/src/main/java/net/prmx/kafka/platform/generator/producer/PriceUpdateProducer.java`
  - **Description**: Kafka producer wrapper with error handling
  - **Acceptance**: T020 now PASSES
  - **Implementation**:
    - @Component annotation
    - Inject KafkaTemplate<String, PriceUpdate>
    - publishPrice(PriceUpdate) method
    - Send with instrumentId as key
    - Log error and continue on failure (no retry)
  - **Dependencies**: T020

- [ ] **T027** [P] Implement KafkaProducerConfig
  - **Files**: `kafka-platform-price-generator/src/main/java/net/prmx/kafka/platform/generator/config/KafkaProducerConfig.java`
  - **Description**: Spring Kafka producer configuration
  - **Acceptance**: T021 now PASSES
  - **Implementation**:
    - @Configuration annotation
    - ProducerFactory bean with JSON serializer
    - KafkaTemplate bean
    - Bootstrap servers from application.yml
  - **Dependencies**: T021

- [ ] **T028** Configure application.yml for price-generator
  - **Files**: `kafka-platform-price-generator/src/main/resources/application.yml`
  - **Description**: Spring Boot configuration with Kafka settings, actuator, logging
  - **Acceptance**: Service starts without configuration errors
  - **Implementation**:
    - spring.application.name: price-generator
    - spring.kafka.bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    - spring.kafka.producer.key-serializer: StringSerializer
    - spring.kafka.producer.value-serializer: JsonSerializer
    - kafka.topics.price-updates: price-updates
    - management.endpoints.web.exposure.include: health,metrics,prometheus
    - logging.level configuration (DEBUG for net.prmx.kafka.platform)
    - server.port: 8081
  - **Dependencies**: None (can be done in parallel with tests)

- [ ] **T029** Implement PriceGeneratorApplication main class
  - **Files**: `kafka-platform-price-generator/src/main/java/net/prmx/kafka/platform/generator/PriceGeneratorApplication.java`
  - **Description**: Spring Boot application main class with @SpringBootApplication
  - **Acceptance**: T022 now PASSES, application starts
  - **Implementation**:
    - @SpringBootApplication annotation
    - main(String[] args) method with SpringApplication.run()
    - @PostConstruct to start price generation
    - @PreDestroy to stop generation
  - **Dependencies**: T024, T025, T026, T027, T028

- [ ] **T030** Run price-generator module tests
  - **Files**: N/A (validation step)
  - **Description**: Execute `mvnw test -pl kafka-platform-price-generator`
  - **Acceptance**: All tests T018-T023 now PASS
  - **Dependencies**: T024, T025, T026, T027, T029

- [ ] **T031** Build price-generator module JAR
  - **Files**: N/A (validation step)
  - **Description**: Execute `mvnw clean install -pl kafka-platform-price-generator`
  - **Acceptance**: Executable JAR created, can run with `java -jar`
  - **Dependencies**: T030

---

## Phase 3.6: Tests First - Subscription Manager Module (T032-T040) ⚠️ TDD GATE

- [ ] **T032** [P] SubscriptionController REST API test (PUT /subscriptions/{id})
  - **Files**: `kafka-platform-subscription-manager/src/test/java/net/prmx/kafka/platform/manager/controller/SubscriptionControllerReplaceTest.java`
  - **Description**: Test REPLACE subscription endpoint with MockMvc
  - **Acceptance**: Test compiles, runs, and FAILS
  - **Test Cases**:
    - PUT returns 200 OK with valid request
    - Response contains subscriberId, action=REPLACE, instrumentIds
    - 400 Bad Request for empty instrumentIds
    - 400 Bad Request for invalid instrument ID
  - **Dependencies**: T005, T017

- [ ] **T033** [P] SubscriptionController REST API test (POST /subscriptions/{id}/add)
  - **Files**: `kafka-platform-subscription-manager/src/test/java/net/prmx/kafka/platform/manager/controller/SubscriptionControllerAddTest.java`
  - **Description**: Test ADD instruments endpoint
  - **Acceptance**: Test compiles, runs, and FAILS
  - **Test Cases**:
    - POST returns 200 OK with valid request
    - Response action=ADD
    - Validation errors return 400
  - **Dependencies**: T005, T017

- [ ] **T034** [P] SubscriptionController REST API test (POST /subscriptions/{id}/remove)
  - **Files**: `kafka-platform-subscription-manager/src/test/java/net/prmx/kafka/platform/manager/controller/SubscriptionControllerRemoveTest.java`
  - **Description**: Test REMOVE instruments endpoint
  - **Acceptance**: Test compiles, runs, and FAILS
  - **Test Cases**:
    - POST returns 200 OK with valid request
    - Response action=REMOVE
    - Validation errors return 400
  - **Dependencies**: T005, T017

- [ ] **T035** [P] SubscriptionCommandService unit test
  - **Files**: `kafka-platform-subscription-manager/src/test/java/net/prmx/kafka/platform/manager/service/SubscriptionCommandServiceTest.java`
  - **Description**: Test command creation and validation logic
  - **Acceptance**: Test compiles, runs, and FAILS
  - **Test Cases**:
    - createCommand() returns valid SubscriptionCommand
    - Instrument IDs validated before command creation
    - Timestamp set to current time
  - **Dependencies**: T005, T017

- [ ] **T036** [P] RequestSerializationService unit test
  - **Files**: `kafka-platform-subscription-manager/src/test/java/net/prmx/kafka/platform/manager/service/RequestSerializationServiceTest.java`
  - **Description**: Test concurrent request serialization per subscriber
  - **Acceptance**: Test compiles, runs, and FAILS
  - **Test Cases**:
    - Requests for same subscriberId are serialized
    - Requests for different subscriberIds execute in parallel
    - Lock acquired and released properly
  - **Dependencies**: T005, T017

- [ ] **T037** [P] SubscriptionCommandProducer unit test
  - **Files**: `kafka-platform-subscription-manager/src/test/java/net/prmx/kafka/platform/manager/producer/SubscriptionCommandProducerTest.java`
  - **Description**: Test Kafka producer for subscription commands
  - **Acceptance**: Test compiles, runs, and FAILS
  - **Test Cases**:
    - publishCommand() sends to correct topic
    - Message key is subscriberId (for compaction)
    - Error handling with retry
  - **Dependencies**: T005, T017

- [ ] **T038** [P] Subscription manager integration test (embedded Kafka)
  - **Files**: `kafka-platform-subscription-manager/src/test/java/net/prmx/kafka/platform/manager/integration/SubscriptionManagerIntegrationTest.java`
  - **Description**: End-to-end REST API → Kafka test
  - **Acceptance**: Test compiles, runs, and FAILS
  - **Test Cases**:
    - POST to API publishes command to Kafka
    - Command consumed from embedded Kafka topic
    - Key is subscriberId, value is SubscriptionCommand
  - **Dependencies**: T005, T017

---

## Phase 3.7: Core Implementation - Subscription Manager Module (T039-T047)

- [ ] **T039** [P] Implement SubscriptionRequest DTO
  - **Files**: `kafka-platform-subscription-manager/src/main/java/net/prmx/kafka/platform/manager/dto/SubscriptionRequest.java`
  - **Description**: Request DTO with validation annotations
  - **Acceptance**: Used in controller tests
  - **Implementation**:
    - Record with instrumentIds field
    - @NotEmpty and @Pattern validation annotations
  - **Dependencies**: None

- [ ] **T040** [P] Implement SubscriptionResponse DTO
  - **Files**: `kafka-platform-subscription-manager/src/main/java/net/prmx/kafka/platform/manager/dto/SubscriptionResponse.java`
  - **Description**: Response DTO
  - **Acceptance**: Used in controller tests
  - **Implementation**:
    - Record with subscriberId, action, instrumentIds, timestamp, status
  - **Dependencies**: None

- [ ] **T041** [P] Implement ErrorResponse DTO
  - **Files**: `kafka-platform-subscription-manager/src/main/java/net/prmx/kafka/platform/manager/dto/ErrorResponse.java`
  - **Description**: Error response DTO for validation failures
  - **Acceptance**: Used in controller tests
  - **Implementation**:
    - Record with error, message, timestamp
  - **Dependencies**: None

- [ ] **T042** [P] Implement RequestSerializationService
  - **Files**: `kafka-platform-subscription-manager/src/main/java/net/prmx/kafka/platform/manager/service/RequestSerializationService.java`
  - **Description**: Service to serialize concurrent requests per subscriber
  - **Acceptance**: T036 now PASSES
  - **Implementation**:
    - @Service annotation
    - ConcurrentHashMap<String, ReentrantLock> for per-subscriber locks
    - executeSerializedForSubscriber() method
  - **Dependencies**: T036

- [ ] **T043** [P] Implement SubscriptionCommandService
  - **Files**: `kafka-platform-subscription-manager/src/main/java/net/prmx/kafka/platform/manager/service/SubscriptionCommandService.java`
  - **Description**: Business logic for command creation and validation
  - **Acceptance**: T035 now PASSES
  - **Implementation**:
    - @Service annotation
    - createCommand() method validates instruments and creates SubscriptionCommand
    - Use InstrumentValidator from common module
  - **Dependencies**: T035

- [ ] **T044** [P] Implement SubscriptionCommandProducer
  - **Files**: `kafka-platform-subscription-manager/src/main/java/net/prmx/kafka/platform/manager/producer/SubscriptionCommandProducer.java`
  - **Description**: Kafka producer for subscription commands
  - **Acceptance**: T037 now PASSES
  - **Implementation**:
    - @Component annotation
    - Inject KafkaTemplate<String, SubscriptionCommand>
    - publishCommand() with subscriberId as key
    - Retry with exponential backoff on failure
  - **Dependencies**: T037

- [ ] **T045** Implement SubscriptionController
  - **Files**: `kafka-platform-subscription-manager/src/main/java/net/prmx/kafka/platform/manager/controller/SubscriptionController.java`
  - **Description**: REST controller with PUT and POST endpoints
  - **Acceptance**: T032-T034 now PASS
  - **Implementation**:
    - @RestController with @RequestMapping("/api/v1/subscriptions")
    - PUT /{subscriberId} for REPLACE
    - POST /{subscriberId}/add for ADD
    - POST /{subscriberId}/remove for REMOVE
    - @Valid on request bodies
    - @ExceptionHandler for validation errors
    - Use RequestSerializationService to serialize per subscriber
  - **Dependencies**: T032, T033, T034, T039, T040, T041, T042, T043, T044

- [ ] **T046** [P] Implement KafkaProducerConfig for subscription-manager
  - **Files**: `kafka-platform-subscription-manager/src/main/java/net/prmx/kafka/platform/manager/config/KafkaProducerConfig.java`
  - **Description**: Spring Kafka producer configuration
  - **Acceptance**: Context loads without errors
  - **Implementation**:
    - @Configuration annotation
    - ProducerFactory bean with JSON serializer for SubscriptionCommand
    - KafkaTemplate bean
  - **Dependencies**: None

- [ ] **T047** Configure application.yml for subscription-manager
  - **Files**: `kafka-platform-subscription-manager/src/main/resources/application.yml`
  - **Description**: Spring Boot configuration
  - **Acceptance**: Service starts without errors
  - **Implementation**:
    - spring.application.name: subscription-manager
    - spring.kafka configuration
    - kafka.topics.subscription-commands: subscription-commands
    - server.port: 8080
    - management.endpoints configuration
    - logging configuration
  - **Dependencies**: None

- [ ] **T048** Implement SubscriptionManagerApplication main class
  - **Files**: `kafka-platform-subscription-manager/src/main/java/net/prmx/kafka/platform/manager/SubscriptionManagerApplication.java`
  - **Description**: Spring Boot application entry point
  - **Acceptance**: Application starts, REST endpoints available
  - **Implementation**:
    - @SpringBootApplication annotation
    - main() method
  - **Dependencies**: T045, T046, T047

- [ ] **T049** Run subscription-manager module tests
  - **Files**: N/A (validation step)
  - **Description**: Execute `mvnw test -pl kafka-platform-subscription-manager`
  - **Acceptance**: All tests T032-T038 now PASS
  - **Dependencies**: T039-T048

- [ ] **T050** Build subscription-manager module JAR
  - **Files**: N/A (validation step)
  - **Description**: Execute `mvnw clean install -pl kafka-platform-subscription-manager`
  - **Acceptance**: Executable JAR created
  - **Dependencies**: T049

---

## Phase 3.8: Tests First - Price Subscriber Module (T051-T058) ⚠️ TDD GATE

- [ ] **T051** [P] PriceUpdateConsumer unit test
  - **Files**: `kafka-platform-price-subscriber/src/test/java/net/prmx/kafka/platform/subscriber/consumer/PriceUpdateConsumerTest.java`
  - **Description**: Test Kafka consumer for price updates with filtering
  - **Acceptance**: Test compiles, runs, and FAILS
  - **Test Cases**:
    - Consumer receives PriceUpdate messages
    - Filter applied before processing
    - Statistics updated for matching instruments
  - **Dependencies**: T004, T017

- [ ] **T052** [P] SubscriptionCommandConsumer unit test
  - **Files**: `kafka-platform-price-subscriber/src/test/java/net/prmx/kafka/platform/subscriber/consumer/SubscriptionCommandConsumerTest.java`
  - **Description**: Test Kafka consumer for subscription commands
  - **Acceptance**: Test compiles, runs, and FAILS
  - **Test Cases**:
    - Consumer receives SubscriptionCommand messages
    - Subscription state updated based on action (ADD/REMOVE/REPLACE)
  - **Dependencies**: T004, T017

- [ ] **T053** [P] PriceFilterService unit test
  - **Files**: `kafka-platform-price-subscriber/src/test/java/net/prmx/kafka/platform/subscriber/service/PriceFilterServiceTest.java`
  - **Description**: Test in-memory Set-based filtering
  - **Acceptance**: Test compiles, runs, and FAILS
  - **Test Cases**:
    - shouldProcess() returns true for subscribed instrument
    - shouldProcess() returns false for non-subscribed instrument
    - updateSubscriptions() replaces subscription list
    - Thread-safe operations
  - **Dependencies**: T004, T017

- [ ] **T054** [P] StatisticsAggregator unit test
  - **Files**: `kafka-platform-price-subscriber/src/test/java/net/prmx/kafka/platform/subscriber/service/StatisticsAggregatorTest.java`
  - **Description**: Test 5-second window aggregation with atomic counters
  - **Acceptance**: Test compiles, runs, and FAILS
  - **Test Cases**:
    - recordMessage() increments counters
    - logAndResetStatistics() logs and resets
    - @Scheduled triggers every 5 seconds
    - Zero-count logging works
  - **Dependencies**: T004, T017

- [ ] **T055** [P] SubscriptionManager (state) unit test
  - **Files**: `kafka-platform-price-subscriber/src/test/java/net/prmx/kafka/platform/subscriber/service/SubscriptionManagerTest.java`
  - **Description**: Test ADD/REMOVE/REPLACE logic for subscription state
  - **Acceptance**: Test compiles, runs, and FAILS
  - **Test Cases**:
    - ADD action adds instruments to existing set
    - REMOVE action removes instruments from set
    - REPLACE action replaces entire set
    - Thread-safe state updates
  - **Dependencies**: T004, T017

- [ ] **T056** [P] Price subscriber integration test (embedded Kafka)
  - **Files**: `kafka-platform-price-subscriber/src/test/java/net/prmx/kafka/platform/subscriber/integration/PriceSubscriberIntegrationTest.java`
  - **Description**: End-to-end test with dual consumers (prices + commands)
  - **Acceptance**: Test compiles, runs, and FAILS
  - **Test Cases**:
    - Publish SubscriptionCommand, verify filter updated
    - Publish PriceUpdate for subscribed instrument, verify statistics
    - Publish PriceUpdate for non-subscribed instrument, verify filtered out
    - Test dynamic subscription change without restart
  - **Dependencies**: T004, T017

---

## Phase 3.9: Core Implementation - Price Subscriber Module (T057-T066)

- [ ] **T057** [P] Implement PriceStatistics model
  - **Files**: `kafka-platform-price-subscriber/src/main/java/net/prmx/kafka/platform/subscriber/model/PriceStatistics.java`
  - **Description**: Mutable state object for 5-second window as per data-model.md
  - **Acceptance**: Used in StatisticsAggregator
  - **Implementation**:
    - Class with AtomicLong for totalMessages
    - ConcurrentHashMap for instrumentCounts
    - recordMessage(), getTotalMessages(), getUniqueInstruments(), reset()
  - **Dependencies**: None

- [ ] **T058** [P] Implement PriceFilterService
  - **Files**: `kafka-platform-price-subscriber/src/main/java/net/prmx/kafka/platform/subscriber/service/PriceFilterService.java`
  - **Description**: In-memory Set-based filtering service
  - **Acceptance**: T053 now PASSES
  - **Implementation**:
    - @Component annotation
    - ConcurrentHashMap.newKeySet() for subscribed instruments
    - shouldProcess(PriceUpdate) method
    - updateSubscriptions(Set<String>) method
  - **Dependencies**: T053

- [ ] **T059** [P] Implement StatisticsAggregator
  - **Files**: `kafka-platform-price-subscriber/src/main/java/net/prmx/kafka/platform/subscriber/service/StatisticsAggregator.java`
  - **Description**: 5-second aggregation with scheduled logging
  - **Acceptance**: T054 now PASSES
  - **Implementation**:
    - @Service annotation
    - @EnableScheduling in configuration
    - @Scheduled(fixedRate = 5000) on logAndResetStatistics()
    - recordMessage(PriceUpdate) updates counters
    - Log includes totalMessages, uniqueInstruments
    - Structured JSON logging
  - **Dependencies**: T054, T057

- [ ] **T060** [P] Implement SubscriptionManager (state management)
  - **Files**: `kafka-platform-price-subscriber/src/main/java/net/prmx/kafka/platform/subscriber/service/SubscriptionManager.java`
  - **Description**: Manages subscription state with ADD/REMOVE/REPLACE logic
  - **Acceptance**: T055 now PASSES
  - **Implementation**:
    - @Service annotation
    - Inject PriceFilterService
    - applyCommand(SubscriptionCommand) method
    - Switch on action type (ADD, REMOVE, REPLACE)
    - Update filter service
  - **Dependencies**: T055, T058

- [ ] **T061** Implement PriceUpdateConsumer
  - **Files**: `kafka-platform-price-subscriber/src/main/java/net/prmx/kafka/platform/subscriber/consumer/PriceUpdateConsumer.java`
  - **Description**: Kafka consumer for price updates with filtering
  - **Acceptance**: T051 now PASSES
  - **Implementation**:
    - @Component annotation
    - @KafkaListener(topics = "price-updates", groupId = "price-subscriber-group")
    - Inject PriceFilterService and StatisticsAggregator
    - Filter and record messages
    - Error handling with @KafkaRetryTopic
  - **Dependencies**: T051, T058, T059

- [ ] **T062** Implement SubscriptionCommandConsumer
  - **Files**: `kafka-platform-price-subscriber/src/main/java/net/prmx/kafka/platform/subscriber/consumer/SubscriptionCommandConsumer.java`
  - **Description**: Kafka consumer for subscription commands
  - **Acceptance**: T052 now PASSES
  - **Implementation**:
    - @Component annotation
    - @KafkaListener(topics = "subscription-commands", groupId = "price-subscriber-group")
    - Inject SubscriptionManager
    - Apply commands to update filter
    - Handle compacted topic (seek to beginning on startup)
  - **Dependencies**: T052, T060

- [ ] **T063** [P] Implement KafkaConsumerConfig
  - **Files**: `kafka-platform-price-subscriber/src/main/java/net/prmx/kafka/platform/subscriber/config/KafkaConsumerConfig.java`
  - **Description**: Spring Kafka consumer configuration for dual consumers
  - **Acceptance**: Both consumers work
  - **Implementation**:
    - @Configuration annotation
    - @EnableKafka annotation
    - ConsumerFactory beans for PriceUpdate and SubscriptionCommand
    - JSON deserializer with trusted packages
    - Group ID configuration
  - **Dependencies**: None

- [ ] **T064** Configure application.yml for price-subscriber
  - **Files**: `kafka-platform-price-subscriber/src/main/resources/application.yml`
  - **Description**: Spring Boot configuration with dual consumers
  - **Acceptance**: Service starts, consumes from both topics
  - **Implementation**:
    - spring.application.name: price-subscriber
    - spring.kafka.bootstrap-servers
    - spring.kafka.consumer configuration
    - spring.kafka.consumer.properties.spring.json.trusted.packages
    - kafka.topics (price-updates, subscription-commands)
    - server.port: 8082
    - management.endpoints
    - logging configuration
    - spring.scheduling.enabled: true
  - **Dependencies**: None

- [ ] **T065** Implement PriceSubscriberApplication main class
  - **Files**: `kafka-platform-price-subscriber/src/main/java/net/prmx/kafka/platform/subscriber/PriceSubscriberApplication.java`
  - **Description**: Spring Boot application entry point with scheduling enabled
  - **Acceptance**: Application starts, consumes messages, logs statistics
  - **Implementation**:
    - @SpringBootApplication annotation
    - @EnableScheduling annotation
    - main() method
  - **Dependencies**: T061, T062, T063, T064

- [ ] **T066** Run price-subscriber module tests
  - **Files**: N/A (validation step)
  - **Description**: Execute `mvnw test -pl kafka-platform-price-subscriber`
  - **Acceptance**: All tests T051-T056 now PASS
  - **Dependencies**: T057-T065

- [ ] **T067** Build price-subscriber module JAR
  - **Files**: N/A (validation step)
  - **Description**: Execute `mvnw clean install -pl kafka-platform-price-subscriber`
  - **Acceptance**: Executable JAR created
  - **Dependencies**: T066

---

## Phase 3.10: Integration & Validation (T068-T075)

- [ ] **T068** Full multi-module build
  - **Files**: N/A (validation step)
  - **Description**: Execute `mvnw clean install` from repository root
  - **Acceptance**: All 4 modules build successfully, all tests pass
  - **Dependencies**: T017, T031, T050, T067

- [ ] **T069** Start local Kafka (Docker Compose)
  - **Files**: `docker-compose.yml` (create at repository root)
  - **Description**: Create Docker Compose file for Zookeeper + Kafka, start containers
  - **Acceptance**: Kafka broker accessible at localhost:9092
  - **Implementation**: Use docker-compose.yml from quickstart.md
  - **Dependencies**: T068

- [ ] **T070** Create Kafka topics
  - **Files**: N/A (command execution)
  - **Description**: Run kafka-topics commands to create price-updates and subscription-commands topics
  - **Acceptance**: Both topics exist with correct configurations (partitions, retention, compaction)
  - **Implementation**: Use commands from quickstart.md Step 2
  - **Dependencies**: T069

- [ ] **T071** Execute quickstart.md Steps 4-6 (Run services)
  - **Files**: N/A (validation)
  - **Description**: Start all 3 services, verify they connect to Kafka
  - **Acceptance**: 
    - Price generator logs "Generating price updates"
    - Subscriber logs "Statistics: totalMessages=0"
    - Subscription manager REST API responds to health check
  - **Implementation**: Follow quickstart.md Steps 4-6
  - **Dependencies**: T070

- [ ] **T072** Execute quickstart.md Steps 5-7 (Configure and observe)
  - **Files**: N/A (validation)
  - **Description**: Use REST API to configure subscriptions, observe price flow
  - **Acceptance**: 
    - curl PUT /subscriptions/subscriber-001 returns 200 OK
    - Subscriber logs show non-zero message counts
    - Dynamic subscription changes reflected in statistics
  - **Implementation**: Follow quickstart.md Steps 5-7
  - **Dependencies**: T071

- [ ] **T073** Validate acceptance scenarios 1-6
  - **Files**: N/A (validation)
  - **Description**: Execute all 6 acceptance scenarios from quickstart.md
  - **Acceptance**: All scenarios pass
  - **Scenarios**:
    1. Generator publishes at 100ms-1s intervals
    2. Subscriber receives only subscribed instruments
    3. Statistics logged every 5 seconds
    4. Dynamic subscription changes without restart
    5. Multiple subscribers work independently
    6. State recovery after restart (compaction)
  - **Dependencies**: T072

- [ ] **T074** Performance validation
  - **Files**: N/A (validation)
  - **Description**: Measure price update latency (generation to consumption)
  - **Acceptance**: p95 < 500ms, p99 < 1s (as per specification FR-020)
  - **Implementation**: Add timestamps, calculate latency, use metrics endpoint
  - **Dependencies**: T073

- [ ] **T075** Update README.md with quickstart
  - **Files**: `README.md` (create or update at repository root)
  - **Description**: Add project overview, architecture diagram, quickstart instructions
  - **Acceptance**: README explains what the project does and how to run it
  - **Implementation**: 
    - Project description
    - Architecture overview (3 services, 2 topics)
    - Prerequisites (Java 25, Maven, Kafka)
    - Quick start steps (link to quickstart.md)
    - Module descriptions
  - **Dependencies**: T073

---

## Dependencies Graph

```
Setup Phase:
T001 (root POM) 
  → T002 [P] (common module) → T017 (common JAR)
  → T003 [P] (generator module)
  → T004 [P] (subscriber module)
  → T005 [P] (subscription-manager module)

Common Module Tests (can run in parallel after T002):
T006 [P] ┐
T007 [P] │
T008 [P] ├→ T013 [P] (PriceUpdate impl)      ┐
T009 [P] │                                     ├→ T016 (test) → T017 (JAR)
T010 [P] ├→ T014 [P] (SubscriptionCommand)   │
T011 [P] │                                     │
T012 [P] ┘  T015 [P] (InstrumentValidator)   ┘

Price Generator (depends on T017):
T018 [P] ┐
T019 [P] ├→ T024-T029 [P] (implementations) → T030 (test) → T031 (JAR)
T020 [P] │
T021 [P] │
T022 [P] │
T023 [P] ┘

Subscription Manager (depends on T017):
T032 [P] ┐
T033 [P] │
T034 [P] ├→ T039-T048 (implementations) → T049 (test) → T050 (JAR)
T035 [P] │
T036 [P] │
T037 [P] │
T038 [P] ┘

Price Subscriber (depends on T017):
T051 [P] ┐
T052 [P] │
T053 [P] ├→ T057-T065 (implementations) → T066 (test) → T067 (JAR)
T054 [P] │
T055 [P] │
T056 [P] ┘

Final Integration:
T068 (full build) → T069 (Kafka) → T070 (topics) → T071-T075 (validation)
```

---

## Parallel Execution Examples

### Example 1: Common Module Contract Tests (after T002)
Run all 7 contract tests in parallel:
```powershell
# These tests touch different files and have no dependencies
mvnw test -pl kafka-platform-common -Dtest=PriceUpdateSerializationTest &
mvnw test -pl kafka-platform-common -Dtest=PriceUpdateValidationTest &
mvnw test -pl kafka-platform-common -Dtest=SubscriptionCommandSerializationTest &
mvnw test -pl kafka-platform-common -Dtest=SubscriptionCommandValidationTest &
mvnw test -pl kafka-platform-common -Dtest=InstrumentValidatorTest &
mvnw test -pl kafka-platform-common -Dtest=PriceUpdateKafkaContractTest &
mvnw test -pl kafka-platform-common -Dtest=SubscriptionCommandKafkaContractTest
```

### Example 2: Service Module Tests (after T017)
Run all 3 service module test suites in parallel:
```powershell
# Each module is independent
mvnw test -pl kafka-platform-price-generator &
mvnw test -pl kafka-platform-subscription-manager &
mvnw test -pl kafka-platform-price-subscriber
```

### Example 3: Module Structures (after T001)
Create all module directories in parallel:
```powershell
# Run T002, T003, T004, T005 together (different directories)
```

---

## Task Execution Notes

### TDD Enforcement
- **CRITICAL**: All tests marked with ⚠️ MUST be written and MUST FAIL before implementation
- Run each test phase completely before proceeding to implementation phase
- Verify test failure before implementing (confirms test is valid)
- Verify test passes after implementing (confirms implementation is correct)

### Parallel Execution ([P] marker)
- Tasks marked [P] modify different files and have no shared dependencies
- Can be executed simultaneously by different developers or CI jobs
- Reduces total implementation time from sequential ~67 tasks to ~30 task "rounds"

### File Path Conventions
- All paths are relative to repository root: `kafka-platform/`
- Follow multi-module Maven structure exactly as planned
- Package naming: `net.prmx.kafka.platform.{module}.{layer}`

### Constitutional Compliance
- Every task maintains Java 25 and Spring Boot 3.x requirements
- Kafka messaging for all inter-service communication
- Multi-module Maven architecture strictly followed
- TDD approach with contract tests before implementation
- Structured logging and actuator endpoints in all services

### Integration Testing Strategy
- Common module: Unit + Kafka contract tests (embedded Kafka)
- Service modules: Unit + Integration tests (embedded Kafka + MockMvc)
- Final validation: End-to-end with real Kafka (Docker Compose)

---

## Validation Checklist

Before marking tasks.md as complete, verify:

- [x] All contracts have corresponding test tasks (T006-T012, T018-T023, T032-T038, T051-T056)
- [x] All entities have model implementation tasks (T013-T015, T057)
- [x] All tests come before implementation (Phase 3.2 before 3.3, etc.)
- [x] Parallel tasks truly independent (different files, no shared state)
- [x] Each task specifies exact file path
- [x] No task modifies same file as another [P] task
- [x] All quickstart scenarios covered in validation phase (T073)
- [x] Performance requirements validated (T074)
- [x] Documentation updated (T075)

---

## Estimated Timeline

**Total Tasks**: 67  
**Parallel-Eligible Tasks**: 24  
**Sequential Tasks**: 43

**Estimated Effort** (with parallel execution):
- Setup: 1-2 hours (T001-T005)
- Common Module: 3-4 hours (T006-T017)
- Price Generator: 4-5 hours (T018-T031)
- Subscription Manager: 5-6 hours (T032-T050)
- Price Subscriber: 6-7 hours (T051-T067)
- Integration & Validation: 2-3 hours (T068-T075)

**Total**: ~22-27 hours (single developer)  
**With 3 parallel developers**: ~12-15 hours

---

**Status**: Tasks.md generated and ready for execution  
**Next Step**: Begin with T001 (Update root pom.xml)  
**Command**: Select task T001 and execute following TDD principles
