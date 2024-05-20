package it.pagopa.guild.grpc.booking.client;

import it.pagopa.guild.grpc.booking.Common;
import it.pagopa.guild.grpc.booking.Vehicle;
import it.pagopa.guild.grpc.booking.VehicleServiceGrpc;
import it.pagopa.guild.grpc.booking.dto.AckResponseDto;
import it.pagopa.guild.grpc.booking.mapper.BookingMapper;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VehicleClient {
    @GrpcClient("vehicle")
    private VehicleServiceGrpc.VehicleServiceBlockingStub vehicleServiceStub;

    @Autowired
    private BookingMapper bookingMapper;

    public AckResponseDto sendBookConfirmation(String userId, String vehicleId, Double lat, Double lon) {
        var bookConfirmationRequest = Vehicle.BookConfirmationRequest.newBuilder()
                .setUserId(userId)
                .setVehicleId(vehicleId)
                .setLocation(Common.Location.newBuilder()
                        .setLatitude(lat)
                        .setLongitude(lon)
                        .build())
                .build();
        return bookingMapper.toAckResponseDto(vehicleServiceStub.bookConfirmation(bookConfirmationRequest));
    }
}
