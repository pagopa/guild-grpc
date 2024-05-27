package it.pagopa.guild.grpc.booking.client;

import it.pagopa.guild.grpc.booking.client.impl.VehicleClientRest;
import it.pagopa.guild.grpc.booking.dto.AckResponseDto;
import it.pagopa.guild.grpc.booking.exception.BookingConfirmationException;
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
class VehicleClientRestTest {
    @Mock
    private VehicleClientFeign vehicleClientFeign;

    @InjectMocks
    private VehicleClientRest vehicleClientRest;

    @Test
    public void testSendBookConfirmation_Success() throws BookingConfirmationException {
        String userId = "user123";
        String vehicleId = "vehicleXYZ";
        Double lat = 10.0;
        Double lon = 20.0;
        AckResponseDto expectedResponse = AckResponseDto.builder().success(true).build();
        when(vehicleClientFeign.sendBookConfirmation(any())).thenReturn(expectedResponse);

        // Execution
        AckResponseDto response = vehicleClientRest.sendBookConfirmation(userId, vehicleId, lat, lon);

        assertEquals(expectedResponse, response);
        verify(vehicleClientFeign).sendBookConfirmation(any());
    }

    @Test
    public void testSendBookConfirmation_Failure() {
        String userId = "user123";
        String vehicleId = "vehicleXYZ";
        Double lat = 10.0;
        Double lon = 20.0;
        when(vehicleClientFeign.sendBookConfirmation(any())).thenThrow(RuntimeException.class);

        assertThrows(BookingConfirmationException.class, () -> vehicleClientRest.sendBookConfirmation(userId, vehicleId, lat, lon));
    }
}