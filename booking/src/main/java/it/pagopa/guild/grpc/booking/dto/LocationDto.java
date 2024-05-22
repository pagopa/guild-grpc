package it.pagopa.guild.grpc.booking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LocationDto {
    @NotBlank(message = "latitude is required")
    private Double latitude;
    @NotBlank(message = "longitude is required")
    private Double longitude;
}
