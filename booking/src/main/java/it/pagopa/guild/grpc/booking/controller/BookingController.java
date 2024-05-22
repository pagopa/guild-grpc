package it.pagopa.guild.grpc.booking.controller;


import it.pagopa.guild.grpc.booking.dto.AckResponseDto;
import it.pagopa.guild.grpc.booking.dto.BookRequestDto;
import it.pagopa.guild.grpc.booking.exception.BookingConfirmationException;
import it.pagopa.guild.grpc.booking.exception.ResourceNotAvailableException;
import it.pagopa.guild.grpc.booking.exception.ResourceNotFoundException;
import it.pagopa.guild.grpc.booking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("/booking")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<AckResponseDto> bookVehicle(@Valid @RequestBody BookRequestDto bookRequest)
            throws ResourceNotAvailableException, ResourceNotFoundException, BookingConfirmationException {
        AckResponseDto ackResponseDto = bookingService.bookVehicle(bookRequest.getVehicleId(),
                bookRequest.getUserId(), bookRequest.getLocation());
        var httpStatus = Boolean.TRUE.equals(ackResponseDto.getSuccess()) ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(httpStatus).body(ackResponseDto);
    }

}
