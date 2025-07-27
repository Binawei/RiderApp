package com.tev.riderapp.rideshare.payment;

public interface PaymentStrategy {
    boolean processPayment(double amount);
    boolean refundPayment(double amount);
}