package it.pagopa.guild.grpc.booking.exception;

import it.pagopa.guild.grpc.booking.controller.BookingController;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import it.pagopa.guild.grpc.booking.utils.Constants;

@ControllerAdvice(assignableTypes = {BookingController.class})
@Slf4j
public class BookingRestExceptionHandler {

    @Data
    @Builder
    public static class ErrorResponse {
        private String message;
    }

    @Data
    @Builder
    public static class ValidationErrorResponse {
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
                .body(ErrorResponse.builder().message(Constants.VEHICLE_ALREADY_BOOKED).build());
    }

    @ExceptionHandler(ResourceNotAvailableException.class)
    ResponseEntity<ErrorResponse> handleBookingConfirmationExceptionError(BookingConfirmationException e) {
        log.error("vehicle sendBookConfirmation REST failed", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder().message(e.getError()).build());
    }

    ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValidError(MethodArgumentNotValidException e) {
        Map<String, String> errors = e.getBindingResult().getAllErrors().stream()
                .collect(Collectors.toMap(
                        error -> ((FieldError) error).getField(),
                        error -> error.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ValidationErrorResponse.builder().errors(errors).build());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleGenericError(Exception e) {
        log.error("Generic error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder().message(Constants.GENERIC_ERROR).build());
    }
}
