package com.longscoop.ruankao.api;

import com.longscoop.ruankao.auth.AdminAuthService;
import com.longscoop.ruankao.auth.RuankaoPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/auth/admin/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        AdminAuthService.LoginResult result =
                adminAuthService.login(request.username(), request.password());
        AdminAuthService.AdminProfile admin = result.admin();
        return new LoginResponse(
                result.token(),
                admin.userId(),
                admin.username(),
                admin.displayName(),
                admin.role());
    }

    @GetMapping("/admin/auth/me")
    public AdminAuthService.AdminProfile me(
            @AuthenticationPrincipal RuankaoPrincipal principal) {
        return adminAuthService.getProfile(principal.userId());
    }

    @PostMapping("/admin/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        adminAuthService.logout(extractBearerToken(authorization));
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null
                || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return "";
        }
        return authorization.substring(7).trim();
    }

    public record LoginRequest(String username, String password) {
    }

    public record LoginResponse(
            String token,
            long userId,
            String username,
            String displayName,
            String role) {
    }
}
