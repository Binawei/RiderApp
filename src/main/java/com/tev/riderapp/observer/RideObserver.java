package com.tev.riderapp.observer;


import com.tev.riderapp.model.Ride;

public interface RideObserver {
    void update(Ride ride);
}