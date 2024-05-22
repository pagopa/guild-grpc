package it.pagopa.guild.grpc.booking.mapper;

import it.pagopa.guild.grpc.booking.Common;
import it.pagopa.guild.grpc.booking.dto.AckResponseDto;
import it.pagopa.guild.grpc.booking.dto.LocationDto;
import it.pagopa.guild.grpc.booking.entity.Location;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface BookingMapper {
    LocationDto toLocationDto(Common.Location locationGrpc);
    Location toLocationEntity(LocationDto locationDto);
    AckResponseDto toAckResponseDto(Common.AckResponse ackResponseGrpc);
}
