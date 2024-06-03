package it.pagopa.guild.grpc.booking.client;

import it.pagopa.guild.grpc.booking.dto.AckResponseDto;
import it.pagopa.guild.grpc.booking.dto.BookRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "vehicleService", url = "${rest.service.vehicle.url}")
public interface VehicleClientFeign {
    @PostMapping("/booking")
    AckResponseDto sendBookConfirmation(@RequestBody BookRequestDto bookRequest);
}
