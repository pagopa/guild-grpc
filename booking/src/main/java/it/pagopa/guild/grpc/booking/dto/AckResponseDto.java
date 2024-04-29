package it.pagopa.guild.grpc.booking.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AckResponseDto {
    Boolean success;
    String message;
}
