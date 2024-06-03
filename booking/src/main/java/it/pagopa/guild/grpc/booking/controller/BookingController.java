package it.pagopa.guild.grpc.booking.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import it.pagopa.guild.grpc.booking.dto.AckResponseDto;
import it.pagopa.guild.grpc.booking.dto.BookRequestDto;
import it.pagopa.guild.grpc.booking.exception.BookingConfirmationException;
import it.pagopa.guild.grpc.booking.exception.BookingRestExceptionHandler;
import it.pagopa.guild.grpc.booking.exception.ResourceNotAvailableException;
import it.pagopa.guild.grpc.booking.exception.ResourceNotFoundException;
import it.pagopa.guild.grpc.booking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/booking")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Operation(summary = "Book a vehicle by vechicle id, for a user", description = "Returns a success feedback")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Vehicle booked successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AckResponseDto.class)) }),
            @ApiResponse(responseCode = "404",
                    description = "No vehicle found",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BookingRestExceptionHandler.ErrorResponse.class)) }),
            @ApiResponse(responseCode = "409",
                    description = "Vehicle not available (already booked)",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BookingRestExceptionHandler.ErrorResponse.class)) }),
            @ApiResponse(responseCode = "400",
                    description = "Bad request: wrong or missing inputs",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BookingRestExceptionHandler.ValidationErrorResponse.class)) }),
            @ApiResponse(responseCode = "502",
                    description = "Booking confirmation to vehicle service failed",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BookingRestExceptionHandler.ErrorResponse.class)) }),
            @ApiResponse(responseCode = "500",
                    description = "Generic internal error",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BookingRestExceptionHandler.ErrorResponse.class)) }) }
    )
    @PostMapping
    public ResponseEntity<AckResponseDto> bookVehicle(@Valid @RequestBody BookRequestDto bookRequest)
            throws ResourceNotAvailableException, ResourceNotFoundException, BookingConfirmationException {
        AckResponseDto ackResponseDto = bookingService.bookVehicle(bookRequest.getVehicleId(),
                bookRequest.getUserId(), bookRequest.getLocation());
        var httpStatus = Boolean.TRUE.equals(ackResponseDto.getSuccess()) ? HttpStatus.CREATED : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(httpStatus).body(ackResponseDto);
    }

}
