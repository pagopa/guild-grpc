package it.pagopa.guild.grpc.booking.client.impl;

import io.grpc.StatusRuntimeException;
import it.pagopa.guild.grpc.booking.Common;
import it.pagopa.guild.grpc.booking.Vehicle;
import it.pagopa.guild.grpc.booking.VehicleServiceGrpc;
import it.pagopa.guild.grpc.booking.client.VehicleClient;
import it.pagopa.guild.grpc.booking.dto.AckResponseDto;
import it.pagopa.guild.grpc.booking.exception.BookingConfirmationException;
import it.pagopa.guild.grpc.booking.mapper.BookingMapper;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.pagopa.guild.grpc.booking.utils.Constants;

@Service
public class VehicleClientGrpc implements VehicleClient {
    @GrpcClient("vehicle")
    private VehicleServiceGrpc.VehicleServiceBlockingStub vehicleServiceStub;

    @Autowired
    private BookingMapper bookingMapper;

    @Override
    public AckResponseDto sendBookConfirmation(String userId, String vehicleId, Double lat, Double lon) throws BookingConfirmationException {
        var bookConfirmationRequest = Vehicle.BookConfirmationRequest.newBuilder()
                .setUserId(userId)
                .setVehicleId(vehicleId)
                .setLocation(Common.Location.newBuilder()
                        .setLatitude(lat)
                        .setLongitude(lon)
                        .build())
                .build();
        try {
            return bookingMapper.toAckResponseDto(vehicleServiceStub.bookConfirmation(bookConfirmationRequest));
        } catch (StatusRuntimeException e) {
            throw new BookingConfirmationException(Constants.VEHICLE_SEND_CONFIRMATION_FAILURE, e);
        }
    }
}
