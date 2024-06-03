package it.pagopa.guild.grpc.booking.service.impl;

import it.pagopa.guild.grpc.booking.dto.LocationDto;
import it.pagopa.guild.grpc.booking.entity.Location;
import it.pagopa.guild.grpc.booking.entity.Vehicle;
import it.pagopa.guild.grpc.booking.entity.VehicleStatus;
import it.pagopa.guild.grpc.booking.exception.ResourceNotAvailableException;
import it.pagopa.guild.grpc.booking.exception.ResourceNotFoundException;
import it.pagopa.guild.grpc.booking.mapper.BookingMapper;
import it.pagopa.guild.grpc.booking.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataManagerServiceImplTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private DataManagerServiceImpl dataManagerService;

    @Test
    public void testCheckVehicleAvailabilityAndGet() throws ResourceNotAvailableException, ResourceNotFoundException {
        String vehicleId = "123";
        Vehicle vehicle = new Vehicle();
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));

        Vehicle result = dataManagerService.checkVehicleAvailabilityAndGet(vehicleId);
        assertEquals(vehicle, result);
    }

    @Test
    public void testCheckVehicleAvailabilityAndGet_WhenVehicleNotFound() {
        String vehicleId = "123";
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            dataManagerService.checkVehicleAvailabilityAndGet(vehicleId);
        });
        assertEquals("Vehicle " + vehicleId + " doesn't exists", exception.getMessage());
    }

    @Test
    public void testCheckVehicleAvailabilityAndGet_WhenVehicleAlreadyBooked() {
        String vehicleId = "123";
        Vehicle vehicle = new Vehicle();
        vehicle.setStatus(VehicleStatus.BOOKED);
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));

        ResourceNotAvailableException exception = assertThrows(ResourceNotAvailableException.class, () -> {
            dataManagerService.checkVehicleAvailabilityAndGet(vehicleId);
        });
        assertEquals("Vehicle " + vehicle + " is already booked", exception.getMessage());
    }

    @Test
    public void testUpdateVehicleBookingAndStatus() {
        String userId = "user123";
        LocationDto locationDto = LocationDto.builder().latitude(10.0).longitude(5.0).build();
        Vehicle vehicle = new Vehicle();
        when(bookingMapper.toLocationEntity(locationDto)).thenReturn(new Location(10.0, 5.0));

        dataManagerService.updateVehicleBookingAndStatus(vehicle, userId, locationDto);

        assertEquals(VehicleStatus.BOOKED, vehicle.getStatus());
        assertEquals(1, vehicle.getBookings().size());
        verify(vehicleRepository, times(1)).save(vehicle);
    }

}