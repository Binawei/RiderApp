package com.tev.riderapp.dto;

import com.tev.riderapp.model.PaymentType;
import lombok.Data;

@Data
public class PaymentRequestDto {
    private Long rideId;
    private PaymentType paymentType;
    private String paymentDetails;
}