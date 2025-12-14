package me.boonyarit.finance.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
    "security.jwt.secret=oGPlU1Q4HgnT0m0kEaDISKtUAvr2BJ1WTbTJK4VuoZ4=",
    "security.jwt.expiration-ms=900000"
})
class SecurityConfigTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void passwordEncoderShouldEncodePassword() {
        String password = "testPassword123";
        String encodedPassword = passwordEncoder.encode(password);

        assertNotEquals(password, encodedPassword);
        assertTrue(passwordEncoder.matches(password, encodedPassword));
    }
}
