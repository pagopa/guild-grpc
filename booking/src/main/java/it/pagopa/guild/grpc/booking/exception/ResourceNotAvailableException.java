package it.pagopa.guild.grpc.booking.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ResourceNotAvailableException extends Exception {
    private final String error;

    public ResourceNotAvailableException(String error) {
        super(error);
        this.error = error;
    }
}
