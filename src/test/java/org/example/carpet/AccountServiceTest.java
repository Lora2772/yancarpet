package org.example.carpet.service;

import org.example.carpet.dto.AccountCreateRequest;
import org.example.carpet.dto.AccountUpdateRequest;
import org.example.carpet.dto.AccountResponse;
import org.example.carpet.model.Address;
import org.example.carpet.model.PaymentMethodInfo;
import org.example.carpet.model.Account;
import org.example.carpet.repository.mongo.AccountRepository;
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
    AccountRepository userRepo;

    @Mock
    org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

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

        when(userRepo.findByEmail("buyer@yancarpet.com"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("pw123"))
                .thenReturn("$2a$10$encodedHash");

        when(userRepo.save(any(Account.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AccountResponse resp = accountService.createAccount(req);

        assertEquals("buyer@yancarpet.com", resp.getEmail());
        assertEquals("Buyer A", resp.getUserName());
        assertEquals("CARD", resp.getDefaultPaymentMethod().getType());
    }

    @Test
    void updateAccount_shouldModifyProfileFields() {
        Account existing = Account.builder()
                .email("buyer@yancarpet.com")
                .userName("Old Name")
                .passwordHash("{plain}pw123")
                .shippingAddress(Address.builder().city("OldCity").build())
                .billingAddress(null)
                .defaultPaymentMethod(null)
                .build();

        when(userRepo.findByEmail("buyer@yancarpet.com"))
                .thenReturn(Optional.of(existing));

        when(userRepo.save(any(Account.class)))
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
        Account existing = Account.builder()
                .email("abc@test.com")
                .userName("Cool User")
                .passwordHash("{plain}111")
                .build();

        when(userRepo.findByEmail("abc@test.com"))
                .thenReturn(Optional.of(existing));

        AccountResponse r = accountService.getMyAccount("abc@test.com");

        assertEquals("abc@test.com", r.getEmail());
        assertEquals("Cool User", r.getUserName());
    }

    @Test
    void passwordMatches_shouldCheckPlainWrapped() {
        Account user = Account.builder()
                .email("abc@test.com")
                .passwordHash("{plain}secret")
                .build();

        // Mock encoder.encode for plain password migration
        when(passwordEncoder.encode("secret")).thenReturn("$2a$10$newHash");
        when(userRepo.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        assertTrue(accountService.passwordMatches(user, "secret"));

        // Reset mocks for second call
        Account user2 = Account.builder()
                .email("abc@test.com")
                .passwordHash("{plain}secret")
                .build();

        assertFalse(accountService.passwordMatches(user2, "wrong"));
    }

    @Test
    void createAccount_duplicateEmail_shouldThrowException() {
        // Arrange
        AccountCreateRequest req = new AccountCreateRequest();
        req.setEmail("duplicate@yancarpet.com");
        req.setUserName("Test User");
        req.setPassword("password");

        Account existing = Account.builder()
                .email("duplicate@yancarpet.com")
                .userName("Existing User")
                .build();

        when(userRepo.findByEmail("duplicate@yancarpet.com"))
                .thenReturn(Optional.of(existing));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            accountService.createAccount(req);
        });

        verify(userRepo, never()).save(any(Account.class));
    }

    @Test
    void updateAccount_nonExistentUser_shouldThrowException() {
        // Arrange
        AccountUpdateRequest req = new AccountUpdateRequest();
        req.setUserName("New Name");

        when(userRepo.findByEmail("nonexistent@test.com"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            accountService.updateAccount("nonexistent@test.com", req);
        });
    }

    @Test
    void getMyAccount_nonExistentUser_shouldThrowException() {
        // Arrange
        when(userRepo.findByEmail("nonexistent@test.com"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            accountService.getMyAccount("nonexistent@test.com");
        });
    }

    @Test
    void passwordMatches_nullPassword_shouldReturnFalse() {
        // Arrange
        Account user = Account.builder()
                .email("test@test.com")
                .passwordHash("{plain}secret")
                .build();

        // Act & Assert
        assertFalse(accountService.passwordMatches(user, null));
    }

    @Test
    void passwordMatches_emptyPassword_shouldReturnFalse() {
        // Arrange
        Account user = Account.builder()
                .email("test@test.com")
                .passwordHash("{plain}secret")
                .build();

        // Act & Assert
        assertFalse(accountService.passwordMatches(user, ""));
    }
}
