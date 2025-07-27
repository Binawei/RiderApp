package com.tev.riderapp.controller;

import com.tev.riderapp.model.Passenger;
import com.tev.riderapp.service.PassengerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/passengers")
@RequiredArgsConstructor
public class PassengerController {
    private final PassengerService passengerService;

    @GetMapping("/{passengerId}/wallet")
    public ResponseEntity<Double> getWalletBalance(@PathVariable Long passengerId) {
        return ResponseEntity.ok(passengerService.getWalletBalance(passengerId));
    }

    @PutMapping("/{passengerId}/wallet/add")
    public ResponseEntity<Void> addToWallet(@PathVariable Long passengerId, @RequestParam double amount) {
        passengerService.addToWallet(passengerId, amount);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{passengerId}/wallet/deduct")
    public ResponseEntity<Void> deductFromWallet(@PathVariable Long passengerId, @RequestParam double amount) {
        passengerService.deductFromWallet(passengerId, amount);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{passengerId}")
    public ResponseEntity<Passenger> getPassenger(@PathVariable Long passengerId) {
        return ResponseEntity.ok(passengerService.getPassenger(passengerId));
    }
}