package com.tev.riderapp.service;

import com.tev.riderapp.model.Payment;
import com.tev.riderapp.model.PaymentStatus;
import com.tev.riderapp.model.PaymentType;
import com.tev.riderapp.model.Ride;
import com.tev.riderapp.rideshare.repository.PaymentRepository;
import com.tev.riderapp.rideshare.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final RideRepository rideRepository;

    public Payment processPayment(Long rideId, PaymentType paymentType, String paymentDetails) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        Payment payment = new Payment();
        payment.setRide(ride);
        payment.setAmount(ride.getFare());
        payment.setPaymentType(paymentType);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTimestamp(LocalDateTime.now());
        payment.setTransactionId(UUID.randomUUID().toString());

        // Simulate payment processing
        try {
            // Here you would integrate with actual payment gateway
            Thread.sleep(1000); // Simulate processing time
            payment.setStatus(PaymentStatus.COMPLETED);
        } catch (InterruptedException e) {
            payment.setStatus(PaymentStatus.FAILED);
        }

        return paymentRepository.save(payment);
    }

    public Payment refundPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            payment.setStatus(PaymentStatus.REFUNDED);
            return paymentRepository.save(payment);
        }

        throw new RuntimeException("Payment cannot be refunded");
    }

    public Payment getPaymentByRide(Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        return paymentRepository.findByRide(ride);
    }

    public List<Payment> getPassengerPayments(Long passengerId) {
        return paymentRepository.findByRidePassengerId(passengerId);
    }
}