package it.pagopa.guild.grpc.booking.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BookingConfirmationException extends Exception {
    private final String error;
    private final Exception exception;

    public BookingConfirmationException(String error, Exception e) {
        super(error, e);
        this.error = error;
        this.exception = e;
    }

}
