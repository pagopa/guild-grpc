package it.pagopa.guild.grpc.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LocationDto {
    @Schema(description = "Latitude", example = "36.07")
    @NotNull(message = "latitude is required")
    private Double latitude;

    @Schema(description = "Longitude", example = "-95.90")
    @NotNull(message = "longitude is required")
    private Double longitude;
}
