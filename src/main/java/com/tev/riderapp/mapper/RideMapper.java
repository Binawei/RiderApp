package com.tev.riderapp.mapper;

import com.tev.riderapp.dto.RideRequestDto;
import com.tev.riderapp.dto.RideResponseDto;
import com.tev.riderapp.model.Ride;
import org.springframework.stereotype.Component;

@Component
public class RideMapper {
    
    public RideResponseDto toDto(Ride ride) {
        RideResponseDto dto = new RideResponseDto();
        dto.setId(ride.getId());
        dto.setPassengerName(ride.getPassenger() != null ? ride.getPassenger().getFirstName() : null);
        dto.setDriverName(ride.getDriver() != null ? ride.getDriver().getFirstName() : null);
        dto.setPickupLocation(ride.getPickupLocation());
        dto.setDropoffLocation(ride.getDropoffLocation());
        dto.setRequestTime(ride.getRequestTime());
        dto.setStatus(ride.getStatus());
        dto.setRideType(ride.getRideType());
        dto.setFare(ride.getFare());
        dto.setDistance(ride.getDistance());
        dto.setSurgeMultiplier(ride.getSurgeMultiplier());
        return dto;
    }
}