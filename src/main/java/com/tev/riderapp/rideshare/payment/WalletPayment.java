package com.tev.riderapp.rideshare.payment;


import com.tev.riderapp.model.Passenger;

public class WalletPayment implements PaymentStrategy {
    private Passenger passenger;

    public WalletPayment(Passenger passenger) {
        this.passenger = passenger;
    }

    @Override
    public boolean processPayment(double amount) {
        if (passenger.getWalletBalance() >= amount) {
            passenger.setWalletBalance(passenger.getWalletBalance() - amount);
            return true;
        }
        return false;
    }

    @Override
    public boolean refundPayment(double amount) {
        passenger.setWalletBalance(passenger.getWalletBalance() + amount);
        return true;
    }
}