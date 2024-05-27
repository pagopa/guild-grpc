package it.pagopa.guild.grpc.booking.client.impl;

import it.pagopa.guild.grpc.booking.client.VehicleClient;
import it.pagopa.guild.grpc.booking.client.VehicleClientFeign;
import it.pagopa.guild.grpc.booking.dto.AckResponseDto;
import it.pagopa.guild.grpc.booking.dto.BookRequestDto;
import it.pagopa.guild.grpc.booking.dto.LocationDto;
import it.pagopa.guild.grpc.booking.exception.BookingConfirmationException;
import org.springframework.stereotype.Service;

import it.pagopa.guild.grpc.booking.utils.Constants;

@Service
public class VehicleClientRest implements VehicleClient {
    private final VehicleClientFeign vehicleClientFeign;

    public VehicleClientRest(VehicleClientFeign vehicleClientFeign) {
        this.vehicleClientFeign = vehicleClientFeign;
    }
    @Override
    public AckResponseDto sendBookConfirmation(String userId, String vehicleId, Double lat, Double lon) throws BookingConfirmationException {
        BookRequestDto request = BookRequestDto.builder()
                .userId(userId)
                .vehicleId(vehicleId)
                .location(LocationDto.builder().latitude(lat).longitude(lon).build())
                .build();
        try {
            return vehicleClientFeign.sendBookConfirmation(request);
        } catch (Exception e) {
            throw new BookingConfirmationException(Constants.VEHICLE_SEND_CONFIRMATION_FAILURE, e);
        }
    }
}
