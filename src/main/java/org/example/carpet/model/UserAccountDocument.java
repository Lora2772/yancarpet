package org.example.carpet.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccountDocument {

    @Id
    private String id;

    private String email;        // unique per user
    private String userName;

    // For demo we just store "{plain}password". In production you'd store BCrypt.
    private String passwordHash;

    private Address shippingAddress;
    private Address billingAddress;

    private PaymentMethodInfo defaultPaymentMethod;
}
