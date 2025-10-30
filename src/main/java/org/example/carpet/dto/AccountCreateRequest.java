package org.example.carpet.dto;

import lombok.Data;
import org.example.carpet.model.Address;
import org.example.carpet.model.PaymentMethodInfo;

@Data
public class AccountCreateRequest {
    private String email;
    private String userName;
    private String password; // plain text in request
    private Address shippingAddress;
    private Address billingAddress;
    private PaymentMethodInfo defaultPaymentMethod;
}
