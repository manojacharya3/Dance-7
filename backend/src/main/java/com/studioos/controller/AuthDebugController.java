package com.studioos.controller;

import com.studioos.model.User;
import com.studioos.repository.UserRepository;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("local")
@RequestMapping("/api/auth")
public class AuthDebugController {
    private final UserRepository userRepository;
    private final boolean debugUserEnabled;

    public AuthDebugController(
        UserRepository userRepository,
        @Value("${app.security.debug-user-enabled:false}") boolean debugUserEnabled
    ) {
        this.userRepository = userRepository;
        this.debugUserEnabled = debugUserEnabled;
    }

    @GetMapping("/debug-user")
    public ResponseEntity<Map<String, Object>> debugUser(@RequestParam String email) {
        if (!debugUserEnabled) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return userRepository.findByEmailIgnoreCase(email.trim())
            .map(this::foundResponse)
            .orElseGet(() -> ResponseEntity.ok(Map.of(
                "email", email,
                "enabled", false,
                "passwordHash", "",
                "userFound", false
            )));
    }

    private ResponseEntity<Map<String, Object>> foundResponse(User user) {
        return ResponseEntity.ok(Map.of(
            "email", user.getEmail(),
            "enabled", user.isEnabled(),
            "passwordHash", user.getPassword(),
            "userFound", true
        ));
    }
}
