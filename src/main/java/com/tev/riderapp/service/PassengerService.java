package com.tev.riderapp.service;

import com.tev.riderapp.model.Passenger;
import com.tev.riderapp.rideshare.repository.PassengerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PassengerService {
    private final PassengerRepository passengerRepository;

    public Double getWalletBalance(Long passengerId) {
        Passenger passenger = getPassenger(passengerId);
        return passenger.getWalletBalance();
    }

    public void addToWallet(Long passengerId, double amount) {
        Passenger passenger = getPassenger(passengerId);
        passenger.setWalletBalance(passenger.getWalletBalance() + amount);
        passengerRepository.save(passenger);
    }

    public void deductFromWallet(Long passengerId, double amount) {
        Passenger passenger = getPassenger(passengerId);
        if (passenger.getWalletBalance() < amount) {
            throw new RuntimeException("Insufficient wallet balance");
        }
        passenger.setWalletBalance(passenger.getWalletBalance() - amount);
        passengerRepository.save(passenger);
    }

    public Passenger getPassenger(Long passengerId) {
        return passengerRepository.findById(passengerId)
                .orElseThrow(() -> new RuntimeException("Passenger not found"));
    }
}