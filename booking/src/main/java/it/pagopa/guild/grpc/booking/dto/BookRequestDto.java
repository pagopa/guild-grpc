package it.pagopa.guild.grpc.booking.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BookRequestDto {
    @Schema(description = "ID of the vehicle", example = "b5b23d1b-ad4a-4a74")
    @NotBlank(message = "vehicleId is required")
    private String vehicleId;

    @Valid
    @Schema(description = "User location object containing latitude and longitude")
    @NotNull(message = "Location is required.")
    private LocationDto location;

    @Schema(description = "ID of the user", example = "user-99a2")
    @NotBlank(message = "userId is mandatory")
    private String userId;
}
