package it.pagopa.guild.grpc.booking.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BookRequestDto {
    @NotBlank(message = "vehicleId is required")
    private String vehicleId;
    @Valid
    @NotNull(message = "Location is required.")
    private LocationDto location;
    @NotBlank(message = "userId is mandatory")
    private String userId;
}
