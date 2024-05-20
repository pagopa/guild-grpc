package it.pagopa.guild.grpc.booking.service;

import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import it.pagopa.guild.grpc.booking.Booking;
import it.pagopa.guild.grpc.booking.Common;
import it.pagopa.guild.grpc.booking.client.VehicleClient;
import it.pagopa.guild.grpc.booking.dto.AckResponseDto;
import it.pagopa.guild.grpc.booking.entity.Location;
import it.pagopa.guild.grpc.booking.entity.Vehicle;
import it.pagopa.guild.grpc.booking.entity.VehicleStatus;
import it.pagopa.guild.grpc.booking.entity.VehicleType;
import it.pagopa.guild.grpc.booking.mapper.BookingMapper;
import it.pagopa.guild.grpc.booking.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingGrpcTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VehicleClient vehicleClient;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingGrpc bookingGrpcService;

    /*@Before
    public void setUp() {
        bookingGrpcService = new BookingGrpc(vehicleRepository, vehicleClient, bookingMapper);
    }*/

    @Test
    public void testBook_Success() {
        String userId = "user123";
        String vehicleId = "vehicleXYZ";
        Booking.BookingRequest request = getBookingRequest(userId, vehicleId);
        Vehicle vehicle = getVehicleEntity(vehicleId, VehicleStatus.AVAILABLE);

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(bookingMapper.toLocationEntity(request.getLocation()))
                .thenReturn(new Location(request.getLocation().getLatitude(), request.getLocation().getLongitude()));

        AckResponseDto ackResponseVehicle = AckResponseDto.builder().success(true).message("OK").build();
        when(vehicleClient.sendBookConfirmation(
                userId, request.getVehicleId(), request.getLocation().getLatitude(), request.getLocation().getLongitude()))
                .thenReturn(ackResponseVehicle);

        StreamObserver<Common.AckResponse> responseObserver = mock(StreamObserver.class);
        bookingGrpcService.book(request, responseObserver);

        verify(vehicleRepository).save(vehicle);
        assertEquals(VehicleStatus.BOOKED, vehicle.getStatus());
        assertEquals(1, vehicle.getBookings().size());

        Common.AckResponse expectedResponse = Common.AckResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Vehicle booked successfully")
                .build();
        verify(responseObserver).onNext(expectedResponse);
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());
    }

    @Test
    public void testBook_VehicleNotFound() {
        String userId = "user123";
        String vehicleId = "vehicleXYZ";
        Booking.BookingRequest request = getBookingRequest(userId, vehicleId);

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());

        StreamObserver<Common.AckResponse> responseObserver = mock(StreamObserver.class);
        bookingGrpcService.book(request, responseObserver);

        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
        verify(responseObserver).onError(any(StatusRuntimeException.class));
    }

    @Test
    public void testBook_VehicleAlreadyBooked() {
        String userId = "user123";
        String vehicleId = "vehicleXYZ";
        Booking.BookingRequest request = getBookingRequest(userId, vehicleId);
        Vehicle vehicle = getVehicleEntity(vehicleId, VehicleStatus.BOOKED);

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));

        StreamObserver<Common.AckResponse> responseObserver = mock(StreamObserver.class);
        bookingGrpcService.book(request, responseObserver);

        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
        verify(responseObserver).onError(any(StatusRuntimeException.class));
    }

    @Test
    public void testBook_UnexpectedException() {
        String userId = "user123";
        String vehicleId = "vehicleXYZ";
        Booking.BookingRequest request = getBookingRequest(userId, vehicleId);

        doThrow(new RuntimeException("Unexpected error"))
                .when(vehicleRepository).findById(vehicleId);

        StreamObserver<Common.AckResponse> responseObserver = mock(StreamObserver.class);
        bookingGrpcService.book(request, responseObserver);

        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
        verify(responseObserver).onError(any(StatusRuntimeException.class));
    }

    // TODO: client exception

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