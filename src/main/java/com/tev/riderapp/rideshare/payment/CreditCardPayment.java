package com.tev.riderapp.rideshare.payment;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;

public class CreditCardPayment implements PaymentStrategy {
    private String cardNumber;
    private String cvv;
    private String expiryDate;
    private String paymentIntentId;
    
    static {
        Stripe.apiKey = "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";
    }

    public CreditCardPayment(String cardNumber, String cvv, String expiryDate) {
        this.cardNumber = cardNumber;
        this.cvv = cvv;
        this.expiryDate = expiryDate;
    }

    @Override
    public boolean processPayment(double amount) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount((long) (amount * 100))
                .setCurrency("usd")
                .setPaymentMethod(createPaymentMethod())
                .setConfirm(true)
                .setReturnUrl("https://rideshare.com/return")
                .build();
            
            PaymentIntent intent = PaymentIntent.create(params);
            this.paymentIntentId = intent.getId();
            return "succeeded".equals(intent.getStatus());
        } catch (StripeException e) {
            System.err.println("Payment failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean refundPayment(double amount) {
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .setAmount((long) (amount * 100))
                .build();
            
            Refund refund = Refund.create(params);
            return "succeeded".equals(refund.getStatus());
        } catch (StripeException e) {
            System.err.println("Refund failed: " + e.getMessage());
            return false;
        }
    }
    
    private String createPaymentMethod() throws StripeException {
        return "pm_card_visa";
    }
}