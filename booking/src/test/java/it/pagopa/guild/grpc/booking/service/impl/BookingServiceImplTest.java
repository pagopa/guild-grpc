package it.pagopa.guild.grpc.booking.service.impl;

import it.pagopa.guild.grpc.booking.client.VehicleClient;
import it.pagopa.guild.grpc.booking.dto.AckResponseDto;
import it.pagopa.guild.grpc.booking.dto.LocationDto;
import it.pagopa.guild.grpc.booking.entity.Vehicle;
import it.pagopa.guild.grpc.booking.entity.VehicleStatus;
import it.pagopa.guild.grpc.booking.entity.VehicleType;
import it.pagopa.guild.grpc.booking.exception.BookingConfirmationException;
import it.pagopa.guild.grpc.booking.exception.ResourceNotAvailableException;
import it.pagopa.guild.grpc.booking.exception.ResourceNotFoundException;
import it.pagopa.guild.grpc.booking.service.DataManagerService;
import it.pagopa.guild.grpc.booking.utils.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.ArrayList;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {
    @Mock
    private DataManagerService dataManagerService;

    @Mock
    @Qualifier("vehicleClientRest")
    private VehicleClient vehicleClientRest;

    @InjectMocks
    private BookingServiceImpl bookingServiceImpl;

    @Test
    public void testBookVehicle_Success() throws ResourceNotAvailableException, ResourceNotFoundException, BookingConfirmationException {
        String vehicleId = "vehicle123";
        String userId = "user123";
        LocationDto locationDto = getLocationDto();
        Vehicle vehicle = getVehicleEntity(vehicleId, VehicleStatus.AVAILABLE);
        when(dataManagerService.checkVehicleAvailabilityAndGet(vehicleId)).thenReturn(vehicle);
        when(vehicleClientRest.sendBookConfirmation(userId, vehicleId, locationDto.getLatitude(), locationDto.getLongitude()))
                .thenReturn(AckResponseDto.builder().success(true).message("OK").build());

        AckResponseDto response = bookingServiceImpl.bookVehicle(vehicleId, userId, locationDto);

        assertEquals(Boolean.TRUE, response.getSuccess());
        assertEquals(Constants.BOOKING_SUCCESS, response.getMessage());
        verify(dataManagerService).updateVehicleBookingAndStatus(vehicle, userId, locationDto);
    }

    @ParameterizedTest
    @MethodSource("provideResourceExceptionArguments")
    public void test_ResourceException(Class<? extends Exception> exceptionClass) throws ResourceNotAvailableException, ResourceNotFoundException, BookingConfirmationException {
        String userId = "user123";
        String vehicleId = "vehicleXYZ";
        LocationDto locationDto = getLocationDto();
        when(dataManagerService.checkVehicleAvailabilityAndGet(vehicleId)).thenThrow(exceptionClass);
        assertThrows(exceptionClass, () -> bookingServiceImpl.bookVehicle(vehicleId, userId, locationDto));
        verify(dataManagerService, never()).updateVehicleBookingAndStatus(any(), eq(userId), eq(locationDto));
    }

    private static Stream<Arguments> provideResourceExceptionArguments() {
        return Stream.of(
                Arguments.of( ResourceNotFoundException.class),
                Arguments.of(ResourceNotAvailableException.class),
                Arguments.of(RuntimeException.class)
        );
    }

    @Test
    public void testBookVehicle_Failure() throws ResourceNotAvailableException, ResourceNotFoundException, BookingConfirmationException {
        String vehicleId = "vehicle123";
        String userId = "user123";
        LocationDto locationDto = getLocationDto();
        Vehicle vehicle = getVehicleEntity(vehicleId, VehicleStatus.AVAILABLE);
        when(dataManagerService.checkVehicleAvailabilityAndGet(vehicleId)).thenReturn(vehicle);
        when(vehicleClientRest.sendBookConfirmation(userId, vehicleId, locationDto.getLatitude(), locationDto.getLongitude()))
                .thenReturn(AckResponseDto.builder().success(false).message("Error").build());

        AckResponseDto response = bookingServiceImpl.bookVehicle(vehicleId, userId, locationDto);

        assertEquals(Boolean.FALSE, response.getSuccess());
        assertEquals(Constants.BOOKING_GENERIC_FAILURE, response.getMessage());
        verify(dataManagerService, never()).updateVehicleBookingAndStatus(vehicle, userId, locationDto);
    }

    @Test
    public void test_VehicleClientError() throws BookingConfirmationException {
        String userId = "user123";
        String vehicleId = "vehicleXYZ";

        when(vehicleClientRest.sendBookConfirmation(userId, vehicleId, 10.0, 5.0)).thenThrow(BookingConfirmationException.class);

        assertThrows(BookingConfirmationException.class, () -> bookingServiceImpl.bookVehicle(vehicleId, userId, getLocationDto()));
        verify(dataManagerService, never()).updateVehicleBookingAndStatus(any(), eq(userId), eq(getLocationDto()));
    }

    private static Vehicle getVehicleEntity(String vehicleId, VehicleStatus status) {
        return new Vehicle(vehicleId, VehicleType.CAR, status, new ArrayList<>());
    }

    private static LocationDto getLocationDto() {
        return LocationDto.builder()
                .latitude(10.0)
                .longitude(5.0)
                .build();
    }
}