package org.odema.posnew.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.odema.posnew.dto.request.LoginRequest;
import org.odema.posnew.dto.request.RegisterRequest;
import org.odema.posnew.dto.response.LoginResponse;
import org.odema.posnew.repository.UserRepository;
import org.odema.posnew.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {

        return ResponseEntity.ok(authService.login(loginRequest));


    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest  registerRequest) {
        authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Utilisateur enregistré avec succès");
    }
}
