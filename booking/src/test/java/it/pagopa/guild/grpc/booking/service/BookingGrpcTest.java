package it.pagopa.guild.grpc.booking.service;

import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import it.pagopa.guild.grpc.booking.Booking;
import it.pagopa.guild.grpc.booking.Common;
import it.pagopa.guild.grpc.booking.client.VehicleClient;
import it.pagopa.guild.grpc.booking.dto.AckResponseDto;
import it.pagopa.guild.grpc.booking.dto.LocationDto;
import it.pagopa.guild.grpc.booking.entity.Vehicle;
import it.pagopa.guild.grpc.booking.entity.VehicleStatus;
import it.pagopa.guild.grpc.booking.entity.VehicleType;
import it.pagopa.guild.grpc.booking.exception.BookingConfirmationException;
import it.pagopa.guild.grpc.booking.exception.ResourceNotAvailableException;
import it.pagopa.guild.grpc.booking.exception.ResourceNotFoundException;
import it.pagopa.guild.grpc.booking.mapper.BookingMapper;
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
class BookingGrpcTest {

    @Mock
    private DataManagerService dataManagerService;

    @Mock
    @Qualifier("vehicleClientGrpc")
    private VehicleClient vehicleClientGrpc;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingGrpc bookingGrpcService;

    /*@Before
    public void setUp() {
        bookingGrpcService = new BookingGrpc(vehicleRepository, vehicleClient, bookingMapper);
    }*/

    @Test
    public void test_Success() throws ResourceNotAvailableException, ResourceNotFoundException, BookingConfirmationException {
        String userId = "user123";
        String vehicleId = "vehicleXYZ";
        Booking.BookingRequest request = getBookingRequest(userId, vehicleId);
        Vehicle vehicle = getVehicleEntity(vehicleId, VehicleStatus.AVAILABLE);

        when(dataManagerService.checkVehicleAvailabilityAndGet(vehicleId)).thenReturn(vehicle);
        LocationDto locationDto = LocationDto.builder()
                .latitude(request.getLocation().getLatitude())
                .longitude(request.getLocation().getLongitude())
                .build();
        when(bookingMapper.toLocationDto(request.getLocation())).thenReturn(locationDto);

        AckResponseDto ackResponseVehicle = AckResponseDto.builder().success(true).message("OK").build();
        when(vehicleClientGrpc.sendBookConfirmation(
                userId, request.getVehicleId(), request.getLocation().getLatitude(), request.getLocation().getLongitude()))
                .thenReturn(ackResponseVehicle);

        StreamObserver<Common.AckResponse> responseObserver = mock(StreamObserver.class);
        bookingGrpcService.book(request, responseObserver);

        verify(dataManagerService).updateVehicleBookingAndStatus(vehicle, userId, locationDto);
        Common.AckResponse expectedResponse = Common.AckResponse.newBuilder()
                .setSuccess(true)
                .setMessage(Constants.BOOKING_SUCCESS)
                .build();
        verify(responseObserver).onNext(expectedResponse);
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());
    }

    @ParameterizedTest
    @MethodSource("provideResourceExceptionArguments")
    public void test_ResourceException(Class<? extends Exception> exceptionClass) throws ResourceNotAvailableException, ResourceNotFoundException {
        String userId = "user123";
        String vehicleId = "vehicleXYZ";
        Booking.BookingRequest request = getBookingRequest(userId, vehicleId);

        when(dataManagerService.checkVehicleAvailabilityAndGet(vehicleId)).thenThrow(exceptionClass);

        StreamObserver<Common.AckResponse> responseObserver = mock(StreamObserver.class);
        bookingGrpcService.book(request, responseObserver);

        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
        verify(responseObserver).onError(any(StatusRuntimeException.class));
    }

    private static Stream<Arguments> provideResourceExceptionArguments() {
        return Stream.of(
                Arguments.of( ResourceNotFoundException.class),
                Arguments.of(ResourceNotAvailableException.class),
                Arguments.of(RuntimeException.class)
        );
    }

    @Test
    public void test_VehicleClientError() throws BookingConfirmationException {
        String userId = "user123";
        String vehicleId = "vehicleXYZ";
        Booking.BookingRequest request = getBookingRequest(userId, vehicleId);

        when(vehicleClientGrpc.sendBookConfirmation(userId, vehicleId, request.getLocation().getLatitude(),
                request.getLocation().getLongitude())).thenThrow(BookingConfirmationException.class);

        StreamObserver<Common.AckResponse> responseObserver = mock(StreamObserver.class);
        bookingGrpcService.book(request, responseObserver);

        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
        verify(responseObserver).onError(any(StatusRuntimeException.class));
    }

    private static Vehicle getVehicleEntity(String vehicleId, VehicleStatus status) {
        return new Vehicle(vehicleId, VehicleType.CAR, status, new ArrayList<>());
    }

    private static Booking.BookingRequest getBookingRequest(String userId, String vehicleId) {
        return Booking.BookingRequest.newBuilder()
                .setUserId(userId)
                .setVehicleId(vehicleId)
                .setLocation(Common.Location.newBuilder().setLatitude(42.361111).setLongitude(13.377222).build())
                .build();
    }


}