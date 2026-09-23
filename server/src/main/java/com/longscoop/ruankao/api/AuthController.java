package com.longscoop.ruankao.api;

import com.longscoop.ruankao.auth.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/wechat/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.loginWechat(request.code());
        return new LoginResponse(
                result.token(),
                result.userId(),
                result.profileCompleted());
    }

    public record LoginRequest(String code) {
    }

    public record LoginResponse(String token, long userId, boolean profileCompleted) {
    }
}
