package it.pagopa.guild.grpc.booking.service;

import it.pagopa.guild.grpc.booking.dto.LocationDto;
import it.pagopa.guild.grpc.booking.entity.Vehicle;
import it.pagopa.guild.grpc.booking.exception.ResourceNotAvailableException;
import it.pagopa.guild.grpc.booking.exception.ResourceNotFoundException;

public interface DataManagerService {
    Vehicle checkVehicleAvailabilityAndGet(String vehicleId) throws ResourceNotFoundException, ResourceNotAvailableException;
    void updateVehicleBookingAndStatus(Vehicle vehicle, String userId, LocationDto locationDto);
}
