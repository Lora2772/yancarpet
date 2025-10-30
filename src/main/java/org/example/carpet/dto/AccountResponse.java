package org.example.carpet.dto;

import lombok.Builder;
import lombok.Data;
import org.example.carpet.model.Address;
import org.example.carpet.model.PaymentMethodInfo;

@Data
@Builder
public class AccountResponse {
    private String email;
    private String userName;
    private Address shippingAddress;
    private Address billingAddress;
    private PaymentMethodInfo defaultPaymentMethod;
}
