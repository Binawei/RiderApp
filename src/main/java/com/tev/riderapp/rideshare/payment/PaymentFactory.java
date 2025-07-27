package com.tev.riderapp.rideshare.payment;


import com.tev.riderapp.model.Passenger;

public class PaymentFactory {
    public static PaymentStrategy createPayment(String type, Object... args) {
        switch (type.toUpperCase()) {
            case "CREDIT_CARD":
                if (args.length == 3) {
                    return new CreditCardPayment((String) args[0], (String) args[1], (String) args[2]);
                }
                throw new IllegalArgumentException("Invalid arguments for credit card payment");
            
            case "WALLET":
                if (args.length == 1 && args[0] instanceof Passenger) {
                    return new WalletPayment((Passenger) args[0]);
                }
                throw new IllegalArgumentException("Invalid arguments for wallet payment");
            
            default:
                throw new IllegalArgumentException("Unknown payment type: " + type);
        }
    }
}