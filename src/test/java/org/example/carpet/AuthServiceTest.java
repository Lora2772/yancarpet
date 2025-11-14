package org.example.carpet;

import org.example.carpet.dto.LoginResp;
import org.example.carpet.model.Account;
import org.example.carpet.repository.mongo.AccountRepository;
import org.example.carpet.security.JwtUtil;
import org.example.carpet.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_withValidCredentials_shouldReturnToken() {
        // Arrange
        String email = "test@example.com";
        String password = "password123";
        String passwordHash = "$2a$10$hashvalue";
        String expectedToken = "jwt.token.here";

        Account account = Account.builder()
                .email(email)
                .passwordHash(passwordHash)
                .build();

        when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches(password, passwordHash)).thenReturn(true);
        when(jwtUtil.generateToken(email)).thenReturn(expectedToken);

        // Act
        LoginResp response = authService.login(email, password);

        // Assert
        assertNotNull(response);
        assertEquals(expectedToken, response.getToken());
        verify(accountRepository).findByEmail(email);
        verify(passwordEncoder).matches(password, passwordHash);
        verify(jwtUtil).generateToken(email);
    }

    @Test
    void login_withInvalidEmail_shouldThrowBadCredentialsException() {
        // Arrange
        String email = "nonexistent@example.com";
        String password = "password";

        when(accountRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> {
            authService.login(email, password);
        });

        assertTrue(exception.getMessage().contains("Incorrect Password"));
        verify(accountRepository).findByEmail(email);
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void login_withInvalidPassword_shouldThrowBadCredentialsException() {
        // Arrange
        String email = "test@example.com";
        String correctHash = "$2a$10$correcthash";
        String wrongPassword = "wrongPassword";

        Account account = Account.builder()
                .email(email)
                .passwordHash(correctHash)
                .build();

        when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches(wrongPassword, correctHash)).thenReturn(false);

        // Act & Assert
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> {
            authService.login(email, wrongPassword);
        });

        assertTrue(exception.getMessage().contains("Incorrect Password"));
        verify(accountRepository).findByEmail(email);
        verify(passwordEncoder).matches(wrongPassword, correctHash);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void login_withNullPassword_shouldThrowBadCredentialsException() {
        // Arrange
        String email = "test@example.com";

        Account account = Account.builder()
                .email(email)
                .passwordHash(null)
                .build();

        when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));

        // Act & Assert
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> {
            authService.login(email, "anypassword");
        });

        assertTrue(exception.getMessage().contains("Incorrect Password"));
        verify(accountRepository).findByEmail(email);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void login_withEmptyPasswordHash_shouldThrowBadCredentialsException() {
        // Arrange
        String email = "test@example.com";
        String password = "password";

        Account account = Account.builder()
                .email(email)
                .passwordHash("")
                .build();

        when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches(password, "")).thenReturn(false);

        // Act & Assert
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> {
            authService.login(email, password);
        });

        assertTrue(exception.getMessage().contains("Incorrect Password"));
        verify(accountRepository).findByEmail(email);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void login_successfulLogin_shouldGenerateValidToken() {
        // Arrange
        String email = "user@domain.com";
        String password = "securepass";
        String hash = "$2a$10$hashedpassword";
        String generatedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

        Account account = Account.builder()
                .email(email)
                .userName("Test User")
                .passwordHash(hash)
                .build();

        when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches(password, hash)).thenReturn(true);
        when(jwtUtil.generateToken(email)).thenReturn(generatedToken);

        // Act
        LoginResp result = authService.login(email, password);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getToken());
        assertEquals(generatedToken, result.getToken());
        verify(jwtUtil).generateToken(email);
    }
}
