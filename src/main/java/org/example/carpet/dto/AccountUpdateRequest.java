package org.example.carpet.dto;

import lombok.Data;
import org.example.carpet.model.Address;
import org.example.carpet.model.PaymentMethodInfo;

@Data
public class AccountUpdateRequest {
    private String userName;
    private Address shippingAddress;
    private Address billingAddress;
    private PaymentMethodInfo defaultPaymentMethod;
    // (不允许直接在这个接口里更新 email / password；可以单独做改密码接口)
}
