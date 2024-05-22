package it.pagopa.guild.grpc.booking.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {
    public String BOOKING_SUCCESS = "Vehicle booked successfully";
    public String BOOKING_GENERIC_FAILURE = "Vehicle booking failed, please try again";
    public String VEHICLE_NOT_FOUND = "Vehicle not found";
    public String VEHICLE_ALREADY_BOOKED = "Vehicle is already booked";
    public String VEHICLE_SEND_CONFIRMATION_FAILURE = "Send booking confirmation to vehicle service failed";
    public String GENERIC_ERROR = "Something failed, please try again";
}
