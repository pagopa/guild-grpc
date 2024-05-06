package it.pagopa.guild.grpc.booking.client;

import it.pagopa.guild.grpc.booking.VehicleServiceGrpc;
import it.pagopa.guild.grpc.booking.dto.AckResponseDto;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class VehicleClient {
    @GrpcClient("vehicle")
    private VehicleServiceGrpc.VehicleServiceBlockingStub vehicleServiceStub;

    public AckResponseDto sendBookConfirmation(String userId, Double lat, Double lon) {
        // TODO: to implement send book confirmation request to vehicle
        return AckResponseDto.builder().build();
    }
}
