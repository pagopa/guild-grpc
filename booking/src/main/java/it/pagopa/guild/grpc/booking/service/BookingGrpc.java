package it.pagopa.guild.grpc.booking.service;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import it.pagopa.guild.grpc.booking.Booking;
import it.pagopa.guild.grpc.booking.Common;
import it.pagopa.guild.grpc.booking.client.VehicleClient;
import it.pagopa.guild.grpc.booking.dto.AckResponseDto;
import it.pagopa.guild.grpc.booking.entity.Vehicle;
import it.pagopa.guild.grpc.booking.entity.VehicleStatus;
import it.pagopa.guild.grpc.booking.exception.ResourceNotAvailableException;
import it.pagopa.guild.grpc.booking.exception.ResourceNotFoundException;
import it.pagopa.guild.grpc.booking.mapper.BookingMapper;
import it.pagopa.guild.grpc.booking.repository.VehicleRepository;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@Slf4j
public class BookingGrpc extends it.pagopa.guild.grpc.booking.BookingServiceGrpc.BookingServiceImplBase {

    private final VehicleRepository vehicleRepository;
    private final VehicleClient vehicleClient;
    private final BookingMapper bookingMapper;

    public BookingGrpc(VehicleRepository vehicleRepository, VehicleClient vehicleClient, BookingMapper bookingMapper) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleClient = vehicleClient;
        this.bookingMapper = bookingMapper;
    }

    @Override
    // TODO: transaction with mogno and vehicle service
    public void book(Booking.BookingRequest request, StreamObserver<Common.AckResponse> responseObserver) {
        try {
            // Check if vehicle exists
            Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format("Vehicle %s doesn't exists", request.getVehicleId())));

            // Check if vehicle is not booked yet
            if (vehicle.getStatus() == VehicleStatus.BOOKED) {
                throw  new ResourceNotAvailableException(
                        String.format("Vehicle %s is already booked", request.getVehicleId()));
            }

            /* // TODO: send book confirmation to vehicle service
            AckResponseDto ackResponseVehicle = vehicleClient.sendBookConfirmation(
                    request.getUserId(),
                    request.getLocation().getLatitude(),
                    request.getLocation().getLongitude()); */

            // AckResponse MOCK
            AckResponseDto ackResponseVehicle = AckResponseDto.builder()
                    .message("OK")
                    .success(true)
                    .build();

            // Check grpc api response, create booking on db and send related response to the caller
            if (ackResponseVehicle.getSuccess()) {
                vehicle.setStatus(VehicleStatus.BOOKED);
                vehicle.addBooking(it.pagopa.guild.grpc.booking.entity.Booking.builder()
                        .userId(request.getUserId())
                        .location(bookingMapper.toLocationEntity(request.getLocation()))
                        .build());
                vehicleRepository.save(vehicle);
                var ackResponse = Common.AckResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Vehicle booked successfully")
                        .build();
                responseObserver.onNext(ackResponse);
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(Status.INTERNAL
                        .withDescription("Vehicle booking failed, please try again")
                        .asRuntimeException());
            }
        } catch (ResourceNotFoundException e) {
            log.error("Vehicle booking failed", e);
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Vehicle not found")
                    .asRuntimeException());
        } catch (ResourceNotAvailableException e) {
            log.error("Vehicle booking failed", e);
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription("Vehicle is already booked")
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Vehicle booking failed", e);
            responseObserver.onError(Status.UNKNOWN
                    .withDescription("Vehicle booking failed, please try again")
                    .asRuntimeException());
        }

    }
}
