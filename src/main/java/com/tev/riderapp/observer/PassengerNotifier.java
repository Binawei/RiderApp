package com.tev.riderapp.observer;


import com.tev.riderapp.model.Ride;

public class PassengerNotifier implements RideObserver {
    @Override
    public void update(Ride ride) {
        switch (ride.getStatus()) {
            case ACCEPTED:
                notifyPassengerRideAccepted(ride);
                break;
            case PICKED_UP:
                notifyPassengerRideStarted(ride);
                break;
            case COMPLETED:
                notifyPassengerRideCompleted(ride);
                break;
        }
    }

    private void notifyPassengerRideAccepted(Ride ride) {
        // Implementation would send actual notification to passenger
        System.out.println("Notification to passenger: Your ride has been accepted by " + ride.getDriver().getFirstName());
    }

    private void notifyPassengerRideStarted(Ride ride) {
        System.out.println("Notification to passenger: Your ride has started");
    }

    private void notifyPassengerRideCompleted(Ride ride) {
        System.out.println("Notification to passenger: Your ride has completed. Fare: £" + String.format("%.2f", ride.getFare()));
    }
}