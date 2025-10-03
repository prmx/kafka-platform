package net.prmx.kafka.platform.manager.controller;

import jakarta.validation.Valid;
import net.prmx.kafka.platform.common.model.SubscriptionCommand;
import net.prmx.kafka.platform.manager.dto.ErrorResponse;
import net.prmx.kafka.platform.manager.dto.SubscriptionRequest;
import net.prmx.kafka.platform.manager.dto.SubscriptionResponse;
import net.prmx.kafka.platform.manager.service.SubscriptionCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

/**
 * T045: REST controller for subscription management
 * Provides PUT and POST endpoints for REPLACE, ADD, and REMOVE operations
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionController.class);

    private final SubscriptionCommandService commandService;

    public SubscriptionController(SubscriptionCommandService commandService) {
        this.commandService = commandService;
    }

    /**
     * REPLACE subscription - replaces entire instrument list
     */
    @PutMapping("/{subscriberId}")
    public ResponseEntity<SubscriptionResponse> replaceSubscription(
            @PathVariable String subscriberId,
            @Valid @RequestBody SubscriptionRequest request) {
        
        log.info("REPLACE subscription request: subscriberId={}, instruments={}", 
                subscriberId, request.instrumentIds().size());

        SubscriptionCommand command = commandService.publishCommand(
                subscriberId, 
                SubscriptionCommand.Action.REPLACE, 
                request.instrumentIds()
        );

        SubscriptionResponse response = SubscriptionResponse.fromCommand(command, "PUBLISHED");
        return ResponseEntity.ok(response);
    }

    /**
     * ADD instruments to existing subscription
     */
    @PostMapping("/{subscriberId}/add")
    public ResponseEntity<SubscriptionResponse> addInstruments(
            @PathVariable String subscriberId,
            @Valid @RequestBody SubscriptionRequest request) {
        
        log.info("ADD instruments request: subscriberId={}, instruments={}", 
                subscriberId, request.instrumentIds().size());

        SubscriptionCommand command = commandService.publishCommand(
                subscriberId, 
                SubscriptionCommand.Action.ADD, 
                request.instrumentIds()
        );

        SubscriptionResponse response = SubscriptionResponse.fromCommand(command, "PUBLISHED");
        return ResponseEntity.ok(response);
    }

    /**
     * REMOVE instruments from existing subscription
     */
    @PostMapping("/{subscriberId}/remove")
    public ResponseEntity<SubscriptionResponse> removeInstruments(
            @PathVariable String subscriberId,
            @Valid @RequestBody SubscriptionRequest request) {
        
        log.info("REMOVE instruments request: subscriberId={}, instruments={}", 
                subscriberId, request.instrumentIds().size());

        SubscriptionCommand command = commandService.publishCommand(
                subscriberId, 
                SubscriptionCommand.Action.REMOVE, 
                request.instrumentIds()
        );

        SubscriptionResponse response = SubscriptionResponse.fromCommand(command, "PUBLISHED");
        return ResponseEntity.ok(response);
    }

    /**
     * Exception handler for validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        StringBuilder message = new StringBuilder("Validation failed: ");
        
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            message.append(error.getField()).append(" - ").append(error.getDefaultMessage()).append("; ");
        }

        ErrorResponse errorResponse = ErrorResponse.of("VALIDATION_ERROR", message.toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Exception handler for illegal arguments (e.g., invalid instrument IDs)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse errorResponse = ErrorResponse.of("INVALID_REQUEST", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
