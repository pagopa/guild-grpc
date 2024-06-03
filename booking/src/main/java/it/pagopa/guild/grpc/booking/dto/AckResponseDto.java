package it.pagopa.guild.grpc.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AckResponseDto {
    @Schema(description = "Feedback about the success of the operation", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean success;

    @Schema(description = "Error message", example = "Vehicle booked successfully")
    private String message;
}
