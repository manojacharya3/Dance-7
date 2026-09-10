package com.studioos.controller;

import com.studioos.config.JwtService;
import com.studioos.dto.AuthResponse;
import com.studioos.dto.LoginRequest;
import com.studioos.dto.RegisterRequest;
import com.studioos.model.RefreshToken;
import com.studioos.model.User;
import com.studioos.repository.RefreshTokenRepository;
import com.studioos.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private static final String ACCESS_COOKIE = "dance7_access_token";
    private static final String REFRESH_COOKIE = "dance7_refresh_token";
    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final DataSource dataSource;
    private final Duration refreshTokenLifetime;
    private final boolean cookieSecure;

    public AuthController(
        UserService userService,
        JwtService jwtService,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        DataSource dataSource,
        @Value("${app.security.refresh-token-days:7}") long refreshTokenDays,
        @Value("${app.security.cookie-secure:false}") boolean cookieSecure
    ) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.dataSource = dataSource;
        this.refreshTokenLifetime = Duration.ofDays(refreshTokenDays);
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        User user = userService.register(request.email(), request.password(), request.fullName(), request.tenantId());
        issueTokens(user, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        String submittedEmail = request.email().trim().toLowerCase();
        User user;
        try {
            user = userService.getByEmail(submittedEmail);
            logger.warn("LOGIN_DEBUG userFound=true submittedEmail={} userId={} enabled={} storedPasswordHash={}",
                submittedEmail, user.getId(), user.isEnabled(), user.getPassword());
        } catch (UsernameNotFoundException exception) {
            logger.warn("LOGIN_DEBUG userFound=false submittedEmail={}", submittedEmail);
            logDatabaseDetails();
            throw exception;
        }

        logDatabaseDetails();
        boolean passwordMatches = passwordEncoder.matches(request.password(), user.getPassword());
        logger.warn("LOGIN_DEBUG passwordMatches={} submittedEmail={}", passwordMatches, submittedEmail);
        if (!passwordMatches || !user.isEnabled()) {
            throw new org.springframework.security.authentication.BadCredentialsException("Invalid email or password");
        }
        issueTokens(user, response);
        return toResponse(user);
    }

    private void logDatabaseDetails() {
        try (Connection connection = dataSource.getConnection()) {
            logger.warn("LOGIN_DEBUG databaseUrl={} databaseName={}",
                connection.getMetaData().getURL(), connection.getCatalog());
        } catch (SQLException exception) {
            logger.warn("LOGIN_DEBUG databaseDetailsUnavailable={}", exception.getMessage());
        }
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
        @CookieValue(value = REFRESH_COOKIE, required = false) String refreshCookie,
        HttpServletResponse response
    ) {
        if (refreshCookie == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is required");
        RefreshToken stored = refreshTokenRepository.findByToken(refreshCookie)
            .filter(item -> !item.isRevoked() && item.getExpiresAt().isAfter(Instant.now()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired"));
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        issueTokens(stored.getUser(), response);
        return toResponse(stored.getUser());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @CookieValue(value = REFRESH_COOKIE, required = false) String refreshCookie,
        HttpServletResponse response
    ) {
        if (refreshCookie != null) {
            refreshTokenRepository.findByToken(refreshCookie).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }
        clearCookie(ACCESS_COOKIE, response);
        clearCookie(REFRESH_COOKIE, response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public AuthResponse me(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return toResponse(userService.getByEmail(authentication.getName()));
    }

    private void issueTokens(User user, HttpServletResponse response) {
        refreshTokenRepository.deleteByUserId(user.getId());
        String refreshValue = UUID.randomUUID().toString();
        refreshTokenRepository.save(new RefreshToken(refreshValue, user, Instant.now().plus(refreshTokenLifetime)));
        addCookie(ACCESS_COOKIE, jwtService.createAccessToken(user), 900, response);
        addCookie(REFRESH_COOKIE, refreshValue, (int) refreshTokenLifetime.toSeconds(), response);
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(user.getId(), user.getEmail(), user.getFullName(), user.getTenantId(),
            user.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toSet()),
            user.getBranchId(), user.getInstructorId());
    }

    private void addCookie(String name, String value, int maxAge, HttpServletResponse response) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setAttribute("SameSite", cookieSecure ? "None" : "Lax");
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    private void clearCookie(String name, HttpServletResponse response) { addCookie(name, "", 0, response); }
}
