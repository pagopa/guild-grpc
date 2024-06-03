package it.pagopa.guild.grpc.booking.client;

import it.pagopa.guild.grpc.booking.dto.AckResponseDto;
import it.pagopa.guild.grpc.booking.exception.BookingConfirmationException;

public interface VehicleClient {
    AckResponseDto sendBookConfirmation(String userId, String vehicleId, Double lat, Double lon) throws BookingConfirmationException;
}
