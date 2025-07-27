package com.tev.riderapp.observer;


import com.tev.riderapp.model.Ride;

public class DriverNotifier implements RideObserver {
    @Override
    public void update(Ride ride) {
        switch (ride.getStatus()) {
            case REQUESTED:
                notifyDriverNewRide(ride);
                break;
            case COMPLETED:
                notifyDriverRideCompleted(ride);
                break;
        }
    }

    private void notifyDriverNewRide(Ride ride) {
        // Implementation would send actual notification to nearby drivers
        String pickupAddress = ride.getPickupLocation() != null ? ride.getPickupLocation().getAddress() : "Unknown location";
        System.out.println("Notification to nearby drivers: New ride request from " + pickupAddress);
    }

    private void notifyDriverRideCompleted(Ride ride) {
        System.out.println("Notification to driver: Ride completed. Earnings: £" + String.format("%.2f", ride.getFare()));
    }
}