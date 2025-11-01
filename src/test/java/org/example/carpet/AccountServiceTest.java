package org.example.carpet.service;

import org.example.carpet.dto.AccountCreateRequest;
import org.example.carpet.dto.AccountUpdateRequest;
import org.example.carpet.dto.AccountResponse;
import org.example.carpet.model.Address;
import org.example.carpet.model.PaymentMethodInfo;
import org.example.carpet.model.UserAccountDocument;
import org.example.carpet.repository.mongo.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Covers Account create / update / lookup / passwordMatches.
 * This helps bump overall coverage across authentication & account domain.
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    UserAccountRepository userRepo;

    @InjectMocks
    AccountService accountService;

    @Test
    void createAccount_shouldPersistUserAndReturnResponse() {
        AccountCreateRequest req = new AccountCreateRequest();
        req.setEmail("buyer@yancarpet.com");
        req.setUserName("Buyer A");
        req.setPassword("pw123");

        Address ship = Address.builder()
                .line1("123 Ocean Rd")
                .city("Shanghai")
                .stateOrProvince("Shanghai")
                .postalCode("200000")
                .country("CN")
                .build();
        req.setShippingAddress(ship);

        PaymentMethodInfo pm = PaymentMethodInfo.builder()
                .type("CARD")
                .maskedDetail("VISA **** 1234")
                .build();
        req.setDefaultPaymentMethod(pm);

        when(userRepo.findByEmailIgnoreCase("buyer@yancarpet.com"))
                .thenReturn(Optional.empty());

        when(userRepo.save(any(UserAccountDocument.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AccountResponse resp = accountService.createAccount(req);

        assertEquals("buyer@yancarpet.com", resp.getEmail());
        assertEquals("Buyer A", resp.getUserName());
        assertEquals("CARD", resp.getDefaultPaymentMethod().getType());
    }

    @Test
    void updateAccount_shouldModifyProfileFields() {
        UserAccountDocument existing = UserAccountDocument.builder()
                .email("buyer@yancarpet.com")
                .userName("Old Name")
                .passwordHash("{plain}pw123")
                .shippingAddress(Address.builder().city("OldCity").build())
                .billingAddress(null)
                .defaultPaymentMethod(null)
                .build();

        when(userRepo.findByEmailIgnoreCase("buyer@yancarpet.com"))
                .thenReturn(Optional.of(existing));

        when(userRepo.save(any(UserAccountDocument.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AccountUpdateRequest req = new AccountUpdateRequest();
        req.setUserName("New Name");
        req.setShippingAddress(Address.builder().city("NewCity").build());
        req.setDefaultPaymentMethod(
                PaymentMethodInfo.builder()
                        .type("CARD")
                        .maskedDetail("VISA **** 9876")
                        .build()
        );

        AccountResponse out = accountService.updateAccount("buyer@yancarpet.com", req);

        assertEquals("buyer@yancarpet.com", out.getEmail());
        assertEquals("New Name", out.getUserName());
        assertEquals("NewCity", out.getShippingAddress().getCity());
        assertEquals("VISA **** 9876", out.getDefaultPaymentMethod().getMaskedDetail());
    }

    @Test
    void getMyAccount_shouldReturnProfile() {
        UserAccountDocument existing = UserAccountDocument.builder()
                .email("abc@test.com")
                .userName("Cool User")
                .passwordHash("{plain}111")
                .build();

        when(userRepo.findByEmailIgnoreCase("abc@test.com"))
                .thenReturn(Optional.of(existing));

        AccountResponse r = accountService.getMyAccount("abc@test.com");

        assertEquals("abc@test.com", r.getEmail());
        assertEquals("Cool User", r.getUserName());
    }

    @Test
    void passwordMatches_shouldCheckPlainWrapped() {
        UserAccountDocument user = UserAccountDocument.builder()
                .email("abc@test.com")
                .passwordHash("{plain}secret")
                .build();

        assertTrue(accountService.passwordMatches(user, "secret"));
        assertFalse(accountService.passwordMatches(user, "wrong"));
    }
}
