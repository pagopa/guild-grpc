package it.pagopa.guild.grpc.booking.service;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import it.pagopa.guild.grpc.booking.Booking;
import it.pagopa.guild.grpc.booking.Common;
import it.pagopa.guild.grpc.booking.client.VehicleClient;
import it.pagopa.guild.grpc.booking.dto.AckResponseDto;
import it.pagopa.guild.grpc.booking.entity.Vehicle;
import it.pagopa.guild.grpc.booking.exception.BookingConfirmationException;
import it.pagopa.guild.grpc.booking.exception.ResourceNotAvailableException;
import it.pagopa.guild.grpc.booking.exception.ResourceNotFoundException;
import it.pagopa.guild.grpc.booking.mapper.BookingMapper;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Qualifier;

import it.pagopa.guild.grpc.booking.utils.Constants;

@GrpcService
@Slf4j
public class BookingGrpc extends it.pagopa.guild.grpc.booking.BookingServiceGrpc.BookingServiceImplBase {

    private final DataManagerService dataManagerService;
    private final VehicleClient vehicleClientGrpc;
    private final BookingMapper bookingMapper;

    public BookingGrpc(DataManagerService dataManagerService, @Qualifier("vehicleClientGrpc") VehicleClient vehicleClientGrpc,
                       BookingMapper bookingMapper) {
        this.dataManagerService = dataManagerService;
        this.vehicleClientGrpc = vehicleClientGrpc;
        this.bookingMapper = bookingMapper;
    }

    @Override
    // TODO: transaction with mongo and vehicle service
    public void book(Booking.BookingRequest request, StreamObserver<Common.AckResponse> responseObserver) {
        try {
            // Check if vehicle exists and if it is not booked yet, and get it
            Vehicle vehicle = dataManagerService.checkVehicleAvailabilityAndGet(request.getVehicleId());

            // send book confirmation to vehicle service
            AckResponseDto ackResponseVehicle = vehicleClientGrpc.sendBookConfirmation(
                    request.getUserId(),
                    request.getVehicleId(),
                    request.getLocation().getLatitude(),
                    request.getLocation().getLongitude());

            // Check grpc api response, add booking on db and send related response to the caller
            if (ackResponseVehicle.getSuccess()) {
                dataManagerService.updateVehicleBookingAndStatus(vehicle, request.getUserId(),
                        bookingMapper.toLocationDto(request.getLocation()));
                var ackResponse = Common.AckResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage(Constants.BOOKING_SUCCESS)
                        .build();
                responseObserver.onNext(ackResponse);
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(Status.INTERNAL
                        .withDescription(Constants.BOOKING_GENERIC_FAILURE)
                        .asRuntimeException());
            }
        } catch (ResourceNotFoundException e) {
            log.error("Vehicle booking failed", e);
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(Constants.VEHICLE_NOT_FOUND)
                    .asRuntimeException());
        } catch (ResourceNotAvailableException e) {
            log.error("Vehicle booking failed", e);
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription(Constants.VEHICLE_ALREADY_BOOKED)
                    .asRuntimeException());
        } catch (BookingConfirmationException e) {
            log.error("vehicle sendBookConfirmation gRPC failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getError())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Vehicle booking failed", e);
            responseObserver.onError(Status.UNKNOWN
                    .withDescription(Constants.GENERIC_ERROR)
                    .asRuntimeException());
        }

    }
}
