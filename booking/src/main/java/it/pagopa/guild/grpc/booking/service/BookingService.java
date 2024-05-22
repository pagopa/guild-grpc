package it.pagopa.guild.grpc.booking.service;

import it.pagopa.guild.grpc.booking.dto.AckResponseDto;
import it.pagopa.guild.grpc.booking.dto.LocationDto;
import it.pagopa.guild.grpc.booking.exception.BookingConfirmationException;
import it.pagopa.guild.grpc.booking.exception.ResourceNotAvailableException;
import it.pagopa.guild.grpc.booking.exception.ResourceNotFoundException;

public interface BookingService {
    AckResponseDto bookVehicle(String vehicleId, String userId, LocationDto locationDto)
            throws ResourceNotAvailableException, ResourceNotFoundException, BookingConfirmationException;
}
