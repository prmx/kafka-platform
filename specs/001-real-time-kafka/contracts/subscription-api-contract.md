# REST API Contract: Subscription Manager

**Service**: kafka-platform-subscription-manager  
**Base URL**: `http://localhost:8080` (configurable)  
**Version**: 1.0.0

## Overview
REST API for managing subscriber instrument subscriptions. Changes are published to the `subscription-commands` Kafka topic for consumption by subscriber instances.

---

## Endpoints

### 1. Replace Subscription List
**Operation**: Replace the entire subscription list for a subscriber

**Request**:
```http
PUT /api/v1/subscriptions/{subscriberId}
Content-Type: application/json

{
  "instrumentIds": ["KEY000001", "KEY000050", "KEY001000"]
}
```

**Path Parameters**:
- `subscriberId` (string, required): Unique identifier for the subscriber instance

**Request Body**:
```json
{
  "instrumentIds": ["string", "..."]
}
```

**Success Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "subscriberId": "subscriber-001",
  "action": "REPLACE",
  "instrumentIds": ["KEY000001", "KEY000050", "KEY001000"],
  "timestamp": 1696348800000,
  "status": "PUBLISHED"
}
```

**Error Responses**:
```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "error": "VALIDATION_ERROR",
  "message": "Invalid instrument ID: INVALID_ID",
  "timestamp": 1696348800000
}
```

```http
HTTP/1.1 500 Internal Server Error
Content-Type: application/json

{
  "error": "KAFKA_PUBLISH_ERROR",
  "message": "Failed to publish command to Kafka after retries",
  "timestamp": 1696348800000
}
```

---

### 2. Add Instruments to Subscription
**Operation**: Add instruments to the existing subscription list

**Request**:
```http
POST /api/v1/subscriptions/{subscriberId}/add
Content-Type: application/json

{
  "instrumentIds": ["KEY000999"]
}
```

**Path Parameters**:
- `subscriberId` (string, required): Unique identifier for the subscriber instance

**Request Body**:
```json
{
  "instrumentIds": ["string", "..."]
}
```

**Success Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "subscriberId": "subscriber-001",
  "action": "ADD",
  "instrumentIds": ["KEY000999"],
  "timestamp": 1696348801000,
  "status": "PUBLISHED"
}
```

**Error Responses**: Same as Replace endpoint

---

### 3. Remove Instruments from Subscription
**Operation**: Remove instruments from the existing subscription list

**Request**:
```http
POST /api/v1/subscriptions/{subscriberId}/remove
Content-Type: application/json

{
  "instrumentIds": ["KEY000050"]
}
```

**Path Parameters**:
- `subscriberId` (string, required): Unique identifier for the subscriber instance

**Request Body**:
```json
{
  "instrumentIds": ["string", "..."]
}
```

**Success Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "subscriberId": "subscriber-002",
  "action": "REMOVE",
  "instrumentIds": ["KEY000050"],
  "timestamp": 1696348802000,
  "status": "PUBLISHED"
}
```

**Error Responses**: Same as Replace endpoint

---

### 4. Health Check (Actuator)
**Operation**: Check service health and readiness

**Request**:
```http
GET /actuator/health
```

**Success Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "status": "UP",
  "components": {
    "kafka": {
      "status": "UP",
      "details": {
        "clusterId": "demo-cluster"
      }
    }
  }
}
```

---

## Data Models

### SubscriptionRequest (Request DTO)
```java
package net.prmx.kafka.platform.manager.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record SubscriptionRequest(
    @NotEmpty(message = "instrumentIds cannot be empty")
    List<@Pattern(regexp = "KEY\\d{6}", message = "Invalid instrument ID format") 
         String> instrumentIds
) {}
```

### SubscriptionResponse (Response DTO)
```java
package net.prmx.kafka.platform.manager.dto;

import java.util.List;

public record SubscriptionResponse(
    String subscriberId,
    String action,  // ADD, REMOVE, REPLACE
    List<String> instrumentIds,
    long timestamp,
    String status   // PUBLISHED, FAILED
) {}
```

### ErrorResponse (Error DTO)
```java
package net.prmx.kafka.platform.manager.dto;

public record ErrorResponse(
    String error,      // Error code
    String message,    // Human-readable message
    long timestamp
) {}
```

---

## Validation Rules

### Subscriber ID Validation
- **Pattern**: Not blank, max 100 characters
- **Error**: 400 Bad Request if validation fails

### Instrument ID Validation
- **Pattern**: `KEY\d{6}` (KEY followed by exactly 6 digits)
- **Range**: KEY000000 to KEY999999
- **Error**: 400 Bad Request if any instrument ID is invalid

### Request Body Validation
- **instrumentIds**: Must not be null or empty
- **Error**: 400 Bad Request with details

---

## Contract Tests

### Test: Replace Subscription Success
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class SubscriptionControllerContractTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void testReplaceSubscription() throws Exception {
        String subscriberId = "subscriber-test";
        String requestBody = """
            {
              "instrumentIds": ["KEY000001", "KEY000050"]
            }
            """;
        
        mockMvc.perform(put("/api/v1/subscriptions/" + subscriberId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subscriberId").value(subscriberId))
            .andExpect(jsonPath("$.action").value("REPLACE"))
            .andExpect(jsonPath("$.instrumentIds").isArray())
            .andExpect(jsonPath("$.instrumentIds[0]").value("KEY000001"))
            .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }
}
```

### Test: Add Instruments Success
```java
@Test
public void testAddInstruments() throws Exception {
    String subscriberId = "subscriber-test";
    String requestBody = """
        {
          "instrumentIds": ["KEY000999"]
        }
        """;
    
    mockMvc.perform(post("/api/v1/subscriptions/" + subscriberId + "/add")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.action").value("ADD"));
}
```

### Test: Remove Instruments Success
```java
@Test
public void testRemoveInstruments() throws Exception {
    String subscriberId = "subscriber-test";
    String requestBody = """
        {
          "instrumentIds": ["KEY000001"]
        }
        """;
    
    mockMvc.perform(post("/api/v1/subscriptions/" + subscriberId + "/remove")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.action").value("REMOVE"));
}
```

### Test: Invalid Instrument ID Returns 400
```java
@Test
public void testInvalidInstrumentIdReturns400() throws Exception {
    String subscriberId = "subscriber-test";
    String requestBody = """
        {
          "instrumentIds": ["INVALID_ID"]
        }
        """;
    
    mockMvc.perform(put("/api/v1/subscriptions/" + subscriberId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").exists());
}
```

### Test: Empty Instrument List Returns 400
```java
@Test
public void testEmptyInstrumentListReturns400() throws Exception {
    String subscriberId = "subscriber-test";
    String requestBody = """
        {
          "instrumentIds": []
        }
        """;
    
    mockMvc.perform(put("/api/v1/subscriptions/" + subscriberId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
}
```

### Test: Health Endpoint Returns UP
```java
@Test
public void testHealthEndpoint() throws Exception {
    mockMvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
}
```

---

## Error Handling

### Client Errors (4xx)
- **400 Bad Request**: Invalid request body, validation failures
- **404 Not Found**: Invalid endpoint path
- **405 Method Not Allowed**: Wrong HTTP method for endpoint

### Server Errors (5xx)
- **500 Internal Server Error**: Kafka publish failure, unexpected exceptions
- **503 Service Unavailable**: Service is starting up or shutting down

### Error Response Format
All errors return consistent JSON structure:
```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable description",
  "timestamp": 1696348800000
}
```

---

## Concurrency & Thread Safety

### Request Serialization
- Concurrent requests for the **same subscriberId** are serialized
- Uses `ConcurrentHashMap<String, ReentrantLock>` per subscriber
- Prevents race conditions when multiple requests arrive simultaneously
- Requests for **different subscriberIds** execute in parallel

### Implementation Pattern
```java
@Service
public class RequestSerializationService {
    private final ConcurrentHashMap<String, ReentrantLock> locks = 
        new ConcurrentHashMap<>();
    
    public <T> T executeSerializedForSubscriber(String subscriberId, 
                                                 Supplier<T> operation) {
        ReentrantLock lock = locks.computeIfAbsent(
            subscriberId, k -> new ReentrantLock());
        lock.lock();
        try {
            return operation.get();
        } finally {
            lock.unlock();
        }
    }
}
```

---

## Performance Requirements

- **Latency**: p95 < 100ms for successful requests
- **Throughput**: 10 requests/second per subscriber (demonstration scale)
- **Concurrent Subscribers**: Support 100+ unique subscriber IDs

---

## Breaking Change Policy

**MINOR version changes** (backward-compatible):
- Adding new endpoints
- Adding optional request fields
- Adding response fields

**MAJOR version changes** (breaking):
- Removing endpoints
- Changing endpoint paths
- Removing request/response fields
- Changing HTTP status code semantics

---

## Status
- [ ] Contract defined
- [ ] Tests written (must fail before implementation)
- [ ] Tests passing (after implementation)
