package com.tev.riderapp.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import com.stripe.Stripe;


@Configuration
public class StripeConfig {
    
    @Value("${stripe.secret.key:sk_test_your_stripe_secret_key_here}")
    private String secretKey;
    
    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }
}