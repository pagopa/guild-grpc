package it.pagopa.guild.grpc.booking.service.impl;

import it.pagopa.guild.grpc.booking.dto.LocationDto;
import it.pagopa.guild.grpc.booking.entity.Vehicle;
import it.pagopa.guild.grpc.booking.entity.VehicleStatus;
import it.pagopa.guild.grpc.booking.exception.ResourceNotAvailableException;
import it.pagopa.guild.grpc.booking.exception.ResourceNotFoundException;
import it.pagopa.guild.grpc.booking.mapper.BookingMapper;
import it.pagopa.guild.grpc.booking.repository.VehicleRepository;
import it.pagopa.guild.grpc.booking.service.DataManagerService;
import org.springframework.stereotype.Service;

@Service
public class DataManagerServiceImpl implements DataManagerService {
    private final VehicleRepository vehicleRepository;
    private final BookingMapper bookingMapper;

    public DataManagerServiceImpl(VehicleRepository vehicleRepository, BookingMapper bookingMapper) {
        this.vehicleRepository = vehicleRepository;
        this.bookingMapper = bookingMapper;
    }

    @Override
    public Vehicle checkVehicleAvailabilityAndGet(String vehicleId) throws ResourceNotFoundException, ResourceNotAvailableException {
        // Check if vehicle exists
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Vehicle %s doesn't exists", vehicleId)));

        // Check if vehicle is not booked yet
        if (vehicle.getStatus() == VehicleStatus.BOOKED) {
            throw new ResourceNotAvailableException(
                    String.format("Vehicle %s is already booked", vehicle));
        }

        return vehicle;
    }

    @Override
    public void updateVehicleBookingAndStatus(Vehicle vehicle, String userId, LocationDto locationDto) {
        vehicle.setStatus(VehicleStatus.BOOKED);
        vehicle.addBooking(it.pagopa.guild.grpc.booking.entity.Booking.builder()
                .userId(userId)
                .location(bookingMapper.toLocationEntity(locationDto))
                .build());
        vehicleRepository.save(vehicle);
    }
}
