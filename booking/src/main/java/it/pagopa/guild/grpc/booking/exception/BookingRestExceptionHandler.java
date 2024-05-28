package it.pagopa.guild.grpc.booking.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.guild.grpc.booking.controller.BookingController;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.stream.Collectors;

import it.pagopa.guild.grpc.booking.utils.Constants;

@ControllerAdvice(assignableTypes = {BookingController.class})
@Slf4j
public class BookingRestExceptionHandler {

    @Data
    @SuperBuilder
    public static class ErrorResponse {
        @Schema(description = "Feedback about the success of the operation", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean success;

        @Schema(description = "Error message", example = "Vehicle booking failed")
        String message;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @SuperBuilder
    public static class ValidationErrorResponse extends ErrorResponse {
        @Schema(description = "Map of error messages", example = "{\"userId\": \"UserId is required\"}")
        private Map<String, String> errors;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorResponse> handleResourceNotFoundExceptionError(ResourceNotFoundException e) {
        log.error("Vehicle booking failed", e);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder().message(Constants.VEHICLE_NOT_FOUND).build());
    }

    @ExceptionHandler(ResourceNotAvailableException.class)
    ResponseEntity<ErrorResponse> handleResourceNotAvailableExceptionError(ResourceNotAvailableException e) {
        log.error("Vehicle booking failed", e);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.builder().success(false).message(Constants.VEHICLE_ALREADY_BOOKED).build());
    }

    @ExceptionHandler(BookingConfirmationException.class)
    ResponseEntity<ErrorResponse> handleBookingConfirmationExceptionError(BookingConfirmationException e) {
        log.error("vehicle sendBookConfirmation REST failed", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.builder().success(false).message(e.getError()).build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValidError(MethodArgumentNotValidException e) {
        Map<String, String> errors = e.getBindingResult().getAllErrors().stream()
                .collect(Collectors.toMap(
                        error -> ((FieldError) error).getField(),
                        error -> error.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ValidationErrorResponse.builder()
                        .success(false)
                        .message(Constants.VEHICLE_ALREADY_BOOKED)
                        .errors(errors).build());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleGenericError(Exception e) {
        log.error("Generic error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder().success(false).message(Constants.GENERIC_ERROR).build());
    }
}
