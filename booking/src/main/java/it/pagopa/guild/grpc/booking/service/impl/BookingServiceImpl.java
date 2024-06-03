package it.pagopa.guild.grpc.booking.service.impl;

import it.pagopa.guild.grpc.booking.client.VehicleClient;
import it.pagopa.guild.grpc.booking.dto.AckResponseDto;
import it.pagopa.guild.grpc.booking.dto.LocationDto;
import it.pagopa.guild.grpc.booking.entity.Vehicle;
import it.pagopa.guild.grpc.booking.exception.BookingConfirmationException;
import it.pagopa.guild.grpc.booking.exception.ResourceNotAvailableException;
import it.pagopa.guild.grpc.booking.exception.ResourceNotFoundException;
import it.pagopa.guild.grpc.booking.service.BookingService;
import it.pagopa.guild.grpc.booking.service.DataManagerService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import it.pagopa.guild.grpc.booking.utils.Constants;

@Service
public class BookingServiceImpl implements BookingService {

    private final DataManagerService dataManagerService;
    private final VehicleClient vehicleClientRest;

    public BookingServiceImpl(DataManagerService dataManagerService,
                              @Qualifier("vehicleClientRest") VehicleClient vehicleClientRest) {
        this.dataManagerService = dataManagerService;
        this.vehicleClientRest = vehicleClientRest;
    }
    @Override
    public AckResponseDto bookVehicle(String vehicleId, String userId, LocationDto locationDto)
            throws ResourceNotAvailableException, ResourceNotFoundException, BookingConfirmationException {
        // Check if vehicle exists and if it is not booked yet, and get it
        Vehicle vehicle = dataManagerService.checkVehicleAvailabilityAndGet(vehicleId);

        // send book confirmation to vehicle service
        AckResponseDto ackResponseVehicle = vehicleClientRest.sendBookConfirmation(userId, vehicleId,
                locationDto.getLatitude(), locationDto.getLongitude());

        // Check grpc api response, add booking on db and send related response to the caller
        if (ackResponseVehicle.getSuccess()) {
            dataManagerService.updateVehicleBookingAndStatus(vehicle, userId, locationDto);
            return AckResponseDto.builder().success(true).message(Constants.BOOKING_SUCCESS).build();
        }

        return AckResponseDto.builder().success(false).message(Constants.BOOKING_GENERIC_FAILURE).build();
    }
}
