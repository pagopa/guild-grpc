package it.pagopa.guild.grpc.booking.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Location {
    private Double latitude;
    private Double longitude;
}
