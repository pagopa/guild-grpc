package it.pagopa.guild.grpc.booking.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ResourceNotFoundException extends Exception {
    private final String error;

    public ResourceNotFoundException(String error) {
        super(error);
        this.error = error;
    }
}
