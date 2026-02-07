package org.odema.posnew.service.impl;

import lombok.RequiredArgsConstructor;
import org.odema.posnew.dto.request.LoginRequest;
import org.odema.posnew.dto.request.RegisterRequest;
import org.odema.posnew.dto.response.LoginResponse;
import org.odema.posnew.entity.User;
import org.odema.posnew.entity.enums.UserRole;
import org.odema.posnew.exception.BadRequestException;
import org.odema.posnew.repository.UserRepository;
import org.odema.posnew.security.JwtTokenProvider;
import org.odema.posnew.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.username(),
                        loginRequest.password()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        User user = userRepository.findByUsername(loginRequest.username())
                .orElseThrow(() -> new BadRequestException("Utilisateur non trouvé"));

        // Mettre à jour la dernière connexion
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

       // Set<String> roles = Collections.singleton(user.getUserRole().name());

        return new LoginResponse(
                jwt,
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getUserRole()


        );
    }

    @Override
    public LoginResponse register(RegisterRequest registerRequest) {
        // Vérifier si l'utilisateur existe déjà
        if (userRepository.existsByUsername(registerRequest.username())) {
            throw new BadRequestException("Nom d'utilisateur déjà utilisé");
        }

        if (userRepository.existsByEmail(registerRequest.email())) {
            throw new BadRequestException("Email déjà utilisé");
        }

        // Créer un nouvel utilisateur
        User user = User.builder()
                .username(registerRequest.username())
                .password(passwordEncoder.encode(registerRequest.password()))
                .email(registerRequest.email())
                .phone(registerRequest.phone())
                .address(registerRequest.address())
                .userRole(registerRequest.role() != null ? registerRequest.role() : UserRole.CASHIER)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        // Authentifier automatiquement après l'inscription
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        registerRequest.username(),
                        registerRequest.password()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

       // Set<String> roles = Collections.singleton(savedUser.getUserRole().name());

        return new LoginResponse(
                jwt,
                savedUser.getUserId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getUserRole()


        );
    }
}