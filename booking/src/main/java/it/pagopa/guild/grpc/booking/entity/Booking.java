package it.pagopa.guild.grpc.booking.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class Booking {
    @Builder.Default
    private LocalDateTime date = LocalDateTime.now();
    private String userId;
    private Location location;
}
