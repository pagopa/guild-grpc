package it.pagopa.guild.grpc.booking.client;

import io.grpc.StatusRuntimeException;
import it.pagopa.guild.grpc.booking.Common;
import it.pagopa.guild.grpc.booking.VehicleServiceGrpc;
import it.pagopa.guild.grpc.booking.client.impl.VehicleClientGrpc;
import it.pagopa.guild.grpc.booking.dto.AckResponseDto;
import it.pagopa.guild.grpc.booking.exception.BookingConfirmationException;
import it.pagopa.guild.grpc.booking.mapper.BookingMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleClientGrpcTest {
    @Mock
    private VehicleServiceGrpc.VehicleServiceBlockingStub vehicleServiceStub;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private VehicleClientGrpc vehicleClientGrpc;

    @Test
    public void testSendBookConfirmation_Success() throws BookingConfirmationException {
        String userId = "user123";
        String vehicleId = "vehicleXYZ";
        Double lat = 10.0;
        Double lon = 20.0;
        Common.AckResponse ackResponse = Common.AckResponse.newBuilder().setSuccess(true).build();
        when(vehicleServiceStub.bookConfirmation(any())).thenReturn(ackResponse);
        when(bookingMapper.toAckResponseDto(ackResponse)).thenReturn(AckResponseDto.builder().success(true).build());

        AckResponseDto response = vehicleClientGrpc.sendBookConfirmation(userId, vehicleId, lat, lon);

        assertEquals(Boolean.TRUE, response.getSuccess());
        verify(vehicleServiceStub).bookConfirmation(any());
    }

    @Test
    public void testSendBookConfirmation_Failure() {
        String userId = "user123";
        String vehicleId = "vehicleXYZ";
        Double lat = 10.0;
        Double lon = 20.0;
        when(vehicleServiceStub.bookConfirmation(any())).thenThrow(StatusRuntimeException.class);

        assertThrows(BookingConfirmationException.class, () -> vehicleClientGrpc.sendBookConfirmation(userId, vehicleId, lat, lon));
    }
}