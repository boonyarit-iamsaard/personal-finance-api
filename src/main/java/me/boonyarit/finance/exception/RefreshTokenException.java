package me.boonyarit.finance.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class RefreshTokenException extends RuntimeException {

    private final String token;

    public RefreshTokenException(String message) {
        super(message);
        this.token = null;
    }

    public RefreshTokenException(String token, String message) {
        super(message);
        this.token = token;
    }
}
